package org.ikasan.mongo.persistence.scheduled.context.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.mongo.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of ScheduledContextRecord.
 *
 * This entity stores the ContextTemplate as a JSON string in MongoDB, providing:
 * - Efficient storage of complex nested objects
 * - Indexing capabilities for key fields
 * - Flexible schema for JSON data
 */
@Document(collection = "scheduled_context_record")
public class MongoScheduledContextRecordImpl implements ScheduledContextRecord {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextRecordImpl.class);
    private static final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("context_name")
    private String contextName;

    /**
     * Store ContextTemplate as JSON string in MongoDB.
     */
    @Field("context_template")
    private String contextTemplateJson;

    private transient ContextTemplate contextTemplate;

    @Field("timestamp")
    private long timestamp;

    @Indexed
    @Field("modified_timestamp")
    private long modifiedTimestamp;

    @Field("modified_by")
    private String modifiedBy;

    @Indexed
    @Field("disabled")
    private boolean disabled;

    @Field("quartz_scheduled_jobs_disabled")
    private boolean quartzScheduledJobsDisabledForContext;

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

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
        this.id = contextName; // Keep ID in sync with context name
    }

    @Override
    public ContextTemplate getContext() {
        if (contextTemplate == null && contextTemplateJson != null) {
            try {
                contextTemplate = objectMapper.readValue(contextTemplateJson, ContextTemplate.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize ContextTemplate from JSON", e);
                throw new RuntimeException("Failed to deserialize ContextTemplate", e);
            }
        }
        return contextTemplate;
    }

    @Override
    public void setContext(ContextTemplate context) {
        this.contextTemplate = context;
        if (context != null) {
            try {
                this.contextTemplateJson = objectMapper.writeValueAsString(context);
                // Update denormalized fields for querying
                this.disabled = context.isDisabled();
                this.quartzScheduledJobsDisabledForContext = context.isQuartzScheduleDrivenJobsDisabledForContext();
            } catch (JsonProcessingException e) {
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

    @Override
    public boolean isQuartzScheduleDrivenJobsDisabledForContext() {
        return quartzScheduledJobsDisabledForContext;
    }

    public String getContextTemplateJson() {
        return contextTemplateJson;
    }

    public void setContextTemplateJson(String contextTemplateJson) {
        this.contextTemplateJson = contextTemplateJson;
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
            ", quartzScheduledJobsDisabledForContext=" + quartzScheduledJobsDisabledForContext +
            '}';
    }
}
