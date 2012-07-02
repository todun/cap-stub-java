This project contains all of your capability's configuration files. All configurations are located under the src/main/resources folder.
Environment specific configurations are located under their respective sub-folders.

The are few key files in this project that you should be familiar with:
- workflow.properties: this file should contain the list of workflows that your capability is implementing. For each workflow you
implement, you should add an entry for the workflowDefConfig.workflows array.

- xfabric.properties: environment-specific configuration file that contains properties that are specific to your Fabric interaction,
such as the Fabric endpoint.

The following are for local development only:
- Launcher.java: this small class contains the main entry point for you to start your capability locally in your IDE. After you have configured
the Fabric endpoint, simply run the main function to start an instance of your capability locally.

- CapabilityInitializer.java: After you have your capability instance running locally, use this small utility to register it with 
the Fabric. It uses the properties in xfabric.properties and xmanager.properties to register your capability.