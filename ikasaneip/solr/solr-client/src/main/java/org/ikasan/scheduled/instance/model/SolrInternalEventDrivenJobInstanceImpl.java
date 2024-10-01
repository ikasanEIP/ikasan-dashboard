package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;

import java.util.List;
import java.util.Objects;

public class SolrInternalEventDrivenJobInstanceImpl extends SolrSchedulerJobInstanceImpl implements InternalEventDrivenJobInstance {
    private List<String> successfulReturnCodes;
    private String workingDirectory;
    private String commandLine;
    private long minExecutionTime;
    private long maxExecutionTime;
    private List<ContextParameter> contextParameters;
    private List<Integer> daysOfWeekToRun;
    private boolean targetResidingContextOnly;
    private String executionEnvironmentProperties;
    boolean participatesInLock;
    private boolean jobRepeatable;
    private boolean killed = false;
    private Boolean errorAcknowledged;
    private String errorAcknowledgedMessage;
    private String errorAcknowledgmentTicketId;
    private String errorAcknowledgeUser;
    private long errorAcknowledgeTimestamp;

    @Override
    public List<String> getSuccessfulReturnCodes() {
        return successfulReturnCodes;
    }

    @Override
    public void setSuccessfulReturnCodes(List<String> successfulReturnCodes) {
        this.successfulReturnCodes = successfulReturnCodes;
    }

    @Override
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
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
    public long getMinExecutionTime() {
        return minExecutionTime;
    }

    @Override
    public void setMinExecutionTime(long minExecutionTime) {
        this.minExecutionTime = minExecutionTime;
    }

    @Override
    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }

    @Override
    public void setMaxExecutionTime(long maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
    }

    @Override
    public List<ContextParameter> getContextParameters() {
        return contextParameters;
    }

    @Override
    public void setContextParameters(List<ContextParameter> contextParameters) {
        this.contextParameters = contextParameters;
    }

    @Override
    public List<Integer> getDaysOfWeekToRun() {
        return daysOfWeekToRun;
    }

    @Override
    public void setDaysOfWeekToRun(List<Integer> daysOfWeekToRun) {
        this.daysOfWeekToRun = daysOfWeekToRun;
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
    public boolean isParticipatesInLock() {
        return participatesInLock;
    }

    @Override
    public void setParticipatesInLock(boolean participatesInLock) {
        this.participatesInLock = participatesInLock;
    }

    @Override
    public String getExecutionEnvironmentProperties() {
        return executionEnvironmentProperties;
    }

    @Override
    public void setExecutionEnvironmentProperties(String executionEnvironmentProperties) {
        this.executionEnvironmentProperties = executionEnvironmentProperties;
    }

    @Override
    public boolean isJobRepeatable() {
        return jobRepeatable;
    }

    @Override
    public void setJobRepeatable(boolean jobRepeatable) {
        this.jobRepeatable = jobRepeatable;
    }

    @Override
    public boolean isKilled() {
        return killed;
    }

    @Override
    public void setKilled(boolean killed) {
        this.killed = killed;
    }

    @Override
    public Boolean isErrorAcknowledged() {
        return errorAcknowledged;
    }

    @Override
    public void setErrorAcknowledged(Boolean errorAcknowledged) {
        this.errorAcknowledged = errorAcknowledged;
    }

    @Override
    public String getErrorAcknowledgedMessage() {
        return errorAcknowledgedMessage;
    }

    @Override
    public void setErrorAcknowledgedMessage(String errorAcknowledgedMessage) {
        this.errorAcknowledgedMessage = errorAcknowledgedMessage;
    }

    @Override
    public String getErrorAcknowledgmentTicketId() {
        return errorAcknowledgmentTicketId;
    }

    @Override
    public void setErrorAcknowledgmentTicketId(String errorAcknowledgmentTicketId) {
        this.errorAcknowledgmentTicketId = errorAcknowledgmentTicketId;
    }

    @Override
    public String getErrorAcknowledgeUser() {
        return errorAcknowledgeUser;
    }

    @Override
    public void setErrorAcknowledgeUser(String errorAcknowledgeUser) {
        this.errorAcknowledgeUser = errorAcknowledgeUser;
    }

    @Override
    public long getErrorAcknowledgeTimestamp() {
        return errorAcknowledgeTimestamp;
    }

    @Override
    public void setErrorAcknowledgeTimestamp(long errorAcknowledgeTimestamp) {
        this.errorAcknowledgeTimestamp = errorAcknowledgeTimestamp;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalEventDrivenJobInstance)) return false;
        if (!super.equals(o)) return false;
        InternalEventDrivenJobInstance that = (InternalEventDrivenJobInstance) o;
        return Objects.equals(super.jobName, that.getJobName())
            && Objects.equals(super.contextName, that.getContextName())
            && Objects.equals(super.getChildContextName(), that.getChildContextName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.jobName, super.contextName, super.getChildContextName());
    }
}
