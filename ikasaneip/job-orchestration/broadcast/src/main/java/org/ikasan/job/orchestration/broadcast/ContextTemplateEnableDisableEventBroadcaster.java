package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextTemplateEnableDisableEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateEnableDisableEventBroadcaster"));

    private static WeakHashMap<ContextTemplateEnableDisableEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextTemplateEnableDisableEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(ContextTemplateEnableDisableEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateEnableDisableEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextTemplate));
        }
    }
}
