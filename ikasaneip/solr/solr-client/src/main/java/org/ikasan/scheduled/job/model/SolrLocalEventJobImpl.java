package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.LocalEventJob;

public class SolrLocalEventJobImpl extends SolrSchedulerJobImpl implements LocalEventJob {

    private final String agentName = JobConstants.LOCAL_EVENT_JOB;

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
