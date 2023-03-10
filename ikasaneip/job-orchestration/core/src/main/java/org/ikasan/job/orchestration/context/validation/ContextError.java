package org.ikasan.job.orchestration.context.validation;

public class ContextError {
    private String contextName;
    private String errorMessage;
    private String jobName;

    public ContextError(String contextName, String errorMessage) {
        this.contextName = contextName;
        this.errorMessage = errorMessage;
    }

    public ContextError(String contextName, String errorMessage, String jobName) {
        this.contextName = contextName;
        this.errorMessage = errorMessage;
        this.jobName = jobName;
    }

    public String getContextName() {
        return contextName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getJobName() {
        return jobName;
    }
}
