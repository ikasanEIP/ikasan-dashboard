package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
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
public class NewSchedulerJobEventBroadcasterTest {

    @Mock
    private NewSchedulerJobEventLocalBroadcastListener listener1;

    @Mock
    private NewSchedulerJobEventLocalBroadcastListener listener2;

    @Mock
    private SchedulerJob schedulerJob;

    @Before
    @After
    public void resetListeners() throws Exception {
        Field listenersField = NewSchedulerJobEventBroadcaster.class.getDeclaredField("localListeners");
        listenersField.setAccessible(true);
        listenersField.set(null, new WeakHashMap<>());
    }

    @Test
    public void testRegister_addsListener() {
        NewSchedulerJobEventBroadcaster.register(listener1);

        NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(schedulerJob);
    }

    @Test
    public void testRegister_multipleListeners() {
        NewSchedulerJobEventBroadcaster.register(listener1);
        NewSchedulerJobEventBroadcaster.register(listener2);

        NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(schedulerJob);
        verify(listener2, atLeastOnce()).receiveBroadcast(schedulerJob);
    }

    @Test
    public void testUnregister_removesListener() {
        NewSchedulerJobEventBroadcaster.register(listener1);
        NewSchedulerJobEventBroadcaster.unregister(listener1);

        NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        NewSchedulerJobEventLocalBroadcastListener asyncListener = schedulerJob -> latch.countDown();

        NewSchedulerJobEventBroadcaster.register(asyncListener);
        NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullSchedulerJob() {
        NewSchedulerJobEventBroadcaster.register(listener1);

        NewSchedulerJobEventBroadcaster.broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        NewSchedulerJobEventBroadcaster.register(listener1);
        NewSchedulerJobEventBroadcaster.register(listener1);

        NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(schedulerJob);
    }
}
