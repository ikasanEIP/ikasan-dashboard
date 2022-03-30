package org.ikasan.scheduled.context.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledContextRecordImpl implements ScheduledContextRecord {
    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();


    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String contextName;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String context;

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
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public ContextTemplate getContext() {
        try {
            return objectMapper.readValue(this.context, SolrContextTemplateImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + context, e);
        }
    }

    @Override
    public void setContext(ContextTemplate context) {
        try {
            this.context = objectMapper.writeValueAsString(context);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + context, e);
        }
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
