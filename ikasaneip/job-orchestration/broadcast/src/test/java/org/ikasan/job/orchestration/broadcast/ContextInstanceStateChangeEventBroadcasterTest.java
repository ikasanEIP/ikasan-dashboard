package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventRemoteBroadcastListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceStateChangeEventBroadcasterTest {

    @Mock
    private ContextInstanceStateChangeEventLocalBroadcastListener listener1;

    @Mock
    private ContextInstanceStateChangeEventLocalBroadcastListener listener2;

    @Mock
    private ContextInstanceStateChangeEvent event;

    @Mock
    private ContextInstanceStateChangeEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = ContextInstanceStateChangeEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(ContextInstanceStateChangeEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        ContextInstanceStateChangeEventBroadcaster.instance().register(listener1);

        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextInstanceStateChangeEventBroadcaster.instance().register(listener1);
        ContextInstanceStateChangeEventBroadcaster.instance().register(listener2);

        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);

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
        ContextInstanceStateChangeEventBroadcaster.instance().register(listener1);
        ContextInstanceStateChangeEventBroadcaster.instance().unregister(listener1);

        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextInstanceStateChangeEventLocalBroadcastListener asyncListener = event -> latch.countDown();

        ContextInstanceStateChangeEventBroadcaster.instance().register(asyncListener);
        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullEvent() {
        ContextInstanceStateChangeEventBroadcaster.instance().register(listener1);

        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        ContextInstanceStateChangeEventBroadcaster.instance().setRemoteListener(remoteListener);
        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);
        verify(remoteListener).receiveBroadcast(event);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextInstanceStateChangeEventBroadcaster.instance().remoteBroadcast(event);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextInstanceStateChangeEventBroadcaster.instance().register(listener1);
        ContextInstanceStateChangeEventBroadcaster.instance().register(listener1);

        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testBroadcast_multipleEventsToSameListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        ContextInstanceStateChangeEventLocalBroadcastListener asyncListener = event -> latch.countDown();

        ContextInstanceStateChangeEventBroadcaster.instance().register(asyncListener);
        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);
        ContextInstanceStateChangeEventBroadcaster.instance().broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Both broadcasts should execute", completed);
    }

    /**
     * Proves a stale reference held from before reset() throws RejectedExecutionException if broadcast attempted.
     */
    @Test
    public void testLocalBroadcast_onStaleReferenceAfterReset_throwsRejectedExecutionException() throws Exception {
        ContextInstanceStateChangeEventBroadcaster staleReference = ContextInstanceStateChangeEventBroadcaster.instance();
        staleReference.register(listener1);

        Method resetMethod = ContextInstanceStateChangeEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(staleReference);

        Assert.assertThrows(RejectedExecutionException.class, () -> staleReference.localBroadcast(event));
    }
}
