package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;

public class SchedulerJobStateChangeEventBroadcasterImpl implements SchedulerJobStateChangeEventBroadcaster {

    @Override
    public synchronized void register(SchedulerJobStateChangeEventBroadcastListener listener) {
        // should not register listeners via this interface
        throw new UnsupportedOperationException("SchedulerJobStateChangeEventBroadcastListener listeners should not" +
            " be registered via this method");
    }

    @Override
    public synchronized void broadcast(SchedulerJobInstanceStateChangeEvent message) {
        org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster.broadcast(message);
    }
}
