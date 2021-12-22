package org.ikasan.scheduler.core.model.status;


import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.List;

public class ContextInstanceStatus {
    private String contextName;
    private InstanceStatus instanceStatus;
    private List<ContextInstanceStatus> contextInstanceStatuses;
    private List<SchedulerJobInstanceStatus> schedulerJobInstanceStatuses;

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public List<ContextInstanceStatus> getContextInstanceStatuses() {
        return contextInstanceStatuses;
    }

    public void setContextInstanceStatuses(List<ContextInstanceStatus> contextInstanceStatuses) {
        this.contextInstanceStatuses = contextInstanceStatuses;
    }

    public List<SchedulerJobInstanceStatus> getSchedulerJobInstanceStatuses() {
        return schedulerJobInstanceStatuses;
    }

    public void setSchedulerJobInstanceStatuses(List<SchedulerJobInstanceStatus> schedulerJobInstanceStatuses) {
        this.schedulerJobInstanceStatuses = schedulerJobInstanceStatuses;
    }
}
