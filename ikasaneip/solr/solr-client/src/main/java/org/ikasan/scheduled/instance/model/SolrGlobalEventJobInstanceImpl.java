package org.ikasan.scheduled.instance.model;

import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class SolrGlobalEventJobInstanceImpl extends SolrSchedulerJobInstanceImpl implements GlobalEventJobInstance {

    private final String agentName = JobConstants.GLOBAL_EVENT;

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
