package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;

import java.util.HashMap;
import java.util.Map;

public class SolrSchedulerJobInstanceSearchFilterImpl implements SchedulerJobInstanceSearchFilter {

    public static Map<String, String> JOB_TYPE_MAPPINGS = new HashMap<>();
    public static Map<String, String> JOB_TYPE_MAPPINGS_INVERTED = new HashMap<>();

    static {
        JOB_TYPE_MAPPINGS.put("Scheduled Job", "quartzScheduleDrivenJobInstance");
        JOB_TYPE_MAPPINGS.put("File Watcher Job", "fileEventDrivenJobInstance");
        JOB_TYPE_MAPPINGS.put("Command Execution Job", "internalEventDrivenJobInstance");

        JOB_TYPE_MAPPINGS_INVERTED.put("quartzScheduleDrivenJobInstance", "Scheduled Job");
        JOB_TYPE_MAPPINGS_INVERTED.put("fileEventDrivenJobInstance", "File Watcher Job");
        JOB_TYPE_MAPPINGS_INVERTED.put("internalEventDrivenJobInstance", "Command Execution Job");
    }

    private String jobName;
    private String jobType;
    private String contextName;
    private String contextInstanceId;
    private String childContextName;
    private String status;

    private Boolean targetResidingContextOnly = null;
    private Boolean participatesInLock = null;

    private long startTimeWindowStart;
    private long startTimeWindowEnd;
    private long endTimeWindowStart;
    private long endTimeWindowEnd;

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
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
