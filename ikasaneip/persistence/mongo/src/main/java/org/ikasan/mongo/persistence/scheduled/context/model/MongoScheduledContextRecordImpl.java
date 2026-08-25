package org.ikasan.mongo.persistence.scheduled.context.model;

import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * MongoDB implementation of ScheduledContextRecord.
 *
 * This entity stores the ContextTemplate as a JSON string in MongoDB, providing:
 * - Efficient storage of complex nested objects
 * - Indexing capabilities for key fields
 * - Flexible schema for JSON data
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoScheduledContextRecordImpl implements ScheduledContextRecord {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextRecordImpl.class);
    private static final JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Indexed(unique = true)
    @Field(EntityFields.MODULE_NAME)
    private String contextName;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String context;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Indexed
    @Field(EntityFields.DISABLED)
    private boolean disabled = false;

    @Field(EntityFields.QUARTZ_SCHEDULED_JOBS_DISABLED)
    private boolean isQuartzScheduleDrivenJobsDisabledForContext = false;

    @Indexed
    @Field(EntityFields.EXPIRY)
    private long expiry;

    private transient ContextTemplate contextTemplate;

    /**
     * Default constructor for MongoDB
     */
    public MongoScheduledContextRecordImpl() {
    }

    /**
     * Constructor with context name
     */
    public MongoScheduledContextRecordImpl(String contextName) {
        this.contextName = contextName;
        this.id = contextName;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
    public ContextTemplate getContext() {
        if (contextTemplate == null && this.context != null) {
            try {
                contextTemplate = objectMapper.readValue(context, ContextTemplate.class);
            } catch (JacksonException e) {
                logger.error("Failed to deserialize ContextTemplate from JSON", e);
                throw new RuntimeException("Failed to deserialize ContextTemplate", e);
            }
        }
        return contextTemplate;
    }

    @Override
    public void setContext(ContextTemplate ct) {
        this.contextTemplate = ct;
        if (context != null) {
            try {
                this.context = objectMapper.writeValueAsString(context);
                // Update denormalized fields for querying
                this.disabled = ct.isDisabled();
                this.isQuartzScheduleDrivenJobsDisabledForContext
                    = ct.isQuartzScheduleDrivenJobsDisabledForContext();
            } catch (JacksonException e) {
                logger.error("Failed to serialize ContextTemplate to JSON", e);
                throw new RuntimeException("Failed to serialize ContextTemplate", e);
            }
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
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean isQuartzScheduleDrivenJobsDisabledForContext() {
        return isQuartzScheduleDrivenJobsDisabledForContext;
    }

    public void setQuartzScheduleDrivenJobsDisabledForContext(boolean quartzScheduleDrivenJobsDisabledForContext) {
        isQuartzScheduleDrivenJobsDisabledForContext = quartzScheduleDrivenJobsDisabledForContext;
    }

    public String getContextTemplateJson() {
        return context;
    }

    public void setContextTemplateJson(String contextTemplateJson) {
        this.context = contextTemplateJson;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    @Override
    public String toString() {
        return "MongoScheduledContextRecordImpl{" +
            "id='" + id + '\'' +
            ", contextName='" + contextName + '\'' +
            ", timestamp=" + timestamp +
            ", modifiedTimestamp=" + modifiedTimestamp +
            ", modifiedBy='" + modifiedBy + '\'' +
            ", disabled=" + disabled +
            ", quartzScheduledJobsDisabledForContext=" + isQuartzScheduleDrivenJobsDisabledForContext +
            '}';
    }
}
