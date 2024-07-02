package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrContextStartJobImpl extends SolrSchedulerJobImpl implements ContextStartJob {

    public SolrContextStartJobImpl() {
        super();
        super.agentName = JobConstants.CONTEXT_START_JOB;
    }

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
