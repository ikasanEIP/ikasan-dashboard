package org.ikasan.job.orchestration.model.status;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.HashSet;
import java.util.Set;

public class ContextJobInstanceDetailsStatus {

    private String jobName;
    private Set<String> childContextName = new HashSet<>();
    private InstanceStatus instanceStatus;
    private boolean targetResidingContextOnly;
    private long startTime = 0;
    private long endTime = 0;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Set<String> getChildContextName() {
        return childContextName;
    }

    public void setChildContextName(Set<String> childContextName) {
        this.childContextName = childContextName;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public boolean isTargetResidingContextOnly() {
        return targetResidingContextOnly;
    }

    public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "ContextJobInstanceDetailsStatus{" +
            "jobName='" + jobName + '\'' +
            ", childContextName=" + childContextName +
            ", instanceStatus=" + instanceStatus +
            ", targetResidingContextOnly=" + targetResidingContextOnly +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            "}\n";
    }

    public boolean checkExist(String jobName) {
        if (this.getJobName().equals(jobName) &&
            !this.isTargetResidingContextOnly()) {
            return true;
        }
        return false;
    }
}
