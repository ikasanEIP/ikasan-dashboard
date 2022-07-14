package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ContextMachineUpdateBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<ContextInstance>> listeners = new LinkedList<>();

    public static synchronized void register(Consumer<ContextInstance> listener)
    {
        listeners.add(listener);
    }

    public static synchronized void broadcast(ContextInstance message)
    {
        for (Consumer<ContextInstance> listener : listeners)
        {
            executor.execute(() -> listener.accept(message));
        }
    }
}
