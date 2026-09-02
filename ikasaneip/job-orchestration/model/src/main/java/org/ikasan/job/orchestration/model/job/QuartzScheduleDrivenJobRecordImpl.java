package org.ikasan.job.orchestration.model.job;

import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import tools.jackson.databind.json.JsonMapper;

public class QuartzScheduleDrivenJobRecordImpl implements QuartzScheduleDrivenJobRecord {

    private static final JsonMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();

    private String id;
    private String agentName;
    private String jobName;
    private String displayName;
    private String contextName;
    private String quartzScheduleDrivenJob;
    private long timestamp;
    private long modifiedTimestamp;
    private String modifiedBy;

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
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public QuartzScheduleDrivenJob getQuartzScheduleDrivenJob() {
        return objectMapper.readValue(quartzScheduleDrivenJob, QuartzScheduleDrivenJobImpl.class);
    }

    public void setQuartzScheduleDrivenJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {
        this.quartzScheduleDrivenJob = objectMapper.writeValueAsString(quartzScheduleDrivenJob);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
