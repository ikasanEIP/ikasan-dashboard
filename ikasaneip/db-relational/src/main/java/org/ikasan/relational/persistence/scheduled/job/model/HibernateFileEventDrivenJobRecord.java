package org.ikasan.relational.persistence.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.*;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;

/**
 * Hibernate/PostgreSQL implementation of FileEventDrivenJobRecord.
 *
 * Stores the FileEventDrivenJob as JSONB in PostgreSQL for efficient storage and querying.
 * This record tracks file event driven jobs that monitor file system events.
 *
 * Uses Table per Class inheritance - has its own table with all fields.
 */
@Entity
@Table(name = "file_event_driven_job",
    indexes = {
        @Index(name = "idx_fedj_context_name", columnList = "context_name"),
        @Index(name = "idx_fedj_job_name", columnList = "job_name"),
        @Index(name = "idx_fedj_agent_name", columnList = "agent_name"),
        @Index(name = "idx_fedj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_fedj_modified_timestamp", columnList = "modified_timestamp")
    })
public class HibernateFileEventDrivenJobRecord extends HibernateSchedulerJobRecord implements FileEventDrivenJobRecord {

    /**
     * Default constructor for JPA
     */
    public HibernateFileEventDrivenJobRecord() {
        super();
        this.type = JobConstants.FILE_EVENT_DRIVEN_JOB;
    }

    /**
     * Constructor with ID
     */
    public HibernateFileEventDrivenJobRecord(String id) {
        super(id);
        this.type = JobConstants.FILE_EVENT_DRIVEN_JOB;
    }

    @Override
    protected FileEventDrivenJob deserializeJobFromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, FileEventDrivenJob.class);
    }

    @Override
    public FileEventDrivenJob getFileEventDrivenJob() {
        return (FileEventDrivenJob) getJob();
    }

    @Override
    public void setFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob) {
        setJob(fileEventDrivenJob);
        this.type = JobConstants.FILE_EVENT_DRIVEN_JOB;
    }
}
