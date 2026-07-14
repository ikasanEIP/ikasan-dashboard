package org.ikasan.dashboard.cache;

import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CacheStateBroadcaster {
    private final Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("CacheStateBroadcaster"));

    private final WeakHashMap<CacheStateBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static CacheStateBroadcaster INSTANCE = new CacheStateBroadcaster();

    /**
     * Private constructor for the CacheStateBroadcaster class.
     * This constructor enforces the singleton design pattern use the {@code instance()}
     * method to get the singleton instance.
     */
    private CacheStateBroadcaster() {}

    /**
     * Ensures that only one instance of the class exists, in compliance
     * with the singleton design pattern.
     * @return the singleton instance of {@code CacheStateBroadcaster}
     */
    public static CacheStateBroadcaster instance() {
        return INSTANCE;
    }

    public synchronized void register(CacheStateBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public synchronized void unregister(CacheStateBroadcastListener listener) {
        listeners.remove(listener);
    }

    public synchronized void broadcast(final FlowState flowState) {
        for (final CacheStateBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveCacheStateBroadcast(flowState));
        }
    }

    /**
     * Resets the singleton instance of the {@code CacheStateBroadcaster}.
     *
     * This method creates a new instance of the {@code CacheStateBroadcaster}
     * and assigns it to the {@code INSTANCE} field, effectively clearing any existing
     * listeners or configurations associated with the previous instance.
     *
     * Use this method cautiously as it overrides the previous state of the singleton,
     * which may affect ongoing operations.
     */
    private void reset() {
        INSTANCE = new CacheStateBroadcaster();
    }
}
