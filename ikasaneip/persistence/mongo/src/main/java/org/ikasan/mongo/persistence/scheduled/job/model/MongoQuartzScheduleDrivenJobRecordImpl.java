package org.ikasan.mongo.persistence.scheduled.job.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoQuartzScheduleDrivenJobRecordImpl implements QuartzScheduleDrivenJobRecord {

    private static final JsonMapper OBJECT_MAPPER = ObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field("agentName")
    private String agentName;

    @Field("jobName")
    private String jobName;

    @Field("displayName")
    private String displayName;

    @Field("contextName")
    private String contextName;

    @Field("quartzScheduleDrivenJob")
    private String quartzScheduleDrivenJob;

    @Field("timestamp")
    private long timestamp;

    @Field("modifiedTimestamp")
    private long modifiedTimestamp;

    @Field("modifiedBy")
    private String modifiedBy;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public QuartzScheduleDrivenJob getQuartzScheduleDrivenJob() {
        try {
            return OBJECT_MAPPER.readValue(quartzScheduleDrivenJob, QuartzScheduleDrivenJobImpl.class);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.quartzScheduleDrivenJob, e);
        }
    }

    public void setQuartzScheduleDrivenJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {
        try {
            this.quartzScheduleDrivenJob = OBJECT_MAPPER.writeValueAsString(quartzScheduleDrivenJob);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + quartzScheduleDrivenJob, e);
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
}
