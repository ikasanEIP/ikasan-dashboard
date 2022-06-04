package org.ikasan.job.orchestration.model.event;

import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;

public class ContextualisedSchedulerJobInitiationEventImpl implements ContextualisedSchedulerJobInitiationEvent {

    private SchedulerJobInitiationEvent schedulerJobInitiationEvent;
    private String contextName;

    @Override
    public void setSchedulerJobInitiationEvent(SchedulerJobInitiationEvent schedulerJobInitiationEvent) {
        this.schedulerJobInitiationEvent = schedulerJobInitiationEvent;
    }

    @Override
    public SchedulerJobInitiationEvent getSchedulerJobInitiationEvent() {
        return this.schedulerJobInitiationEvent;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }
}
