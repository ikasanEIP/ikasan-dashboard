package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context instance saved events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceSavedEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextInstanceSavedEventBroadcaster"));

    private static final WeakHashMap<ContextInstanceSavedEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static ContextInstanceSavedEventRemoteBroadcastListener remoteListener;

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(ContextInstanceSavedEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(ContextInstanceSavedEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void setRemoteListener(ContextInstanceSavedEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a context instance saved event to both local and remote listeners.
     *
     * @param contextInstance the context instance to broadcast
     */
    public static synchronized void broadcast(final ContextInstance contextInstance) {
        localBroadcast(contextInstance);
        remoteBroadcast(contextInstance);
    }

    /**
     * Broadcasts a context instance saved event to remote listeners only.
     *
     * @param contextInstance the context instance to broadcast
     */
    public static synchronized void remoteBroadcast(final ContextInstance contextInstance) {
        if (remoteListener != null) {
            remoteListener.receiveBroadcast(contextInstance);
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param contextInstance the context instance to broadcast locally
     */
    public static synchronized void localBroadcast(final ContextInstance contextInstance) {
        for (final ContextInstanceSavedEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextInstance));
        }
    }
}
