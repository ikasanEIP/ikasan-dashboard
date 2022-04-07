package org.ikasan.scheduled.instance.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAudit;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;
import org.ikasan.spec.solr.SolrDaoBase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SolrScheduledContextInstanceAuditRecordImpl implements ScheduledContextInstanceAuditRecord {

    private static final ObjectMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String contextName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String contextInstanceId;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String contextInstanceAudit;

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
    public String getContextInstanceId() {
        return this.contextInstanceId;
    }

    @Override
    public ScheduledContextInstanceAudit getScheduledContextInstanceAudit() {
        try {
            return OBJECT_MAPPER.readValue(this.contextInstanceAudit, SolrScheduledContextInstanceAuditImpl.class);
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.contextInstanceAudit, e);
        }
    }

    @Override
    public void setScheduledContextInstanceAudit(ScheduledContextInstanceAudit scheduledContextInstanceAudit) {
        try {
            this.contextInstanceAudit = OBJECT_MAPPER.writeValueAsString(scheduledContextInstanceAudit);
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + scheduledContextInstanceAudit, e);
        }
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }
}
