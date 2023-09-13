package org.ikasan.job.orchestration.model.status;

import java.util.List;

public class ContextJobInstanceStatusWrapper {

    List<ContextJobInstanceStatus> jobPlans;

    public List<ContextJobInstanceStatus> getJobPlans() {
        return jobPlans;
    }

    public void setJobPlans(List<ContextJobInstanceStatus> jobPlans) {
        this.jobPlans = jobPlans;
    }

    @Override
    public String toString() {
        return "ContextJobInstanceStatusWrapper{" +
            "jobPlanStatus=" + jobPlans +
            '}';
    }
}
