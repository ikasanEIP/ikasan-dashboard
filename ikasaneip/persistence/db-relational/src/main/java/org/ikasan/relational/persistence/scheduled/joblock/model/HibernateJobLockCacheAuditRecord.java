package org.ikasan.relational.persistence.scheduled.joblock.model;

 
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Hibernate/PostgreSQL implementation of JobLockCacheAuditRecord.
 *
 * This entity stores audit records of job lock cache snapshots in PostgreSQL using JSONB
 * for efficient storage and querying. Each audit record represents a point-in-time snapshot
 * of the job lock cache state.
 *
 * Unlike JobLockCacheRecord which uses environment as PK, audit records use auto-generated UUIDs
 * to allow multiple historical snapshots.
 */
@Entity
@Table(name = "job_lock_cache_audit",
    indexes = {
        @Index(name = "idx_jlca_id", columnList = "id"),
        @Index(name = "idx_jlca_environment", columnList = "environment"),
        @Index(name = "idx_jlca_timestamp", columnList = "timestamp"),
        @Index(name = "idx_jlca_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateJobLockCacheAuditRecord implements JobLockCacheAuditRecord {

    private static final Logger logger = LoggerFactory.getLogger(HibernateJobLockCacheAuditRecord.class);
    private static final JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "environment", nullable = false, length = 255)
    private String environment = DEFAULT_ENVIRONMENT;

    @Column(name = "job_lock_cache_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String jobLockCacheJson;

    @Transient
    private JobLockCacheData jobLockCacheData;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    private long modifiedTimestamp;

    /**
     * Default constructor for JPA
     */
    public HibernateJobLockCacheAuditRecord() {
        this.id = UUID.randomUUID().toString();
        this.environment = DEFAULT_ENVIRONMENT;
    }

    /**
     * Constructor with environment
     *
     * @param environment the environment identifier
     */
    public HibernateJobLockCacheAuditRecord(String environment) {
        this.id = UUID.randomUUID().toString();
        this.environment = environment != null ? environment : DEFAULT_ENVIRONMENT;
    }

    /**
     * Constructor with ID and environment (mainly for testing)
     *
     * @param id          the unique identifier
     * @param environment the environment identifier
     */
    public HibernateJobLockCacheAuditRecord(String id, String environment) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.environment = environment != null ? environment : DEFAULT_ENVIRONMENT;
    }

    /**
     * Serialize JobLockCacheData to JSON before persisting
     */
    @PrePersist
    @PreUpdate
    protected void serializeJobLockCache() {
        if (jobLockCacheData != null) {
            try {
                this.jobLockCacheJson = objectMapper.writeValueAsString(jobLockCacheData);
            } catch (JacksonException e) {
                logger.error("Failed to serialize JobLockCacheData to JSON", e);
                throw new RuntimeException("Failed to serialize JobLockCacheData to JSON", e);
            }
        }

        // Ensure ID is set
        if (this.id == null || this.id.isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }

        // Update modified timestamp
        this.modifiedTimestamp = System.currentTimeMillis();

        // Set initial timestamp if not set
        if (this.timestamp == 0) {
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Deserialize JobLockCacheData from JSON after loading
     */
    @PostLoad
    protected void deserializeJobLockCache() {
        if (jobLockCacheJson != null && !jobLockCacheJson.isEmpty()) {
            try {
                this.jobLockCacheData = objectMapper.readValue(jobLockCacheJson, HibernateJobLockCacheData.class);
            } catch (JacksonException e) {
                logger.error("Failed to deserialize JobLockCacheData from JSON: {}", jobLockCacheJson, e);
                throw new RuntimeException("Failed to deserialize JobLockCacheData from JSON", e);
            }
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getEnvironment() {
        return this.environment;
    }

    @Override
    public void setEnvironment(String environment) {
        this.environment = environment != null ? environment : DEFAULT_ENVIRONMENT;
    }

    @Override
    public void setJobLockCache(JobLockCacheData jobLockCache) {
        this.jobLockCacheData = jobLockCache;
    }

    @Override
    public JobLockCacheData getJobLockCache() {
        if (jobLockCacheData == null && jobLockCacheJson != null) {
            try {
                jobLockCacheData = objectMapper.readValue(jobLockCacheJson, HibernateJobLockCacheData.class);
            } catch (JacksonException e) {
                logger.error("Failed to deserialize JobLockCacheData from JSON", e);
                throw new RuntimeException("Failed to deserialize JobLockCacheData from JSON", e);
            }
        }
        return jobLockCacheData;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return this.modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    /**
     * Get the raw JSON string (for debugging or direct access)
     */
    public String getJobLockCacheJson() {
        return jobLockCacheJson;
    }

    /**
     * Set the raw JSON string (mainly for testing or direct manipulation)
     */
    public void setJobLockCacheJson(String jobLockCacheJson) {
        this.jobLockCacheJson = jobLockCacheJson;
        this.jobLockCacheData = null; // Force re-deserialization
    }
}
