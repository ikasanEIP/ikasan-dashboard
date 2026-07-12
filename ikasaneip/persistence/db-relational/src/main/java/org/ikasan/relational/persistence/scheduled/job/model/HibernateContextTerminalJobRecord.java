package org.ikasan.relational.persistence.scheduled.job.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;

/**
 * Hibernate/PostgreSQL implementation of ContextTerminalJobRecord.
 *
 * Stores the ContextTerminalJob as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks context terminal jobs that finalize context execution.
 *
 * Uses Table per Class inheritance - has its own table with all fields.
 */
@Entity
@Table(name = "context_terminal_job",
    indexes = {
        @Index(name = "idx_ctj_context_name", columnList = "context_name"),
        @Index(name = "idx_ctj_job_name", columnList = "job_name"),
        @Index(name = "idx_ctj_agent_name", columnList = "agent_name"),
        @Index(name = "idx_ctj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_ctj_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateContextTerminalJobRecord extends HibernateSchedulerJobRecord implements ContextTerminalJobRecord {

    /**
     * Default constructor for JPA
     */
    public HibernateContextTerminalJobRecord() {
        super();
        this.type = JobConstants.CONTEXT_TERMINAL_JOB;
    }

    /**
     * Constructor with ID
     */
    public HibernateContextTerminalJobRecord(String id) {
        super(id);
        this.type = JobConstants.CONTEXT_TERMINAL_JOB;
    }

    @Override
    protected ContextTerminalJob deserializeJobFromJson(String json)  {
        return objectMapper.readValue(json, ContextTerminalJob.class);
    }

    @Override
    public ContextTerminalJob getContextTerminalJob() {
        return (ContextTerminalJob) getJob();
    }

    @Override
    public void setContextTerminalJob(ContextTerminalJob contextTerminalJob) {
        setJob(contextTerminalJob);
        this.type = JobConstants.CONTEXT_TERMINAL_JOB;
    }
}
