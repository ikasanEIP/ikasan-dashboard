package org.ikasan.scheduler.core.model.instance;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.scheduler.core.model.context.*;
import org.ikasan.scheduler.core.model.job.SchedulerJobImpl;
import org.ikasan.scheduler.exception.EntityConversionException;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class ScheduledContextInstanceRecordImpl implements ScheduledContextInstanceRecord {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();

        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(And.class, AndImpl.class)
            .addAbstractTypeMapping(Or.class, OrImpl.class)
            .addAbstractTypeMapping(Not.class, NotImpl.class)
            .addAbstractTypeMapping(ContextTemplate.class, ContextTemplateImpl.class)
            .addAbstractTypeMapping(Context.class, ContextImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, ContextParameterImpl.class)
            .addAbstractTypeMapping(SchedulerJob.class, SchedulerJobImpl.class)
            .addAbstractTypeMapping(JobDependency.class, JobDependencyImpl.class)
            .addAbstractTypeMapping(ContextDependency.class, ContextDependencyImpl.class)
            .addAbstractTypeMapping(LogicalGrouping.class, LogicalGroupingImpl.class)
            .addAbstractTypeMapping(LogicalOperator.class, LogicalOperatorImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, ContextParameterImpl.class);

        objectMapper.registerModule(simpleModule);
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
