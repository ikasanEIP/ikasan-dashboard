package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
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
public class ContextInstanceStateChangeEventBroadcasterTest {

    @Mock
    private ContextInstanceStateChangeEventLocalBroadcastListener listener1;

    @Mock
    private ContextInstanceStateChangeEventLocalBroadcastListener listener2;

    @Mock
    private ContextInstanceStateChangeEvent event;

    @Before
    @After
    public void resetListeners() throws Exception {
        Field listenersField = ContextInstanceStateChangeEventBroadcaster.class.getDeclaredField("localListeners");
        listenersField.setAccessible(true);
        listenersField.set(null, new WeakHashMap<>());
    }

    @Test
    public void testRegister_addsListener() {
        ContextInstanceStateChangeEventBroadcaster.register(listener1);

        ContextInstanceStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextInstanceStateChangeEventBroadcaster.register(listener1);
        ContextInstanceStateChangeEventBroadcaster.register(listener2);

        ContextInstanceStateChangeEventBroadcaster.broadcast(event);

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
        ContextInstanceStateChangeEventBroadcaster.register(listener1);
        ContextInstanceStateChangeEventBroadcaster.unregister(listener1);

        ContextInstanceStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextInstanceStateChangeEventBroadcaster.broadcast(event);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextInstanceStateChangeEventLocalBroadcastListener asyncListener = event -> latch.countDown();

        ContextInstanceStateChangeEventBroadcaster.register(asyncListener);
        ContextInstanceStateChangeEventBroadcaster.broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullEvent() {
        ContextInstanceStateChangeEventBroadcaster.register(listener1);

        ContextInstanceStateChangeEventBroadcaster.broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextInstanceStateChangeEventBroadcaster.register(listener1);
        ContextInstanceStateChangeEventBroadcaster.register(listener1);

        ContextInstanceStateChangeEventBroadcaster.broadcast(event);

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

        ContextInstanceStateChangeEventBroadcaster.register(asyncListener);
        ContextInstanceStateChangeEventBroadcaster.broadcast(event);
        ContextInstanceStateChangeEventBroadcaster.broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Both broadcasts should execute", completed);
    }
}
