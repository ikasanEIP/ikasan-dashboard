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

public class SchedulerJobInstanceStateChangeEventListenerTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_scheduler_job_instance_event_listener_success() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context);
        contextMachine.addSchedulerJobStateChangeEventListener(event -> {
            Assert.assertNotNull(event);
            Assert.assertEquals("agentName3", event.getSchedulerJobInstance().getAgentName());
            Assert.assertEquals("jobName3", event.getSchedulerJobInstance().getJobName());
            Assert.assertEquals("agentName3-jobName3", event.getSchedulerJobInstance().getIdentifier());
            Assert.assertEquals(InstanceStatus.WAITING, event.getPreviousStatus());
            Assert.assertEquals(InstanceStatus.COMPLETE, event.getPreviousStatus());
        });
        contextMachine.addSchedulerJobStateChangeEventListener(event -> {
            Assert.assertNotNull(event);
            Assert.assertEquals("agentName3", event.getSchedulerJobInstance().getAgentName());
            Assert.assertEquals("jobName3", event.getSchedulerJobInstance().getJobName());
            Assert.assertEquals("agentName3-jobName3", event.getSchedulerJobInstance().getIdentifier());
            Assert.assertEquals(InstanceStatus.WAITING, event.getPreviousStatus());
            Assert.assertEquals(InstanceStatus.COMPLETE, event.getPreviousStatus());
        });

        ScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        contextMachine.eventReceived(eventInstance);
    }
}
