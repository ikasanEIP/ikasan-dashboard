package org.ikasan.job.orchestration.model.instance;

import org.ikasan.spec.scheduled.instance.model.ContextStartJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class ContextStartJobInstanceImpl extends SchedulerJobInstanceImpl implements ContextStartJobInstance {
    private final String agentName = JobConstants.CONTEXT_START_JOB_INSTANCE;

    public final String getAgentName() {
        return agentName;
    }

    public final void setAgentName(String agentName) {
        // nothing to do
    }
}
