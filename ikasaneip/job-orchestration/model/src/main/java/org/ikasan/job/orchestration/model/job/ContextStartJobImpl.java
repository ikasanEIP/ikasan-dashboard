package org.ikasan.job.orchestration.model.job;

import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class ContextStartJobImpl extends SchedulerJobImpl implements ContextStartJob {
    public ContextStartJobImpl() {
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
