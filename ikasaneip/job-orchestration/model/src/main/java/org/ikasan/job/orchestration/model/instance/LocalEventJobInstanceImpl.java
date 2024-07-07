package org.ikasan.job.orchestration.model.instance;

import org.ikasan.spec.scheduled.instance.model.LocalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public class LocalEventJobInstanceImpl extends SchedulerJobInstanceImpl implements LocalEventJobInstance {
    public LocalEventJobInstanceImpl() {
        super();
        super.agentName = JobConstants.LOCAL_EVENT_JOB;
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
