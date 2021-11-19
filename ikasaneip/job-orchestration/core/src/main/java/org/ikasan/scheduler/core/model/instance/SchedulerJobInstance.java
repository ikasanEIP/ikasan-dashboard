package org.ikasan.scheduler.core.model.instance;

import org.ikasan.scheduler.core.model.context.SchedulerJob;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

public class SchedulerJobInstance extends SchedulerJob {
    private boolean held = false;
    private boolean skip = false;
    private boolean completedSuccessfully = false;
    private boolean initiationEventRaised = false;
    private ScheduledProcessEvent scheduledProcessEvent;


    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public boolean isCompletedSuccessfully() {
        return completedSuccessfully;
    }

    public void setCompletedSuccessfully(boolean completedSuccessfully) {
        this.completedSuccessfully = completedSuccessfully;
    }

    public boolean isInitiationEventRaised() {
        return initiationEventRaised;
    }

    public void setInitiationEventRaised(boolean initiationEventRaised) {
        this.initiationEventRaised = initiationEventRaised;
    }

    public ScheduledProcessEvent getScheduledProcessEvent() {
        return scheduledProcessEvent;
    }

    public void setScheduledProcessEvent(ScheduledProcessEvent scheduledProcessEvent) {
        this.scheduledProcessEvent = scheduledProcessEvent;
    }
}
