package org.ikasan.job.orchestration.model.event;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;

public class SchedulerJobInstanceStateChangeEventImpl extends StateChangeEventImpl implements SchedulerJobInstanceStateChangeEvent {
    private SchedulerJobInstance schedulerJobInstance;
    private ContextInstance contextInstance;

    public SchedulerJobInstanceStateChangeEventImpl() {
    }

    public SchedulerJobInstanceStateChangeEventImpl(SchedulerJobInstance schedulerJobInstance, ContextInstance contextInstance, InstanceStatus previousStatus, InstanceStatus newStatus) {
        super(previousStatus, newStatus);
        this.schedulerJobInstance = schedulerJobInstance;
        this.contextInstance = contextInstance;
    }

    @Override
    public SchedulerJobInstance getSchedulerJobInstance() {
        return schedulerJobInstance;
    }

    public void setSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) {
        this.schedulerJobInstance = schedulerJobInstance;
    }

    @Override
    public ContextInstance getContextInstance() {
        return contextInstance;
    }

    public void setContextInstance(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
    }
}
