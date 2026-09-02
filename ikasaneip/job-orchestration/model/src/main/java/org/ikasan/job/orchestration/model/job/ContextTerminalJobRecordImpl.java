package org.ikasan.job.orchestration.model.job;

import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJobRecord;
import tools.jackson.databind.json.JsonMapper;

public class ContextTerminalJobRecordImpl implements ContextTerminalJobRecord {

    private static final JsonMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();

    private String id;
    private String agentName;
    private String jobName;
    private String displayName;
    private String contextName;
    private String contextStartJob;
    private long timestamp = -1;
    private long modifiedTimestamp;
    private String modifiedBy;
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
        return objectMapper.readValue(contextStartJob, ContextTerminalJobImpl.class);
    }

    @Override
    public void setContextTerminalJob(ContextTerminalJob contextTerminalJob) {
        this.contextStartJob = objectMapper.writeValueAsString(contextTerminalJob);
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
