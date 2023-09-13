package org.ikasan.job.orchestration.model.status;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.List;

public class ContextJobInstanceStatus {

    private String contextName;
    private String contextInstanceId;
    private InstanceStatus instanceStatus;
    private List<ContextJobInstanceDetailsStatus> jobDetails;

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

    public List<ContextJobInstanceDetailsStatus> getJobDetails() {
        return jobDetails;
    }

    public void setJobDetails(List<ContextJobInstanceDetailsStatus> jobDetails) {
        this.jobDetails = jobDetails;
    }

    @Override
    public String toString() {
        return "ContextJobInstanceStatus{" +
            "contextName='" + contextName + '\'' +
            ", contextInstanceId='" + contextInstanceId + '\'' +
            ", instanceStatus=" + instanceStatus +
            ", jobDetails=" + jobDetails +
            "}\n";
    }
}
