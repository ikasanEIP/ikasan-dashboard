package org.ikasan.scheduled.instance.model;

import org.ikasan.scheduled.job.model.SolrContextStartJobImpl;
import org.ikasan.spec.scheduled.instance.model.ContextStartJobInstance;
import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrContextStartJobInstanceImpl extends SolrSchedulerJobInstanceImpl implements ContextStartJobInstance {

    private final String agentName = JobConstants.CONTEXT_START_JOB;

    public final String getAgentName() {
        return agentName;
    }

    public final void setAgentName(String agentName) {
        // nothing to do
    }

    @Override
    public String getIdentifier() {
        return agentName + "-" + getJobName();
    }

    @Override
    public void setIdentifier(String jobIdentifier) {
        // nothing to do
    }
}
