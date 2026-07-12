package org.ikasan.relational.persistence.scheduled.job.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.ContextStartJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;

/**
 * Hibernate/PostgreSQL implementation of ContextStartJobRecord.
 *
 * Stores the ContextStartJob as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks context start jobs that initialize or start a context.
 *
 * Uses Table per Class inheritance - has its own table with all fields.
 */
@Entity
@Table(name = "context_start_job",
    indexes = {
        @Index(name = "idx_csj_context_name", columnList = "context_name"),
        @Index(name = "idx_csj_job_name", columnList = "job_name"),
        @Index(name = "idx_csj_agent_name", columnList = "agent_name"),
        @Index(name = "idx_csj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_csj_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateContextStartJobRecord extends HibernateSchedulerJobRecord implements ContextStartJobRecord {

    /**
     * Default constructor for JPA
     */
    public HibernateContextStartJobRecord() {
        super();
        this.type = JobConstants.CONTEXT_START_JOB;
    }

    /**
     * Constructor with ID
     */
    public HibernateContextStartJobRecord(String id) {
        super(id);
        this.type = JobConstants.CONTEXT_START_JOB;
    }

    @Override
    protected ContextStartJob deserializeJobFromJson(String json)  {
        return objectMapper.readValue(json, ContextStartJob.class);
    }

    @Override
    public ContextStartJob getContextStartJob() {
        return (ContextStartJob) getJob();
    }

    @Override
    public void setContextStartJob(ContextStartJob contextStartJob) {
        setJob(contextStartJob);
        this.type = JobConstants.CONTEXT_START_JOB;
    }
}
