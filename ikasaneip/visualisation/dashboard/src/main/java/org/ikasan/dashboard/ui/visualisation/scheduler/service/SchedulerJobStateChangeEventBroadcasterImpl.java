package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;

import java.util.function.Consumer;

public class SchedulerJobStateChangeEventBroadcasterImpl implements SchedulerJobStateChangeEventBroadcaster<Registration> {

    @Override
    public synchronized Registration register(Consumer<SchedulerJobInstanceStateChangeEvent> listener) {
        return org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster.register(listener);
    }

    @Override
    public synchronized void broadcast(SchedulerJobInstanceStateChangeEvent message) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster.broadcast(message);
    }
}
