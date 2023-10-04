package org.ikasan.dashboard.ui.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.util.VaadimThreadFactory;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NewSchedulerJobEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new VaadimThreadFactory("NewSchedulerJobEventBroadcaster"));

    private static WeakHashMap<NewSchedulerJobEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(NewSchedulerJobEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(NewSchedulerJobEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final SchedulerJob schedulerJob) {
        for (final NewSchedulerJobEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(schedulerJob));
        }
    }
}
