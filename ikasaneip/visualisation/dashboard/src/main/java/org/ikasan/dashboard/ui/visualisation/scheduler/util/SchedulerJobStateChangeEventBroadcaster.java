package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SchedulerJobStateChangeEventBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<SchedulerJobInstanceStateChangeEvent>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<SchedulerJobInstanceStateChangeEvent> listener)
    {
        listeners.add(listener);

        return () ->
        {
            synchronized (SchedulerJobStateChangeEventBroadcaster.class)
            {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(SchedulerJobInstanceStateChangeEvent message)
    {
        for (Consumer<SchedulerJobInstanceStateChangeEvent> listener : listeners)
        {
            executor.execute(() -> listener.accept(message));
        }
    }
}
