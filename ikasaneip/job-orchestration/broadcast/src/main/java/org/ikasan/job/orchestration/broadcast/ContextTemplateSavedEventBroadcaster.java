package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context template saved events across cluster nodes.
 * Maintains separate executors for local and remote broadcast operations.
 * Uses WeakHashMap to prevent memory leaks from registered listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextTemplateSavedEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateSavedEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateSavedEventRestBroadcaster"));

    private static WeakHashMap<ContextTemplateSavedEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<ContextTemplateSavedEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(ContextTemplateSavedEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(ContextTemplateSavedEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers a remote broadcast listener.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void register(ContextTemplateSavedEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    /**
     * Unregisters a remote broadcast listener.
     *
     * @param listener the remote listener to unregister
     */
    public static synchronized void unregister(ContextTemplateSavedEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    /**
     * Broadcasts a context template saved event to both local and remote listeners.
     *
     * @param contextTemplate the context template to broadcast
     */
    public static synchronized void broadcast(final ContextTemplate contextTemplate) {
        localBroadcast(contextTemplate);
        remoteBroadcast(contextTemplate);
    }

    /**
     * Broadcasts a context template saved event to remote listeners only.
     *
     * @param contextTemplate the context template to broadcast
     */
    public static synchronized void remoteBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateSavedEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveContextTemplateSavedEventBroadcast(contextTemplate));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param contextTemplate the context template to broadcast locally
     */
    public static synchronized void localBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateSavedEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveContextTemplateSavedEventBroadcast(contextTemplate));
        }
    }
}
