package org.ikasan.orchestration.service.context.status;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RegExUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
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
    private String jsonContextCONTEXTNAMEPLAN;
    private String jsonContextCONTEXTNAMEPLANSTATUS;
    private String getJsonContextStatusForAllJobResult;

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
        jsonContextCONTEXTNAMEPLAN = new String(new ClassPathResource("context-status-plan.json").getInputStream().readAllBytes());
        jsonContextCONTEXTNAMEPLANSTATUS = new String(new ClassPathResource("context-status-instance.json").getInputStream().readAllBytes());
        getJsonContextStatusForAllJobResult = new String(new ClassPathResource("getJsonContextStatusForAllJobResult.json").getInputStream().readAllBytes());
        contextStatusService = new ContextStatusServiceImpl();
    }

    @After
    public void tidyup() {
        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void testGetContextStatus_UnknownInstance() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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
        contextInstance.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);
    }

    @Test
    public void testGetContextWithTwoRunningStatus() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("77777777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        String contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING, 77777777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING, 77777777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING, 77777777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);
    }

    @Test
    public void testGetContextWithOneRunningOnePreparedStatus() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("77777777-8ee0-4354-b390-f2ad0712ca63");
        contextInstance2.setStatus(InstanceStatus.PREPARED);

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        // contextMachine2 is expected to be ignored
        String contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);

        contextStatus = contextStatusService.getContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", contextStatus);
    }

    @Test
    public void getContextStatusForJob_UnknownContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null
            , null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "UNKNOWN_INSTANCE", "scheduler-agent-1799613995");
    }

    @Test
    public void getContextStatusForJob_UnknownJobIdentifier() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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
        contextInstance.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "scheduler-agent-1799613995");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent-744167903");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent--1692626050");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);
    }

    @Test
    public void getContextStatusForJobTwoRunning() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("77777777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        String status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "scheduler-agent-1799613995");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING, 77777777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent-744167903");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING, 77777777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent--1692626050");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING, 77777777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);
    }

    @Test
    public void getContextStatusForJobOneRunningOnePrepared() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("77777777-8ee0-4354-b390-f2ad0712ca63");
        contextInstance2.setStatus(InstanceStatus.PREPARED);

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        String status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "scheduler-agent-1799613995");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent-744167903");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);

        status = contextStatusService.getContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "scheduler-agent--1692626050");
        assertEquals("{3e774777-8ee0-4354-b390-f2ad0712ca63=WAITING}", status);
    }

    @Test
    public void testGetJsonContextStatus_UnknownInstance() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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
        contextInstance.setId("6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatus, contextStatus, false);

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatusContext1848727981, contextStatus, false);

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatusContext1209755884, contextStatus, false);
    }

    @Test
    public void testGetJsonContextStatusTwoRunning() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("44444444-3b4c-4cf0-a84f-95f64ffc3ac5");

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        String contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatus, contextStatus, false);
        Assert.assertTrue(contextStatus.contains("\"44444444-3b4c-4cf0-a84f-95f64ffc3ac5\""));
        Assert.assertTrue(contextStatus.contains("\"6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5\""));

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatusContext1848727981, contextStatus, false);
        Assert.assertTrue(contextStatus.contains("\"44444444-3b4c-4cf0-a84f-95f64ffc3ac5\""));
        Assert.assertTrue(contextStatus.contains("\"6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5\""));

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatusContext1209755884, contextStatus, false);
        Assert.assertTrue(contextStatus.contains("\"44444444-3b4c-4cf0-a84f-95f64ffc3ac5\""));
        Assert.assertTrue(contextStatus.contains("\"6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5\""));
    }

    @Test
    public void testGetJsonContextStatusOneRunningOnePrepared() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("44444444-3b4c-4cf0-a84f-95f64ffc3ac5");
        contextInstance2.setStatus(InstanceStatus.PREPARED);

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        String contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1436221681");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatus, contextStatus, false);
        Assert.assertTrue(!contextStatus.contains("\"44444444-3b4c-4cf0-a84f-95f64ffc3ac5\""));
        Assert.assertTrue(contextStatus.contains("\"6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5\""));

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT-1848727981");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatusContext1848727981, contextStatus, false);
        Assert.assertTrue(!contextStatus.contains("\"44444444-3b4c-4cf0-a84f-95f64ffc3ac5\""));
        Assert.assertTrue(contextStatus.contains("\"6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5\""));

        contextStatus = contextStatusService.getJsonContextStatus("CONTEXT-1436221681", "CONTEXT--1209755884");
        contextStatus = formatContextStatus(contextStatus);

        JSONAssert.assertEquals(jsonContextStatusContext1209755884, contextStatus, false);
        Assert.assertTrue(!contextStatus.contains("\"44444444-3b4c-4cf0-a84f-95f64ffc3ac5\""));
        Assert.assertTrue(contextStatus.contains("\"6ae44543-3b4c-4cf0-a84f-95f64ffc3ac5\""));
    }

    @Test
    public void getJsonContextStatusForJob_UnknownContext() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
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
        contextInstance.setId("d772ed1d-84c7-4cc2-bb54-4c5ba378a515");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "1799613995");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext1799613995, jobStatus, JSONCompareMode.LENIENT);

        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "744167903");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext744167903, jobStatus, JSONCompareMode.LENIENT);

        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "-1692626050");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext1692626050, jobStatus, JSONCompareMode.LENIENT);
    }

    @Test
    public void getJsonContextStatusForJobTwoRunning() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("d772ed1d-84c7-4cc2-bb54-4c5ba378a515");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("d772ed1d-8888-8888-8888-4c5ba378a515");

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        String jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "1799613995");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext1799613995, jobStatus, JSONCompareMode.LENIENT);
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-84c7-4cc2-bb54-4c5ba378a515\""));
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-8888-8888-8888-4c5ba378a515\""));

        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "744167903");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext744167903, jobStatus, JSONCompareMode.LENIENT);
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-84c7-4cc2-bb54-4c5ba378a515\""));
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-8888-8888-8888-4c5ba378a515\""));


        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "-1692626050");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext1692626050, jobStatus, JSONCompareMode.LENIENT);
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-84c7-4cc2-bb54-4c5ba378a515\""));
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-8888-8888-8888-4c5ba378a515\""));
    }

    @Test
    public void getJsonContextStatusForJobOneRunningOnePrepared() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);
        contextInstance.setId("d772ed1d-84c7-4cc2-bb54-4c5ba378a515");

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("d772ed1d-8888-8888-8888-4c5ba378a515");
        contextInstance2.setStatus(InstanceStatus.PREPARED);

        ContextMachine contextMachine2 = new ContextMachine(context, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine2);

        String jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1616645609", "1799613995");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext1799613995, jobStatus, JSONCompareMode.LENIENT);
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-84c7-4cc2-bb54-4c5ba378a515\""));
        Assert.assertTrue(!jobStatus.contains("\"d772ed1d-8888-8888-8888-4c5ba378a515\""));

        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "744167903");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext744167903, jobStatus, JSONCompareMode.LENIENT);
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-84c7-4cc2-bb54-4c5ba378a515\""));
        Assert.assertTrue(!jobStatus.contains("\"d772ed1d-8888-8888-8888-4c5ba378a515\""));


        jobStatus = contextStatusService.getJsonContextStatusForJob("CONTEXT-1436221681", "CONTEXT-1589183395", "-1692626050");
        jobStatus = formatContextStatus(jobStatus);
        JSONAssert.assertEquals(jsonJobStatusContext1692626050, jobStatus, JSONCompareMode.LENIENT);
        Assert.assertTrue(jobStatus.contains("\"d772ed1d-84c7-4cc2-bb54-4c5ba378a515\""));
        Assert.assertTrue(!jobStatus.contains("\"d772ed1d-8888-8888-8888-4c5ba378a515\""));
    }

    @Test
    public void getJsonContextMachineStatusEmpty() throws Exception {
        String jobStatus = contextStatusService.getJsonContextMachineStatus(true);
        String expected = "{ \"contextMachineStatusList\" : [ ] }";
        JSONAssert.assertEquals(expected, jobStatus, JSONCompareMode.LENIENT);
    }

    @Test
    public void getJsonContextMachineStatusForAllJob() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContextCONTEXTNAMEPLAN);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContextCONTEXTNAMEPLANSTATUS);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);

        ContextTemplate context2 = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine2 = new ContextMachine(context2, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);

        ContextMachineCache.instance().put(contextMachine);
        ContextMachineCache.instance().put(contextMachine2);

        String jobStatus = contextStatusService.getJsonContextMachineStatus(true);
        String expected = "{\n" +
            "  \"contextMachineStatusList\" : [ {\n" +
            "    \"contextName\" : \"CONTEXT_NAME_PLAN\",\n" +
            "    \"contextInstanceId\" : \"d9094469-1345-4878-aec6-3b8102b65412\",\n" +
            "    \"instanceStatus\" : \"ERROR\"\n" +
            "  }, {\n" +
            "    \"contextName\" : \"CONTEXT-1436221681\",\n" +
            "    \"contextInstanceId\" : \"3e774777-8ee0-4354-b390-f2ad0712ca63\",\n" +
            "    \"instanceStatus\" : \"WAITING\"\n" +
            "  } ]\n" +
            "}";
        JSONAssert.assertEquals(expected, jobStatus, JSONCompareMode.LENIENT);
    }

    @Test
    public void getJsonContextMachineStatusPreparedTrue() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContextCONTEXTNAMEPLAN);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContextCONTEXTNAMEPLANSTATUS);
        contextInstance.setStatus(InstanceStatus.PREPARED);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);

        ContextTemplate context2 = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine2 = new ContextMachine(context2, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);

        ContextMachineCache.instance().put(contextMachine);
        ContextMachineCache.instance().put(contextMachine2);

        String jobStatus = contextStatusService.getJsonContextMachineStatus(true);
        String expected = "{\n" +
            "  \"contextMachineStatusList\" : [ {\n" +
            "    \"contextName\" : \"CONTEXT-1436221681\",\n" +
            "    \"contextInstanceId\" : \"3e774777-8ee0-4354-b390-f2ad0712ca63\",\n" +
            "    \"instanceStatus\" : \"WAITING\"\n" +
            "  }, {\n" +
            "    \"contextName\" : \"CONTEXT_NAME_PLAN\",\n" +
            "    \"contextInstanceId\" : \"d9094469-1345-4878-aec6-3b8102b65412\",\n" +
            "    \"instanceStatus\" : \"PREPARED\"\n" +
            "  } ]\n" +
            "}";
        JSONAssert.assertEquals(expected, jobStatus, JSONCompareMode.LENIENT);
    }

    @Test
    public void getJsonContextMachineStatusPreparedFalse() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContextCONTEXTNAMEPLAN);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContextCONTEXTNAMEPLANSTATUS);
        contextInstance.setStatus(InstanceStatus.PREPARED);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);

        ContextTemplate context2 = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance2 = this.contextService.getContextInstance(jsonContext);
        contextInstance2.setId("3e774777-8ee0-4354-b390-f2ad0712ca63");

        ContextMachine contextMachine2 = new ContextMachine(context2, contextInstance2, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);

        ContextMachineCache.instance().put(contextMachine);
        ContextMachineCache.instance().put(contextMachine2);

        String jobStatus = contextStatusService.getJsonContextMachineStatus(false);
        String expected = "{\n" +
            "  \"contextMachineStatusList\" : [ {\n" +
            "    \"contextName\" : \"CONTEXT-1436221681\",\n" +
            "    \"contextInstanceId\" : \"3e774777-8ee0-4354-b390-f2ad0712ca63\",\n" +
            "    \"instanceStatus\" : \"WAITING\"\n" +
            "  } ]\n" +
            "}";
        JSONAssert.assertEquals(expected, jobStatus, JSONCompareMode.LENIENT);
    }

    @Test(expected = ContextStatusServiceException.class)
    public void getJsonContextStatusEmpty() throws Exception {
        contextStatusService.getJsonContextJobStatus(null, null);
    }

    @Test
    public void getJsonContextStatusForAllJob() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContextCONTEXTNAMEPLAN);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContextCONTEXTNAMEPLANSTATUS);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        String jobStatus = contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("d9094469-1345-4878-aec6-3b8102b65412", contextMachine));
        JSONAssert.assertEquals(getJsonContextStatusForAllJobResult, jobStatus, JSONCompareMode.LENIENT);
    }

    @Test
    public void getJsonContextStatusForAllJobThatErrored() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContextCONTEXTNAMEPLAN);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContextCONTEXTNAMEPLANSTATUS);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        // NOTE we are not passing the internalEventDrivenJobInstance so target targetResidingContext will always be false + end time will equal start time

        String jobStatus = contextStatusService.getJsonContextJobStatus(InstanceStatus.ERROR, Collections.singletonMap("d9094469-1345-4878-aec6-3b8102b65412", contextMachine));
        String expected = "{\n" +
            "  \"jobPlans\" : [ {\n" +
            "    \"contextName\" : \"CONTEXT_NAME_PLAN\",\n" +
            "    \"contextInstanceId\" : \"d9094469-1345-4878-aec6-3b8102b65412\",\n" +
            "    \"instanceStatus\" : \"ERROR\",\n" +
            "    \"jobDetails\" : [ {\n" +
            "      \"jobName\" : \"INVOKE_SOME_TEST_OBSERVER_some-test-name-2_2_0-Suffix111\",\n" +
            "      \"childContextName\" : [ \"TESTING_ASSURED_DEV_OPS\", \"TESTING_PARALLEL_STREAM_DEV_OPS\" ],\n" +
            "      \"instanceStatus\" : \"ERROR\",\n" +
            "      \"targetResidingContextOnly\" : false,\n" +
            "      \"startTime\" : 1694138409993,\n" +
            "      \"endTime\" : 1694138409993\n" +
            "    } ]\n" +
            "  } ]\n" +
            "}";
        JSONAssert.assertEquals(expected, jobStatus, JSONCompareMode.LENIENT);
    }

    @Test
    public void getJsonContextStatusForAllJobThatCompleted() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContextCONTEXTNAMEPLAN);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContextCONTEXTNAMEPLANSTATUS);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null
            , null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        // NOTE we are not passing the internalEventDrivenJobInstance so target targetResidingContext will always be false + end time will equal start time

        String jobStatus = contextStatusService.getJsonContextJobStatus(InstanceStatus.COMPLETE, Collections.singletonMap("d9094469-1345-4878-aec6-3b8102b65412", contextMachine));
        String expected = "{\n" +
            "  \"jobPlans\" : [ {\n" +
            "    \"contextName\" : \"CONTEXT_NAME_PLAN\",\n" +
            "    \"contextInstanceId\" : \"d9094469-1345-4878-aec6-3b8102b65412\",\n" +
            "    \"instanceStatus\" : \"ERROR\",\n" +
            "    \"jobDetails\" : [ {\n" +
            "      \"jobName\" : \"JOB_TEST_TRUE_ScheduledJob_03:00:00\",\n" +
            "      \"childContextName\" : [ \"JOB_TEST_TRUE\" ],\n" +
            "      \"instanceStatus\" : \"COMPLETE\",\n" +
            "      \"targetResidingContextOnly\" : false,\n" +
            "      \"startTime\" : 1694138400008,\n" +
            "      \"endTime\" : 1694138400008\n" +
            "    }, {\n" +
            "      \"jobName\" : \"JOB_TEST_TRUE\",\n" +
            "      \"childContextName\" : [ \"some-test-name1\", \"JOB_TEST_TRUE\" ],\n" +
            "      \"instanceStatus\" : \"COMPLETE\",\n" +
            "      \"targetResidingContextOnly\" : false,\n" +
            "      \"startTime\" : 1694138401336,\n" +
            "      \"endTime\" : 1694138401336\n" +
            "    }, {\n" +
            "      \"jobName\" : \"INVOKE_SOME_TEST_OBSERVER_some-test-name-2_1_0-xxxxx\",\n" +
            "      \"childContextName\" : [ \"TESTING_ASSURED_DEV_OPS\", \"some-test-name1\" ],\n" +
            "      \"instanceStatus\" : \"COMPLETE\",\n" +
            "      \"targetResidingContextOnly\" : false,\n" +
            "      \"startTime\" : 1694138407055,\n" +
            "      \"endTime\" : 1694138407055\n" +
            "    }, {\n" +
            "      \"jobName\" : \"POP_ABCDE_CREATE_CAR_LISTS_FOR_WELCOME_ScheduledJob_05:00:00\",\n" +
            "      \"childContextName\" : [ \"POP_ABCDE_MOTO\" ],\n" +
            "      \"instanceStatus\" : \"COMPLETE\",\n" +
            "      \"targetResidingContextOnly\" : false,\n" +
            "      \"startTime\" : 1694145600015,\n" +
            "      \"endTime\" : 1694145600015\n" +
            "    } ]\n" +
            "  } ]\n" +
            "}";
        JSONAssert.assertEquals(expected, jobStatus, JSONCompareMode.LENIENT);
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