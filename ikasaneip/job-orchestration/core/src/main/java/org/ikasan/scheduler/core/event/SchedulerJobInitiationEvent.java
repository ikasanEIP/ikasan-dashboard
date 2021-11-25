package org.ikasan.scheduler.core.event;

import org.ikasan.scheduler.core.model.job.InternalEventDrivenJob;

public class SchedulerJobInitiationEvent {
    private String agentName;
    private String jobName;
    private InternalEventDrivenJob internalEventDrivenJob;

    public SchedulerJobInitiationEvent(String agentName, String jobName) {
        this.agentName = agentName;
        this.jobName = jobName;
    }

    public SchedulerJobInitiationEvent() {

    }

    public String getAgentName() {
        return agentName;
    }

    public String getJobName() {
        return jobName;
    }

    public InternalEventDrivenJob getInternalEventDrivenJob() {
        return internalEventDrivenJob;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
        this.internalEventDrivenJob = internalEventDrivenJob;
    }
}
