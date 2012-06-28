package com.x.biz;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.x.infra.application.workflow.TopicManager;
import com.x.infra.application.workflow.def.IWorkflowDef;
import com.x.infra.application.workflow.def.IllegalWfDefException;
import com.x.infra.application.workflow.def.avro.WorkflowDefConfig;
import com.x.infra.application.workflow.def.avro.WorkflowDefinitionManager;
import com.x.infra.application.xfabric.client.FabricManagerRestClient;
import com.x.infra.application.xfabric.client.restapi.BatchInit;
import com.x.infra.application.xfabric.client.restapi.RestApiConstants;

/**
 * 1. Generates the json file to be submitted for batch initialization
 * 2. Posts the json file to XManager (can be turned off by post2X=false in cmd line)
 * 3. Posts the XManager response back to the capability (can be turned off by post2Cap=false in cmd line)
 * 
 * Takes the capability name and endpoint from xfabric.properties
 * Takes the workflow definition file name from workflow.properties
 * Takes the xmanager api url, username, password from xmanager.properties
 * */
public class CapabilityInitializer {

	private static final String XFABRIC_PROPERTIES = "xfabric.properties";
	private static final String CAPABILITY_NAME = "xFabricConfig.capabilityName";
	private static final String ENDPOINT_URI = "xFabricConfig.myself";
	
	private static final String XMANAGER_PROPERTIES = "xmanager.properties";
	private static final String XMANAGER_USERNAME = "xmanagerConfig.username";
	private static final String XMANAGER_PASSWORD = "xmanagerConfig.password";
	private static final String XMANAGER_API_URL = "xmanagerConfig.apiUrl";
	
	private static final String WORKFLOW_PROPERTIES = "workflow.properties";
	private static final String WORKFLOW_JSON_FILE_PATTERN = "workflowDefConfig.workflows\\[\\d+\\]";
	
	private static final String SET_CAPABILITY_SPECIFIC_DATA_ENDPOINT = "/setCapabilitySpecificData";
	
	private static final String ENV_DEVELOPMENT = "development";
	private static final String APPLICATION_JSON = "application/json";
	
	private static final String POST_2_X = "post2X=";
	private static final String POST_2_CAP = "post2Cap=";
	
	private static final String TEST_MERCHANT = "testMerchant";
	
	private CapabilityInitializer() {}
	
	private void init(boolean postToXManager, boolean postToCapability) throws IOException, IllegalWfDefException {
		String capabilityName = null;
		String endpointUri =  null;
		List<String> tenants = new ArrayList<String>();
		tenants.add(TEST_MERCHANT);
		
		Map<String,String> envMap = System.getenv();
		String xcomEnv = envMap.get("XCOM_ENV");
		String environment = xcomEnv != null ? xcomEnv : ENV_DEVELOPMENT;
		Properties xfabricProps = getProperties(environment + File.separator + XFABRIC_PROPERTIES);
		
		if(xfabricProps != null) {
			capabilityName = xfabricProps.getProperty(CAPABILITY_NAME);
			endpointUri = xfabricProps.getProperty(ENDPOINT_URI);
		}
		
		Properties workflowProps = getProperties(WORKFLOW_PROPERTIES);
		WorkflowDefConfig config = new WorkflowDefConfig();
		List<String> workflowFiles = new LinkedList<String>();
		Set<Object> keys = workflowProps.keySet();
		for(Object key : keys) {
			String k = (String)key;
			if(k.matches(WORKFLOW_JSON_FILE_PATTERN)) {
				String file = workflowProps.getProperty(k);
				workflowFiles.add(file);
			}
		}
		config.setWorkflows(workflowFiles);
		WorkflowDefinitionManager wfDefManager = WorkflowDefinitionManager.theInstance();
		wfDefManager.init(config);
		Map<String, IWorkflowDef> workflows = wfDefManager.getRegisteredWorkflows();
		Set<String> allTopics = new HashSet<String>();
		Set<String> subscribedTopics = new HashSet<String>();
		for(String wfId : workflows.keySet()) {
			IWorkflowDef wfDef = workflows.get(wfId);
			 allTopics.addAll(wfDef.getAllTopics());
			 subscribedTopics.addAll(wfDef.getSubscribedTopics());
		}
		
		// remove any system topics so that they are not sent to XManager
		for(String sysTopic : TopicManager.XFABRIC_SYSTEM_TOPICS) {
			allTopics.remove(sysTopic);
			subscribedTopics.remove(sysTopic);
		}
		
		// Ideally these topis should be added by XFabric to every capability
		// Once XFabric does that, these can be removed
		allTopics.add(TopicManager.CORE_MESSAGE_RECEIVED);
		allTopics.add(TopicManager.CORE_MESSAGE_VALIDATED);
		allTopics.add(TopicManager.CORE_TRANSACTION_CANCELLED);
		allTopics.add(TopicManager.CORE_TRANSACTION_COMPLETED);
		subscribedTopics.add(TopicManager.CORE_MESSAGE_RECEIVED);
		subscribedTopics.add(TopicManager.CORE_MESSAGE_VALIDATED);
		subscribedTopics.add(TopicManager.CORE_TRANSACTION_CANCELLED);
		subscribedTopics.add(TopicManager.CORE_TRANSACTION_COMPLETED);	
				
		BatchInit batchInit = constructBatchInitJson(capabilityName, endpointUri, tenants, 
				new ArrayList<String>(allTopics), new ArrayList<String>(subscribedTopics));
		
		if(postToXManager) {
			String xManagerUser = null, xManagerPass = null, xManagerApiUrl = null;
			String xmPropsFile = environment + File.separator + XMANAGER_PROPERTIES;
			Properties xmanagerProps = null;
			try {
				xmanagerProps = getProperties(xmPropsFile); 
			} catch(IOException e) {
				System.out.println("Cannot find file " + xmPropsFile + ". Will use defaults");
			}
			
			if(xmanagerProps != null) {
				xManagerUser = xmanagerProps.getProperty(XMANAGER_USERNAME);
				xManagerPass = xmanagerProps.getProperty(XMANAGER_PASSWORD);
				xManagerApiUrl = xmanagerProps.getProperty(XMANAGER_API_URL);
			}
			
			if(xManagerUser == null)
				xManagerUser = RestApiConstants.DEFAULT_USERNAME;
			
			if(xManagerPass == null)
				xManagerPass = RestApiConstants.DEFAULT_PASSWORD;
			
			if(xManagerApiUrl == null)
				xManagerApiUrl = RestApiConstants.DEFAULT_BASE_MANAGER_API_URL;
				
			
			FabricManagerRestClient restClient = new FabricManagerRestClient(xManagerUser, xManagerPass, xManagerApiUrl);
			System.out.println("Posting batch init json to XManager");
			System.out.println(batchInit.toJson());
			HttpResponse response = restClient.post(appendPathToUrl(xManagerApiUrl, "/batch"), 
					batchInit.toJson(), APPLICATION_JSON);
			int statusCode = response.getStatusLine().getStatusCode();
			String xResponse = EntityUtils.toString(response.getEntity());
			EntityUtils.consume(response.getEntity());
			System.out.println("Response from XManager: " + xResponse);
			
			if(postToCapability && statusCode == HttpStatus.SC_OK) {			
				if(postToCapability) {
					System.out.println("Posting XManager response to capability");
					URL u = new URL(endpointUri);
					restClient.setTargetHost(new HttpHost(u.getHost(), u.getPort(), u.getProtocol()));
					response = restClient.post(
							appendPathToUrl(endpointUri, SET_CAPABILITY_SPECIFIC_DATA_ENDPOINT),
							xResponse, APPLICATION_JSON);
					statusCode = response.getStatusLine().getStatusCode();
					String respBody = EntityUtils.toString(response.getEntity());
					System.out.println("status code: " + statusCode);
					System.out.println(respBody);
					EntityUtils.consume(response.getEntity());
				}
			}
		}
	}
	
