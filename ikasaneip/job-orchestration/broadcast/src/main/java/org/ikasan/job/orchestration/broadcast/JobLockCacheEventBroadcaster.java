package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventRemoteBroadcastListener;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Broadcaster for job lock cache events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class JobLockCacheEventBroadcaster {
    static ExecutorService executor = Executors.newSingleThreadExecutor
        (new BroadcasterThreadFactory("JobLockCacheEventBroadcaster"));

    private static final WeakHashMap<JobLockCacheEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static JobLockCacheEventRemoteBroadcastListener remoteListener;

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
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void setRemoteListener(JobLockCacheEventRemoteBroadcastListener listener) {
        remoteListener = listener;
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
        if (remoteListener != null) {
            remoteListener.receiveBroadcast(event);
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

    /**
     * Resets the executor service used for broadcasting job lock cache events.
     * This involves shutting down the current executor service, releasing its resources,
     * and initializing a new single-threaded executor with a dedicated thread factory.
     * The reset ensures a fresh executor for handling broadcast tasks.
     *
     * Note:
     * - Any pending tasks in the current executor will be terminated abruptly.
     * - A new executor will be created with a thread factory that assigns
     *   threads a prefix name "JobLockCacheEventBroadcaster".
     */
    public static void resetExecutorService() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(15, TimeUnit.SECONDS);
        executor.close();

        executor = Executors.newSingleThreadExecutor
            (new BroadcasterThreadFactory("JobLockCacheEventBroadcaster"));
    }
}
