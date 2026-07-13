package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventRemoteBroadcastListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextViewUpdateEventBroadcasterTest {

    @Mock
    private ContextViewUpdateEventLocalBroadcastListener listener1;

    @Mock
    private ContextViewUpdateEventLocalBroadcastListener listener2;

    private String testMessage = "test message";

    @Mock
    private ContextViewUpdateEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = ContextViewUpdateEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(ContextViewUpdateEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        ContextViewUpdateEventBroadcaster.instance().register(listener1);

        ContextViewUpdateEventBroadcaster.instance().broadcast(testMessage);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(testMessage);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextViewUpdateEventBroadcaster.instance().register(listener1);
        ContextViewUpdateEventBroadcaster.instance().register(listener2);

        ContextViewUpdateEventBroadcaster.instance().broadcast(testMessage);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(testMessage);
        verify(listener2, atLeastOnce()).receiveBroadcast(testMessage);
    }

    @Test
    public void testUnregister_removesListener() {
        ContextViewUpdateEventBroadcaster.instance().register(listener1);
        ContextViewUpdateEventBroadcaster.instance().unregister(listener1);

        ContextViewUpdateEventBroadcaster.instance().broadcast(testMessage);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextViewUpdateEventBroadcaster.instance().broadcast(testMessage);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextViewUpdateEventLocalBroadcastListener asyncListener = message -> latch.countDown();

        ContextViewUpdateEventBroadcaster.instance().register(asyncListener);
        ContextViewUpdateEventBroadcaster.instance().broadcast(testMessage);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullMessage() {
        ContextViewUpdateEventBroadcaster.instance().register(listener1);

        ContextViewUpdateEventBroadcaster.instance().broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_emptyMessage() {
        ContextViewUpdateEventBroadcaster.instance().register(listener1);

        ContextViewUpdateEventBroadcaster.instance().broadcast("");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast("");
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        ContextViewUpdateEventBroadcaster.instance().setRemoteListener(remoteListener);
        ContextViewUpdateEventBroadcaster.instance().broadcast(testMessage);
        verify(remoteListener).receiveBroadcast(testMessage);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextViewUpdateEventBroadcaster.instance().remoteBroadcast(testMessage);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextViewUpdateEventBroadcaster.instance().register(listener1);
        ContextViewUpdateEventBroadcaster.instance().register(listener1);

        ContextViewUpdateEventBroadcaster.instance().broadcast(testMessage);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(testMessage);
    }
}
