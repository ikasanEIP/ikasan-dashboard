package org.ikasan.scheduled.event.model;

import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;

import java.util.StringJoiner;

public class SolrContextualisedSchedulerJobInitiationEventImpl implements ContextualisedSchedulerJobInitiationEvent {

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

    @Override
    public String toString() {
        return new StringJoiner(", ", SolrContextualisedSchedulerJobInitiationEventImpl.class.getSimpleName() + "[", "]")
            .add("schedulerJobInitiationEvent=" + schedulerJobInitiationEvent)
            .add("contextName='" + contextName + "'")
            .toString();
    }
}
