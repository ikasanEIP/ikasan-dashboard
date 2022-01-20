package org.ikasan.job.orchestration.model.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;

public class ScheduledContextRecordImpl implements ScheduledContextRecord {
    private static ObjectMapper objectMapper;

    static {
        objectMapper = ObjectMapperFactory.newInstance();
    }

    private String id;
    private String contextName;
    private String context;
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
    public ContextTemplate getContext() {
        try {
            return objectMapper.readValue(this.context, ContextTemplateImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.context, e);
        }
    }

    @Override
    public void setContext(ContextTemplate context) {
        try {
            this.context = objectMapper.writeValueAsString(context);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + context, e);
        }
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }
}
