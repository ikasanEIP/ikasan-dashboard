package org.ikasan.relational.persistence.scheduled.job.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;

/**
 * Hibernate/PostgreSQL implementation of InternalEventDrivenJobRecord.
 *
 * Stores the InternalEventDrivenJob as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks internal event driven jobs that respond to internal system events.
 *
 * Uses Table per Class inheritance - has its own table with all fields.
 */
@Entity
@Table(name = "internal_event_driven_job",
    indexes = {
        @Index(name = "idx_iedj_context_name", columnList = "context_name"),
        @Index(name = "idx_iedj_job_name", columnList = "job_name"),
        @Index(name = "idx_iedj_agent_name", columnList = "agent_name"),
        @Index(name = "idx_iedj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_iedj_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateInternalEventDrivenJobRecord extends HibernateSchedulerJobRecord implements InternalEventDrivenJobRecord {

    /**
     * Default constructor for JPA
     */
    public HibernateInternalEventDrivenJobRecord() {
        super();
        this.type = JobConstants.INTERNAL_EVENT_DRIVEN_JOB;
    }

    /**
     * Constructor with ID
     */
    public HibernateInternalEventDrivenJobRecord(String id) {
        super(id);
        this.type = JobConstants.INTERNAL_EVENT_DRIVEN_JOB;
    }

    @Override
    protected InternalEventDrivenJob deserializeJobFromJson(String json)  {
        return objectMapper.readValue(json, InternalEventDrivenJob.class);
    }

    @Override
    public InternalEventDrivenJob getInternalEventDrivenJob() {
        return (InternalEventDrivenJob) getJob();
    }

    @Override
    public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
        setJob(internalEventDrivenJob);
        // Check if template job
        if (Boolean.TRUE.equals(internalEventDrivenJob.isTemplateJob())) {
            this.type = JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE;
        } else {
            this.type = JobConstants.INTERNAL_EVENT_DRIVEN_JOB;
        }
    }
}
