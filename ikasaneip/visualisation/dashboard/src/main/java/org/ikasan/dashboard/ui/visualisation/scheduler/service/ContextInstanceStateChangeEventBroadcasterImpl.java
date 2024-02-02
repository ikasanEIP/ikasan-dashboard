package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;

public class ContextInstanceStateChangeEventBroadcasterImpl implements ContextInstanceStateChangeEventBroadcaster {

    @Override
    public void register(ContextInstanceStateChangeEventBroadcastListener listener) {
        // should not register listeners via this interface
        throw new UnsupportedOperationException("ContextInstanceStateChangeEventBroadcastListener listeners should not" +
            " be registered via this method");
    }

    @Override
    public synchronized void broadcast(ContextInstanceStateChangeEvent message) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster.broadcast(message);
    }
}
