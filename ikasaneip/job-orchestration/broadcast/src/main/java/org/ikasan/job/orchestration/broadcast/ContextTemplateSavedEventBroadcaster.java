package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextTemplateSavedEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateSavedEventBroadcaster"));

    private static WeakHashMap<ContextTemplateSavedEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextTemplateSavedEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(ContextTemplateSavedEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateSavedEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveContextTemplateSavedEventBroadcast(contextTemplate));
        }
    }
}
