package org.ikasan.orchestration.service.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class ContextStatusServiceImplTest {

    private ContextService contextService;
    private String jsonContext;

    private ContextStatusServiceImpl contextStatusService;

    @Before
    public void setUp() throws IOException {
        contextService = new ContextService();
        jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        contextStatusService = new ContextStatusServiceImpl();
    }

    @Test
    public void testGetContextStatus_UnknownInstance() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getContextStatus("UNKNOWN_INSTANCE", "CONTEXT-1436221681");
            fail("should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find context machine for instance UNKNOWN_INSTANCE", e.getMessage());
        }
    }

    @Test
    public void testGetContextStatus_UnknownContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getContextStatus("CONTEXT-1436221681", "UNKNOWN_CONTEXT");
            fail("should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find context UNKNOWN_CONTEXT in context machine CONTEXT-1436221681", e.getMessage());
        }
    }

    @Test
    public void testGetContextStatus() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        assertEquals("WAITING", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        assertEquals("WAITING", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        assertEquals("WAITING", contextStatus);
    }

    @Test
    public void getContextStatusForJob_UnknownContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getContextStatusForJob("UNKNOWN_INSTANCE", "CONTEXT-1436221681", "scheduler-agent-1799613995");
            fail("Should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find context machine for instance UNKNOWN_INSTANCE", e.getMessage());
        }
    }

    @Test(expected = NullPointerException.class)
    public void getContextStatusForJob_UnknownInstance_getsNPE() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "UNKNOWN_INSTANCE", "scheduler-agent-1799613995");
    }

    @Test
    public void getContextStatusForJob_UnknownJobIdentifier() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1436221681", "UNKNOWN_JOB_IDENTIFIER");
            fail("Should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find job identifier UNKNOWN_JOB_IDENTIFIER for context CONTEXT-1436221681 in context machine CONTEXT-1436221681", e.getMessage());
        }
    }

    @Test
    public void getContextStatusForJob() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "scheduler-agent-1799613995");
        assertEquals("WAITING", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent-744167903");
        assertEquals("WAITING", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent--1692626050");
        assertEquals("WAITING", status);
    }

}