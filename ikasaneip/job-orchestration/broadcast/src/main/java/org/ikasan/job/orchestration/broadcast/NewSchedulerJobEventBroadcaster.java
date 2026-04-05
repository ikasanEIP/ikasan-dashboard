package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NewSchedulerJobEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventBroadcaster"));

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
