package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextInstanceStateChangeEventBroadcaster {
    static Executor executor = Executors.newFixedThreadPool(10, new BroadcasterThreadFactory("ContextInstanceStateChangeEventBroadcaster"));

    private static WeakHashMap<ContextInstanceStateChangeEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextInstanceStateChangeEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(ContextInstanceStateChangeEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final ContextInstanceStateChangeEvent event) {
        for (final ContextInstanceStateChangeEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }
}
