package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context instance DLQ (Dead Letter Queue) events across cluster nodes.
 * Maintains separate executors for local and remote broadcast operations.
 * Uses WeakHashMap to prevent memory leaks from registered listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceDlqEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextInstanceDlqEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextInstanceDlqEventRestBroadcaster"));

    private static WeakHashMap<ContextInstanceDlqEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<ContextInstanceDlqEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(ContextInstanceDlqEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(ContextInstanceDlqEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers a remote broadcast listener.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void register(ContextInstanceDlqEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    /**
     * Unregisters a remote broadcast listener.
     *
     * @param listener the remote listener to unregister
     */
    public static synchronized void unregister(ContextInstanceDlqEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    /**
     * Broadcasts a context instance DLQ event to both local and remote listeners.
     *
     * @param contextInstance the context instance to broadcast
     */
    public static synchronized void broadcast(final ContextInstance contextInstance) {
        localBroadcast(contextInstance);
        remoteBroadcast(contextInstance);
    }

    /**
     * Broadcasts a context instance DLQ event to remote listeners only.
     *
     * @param contextInstance the context instance to broadcast
     */
    public static synchronized void remoteBroadcast(final ContextInstance contextInstance) {
        for (final ContextInstanceDlqEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(contextInstance));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param contextInstance the context instance to broadcast locally
     */
    public static synchronized void localBroadcast(final ContextInstance contextInstance) {
        for (final ContextInstanceDlqEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextInstance));
        }
    }
}
