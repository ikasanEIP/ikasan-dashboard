package org.ikasan.scheduled.context.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledContextRecordImpl implements ScheduledContextRecord {
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
    public String getContext() {
        return this.context;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }
}
