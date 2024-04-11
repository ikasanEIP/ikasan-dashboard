package org.ikasan.orchestration.service.context.reset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

@RunWith(MockitoJUnitRunner.class)
public class ContextResetServiceImplTest {

    private ContextService contextService;
    private String jsonContext;

    private ContextResetServiceImpl contextResetService;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Mock
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @Mock
    private ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    @Before
    public void setUp() throws IOException {
        contextService = new ContextService();
        jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        contextResetService = new ContextResetServiceImpl();
    }

    @Test(expected = ContextResetException.class)
    public void should_throw_exception_if_context_not_found() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, this.scheduledContextService
            , this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextResetService.resetContext("UNKNOWN_CONTEXT", false);
            fail("should not get here");
        } catch (ContextResetException e) {
            assertEquals("Could not find context for context name [UNKNOWN_CONTEXT] to reset", e.getMessage());
            throw e;
        }
    }

    @Test
    @Ignore
    // todo need this test to mock the contsructor args
    public void shouldResetContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, this.scheduledContextService
            , this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);
        ContextMachineCache.instance().put(contextMachine);

        contextResetService.resetContext("CONTEXT-1436221681", false);

        InstanceStatus instanceStatus = contextMachine.getContextStatus("CONTEXT-1436221681");
        assertEquals("WAITING", instanceStatus.toString());
    }
}