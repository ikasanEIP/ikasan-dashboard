package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class JobLockCacheEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor();

    private static WeakHashMap<JobLockCacheEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(JobLockCacheEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(JobLockCacheEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final JobLockCacheEvent event) {
        for (final JobLockCacheEventBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }
}
