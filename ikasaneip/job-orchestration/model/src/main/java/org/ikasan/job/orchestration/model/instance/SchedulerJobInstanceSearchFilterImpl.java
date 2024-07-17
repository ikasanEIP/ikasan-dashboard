package org.ikasan.job.orchestration.model.instance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;

public class SchedulerJobInstanceSearchFilterImpl implements SchedulerJobInstanceSearchFilter {
    private String jobName;
    private boolean includeStartAndTerminalJobsInSearchResults = true;
    private String displayNameFilter = null;
    private String jobType;
    private String contextName;
    private String contextInstanceId;
    private String childContextName;
    private String status;

    private long startTimeWindowStart;
    private long startTimeWindowEnd;
    private long endTimeWindowStart;
    private long endTimeWindowEnd;

    private Boolean targetResidingContextOnly = null;
    private Boolean participatesInLock = null;

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public boolean includeStartAndTerminalJobsInSearchResults() {
        return this.includeStartAndTerminalJobsInSearchResults;
    }

    @Override
    public void setIncludeStartAndTerminalJobsInSearchResults(boolean includeStartAndTerminalJobsInSearchResults) {
        this.includeStartAndTerminalJobsInSearchResults = includeStartAndTerminalJobsInSearchResults;
    }

    @Override
    public String getDisplayNameFilter() {
        return displayNameFilter;
    }

    @Override
    public void setDisplayNameFilter(String displayNameFilter) {
        this.displayNameFilter = displayNameFilter;
    }

    @Override
    public String getJobType() {
        return jobType;
    }

    @Override
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getContextInstanceId() {
        return this.contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    @Override
    public String getChildContextName() {
        return childContextName;
    }

    @Override
    public void setChildContextName(String childContextName) {
        this.childContextName = childContextName;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public Boolean isTargetResidingContextOnly() {
        return targetResidingContextOnly;
    }

    @Override
    public void setTargetResidingContextOnly(Boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    @Override
    public Boolean isParticipatesInLock() {
        return participatesInLock;
    }

    @Override
    public void setParticipatesInLock(Boolean participatesInLock) {
        this.participatesInLock = participatesInLock;
    }

    @Override
    public long getStartTimeWindowStart() {
        return startTimeWindowStart;
    }

    @Override
    public void setStartTimeWindowStart(long startTimeWindowStart) {
        this.startTimeWindowStart = startTimeWindowStart;
    }

    @Override
    public long getStartTimeWindowEnd() {
        return startTimeWindowEnd;
    }

    @Override
    public void setStartTimeWindowEnd(long startTimeWindowEnd) {
        this.startTimeWindowEnd = startTimeWindowEnd;
    }

    @Override
    public long getEndTimeWindowStart() {
        return endTimeWindowStart;
    }

    @Override
    public void setEndTimeWindowStart(long endTimeWindowStart) {
        this.endTimeWindowStart = endTimeWindowStart;
    }

    @Override
    public long getEndTimeWindowEnd() {
        return endTimeWindowEnd;
    }

    @Override
    public void setEndTimeWindowEnd(long endTimeWindowEnd) {
        this.endTimeWindowEnd = endTimeWindowEnd;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
