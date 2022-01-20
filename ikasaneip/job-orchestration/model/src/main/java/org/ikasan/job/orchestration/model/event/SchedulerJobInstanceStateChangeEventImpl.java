package org.ikasan.job.orchestration.model.event;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;

public class SchedulerJobInstanceStateChangeEventImpl extends StateChangeEventImpl implements SchedulerJobInstanceStateChangeEvent {
    private SchedulerJobInstance schedulerJobInstance;

    public SchedulerJobInstanceStateChangeEventImpl(SchedulerJobInstance schedulerJobInstance, InstanceStatus previousStatus, InstanceStatus newStatus) {
        super(previousStatus, newStatus);
        this.schedulerJobInstance = schedulerJobInstance;
    }

    public SchedulerJobInstance getSchedulerJobInstance() {
        return schedulerJobInstance;
    }
}
