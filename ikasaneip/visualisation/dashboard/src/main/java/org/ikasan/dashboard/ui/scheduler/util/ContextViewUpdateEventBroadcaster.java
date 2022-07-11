package org.ikasan.dashboard.ui.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ContextViewUpdateEventBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<String>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<String> listener)
    {
        listeners.add(listener);

        return () ->
        {
            synchronized (ContextViewUpdateEventBroadcaster.class)
            {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(String message)
    {
        for (Consumer<String> listener : listeners)
        {
            executor.execute(() -> listener.accept(message));
        }
    }
}
