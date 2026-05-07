package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextViewUpdateEventBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextViewUpdateEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextViewUpdateEventRestBroadcaster"));

    private static WeakHashMap<ContextViewUpdateEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<ContextViewUpdateEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextViewUpdateEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    public static synchronized void unregister(ContextViewUpdateEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    public static synchronized void register(ContextViewUpdateEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    public static synchronized void unregister(ContextViewUpdateEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    public static synchronized void broadcast(final String message) {
        localBroadcast(message);
        remoteBroadcast(message);
    }

    public static synchronized void remoteBroadcast(final String message) {
        for (final ContextViewUpdateEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(message));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     */
    public static synchronized void localBroadcast(final String message) {
        for (final ContextViewUpdateEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(message));
        }
    }
}
