package org.ikasan.orchestration.service.context.reset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.orchestration.service.JobLockCacheServiceTestImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class ContextResetServiceImplTest {

    private ContextService contextService;
    private String jsonContext;

    private ContextResetServiceImpl contextResetService;

    @Before
    public void setUp() throws IOException {
        contextService = new ContextService();
        jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        contextResetService = new ContextResetServiceImpl();
    }

    @Test
    public void shouldThrowExceptionIfContextNotFound() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null, new JobLockCacheServiceTestImpl());
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextResetService.resetContext("UNKNOWN_CONTEXT");
            fail("should not get here");
        } catch (ContextResetException e) {
            assertEquals("Could not find context for UNKNOWN_CONTEXT to reset", e.getMessage());
        }
    }

    @Test
    public void shouldResetContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null, new JobLockCacheServiceTestImpl());
        ContextMachineCache.instance().put(contextMachine);

        contextResetService.resetContext("CONTEXT-1436221681");

        InstanceStatus instanceStatus = contextMachine.getContextStatus("CONTEXT-1436221681");
        assertEquals("WAITING", instanceStatus.toString());
    }
}