package org.ikasan.relational.persistence.scheduled.instance.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregate;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextInstanceAuditAggregateRecord.
 *
 * Stores the ScheduledContextInstanceAuditAggregate as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks the audit aggregate information for a scheduled context instance execution.
 */
@Entity
@Table(name = "scheduled_context_instance_audit_aggregate",
    indexes = {
        @Index(name = "idx_sciaa_context_name", columnList = "context_name"),
        @Index(name = "idx_sciaa_context_instance_id", columnList = "context_instance_id"),
        @Index(name = "idx_sciaa_process_event_name", columnList = "scheduled_process_event_name"),
        @Index(name = "idx_sciaa_status", columnList = "status"),
        @Index(name = "idx_sciaa_timestamp", columnList = "timestamp"),
        @Index(name = "idx_sciaa_repeating_job", columnList = "repeating_job"),
        @Index(name = "idx_sciaa_job_type", columnList = "job_type")
    })
public class HibernateScheduledContextInstanceAuditAggregateRecord implements ScheduledContextInstanceAuditAggregateRecord {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextInstanceAuditAggregateRecord.class);
    private static final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 512)
    private String id;

    @Column(name = "context_name", nullable = false, length = 512)
    private String contextName;

    @Column(name = "context_instance_id", nullable = false, length = 512)
    private String contextInstanceId;

    @Column(name = "scheduled_process_event_name", length = 512)
    private String scheduledProcessEventName;

    @Column(name = "raised_events", columnDefinition = "text")
    private String raisedEvents;

    @Column(name = "audit_aggregate", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String auditAggregateJson;

    @Transient
    private ScheduledContextInstanceAuditAggregate scheduledContextInstanceAuditAggregate;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "repeating_job", nullable = false)
    private boolean repeatingJob = false;

    @Column(name = "job_type", length = 255)
    private String jobType;

    /**
     * Default constructor for JPA
     */
    public HibernateScheduledContextInstanceAuditAggregateRecord() {
    }

    /**
     * Constructor with ID
     */
    public HibernateScheduledContextInstanceAuditAggregateRecord(String id) {
        this.id = id;
    }

    @PrePersist
    @PreUpdate
    protected void serializeAuditAggregate() {
        if (scheduledContextInstanceAuditAggregate != null) {
            try {
                this.auditAggregateJson = objectMapper.writeValueAsString(scheduledContextInstanceAuditAggregate);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize ScheduledContextInstanceAuditAggregate to JSON", e);
                throw new RuntimeException("Failed to serialize ScheduledContextInstanceAuditAggregate to JSON", e);
            }
        }

        // Set timestamp if not already set
        if (this.timestamp == 0) {
            this.timestamp = System.currentTimeMillis();
        }
    }

    @PostLoad
    protected void deserializeAuditAggregate() {
        if (auditAggregateJson != null && !auditAggregateJson.isEmpty()) {
            try {
                this.scheduledContextInstanceAuditAggregate = objectMapper.readValue(
                    auditAggregateJson, ScheduledContextInstanceAuditAggregate.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize ScheduledContextInstanceAuditAggregate from JSON: {}",
                    auditAggregateJson, e);
                throw new RuntimeException("Failed to deserialize ScheduledContextInstanceAuditAggregate from JSON", e);
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
    }

    @Override
    public String getScheduledProcessEventName() {
        return scheduledProcessEventName;
    }

    @Override
    public void setScheduledProcessEventName(String scheduledProcessEventName) {
        this.scheduledProcessEventName = scheduledProcessEventName;
    }

    @Override
    public String getRaisedEvents() {
        return raisedEvents;
    }

    public void setRaisedEvents(String raisedEvents) {
        this.raisedEvents = raisedEvents;
    }

    @Override
    public ScheduledContextInstanceAuditAggregate getScheduledContextInstanceAuditAggregate() {
        if (scheduledContextInstanceAuditAggregate == null && auditAggregateJson != null) {
            try {
                scheduledContextInstanceAuditAggregate = objectMapper.readValue(
                    auditAggregateJson, ScheduledContextInstanceAuditAggregate.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize ScheduledContextInstanceAuditAggregate from JSON", e);
                throw new RuntimeException("Failed to deserialize ScheduledContextInstanceAuditAggregate from JSON", e);
            }
        }
        return scheduledContextInstanceAuditAggregate;
    }

    @Override
    public void setScheduledContextInstanceAuditAggregate(ScheduledContextInstanceAuditAggregate scheduledContextInstanceAudit) {
        this.scheduledContextInstanceAuditAggregate = scheduledContextInstanceAudit;
        // JSON will be serialized in PrePersist/PreUpdate
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
    public boolean isRepeatingJob() {
        return repeatingJob;
    }

    @Override
    public void setRepeatingJob(boolean repeatingJob) {
        this.repeatingJob = repeatingJob;
    }

    @Override
    public String getJobType() {
        return jobType;
    }

    @Override
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    /**
     * Get the raw JSON string (for debugging or direct access)
     */
    public String getAuditAggregateJson() {
        return auditAggregateJson;
    }

    /**
     * Set the raw JSON string (mainly for testing or direct manipulation)
     */
    public void setAuditAggregateJson(String auditAggregateJson) {
        this.auditAggregateJson = auditAggregateJson;
        this.scheduledContextInstanceAuditAggregate = null; // Force re-deserialization
    }
}
