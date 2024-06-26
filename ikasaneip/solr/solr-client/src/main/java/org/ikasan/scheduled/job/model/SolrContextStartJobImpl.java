package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrContextStartJobImpl extends SolrSchedulerJobImpl implements ContextStartJob {

    private final String agentName = JobConstants.CONTEXT_START_JOB;

    public final String getAgentName() {
        return agentName;
    }

    public final void setAgentName(String agentName) {
        // nothing to do
    }
}
