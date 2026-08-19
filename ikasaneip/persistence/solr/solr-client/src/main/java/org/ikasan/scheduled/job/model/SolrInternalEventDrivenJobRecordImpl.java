package org.ikasan.scheduled.job.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class  SolrInternalEventDrivenJobRecordImpl implements InternalEventDrivenJobRecord {

    private static final JsonMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.MODULE_NAME)
    private String agentName;

    @Field(EntityFields.FLOW_NAME)
    private String jobName;

    @Field(EntityFields.DISPLAY_NAME)
    private String displayName;

    @Field(EntityFields.COMPONENT_NAME)
    private String contextName;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String internalEventDrivenJob;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp = -1;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Field(EntityFields.HELD)
    private boolean held;

    @Field(EntityFields.SKIPPED)
    private boolean skipped;

    @Field(EntityFields.TARGET_RESIDING_CONTEXT_ONLY)
    private boolean targetResidingContextOnly;

    @Field(EntityFields.PARTICIPATES_IN_LOCK)
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
        try {
            return objectMapper.readValue(internalEventDrivenJob, SolrInternalEventDrivenJobImpl.class);
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.internalEventDrivenJob, e);
        }
    }

    public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
        try {
            this.internalEventDrivenJob = objectMapper.writeValueAsString(internalEventDrivenJob);
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + internalEventDrivenJob, e);
        }
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
