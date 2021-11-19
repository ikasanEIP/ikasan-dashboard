package org.ikasan.scheduler.core.event;

import org.ikasan.scheduler.core.model.job.SchedulerJob;

public class SchedulerJobInitiationEvent {
    private String agentName;
    private String jobName;
    private SchedulerJob schedulerJob;

    public SchedulerJobInitiationEvent(String agentName, String jobName) {
        this.agentName = agentName;
        this.jobName = jobName;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getJobName() {
        return jobName;
    }

    public SchedulerJob getSchedulerJob() {
        return schedulerJob;
    }
}
