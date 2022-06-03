package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;

import com.vaadin.flow.shared.Registration;

public class ContextInstanceStateChangeEventBroadcasterImpl implements ContextInstanceStateChangeEventBroadcaster<Registration> {

    private final Executor executor;
    private final LinkedList<Consumer<ContextInstanceStateChangeEvent>> listeners;

    public ContextInstanceStateChangeEventBroadcasterImpl() {
        executor = Executors.newSingleThreadExecutor();
        listeners = new LinkedList<>();
    }

    @Override
    public synchronized Registration register(Consumer<ContextInstanceStateChangeEvent> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (ContextInstanceStateChangeEventBroadcasterImpl.class) {
                listeners.remove(listener);
            }
        };
    }

    @Override
    public synchronized void broadcast(ContextInstanceStateChangeEvent message) {
        for (Consumer<ContextInstanceStateChangeEvent> listener : listeners) {
            executor.execute(() -> listener.accept(message));
        }
    }
}
