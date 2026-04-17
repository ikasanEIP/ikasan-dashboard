package org.ikasan.relational.persistence.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.*;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;

/**
 * Hibernate/PostgreSQL implementation of QuartzScheduleDrivenJobRecord.
 *
 * Stores the QuartzScheduleDrivenJob as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks quartz schedule driven jobs that execute based on cron expressions.
 *
 * Uses Table per Class inheritance - has its own table with all fields.
 */
@Entity
@Table(name = "quartz_schedule_driven_job",
    indexes = {
        @Index(name = "idx_qsdj_context_name", columnList = "context_name"),
        @Index(name = "idx_qsdj_job_name", columnList = "job_name"),
        @Index(name = "idx_qsdj_agent_name", columnList = "agent_name"),
        @Index(name = "idx_qsdj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_qsdj_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateQuartzScheduleDrivenJobRecord extends HibernateSchedulerJobRecord implements QuartzScheduleDrivenJobRecord {

    /**
     * Default constructor for JPA
     */
    public HibernateQuartzScheduleDrivenJobRecord() {
        super();
        this.type = JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB;
    }

    /**
     * Constructor with ID
     */
    public HibernateQuartzScheduleDrivenJobRecord(String id) {
        super(id);
        this.type = JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB;
    }

    @Override
    protected QuartzScheduleDrivenJob deserializeJobFromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, QuartzScheduleDrivenJob.class);
    }

    @Override
    public QuartzScheduleDrivenJob getQuartzScheduleDrivenJob() {
        return (QuartzScheduleDrivenJob) getJob();
    }

    @Override
    public void setQuartzScheduleDrivenJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {
        setJob(quartzScheduleDrivenJob);
        this.type = JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB;
    }
}
