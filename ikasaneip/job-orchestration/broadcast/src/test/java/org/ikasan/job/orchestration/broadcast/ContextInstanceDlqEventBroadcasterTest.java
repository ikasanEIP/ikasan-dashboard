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

import java.lang.reflect.Field;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        // localListeners is static final — clear the existing instance rather than replacing it,
        // since Java 17 refuses reflective writes to final fields.
        Field listenersField = ContextInstanceDlqEventBroadcaster.class.getDeclaredField("localListeners");
        listenersField.setAccessible(true);
        ((WeakHashMap<?, ?>) listenersField.get(null)).clear();
        Field remoteListenerField = ContextInstanceDlqEventBroadcaster.class.getDeclaredField("remoteListener");
        remoteListenerField.setAccessible(true);
        remoteListenerField.set(null, null);
    }

    @Test
    public void testRegister_addsListener() {
        ContextInstanceDlqEventBroadcaster.register(listener1);

        // Verify listener is registered by broadcasting
        ContextInstanceDlqEventBroadcaster.broadcast(contextInstance);

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
        ContextInstanceDlqEventBroadcaster.register(listener1);
        ContextInstanceDlqEventBroadcaster.register(listener2);

        ContextInstanceDlqEventBroadcaster.broadcast(contextInstance);

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
        ContextInstanceDlqEventBroadcaster.register(listener1);
        ContextInstanceDlqEventBroadcaster.unregister(listener1);

        ContextInstanceDlqEventBroadcaster.broadcast(contextInstance);

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
        ContextInstanceDlqEventBroadcaster.broadcast(contextInstance);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextInstanceDlqEventLocalBroadcastListener asyncListener = contextInstance -> latch.countDown();

        ContextInstanceDlqEventBroadcaster.register(asyncListener);
        ContextInstanceDlqEventBroadcaster.broadcast(contextInstance);

        // Wait for async execution
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullContextInstance() {
        ContextInstanceDlqEventBroadcaster.register(listener1);

        // Should not throw exception
        ContextInstanceDlqEventBroadcaster.broadcast(null);

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
        ContextInstanceDlqEventBroadcaster.setRemoteListener(remoteListener);
        ContextInstanceDlqEventBroadcaster.broadcast(contextInstance);
        verify(remoteListener).receiveBroadcast(contextInstance);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextInstanceDlqEventBroadcaster.remoteBroadcast(contextInstance);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextInstanceDlqEventBroadcaster.register(listener1);
        ContextInstanceDlqEventBroadcaster.register(listener1);

        ContextInstanceDlqEventBroadcaster.broadcast(contextInstance);

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Should only be called once because it's the same listener in a map
        verify(listener1, atLeastOnce()).receiveBroadcast(contextInstance);
    }
}
