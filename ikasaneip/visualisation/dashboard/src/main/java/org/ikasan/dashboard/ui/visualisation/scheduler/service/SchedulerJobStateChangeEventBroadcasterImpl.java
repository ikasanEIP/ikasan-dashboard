package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;

public class SchedulerJobStateChangeEventBroadcasterImpl implements SchedulerJobStateChangeEventBroadcaster {

    @Override
    public synchronized void register(SchedulerJobStateChangeEventBroadcastListener listener) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster.register(listener);
    }

    @Override
    public synchronized void broadcast(SchedulerJobInstanceStateChangeEvent message) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster.broadcast(message);
    }
}
