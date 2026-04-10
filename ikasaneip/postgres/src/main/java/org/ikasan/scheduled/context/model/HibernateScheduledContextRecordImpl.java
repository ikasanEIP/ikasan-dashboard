package org.ikasan.scheduled.context.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextRecord using JSONB for ContextTemplate storage.
 *
 * This entity stores the ContextTemplate as a JSONB column in PostgreSQL, which provides:
 * - Efficient storage of complex nested objects
 * - Indexing capabilities for JSON fields
 * - Native JSON query support
 */
@Entity
@Table(name = "scheduled_context_record",
    indexes = {
        @Index(name = "idx_context_name", columnList = "context_name"),
        @Index(name = "idx_modified_timestamp", columnList = "modified_timestamp"),
        @Index(name = "idx_disabled", columnList = "disabled")
    })
public class HibernateScheduledContextRecordImpl implements ScheduledContextRecord {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextRecordImpl.class);
    private static final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 512)
    private String id;

    @Column(name = "context_name", nullable = false, unique = true, length = 512)
    private String contextName;

    /**
     * Store ContextTemplate as JSONB in PostgreSQL.
     * JSONB provides better performance for querying and indexing compared to plain JSON.
     */
    @Column(name = "context_template", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String contextTemplateJson;

    private ContextTemplate contextTemplate;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    private long modifiedTimestamp;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    @Column(name = "disabled", nullable = false)
    private boolean disabled;

    @Column(name = "quartz_scheduled_jobs_disabled", nullable = false)
    private boolean quartzScheduledJobsDisabledForContext;

    /**
     * Default constructor for Hibernate
     */
    public HibernateScheduledContextRecordImpl() {
    }

    /**
     * Constructor with context name
     */
    public HibernateScheduledContextRecordImpl(String contextName) {
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
            // Update denormalized fields for querying
            this.disabled = context.isDisabled();
            this.quartzScheduledJobsDisabledForContext = context.isQuartzScheduleDrivenJobsDisabledForContext();
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

    /**
     * Pre-persist hook to serialize ContextTemplate to JSON before saving
     */
    @PrePersist
    @PreUpdate
    protected void serializeContextTemplate() {
        if (contextTemplate != null) {
            try {
                this.contextTemplateJson = objectMapper.writeValueAsString(contextTemplate);
                // Update denormalized fields
                this.disabled = contextTemplate.isDisabled();
                this.quartzScheduledJobsDisabledForContext = contextTemplate.isQuartzScheduleDrivenJobsDisabledForContext();
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize ContextTemplate to JSON", e);
                throw new RuntimeException("Failed to serialize ContextTemplate", e);
            }
        }

        // Update modified timestamp
        this.modifiedTimestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "HibernateScheduledContextRecordImpl{" +
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
