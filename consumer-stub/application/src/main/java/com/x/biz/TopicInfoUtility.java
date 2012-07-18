package com.x.biz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.x.infra.application.workflow.TopicManager;
import com.x.infra.application.workflow.def.IWorkflowDef;
import com.x.infra.application.workflow.def.IllegalWfDefException;
import com.x.infra.application.workflow.def.WorkflowDefKey;
import com.x.infra.application.workflow.def.avro.WorkflowDefConfig;
import com.x.infra.application.workflow.def.avro.WorkflowDefinitionManager;

public class TopicInfoUtility {
	
	private static final String WORKFLOW_PROPERTIES = "workflow.properties";
	private static final String WORKFLOW_JSON_FILE_PATTERN = "workflowDefConfig.workflows\\[\\d+\\]";
	
	private TopicInfoUtility() {}
	
	private void init() throws IOException, IllegalWfDefException {
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
		Map<WorkflowDefKey, IWorkflowDef> workflows = wfDefManager.getRegisteredWorkflows();
		Set<String> allTopics = new HashSet<String>();
		Set<String> subscribedTopics = new HashSet<String>();
		for(WorkflowDefKey defKey : workflows.keySet()) {
			IWorkflowDef wfDef = workflows.get(defKey);
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
		
		List<String> allTopicsList = new ArrayList<String>();
		for(String s : allTopics) 
			allTopicsList.add(s);
		
		List<String> subscribedTopicsList = new ArrayList<String>();
		for(String s : subscribedTopics) 
			subscribedTopicsList.add(s);
		
		Collections.sort(allTopicsList);
		Collections.sort(subscribedTopicsList);
		
		System.out.println("### TOPICS TO CREATE ###");
		for(String topic : allTopicsList) {
			System.out.println(topic);
		}
		System.out.println();
		
		System.out.println("### TOPICS TO SUBSRIBE TO ###");
		for(String topic : subscribedTopicsList) {
			System.out.println(topic);
		}
		
	}
	
	private Properties getProperties(String resourceFile) throws IOException {
		Resource resource = new ClassPathResource(resourceFile);
		return PropertiesLoaderUtils.loadProperties(resource);
	}
	
	public static void main(String... args) throws Exception {
		new TopicInfoUtility().init(); 
	}

}
