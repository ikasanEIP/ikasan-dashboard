package org.ikasan.scheduler.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SchedulerJobInstanceStateChangeEventListenerTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_scheduler_job_instance_event_listener_success() throws IOException, InterruptedException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, new ScheduledContextInstanceServiceTestImpl());
        contextMachine.init();
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

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        ObjectMapper mapper = new ObjectMapper();
        contextMachine.eventReceived(mapper.writeValueAsString(eventInstance));

        Thread.sleep(1000);
        contextMachine.teardown();
    }
}
