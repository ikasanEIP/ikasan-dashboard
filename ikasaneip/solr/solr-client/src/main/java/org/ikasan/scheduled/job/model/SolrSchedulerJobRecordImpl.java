package org.ikasan.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrSchedulerJobRecordImpl implements SchedulerJobRecord {

    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.TYPE)
    private String type;

    @Field(SolrDaoBase.MODULE_NAME)
    private String agentName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String jobName;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String contextId;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String job;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getAgentName() {
        return agentName;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public String getContextId() {
        return this.contextId;
    }

    @Override
    public SchedulerJob getJob() {
        try {
            switch (this.type) {
                case JobConstants.FILE_EVENT_DRIVEN_JOB:
                    return objectMapper.readValue(this.job, SolrFileEventDrivenJobImpl.class);
                case JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB:
                    return objectMapper.readValue(this.job, SolrQuartzScheduleDrivenJobImpl.class);
                case JobConstants.INTERNAL_EVENT_DRIVEN_JOB:
                    return objectMapper.readValue(this.job, SolrInternalEventDrivenJobImpl.class);
                default:
                    throw new SolrEntityConversionException("Could not resolve job type: " + this.type);

            }
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity: " + this.job, e);
        }
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

}
