package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregate;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.solr.SolrDaoBase;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class SolrScheduledContextInstanceAuditAggregateRecordImpl implements ScheduledContextInstanceAuditAggregateRecord {

    private static final JsonMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String contextName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String contextInstanceId;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String contextInstanceAudit;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String scheduledProcessEventName;

    @Field(SolrDaoBase.EVENT)
    private String raisedEvents;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.STATUS)
    private String status;

    @Field(SolrDaoBase.IS_REPEATING_JOB)
    private boolean isRepeatingJob;

    @Field(SolrDaoBase.JOB_TYPE)
    private String jobType;

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
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    @Override
    public String getScheduledProcessEventName() {
        return this.scheduledProcessEventName;
    }

    @Override
    public void setScheduledProcessEventName(String scheduledProcessEventName) {
        this.scheduledProcessEventName = scheduledProcessEventName;
    }

    @Override
    public String getRaisedEvents() {
        return this.raisedEvents;
    }

    @Override
    public ScheduledContextInstanceAuditAggregate getScheduledContextInstanceAuditAggregate() {
        try {
            return OBJECT_MAPPER.readValue(this.contextInstanceAudit, SolrScheduledContextInstanceAuditAggregateImpl.class);
        } catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.contextInstanceAudit, e);
        }
    }

    @Override
    public void setScheduledContextInstanceAuditAggregate(ScheduledContextInstanceAuditAggregate scheduledContextInstanceAudit) {
        try {
            this.contextInstanceAudit = OBJECT_MAPPER.writeValueAsString(scheduledContextInstanceAudit);
        } catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + scheduledContextInstanceAudit, e);
        }
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean isRepeatingJob() {
        return isRepeatingJob;
    }

    @Override
    public void setRepeatingJob(boolean repeatingJob) {
        isRepeatingJob = repeatingJob;
    }

    @Override
    public String getJobType() {
        return jobType;
    }

    @Override
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
