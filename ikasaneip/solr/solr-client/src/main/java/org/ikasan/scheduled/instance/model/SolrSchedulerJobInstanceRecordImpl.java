package org.ikasan.scheduled.instance.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrSchedulerJobInstanceRecordImpl implements SchedulerJobInstanceRecord {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = ScheduledObjectMapperFactory.newInstance();
    }

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String jobName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String contextName;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String contextInstanceId;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String schedulerJobInstance;

    @Field(SolrDaoBase.STATUS)
    private String status;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(SolrDaoBase.MODIFIED_BY)
    private String modifiedBy;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
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
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    @Override
    public SchedulerJobInstance getSchedulerJobInstance() {
        try {
            return objectMapper.readValue(this.schedulerJobInstance, SolrSchedulerJobInstanceImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.schedulerJobInstance, e);
        }
    }

    @Override
    public void setSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) {
        try {
            this.schedulerJobInstance = objectMapper.writeValueAsString(schedulerJobInstance);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + schedulerJobInstance, e);
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
        return this.modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return this.modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