	private Properties getProperties(String resourceFile) throws IOException {
		Resource resource = new ClassPathResource(resourceFile);
		return PropertiesLoaderUtils.loadProperties(resource);
	}
	
	private BatchInit constructBatchInitJson(String capabilityName, String endpointUri, 
			List<String> tenants, List<String> allTopics, List<String> subscribeTopics) {
		
		BatchInit batchInit = new BatchInit();
		
		List<BatchInit.Topic> topicToCreate = new ArrayList<BatchInit.Topic>();
		for(String t : allTopics) {
			topicToCreate.add(new BatchInit.Topic(t, false, false));
		}
		
		batchInit.setTopics(topicToCreate);
		
		List<BatchInit.Tenant> tenantsToCreate = new ArrayList<BatchInit.Tenant>();
		for(String tenantName : tenants) {
			tenantsToCreate.add(new BatchInit.Tenant(tenantName));
		}
		
		batchInit.setTenants(tenantsToCreate);
		
		List<BatchInit.Authorization> authForCap = new ArrayList<BatchInit.Authorization>();
		for(String tenantName : tenants) {
			authForCap.add(new BatchInit.Authorization(tenantName, null, null));
		}
		
		List<BatchInit.Capability> capsToCreate = new ArrayList<BatchInit.Capability>();
		BatchInit.Capability cap = new BatchInit.Capability();
		cap.setName(capabilityName);
		cap.setEndpointUri(endpointUri);
		cap.setSyncEnabled(false);
		cap.setOnboardingUri(null);
		cap.setOnboardingKeys(new LinkedList<String>());
		cap.setAuthorizations(authForCap);
		cap.setTopics(subscribeTopics);
		capsToCreate.add(cap);
		
		batchInit.setCapabilities(capsToCreate);
		System.out.println(batchInit.toJson());
		
		return batchInit;
	}
	
	private String appendPathToUrl(String baseUrl, String pathToAppend) {
		if(baseUrl != null && baseUrl.length() > 1 && baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length()-1);
		}
		
		if(pathToAppend != null && !pathToAppend.startsWith("/")) {
			pathToAppend = "/" + pathToAppend;
		}
		
		return baseUrl + pathToAppend;
	}
	
	public static void main(String... args) throws Exception {
		boolean postToXManager = true;
		boolean postToCapability = true;
		
		if(args != null && args.length > 0) {
			for(String arg : args) {
				if(arg.startsWith(POST_2_X)) {
					String argVal = arg.substring(POST_2_X.length());
					postToXManager = Boolean.parseBoolean(argVal);
				} else if(arg.startsWith(POST_2_CAP)) {
					String argVal = arg.substring(POST_2_CAP.length());
					postToCapability = Boolean.parseBoolean(argVal);
				} 
			}
			if(!postToXManager && postToCapability) {
				throw new IllegalArgumentException("Invalid configuration. " +
						"You cannot post to the capability without posting to XManager first!");
			}
		}
			
		new CapabilityInitializer().init(postToXManager, postToCapability); 
	}

}
