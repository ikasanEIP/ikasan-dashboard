package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventRemoteBroadcastListener;
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
public class ContextTemplateEnableDisableEventBroadcasterTest {

    @Mock
    private ContextTemplateEnableDisableEventLocalBroadcastListener listener1;

    @Mock
    private ContextTemplateEnableDisableEventLocalBroadcastListener listener2;

    @Mock
    private ContextTemplate contextTemplate;

    @Mock
    private ContextTemplateEnableDisableEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = ContextTemplateEnableDisableEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(ContextTemplateEnableDisableEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        ContextTemplateEnableDisableEventBroadcaster.instance().register(listener1);

        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextTemplate);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextTemplateEnableDisableEventBroadcaster.instance().register(listener1);
        ContextTemplateEnableDisableEventBroadcaster.instance().register(listener2);

        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextTemplate);
        verify(listener2, atLeastOnce()).receiveBroadcast(contextTemplate);
    }

    @Test
    public void testUnregister_removesListener() {
        ContextTemplateEnableDisableEventBroadcaster.instance().register(listener1);
        ContextTemplateEnableDisableEventBroadcaster.instance().unregister(listener1);

        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(contextTemplate);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextTemplateEnableDisableEventLocalBroadcastListener asyncListener = contextTemplate -> latch.countDown();

        ContextTemplateEnableDisableEventBroadcaster.instance().register(asyncListener);
        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(contextTemplate);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullContextTemplate() {
        ContextTemplateEnableDisableEventBroadcaster.instance().register(listener1);

        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        ContextTemplateEnableDisableEventBroadcaster.instance().setRemoteListener(remoteListener);
        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(contextTemplate);
        verify(remoteListener).receiveBroadcast(contextTemplate);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextTemplateEnableDisableEventBroadcaster.instance().remoteBroadcast(contextTemplate);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextTemplateEnableDisableEventBroadcaster.instance().register(listener1);
        ContextTemplateEnableDisableEventBroadcaster.instance().register(listener1);

        ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextTemplate);
    }
}
