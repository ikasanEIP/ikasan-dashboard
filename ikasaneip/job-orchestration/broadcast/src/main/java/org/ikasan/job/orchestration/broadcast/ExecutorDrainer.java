package org.ikasan.job.orchestration.broadcast;

import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Shared logic for safely draining an {@link ExecutorService} before a broadcaster's singleton
 * instance is recreated by {@code reset()}. Extracted so all nine broadcaster classes share one
 * implementation of this instead of nine identical copies.
 *
 * @author Ikasan Development Team
 */
public final class ExecutorDrainer {

    private ExecutorDrainer() {}

    /**
     * Shuts down {@code executor}, waits (up to 5 seconds) for any already queued or in-flight
     * tasks to finish, then falls back to {@link ExecutorService#shutdownNow()} if that times out.
     *
     * <p>If the executor still has not terminated within a further 2 seconds after
     * {@code shutdownNow()}, that's logged as an error — {@code shutdownNow()} is best-effort
     * (it interrupts running tasks but cannot force-kill a thread that ignores interruption), so
     * this is the only way to detect, rather than silently accept, a genuinely stuck task.
     *
     * @param executor the executor to shut down and drain
     * @param logger   the caller's logger, so warnings/errors are attributed to the broadcaster
     *                 class that owns the executor, not to this utility
     */
    public static void shutdownAndAwait(ExecutorService executor, Logger logger) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate within 5 seconds during reset(); forcing shutdown. " +
                    "This may indicate a slow or stuck broadcast listener.");
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    logger.error("Executor still had not terminated after shutdownNow(); a broadcast task is " +
                        "likely ignoring interruption and will keep running on a leaked thread.");
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while awaiting executor termination during reset().", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
