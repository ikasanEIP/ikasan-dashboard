package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventRemoteBroadcastListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JobLockCacheEventBroadcasterTest {

    @Mock
    private JobLockCacheEventLocalBroadcastListener listener1;

    @Mock
    private JobLockCacheEventLocalBroadcastListener listener2;

    @Mock
    private JobLockCacheEvent event;

    @Mock
    private JobLockCacheEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = JobLockCacheEventBroadcaster.class.getDeclaredMethod(
            "reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(JobLockCacheEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        JobLockCacheEventBroadcaster.instance().register(listener1);

        JobLockCacheEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testRegister_multipleListeners() {
        JobLockCacheEventBroadcaster.instance().register(listener1);
        JobLockCacheEventBroadcaster.instance().register(listener2);

        JobLockCacheEventBroadcaster.instance().broadcast(event);

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
        JobLockCacheEventBroadcaster.instance().register(listener1);
        JobLockCacheEventBroadcaster.instance().unregister(listener1);

        JobLockCacheEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        JobLockCacheEventBroadcaster.instance().broadcast(event);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        JobLockCacheEventLocalBroadcastListener asyncListener = event -> latch.countDown();

        JobLockCacheEventBroadcaster.instance().register(asyncListener);
        JobLockCacheEventBroadcaster.instance().broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullEvent() {
        JobLockCacheEventBroadcaster.instance().register(listener1);

        JobLockCacheEventBroadcaster.instance().broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        JobLockCacheEventBroadcaster.instance().setRemoteListener(remoteListener);
        JobLockCacheEventBroadcaster.instance().broadcast(event);
        verify(remoteListener).receiveBroadcast(event);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        JobLockCacheEventBroadcaster.instance().remoteBroadcast(event);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        JobLockCacheEventBroadcaster.instance().register(listener1);
        JobLockCacheEventBroadcaster.instance().register(listener1);

        JobLockCacheEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    /**
     * Deterministic counterpart to the documented (see reset()'s javadoc) but merely tolerated
     * RejectedExecutionException in the concurrent stress test on ContextInstanceDlqEventBroadcasterTest:
     * proves a stale reference held from before reset() actually throws, rather than just allowing it to.
     */
    @Test
    public void testLocalBroadcast_onStaleReferenceAfterReset_throwsRejectedExecutionException() throws Exception {
        JobLockCacheEventBroadcaster staleReference = JobLockCacheEventBroadcaster.instance();
        staleReference.register(listener1);

        Method resetMethod = JobLockCacheEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(staleReference);

        Assert.assertThrows(RejectedExecutionException.class, () -> staleReference.localBroadcast(event));
    }
}
