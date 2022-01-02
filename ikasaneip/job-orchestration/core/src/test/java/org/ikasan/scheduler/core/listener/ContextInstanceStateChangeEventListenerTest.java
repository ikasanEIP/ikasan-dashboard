package org.ikasan.scheduler.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduler.core.model.job.InternalEventDrivenJobImpl;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ContextInstanceStateChangeEventListenerTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private String queueDir = "./target";

    @Test
    public void test_context_instance_event_listener_success() throws IOException, InterruptedException {
        Context context = this.contextService.getContext(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        InternalEventDrivenJobImpl job5 = new InternalEventDrivenJobImpl();
        job5.setContextParameters(List.of(getContextParameter("test1", "String"), getContextParameter("test2", "String")));
        internalEventDrivenJobs.put("agentName5-jobName5", job5);
        InternalEventDrivenJobImpl job6 = new InternalEventDrivenJobImpl();
        job6.setContextParameters(List.of(getContextParameter("test3", "String")
            , getContextParameter("test4", "String")
            , getContextParameter("test5", "String")));
        internalEventDrivenJobs.put("agentName6-jobName6", job6);
        internalEventDrivenJobs.put("agentName7-jobName7", new InternalEventDrivenJobImpl());
        InternalEventDrivenJobImpl job8 = new InternalEventDrivenJobImpl();
        job8.setContextParameters(List.of(getContextParameter("test4", "String")
            , getContextParameter("test5", "String")
            , getContextParameter("test6", "String")
            , getContextParameter("test7", "String")));
        internalEventDrivenJobs.put("agentName8-jobName8", job8);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), internalEventDrivenJobs, this.queueDir);
        contextMachine.init();
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

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        ObjectMapper mapper = new ObjectMapper();
        contextMachine.eventReceived(mapper.writeValueAsString(eventInstance));

        Thread.sleep(1000);
        contextMachine.teardown();
    }
}
