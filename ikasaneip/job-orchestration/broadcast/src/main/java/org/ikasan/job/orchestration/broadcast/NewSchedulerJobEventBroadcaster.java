package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Broadcaster for new scheduler job events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class NewSchedulerJobEventBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewSchedulerJobEventBroadcaster.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("NewSchedulerJobEventBroadcaster"));

    private final WeakHashMap<NewSchedulerJobEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private NewSchedulerJobEventRemoteBroadcastListener remoteListener;

    public static volatile NewSchedulerJobEventBroadcaster INSTANCE = new NewSchedulerJobEventBroadcaster();

    /**
     * Private constructor for the NewSchedulerJobEventBroadcaster class.
     * This constructor enforces the singleton design pattern use the {@code instance()}
     * method to get the singleton instance.
     */
    private NewSchedulerJobEventBroadcaster() {}

    /**
     * Ensures that only one instance of the class exists, in compliance
     * with the singleton design pattern.
     * @return the singleton instance of {@code NewSchedulerJobEventBroadcaster}
     */
    public static NewSchedulerJobEventBroadcaster instance() {
        return INSTANCE;
    }

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public synchronized void register(NewSchedulerJobEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public synchronized void unregister(NewSchedulerJobEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public synchronized void setRemoteListener(NewSchedulerJobEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a new scheduler job event to both local and remote listeners.
     *
     * @param schedulerJob the scheduler job to broadcast
     */
    public synchronized void broadcast(final SchedulerJob schedulerJob) {
        localBroadcast(schedulerJob);
        remoteBroadcast(schedulerJob);
    }

    /**
     * Broadcasts a new scheduler job event to remote listeners only.
     *
     * @param schedulerJob the scheduler job to broadcast
     */
    public synchronized void remoteBroadcast(final SchedulerJob schedulerJob) {
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
    public synchronized void localBroadcast(final SchedulerJob schedulerJob) {
        for (final NewSchedulerJobEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(schedulerJob));
        }
    }

    /**
     * Resets the singleton instance of the {@code NewSchedulerJobEventBroadcaster}.
     *
     * <p>Shut down the current instance's executor and wait for any already queued or in-flight
     * broadcasts to finish, this guarantees no broadcast is dispatched while the reset
     * is still running — or silently lost — once this method returns. Prevents potential
     * non-deamon thread leak.
     * This method is {@code synchronized}, so it cannot run concurrently with
     * {@link #register}, {@link #unregister}, {@link #broadcast}, {@link #remoteBroadcast}
     * or {@link #localBroadcast} on the same instance — no undefined-ordering race
     * between a reset and an in-progress broadcast.
     *
     * <p>A caller that already holds a reference to the old instance (obtained via {@link #instance()}
     * before this call) continues to hold a reference to a now-terminated object. However,
     * since the old instance's executor is shut down, any further
     * {@link #broadcast} or {@link #localBroadcast} call made through a stale reference will
     * throw {@link java.util.concurrent.RejectedExecutionException} rather than quietly
     * succeeding into an orphaned instance nobody observes.
     */
    private synchronized void reset() {
        ExecutorDrainer.shutdownAndAwait(executor, LOGGER);
        INSTANCE = new NewSchedulerJobEventBroadcaster();
    }
}
