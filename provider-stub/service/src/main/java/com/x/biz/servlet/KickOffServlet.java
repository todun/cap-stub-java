package com.x.biz.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.x.infra.application.workflow.engine.WorkflowManager;

public class KickOffServlet extends HttpServlet {
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static Logger LOG = LoggerFactory.getLogger(KickOffServlet.class);
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {
    	handleRequest(req, resp);
    }
    
    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {
    	handleRequest(req, resp);
    }
    
    private void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
    		IOException{
    	PrintWriter out = resp.getWriter();
        
    	String workflowId = req.getParameter("workflowId");
    	String tenantPseudonym = req.getParameter("tenantPseudonym");
    	if(StringUtils.isNotBlank(workflowId) && StringUtils.isNotBlank(tenantPseudonym)) {
    		LOG.debug(String.format("WF >>>>>> Starting workflow. Type:%s Tenant:%s", workflowId, tenantPseudonym));
    		String error = null;
    		try {
    			WorkflowManager.theInstance().startWorkflow(workflowId, tenantPseudonym); 
            } catch(Exception e) {
            	error = e.getMessage();
            }
        	out.println(getForm(workflowId, tenantPseudonym, error, (error == null ? "Workflow started" : null)));
    	} else {
    		out.println(getForm(null, null, null, null));
    	}
    	
    }
    
    private String getForm(String workflowId, String tenantPseudonym, String error, String message) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("<html>");
    	sb.append("<head><title>Kick Off Servlet</title></head>");
    	sb.append("<body>");
    	sb.append("<h3>Kick Off Servlet</h3>");
    	if(error != null)
    		sb.append("ERROR: ").append(error);
    	if(message != null)
    		sb.append("SUCCESS: ").append(message);
    	sb.append("<form name=\"kickOffForm\" method=\"POST\">");
    	sb.append("Workflow ID: <input type=\"text\" name=\"workflowId\" size=\"100\" value=\"")
    	  .append((workflowId != null ? workflowId : "")).append("\"><br/>");
    	sb.append("Tenant Pseudonym: <input type=\"text\" name=\"tenantPseudonym\" size=\"100\" value=\"")
    	  .append((tenantPseudonym != null ? tenantPseudonym : "")).append("\"><br/>");
    	sb.append("<input type=\"submit\" value=\"Submit\">");
    	sb.append("</form>");
    	sb.append("</body>");
    	sb.append("</html>");
    	return sb.toString();
    }
}