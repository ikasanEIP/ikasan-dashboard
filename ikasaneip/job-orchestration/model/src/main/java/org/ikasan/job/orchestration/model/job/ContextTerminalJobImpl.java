package org.ikasan.job.orchestration.model.job;

import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class ContextTerminalJobImpl extends SchedulerJobImpl implements ContextStartJob {
    private final String agentName = JobConstants.CONTEXT_TERMINAL_JOB;

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
