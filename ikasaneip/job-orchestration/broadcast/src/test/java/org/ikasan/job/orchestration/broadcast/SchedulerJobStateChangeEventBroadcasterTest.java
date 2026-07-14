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
        resetCurrentBroadcaster();
    }

    private void resetCurrentBroadcaster() throws Exception {
        Method resetMethod = SchedulerJobStateChangeEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(SchedulerJobStateChangeEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1).receiveBroadcast(event);
    }

    @Test
    public void testRegister_multipleListeners() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().register(listener2);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1).receiveBroadcast(event);
        verify(listener2).receiveBroadcast(event);
    }

    @Test
    public void testUnregister_removesListener() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().unregister(listener1);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        CountDownLatch listenerStarted = new CountDownLatch(1);
        CountDownLatch allowListenerToFinish = new CountDownLatch(1);
        SchedulerJobStateChangeEventLocalBroadcastListener asyncListener = event -> {
            listenerStarted.countDown();
            try {
                allowListenerToFinish.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        SchedulerJobStateChangeEventBroadcaster.instance().register(asyncListener);
        Thread broadcasterThread = new Thread(() -> SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event));
        broadcasterThread.start();
        try {
            Assert.assertTrue("Listener should start", listenerStarted.await(1, TimeUnit.SECONDS));
            broadcasterThread.join(1000);
            Assert.assertFalse("broadcast() should return without waiting for its local listener", broadcasterThread.isAlive());
        } finally {
            allowListenerToFinish.countDown();
            broadcasterThread.join(1000);
            resetCurrentBroadcaster();
        }
    }

    @Test
    public void testBroadcast_filtersTerminalJob() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn(JobConstants.CONTEXT_TERMINAL_JOB);

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().setRemoteListener(remoteListener);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1, never()).receiveBroadcast(any());
        verify(remoteListener, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_filtersStartJob() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn(JobConstants.CONTEXT_START_JOB);

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().setRemoteListener(remoteListener);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1, never()).receiveBroadcast(any());
        verify(remoteListener, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNullSchedulerJobInstance() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(null);

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withRegularJob() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("RegularJobAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1).receiveBroadcast(event);
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
    public void testRegister_sameListenerTwice() throws Exception {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);
        SchedulerJobStateChangeEventBroadcaster.instance().register(listener1);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(event);

        resetCurrentBroadcaster();

        verify(listener1).receiveBroadcast(event);
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
        AtomicInteger resetFailures = new AtomicInteger(0);
        AtomicBoolean keepResetting = new AtomicBoolean(true);

        // Each of the 4 workers repeatedly fetches instance(), then register/broadcast/unregister on that
        // reference. The resetter thread below may swap INSTANCE in between, so the reference a worker is
        // holding can go stale mid-iteration.
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
                    // Anything other than the documented RejectedExecutionException above is a real bug.
                    unexpectedExceptions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // The source of the race: continuously resets the singleton (draining the old executor, then
        // swapping INSTANCE) for as long as workers are running. No explicit delay is needed between
        // iterations — reset() itself blocks until the previous instance's executor has drained.
        ExecutorService resetter = Executors.newSingleThreadExecutor();
        resetter.submit(() -> {
            try {
                startLatch.await();
                Method resetMethod = SchedulerJobStateChangeEventBroadcaster.class.getDeclaredMethod("reset");
                resetMethod.setAccessible(true);
                while (keepResetting.get()) {
                    resetMethod.invoke(SchedulerJobStateChangeEventBroadcaster.instance());
                }
            } catch (Exception e) {
                resetFailures.incrementAndGet();
            }
        });

        startLatch.countDown();
        boolean completed;
        try {
            completed = doneLatch.await(30, TimeUnit.SECONDS);
        } finally {
            keepResetting.set(false);
            workers.shutdown();
            resetter.shutdown();
        }

        // completed catches a hang/deadlock; the two awaitTermination checks catch thread leaks; the two
        // AtomicInteger checks are the correctness proof — nothing broke beyond the one documented exception.
        Assert.assertTrue("Worker threads should finish without hanging", completed);
        Assert.assertTrue("Worker executor should terminate", workers.awaitTermination(5, TimeUnit.SECONDS));
        Assert.assertTrue("Resetter should stop before the test ends", resetter.awaitTermination(5, TimeUnit.SECONDS));
        Assert.assertEquals(
            "No exceptions other than the documented RejectedExecutionException should occur under concurrent reset",
            0, unexpectedExceptions.get());
        Assert.assertEquals("Concurrent reset should not fail", 0, resetFailures.get());
    }

    /**
     * Proves a stale reference held from before reset() throws RejectedExecutionException if broadcast attempted.
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
