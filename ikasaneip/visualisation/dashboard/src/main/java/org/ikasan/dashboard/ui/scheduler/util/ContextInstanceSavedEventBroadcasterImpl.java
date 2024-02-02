package org.ikasan.dashboard.ui.scheduler.util;

import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

public class ContextInstanceSavedEventBroadcasterImpl implements org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcaster {

    @Override
    public synchronized void register(ContextInstanceSavedEventBroadcastListener listener) {
        // should not register listeners via this interface
    }

    @Override
    public synchronized void broadcast(ContextInstance message) {
        ContextInstanceSavedEventBroadcaster.broadcast(message);
    }
}
