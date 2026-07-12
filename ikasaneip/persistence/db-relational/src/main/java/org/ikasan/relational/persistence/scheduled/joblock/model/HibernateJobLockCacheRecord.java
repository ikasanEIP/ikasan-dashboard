package org.ikasan.relational.persistence.scheduled.joblock.model;

 
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hibernate/PostgreSQL implementation of JobLockCacheRecord.
 *
 * This entity stores job lock cache data in PostgreSQL using JSONB for efficient
 * storage and querying of complex lock holder structures.
 *
 * The environment acts as the primary key, allowing only one lock cache per environment.
 */
@Entity
@Table(name = "job_lock_cache",
    indexes = {
        @Index(name = "idx_jlc_environment", columnList = "environment"),
        @Index(name = "idx_jlc_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateJobLockCacheRecord implements JobLockCacheRecord {

    private static final Logger logger = LoggerFactory.getLogger(HibernateJobLockCacheRecord.class);
    private static final JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
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
    public HibernateJobLockCacheRecord() {
        this.environment = DEFAULT_ENVIRONMENT;
    }

    /**
     * Constructor with environment
     *
     * @param environment the environment identifier
     */
    public HibernateJobLockCacheRecord(String environment) {
        this.environment = environment != null ? environment : DEFAULT_ENVIRONMENT;
    }

    /**
     * Serialize JobLockCacheData to JSON before persisting
     */
    protected void serializeJobLockCache() {
        if (jobLockCacheData != null) {
            try {
                this.jobLockCacheJson = objectMapper.writeValueAsString(jobLockCacheData);
            } catch (JacksonException e) {
                logger.error("Failed to serialize JobLockCacheData to JSON", e);
                throw new RuntimeException("Failed to serialize JobLockCacheData to JSON", e);
            }
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
        return this.environment;
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
        this.serializeJobLockCache();
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
