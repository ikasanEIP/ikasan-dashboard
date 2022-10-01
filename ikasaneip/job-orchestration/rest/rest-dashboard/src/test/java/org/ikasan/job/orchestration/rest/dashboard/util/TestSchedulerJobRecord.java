package org.ikasan.job.orchestration.rest.dashboard.util;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;

/**
 * Test class to mock records required to test the ContextReportControl Rest Api
 * Requires to insert correct type from JobConstants and jobs of type ScheduleJob
 */
public class TestSchedulerJobRecord implements SchedulerJobRecord {

    private String id;

    private String type;

    private String agentName;

    private String jobName;

    private String contextName;

    private SchedulerJob job;

    private long timestamp;

    private long modifiedTimestamp;

    private String modifiedBy;

    private boolean held;

    private boolean skipped;

    private boolean targetResidingContextOnly;

    private boolean participatesInLock;

    @Override
    public SchedulerJob getJob() {
        return job;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getAgentName() {
        return agentName;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public boolean isHeld() {
        return held;
    }

    @Override
    public boolean isSkipped() {
        return skipped;
    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return targetResidingContextOnly;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public void setJob(SchedulerJob job) {
        this.job = job;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean isParticipatesInLock() {
        return this.participatesInLock;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    public void setParticipatesInLock(boolean participatesInLock) {
        this.participatesInLock = participatesInLock;
    }
}