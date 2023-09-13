package org.ikasan.job.orchestration.model.status;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class ContextMachineStatus {

    private String contextName;
    private String contextInstanceId;
    private InstanceStatus instanceStatus;

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public String getContextInstanceId() {
        return contextInstanceId;
    }

    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    @Override
    public String toString() {
        return "ContextMachineStatus{" +
            "contextName='" + contextName + '\'' +
            ", contextInstanceId='" + contextInstanceId + '\'' +
            ", instanceStatus=" + instanceStatus +
            '}';
    }
}
