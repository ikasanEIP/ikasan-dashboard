package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventRemoteBroadcastListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context template saved events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextTemplateSavedEventBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextTemplateSavedEventBroadcaster.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextTemplateSavedEventBroadcaster"));

    private final WeakHashMap<ContextTemplateSavedEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private ContextTemplateSavedEventRemoteBroadcastListener remoteListener;

    public static volatile ContextTemplateSavedEventBroadcaster INSTANCE = new ContextTemplateSavedEventBroadcaster();

    /**
     * Private constructor for the ContextTemplateSavedEventBroadcaster class.
     *
     * This constructor enforces the singleton design pattern, ensuring that
     * instances of this class cannot be created directly from outside the class.
     * Use the {@code instance()} method to get the singleton instance.
     */
    private ContextTemplateSavedEventBroadcaster() {}

    /**
     * Retrieves the singleton instance of the {@code ContextTemplateSavedEventBroadcaster}.
     * This method ensures that only one instance of the class exists, in compliance
     * with the singleton design pattern.
     *
     * @return the singleton instance of {@code ContextTemplateSavedEventBroadcaster}
     */
    public static ContextTemplateSavedEventBroadcaster instance() {
        return INSTANCE;
    }

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public synchronized void register(ContextTemplateSavedEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public synchronized void unregister(ContextTemplateSavedEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public synchronized void setRemoteListener(ContextTemplateSavedEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a context template saved event to both local and remote listeners.
     *
     * @param contextTemplate the context template to broadcast
     */
    public synchronized void broadcast(final ContextTemplate contextTemplate) {
        localBroadcast(contextTemplate);
        remoteBroadcast(contextTemplate);
    }

    /**
     * Broadcasts a context template saved event to remote listeners only.
     *
     * @param contextTemplate the context template to broadcast
     */
    public synchronized void remoteBroadcast(final ContextTemplate contextTemplate) {
        if (remoteListener != null) {
            remoteListener.receiveContextTemplateSavedEventBroadcast(contextTemplate);
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param contextTemplate the context template to broadcast locally
     */
    public synchronized void localBroadcast(final ContextTemplate contextTemplate) {
        for (final ContextTemplateSavedEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveContextTemplateSavedEventBroadcast(contextTemplate));
        }
    }

    /**
     * Resets the singleton instance of the {@code ContextInstanceDlqEventBroadcaster}.
     *
     * <p>Shut down the current instance's executor and wait for any already queued or in-flight
     * broadcasts to finish, this guarantees no broadcast is dispatched while the reset
     * is still running — or silently lost — once this method returns. Prevents potential
     * non-deamon thread leak
     * This method is {@code synchronized}, so it cannot run concurrently with
     * {@link #register}, {@link #unregister}, {@link #broadcast}, {@link #remoteBroadcast}
     * or {@link #localBroadcast} on the same instance — no undefined-ordering race
     * between a reset and an in-progress broadcast.
     *
     * <p>A caller hat already holds a reference to the old instance (obtained via {@link #instance()}
     * before this call) continues to hold a reference to a now-terminated object. However,
     * since the old instance's executor is shut down, any further
     * {@link #broadcast} or {@link #localBroadcast} call made through a stale reference will
     * throw {@link java.util.concurrent.RejectedExecutionException} rather than quietly
     * succeeding into an orphaned instance nobody observes.
     */
    private synchronized void reset() {
        ExecutorDrainer.shutdownAndAwait(executor, LOGGER);
        INSTANCE = new ContextTemplateSavedEventBroadcaster();
    }
}
