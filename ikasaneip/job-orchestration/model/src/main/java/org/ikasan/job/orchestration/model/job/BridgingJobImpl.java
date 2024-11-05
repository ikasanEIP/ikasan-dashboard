package org.ikasan.job.orchestration.model.job;

import org.ikasan.spec.scheduled.job.model.BridgingJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class BridgingJobImpl extends SchedulerJobImpl implements BridgingJob {
    public BridgingJobImpl() {
        super();
        super.agentName = JobConstants.BRIDGING_JOB;
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
