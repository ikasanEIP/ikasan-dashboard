package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for new scheduler job events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class NewSchedulerJobEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventBroadcaster"));

    private static final WeakHashMap<NewSchedulerJobEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static NewSchedulerJobEventRemoteBroadcastListener remoteListener;

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(NewSchedulerJobEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(NewSchedulerJobEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void setRemoteListener(NewSchedulerJobEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a new scheduler job event to both local and remote listeners.
     *
     * @param schedulerJob the scheduler job to broadcast
     */
    public static synchronized void broadcast(final SchedulerJob schedulerJob) {
        localBroadcast(schedulerJob);
        remoteBroadcast(schedulerJob);
    }

    /**
     * Broadcasts a new scheduler job event to remote listeners only.
     *
     * @param schedulerJob the scheduler job to broadcast
     */
    public static synchronized void remoteBroadcast(final SchedulerJob schedulerJob) {
        if (remoteListener != null) {
            remoteListener.receiveBroadcast(schedulerJob);
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param schedulerJob the scheduler job to broadcast locally
     */
    public static synchronized void localBroadcast(final SchedulerJob schedulerJob) {
        for (final NewSchedulerJobEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(schedulerJob));
        }
    }
}
