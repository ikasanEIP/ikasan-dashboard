package org.ikasan.rest.dashboard.model.scheduled;

import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;

import java.util.Objects;

public class ScheduledProcessEventImpl implements ScheduledProcessEvent<Outcome> {
    private String agentName;
    private String jobName;
    private String jobGroup;
    private String jobDescription;
    private String commandLine;
    private String resultOutput;
    private String resultError;
    private long pid;
    private String user;
    private long fireTime;
    private long nextFireTime;
    private boolean successful;
    private long completionTime;
    private int returnCode;
    private Outcome outcome;

    @Override
    public int getReturnCode() {
        return this.returnCode;
    }

    @Override
    public void setReturnCode(int result) {
        this.returnCode = result;
    }

    @Override
    public boolean isSuccessful() {
        return this.successful;
    }

    @Override
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    @Override
    public long getCompletionTime() {
        return this.completionTime;
    }

    @Override
    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    @Override
    public String getAgentName() {
        return agentName;
    }

    @Override
    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getJobGroup() {
        return jobGroup;
    }

    @Override
    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    @Override
    public String getJobDescription() {
        return this.jobDescription;
    }

    @Override
    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    @Override
    public String getCommandLine() {
        return commandLine;
    }

    @Override
    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    public String getResultOutput() {
        return resultOutput;
    }

    @Override
    public void setResultOutput(String resultOutput) {
        this.resultOutput = resultOutput;
    }

    @Override
    public String getResultError() {
        return resultError;
    }

    @Override
    public void setResultError(String resultError) {
        this.resultError = resultError;
    }

    @Override
    public long getPid() {
        return pid;
    }

    @Override
    public void setPid(long pid) {
        this.pid = pid;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public long getFireTime() {
        return fireTime;
    }

    @Override
    public void setFireTime(long fireTime) {
        this.fireTime = fireTime;
    }

    @Override
    public long getNextFireTime() {
        return nextFireTime;
    }

    @Override
    public void setNextFireTime(long nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    @Override
    public Outcome getOutcome() {
        return this.outcome;
    }

    @Override
    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    @Override
    public boolean isJobStarting() {
            return false;
    }

    @Override
    public void setJobStarting(boolean jobStarting) {

    }

    @Override
    public boolean isDryRun() {
        return false;
    }

    @Override
    public void setDryRun(boolean dryRun) {

    }

    @Override
    public String getContextId() {
        return null;
    }

    @Override
    public void setContextId(String contextId) {

    }

    @Override
    public String getContextInstanceId() {
        return null;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledProcessEventImpl that = (ScheduledProcessEventImpl) o;
        return pid == that.pid &&
            fireTime == that.fireTime &&
            nextFireTime == that.nextFireTime &&
            successful == that.successful &&
            completionTime == that.completionTime &&
            returnCode == that.returnCode &&
            Objects.equals(agentName, that.agentName) &&
            Objects.equals(jobName, that.jobName) &&
            Objects.equals(jobGroup, that.jobGroup) &&
            Objects.equals(jobDescription, that.jobDescription) &&
            Objects.equals(commandLine, that.commandLine) &&
            Objects.equals(resultOutput, that.resultOutput) &&
            Objects.equals(resultError, that.resultError) &&
            Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int) fireTime;
        hash = 31 * hash + (agentName == null ? 0 : agentName.hashCode());
        hash = 31 * hash + (jobName == null ? 0 : jobName.hashCode());
        hash = 31 * hash + (jobDescription == null ? 0 : jobDescription.hashCode());
        return hash;
    }
}
