package org.ikasan.job.orchestration.model.job;

import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import tools.jackson.databind.json.JsonMapper;

public class InternalEventDrivenJobRecordImpl implements InternalEventDrivenJobRecord {

    private static final JsonMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();

    private String id;
    private String agentName;
    private String jobName;
    private String displayName;
    private String contextName;
    private String internalEventDrivenJob;
    private long timestamp = -1;
    private long modifiedTimestamp;
    private String modifiedBy;
    private boolean held;
    private boolean skipped;
    private boolean targetResidingContextOnly;
    boolean participatesInLock;

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public String getId() {
        return id;
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
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public InternalEventDrivenJob getInternalEventDrivenJob() {
        return objectMapper.readValue(internalEventDrivenJob, InternalEventDrivenJobImpl.class);
    }

    public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
        this.internalEventDrivenJob = objectMapper.writeValueAsString(internalEventDrivenJob);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean isHeld() {
        return held;
    }

    @Override
    public void setHeld(boolean held) {
        this.held = held;
    }

    @Override
    public boolean isSkipped() {
        return skipped;
    }

    @Override
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
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
}
