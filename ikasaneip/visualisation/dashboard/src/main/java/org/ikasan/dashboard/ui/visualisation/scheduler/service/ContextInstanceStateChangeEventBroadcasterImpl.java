package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import java.util.function.Consumer;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;

import com.vaadin.flow.shared.Registration;

public class ContextInstanceStateChangeEventBroadcasterImpl implements ContextInstanceStateChangeEventBroadcaster<Registration> {


    @Override
    public synchronized Registration register(Consumer<ContextInstanceStateChangeEvent> listener) {
        return org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster.register(listener);
    }

    @Override
    public synchronized void broadcast(ContextInstanceStateChangeEvent message) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster.broadcast(message);
    }
}
