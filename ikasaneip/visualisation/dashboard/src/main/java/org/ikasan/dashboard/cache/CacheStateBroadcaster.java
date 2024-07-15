package org.ikasan.dashboard.cache;

import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CacheStateBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("CacheStateBroadcaster"));

    private static WeakHashMap<CacheStateBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(CacheStateBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(CacheStateBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final FlowState flowState) {
        for (final CacheStateBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveCacheStateBroadcast(flowState));
        }
    }
}
