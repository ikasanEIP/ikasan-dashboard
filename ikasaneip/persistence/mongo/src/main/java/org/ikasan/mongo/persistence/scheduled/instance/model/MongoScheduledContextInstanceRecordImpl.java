package org.ikasan.mongo.persistence.scheduled.instance.model;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * MongoDB implementation of ScheduledContextInstanceRecord.
 *
 * @author Ikasan Development Team
 */
@Document(collection = "scheduledContextInstance")
public class MongoScheduledContextInstanceRecordImpl implements ScheduledContextInstanceRecord {

    private static final JsonMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field("context_name")
    @Indexed
    private String contextName;

    @Field("context_instance_id")
    @Indexed
    private String contextInstanceId;

    @Field("context_instance")
    private String contextInstance;

    @Field("status")
    @Indexed
    private String status;

    @Field("created_date_time")
    @Indexed
    private long timestamp;

    @Field("updated_date_time")
    @Indexed
    private long modifiedTimestamp;

    @Field("modified_by")
    private String modifiedBy;

    @Field("start_time")
    @Indexed
    private long startTime;

    @Field("end_time")
    @Indexed
    private long endTime;

    @Field("contains_repeating_jobs")
    private boolean containsRepeatingJobs = false;

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
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
    public ContextInstance getContextInstance() {
        try {
            return objectMapper.readValue(this.contextInstance, ContextInstanceImpl.class);
        }
        catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.contextInstance, e);
        }
    }

    @Override
    public void setContextInstance(ContextInstance context) {
        try {
            this.contextInstance = objectMapper.writeValueAsString(context);
            // Also copy startTime and endTime to top-level fields for querying
            if (context != null) {
                this.startTime = context.getStartTime();
                this.endTime = context.getEndTime();
                this.containsRepeatingJobs = context.isContainsRepeatingJobs();
            }
        }
        catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + context, e);
        }
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean isContainsRepeatingJobs() {
        return containsRepeatingJobs;
    }

    @Override
    public void setContainsRepeatingJobs(boolean containsRepeatingJobs) {
        this.containsRepeatingJobs = containsRepeatingJobs;
    }
}
