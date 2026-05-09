package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context template enable/disable events across cluster nodes.
 * Maintains separate executors for local and remote broadcast operations.
 * Uses WeakHashMap to prevent memory leaks from registered listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextTemplateEnableDisableEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateEnableDisableEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateEnableDisableEventRestBroadcaster"));

    private static WeakHashMap<ContextTemplateEnableDisableEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<ContextTemplateEnableDisableEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(ContextTemplateEnableDisableEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(ContextTemplateEnableDisableEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers a remote broadcast listener.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void register(ContextTemplateEnableDisableEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    /**
     * Unregisters a remote broadcast listener.
     *
     * @param listener the remote listener to unregister
     */
    public static synchronized void unregister(ContextTemplateEnableDisableEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    /**
     * Broadcasts a context template enable/disable event to both local and remote listeners.
     *
     * @param contextTemplate the context template to broadcast
     */
    public static synchronized void broadcast(final ContextTemplate contextTemplate) {
        localBroadcast(contextTemplate);
        remoteBroadcast(contextTemplate);
    }

    /**
     * Broadcasts a context template enable/disable event to remote listeners only.
     *
     * @param contextTemplate the context template to broadcast
     */
    public static synchronized void remoteBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateEnableDisableEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(contextTemplate));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param contextTemplate the context template to broadcast locally
     */
    public static synchronized void localBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateEnableDisableEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextTemplate));
        }
    }
}
