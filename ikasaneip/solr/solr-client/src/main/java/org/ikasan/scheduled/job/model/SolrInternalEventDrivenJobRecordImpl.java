package org.ikasan.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrInternalEventDrivenJobRecordImpl implements InternalEventDrivenJobRecord {

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String agentName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String jobName;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String contextId;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String internalEventDrivenJob;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    private ObjectMapper objectMapper = new ObjectMapper();

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

    public InternalEventDrivenJob getInternalEventDrivenJob() throws JsonProcessingException {
        return this.objectMapper.readValue(internalEventDrivenJob, SolrInternalEventDrivenJobImpl.class);
    }

    public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) throws JsonProcessingException {
        this.internalEventDrivenJob = this.objectMapper.writeValueAsString(internalEventDrivenJob);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
