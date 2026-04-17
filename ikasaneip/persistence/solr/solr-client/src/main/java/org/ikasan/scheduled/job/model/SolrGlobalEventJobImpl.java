package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrGlobalEventJobImpl extends SolrSchedulerJobImpl implements GlobalEventJob {

    private final String agentName = JobConstants.GLOBAL_EVENT;

    public final String getAgentName() {
        return agentName;
    }

    public final void setAgentName(String agentName) {
        // nothing to do
    }
}
