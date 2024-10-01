package org.ikasan.job.orchestration.model.status;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceDetailsStatus;

import java.util.HashSet;
import java.util.Set;

public class ContextJobInstanceDetailsStatusImpl implements ContextJobInstanceDetailsStatus {

    private String jobName;
    private Set<String> childContextName = new HashSet<>();
    private InstanceStatus instanceStatus;
    private boolean targetResidingContextOnly;
    private boolean isErrorAcknowledged;
    private long startTime = 0;
    private long endTime = 0;

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public Set<String> getChildContextName() {
        return childContextName;
    }

    @Override
    public void setChildContextName(Set<String> childContextName) {
        this.childContextName = childContextName;
    }

    @Override
    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    @Override
    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return targetResidingContextOnly;
    }

    @Override
    public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean isErrorAcknowledged() {
        return isErrorAcknowledged;
    }

    @Override
    public void setErrorAcknowledged(boolean errorAcknowledged) {
        isErrorAcknowledged = errorAcknowledged;
    }

    @Override
    public String toString() {
        return "ContextJobInstanceDetailsStatusImpl{" +
            "jobName='" + jobName + '\'' +
            ", childContextName=" + childContextName +
            ", instanceStatus=" + instanceStatus +
            ", targetResidingContextOnly=" + targetResidingContextOnly +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            "}\n";
    }

    @Override
    public boolean checkExist(String jobName) {
        if (this.getJobName().equals(jobName) &&
            !this.isTargetResidingContextOnly()) {
            return true;
        }
        return false;
    }
}
