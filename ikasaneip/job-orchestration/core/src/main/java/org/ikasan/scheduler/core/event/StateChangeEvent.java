package org.ikasan.scheduler.core.event;

import org.ikasan.scheduler.core.spec.InstanceStatus;

public abstract class StateChangeEvent {
    protected InstanceStatus previousStatus;
    protected InstanceStatus newStatus;

    public StateChangeEvent(InstanceStatus previousStatus, InstanceStatus newStatus) {
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public InstanceStatus getPreviousStatus() {
        return previousStatus;
    }

    public InstanceStatus getNewStatus() {
        return newStatus;
    }
}
