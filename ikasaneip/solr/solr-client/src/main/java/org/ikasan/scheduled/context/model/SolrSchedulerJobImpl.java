package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class SolrSchedulerJobImpl implements SchedulerJob {
    private String identifier;
    private String agentName;
    private String jobName;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getContextId() {
        return null;
    }

    @Override
    public void setContextId(String contextId) {

    }

    @Override
    public String getJobIdentifier() {
        return identifier;
    }

    @Override
    public void setJobIdentifier(String jobIdentifier) {
        this.identifier = jobIdentifier;
    }

    @Override
    public String getJobDescription() {
        return null;
    }

    @Override
    public void setJobDescription(String jobDescription) {

    }
}
