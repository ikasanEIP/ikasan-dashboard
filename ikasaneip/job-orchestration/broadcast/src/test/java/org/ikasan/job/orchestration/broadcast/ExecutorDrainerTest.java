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

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskCompleted = new CountDownLatch(1);

        executor.execute(() -> {
            taskStarted.countDown();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                taskCompleted.countDown();
            }
        });

        Assert.assertTrue("Task should have started before the executor is shut down",
            taskStarted.await(1, TimeUnit.SECONDS));
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
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> {
            taskStarted.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                }
        });

        Assert.assertTrue("Task should have started before the calling thread is interrupted",
            taskStarted.await(1, TimeUnit.SECONDS));

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
        Assert.assertTrue("Interrupt flag should be restored on the calling thread",
            Thread.interrupted()); // also clears the flag so it doesn't leak into other tests
        interrupter.join();
        release.countDown();
    }

    @Test
    public void testShutdownAndAwait_firstWaitTimesOut_forcesShutdownNowAndThenTerminates() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskInterrupted = new CountDownLatch(1);

        executor.execute(() -> {
            taskStarted.countDown();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                taskInterrupted.countDown();
            }
        });

        Assert.assertTrue("Task should have started before the graceful wait begins",
            taskStarted.await(1, TimeUnit.SECONDS));
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
        Logger timeoutLogger = mock(Logger.class);
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch allowTaskToFinish = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);

        // Create the 'stuck' task
        executor.execute(() -> {
            taskStarted.countDown();
            // Ignores interruption, simulating a genuinely stuck task that shutdownNow() cannot kill.
            boolean interrupted = false;
            while (allowTaskToFinish.getCount() != 0) {
                try {
                    allowTaskToFinish.await();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            taskFinished.countDown();
        });

        Assert.assertTrue("Task should have started before the graceful wait begins",
            taskStarted.await(1, TimeUnit.SECONDS));
        try {
            ExecutorDrainer.shutdownAndAwait(executor, timeoutLogger, Duration.ofMillis(50), Duration.ofMillis(50));

            Assert.assertEquals("Method should return after the second timeout while the task is still stuck",
                1, taskFinished.getCount());
            verify(timeoutLogger).error(contains("Executor still had not terminated after shutdownNow()"));
        } finally {
            allowTaskToFinish.countDown();
            Assert.assertTrue("Stuck task should be released so the executor thread is not leaked",
                taskFinished.await(1, TimeUnit.SECONDS));
            Assert.assertTrue("Executor should terminate once the stuck task is released",
                executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }
}
