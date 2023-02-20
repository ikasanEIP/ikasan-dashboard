package org.ikasan.dashboard.ui.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.function.Consumer;

public class ContextInstanceSavedEventBroadcasterImpl implements org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcaster<Registration> {

    @Override
    public synchronized Registration register(Consumer<ContextInstance> listener) {
        return ContextInstanceSavedEventBroadcaster.register(listener);
    }

    @Override
    public synchronized void broadcast(ContextInstance message) {
        ContextInstanceSavedEventBroadcaster.broadcast(message);
    }
}
