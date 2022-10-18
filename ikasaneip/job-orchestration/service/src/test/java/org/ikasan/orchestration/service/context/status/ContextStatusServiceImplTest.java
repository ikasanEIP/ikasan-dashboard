package org.ikasan.orchestration.service.context.status;

import org.assertj.core.api.Assertions;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
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
    private String jsonContextStatus;
    private String jsonContextStatusContext1848727981;
    private String jsonContextStatusContext1209755884;
    private String jsonJobStatusContext1799613995;
    private String jsonJobStatusContext744167903;
    private String jsonJobStatusContext1692626050;

    private ContextStatusServiceImpl contextStatusService;

    @Before
    public void setUp() throws IOException {
        contextService = new ContextService();
        jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContextStatus = new String(new ClassPathResource("contextStatus.json").getInputStream().readAllBytes());
        jsonContextStatusContext1848727981 = new String(new ClassPathResource("contextStatusChild-CONTEXT-1848727981.json").getInputStream().readAllBytes());
        jsonContextStatusContext1209755884 = new String(new ClassPathResource("contextStatusChild-CONTEXT--1209755884.json").getInputStream().readAllBytes());
        jsonJobStatusContext1799613995 = new String(new ClassPathResource("jobStatus-scheduler-agent-1799613995.json").getInputStream().readAllBytes());
        jsonJobStatusContext744167903 = new String(new ClassPathResource("jobStatus-scheduler-agent-744167903.json").getInputStream().readAllBytes());
        jsonJobStatusContext1692626050 = new String(new ClassPathResource("jobStatus-scheduler-agent--1692626050.json").getInputStream().readAllBytes());
        contextStatusService = new ContextStatusServiceImpl();
    }

    @Test
    public void testGetContextStatus_UnknownInstance() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "UNKNOWN_INSTANCE", "scheduler-agent-1799613995");
    }

    @Test
    public void getContextStatusForJob_UnknownJobIdentifier() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "scheduler-agent-1799613995");
        assertEquals("WAITING", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent-744167903");
        assertEquals("WAITING", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent--1692626050");
        assertEquals("WAITING", status);
    }

    @Test
    public void testGetJsonContextStatus_UnknownInstance() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getJsonContextStatus("UNKNOWN_INSTANCE", "CONTEXT-1436221681");
            fail("should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find context machine for instance UNKNOWN_INSTANCE", e.getMessage());
        }
    }

    @Test
    public void testGetJsonContextStatus_UnknownContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "UNKNOWN_CONTEXT");
            fail("should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find context instance for context UNKNOWN_CONTEXT", e.getMessage());
        }
    }

    @Test
    public void testGetJsonContextStatus() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        contextStatus = formatContextStatus(contextStatus);
        Assertions.assertThat(jsonContextStatus).isEqualToIgnoringNewLines(contextStatus);

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        contextStatus = formatContextStatus(contextStatus);
        Assertions.assertThat(jsonContextStatusContext1848727981).isEqualToIgnoringNewLines(contextStatus);

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        contextStatus = formatContextStatus(contextStatus);
        Assertions.assertThat(jsonContextStatusContext1209755884).isEqualToIgnoringNewLines(contextStatus);
    }

    @Test
    public void getJsonContextStatusForJob_UnknownContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getJsonContextStatusForJob("UNKNOWN_INSTANCE", "CONTEXT-1436221681", "scheduler-agent-1799613995");
            fail("Should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find context machine for instance UNKNOWN_INSTANCE", e.getMessage());
        }
    }

    @Test
    public void getJsonContextStatusForJob_UnknownInstance() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "UNKNOWN_INSTANCE", "scheduler-agent-1799613995");
            fail("Should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find job identifier scheduler-agent-1799613995 for context UNKNOWN_INSTANCE in context machine CONTEXT-1436221681", e.getMessage());
        }
    }

    @Test
    public void getJsonContextStatusForJob_UnknownJobIdentifier() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        try {
            contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1436221681", "UNKNOWN_JOB_IDENTIFIER");
            fail("Should not get here");
        } catch (ContextStatusServiceException e) {
            assertEquals("Could not find job identifier UNKNOWN_JOB_IDENTIFIER for context CONTEXT-1436221681 in context machine CONTEXT-1436221681", e.getMessage());
        }
    }

    @Test
    public void getJsonContextStatusForJob() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "1799613995");
        jobStatus = formatContextStatus(jobStatus);
        Assertions.assertThat(jsonJobStatusContext1799613995).isEqualToIgnoringNewLines(jobStatus);

        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "744167903");
        jobStatus = formatContextStatus(jobStatus);
        Assertions.assertThat(jsonJobStatusContext744167903).isEqualToIgnoringNewLines(jobStatus);

        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "-1692626050");
        jobStatus = formatContextStatus(jobStatus);
        Assertions.assertThat(jsonJobStatusContext1692626050).isEqualToIgnoringNewLines(jobStatus);
    }

    /** Helper method to replace the dynamic created String in the JSON, i.e. the creation time and the UUID */
    private String formatContextStatus(String jsonString) {
        String formatJson = jsonString;
        // Update id, createdDateTime and updatedDateTime to a static value as they are dynamically in the generated
        formatJson = RegExUtils.replaceAll(formatJson, Pattern.compile("\"id\" : \".*"), "\"id\" : \"UUID\",");
        formatJson = RegExUtils.replaceAll(formatJson, Pattern.compile("\"createdDateTime\" :.*"), "\"createdDateTime\" : 1665669706022,");
        formatJson = RegExUtils.replaceAll(formatJson, Pattern.compile("\"updatedDateTime\" :.*"), "\"updatedDateTime\" : 1665669706022,");
        return formatJson;
    }
}