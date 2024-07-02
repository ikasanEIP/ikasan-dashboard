package org.ikasan.job.orchestration.model.instance;

import org.ikasan.spec.scheduled.instance.model.ContextStartJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class ContextStartJobInstanceImpl extends SchedulerJobInstanceImpl implements ContextStartJobInstance {
    public ContextStartJobInstanceImpl() {
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
