This project should contain all of your business logic handler code that are registered to handle callbacks from the Workflow Engine.

Your callback method should be annotated with the @XTransactionCallback annotation and the class that contains that method should be initialized
as a bean on startup. This will allow your method to be registered with the workflow engine.