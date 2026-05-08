package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventLocalBroadcastListener;
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
public class ContextTemplateSavedEventBroadcasterTest {

    @Mock
    private ContextTemplateSavedEventLocalBroadcastListener listener1;

    @Mock
    private ContextTemplateSavedEventLocalBroadcastListener listener2;

    @Mock
    private ContextTemplate contextTemplate;

    @Before
    @After
    public void resetListeners() throws Exception {
        Field listenersField = ContextTemplateSavedEventBroadcaster.class.getDeclaredField("localListeners");
        listenersField.setAccessible(true);
        listenersField.set(null, new WeakHashMap<>());
    }

    @Test
    public void testRegister_addsListener() {
        ContextTemplateSavedEventBroadcaster.register(listener1);

        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(contextTemplate);
    }

    @Test
    public void testRegister_multipleListeners() {
        ContextTemplateSavedEventBroadcaster.register(listener1);
        ContextTemplateSavedEventBroadcaster.register(listener2);

        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);

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
        ContextTemplateSavedEventBroadcaster.register(listener1);
        ContextTemplateSavedEventBroadcaster.unregister(listener1);

        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, never()).receiveContextTemplateSavedEventBroadcast(any());
    }

    @Test
    public void testBroadcast_withNoListeners() {
        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);
    }

    @Test
    public void testBroadcast_executesAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ContextTemplateSavedEventLocalBroadcastListener asyncListener = contextTemplate -> latch.countDown();

        ContextTemplateSavedEventBroadcaster.register(asyncListener);
        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        Assert.assertTrue("Broadcast should execute asynchronously", completed);
    }

    @Test
    public void testBroadcast_nullContextTemplate() {
        ContextTemplateSavedEventBroadcaster.register(listener1);

        ContextTemplateSavedEventBroadcaster.broadcast(null);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(null);
    }

    @Test
    public void testRegister_sameListenerTwice() {
        ContextTemplateSavedEventBroadcaster.register(listener1);
        ContextTemplateSavedEventBroadcaster.register(listener1);

        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(listener1, atLeastOnce()).receiveContextTemplateSavedEventBroadcast(contextTemplate);
    }
}
