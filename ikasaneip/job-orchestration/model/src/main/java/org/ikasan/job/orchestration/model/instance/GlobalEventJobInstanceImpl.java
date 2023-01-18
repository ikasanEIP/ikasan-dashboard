package org.ikasan.job.orchestration.model.instance;

import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class GlobalEventJobInstanceImpl extends SchedulerJobInstanceImpl implements GlobalEventJobInstance {
    private final String agentName = JobConstants.GLOBAL_EVENT;

    public final String getAgentName() {
        return agentName;
    }

    public final void setAgentName(String agentName) {
        // nothing to do
    }
}
