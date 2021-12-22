package org.ikasan.scheduled.context.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledContextRecordImpl implements ScheduledContextRecord {
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
    private String context;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    public SolrScheduledContextRecordImpl(String id, String contextName, String context, long timestamp) {
        this.id = id;
        this.contextName = contextName;
        this.context = context;
        this.timestamp = timestamp;
    }

    public SolrScheduledContextRecordImpl() {
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
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setId(String id) {

    }

    @Override
    public void setContextName(String contextName) {

    }

    @Override
    public ContextTemplate getContext() {
        return null;
    }

    @Override
    public void setContext(ContextTemplate context) {

    }

    @Override
    public void setTimestamp(long timestamp) {

    }
}
