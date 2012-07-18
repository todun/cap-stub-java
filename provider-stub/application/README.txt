This project contains all of your capability's configuration files. All configurations are located under the src/main/resources folder.
Environment specific configurations are located under their respective sub-folders.

The are few key files in this project that you should be familiar with:
- workflow.properties: this file should contain the list of workflows that your capability is implementing. For each workflow you
implement, you should add an entry for the workflowDefConfig.workflows array.

- xfabric.properties: environment-specific configuration file that contains properties that are specific to your Fabric interaction,
such as the Fabric endpoint.

- Launcher.java: this small class contains the main entry point for you to start your capability locally in your IDE. After you have 
configured the Fabric endpoint, simply run the main function to start an instance of your capability locally. (You do not need to 
start Launcher.java if you deploy your capability to a servlet engine like Tomcat that can communicate with the Fabric on a 
public interface. If your servlet engine does not run on a public interface, you must use the X.commerce Sync Bridge Client
to communicate with the Fabric.)

- TopicInfoUtility.java: You can use this utility to see which topics your capability should create and which ones it should subscribe to
