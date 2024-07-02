package org.ikasan.scheduled.instance.model;

import org.ikasan.spec.scheduled.instance.model.ContextTerminalJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrContextTerminalJobInstanceImpl extends SolrSchedulerJobInstanceImpl implements ContextTerminalJobInstance {

    public SolrContextTerminalJobInstanceImpl() {
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
