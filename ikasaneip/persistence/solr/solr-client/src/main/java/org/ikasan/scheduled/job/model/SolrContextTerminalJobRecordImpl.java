package org.ikasan.scheduled.job.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJobRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class SolrContextTerminalJobRecordImpl implements ContextTerminalJobRecord {

    private static final JsonMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.MODULE_NAME)
    private String agentName;

    @Field(EntityFields.FLOW_NAME)
    private String jobName;

    @Field(EntityFields.DISPLAY_NAME)
    private String displayName;

    @Field(EntityFields.COMPONENT_NAME)
    private String contextName;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String contextStartJob;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp = -1;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Field(EntityFields.SKIPPED)
    private boolean skipped;

    @Override
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
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public ContextTerminalJob getContextTerminalJob() {
        try {
            return objectMapper.readValue(contextStartJob, SolrContextTerminalJobImpl.class);
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.contextStartJob, e);
        }
    }

    @Override
    public void setContextTerminalJob(ContextTerminalJob contextTerminalJob) {
        try {
            this.contextStartJob = objectMapper.writeValueAsString(contextTerminalJob);
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + contextTerminalJob, e);
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
