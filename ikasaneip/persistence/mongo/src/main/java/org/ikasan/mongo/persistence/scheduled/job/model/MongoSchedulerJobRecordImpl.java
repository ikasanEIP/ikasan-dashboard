package org.ikasan.mongo.persistence.scheduled.job.model;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoSchedulerJobRecordImpl implements SchedulerJobRecord {

    private static final JsonMapper OBJECT_MAPPER = ObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field("type")
    private String type;

    @Field("agentName")
    private String agentName;

    @Field("jobName")
    private String jobName;

    @Field("displayName")
    private String displayName;

    @Field("contextName")
    private String contextName;

    @Field("job")
    private String job;

    @Field("timestamp")
    private long timestamp;

    @Field("modifiedTimestamp")
    private long modifiedTimestamp;

    @Field("modifiedBy")
    private String modifiedBy;

    @Field("held")
    private boolean held;

    @Field("skipped")
    private boolean skipped;

    @Field("targetResidingContextOnly")
    private boolean targetResidingContextOnly;

    @Field("participatesInLock")
    private boolean participatesInLock;

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
    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public SchedulerJob getJob() {
        try {
            switch (this.type) {
                case JobConstants.FILE_EVENT_DRIVEN_JOB:
                    return OBJECT_MAPPER.readValue(this.job, FileEventDrivenJobImpl.class);
                case JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB:
                    return OBJECT_MAPPER.readValue(this.job, QuartzScheduleDrivenJobImpl.class);
                case JobConstants.INTERNAL_EVENT_DRIVEN_JOB:
                case JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE:
                    return OBJECT_MAPPER.readValue(this.job, InternalEventDrivenJobImpl.class);
                case JobConstants.GLOBAL_EVENT_JOB:
                    return OBJECT_MAPPER.readValue(this.job, GlobalEventJobImpl.class);
                case JobConstants.CONTEXT_START_JOB:
                    return OBJECT_MAPPER.readValue(this.job, ContextStartJobImpl.class);
                case JobConstants.CONTEXT_TERMINAL_JOB:
                    return OBJECT_MAPPER.readValue(this.job, ContextTerminalJobImpl.class);
                default:
                    throw new EntityConversionException("Could not resolve job type: " + this.type);
            }
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity: " + this.job, e);
        }
    }

    public void setJob(SchedulerJob schedulerJob) {
        try {
            this.job = OBJECT_MAPPER.writeValueAsString(schedulerJob);
            // Set the type based on the job class
            if (schedulerJob instanceof FileEventDrivenJobImpl) {
                this.type = JobConstants.FILE_EVENT_DRIVEN_JOB;
            } else if (schedulerJob instanceof QuartzScheduleDrivenJobImpl) {
                this.type = JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB;
            } else if (schedulerJob instanceof InternalEventDrivenJobImpl) {
                // Check if it's a template or regular job
                // For now, default to INTERNAL_EVENT_DRIVEN_JOB
                this.type = JobConstants.INTERNAL_EVENT_DRIVEN_JOB;
            } else if (schedulerJob instanceof GlobalEventJobImpl) {
                this.type = JobConstants.GLOBAL_EVENT_JOB;
            } else if (schedulerJob instanceof ContextStartJobImpl) {
                this.type = JobConstants.CONTEXT_START_JOB;
            } else if (schedulerJob instanceof ContextTerminalJobImpl) {
                this.type = JobConstants.CONTEXT_TERMINAL_JOB;
            }
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + schedulerJob, e);
        }
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    @Override
    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return targetResidingContextOnly;
    }

    public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    @Override
    public boolean isParticipatesInLock() {
        return participatesInLock;
    }

    public void setParticipatesInLock(boolean participatesInLock) {
        this.participatesInLock = participatesInLock;
    }
}
