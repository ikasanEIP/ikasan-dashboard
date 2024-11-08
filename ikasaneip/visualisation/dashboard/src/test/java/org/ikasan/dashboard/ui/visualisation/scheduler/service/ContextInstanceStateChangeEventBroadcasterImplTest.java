package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
// need to work out the intent of this test class
public class ContextInstanceStateChangeEventBroadcasterImplTest {

    @Test
    public void should_init() {
        ContextInstanceStateChangeEventBroadcasterImpl broadcaster = new ContextInstanceStateChangeEventBroadcasterImpl();

        Executor executor = (Executor) ReflectionTestUtils.getField(broadcaster, "executor");
        assertNotNull(executor);

        LinkedList<Consumer<ContextInstanceStateChangeEvent>> listeners
            = (LinkedList<Consumer<ContextInstanceStateChangeEvent>>) ReflectionTestUtils.getField(broadcaster, "listeners");

        assertNotNull(listeners);
        assertEquals(0, listeners.size());
    }

}