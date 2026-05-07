package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NewSchedulerJobEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventRestBroadcaster"));

    private static WeakHashMap<NewSchedulerJobEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<NewSchedulerJobEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    public static synchronized void register(NewSchedulerJobEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    public static synchronized void unregister(NewSchedulerJobEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    public static synchronized void register(NewSchedulerJobEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    public static synchronized void unregister(NewSchedulerJobEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    public static synchronized void broadcast(final SchedulerJob schedulerJob) {
        localBroadcast(schedulerJob);
        remoteBroadcast(schedulerJob);
    }

    public static synchronized void remoteBroadcast(final SchedulerJob schedulerJob) {
        for (final NewSchedulerJobEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(schedulerJob));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     */
    public static synchronized void localBroadcast(final SchedulerJob schedulerJob) {
        for (final NewSchedulerJobEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(schedulerJob));
        }
    }
}
