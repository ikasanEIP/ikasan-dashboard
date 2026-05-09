package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for job lock cache events across cluster nodes.
 * Maintains separate executors for local and remote broadcast operations.
 * Uses WeakHashMap to prevent memory leaks from registered listeners.
 *
 * @author Ikasan Development Team
 */
public class JobLockCacheEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("JobLockCacheEventBroadcaster"));
    static Executor restExecutor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("JobLockCacheEventRestBroadcaster"));

    private static WeakHashMap<JobLockCacheEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static WeakHashMap<JobLockCacheEventRemoteBroadcastListener, Object> remoteListeners =
        new WeakHashMap<>();

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(JobLockCacheEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(JobLockCacheEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers a remote broadcast listener.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void register(JobLockCacheEventRemoteBroadcastListener listener) {
        remoteListeners.put(listener, null);
    }

    /**
     * Unregisters a remote broadcast listener.
     *
     * @param listener the remote listener to unregister
     */
    public static synchronized void unregister(JobLockCacheEventRemoteBroadcastListener listener) {
        remoteListeners.remove(listener);
    }

    /**
     * Broadcasts a job lock cache event to both local and remote listeners.
     *
     * @param event the job lock cache event to broadcast
     */
    public static synchronized void broadcast(final JobLockCacheEvent event) {
        localBroadcast(event);
        remoteBroadcast(event);
    }

    /**
     * Broadcasts a job lock cache event to remote listeners only.
     *
     * @param event the job lock cache event to broadcast
     */
    public static synchronized void remoteBroadcast(final JobLockCacheEvent event) {
        for (final JobLockCacheEventRemoteBroadcastListener remoteListener : remoteListeners.keySet()) {
            restExecutor.execute(() -> remoteListener.receiveBroadcast(event));
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param event the job lock cache event to broadcast locally
     */
    public static synchronized void localBroadcast(final JobLockCacheEvent event) {
        for (final JobLockCacheEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }
}
