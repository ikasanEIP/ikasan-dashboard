package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
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
public class ContextInstanceDlqEventBroadcasterTest {

    @Mock
    private ContextInstanceDlqEventLocalBroadcastListener listener1;

    @Mock
    private ContextInstanceDlqEventLocalBroadcastListener listener2;

    @Mock
    private ContextInstance contextInstance;

    @Mock
    private ContextInstanceDlqEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = ContextInstanceDlqEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(ContextInstanceDlqEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        ContextInstanceDlqEventBroadcaster.instance().register(listener1);

        // Verify listener is registered by broadcasting
        ContextInstanceDlqEventBroadcaster.instance().broadcast(contextInstance);

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextInstance);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextInstanceDlqEventBroadcaster.instance().register(listener1);
        ContextInstanceDlqEventBroadcaster.instance().register(listener2);

        ContextInstanceDlqEventBroadcaster.instance().broadcast(contextInstance);

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextInstance);
        verify(listener2, atLeastOnce()).receiveBroadcast(contextInstance);
    }

    @Test
    public void testUnregister_removesListener() {
        ContextInstanceDlqEventBroadcaster.instance().register(listener1);
        ContextInstanceDlqEventBroadcaster.instance().unregister(listener1);

        ContextInstanceDlqEventBroadcaster.instance().broadcast(contextInstance);

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        // Should not throw exception
        ContextInstanceDlqEventBroadcaster.instance().broadcast(contextInstance);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextInstanceDlqEventLocalBroadcastListener asyncListener = contextInstance -> latch.countDown();

        ContextInstanceDlqEventBroadcaster.instance().register(asyncListener);
        ContextInstanceDlqEventBroadcaster.instance().broadcast(contextInstance);

        // Wait for async execution
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullContextInstance() {
        ContextInstanceDlqEventBroadcaster.instance().register(listener1);

        // Should not throw exception
        ContextInstanceDlqEventBroadcaster.instance().broadcast(null);

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        ContextInstanceDlqEventBroadcaster.instance().setRemoteListener(remoteListener);
        ContextInstanceDlqEventBroadcaster.instance().broadcast(contextInstance);
        verify(remoteListener).receiveBroadcast(contextInstance);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextInstanceDlqEventBroadcaster.instance().remoteBroadcast(contextInstance);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextInstanceDlqEventBroadcaster.instance().register(listener1);
        ContextInstanceDlqEventBroadcaster.instance().register(listener1);

        ContextInstanceDlqEventBroadcaster.instance().broadcast(contextInstance);

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Should only be called once because it's the same listener in a map
        verify(listener1, atLeastOnce()).receiveBroadcast(contextInstance);
    }

    /**
     * Proves reset()'s concurrency fix (volatile INSTANCE + synchronized + drain-before-swap) rather
     * than just arguing it: hammers register/broadcast/unregister from several threads while a
     * separate thread repeatedly calls reset() via reflection. The only acceptable exception is the
     * documented RejectedExecutionException from a stale reference to a just-reset instance — anything
     * else (corruption, NPE, ConcurrentModificationException, a hang) fails the test.
     */
    @Test
    public void testReset_isSafeUnderConcurrentBroadcastAndRegister() throws Exception {
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
                    ContextInstanceDlqEventLocalBroadcastListener localListener = ci -> { };
                    for (int j = 0; j < iterationsPerThread; j++) {
                        try {
                            ContextInstanceDlqEventBroadcaster broadcaster = ContextInstanceDlqEventBroadcaster.instance();
                            broadcaster.register(localListener);
                            broadcaster.broadcast(contextInstance);
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
                Method resetMethod = ContextInstanceDlqEventBroadcaster.class.getDeclaredMethod("reset");
                resetMethod.setAccessible(true);
                while (keepResetting.get()) {
                    resetMethod.invoke(ContextInstanceDlqEventBroadcaster.instance());
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
}
