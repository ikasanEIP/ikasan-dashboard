package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class SolrScheduledContextInstanceRecordImpl implements ScheduledContextInstanceRecord {
    private static final JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Field(EntityFields.ID)
    protected String id;

    @Field(EntityFields.MODULE_NAME)
    private String contextName;

    @Field(EntityFields.COMPONENT_NAME)
    private String contextInstanceId;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String contextInstance;

    @Field(EntityFields.STATUS)
    private String status;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Field(EntityFields.START_TIME)
    private long startTime;

    @Field(EntityFields.END_TIME)
    private long endTime;

    @Field(EntityFields.CONTAINS_REPEATING_JOBS)
    private boolean containsRepeatingJobs = false;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
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
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.contextInstance, e);
        }
    }

    @Override
    public void setContextInstance(ContextInstance context) {
        try {
            this.contextInstance = objectMapper.writeValueAsString(context);
        }
        catch (JacksonException e) {
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

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean isContainsRepeatingJobs() {
        return containsRepeatingJobs;
    }

    @Override
    public void setContainsRepeatingJobs(boolean containsRepeatingJobs) {
        this.containsRepeatingJobs = containsRepeatingJobs;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
