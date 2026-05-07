package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextTemplateEnableDisableEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateEnableDisableEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateEnableDisableEventRestBroadcaster"));

    private static WeakHashMap<ContextTemplateEnableDisableEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<ContextTemplateEnableDisableEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextTemplateEnableDisableEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    public static synchronized void unregister(ContextTemplateEnableDisableEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    public static synchronized void register(ContextTemplateEnableDisableEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    public static synchronized void unregister(ContextTemplateEnableDisableEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    public static synchronized void broadcast(final ContextTemplate contextTemplate) {
        localBroadcast(contextTemplate);
        remoteBroadcast(contextTemplate);
    }

    public static synchronized void remoteBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateEnableDisableEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(contextTemplate));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     */
    public static synchronized void localBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateEnableDisableEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextTemplate));
        }
    }
}
