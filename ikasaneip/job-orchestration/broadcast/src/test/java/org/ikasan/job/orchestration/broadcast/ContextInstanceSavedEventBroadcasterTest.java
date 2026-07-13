package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventRemoteBroadcastListener;
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
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceSavedEventBroadcasterTest {

    @Mock
    private ContextInstanceSavedEventLocalBroadcastListener listener1;

    @Mock
    private ContextInstanceSavedEventLocalBroadcastListener listener2;

    @Mock
    private ContextInstance contextInstance;

    @Mock
    private ContextInstanceSavedEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = ContextInstanceSavedEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(ContextInstanceSavedEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        ContextInstanceSavedEventBroadcaster.instance().register(listener1);

        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextInstance);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextInstanceSavedEventBroadcaster.instance().register(listener1);
        ContextInstanceSavedEventBroadcaster.instance().register(listener2);

        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);

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
        ContextInstanceSavedEventBroadcaster.instance().register(listener1);
        ContextInstanceSavedEventBroadcaster.instance().unregister(listener1);

        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextInstanceSavedEventLocalBroadcastListener asyncListener = contextInstance -> latch.countDown();

        ContextInstanceSavedEventBroadcaster.instance().register(asyncListener);
        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullContextInstance() {
        ContextInstanceSavedEventBroadcaster.instance().register(listener1);

        ContextInstanceSavedEventBroadcaster.instance().broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        ContextInstanceSavedEventBroadcaster.instance().setRemoteListener(remoteListener);
        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);
        verify(remoteListener).receiveBroadcast(contextInstance);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextInstanceSavedEventBroadcaster.instance().remoteBroadcast(contextInstance);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextInstanceSavedEventBroadcaster.instance().register(listener1);
        ContextInstanceSavedEventBroadcaster.instance().register(listener1);

        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextInstance);
    }
}
