package org.ikasan.scheduled.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduledProcessAggregateConfiguration {

    private String agentName;
    private String jobName;
    private String jobGroup;
    private String jobDescription;
    private String cronExpression;
    private String timezone;
    private String commandLine;
    private String stdOut;
    private String stdErr;
    private String threshold;
    private Boolean eager;
    private Boolean retryOnFail;
    private Boolean ignoreMisfire = true;
    private Integer maxEagerCallbacks;
    private Map<String,String> passthroughProperties = new HashMap<>();
    private String workingDirectory;
    private List<Integer> successfulReturnCodes = new ArrayList<>();
    private Long secondsToWaitForProcessStart = 10L;
    private List<String> blackoutCronExpressions = new ArrayList<>();
    private Map<Long,Long> blackoutDateTimeRanges = new HashMap<>();

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
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public String getStdOut() {
        return stdOut;
    }

    public void setStdOut(String stdOut) {
        this.stdOut = stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }

    public void setStdErr(String stdErr) {
        this.stdErr = stdErr;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    public Boolean isEager() {
        return eager;
    }

    public void setEager(Boolean eager) {
        this.eager = eager;
    }

    public Boolean isRetryOnFail() {
        return retryOnFail;
    }

    public void setRetryOnFail(Boolean retryOnFail) {
        this.retryOnFail = retryOnFail;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public Boolean isIgnoreMisfire() {
        return ignoreMisfire;
    }

    public void setIgnoreMisfire(Boolean ignoreMisfire) {
        this.ignoreMisfire = ignoreMisfire;
    }

    public Integer getMaxEagerCallbacks() {
        return maxEagerCallbacks;
    }

    public void setMaxEagerCallbacks(Integer maxEagerCallbacks) {
        this.maxEagerCallbacks = maxEagerCallbacks;
    }

    public Map<String, String> getPassthroughProperties() {
        if(this.passthroughProperties == null) return new HashMap<>();
        return passthroughProperties;
    }

    public void setPassthroughProperties(Map<String, String> passthroughProperties) {
        this.passthroughProperties = passthroughProperties;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public List<Integer> getSuccessfulReturnCodes() {
        if(this.successfulReturnCodes == null) return new ArrayList<>();
        return successfulReturnCodes;
    }

    public void setSuccessfulReturnCodes(List<Integer> successfulReturnCodes) {
        this.successfulReturnCodes = successfulReturnCodes;
    }

    public Long getSecondsToWaitForProcessStart() {
        return secondsToWaitForProcessStart;
    }

    public void setSecondsToWaitForProcessStart(Long secondsToWaitForProcessStart) {
        this.secondsToWaitForProcessStart = secondsToWaitForProcessStart;
    }

    public List<String> getBlackoutCronExpressions() {
        if(this.blackoutCronExpressions == null) return new ArrayList<>();
        return blackoutCronExpressions;
    }

    public void setBlackoutCronExpressions(List<String> blackoutCronExpressions) {
        this.blackoutCronExpressions = blackoutCronExpressions;
    }

    public Map<Long, Long> getBlackoutDateTimeRanges() {
        if(this.blackoutDateTimeRanges == null) return new HashMap<>();
        return this.blackoutDateTimeRanges;
    }

    public void setBlackoutDateTimeRanges(Map<Long, Long> blackoutDateTimeRanges) {
        this.blackoutDateTimeRanges = blackoutDateTimeRanges;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ScheduledProcessAggregateConfiguration{");
        sb.append("agentName='").append(agentName).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append(", jobGroup='").append(jobGroup).append('\'');
        sb.append(", jobDescription='").append(jobDescription).append('\'');
        sb.append(", cronExpression='").append(cronExpression).append('\'');
        sb.append(", timezone='").append(timezone).append('\'');
        sb.append(", commandLine='").append(commandLine).append('\'');
        sb.append(", stdOut='").append(stdOut).append('\'');
        sb.append(", stdErr='").append(stdErr).append('\'');
        sb.append(", threshold='").append(threshold).append('\'');
        sb.append(", eager=").append(eager);
        sb.append(", retryOnFail=").append(retryOnFail);
        sb.append(", ignoreMisfire=").append(ignoreMisfire);
        sb.append(", maxEagerCallbacks=").append(maxEagerCallbacks);
        sb.append(", passthroughProperties=").append(passthroughProperties);
        sb.append(", workingDirectory='").append(workingDirectory).append('\'');
        sb.append(", successfulReturnCodes=").append(successfulReturnCodes);
        sb.append(", secondsToWaitForProcessStart=").append(secondsToWaitForProcessStart);
        sb.append(", blackoutCronExpressions=").append(blackoutCronExpressions);
        sb.append(", blackoutDateTimeRanges=").append(blackoutDateTimeRanges);
        sb.append('}');
        return sb.toString();
    }
}

