package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ContextInstanceStateChangeEventBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<ContextInstanceStateChangeEvent>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<ContextInstanceStateChangeEvent> listener)
    {
        listeners.add(listener);

        return () ->
        {
            synchronized (ContextInstanceStateChangeEventBroadcaster.class)
            {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(ContextInstanceStateChangeEvent message)
    {
        for (Consumer<ContextInstanceStateChangeEvent> listener : listeners)
        {
            executor.execute(() -> listener.accept(message));
        }
    }
}
