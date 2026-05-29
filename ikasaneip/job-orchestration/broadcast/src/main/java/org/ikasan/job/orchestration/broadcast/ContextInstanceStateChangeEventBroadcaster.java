package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context instance state change events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceStateChangeEventBroadcaster {
    static Executor executor = Executors.newFixedThreadPool(10, new BroadcasterThreadFactory("ContextInstanceStateChangeEventBroadcaster"));

    private static final WeakHashMap<ContextInstanceStateChangeEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static ContextInstanceStateChangeEventRemoteBroadcastListener remoteListener;

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(ContextInstanceStateChangeEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(ContextInstanceStateChangeEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void setRemoteListener(ContextInstanceStateChangeEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a context instance state change event to both local and remote listeners.
     *
     * @param event the state change event to broadcast
     */
    public static synchronized void broadcast(final ContextInstanceStateChangeEvent event) {
        localBroadcast(event);
        remoteBroadcast(event);
    }

    /**
     * Broadcasts a context instance state change event to remote listeners only.
     *
     * @param event the state change event to broadcast
     */
    public static synchronized void remoteBroadcast(final ContextInstanceStateChangeEvent event) {
        if (remoteListener != null) {
            remoteListener.receiveBroadcast(event);
        }
    }

    /**
     * Called when receiving a state change event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param event the state change event to broadcast locally
     */
    public static synchronized void localBroadcast(final ContextInstanceStateChangeEvent event) {
        for (final ContextInstanceStateChangeEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }
}
