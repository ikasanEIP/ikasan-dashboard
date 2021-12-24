package org.ikasan.scheduler.core.model.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.model.job.SchedulerJobImpl;
import org.ikasan.scheduler.exception.EntityConversionException;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class ScheduledContextRecordImpl implements ScheduledContextRecord {
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
