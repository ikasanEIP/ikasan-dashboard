package org.ikasan.scheduled.job.model;


import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public abstract class SolrSchedulerJobImpl implements SchedulerJob {
    protected String jobIdentifier;
    protected String agentName;
    protected String jobName;
    protected String contextId;

    @Override
    public String getContextId() {
        return this.contextId;
    }

    @Override
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    @Override
    public String getJobIdentifier() {
        return this.jobIdentifier;
    }

    @Override
    public void setJobIdentifier(String jobIdentifier) {
        this.jobIdentifier = jobIdentifier;
    }

    @Override
    public String getAgentName() {
        return this.agentName;
    }

    @Override
    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
