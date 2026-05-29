package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context view update events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextViewUpdateEventBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextViewUpdateEventBroadcaster"));

    private static final WeakHashMap<ContextViewUpdateEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static ContextViewUpdateEventRemoteBroadcastListener remoteListener;

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(ContextViewUpdateEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(ContextViewUpdateEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void setRemoteListener(ContextViewUpdateEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a context view update message to both local and remote listeners.
     *
     * @param message the message to broadcast
     */
    public static synchronized void broadcast(final String message) {
        localBroadcast(message);
        remoteBroadcast(message);
    }

    /**
     * Broadcasts a context view update message to remote listeners only.
     *
     * @param message the message to broadcast
     */
    public static synchronized void remoteBroadcast(final String message) {
        if (remoteListener != null) {
            remoteListener.receiveBroadcast(message);
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param message the message to broadcast locally
     */
    public static synchronized void localBroadcast(final String message) {
        for (final ContextViewUpdateEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(message));
        }
    }
}
