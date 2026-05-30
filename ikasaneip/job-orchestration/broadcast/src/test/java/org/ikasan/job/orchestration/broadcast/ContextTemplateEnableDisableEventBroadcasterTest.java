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

import java.lang.reflect.Field;
import java.util.WeakHashMap;
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
        Field listenersField = ContextTemplateEnableDisableEventBroadcaster.class.getDeclaredField("localListeners");
        listenersField.setAccessible(true);
        ((WeakHashMap<?, ?>) listenersField.get(null)).clear();
        Field remoteListenerField = ContextTemplateEnableDisableEventBroadcaster.class.getDeclaredField("remoteListener");
        remoteListenerField.setAccessible(true);
        remoteListenerField.set(null, null);
    }

    @Test
    public void testRegister_addsListener() {
        ContextTemplateEnableDisableEventBroadcaster.register(listener1);

        ContextTemplateEnableDisableEventBroadcaster.broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextTemplate);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextTemplateEnableDisableEventBroadcaster.register(listener1);
        ContextTemplateEnableDisableEventBroadcaster.register(listener2);

        ContextTemplateEnableDisableEventBroadcaster.broadcast(contextTemplate);

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
        ContextTemplateEnableDisableEventBroadcaster.register(listener1);
        ContextTemplateEnableDisableEventBroadcaster.unregister(listener1);

        ContextTemplateEnableDisableEventBroadcaster.broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextTemplateEnableDisableEventBroadcaster.broadcast(contextTemplate);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextTemplateEnableDisableEventLocalBroadcastListener asyncListener = contextTemplate -> latch.countDown();

        ContextTemplateEnableDisableEventBroadcaster.register(asyncListener);
        ContextTemplateEnableDisableEventBroadcaster.broadcast(contextTemplate);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullContextTemplate() {
        ContextTemplateEnableDisableEventBroadcaster.register(listener1);

        ContextTemplateEnableDisableEventBroadcaster.broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        ContextTemplateEnableDisableEventBroadcaster.setRemoteListener(remoteListener);
        ContextTemplateEnableDisableEventBroadcaster.broadcast(contextTemplate);
        verify(remoteListener).receiveBroadcast(contextTemplate);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextTemplateEnableDisableEventBroadcaster.remoteBroadcast(contextTemplate);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextTemplateEnableDisableEventBroadcaster.register(listener1);
        ContextTemplateEnableDisableEventBroadcaster.register(listener1);

        ContextTemplateEnableDisableEventBroadcaster.broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(contextTemplate);
    }
}
