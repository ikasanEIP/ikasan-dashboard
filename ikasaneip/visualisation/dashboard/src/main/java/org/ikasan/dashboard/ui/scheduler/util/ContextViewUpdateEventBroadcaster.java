package org.ikasan.dashboard.ui.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;

import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ContextViewUpdateEventBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor();

    private static WeakHashMap<ContextViewUpdateEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(ContextViewUpdateEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(ContextViewUpdateEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final String contextTemplate) {
        for (final ContextViewUpdateEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextTemplate));
        }
    }
}
