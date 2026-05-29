package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;
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
public class SchedulerJobStateChangeEventBroadcasterTest {

    @Mock
    private SchedulerJobStateChangeEventLocalBroadcastListener listener1;

    @Mock
    private SchedulerJobStateChangeEventLocalBroadcastListener listener2;

    @Mock
    private SchedulerJobInstanceStateChangeEvent event;

    @Mock
    private SchedulerJobInstance schedulerJobInstance;

    @Mock
    private SchedulerJobStateChangeEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Field listenersField = SchedulerJobStateChangeEventBroadcaster.class.getDeclaredField("localListeners");
        listenersField.setAccessible(true);
        listenersField.set(null, new WeakHashMap<>());
        Field remoteListenerField = SchedulerJobStateChangeEventBroadcaster.class.getDeclaredField("remoteListener");
        remoteListenerField.setAccessible(true);
        remoteListenerField.set(null, null);
    }

    @Test
    public void testRegister_addsListener() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.register(listener1);

        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testRegister_multipleListeners() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.register(listener1);
        SchedulerJobStateChangeEventBroadcaster.register(listener2);

        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

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
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.register(listener1);
        SchedulerJobStateChangeEventBroadcaster.unregister(listener1);

        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.broadcast(event);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        CountDownLatch latch = new CountDownLatch(1);

        SchedulerJobStateChangeEventLocalBroadcastListener asyncListener = event -> latch.countDown();

        SchedulerJobStateChangeEventBroadcaster.register(asyncListener);
        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_filtersTerminalJob() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn(JobConstants.CONTEXT_TERMINAL_JOB);

        SchedulerJobStateChangeEventBroadcaster.register(listener1);
        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_filtersStartJob() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn(JobConstants.CONTEXT_START_JOB);

        SchedulerJobStateChangeEventBroadcaster.register(listener1);
        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withNullSchedulerJobInstance() {
        when(event.getSchedulerJobInstance()).thenReturn(null);

        SchedulerJobStateChangeEventBroadcaster.register(listener1);
        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveBroadcast(any());
    }

    @Test
    public void testBroadcast_withRegularJob() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("RegularJobAgent");

        SchedulerJobStateChangeEventBroadcaster.register(listener1);
        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.setRemoteListener(remoteListener);
        SchedulerJobStateChangeEventBroadcaster.broadcast(event);
        verify(remoteListener).receiveBroadcast(event);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        SchedulerJobStateChangeEventBroadcaster.remoteBroadcast(event);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        when(event.getSchedulerJobInstance()).thenReturn(schedulerJobInstance);
        when(schedulerJobInstance.getAgentName()).thenReturn("TestAgent");

        SchedulerJobStateChangeEventBroadcaster.register(listener1);
        SchedulerJobStateChangeEventBroadcaster.register(listener1);

        SchedulerJobStateChangeEventBroadcaster.broadcast(event);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveBroadcast(event);
    }
}
