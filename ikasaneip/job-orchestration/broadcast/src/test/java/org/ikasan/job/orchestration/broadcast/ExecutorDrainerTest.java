package org.ikasan.job.orchestration.broadcast;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Deliberately does not exercise the 5-second-timeout / shutdownNow() branch — doing so
 * correctly would mean the test itself has to block for 5+ real seconds, which isn't a
 * worthwhile trade for coverage of a simple, already-reviewed if-branch. These tests cover
 * the behaviour that actually matters: a normal drain terminates the executor, and a task
 * already in flight is allowed to finish rather than being killed prematurely.
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
}
