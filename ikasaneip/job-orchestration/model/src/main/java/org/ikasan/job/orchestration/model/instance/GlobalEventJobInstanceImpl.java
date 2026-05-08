package org.ikasan.job.orchestration.model.instance;

import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class GlobalEventJobInstanceImpl extends SchedulerJobInstanceImpl implements GlobalEventJobInstance {

    @Override
    public final String getAgentName() {
        return JobConstants.GLOBAL_EVENT;
    }

    @Override
    public final void setAgentName(String agentName) {
        // nothing to do
    }
}
