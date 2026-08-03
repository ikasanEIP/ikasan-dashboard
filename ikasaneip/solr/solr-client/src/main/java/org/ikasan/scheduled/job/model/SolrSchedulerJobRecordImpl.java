package org.ikasan.scheduled.job.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrSchedulerJobRecordImpl implements SchedulerJobRecord {

    private final static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.TYPE)
    private String type;

    @Field(SolrDaoBase.MODULE_NAME)
    private String agentName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String jobName;

    @Field(SolrDaoBase.DISPLAY_NAME)
    private String displayName;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String contextName;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String job;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(SolrDaoBase.MODIFIED_BY)
    private String modifiedBy;

    @Field(SolrDaoBase.HELD)
    private boolean held;

    @Field(SolrDaoBase.SKIPPED)
    private boolean skipped;

    @Field(SolrDaoBase.TARGET_RESIDING_CONTEXT_ONLY)
    private boolean targetResidingContextOnly;

    @Field(SolrDaoBase.PARTICIPATES_IN_LOCK)
    private boolean participatesInLock;

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
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getContextName() {
        return this.contextName;
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
                case JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE:
                    return objectMapper.readValue(this.job, SolrInternalEventDrivenJobImpl.class);
                case JobConstants.GLOBAL_EVENT_JOB:
                    return objectMapper.readValue(this.job, SolrGlobalEventJobImpl.class);
                case JobConstants.CONTEXT_START_JOB:
                    return objectMapper.readValue(this.job, SolrContextStartJobImpl.class);
                case JobConstants.CONTEXT_TERMINAL_JOB:
                    return objectMapper.readValue(this.job, SolrContextTerminalJobImpl.class);
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

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }


    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public boolean isHeld() {
        return held;
    }

    @Override
    public boolean isSkipped() {
        return skipped;
    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return this.targetResidingContextOnly;
    }

    @Override
    public boolean isParticipatesInLock() {
        return participatesInLock;
    }
}
