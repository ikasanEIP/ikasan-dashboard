package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerJobStateChangeEventBroadcasterTest {

    @Mock
    private SchedulerJobStateChangeEventLocalBroadcastListener listener1;

    @Mock
    private SchedulerJobStateChangeEventLocalBroadcastListener listener2;

    @Mock
    private SchedulerJobInstanceStateChangeEvent event;

    @Mock
    private SchedulerJobInstance schedulerJobInstance;

    @Mock
    private SchedulerJobStateChangeEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = SchedulerJobStateChangeEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(SchedulerJobStateChangeEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testRegister_multipleListeners() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().register(listener2);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
        verify(listener2, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testUnregister_removesListener() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().unregister(listener1);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        CountDownLatch latch = new CountDownLatch(1);

        SchedulerJobStateChangeEventLocalBroadcastListener asyncListener = event -> latch.countDown();

        SchedulerJobStateChangeEventBroadcaster.instance().register(asyncListener);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_filtersTerminalJob() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn(JobConstants.CONTEXT_TERMINAL_JOB);

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_filtersStartJob() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn(JobConstants.CONTEXT_START_JOB);

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNullSchedulerJobInstance() {
        when(event.getSchedulerJobInstance()).thenReturn(null);

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withRegularJob() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("RegularJobAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().setRemoteListener(remoteListener);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);
        verify(remoteListener).receiveBroadcast(event);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        SchedulerJobStateChangeEventBroadcaster.instance().remoteBroadcast(event);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    /**
     * Same proof as ContextInstanceDlqEventBroadcasterTest's equivalent test, but for the 10-thread-pool
     * executor variant (this class and ContextInstanceStateChangeEventBroadcaster use newFixedThreadPool(10,
     * ...) rather than a single-thread executor) — confirms awaitTermination() draining all pool threads,
     * not just one, still holds under concurrent register/broadcast/reset.
     */
    @Test
    public void testReset_isSafeUnderConcurrentBroadcastAndRegister() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        int threadCount = 4;
        int iterationsPerThread = 100;
        ExecutorService workers = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger unexpectedExceptions = new AtomicInteger(0);
        AtomicBoolean keepResetting = new AtomicBoolean(true);

        for (int i = 0; i < threadCount; i++) {
            workers.submit(() -> {
                try {
                    startLatch.await();
                    SchedulerJobStateChangeEventLocalBroadcastListener localListener = evt -> { };
                    for (int j = 0; j < iterationsPerThread; j++) {
                        try {
                            SchedulerJobStateChangeEventBroadcaster broadcaster = SchedulerJobStateChangeEventBroadcaster.instance();
                            broadcaster.register(localListener);
                            broadcaster.broadcast(event);
                            broadcaster.unregister(localListener);
                        } catch (RejectedExecutionException expected) {
                            // A stale reference to an instance reset concurrently is documented (see
                            // reset()'s javadoc) to throw here rather than silently going nowhere.
                        }
                    }
                } catch (Exception e) {
                    unexpectedExceptions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        ExecutorService resetter = Executors.newSingleThreadExecutor();
        resetter.submit(() -> {
            try {
                startLatch.await();
                Method resetMethod = SchedulerJobStateChangeEventBroadcaster.class.getDeclaredMethod("reset");
                resetMethod.setAccessible(true);
                while (keepResetting.get()) {
                    resetMethod.invoke(SchedulerJobStateChangeEventBroadcaster.instance());
                }
            } catch (Exception ignored) {
                // best-effort background resetter; failures here don't affect the assertions below
            }
        });

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        keepResetting.set(false);
        workers.shutdown();
        resetter.shutdown();

        Assert.assertTrue("Worker threads should finish without hanging", completed);
        Assert.assertEquals(
            "No exceptions other than the documented RejectedExecutionException should occur under concurrent reset",
            0, unexpectedExceptions.get());
    }

    /**
     * Deterministic counterpart to the test above: that concurrent stress test only tolerates
     * RejectedExecutionException if the race happens to produce one, it never asserts that it does. This proves
     * a stale reference held from before reset() actually throws, rather than just allowing it to.
     */
    @Test
    public void testLocalBroadcast_onStaleReferenceAfterReset_throwsRejectedExecutionException() throws Exception {
        SchedulerJobStateChangeEventBroadcaster staleReference = SchedulerJobStateChangeEventBroadcaster.instance();
        SchedulerJobStateChangeEventLocalBroadcastListener localListener = evt -> { };
        staleReference.register(localListener);

        Method resetMethod = SchedulerJobStateChangeEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(staleReference);

        Assert.assertThrows(RejectedExecutionException.class, () -> staleReference.localBroadcast(event));
    }
}
