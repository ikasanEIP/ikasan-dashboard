package org.ikasan.mongo.persistence.scheduled.job.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * MongoDB implementation of FileEventDrivenJobRecord.
 * Stores the FileEventDrivenJob as a JSON string in MongoDB.
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoFileEventDrivenJobRecordImpl implements FileEventDrivenJobRecord {

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

    @Field("fileEventDrivenJob")
    private String fileEventDrivenJob;

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
    public FileEventDrivenJob getFileEventDrivenJob() {
        try {
            return OBJECT_MAPPER.readValue(fileEventDrivenJob, FileEventDrivenJobImpl.class);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.fileEventDrivenJob, e);
        }
    }

    @Override
    public void setFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob) {
        try {
            this.fileEventDrivenJob = OBJECT_MAPPER.writeValueAsString(fileEventDrivenJob);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + fileEventDrivenJob, e);
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
