package org.ikasan.job.orchestration.broadcast;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The timeout/{@code shutdownNow()} branches are exercised via the package-private
 * {@code shutdownAndAwait(executor, logger, firstWait, secondWait)} overload with short
 * durations, rather than the public 5s/2s defaults, so this suite doesn't have to block for
 * real seconds to cover them.
 */
public class ExecutorDrainerTest {

    private final Logger logger = LoggerFactory.getLogger(ExecutorDrainerTest.class);

    @Test
    public void testShutdownAndAwait_idleExecutor_terminatesCleanly() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        ExecutorDrainer.shutdownAndAwait(executor, logger);

        Assert.assertTrue("Executor should be shut down", executor.isShutdown());
        Assert.assertTrue("Executor should have terminated", executor.isTerminated());
    }

    @Test
    public void testShutdownAndAwait_waitsForInFlightTaskToFinish() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch taskCompleted = new CountDownLatch(1);

        executor.execute(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                taskCompleted.countDown();
            }
        });

        ExecutorDrainer.shutdownAndAwait(executor, logger);

        Assert.assertEquals("The in-flight task should have completed, not been interrupted mid-way",
            0, taskCompleted.getCount());
        Assert.assertTrue("Executor should have terminated", executor.isTerminated());
    }

    @Test
    public void testShutdownAndAwait_restoresInterruptFlagIfCallingThreadInterrupted() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Keep the executor busy long enough that awaitTermination has something to wait on
        // when this thread gets interrupted out from under it.
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        });

        Thread self = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            self.interrupt();
        });
        interrupter.start();

        ExecutorDrainer.shutdownAndAwait(executor, logger);
        interrupter.join();
        release.countDown();

        Assert.assertTrue("Interrupt flag should be restored on the calling thread",
            Thread.interrupted()); // also clears the flag so it doesn't leak into other tests
    }

    @Test
    public void testShutdownAndAwait_firstWaitTimesOut_forcesShutdownNowAndThenTerminates() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch taskInterrupted = new CountDownLatch(1);

        executor.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                taskInterrupted.countDown();
            }
        });

        // firstWait shorter than the task's sleep forces the timeout branch; secondWait is long
        // enough for shutdownNow()'s interrupt to actually take effect and terminate cleanly.
        ExecutorDrainer.shutdownAndAwait(executor, logger, Duration.ofMillis(50), Duration.ofSeconds(2));

        Assert.assertTrue("Task should have been interrupted by shutdownNow()",
            taskInterrupted.await(1, TimeUnit.SECONDS));
        Assert.assertTrue("Executor should have terminated after shutdownNow()", executor.isTerminated());
    }

    @Test
    public void testShutdownAndAwait_taskIgnoresInterruption_stillReturnsAfterSecondWaitTimesOut() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch taskFinished = new CountDownLatch(1);

        executor.execute(() -> {
            // Ignores interruption, simulating a genuinely stuck task that shutdownNow() cannot kill.
            long deadline = System.currentTimeMillis() + 500;
            while (System.currentTimeMillis() < deadline) {
                // busy-wait past both firstWait and secondWait below
            }
            taskFinished.countDown();
        });

        ExecutorDrainer.shutdownAndAwait(executor, logger, Duration.ofMillis(50), Duration.ofMillis(50));

        Assert.assertTrue("Method should return (logging an error) rather than blocking forever",
            taskFinished.await(2, TimeUnit.SECONDS));
    }
}
