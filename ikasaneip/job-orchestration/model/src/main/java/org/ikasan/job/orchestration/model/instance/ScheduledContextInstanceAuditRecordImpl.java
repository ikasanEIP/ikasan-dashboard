package org.ikasan.job.orchestration.model.instance;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAudit;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScheduledContextInstanceAuditRecordImpl implements ScheduledContextInstanceAuditRecord {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.newInstance();

    private String id;
    private String contextName;
    private String contextInstanceId;
    private String contextInstanceAudit;
    private long timestamp;

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
    public ScheduledContextInstanceAudit getScheduledContextInstanceAudit() {
        try {
            return OBJECT_MAPPER.readValue(this.contextInstanceAudit, ScheduledContextInstanceAuditImpl.class);
        } catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.contextInstanceAudit, e);
        }
    }

    @Override
    public void setScheduledContextInstanceAudit(ScheduledContextInstanceAudit scheduledContextInstanceAudit) {
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
}
