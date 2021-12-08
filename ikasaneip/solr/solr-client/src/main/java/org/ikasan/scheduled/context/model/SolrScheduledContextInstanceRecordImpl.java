package org.ikasan.scheduled.context.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.scheduled.context.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledContextInstanceRecordImpl implements ScheduledContextInstanceRecord {
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

    public SolrScheduledContextInstanceRecordImpl(String id, String contextName, String contextInstance, long timestamp) {
        this.id = id;
        this.contextName = contextName;
        this.contextInstance = contextInstance;
        this.timestamp = timestamp;
    }

    public SolrScheduledContextInstanceRecordImpl() {
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
    public String getContextInstance() {
        return this.contextInstance;
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
