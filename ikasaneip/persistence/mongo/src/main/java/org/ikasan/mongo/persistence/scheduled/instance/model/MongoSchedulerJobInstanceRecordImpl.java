package org.ikasan.mongo.persistence.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

/**
 * MongoDB implementation of SchedulerJobInstanceRecord.
 *
 * This entity stores the SchedulerJobInstance as a JSON string in MongoDB, providing:
 * - Efficient storage of complex nested job instance objects
 * - Indexing capabilities for key fields (contextInstanceId, contextName, status, etc.)
 * - Flexible schema for polymorphic job instance types
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoSchedulerJobInstanceRecordImpl implements SchedulerJobInstanceRecord {

    private static final Logger logger = LoggerFactory.getLogger(MongoSchedulerJobInstanceRecordImpl.class);
    private static final JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
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

    @Indexed
    @Field(EntityFields.EXPIRY)
    private long expiry;

    /**
     * Default constructor for MongoDB
     */
    public MongoSchedulerJobInstanceRecordImpl() {
    }

    /**
     * Constructor with context instance id
     */
    public MongoSchedulerJobInstanceRecordImpl(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
        this.timestamp = System.currentTimeMillis();
        this.modifiedTimestamp = System.currentTimeMillis();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
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
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    @Override
    public SchedulerJobInstance getSchedulerJobInstance() {
        try {
            SchedulerJobInstance instance;
            if(this.type != null && this.type.equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) {
                instance = objectMapper.readValue(this.schedulerJobInstance, FileEventDrivenJobInstance.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                instance = objectMapper.readValue(this.schedulerJobInstance, QuartzScheduleDrivenJobInstance.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, InternalEventDrivenJobInstance.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, GlobalEventJobInstance.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.CONTEXT_START_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, ContextStartJobInstance.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, ContextTerminalJobInstance.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, LocalEventJobInstance.class);
            }
            else if(this.type != null && this.type.equals(JobConstants.BRIDGING_JOB_INSTANCE)) {
                instance =  objectMapper.readValue(this.schedulerJobInstance, BridgingJobInstance.class);
            }
            else {
                instance = objectMapper.readValue(this.schedulerJobInstance, SchedulerJobInstance.class);
            }

            if(instance.getStatus().equals(InstanceStatus.SKIPPED)) {
                instance.setSkip(true);
            }
            else {
                instance.setSkip(false);
            }

            return instance;
        }
        catch (JacksonException e) {
            // todo make better exception
            throw new RuntimeException("Could not convert string to entity: " + this.schedulerJobInstance, e);
        }
    }

    @Override
    public void setSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) {
        if (schedulerJobInstance != null) {
            try {
                this.schedulerJobInstance = objectMapper.writeValueAsString(schedulerJobInstance);
            } catch (JacksonException e) {
                logger.error("Failed to serialize SchedulerJobInstance to JSON", e);
                throw new RuntimeException("Failed to serialize SchedulerJobInstance", e);
            }
        }
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return targetResidingContextOnly;
    }

    @Override
    public void setParticipatesInLock(boolean participatesInLock) {
        this.participatesInLock = participatesInLock;
    }

    @Override
    public boolean isParticipatesInLock() {
        return participatesInLock;
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
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long timestamp) {
        this.modifiedTimestamp = timestamp;
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
    public String getManuallySubmittedBy() {
        return manuallySubmittedBy;
    }

    @Override
    public void setManuallySubmittedBy(String manuallySubmittedBy) {
        this.manuallySubmittedBy = manuallySubmittedBy;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchedulerJobInstanceRecord that = (MongoSchedulerJobInstanceRecordImpl) o;
        return Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
