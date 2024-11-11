package org.ikasan.scheduled.visualisation.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayout;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayoutRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrContextVisualisationLayoutRecordImpl implements ContextVisualisationLayoutRecord {
    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String parentContext;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String context;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String contextVisualisationLayout;

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

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getParentContext() {
        return parentContext;
    }

    @Override
    public void setParentContext(String parentContext) {
        this.parentContext = parentContext;
    }

    @Override
    public String getContext() {
        return context;
    }

    @Override
    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public ContextVisualisationLayout getContextVisualisationLayout() {
        try {
            return objectMapper.readValue(this.contextVisualisationLayout, SolrContextVisualisationLayoutImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + contextVisualisationLayout, e);
        }
    }

    @Override
    public void setContextVisualisationLayout(ContextVisualisationLayout contextVisualisationLayout) {
        try {
            this.contextVisualisationLayout = objectMapper.writeValueAsString(contextVisualisationLayout);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + contextVisualisationLayout, e);
        }
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
