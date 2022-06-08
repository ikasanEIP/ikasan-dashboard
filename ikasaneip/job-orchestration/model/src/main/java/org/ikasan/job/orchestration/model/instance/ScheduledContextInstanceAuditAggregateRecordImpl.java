package org.ikasan.job.orchestration.model.instance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregate;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;

public class ScheduledContextInstanceAuditAggregateRecordImpl implements ScheduledContextInstanceAuditAggregateRecord {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.newInstance();

    private String id;
    private String contextName;
    private String contextInstanceId;
    private String contextInstanceAudit;
    private long timestamp;
    private String scheduledProcessEventName;
    private String raisedEvents;

    @Override
    public String getId() {
        return this.id;
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
    public ScheduledContextInstanceAuditAggregate getScheduledContextInstanceAuditAggregate() {
        try {
            return OBJECT_MAPPER.readValue(this.contextInstanceAudit, ScheduledContextInstanceAuditAggregateImpl.class);
        } catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.contextInstanceAudit, e);
        }
    }

    @Override
    public void setScheduledContextInstanceAuditAggregate(ScheduledContextInstanceAuditAggregate scheduledContextInstanceAudit) {
        try {
            this.contextInstanceAudit = OBJECT_MAPPER.writeValueAsString(scheduledContextInstanceAudit);
        } catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + scheduledContextInstanceAudit, e);
        }
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
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
}
