package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcastListener;
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
public class JobLockCacheEventBroadcasterTest {

    @Mock
    private JobLockCacheEventBroadcastListener listener1;

    @Mock
    private JobLockCacheEventBroadcastListener listener2;

    @Mock
    private JobLockCacheEvent event;

    @Before
    @After
    public void resetListeners() throws Exception {
        Field listenersField = JobLockCacheEventBroadcaster.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        listenersField.set(null, new WeakHashMap<>());
    }

    @Test
    public void testRegister_addsListener() {
        JobLockCacheEventBroadcaster.register(listener1);

        JobLockCacheEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testRegister_multipleListeners() {
        JobLockCacheEventBroadcaster.register(listener1);
        JobLockCacheEventBroadcaster.register(listener2);

        JobLockCacheEventBroadcaster.broadcast(event);

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
        JobLockCacheEventBroadcaster.register(listener1);
        JobLockCacheEventBroadcaster.unregister(listener1);

        JobLockCacheEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        JobLockCacheEventBroadcaster.broadcast(event);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        JobLockCacheEventBroadcastListener asyncListener = new JobLockCacheEventBroadcastListener() {
            @Override
            public void receiveBroadcast(JobLockCacheEvent event) {
                latch.countDown();
            }
        };

        JobLockCacheEventBroadcaster.register(asyncListener);
        JobLockCacheEventBroadcaster.broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullEvent() {
        JobLockCacheEventBroadcaster.register(listener1);

        JobLockCacheEventBroadcaster.broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        JobLockCacheEventBroadcaster.register(listener1);
        JobLockCacheEventBroadcaster.register(listener1);

        JobLockCacheEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }
}
