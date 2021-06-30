package org.ikasan.dashboard.ui.scheduler.model;

import org.ikasan.dashboard.ui.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleProcessAggregateConfiguration {

    private String agentName;
    private String jobName;
    private String jobGroup;
    private String jobDescription;
    private String cronExpression;
    private DateTimeUtil.TimezonePair timezone;
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

    public DateTimeUtil.TimezonePair getTimezone() {
        return timezone;
    }

    public void setTimezone(DateTimeUtil.TimezonePair timezone) {
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
        return blackoutCronExpressions;
    }

    public void setBlackoutCronExpressions(List<String> blackoutCronExpressions) {
        this.blackoutCronExpressions = blackoutCronExpressions;
    }

    public Map<Long, Long> getBlackoutDateTimeRanges() {
        return blackoutDateTimeRanges;
    }

    public void setBlackoutDateTimeRanges(Map<Long, Long> blackoutDateTimeRanges) {
        this.blackoutDateTimeRanges = blackoutDateTimeRanges;
    }
}

