package org.ikasan.dashboard.ui.scheduler.util;

import org.ikasan.dashboard.ui.util.VaadinThreadFactory;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextViewUpdateEventBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextViewUpdateEventBroadcaster"));

    private static WeakHashMap<ContextViewUpdateEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextViewUpdateEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(ContextViewUpdateEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final String contextTemplate) {
        for (final ContextViewUpdateEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextTemplate));
        }
    }
}
