package org.ikasan.job.orchestration.model.status;

import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatus;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatusWrapper;

import java.util.List;

public class ContextJobInstanceStatusWrapperImpl implements ContextJobInstanceStatusWrapper {

    List<ContextJobInstanceStatus> jobPlans;

    public List<ContextJobInstanceStatus> getJobPlans() {
        return jobPlans;
    }

    public void setJobPlans(List<ContextJobInstanceStatus> jobPlans) {
        this.jobPlans = jobPlans;
    }

    @Override
    public String toString() {
        return "ContextJobInstanceStatusWrapperImpl{" +
            "jobPlanStatus=" + jobPlans +
            '}';
    }
}
