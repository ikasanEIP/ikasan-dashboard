package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.BridgingJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrBridgingJobImpl extends SolrSchedulerJobImpl implements BridgingJob {

    public SolrBridgingJobImpl() {
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
