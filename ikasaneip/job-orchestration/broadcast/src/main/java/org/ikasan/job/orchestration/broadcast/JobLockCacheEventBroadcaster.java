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
    private final ExecutorService executor = Executors.newSingleThreadExecutor
        (new BroadcasterThreadFactory("JobLockCacheEventBroadcaster"));

    private final WeakHashMap<JobLockCacheEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private JobLockCacheEventRemoteBroadcastListener remoteListener;

    public static JobLockCacheEventBroadcaster INSTANCE = new JobLockCacheEventBroadcaster();

    /**
     * Private constructor for the JobLockCacheEventBroadcaster class.
     *
     * This constructor enforces the singleton design pattern, ensuring that
     * instances of this class cannot be created directly from outside the class.
     * Use the {@code instance()} method to get the singleton instance.
     */
    private JobLockCacheEventBroadcaster () {}

    /**
     * Retrieves the singleton instance of the {@code JobLockCacheEventBroadcaster}.
     * This method ensures that only one instance of the class exists, in compliance
     * with the singleton design pattern.
     *
     * @return the singleton instance of {@code JobLockCacheEventBroadcaster}
     */
    public static JobLockCacheEventBroadcaster instance() {
        return INSTANCE;
    }
    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public synchronized void register(JobLockCacheEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public synchronized void unregister(JobLockCacheEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public synchronized void setRemoteListener(JobLockCacheEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a job lock cache event to both local and remote listeners.
     *
     * @param event the job lock cache event to broadcast
     */
    public synchronized void broadcast(final JobLockCacheEvent event) {
        localBroadcast(event);
        remoteBroadcast(event);
    }

    /**
     * Broadcasts a job lock cache event to remote listeners only.
     *
     * @param event the job lock cache event to broadcast
     */
    public synchronized void remoteBroadcast(final JobLockCacheEvent event) {
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
    public synchronized void localBroadcast(final JobLockCacheEvent event) {
        for (final JobLockCacheEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }

    /**
     * Resets the singleton instance of the {@code JobLockCacheEventBroadcaster}.
     *
     * This method creates a new instance of the {@code JobLockCacheEventBroadcaster}
     * and assigns it to the {@code INSTANCE} field, effectively clearing any existing
     * listeners or configurations associated with the previous instance.
     *
     * Use this method cautiously as it overrides the previous state of the singleton,
     * which may affect ongoing operations.
     */
    private void reset() {
        INSTANCE = new JobLockCacheEventBroadcaster();
    }
}
