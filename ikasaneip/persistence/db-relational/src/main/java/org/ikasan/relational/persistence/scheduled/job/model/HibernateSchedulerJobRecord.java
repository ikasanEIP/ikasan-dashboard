package org.ikasan.relational.persistence.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Hibernate/PostgreSQL base class for SchedulerJobRecord implementations.
 *
 * Uses Table per Class inheritance strategy where each concrete subclass has its own table
 * containing all fields including inherited ones. This provides good query performance and
 * allows polymorphic queries across all job types.
 *
 * Subclasses: HibernateBridgingJobRecord, HibernateContextStartJobRecord,
 * HibernateContextTerminalJobRecord, HibernateFileEventDrivenJobRecord,
 * HibernateGlobalEventJobRecord, HibernateInternalEventDrivenJobRecord,
 * HibernateQuartzScheduleDrivenJobRecord
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class HibernateSchedulerJobRecord implements SchedulerJobRecord {

    protected static final Logger logger = LoggerFactory.getLogger(HibernateSchedulerJobRecord.class);
    protected static final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 512)
    protected String id;

    @Column(name = "type", nullable = false, length = 255)
    protected String type;

    @Column(name = "agent_name", nullable = false, length = 255)
    protected String agentName;

    @Column(name = "job_name", nullable = false, length = 255)
    protected String jobName;

    @Column(name = "display_name", length = 512)
    protected String displayName;

    @Column(name = "context_name", nullable = false, length = 512)
    protected String contextName;

    @Column(name = "job", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    protected String jobJson;

    @Transient
    protected SchedulerJob job;

    @Column(name = "timestamp", nullable = false)
    protected long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    protected long modifiedTimestamp;

    @Column(name = "modified_by", length = 255)
    protected String modifiedBy;

    @Column(name = "held", nullable = false)
    protected boolean held = false;

    @Column(name = "skipped", nullable = false)
    protected boolean skipped = false;

    @Column(name = "target_residing_context_only", nullable = false)
    protected boolean targetResidingContextOnly = false;

    @Column(name = "participates_in_lock", nullable = false)
    protected boolean participatesInLock = false;

    /**
     * Default constructor for JPA
     */
    public HibernateSchedulerJobRecord() {
    }

    /**
     * Constructor with ID
     */
    public HibernateSchedulerJobRecord(String id) {
        this.id = id;
    }

    protected void serializeJob() {
        if (job != null) {
            try {
                this.jobJson = objectMapper.writeValueAsString(job);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize SchedulerJob to JSON", e);
                throw new RuntimeException("Failed to serialize SchedulerJob to JSON", e);
            }
        }

        // Ensure ID is set
        if (this.id == null || this.id.isEmpty()) {
            this.id = generateId();
        }
    }

    @PostLoad
    protected void deserializeJob() {
        if (jobJson != null && !jobJson.isEmpty()) {
            try {
                this.job = deserializeJobFromJson(jobJson);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize SchedulerJob from JSON: {}", jobJson, e);
                throw new RuntimeException("Failed to deserialize SchedulerJob from JSON", e);
            }
        }
    }

    /**
     * Subclasses must implement this to deserialize their specific job type from JSON
     */
    protected abstract SchedulerJob deserializeJobFromJson(String json) throws JsonProcessingException;

    /**
     * Generate ID from job name and context name
     */
    protected String generateId() {
        if (jobName != null && contextName != null) {
            return jobName + "_" + contextName;
        }
        return null;
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
        if (job == null && jobJson != null) {
            try {
                job = deserializeJobFromJson(jobJson);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize SchedulerJob from JSON", e);
                throw new RuntimeException("Failed to deserialize SchedulerJob from JSON", e);
            }
        }
        return job;
    }

    public void setJob(SchedulerJob job) {
        this.job = job;
        this.serializeJob();
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

    public void setModifiedTimestamp(long timestamp) {
        this.modifiedTimestamp = timestamp;
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

    /**
     * Get the raw JSON string (for debugging or direct access)
     */
    public String getJobJson() {
        return jobJson;
    }

    /**
     * Set the raw JSON string (mainly for testing or direct manipulation)
     */
    public void setJobJson(String jobJson) {
        this.jobJson = jobJson;
        this.job = null; // Force re-deserialization
    }
}
