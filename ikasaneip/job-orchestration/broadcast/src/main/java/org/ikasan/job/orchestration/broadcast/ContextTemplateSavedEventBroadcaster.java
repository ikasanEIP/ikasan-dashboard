package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextTemplateSavedEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateSavedEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateSavedEventRestBroadcaster"));

    private static WeakHashMap<ContextTemplateSavedEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<ContextTemplateSavedEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextTemplateSavedEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    public static synchronized void unregister(ContextTemplateSavedEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    public static synchronized void register(ContextTemplateSavedEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    public static synchronized void unregister(ContextTemplateSavedEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    public static synchronized void broadcast(final ContextTemplate contextTemplate) {
        localBroadcast(contextTemplate);
        remoteBroadcast(contextTemplate);
    }

    public static synchronized void remoteBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateSavedEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveContextTemplateSavedEventBroadcast(contextTemplate));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     */
    public static synchronized void localBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateSavedEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveContextTemplateSavedEventBroadcast(contextTemplate));
        }
    }
}
