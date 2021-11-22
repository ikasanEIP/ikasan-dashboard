package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.ScheduledProcessEventInstance;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.scheduler.core.spec.InstanceStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ContextInstanceStateChangeEventListenerTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_context_instance_event_listener_success() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context);
        contextMachine.addContextInstanceStateChangeEventListener(event -> {
            Assert.assertNotNull(event);
            Assert.assertEquals("Context3", event.getContextInstance().getName());
            Assert.assertEquals(InstanceStatus.WAITING, event.getPreviousStatus());
            Assert.assertEquals(InstanceStatus.RUNNING, event.getPreviousStatus());
        });
        contextMachine.addContextInstanceStateChangeEventListener(event -> {
            Assert.assertNotNull(event);
            Assert.assertEquals("Context3", event.getContextInstance().getName());
            Assert.assertEquals(InstanceStatus.WAITING, event.getPreviousStatus());
            Assert.assertEquals(InstanceStatus.RUNNING, event.getPreviousStatus());
        });

        ScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        contextMachine.eventReceived(eventInstance);
    }
}
