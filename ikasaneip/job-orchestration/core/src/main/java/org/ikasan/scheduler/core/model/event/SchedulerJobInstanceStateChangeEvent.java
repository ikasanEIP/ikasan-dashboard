package org.ikasan.scheduler.core.model.event;

import org.ikasan.scheduler.core.model.instance.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class SchedulerJobInstanceStateChangeEvent extends StateChangeEvent {
    private SchedulerJobInstance schedulerJobInstance;

    public SchedulerJobInstanceStateChangeEvent(SchedulerJobInstance schedulerJobInstance, InstanceStatus previousStatus, InstanceStatus newStatus) {
        super(previousStatus, newStatus);
        this.schedulerJobInstance = schedulerJobInstance;
    }

    public SchedulerJobInstance getSchedulerJobInstance() {
        return schedulerJobInstance;
    }
}
