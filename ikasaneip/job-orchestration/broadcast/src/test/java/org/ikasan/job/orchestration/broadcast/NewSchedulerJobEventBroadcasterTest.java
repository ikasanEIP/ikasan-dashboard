package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
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
public class NewSchedulerJobEventBroadcasterTest {

    @Mock
    private NewSchedulerJobEventLocalBroadcastListener listener1;

    @Mock
    private NewSchedulerJobEventLocalBroadcastListener listener2;

    @Mock
    private SchedulerJob schedulerJob;

    @Mock
    private NewSchedulerJobEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = NewSchedulerJobEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(NewSchedulerJobEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        NewSchedulerJobEventBroadcaster.instance().register(listener1);

        NewSchedulerJobEventBroadcaster.instance().broadcast(schedulerJob);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(schedulerJob);
    }

    @Test
    public void testRegister_multipleListeners() {
        NewSchedulerJobEventBroadcaster.instance().register(listener1);
        NewSchedulerJobEventBroadcaster.instance().register(listener2);

        NewSchedulerJobEventBroadcaster.instance().broadcast(schedulerJob);

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
        NewSchedulerJobEventBroadcaster.instance().register(listener1);
        NewSchedulerJobEventBroadcaster.instance().unregister(listener1);

        NewSchedulerJobEventBroadcaster.instance().broadcast(schedulerJob);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        NewSchedulerJobEventBroadcaster.instance().broadcast(schedulerJob);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        NewSchedulerJobEventLocalBroadcastListener asyncListener = schedulerJob -> latch.countDown();

        NewSchedulerJobEventBroadcaster.instance().register(asyncListener);
        NewSchedulerJobEventBroadcaster.instance().broadcast(schedulerJob);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullSchedulerJob() {
        NewSchedulerJobEventBroadcaster.instance().register(listener1);

        NewSchedulerJobEventBroadcaster.instance().broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        NewSchedulerJobEventBroadcaster.instance().setRemoteListener(remoteListener);
        NewSchedulerJobEventBroadcaster.instance().broadcast(schedulerJob);
        verify(remoteListener).receiveBroadcast(schedulerJob);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        NewSchedulerJobEventBroadcaster.instance().remoteBroadcast(schedulerJob);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        NewSchedulerJobEventBroadcaster.instance().register(listener1);
        NewSchedulerJobEventBroadcaster.instance().register(listener1);

        NewSchedulerJobEventBroadcaster.instance().broadcast(schedulerJob);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(schedulerJob);
    }
}
