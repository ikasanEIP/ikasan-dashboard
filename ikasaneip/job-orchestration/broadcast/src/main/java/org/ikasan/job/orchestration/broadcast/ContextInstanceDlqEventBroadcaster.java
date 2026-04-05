package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextInstanceDlqEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventBroadcaster"));

    /**
     * A static WeakHashMap that holds instances of ContextInstanceDlqEventBroadcastListener as keys.
     * This map is used to manage listeners for broadcasting events related to ContextInstance events.
     */
    private static WeakHashMap<ContextInstanceDlqEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    /**
     * Registers a ContextInstanceDlqEventBroadcastListener to receive broadcast events related to ContextInstance events.
     * The listener will be added to a WeakHashMap for broadcasting purposes.
     *
     * @param listener the listener to be registered
     */
    public static synchronized void register(ContextInstanceDlqEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    /**
     * Removes the specified ContextInstanceDlqEventBroadcastListener from the list of listeners.
     *
     * @param listener the listener to be unregistered
     */
    public static synchronized void unregister(ContextInstanceDlqEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    /**
     * Broadcasts a context instance to all registered listeners in a synchronized manner.
     *
     * @param contextInstance the context instance to broadcast
     * @param The context instance to be broadcasted to all registered listeners
     */
    public static synchronized void broadcast(final ContextInstance contextInstance) {
        for (final ContextInstanceDlqEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextInstance));
        }
    }
}
