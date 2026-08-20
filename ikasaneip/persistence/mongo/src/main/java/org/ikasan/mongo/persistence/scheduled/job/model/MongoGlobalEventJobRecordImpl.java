package org.ikasan.mongo.persistence.scheduled.job.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJobRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * MongoDB implementation of GlobalEventJobRecord.
 * Stores the GlobalEventJob as a JSON string in MongoDB.
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoGlobalEventJobRecordImpl implements GlobalEventJobRecord {

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

    @Field("globalEventJob")
    private String globalEventJob;

    @Field("timestamp")
    private long timestamp = -1;

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
    public GlobalEventJob getGlobalEventJob() {
        try {
            return OBJECT_MAPPER.readValue(globalEventJob, GlobalEventJobImpl.class);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.globalEventJob, e);
        }
    }

    @Override
    public void setGlobalEventJob(GlobalEventJob globalEventJob) {
        try {
            this.globalEventJob = OBJECT_MAPPER.writeValueAsString(globalEventJob);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + globalEventJob, e);
        }
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
