package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.JobConstants;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SchedulerJobStateChangeEventBroadcaster {
    static Executor executor = Executors.newFixedThreadPool(10, new BroadcasterThreadFactory("SchedulerJobStateChangeEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("SchedulerJobStateChangeEventRestBroadcaster"));

    private static WeakHashMap<SchedulerJobStateChangeEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<SchedulerJobStateChangeEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    public static synchronized void register(SchedulerJobStateChangeEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    public static synchronized void unregister(SchedulerJobStateChangeEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    public static synchronized void register(SchedulerJobStateChangeEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    public static synchronized void unregister(SchedulerJobStateChangeEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    public static synchronized void broadcast(final SchedulerJobInstanceStateChangeEvent event) {
        // We do not broadcast start and terminal jobs!
        if(event.getSchedulerJobInstance() != null
            && (!event.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
                !event.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_START_JOB))) {
            localBroadcast(event);
            remoteBroadcast(event);
        }
    }

    public static synchronized void remoteBroadcast(final SchedulerJobInstanceStateChangeEvent event) {
        for (final SchedulerJobStateChangeEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(event));
        }
    }

    /**
     * Called when receiving a state change event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     */
    public static synchronized void localBroadcast(final SchedulerJobInstanceStateChangeEvent event) {
        for (final SchedulerJobStateChangeEventLocalBroadcastListener listener : localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }
}
