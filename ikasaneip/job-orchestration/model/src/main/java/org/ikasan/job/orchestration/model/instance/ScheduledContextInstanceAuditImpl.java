package org.ikasan.job.orchestration.model.instance;

import java.util.List;

import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAudit;

public class ScheduledContextInstanceAuditImpl implements ScheduledContextInstanceAudit {

    private ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent;
    private List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents;
    private ContextInstance previousContext;
    private ContextInstance updatedContext;

    @Override
    public ContextualisedScheduledProcessEvent getProcessEvent() {
        return this.contextualisedScheduledProcessEvent;
    }

    @Override
    public void setProcessEvent(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) {
        this.contextualisedScheduledProcessEvent = contextualisedScheduledProcessEvent;
    }

    @Override
    public List<SchedulerJobInitiationEvent> getSchedulerJobInitiationEvents() {
        return this.schedulerJobInitiationEvents;
    }

    @Override
    public void setSchedulerJobInitiationEvents(List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents) {
        this.schedulerJobInitiationEvents = schedulerJobInitiationEvents;
    }

    @Override
    public ContextInstance getPreviousContextInstance() {
        return this.previousContext;
    }

    @Override
    public void setPreviousContextInstance(ContextInstance previousContext) {
        this.previousContext = previousContext;
    }

    @Override
    public ContextInstance getUpdatedContextInstance() {
        return this.updatedContext;
    }

    @Override
    public void setUpdatedContextInstance(ContextInstance updatedContext) {
        this.updatedContext = updatedContext;
    }
}
