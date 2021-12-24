package org.ikasan.scheduled.instance.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.context.model.*;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.job.model.SolrSchedulerJobImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledContextInstanceRecordImpl implements ScheduledContextInstanceRecord {
    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();

        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(And.class, SolrAndImpl.class)
            .addAbstractTypeMapping(Or.class, SolrOrImpl.class)
            .addAbstractTypeMapping(Not.class, SolrNotImpl.class)
            .addAbstractTypeMapping(ContextTemplate.class, SolrContextTemplateImpl.class)
            .addAbstractTypeMapping(Context.class, SolrContextImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, SolrContextParameterImpl.class)
            .addAbstractTypeMapping(SchedulerJob.class, SolrSchedulerJobImpl.class)
            .addAbstractTypeMapping(JobDependency.class, SolrJobDependencyImpl.class)
            .addAbstractTypeMapping(ContextDependency.class, SolrContextDependencyImpl.class)
            .addAbstractTypeMapping(LogicalGrouping.class, SolrLogicalGroupingImpl.class)
            .addAbstractTypeMapping(LogicalOperator.class, SolrLogicalOperatorImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, SolrContextParameterImpl.class);

        objectMapper.registerModule(simpleModule);
    }

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String contextName;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String contextInstance;

    @Field(SolrDaoBase.STATUS)
    private String status;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
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
    public ContextInstance getContextInstance() {
        try {
            return objectMapper.readValue(this.contextInstance, SolrContextInstanceImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.contextInstance, e);
        }
    }

    @Override
    public void setContextInstance(ContextInstance context) {
        try {
            this.contextInstance = objectMapper.writeValueAsString(context);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + context, e);
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
}
