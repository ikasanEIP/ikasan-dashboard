package org.ikasan.dashboard.ui.scheduler.model;

public class ScheduledProcessFilter {

    private long startTime;

    private long endTime;

    private String agentName;

    private String jobName;

    private boolean errorsOnly = false;

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

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public boolean isErrorsOnly() {
        return errorsOnly;
    }

    public void setErrorsOnly(boolean errorsOnly) {
        this.errorsOnly = errorsOnly;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
