package org.ikasan.relational.persistence.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.*;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;

/**
 * Hibernate/PostgreSQL implementation of GlobalEventJobRecord.
 *
 * Stores the GlobalEventJob as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks global event jobs that respond to system-wide events.
 *
 * Uses Table per Class inheritance - has its own table with all fields.
 */
@Entity
@Table(name = "global_event_job",
    indexes = {
        @Index(name = "idx_gej_context_name", columnList = "context_name"),
        @Index(name = "idx_gej_job_name", columnList = "job_name"),
        @Index(name = "idx_gej_agent_name", columnList = "agent_name"),
        @Index(name = "idx_gej_timestamp", columnList = "timestamp"),
        @Index(name = "idx_gej_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateGlobalEventJobRecord extends HibernateSchedulerJobRecord implements GlobalEventJobRecord {

    /**
     * Default constructor for JPA
     */
    public HibernateGlobalEventJobRecord() {
        super();
        this.type = JobConstants.GLOBAL_EVENT_JOB;
    }

    /**
     * Constructor with ID
     */
    public HibernateGlobalEventJobRecord(String id) {
        super(id);
        this.type = JobConstants.GLOBAL_EVENT_JOB;
    }

    @Override
    protected GlobalEventJob deserializeJobFromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, GlobalEventJob.class);
    }

    @Override
    public GlobalEventJob getGlobalEventJob() {
        return (GlobalEventJob) getJob();
    }

    @Override
    public void setGlobalEventJob(GlobalEventJob globalEventJob) {
        setJob(globalEventJob);
        this.type = JobConstants.GLOBAL_EVENT_JOB;
    }
}
