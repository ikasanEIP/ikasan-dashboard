package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerJobStateChangeEventBroadcasterImplTest {

    private SchedulerJobStateChangeEventBroadcasterImpl broadcaster;

    @Before
    public void setUp() {
        broadcaster = new SchedulerJobStateChangeEventBroadcasterImpl();
    }

    @Test
    public void should_init() {
        Executor executor = (Executor) ReflectionTestUtils.getField(broadcaster, "executor");
        assertNotNull(executor);

        LinkedList<Consumer<SchedulerJobInstanceStateChangeEvent>> listeners
            = (LinkedList<Consumer<SchedulerJobInstanceStateChangeEvent>>) ReflectionTestUtils.getField(broadcaster, "listeners");

        assertNotNull(listeners);
        assertEquals(0, listeners.size());
    }
}