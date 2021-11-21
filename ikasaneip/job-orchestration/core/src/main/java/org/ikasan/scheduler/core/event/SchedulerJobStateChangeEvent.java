package org.ikasan.scheduler.core.event;

import org.ikasan.scheduler.core.model.instance.SchedulerJobInstance;
import org.ikasan.scheduler.core.spec.InstanceStatus;

public class SchedulerJobStateChangeEvent {
    private SchedulerJobInstance schedulerJobInstance;
    private InstanceStatus previousStatus;
    private InstanceStatus newStatus;

    public SchedulerJobStateChangeEvent(SchedulerJobInstance schedulerJobInstance, InstanceStatus previousStatus, InstanceStatus newStatus) {
        this.schedulerJobInstance = schedulerJobInstance;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public SchedulerJobInstance getSchedulerJobInstance() {
        return schedulerJobInstance;
    }

    public InstanceStatus getPreviousStatus() {
        return previousStatus;
    }

    public InstanceStatus getNewStatus() {
        return newStatus;
    }
}
