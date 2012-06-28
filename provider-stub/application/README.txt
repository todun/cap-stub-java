This project contains all of your capability's configuration files. All configurations are located under the src/main/resources folder.
Environment specific configurations are located under their respective sub-folders.

The are few key files in this project that you should be familiar with:
- workflow.properties: this file should contain the list of workflows that your capability is implementing. For each workflow that you are
implementing, you should add an entry for the workflowDefConfig.workflows array.

- xfabric.properties: this environment specific configuration file contains properties that are specific to your Fabric interactiong like
the Fabric endpoint.

For local development purpose, the two included classes in this project are:
- Launcher.java: this small class contain the main entry point for you to start your capability locally in your IDE. Once you have configured
the Fabric endpoint, simply run the main function to start an instance of your capability locally.

- CapabilityInitializer.java: once you have your capability instance running locally, use this small utility program to register it with the fabric.
This program should only be use for local development only. It uses the properties in xfabric.properties and xmanager.properties to register your
capability.