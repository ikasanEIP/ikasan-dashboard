package org.ikasan.job.orchestration.broadcast;

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
public class ContextViewUpdateEventBroadcasterTest {

    @Mock
    private ContextViewUpdateEventBroadcastListener listener1;

    @Mock
    private ContextViewUpdateEventBroadcastListener listener2;

    private String testMessage = "test message";

    @Before
    @After
    public void resetListeners() throws Exception {
        Field listenersField = ContextViewUpdateEventBroadcaster.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        listenersField.set(null, new WeakHashMap<>());
    }

    @Test
    public void testRegister_addsListener() {
        ContextViewUpdateEventBroadcaster.register(listener1);

        ContextViewUpdateEventBroadcaster.broadcast(testMessage);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(testMessage);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextViewUpdateEventBroadcaster.register(listener1);
        ContextViewUpdateEventBroadcaster.register(listener2);

        ContextViewUpdateEventBroadcaster.broadcast(testMessage);

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
        ContextViewUpdateEventBroadcaster.register(listener1);
        ContextViewUpdateEventBroadcaster.unregister(listener1);

        ContextViewUpdateEventBroadcaster.broadcast(testMessage);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextViewUpdateEventBroadcaster.broadcast(testMessage);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextViewUpdateEventBroadcastListener asyncListener = new ContextViewUpdateEventBroadcastListener() {
            @Override
            public void receiveBroadcast(String message) {
                latch.countDown();
            }
        };

        ContextViewUpdateEventBroadcaster.register(asyncListener);
        ContextViewUpdateEventBroadcaster.broadcast(testMessage);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullMessage() {
        ContextViewUpdateEventBroadcaster.register(listener1);

        ContextViewUpdateEventBroadcaster.broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_emptyMessage() {
        ContextViewUpdateEventBroadcaster.register(listener1);

        ContextViewUpdateEventBroadcaster.broadcast("");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast("");
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextViewUpdateEventBroadcaster.register(listener1);
        ContextViewUpdateEventBroadcaster.register(listener1);

        ContextViewUpdateEventBroadcaster.broadcast(testMessage);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(testMessage);
    }
}
