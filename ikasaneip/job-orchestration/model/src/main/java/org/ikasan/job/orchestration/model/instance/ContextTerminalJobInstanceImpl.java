package org.ikasan.job.orchestration.model.instance;

import org.ikasan.spec.scheduled.instance.model.ContextTerminalJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class ContextTerminalJobInstanceImpl extends SchedulerJobInstanceImpl implements ContextTerminalJobInstance {
    public ContextTerminalJobInstanceImpl() {
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
