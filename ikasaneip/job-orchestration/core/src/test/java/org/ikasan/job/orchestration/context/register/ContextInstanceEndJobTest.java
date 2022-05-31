package org.ikasan.job.orchestration.context.register;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.util.TestUtils;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.serialiser.model.JobExecutionContextDefaultImpl;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceEndJobTest {

    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    private ContextInstanceEndJob contextInstanceEndJob;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private String contextName;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);
        contextInstanceEndJob = new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, "0 0 2 ? * * *", scheduledContextInstanceService);

        TestUtils.resetContextMachineCache();
        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @After
    public void tearDown() {
        TestUtils.resetContextMachineCache();
    }

    @Test
    public void execute_null_contextmachine_should_not_npe() throws Exception {
        JobExecutionContextDefaultImpl jobExecutionContext = new JobExecutionContextDefaultImpl();

        contextInstanceEndJob.execute(jobExecutionContext);

        verifyNoMoreInteractions(scheduledContextInstanceService);

        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @Test
    public void execute_should_save_instance_as_ended() throws Exception {
        String jsonContext = new String(new ClassPathResource("data/context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"Context1\"", "\"name\": \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null, JobLockCacheImpl.instance(), null);

        ContextMachineCache.instance().put(contextMachine);

        JobExecutionContextDefaultImpl jobExecutionContext = new JobExecutionContextDefaultImpl();

        contextInstanceEndJob.execute(jobExecutionContext);

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.ENDED.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(scheduledContextInstanceService);

        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

}