package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for new scheduler job events across cluster nodes.
 * Maintains separate executors for local and remote broadcast operations.
 * Uses WeakHashMap to prevent memory leaks from registered listeners.
 *
 * @author Ikasan Development Team
 */
public class NewSchedulerJobEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventRestBroadcaster"));

    private static WeakHashMap<NewSchedulerJobEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<NewSchedulerJobEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

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
     * Registers a remote broadcast listener.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void register(NewSchedulerJobEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    /**
     * Unregisters a remote broadcast listener.
     *
     * @param listener the remote listener to unregister
     */
    public static synchronized void unregister(NewSchedulerJobEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
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
        for (final NewSchedulerJobEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(schedulerJob));
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
