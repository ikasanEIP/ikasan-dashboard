package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

public class SolrSchedulerJobInstanceRecordImpl implements SchedulerJobInstanceRecord {
    private static final JsonMapper objectMapper  = ScheduledObjectMapperFactory.newInstance();

    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.MODULE_NAME)
    private String jobName;

    @Field(EntityFields.DISPLAY_NAME)
    private String displayName;

    @Field(EntityFields.FLOW_NAME)
    private String contextName;

    @Field(EntityFields.COMPONENT_NAME)
    private String contextInstanceId;

    @Field(EntityFields.CHILD_CONTEXT_NAME)
    private String childContextName;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String schedulerJobInstance;

    @Field(EntityFields.STATUS)
    private String status;

    @Field(EntityFields.TARGET_RESIDING_CONTEXT_ONLY)
    private boolean targetResidingContextOnly;

    @Field(EntityFields.PARTICIPATES_IN_LOCK)
    boolean participatesInLock;

    @Field(EntityFields.START_TIME)
    private long startTime;

    @Field(EntityFields.END_TIME)
    private long endTime;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Field(EntityFields.MANUALLY_SUBMITTED_BY)
    private String manuallySubmittedBy;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getType() {
        return type;
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
    public SchedulerJobInstance getSchedulerJobInstance() {
        try {
            SchedulerJobInstance instance;
            if(this.type != null && this.type.equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) {
                instance=  objectMapper.readValue(this.schedulerJobInstance, SolrFileEventDrivenJobInstanceImpl.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                instance = objectMapper.readValue(this.schedulerJobInstance, SolrQuartzScheduleDrivenJobInstanceImpl.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, SolrInternalEventDrivenJobInstanceImpl.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, SolrGlobalEventJobInstanceImpl.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.CONTEXT_START_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, SolrContextStartJobInstanceImpl.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, SolrContextTerminalJobInstanceImpl.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, SolrLocalEventJobInstanceImpl.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.BRIDGING_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, SolrBridgingJobInstanceImpl.class);
            }
            else {
                instance = objectMapper.readValue(this.schedulerJobInstance, SolrSchedulerJobInstanceImpl.class);
            }

            if(instance.equals(InstanceStatus.SKIPPED)) {
                instance.setSkip(true);
            }
            else {
                instance.setSkip(false);
            }

            return instance;
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.schedulerJobInstance, e);
        }
    }

    @Override
    public void setSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) {
        try {
            if(schedulerJobInstance instanceof FileEventDrivenJobInstance) {
                this.type = JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE;
            }
            else if(schedulerJobInstance instanceof QuartzScheduleDrivenJobInstance) {
                this.type = JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE;
            }
            else if(schedulerJobInstance instanceof InternalEventDrivenJobInstance) {
                this.type = JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE;
            }
            else if(schedulerJobInstance instanceof GlobalEventJobInstance) {
                this.type = JobConstants.GLOBAL_EVENT_JOB_INSTANCE;
            }
            else if(schedulerJobInstance instanceof ContextStartJobInstance) {
                this.type = JobConstants.CONTEXT_START_JOB_INSTANCE;
            }
            else if(schedulerJobInstance instanceof ContextTerminalJobInstance) {
                this.type = JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE;
            }
            else if(schedulerJobInstance instanceof LocalEventJobInstance) {
                this.type = JobConstants.LOCAL_EVENT_JOB_INSTANCE;
            }
            else if(schedulerJobInstance instanceof BridgingJobInstance) {
                this.type = JobConstants.BRIDGING_JOB_INSTANCE;
            }

            this.schedulerJobInstance = objectMapper.writeValueAsString(schedulerJobInstance);
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + schedulerJobInstance, e);
        }
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
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return this.modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return this.modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public String getManuallySubmittedBy() {
        return manuallySubmittedBy;
    }

    @Override
    public void setManuallySubmittedBy(String manuallySubmittedBy) {
        this.manuallySubmittedBy = manuallySubmittedBy;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolrSchedulerJobInstanceRecordImpl that = (SolrSchedulerJobInstanceRecordImpl) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
