package org.ikasan.job.orchestration.rest.dashboard.status.model;

public class ContextStatusNameDto {

    private String instanceName;
    private String contextName;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }
}
