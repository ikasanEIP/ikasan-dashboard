package org.ikasan.dashboard.ui.scheduler.util;

import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextInstanceSavedEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextInstanceSavedEventBroadcaster"));

    private static WeakHashMap<ContextInstanceSavedEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextInstanceSavedEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(ContextInstanceSavedEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final ContextInstance contextInstance) {
        for (final ContextInstanceSavedEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextInstance));
        }
    }
}
