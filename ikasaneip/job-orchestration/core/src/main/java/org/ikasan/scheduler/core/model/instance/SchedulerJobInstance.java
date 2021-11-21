package org.ikasan.scheduler.core.model.instance;

import org.ikasan.scheduler.core.model.context.SchedulerJob;
import org.ikasan.scheduler.core.spec.StatefulEntity;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

public class SchedulerJobInstance extends SchedulerJob implements StatefulEntity {
    private boolean held = false;
    private boolean skip = false;
    private boolean initiationEventRaised = false;
    private InstanceStatus status;
    private ScheduledProcessEvent scheduledProcessEvent;

    public SchedulerJobInstance() {
        status = InstanceStatus.WAITING;
    }

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

    public boolean isInitiationEventRaised() {
        return initiationEventRaised;
    }

    public void setInitiationEventRaised(boolean initiationEventRaised) {
        this.initiationEventRaised = initiationEventRaised;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public ScheduledProcessEvent getScheduledProcessEvent() {
        return scheduledProcessEvent;
    }

    public void setScheduledProcessEvent(ScheduledProcessEvent scheduledProcessEvent) {
        this.scheduledProcessEvent = scheduledProcessEvent;
    }
}
