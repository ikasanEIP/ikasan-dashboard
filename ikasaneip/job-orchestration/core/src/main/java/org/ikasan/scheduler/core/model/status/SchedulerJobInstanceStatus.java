package org.ikasan.scheduler.core.model.status;

import org.ikasan.scheduler.core.spec.InstanceStatus;

public class SchedulerJobInstanceStatus {
    private String jobName;
    private String agentName;
    private InstanceStatus instanceStatus;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }
}
