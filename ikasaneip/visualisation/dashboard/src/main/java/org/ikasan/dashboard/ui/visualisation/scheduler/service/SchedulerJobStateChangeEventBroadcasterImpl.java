package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;

import com.vaadin.flow.shared.Registration;

public class SchedulerJobStateChangeEventBroadcasterImpl implements SchedulerJobStateChangeEventBroadcaster<Registration> {

    private final Executor executor;
    private final LinkedList<Consumer<SchedulerJobInstanceStateChangeEvent>> listeners;

    public SchedulerJobStateChangeEventBroadcasterImpl() {
        executor = Executors.newSingleThreadExecutor();
        listeners = new LinkedList<>();
    }

    @Override
    public synchronized Registration register(Consumer<SchedulerJobInstanceStateChangeEvent> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (SchedulerJobInstanceStateChangeEvent.class) {
                listeners.remove(listener);
            }
        };
    }

    @Override
    public synchronized void broadcast(SchedulerJobInstanceStateChangeEvent message) {
        for (Consumer<SchedulerJobInstanceStateChangeEvent> listener : listeners) {
            executor.execute(() -> listener.accept(message));
        }
    }
}
