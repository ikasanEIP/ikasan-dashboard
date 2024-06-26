package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrContextTerminalJobImpl extends SolrSchedulerJobImpl implements ContextTerminalJob {

    private final String agentName = JobConstants.CONTEXT_TERMINAL_JOB;

    public final String getAgentName() {
        return agentName;
    }

    public final void setAgentName(String agentName) {
        // nothing to do
    }
}
