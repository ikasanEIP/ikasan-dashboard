package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrContextTerminalJobImpl extends SolrSchedulerJobImpl implements ContextTerminalJob {

    public SolrContextTerminalJobImpl() {
        super();
        super.agentName = JobConstants.CONTEXT_TERMINAL_JOB;
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
