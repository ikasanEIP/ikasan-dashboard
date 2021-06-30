package org.ikasan.scheduled.model;

public class UpcomingScheduledProcess {
    private String agentName;
    private String jobName;
    private String jobGroup;
    private String jobDescription;
    private String commandLine;
    private long fireTime;

    public UpcomingScheduledProcess(String agentName, String jobName, String jobGroup, String jobDescription, String commandLine, long fireTime) {
        this.agentName = agentName;
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.jobDescription = jobDescription;
        this.commandLine = commandLine;
        this.fireTime = fireTime;
    }

    public String getAgentName() {
        return agentName;
    }

    
    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    
    public String getJobName() {
        return jobName;
    }

    
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    
    public String getJobGroup() {
        return jobGroup;
    }

    
    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    
    public String getJobDescription() {
        return this.jobDescription;
    }

    
    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    
    public String getCommandLine() {
        return commandLine;
    }

    
    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    
    public long getFireTime() {
        return fireTime;
    }

    
    public void setFireTime(long fireTime) {
        this.fireTime = fireTime;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("UpcomingScheduledProcess{");
        sb.append("agentName='").append(agentName).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append(", jobGroup='").append(jobGroup).append('\'');
        sb.append(", jobDescription='").append(jobDescription).append('\'');
        sb.append(", commandLine='").append(commandLine).append('\'');
        sb.append(", fireTime=").append(fireTime);
        sb.append('}');
        return sb.toString();
    }
}
