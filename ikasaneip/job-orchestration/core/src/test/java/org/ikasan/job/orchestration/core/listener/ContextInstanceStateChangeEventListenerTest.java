package org.ikasan.job.orchestration.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceStateChangeEventListenerTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private String queueDir = "./target";

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Mock
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @Mock
    private ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    @Test
    public void test_context_instance_event_listener_success() throws IOException, InterruptedException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job5 = new InternalEventDrivenJobInstanceImpl();
        job5.setContextParameters(List.of(getContextParameter("test1", "String"), getContextParameter("test2", "String")));
        internalEventDrivenJobs.put("agentName5-jobName5", job5);
        InternalEventDrivenJobInstanceImpl job6 = new InternalEventDrivenJobInstanceImpl();
        job6.setContextParameters(List.of(getContextParameter("test3", "String")
            , getContextParameter("test4", "String")
            , getContextParameter("test5", "String")));
        internalEventDrivenJobs.put("agentName6-jobName6", job6);
        internalEventDrivenJobs.put("agentName7-jobName7", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job8 = new InternalEventDrivenJobInstanceImpl();
        job8.setContextParameters(List.of(getContextParameter("test4", "String")
            , getContextParameter("test5", "String")
            , getContextParameter("test6", "String")
            , getContextParameter("test7", "String")));
        internalEventDrivenJobs.put("agentName8-jobName8", job8);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService
            , this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);
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

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);

        ObjectMapper mapper = ObjectMapperFactory.newInstance();
        BigQueueMessage message = new BigQueueMessageBuilder().withMessage(mapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(mapper.writeValueAsString(message));

        Thread.sleep(1000);
        contextMachine.teardown();
    }
}
