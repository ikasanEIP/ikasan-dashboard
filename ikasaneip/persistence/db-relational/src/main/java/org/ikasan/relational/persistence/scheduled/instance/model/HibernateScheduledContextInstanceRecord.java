package org.ikasan.relational.persistence.scheduled.instance.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextInstanceRecord.
 *
 * Stores the ContextInstance as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks the runtime instance of a scheduled context execution.
 */
@Entity
@Table(name = "scheduled_context_instance",
    indexes = {
        @Index(name = "idx_sci_context_name", columnList = "context_name"),
        @Index(name = "idx_sci_context_instance_id", columnList = "context_instance_id"),
        @Index(name = "idx_sci_status", columnList = "status"),
        @Index(name = "idx_sci_timestamp", columnList = "timestamp"),
        @Index(name = "idx_sci_modified_timestamp", columnList = "modified_timestamp"),
        @Index(name = "idx_sci_start_time", columnList = "start_time"),
        @Index(name = "idx_sci_end_time", columnList = "end_time")
    })
public class HibernateScheduledContextInstanceRecord implements ScheduledContextInstanceRecord {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextInstanceRecord.class);
    private static final JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 512)
    private String id;

    @Column(name = "context_name", nullable = false, length = 512)
    private String contextName;

    @Column(name = "context_instance_id", nullable = false, length = 512)
    private String contextInstanceId;

    @Column(name = "context_instance", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String contextInstanceJson;

    private ContextInstance contextInstance;

    @Column(name = "status", nullable = false, length = 255)
    private String status;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    private long modifiedTimestamp;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    @Column(name = "start_time")
    private long startTime;

    @Column(name = "end_time")
    private long endTime;

    @Column(name = "contains_repeating_jobs", nullable = false)
    private boolean containsRepeatingJobs = false;

    /**
     * Default constructor for JPA
     */
    public HibernateScheduledContextInstanceRecord() {
    }

    /**
     * Constructor with ID
     */
    public HibernateScheduledContextInstanceRecord(String id) {
        this.id = id;
        this.contextInstanceId = id;
    }

    @PrePersist
    @PreUpdate
    protected void serializeContextInstance() {
        if (contextInstance != null) {
            try {
                this.contextInstanceJson = objectMapper.writeValueAsString(contextInstance);
            } catch (JacksonException e) {
                logger.error("Failed to serialize ContextInstance to JSON", e);
                throw new RuntimeException("Failed to serialize ContextInstance to JSON", e);
            }
        }

        // Ensure ID is set
        if (this.id == null || this.id.isEmpty()) {
            this.id = this.contextInstanceId;
        }
    }

    @PostLoad
    protected void deserializeContextInstance() {
        if (contextInstanceJson != null && !contextInstanceJson.isEmpty()) {
            try {
                this.contextInstance = objectMapper.readValue(contextInstanceJson, ContextInstance.class);
            } catch (JacksonException e) {
                logger.error("Failed to deserialize ContextInstance from JSON: {}", contextInstanceJson, e);
                throw new RuntimeException("Failed to deserialize ContextInstance from JSON", e);
            }
        }
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
    }

    @Override
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
        // Keep ID in sync
        if (this.id == null || this.id.isEmpty()) {
            this.id = contextInstanceId;
        }
    }

    @Override
    public ContextInstance getContextInstance() {
        if (contextInstance == null && contextInstanceJson != null) {
            try {
                // Deserialize from JSON using object mapper type resolution
                contextInstance = objectMapper.readValue(contextInstanceJson, ContextInstance.class);
            } catch (JacksonException e) {
                logger.error("Failed to deserialize ContextInstance from JSON", e);
                throw new RuntimeException("Failed to deserialize ContextInstance from JSON", e);
            }
        }
        return contextInstance;
    }

    @Override
    public void setContextInstance(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
        // JSON will be serialized in PrePersist/PreUpdate
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
    public boolean isContainsRepeatingJobs() {
        return containsRepeatingJobs;
    }

    @Override
    public void setContainsRepeatingJobs(boolean containsRepeatingJobs) {
        this.containsRepeatingJobs = containsRepeatingJobs;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * Get the raw JSON string (for debugging or direct access)
     */
    public String getContextInstanceJson() {
        return contextInstanceJson;
    }

    /**
     * Set the raw JSON string (mainly for testing or direct manipulation)
     */
    public void setContextInstanceJson(String contextInstanceJson) {
        this.contextInstanceJson = contextInstanceJson;
        this.contextInstance = null; // Force re-deserialization
    }
}
