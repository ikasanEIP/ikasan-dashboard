package org.ikasan.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrQuartzScheduleDrivenJobRecordImpl implements QuartzScheduleDrivenJobRecord {

    private static ObjectMapper objectMapper = new ObjectMapper();

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public QuartzScheduleDrivenJob getQuartzScheduleDrivenJob() throws JsonProcessingException {
        return objectMapper.readValue(quartzScheduleDrivenJob, SolrQuartzScheduleDrivenJobImpl.class);
    }

    public void setQuartzScheduleDrivenJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) throws JsonProcessingException {
        this.quartzScheduleDrivenJob = objectMapper.writeValueAsString(quartzScheduleDrivenJob);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
