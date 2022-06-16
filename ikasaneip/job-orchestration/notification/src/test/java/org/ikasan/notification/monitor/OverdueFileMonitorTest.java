package org.ikasan.notification.monitor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.notification.NotificationConfiguration;
import org.ikasan.notification.monitor.mock.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.notification.monitor.mock.SchedulerJobServiceTestImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.ikasan.spec.scheduled.notification.model.Notifier;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.with;
import static org.junit.Assert.assertEquals;

public class OverdueFileMonitorTest {

    /** default executor service is a single thread executor */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private String result="test";

    private ObjectMapper objectMapper;

    @Before
    public void startup() throws IOException {
        objectMapper = ObjectMapperFactory.newInstance();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

        ContextMachine contextMachine1 = new ContextMachine(contextTemplate1, contextInstance1, new ScheduledContextInstanceServiceTestImpl(), null,"./target",null,null, null);
        contextMachine1.init();

        ContextMachineCache.instance().put(contextMachine1);

    }

    @Test
    public void test_with_completed_status() throws IOException {

        ContextualisedScheduledProcessEvent scheduledProcessEvent1 = new ContextualisedScheduledProcessEventImpl();
        scheduledProcessEvent1.setAgentName("agent-1");
        scheduledProcessEvent1.setJobName("job-1");
        scheduledProcessEvent1.setJobStarting(false);
        scheduledProcessEvent1.setSuccessful(true);

        // start test
        NotificationConfiguration notificationConfiguration = new NotificationConfiguration();

        Monitor overdueFileMonitor = notificationConfiguration.overdueFileMonitor(Arrays.asList(new TestNotifier()));

        ContextMachineCache.instance().getByContextName("context-instance-1").eventReceived( objectMapper.writeValueAsString(scheduledProcessEvent1));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(60, TimeUnit.SECONDS).await()
            .during(29, TimeUnit.SECONDS)
            .atMost(90, TimeUnit.SECONDS)
            .until(checkResult());

    }

    @Test
    @Ignore
    public void test_with_running_and_overdued() throws IOException {

        ContextualisedScheduledProcessEvent scheduledProcessEvent1 = new ContextualisedScheduledProcessEventImpl();
        scheduledProcessEvent1.setAgentName("agent-1");
        scheduledProcessEvent1.setJobName("job-1");
        scheduledProcessEvent1.setJobStarting(true);
        scheduledProcessEvent1.setSuccessful(false);

        // start test
        Monitor overdueFileMonitor = new OverdueFileMonitorImpl(30, executorService, new SchedulerJobServiceTestImpl());
        overdueFileMonitor.setNotifiers(Arrays.asList(new TestNotifier()));

        ContextMachineCache.instance().getByContextName("context-instance-1").eventReceived( objectMapper.writeValueAsString(scheduledProcessEvent1));


        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(60, TimeUnit.SECONDS).await()
            .atMost(90, TimeUnit.SECONDS).untilAsserted(() -> {
                assertEquals("from testNotifier!", result);
            });
    }

    private Callable<Boolean> checkResult() {
        return new Callable<Boolean>() {
            public Boolean call() {
                return result.equals("test");
            }
        };
    }

    protected class TestNotifier implements Notifier<GenericNotificationDetails>
    {
        @Override
        public void invoke(GenericNotificationDetails notificationDetails)
        {
            result = "from testNotifier!";

        }
    }
}

