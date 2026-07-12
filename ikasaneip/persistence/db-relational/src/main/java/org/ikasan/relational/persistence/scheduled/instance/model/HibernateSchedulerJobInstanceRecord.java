package org.ikasan.relational.persistence.scheduled.instance.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hibernate/PostgreSQL implementation of SchedulerJobInstanceRecord.
 *
 * Stores the SchedulerJobInstance as JSONB in PostgreSQL for efficient storage.
 * This record tracks individual job instances within a context instance execution.
 */
@Entity
@Table(name = "scheduler_job_instance",
    indexes = {
        @Index(name = "idx_sji_job_name", columnList = "job_name"),
        @Index(name = "idx_sji_context_name", columnList = "context_name"),
        @Index(name = "idx_sji_context_instance_id", columnList = "context_instance_id"),
        @Index(name = "idx_sji_child_context_name", columnList = "child_context_name"),
        @Index(name = "idx_sji_status", columnList = "status"),
        @Index(name = "idx_sji_type", columnList = "type"),
        @Index(name = "idx_sji_timestamp", columnList = "timestamp"),
        @Index(name = "idx_sji_start_time", columnList = "start_time"),
        @Index(name = "idx_sji_end_time", columnList = "end_time")
    })
public class HibernateSchedulerJobInstanceRecord implements SchedulerJobInstanceRecord {

    private static final Logger logger = LoggerFactory.getLogger(HibernateSchedulerJobInstanceRecord.class);
    private static final JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 512)
    private String id;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "job_name", nullable = false, length = 512)
    private String jobName;

    @Column(name = "display_name", length = 512)
    private String displayName;

    @Column(name = "context_name", nullable = false, length = 512)
    private String contextName;

    @Column(name = "context_instance_id", nullable = false, length = 512)
    private String contextInstanceId;

    @Column(name = "child_context_name", length = 512)
    private String childContextName;

    @Column(name = "scheduler_job_instance", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String schedulerJobInstanceJson;

    private SchedulerJobInstance schedulerJobInstance;

    @Column(name = "status", nullable = false, length = 255)
    private String status;

    @Column(name = "target_residing_context_only", nullable = false)
    private boolean targetResidingContextOnly = false;

    @Column(name = "participates_in_lock", nullable = false)
    private boolean participatesInLock = false;

    @Column(name = "start_time")
    private long startTime;

    @Column(name = "end_time")
    private long endTime;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    private long modifiedTimestamp;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    @Column(name = "manually_submitted_by", length = 255)
    private String manuallySubmittedBy;

    /**
     * Default constructor for JPA
     */
    public HibernateSchedulerJobInstanceRecord() {
    }

    /**
     * Constructor with ID
     */
    public HibernateSchedulerJobInstanceRecord(String id) {
        this.id = id;
    }

    @PrePersist
    @PreUpdate
    protected void serializeSchedulerJobInstance() {
        if (schedulerJobInstance != null) {
            try {
                this.schedulerJobInstanceJson = objectMapper.writeValueAsString(schedulerJobInstance);
                // Extract type from the instance if not already set
                if (this.type == null) {
                    this.type = determineJobType(schedulerJobInstance);
                }
            } catch (JacksonException e) {
                logger.error("Failed to serialize SchedulerJobInstance to JSON", e);
                throw new RuntimeException("Failed to serialize SchedulerJobInstance to JSON", e);
            }
        }
    }

    @PostLoad
    protected void deserializeSchedulerJobInstance() {
        if (schedulerJobInstanceJson != null && !schedulerJobInstanceJson.isEmpty()) {
            try {
                this.schedulerJobInstance = objectMapper.readValue(schedulerJobInstanceJson, SchedulerJobInstance.class);
            } catch (JacksonException e) {
                logger.error("Failed to deserialize SchedulerJobInstance from JSON: {}", schedulerJobInstanceJson, e);
                throw new RuntimeException("Failed to deserialize SchedulerJobInstance from JSON", e);
            }
        }
    }

    /**
     * Determines the job type from the SchedulerJobInstance
     */
    private String determineJobType(SchedulerJobInstance instance) {
        // This would need to be implemented based on the concrete instance types
        // For now, return a default type
        return instance.getClass().getSimpleName();
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
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
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
    public SchedulerJobInstance getSchedulerJobInstance() {
        if (schedulerJobInstance == null && schedulerJobInstanceJson != null) {
            try {
                // Deserialize using object mapper type resolution
                schedulerJobInstance = objectMapper.readValue(schedulerJobInstanceJson, SchedulerJobInstance.class);
            } catch (JacksonException e) {
                logger.error("Failed to deserialize SchedulerJobInstance from JSON", e);
                throw new RuntimeException("Failed to deserialize SchedulerJobInstance from JSON", e);
            }
        }
        return schedulerJobInstance;
    }

    @Override
    public void setSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) {
        this.schedulerJobInstance = schedulerJobInstance;
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

    @Override
    public String getManuallySubmittedBy() {
        return manuallySubmittedBy;
    }

    @Override
    public void setManuallySubmittedBy(String manuallySubmittedBy) {
        this.manuallySubmittedBy = manuallySubmittedBy;
    }

    /**
     * Get the raw JSON string (for debugging or direct access)
     */
    public String getSchedulerJobInstanceJson() {
        return schedulerJobInstanceJson;
    }

    /**
     * Set the raw JSON string (mainly for testing or direct manipulation)
     */
    public void setSchedulerJobInstanceJson(String schedulerJobInstanceJson) {
        this.schedulerJobInstanceJson = schedulerJobInstanceJson;
        this.schedulerJobInstance = null; // Force re-deserialization
    }
}
