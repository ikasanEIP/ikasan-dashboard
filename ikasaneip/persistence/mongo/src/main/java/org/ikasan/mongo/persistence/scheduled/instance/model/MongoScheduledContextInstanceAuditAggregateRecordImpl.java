package org.ikasan.mongo.persistence.scheduled.instance.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregate;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * MongoDB implementation of ScheduledContextInstanceAuditAggregateRecord.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoScheduledContextInstanceAuditAggregateRecordImpl implements ScheduledContextInstanceAuditAggregateRecord {

    private static final JsonMapper OBJECT_MAPPER = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field("context_name")
    @Indexed
    private String contextName;

    @Field("context_instance_id")
    @Indexed
    private String contextInstanceId;

    @Field("context_instance_audit")
    private String contextInstanceAudit;

    @Field("scheduled_process_event_name")
    @Indexed
    private String scheduledProcessEventName;

    @Field("raised_events")
    private String raisedEvents;

    @Field("created_date_time")
    @Indexed
    private long timestamp;

    @Field("status")
    @Indexed
    private String status;

    @Field("is_repeating_job")
    @Indexed
    private boolean isRepeatingJob;

    @Field("job_type")
    @Indexed
    private String jobType;

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getContextInstanceId() {
        return this.contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    @Override
    public String getScheduledProcessEventName() {
        return this.scheduledProcessEventName;
    }

    @Override
    public void setScheduledProcessEventName(String scheduledProcessEventName) {
        this.scheduledProcessEventName = scheduledProcessEventName;
    }

    @Override
    public String getRaisedEvents() {
        return this.raisedEvents;
    }

    public void setRaisedEvents(String raisedEvents) {
        this.raisedEvents = raisedEvents;
    }

    @Override
    public ScheduledContextInstanceAuditAggregate getScheduledContextInstanceAuditAggregate() {
        try {
            return OBJECT_MAPPER.readValue(this.contextInstanceAudit, ScheduledContextInstanceAuditAggregateImpl.class);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.contextInstanceAudit, e);
        }
    }

    @Override
    public void setScheduledContextInstanceAuditAggregate(ScheduledContextInstanceAuditAggregate scheduledContextInstanceAudit) {
        try {
            this.contextInstanceAudit = OBJECT_MAPPER.writeValueAsString(scheduledContextInstanceAudit);

            // Extract raised events from schedulerJobInitiationEvents for easier querying
            if (scheduledContextInstanceAudit != null &&
                scheduledContextInstanceAudit.getSchedulerJobInitiationEvents() != null &&
                !scheduledContextInstanceAudit.getSchedulerJobInitiationEvents().isEmpty()) {

                StringBuilder eventsBuffer = new StringBuilder();
                scheduledContextInstanceAudit.getSchedulerJobInitiationEvents()
                        .forEach(event -> eventsBuffer.append(event.getJobName()).append(" "));
                this.raisedEvents = eventsBuffer.toString().toLowerCase().trim();
            }
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + scheduledContextInstanceAudit, e);
        }
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean isRepeatingJob() {
        return isRepeatingJob;
    }

    @Override
    public void setRepeatingJob(boolean repeatingJob) {
        isRepeatingJob = repeatingJob;
    }

    @Override
    public String getJobType() {
        return jobType;
    }

    @Override
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
}
