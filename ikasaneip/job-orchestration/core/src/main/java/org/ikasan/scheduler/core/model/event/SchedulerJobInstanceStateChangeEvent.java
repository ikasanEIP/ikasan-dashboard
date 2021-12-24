package org.ikasan.scheduler.core.model.event;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;

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
