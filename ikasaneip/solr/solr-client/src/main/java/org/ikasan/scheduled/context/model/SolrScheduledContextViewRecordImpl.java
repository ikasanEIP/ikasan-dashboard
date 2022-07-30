package org.ikasan.scheduled.context.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledContextViewRecordImpl implements ScheduledContextViewRecord {
    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String parentContextName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String contextName;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String contextView;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(SolrDaoBase.MODIFIED_BY)
    private String modifiedBy;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getParentContextName() {
        return parentContextName;
    }

    @Override
    public void setParentContextName(String parentContextName) {
        this.parentContextName = parentContextName;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getContextView() {
        return contextView;
    }

    @Override
    public void setContextView(String contextView) {
        this.contextView = contextView;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
