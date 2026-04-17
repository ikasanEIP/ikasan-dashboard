package org.ikasan.relational.persistence.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.*;
import org.ikasan.spec.scheduled.job.model.BridgingJob;
import org.ikasan.spec.scheduled.job.model.BridgingJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;

/**
 * Hibernate/PostgreSQL implementation of BridgingJobRecord.
 *
 * Stores the BridgingJob as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks bridging jobs that connect different contexts or job chains.
 *
 * Uses Table per Class inheritance - has its own table with all fields.
 */
@Entity
@Table(name = "bridging_job",
    indexes = {
        @Index(name = "idx_bj_context_name", columnList = "context_name"),
        @Index(name = "idx_bj_job_name", columnList = "job_name"),
        @Index(name = "idx_bj_agent_name", columnList = "agent_name"),
        @Index(name = "idx_bj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_bj_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateBridgingJobRecord extends HibernateSchedulerJobRecord implements BridgingJobRecord {

    /**
     * Default constructor for JPA
     */
    public HibernateBridgingJobRecord() {
        super();
        this.type = JobConstants.BRIDGING_JOB;
    }

    /**
     * Constructor with ID
     */
    public HibernateBridgingJobRecord(String id) {
        super(id);
        this.type = JobConstants.BRIDGING_JOB;
    }

    @Override
    protected BridgingJob deserializeJobFromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, BridgingJob.class);
    }

    @Override
    public BridgingJob getBridgingJob() {
        return (BridgingJob) getJob();
    }

    @Override
    public void setBridgingJob(BridgingJob bridgingJob) {
        setJob(bridgingJob);
        this.type = JobConstants.BRIDGING_JOB;
    }
}
