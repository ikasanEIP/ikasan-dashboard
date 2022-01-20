package org.ikasan.job.orchestration.model.instance;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;

public class ScheduledContextInstanceRecordImpl implements ScheduledContextInstanceRecord {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = ObjectMapperFactory.newInstance();
    }

    private String id;
    private String contextName;
    private String contextInstance;
    private String status;
    private long timestamp;

    @Override
    public void setContextName(String contextName) {

    }

    @Override
    public ContextInstance getContextInstance() {
        try {
            return objectMapper.readValue(this.contextInstance, ContextInstanceImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.contextInstance, e);
        }
    }

    @Override
    public void setContextInstance(ContextInstance context) {
        try {
            this.contextInstance = objectMapper.writeValueAsString(context);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + context, e);
        }
    }

    @Override
    public void setTimestamp(long timestamp) {

    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getContextName() {
        return this.contextName;
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
}
