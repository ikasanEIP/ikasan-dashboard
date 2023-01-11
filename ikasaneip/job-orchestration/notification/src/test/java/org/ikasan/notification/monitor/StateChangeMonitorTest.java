package org.ikasan.notification.monitor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.core.notification.MonitorManagement;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.notification.monitor.mock.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.ikasan.spec.scheduled.notification.model.Notifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.with;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class StateChangeMonitorTest {

    /** default executor service is a single thread executor */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private  ObjectMapper objectMapper;

    private String result="test";

    Monitor stateChangeMonitor;

    private MonitorManagement monitorManagement;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Mock
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @Mock
    private ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    @After
    public void tearDown() throws IOException {
        monitorManagement.unRegisterMonitor(stateChangeMonitor);
    }

    @Before
    public void setup() throws IOException {
        objectMapper = ObjectMapperFactory.newInstance();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        stateChangeMonitor = new StateChangeMonitorImpl(executorService, true);
        stateChangeMonitor.setNotifiers(Arrays.asList(new TestNotifier()));

        monitorManagement = new MonitorManagement();
        monitorManagement.registerMonitor(stateChangeMonitor);

        ContextInstance contextInstance1 = new ContextInstanceImpl();
        contextInstance1.setName("context-instance-1");

        ContextTemplate contextTemplate1 = new ContextTemplateImpl();
        contextTemplate1.setName("context-template-1");

        SchedulerJobInstance schedulerJobInstance1 = new SchedulerJobInstanceImpl();
        schedulerJobInstance1.setAgentName("agent-1");
        schedulerJobInstance1.setJobName("job-1");
        schedulerJobInstance1.setStatus(InstanceStatus.RUNNING);
        schedulerJobInstance1.setIdentifier("agent-1-job-1");

        contextInstance1.setScheduledJobs(Arrays.asList(schedulerJobInstance1));
        contextInstance1.setJobDependencies(new ArrayList<>());

        ContextMachine contextMachine1 = new ContextMachine(contextTemplate1, contextInstance1, new ScheduledContextInstanceServiceTestImpl()
            , null,null,"./target",null,null, null, this.scheduledContextService,
            this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);

        contextMachine1.init();

        ContextMachineCache.instance().put(contextMachine1);


        ContextInstance contextInstance2 = new ContextInstanceImpl();
        contextInstance2.setName("context-instance-2");

        ContextTemplate contextTemplate2 = new ContextTemplateImpl();
        contextTemplate2.setName("context-template-2");

        SchedulerJobInstance schedulerJobInstance2 = new SchedulerJobInstanceImpl();
        schedulerJobInstance2.setAgentName("agent-2");
        schedulerJobInstance2.setJobName("job-2");
        schedulerJobInstance2.setStatus(InstanceStatus.ON_HOLD);
        schedulerJobInstance2.setIdentifier("agent-2-job-2");

        contextInstance2.setScheduledJobs(Arrays.asList(schedulerJobInstance2));
        contextInstance2.setJobDependencies(new ArrayList<>());

        ContextMachine contextMachine2 = new ContextMachine(contextTemplate2, contextInstance2, new ScheduledContextInstanceServiceTestImpl()
            , null,null,"./target",null,null, null, this.scheduledContextService,
            this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);

        contextMachine2.init();

        ContextMachineCache.instance().put(contextMachine2);
    }

    @Test
    public void test_with_error_monitor() throws IOException {

        ContextualisedScheduledProcessEvent scheduledProcessEvent1 = new ContextualisedScheduledProcessEventImpl();
        scheduledProcessEvent1.setAgentName("agent-1");
        scheduledProcessEvent1.setJobName("job-1");
        scheduledProcessEvent1.setJobStarting(false);
        scheduledProcessEvent1.setSuccessful(false);

        BigQueueMessage message = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(scheduledProcessEvent1)).build();
        ContextMachineCache.instance().getFirstByContextName("context-instance-1").eventReceived( objectMapper.writeValueAsString(message));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                assertEquals("from testNotifier:ERROR", result);
            });
    }

    @Test
    public void test_with_complete_monitor() throws IOException {

        ContextualisedScheduledProcessEvent scheduledProcessEvent1 = new ContextualisedScheduledProcessEventImpl();
        scheduledProcessEvent1.setAgentName("agent-1");
        scheduledProcessEvent1.setJobName("job-1");
        scheduledProcessEvent1.setJobStarting(false);
        scheduledProcessEvent1.setSuccessful(true);

        BigQueueMessage message = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(scheduledProcessEvent1)).build();
        ContextMachineCache.instance().getFirstByContextName("context-instance-1").eventReceived( objectMapper.writeValueAsString(message));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                assertEquals("from testNotifier:COMPLETE", result);
            });
    }

    @Test
    public void test_with_start_monitor() throws IOException {

        ContextualisedScheduledProcessEvent scheduledProcessEvent1 = new ContextualisedScheduledProcessEventImpl();
        scheduledProcessEvent1.setAgentName("agent-2");
        scheduledProcessEvent1.setJobName("job-2");
        scheduledProcessEvent1.setJobStarting(true);
        scheduledProcessEvent1.setSuccessful(false);

        BigQueueMessage message = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(scheduledProcessEvent1)).build();
        ContextMachineCache.instance().getFirstByContextName("context-instance-2").eventReceived( objectMapper.writeValueAsString(message));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                assertEquals("from testNotifier:START", result);
            });
    }

    protected class TestNotifier implements Notifier<GenericNotificationDetails>
    {
        @Override
        public void invoke(GenericNotificationDetails notificationDetails)
        {
            result = "from testNotifier:"+notificationDetails.getMonitorType().name();

        }
    }
}
