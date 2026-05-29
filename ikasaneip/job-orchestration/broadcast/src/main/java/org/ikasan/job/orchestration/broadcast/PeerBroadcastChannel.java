package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pairs a {@link ClusterEventService} (representing one peer dashboard node) with a dedicated
 * single-threaded executor and a per-peer circuit breaker.
 *
 * <h3>Execution lane</h3>
 * Each cluster peer gets exactly one execution lane (a single-threaded executor with its own bounded
 * queue). All broadcast types for that peer share the same lane, so:
 * <ul>
 *   <li>Broadcasts to that peer are serialised — events arrive in submission order.</li>
 *   <li>A slow or unreachable peer can only block its own lane; other peers are unaffected.</li>
 *   <li>Queue eviction (oldest-first) is per-peer, preventing one peer's backlog from
 *       consuming capacity meant for others.</li>
 * </ul>
 *
 * <h3>Circuit breaker</h3>
 * <ul>
 *   <li><b>CLOSED</b> — normal operation; all tasks are submitted to the executor.</li>
 *   <li><b>OPEN</b> — after {@code failureThreshold} consecutive network failures the circuit
 *       opens. Submitted tasks are dropped immediately (not queued) until the open window
 *       expires.</li>
 *   <li><b>HALF_OPEN</b> — after {@code openDurationMs} milliseconds exactly one probe task is
 *       allowed through via a CAS on the state. Concurrent submitters that lose the CAS treat
 *       the circuit as still open and drop their task. A successful probe resets the circuit
 *       (CLOSED); a failed probe restarts the open window (OPEN).</li>
 * </ul>
 *
 * <p>The circuit breaker only trips on network-level failures ({@code ResourceAccessException}),
 * not on HTTP error responses (4xx/5xx), because those indicate the peer is reachable.</p>
 *
 * <h3>Lifecycle</h3>
 * Call {@link #shutdown()} when the Spring context closes. This interrupts the lane thread and
 * discards any queued tasks, preventing non-daemon threads from keeping the JVM alive.
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * ikasan.dashboard.cluster.peer-broadcast-queue-capacity  (default 1000)
 * ikasan.dashboard.cluster.http.connect-timeout-ms        (default 3000)
 * ikasan.dashboard.cluster.http.read-timeout-ms           (default 10000)
 * ikasan.dashboard.cluster.circuit-breaker.failure-threshold  (default 5)
 * ikasan.dashboard.cluster.circuit-breaker.open-duration-ms   (default 30000)
 * </pre>
 *
 * @author Ikasan Development Team
 */
public final class PeerBroadcastChannel implements ClusterEventBroadcastChannel {

    private static final Logger logger = LoggerFactory.getLogger(PeerBroadcastChannel.class);

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final ClusterEventService service;
    private final ThreadPoolExecutor executor;
    private final String peerUrl;
    private final int failureThreshold;
    private final long openDurationMs;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenedAt = new AtomicLong(0L);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    /**
     * Creates a channel for one cluster peer.
     *
     * @param service          REST client that publishes events to the peer
     * @param queueCapacity    maximum pending tasks before the oldest is evicted
     * @param peerUrl          base URL of the cluster peer, used only for log messages
     * @param failureThreshold consecutive network failures before the circuit opens
     * @param openDurationMs   milliseconds the circuit stays open before a probe is attempted
     */
    public PeerBroadcastChannel(ClusterEventService service, int queueCapacity,
                                String peerUrl, int failureThreshold, long openDurationMs) {
        this.service = service;
        this.peerUrl = peerUrl;
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
        this.executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new BroadcasterThreadFactory("PeerBroadcaster[" + sanitise(peerUrl) + "]"),
            (r, exec) -> {
                exec.getQueue().poll();
                boolean accepted = !exec.isShutdown() && exec.getQueue().offer(r);
                if (accepted) {
                    logger.warn(
                        "Peer broadcast queue at capacity for [{}]. Oldest event evicted to make room " +
                        "for the latest. No scheduler data is lost — the cluster peer's UI will recover " +
                        "from Solr when it reconnects. " +
                        "Increase ikasan.dashboard.cluster.peer-broadcast-queue-capacity if this " +
                        "occurs during normal operation.", peerUrl);
                } else {
                    logger.warn(
                        "Peer broadcast queue at capacity for [{}] and incoming event could not be " +
                        "queued (executor shutting down or concurrent drain) — both evicted and " +
                        "incoming events dropped.", peerUrl);
                }
            });
    }

    /**
     * Submits a broadcast task to this peer's execution lane.
     *
     * <p>Drops the task immediately if the circuit is OPEN or if another probe is already
     * in flight (HALF_OPEN). Only one caller wins the CAS to become the half-open probe.</p>
     *
     * @param task the broadcast action; must throw a {@link RuntimeException} on network
     *             failure for the circuit breaker to track consecutive failures
     */
    @Override
    public void submit(Runnable task) {
        if (isCircuitOpen()) {
            logger.debug("Circuit open for peer [{}] — broadcast dropped.", peerUrl);
            return;
        }
        executor.execute(() -> {
            try {
                task.run();
                onSuccess();
            } catch (RuntimeException e) {
                onFailure(e);
            }
        });
    }

    /**
     * Shuts down the peer's execution lane. In-flight tasks are interrupted and queued tasks
     * are discarded. Should be called from a Spring {@code @PreDestroy} to prevent non-daemon
     * threads from keeping the JVM alive after context shutdown.
     */
    @Override
    public void shutdown() {
        List<Runnable> pending = executor.shutdownNow();
        if (!pending.isEmpty()) {
            logger.debug("Peer broadcast channel for [{}] shut down with {} pending task(s) discarded.",
                peerUrl, pending.size());
        }
    }

    /**
     * Returns the underlying executor for metrics and diagnostics (queue depth, active count, etc.).
     * Callers must not modify the executor's configuration after construction.
     */
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /** Returns the {@link ClusterEventService} that this channel delivers events to. */
    @Override
    public ClusterEventService service() {
        return service;
    }

    /** Returns the peer URL for logging and diagnostics. */
    public String getPeerUrl() {
        return peerUrl;
    }

    // -------------------------------------------------------------------------
    // Circuit breaker internals
    // -------------------------------------------------------------------------

    /**
     * Returns true if the broadcast should be dropped.
     * <ul>
     *   <li>CLOSED → false (allow)</li>
     *   <li>HALF_OPEN → true (probe already in flight)</li>
     *   <li>OPEN, window not elapsed → true (suppress)</li>
     *   <li>OPEN, window elapsed, CAS wins → false (this caller is the probe, state → HALF_OPEN)</li>
     *   <li>OPEN, window elapsed, CAS loses → true (another caller is the probe)</li>
     * </ul>
     */
    private boolean isCircuitOpen() {
        State current = state.get();
        if (current == State.CLOSED) {
            return false;
        }
        if (current == State.HALF_OPEN) {
            return true;
        }
        // OPEN — check whether the window has expired.
        long elapsed = System.currentTimeMillis() - circuitOpenedAt.get();
        if (elapsed >= openDurationMs) {
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                logger.info("Circuit half-open for peer [{}] — allowing probe after {}ms open.",
                    peerUrl, elapsed);
                return false;
            }
            // Another thread won the CAS — drop this submission.
            return true;
        }
        return true;
    }

    private void onSuccess() {
        int prior = consecutiveFailures.getAndSet(0);
        State prev = state.getAndSet(State.CLOSED);
        if (prev != State.CLOSED || prior > 0) {
            logger.info("Circuit closed for peer [{}] — peer is reachable again " +
                "(was {} consecutive failures).", peerUrl, prior);
        }
    }

    private void onFailure(RuntimeException e) {
        State current = state.get();
        if (current == State.HALF_OPEN) {
            // Probe failed — revert to OPEN and restart the open window.
            state.set(State.OPEN);
            circuitOpenedAt.set(System.currentTimeMillis());
            logger.debug("Probe to peer [{}] failed — circuit remains open: {}", peerUrl, e.getMessage());
            return;
        }
        int failures = consecutiveFailures.incrementAndGet();
        if (current == State.CLOSED && failures >= failureThreshold) {
            if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                circuitOpenedAt.set(System.currentTimeMillis());
                logger.warn(
                    "Circuit opened for peer [{}] after {} consecutive failures. Broadcasts will be " +
                    "suppressed for {}ms before a probe is attempted. Last error: {}",
                    peerUrl, failures, openDurationMs, e.getMessage());
            }
        } else if (current == State.CLOSED) {
            logger.warn("Broadcast to peer [{}] failed ({}/{} before circuit opens): {}",
                peerUrl, failures, failureThreshold, e.getMessage());
        }
        // If OPEN, circuit is already open — no further action.
    }

    /** Strips the URL to host:port for use in thread names (no special characters). */
    private static String sanitise(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() != null ? uri.getHost() : url;
            return uri.getPort() != -1 ? host + ":" + uri.getPort() : host;
        } catch (Exception e) {
            return url.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
    }
}
