package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventRemoteBroadcastListener;
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
public class ContextTemplateSavedEventBroadcasterTest {

    @Mock
    private ContextTemplateSavedEventLocalBroadcastListener listener1;

    @Mock
    private ContextTemplateSavedEventLocalBroadcastListener listener2;

    @Mock
    private ContextTemplate contextTemplate;

    @Mock
    private ContextTemplateSavedEventRemoteBroadcastListener remoteListener;

    @Before
    @After
    public void resetListeners() throws Exception {
        Method resetMethod = ContextTemplateSavedEventBroadcaster.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(ContextTemplateSavedEventBroadcaster.instance());
    }

    @Test
    public void testRegister_addsListener() {
        ContextTemplateSavedEventBroadcaster.instance().register(listener1);

        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(contextTemplate);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextTemplateSavedEventBroadcaster.instance().register(listener1);
        ContextTemplateSavedEventBroadcaster.instance().register(listener2);

        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(contextTemplate);
        verify(listener2, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(contextTemplate);
    }

    @Test
    public void testUnregister_removesListener() {
        ContextTemplateSavedEventBroadcaster.instance().register(listener1);
        ContextTemplateSavedEventBroadcaster.instance().unregister(listener1);

        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveContextTemplateSavedEventBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextTemplateSavedEventLocalBroadcastListener asyncListener = contextTemplate -> latch.countDown();

        ContextTemplateSavedEventBroadcaster.instance().register(asyncListener);
        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullContextTemplate() {
        ContextTemplateSavedEventBroadcaster.instance().register(listener1);

        ContextTemplateSavedEventBroadcaster.instance().broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(null);
    }

    @Test
    public void testBroadcast_forwardsEventToRemoteListener() {
        ContextTemplateSavedEventBroadcaster.instance().setRemoteListener(remoteListener);
        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);
        verify(remoteListener).receiveContextTemplateSavedEventBroadcast(contextTemplate);
    }

    @Test
    public void testRemoteBroadcast_isNoOpWhenRemoteListenerNotSet() {
        // no remote listener set — must not throw
        ContextTemplateSavedEventBroadcaster.instance().remoteBroadcast(contextTemplate);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextTemplateSavedEventBroadcaster.instance().register(listener1);
        ContextTemplateSavedEventBroadcaster.instance().register(listener1);

        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(contextTemplate);
    }
}
