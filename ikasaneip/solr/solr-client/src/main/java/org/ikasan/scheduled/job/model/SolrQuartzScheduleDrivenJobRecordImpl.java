package org.ikasan.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrQuartzScheduleDrivenJobRecordImpl implements QuartzScheduleDrivenJobRecord {

    private static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String agentName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String jobName;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String contextId;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String quartzScheduleDrivenJob;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getAgentName() {
        return agentName;
    }

    @Override
    public void setAgentName(String agentName) {
        this.agentName = agentName;
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
    public String getContextId() {
        return this.contextId;
    }

    @Override
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public QuartzScheduleDrivenJob getQuartzScheduleDrivenJob() {
        try {
            return objectMapper.readValue(quartzScheduleDrivenJob, SolrQuartzScheduleDrivenJobImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.quartzScheduleDrivenJob, e);
        }
    }

    public void setQuartzScheduleDrivenJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {
        try {
            this.quartzScheduleDrivenJob = objectMapper.writeValueAsString(quartzScheduleDrivenJob);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + quartzScheduleDrivenJob, e);
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
