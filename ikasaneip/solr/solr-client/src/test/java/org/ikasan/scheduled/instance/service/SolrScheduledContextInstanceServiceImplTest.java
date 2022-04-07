package org.ikasan.scheduled.instance.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.context.model.SolrContextParameterImpl;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrDryRunParameters;
import org.ikasan.scheduled.event.model.SolrSchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

public class SolrScheduledContextInstanceServiceImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao;
    private SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao;
    private ScheduledContextInstanceService service;

    private Path tmpPath;
    private EmbeddedSolrServer server;

    @Before
    public void setup() throws SolrServerException, IOException {
        tmpPath = createTempDir();
        NodeConfig config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
        scheduledContextInstanceDao.setSolrClient(server);

        scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
        scheduledContextInstanceAuditDao.setSolrClient(server);

        service = new SolrScheduledContextInstanceServiceImpl(scheduledContextInstanceDao, scheduledContextInstanceAuditDao, true);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfContextInstanceDaoIsNull() {
        service = new SolrScheduledContextInstanceServiceImpl(null, scheduledContextInstanceAuditDao, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfContextInstanceAuditDaoIsNull() {
        service = new SolrScheduledContextInstanceServiceImpl(scheduledContextInstanceDao, null, true);
    }

    @Test
    public void should_not_save_audits_if_flag_not_set() {
        ReflectionTestUtils.setField(service, "saveContextInstanceAuditRecords", Boolean.FALSE);

        ScheduledContextInstanceAuditRecord record = createAuditRecord();

        service.saveAudit(record);

        SearchResults<ScheduledContextInstanceAuditRecord> allAuditRecords = service.findAllAuditRecords(100, 0);
        assertEquals(0, allAuditRecords.getResultList().size());
    }

    @Test
    public void test_save_find_audit_record() {
        SearchResults<ScheduledContextInstanceAuditRecord> allAuditRecords = service.findAllAuditRecords(100, 0);
        assertEquals(0, allAuditRecords.getResultList().size());

        ScheduledContextInstanceAuditRecord record = createAuditRecord();

        service.saveAudit(record);

        allAuditRecords = service.findAllAuditRecords(100, 1);
        assertEquals(0, allAuditRecords.getResultList().size());

        allAuditRecords = service.findAllAuditRecords(100, 0);
        assertEquals(1, allAuditRecords.getResultList().size());

        ScheduledContextInstanceAuditRecord savedRecord = allAuditRecords.getResultList().get(0);

        ContextualisedScheduledProcessEvent<String, DryRunParameters> processEvent = record.getScheduledContextInstanceAudit().getProcessEvent();
        List<SchedulerJobInitiationEvent> jobInitiationEvents = record.getScheduledContextInstanceAudit().getSchedulerJobInitiationEvents();

        validateAuditRecord(processEvent, jobInitiationEvents, record, savedRecord);

        String id = record.getScheduledContextInstanceAudit().getPreviousContextInstance().getId();

        allAuditRecords = service.findAllAuditRecordsByContextId(id, 100, 1);
        assertEquals(0, allAuditRecords.getResultList().size());

        allAuditRecords = service.findAllAuditRecordsByContextId(id, 100, 0);
        assertEquals(1, allAuditRecords.getResultList().size());

        savedRecord = allAuditRecords.getResultList().get(0);

        validateAuditRecord(processEvent, jobInitiationEvents, record, savedRecord);

        allAuditRecords = service.findAllAuditRecordsByContextId("contextIdUnknown", 100, 0);
        assertEquals(0, allAuditRecords.getResultList().size());
    }

    private void validateAuditRecord(ContextualisedScheduledProcessEvent<String, DryRunParameters> processEventInstance,
                                     List<SchedulerJobInitiationEvent> jobInitiationEvents,
                                     ScheduledContextInstanceAuditRecord record,
                                     ScheduledContextInstanceAuditRecord savedRecord) {

        assertTrue(savedRecord.getId().startsWith("scheduledContextAuditInstanceId_"));
        // make sure we have an uuid length afterwards
        assertEquals(36, savedRecord.getId().substring("scheduledContextAuditInstanceId_".length()).length());
        assertEquals(record.getContextName(), savedRecord.getContextName());
        // make sure the timestamp is within the last little bit
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());
        assertEquals(record.getScheduledContextInstanceAudit().getPreviousContextInstance().getId(), savedRecord.getContextInstanceId());

        ScheduledContextInstanceAudit auditRecord = record.getScheduledContextInstanceAudit();
        ScheduledContextInstanceAudit savedAuditRecord = savedRecord.getScheduledContextInstanceAudit();

        assertEquals(auditRecord.getPreviousContextInstance(), savedAuditRecord.getPreviousContextInstance());
        assertEquals(auditRecord.getUpdatedContextInstance(), savedAuditRecord.getUpdatedContextInstance());

        assertEquals(processEventInstance, auditRecord.getProcessEvent());

        assertEquals(3, auditRecord.getSchedulerJobInitiationEvents().size());
        assertEquals(jobInitiationEvents, auditRecord.getSchedulerJobInitiationEvents());
    }

    @Test
    public void test_save_and_find_success() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus("RUNNING");
        service.save(scheduledContextRecord);

        ScheduledContextInstanceRecord found = service.findById(contextInstance.getId() + "_scheduledContextInstance");

        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
        Assert.assertEquals("RUNNING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());

        Assert.assertNull(service.findById("bad_id"));
    }

    @Test
    public void test_status_update_success() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus("WAITING");
        service.save(scheduledContextRecord);

        ScheduledContextInstanceRecord found = service.findById(contextInstance.getId() + "_scheduledContextInstance");

        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
        Assert.assertEquals("WAITING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());

        found.setStatus("RUNNING");

        service.save(found);

        found = service.findById(contextInstance.getId() + "_scheduledContextInstance");

        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
        Assert.assertEquals("RUNNING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());
    }

    @Test
    public void test_find_by_status_success() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
        service.save(scheduledContextRecord);

        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ERROR)).getResultList().size());
        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RELEASED)).getResultList().size());
        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.COMPLETE)).getResultList().size());
        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ON_HOLD)).getResultList().size());
        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RUNNING)).getResultList().size());
        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING)).getResultList().size());

        Assert.assertEquals(8, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED)).getResultList().size());
    }

    private ScheduledContextInstanceAuditRecord createAuditRecord() {
        ScheduledContextInstanceAudit audit = new SolrScheduledContextInstanceAuditImpl();

        ContextualisedScheduledProcessEvent<String, DryRunParameters> processEventInstance = createProcessEvent();
        audit.setProcessEvent(processEventInstance);

        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> jobInitiationEvent1 = createJobInitiationEvent();
        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> jobInitiationEvent2 = createJobInitiationEvent();
        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> jobInitiationEvent3 = createJobInitiationEvent();
        List<SchedulerJobInitiationEvent> jobInitiationEvents = List.of(jobInitiationEvent1, jobInitiationEvent2, jobInitiationEvent3);
        audit.setSchedulerJobInitiationEvents(jobInitiationEvents);

        SolrContextInstanceImpl previousContextInstance = new SolrContextInstanceImpl();
        previousContextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl previousScheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        previousScheduledContextRecord.setContextName("contextName");
        previousScheduledContextRecord.setContextInstance(previousContextInstance);
        previousScheduledContextRecord.setTimestamp(1000000L);
        previousScheduledContextRecord.setStatus("RUNNING");

        audit.setPreviousContextInstance(previousContextInstance);

        SolrContextInstanceImpl updatedContextInstance = new SolrContextInstanceImpl();
        updatedContextInstance.setId(previousContextInstance.getId());
        updatedContextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl updatedScheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        updatedScheduledContextRecord.setContextName("contextName");
        updatedScheduledContextRecord.setContextInstance(updatedContextInstance);
        updatedScheduledContextRecord.setTimestamp(1000000L);
        updatedScheduledContextRecord.setStatus("COMPLETE");

        audit.setUpdatedContextInstance(updatedContextInstance);

        ScheduledContextInstanceAuditRecord record = new SolrScheduledContextInstanceAuditRecordImpl();
        record.setContextName("contextName");
        record.setScheduledContextInstanceAudit(audit);
        return record;
    }

    private ContextualisedScheduledProcessEvent<String, DryRunParameters> createProcessEvent() {
        ContextualisedScheduledProcessEvent event = new SolrContextualisedScheduledProcessEventImpl();
        event.setJobName("Job " + RandomStringUtils.randomAlphabetic(5));
        event.setAgentName("Agent " + RandomStringUtils.randomAlphabetic(5));
        event.setJobGroup("JobGroup " + RandomStringUtils.randomAlphabetic(5));
        event.setJobDescription("JobDescription " + RandomStringUtils.randomAlphabetic(5));
        event.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
        event.setReturnCode(0);
        event.setSuccessful(true);
        event.setOutcome("Outcome: " + RandomStringUtils.randomAlphabetic(5));
        event.setResultOutput("resultOutput " + RandomStringUtils.randomAlphabetic(5));
        event.setResultError("resultError " + RandomStringUtils.randomAlphabetic(5));
        event.setPid(78321);
        event.setUser("User " + RandomStringUtils.randomAlphabetic(5));
        event.setFireTime(System.currentTimeMillis());
        event.setNextFireTime(System.currentTimeMillis() + 1000);
        event.setCompletionTime(System.currentTimeMillis() + 2000);
        event.setDryRun(true);
        event.setContextId("contextId " + RandomStringUtils.randomAlphabetic(5));
        event.setChildContextIds(List.of("childContextId1", "childContextId2"));
        event.setContextInstanceId("contextInstanceId " + RandomStringUtils.randomAlphabetic(5));
        event.setJobStarting(true);

        SolrDryRunParameters dryRunParams = new SolrDryRunParameters();
        event.setDryRunParameters(dryRunParams);

        event.setSkipped(false);

        SolrInternalEventDrivenJobImpl internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        internalEventDrivenJob.setSuccessfulReturnCodes(List.of("Code1", "Code2"));
        internalEventDrivenJob.setWorkingDirectory("workingDirectory/" + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setMinExecutionTime(System.currentTimeMillis());
        internalEventDrivenJob.setMaxExecutionTime(System.currentTimeMillis() + 3000);

        SolrContextParameterImpl contextParameter = new SolrContextParameterImpl();
        contextParameter.setType("type " + RandomStringUtils.randomAlphabetic(5));
        contextParameter.setName("name " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setContextParameters(List.of(contextParameter));
        internalEventDrivenJob.setDaysOfWeekToRun(List.of(1, 2, 3, 4, 5));

        event.setInternalEventDrivenJob(internalEventDrivenJob);

        return event;
    }

    private SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> createJobInitiationEvent() {
        SchedulerJobInitiationEvent event = new SolrSchedulerJobInitiationEventImpl();
        event.setAgentName("Agent " + RandomStringUtils.randomAlphabetic(5));
        event.setAgentUrl("AgentUrl " + RandomStringUtils.randomAlphabetic(5));
        event.setJobName("Job " + RandomStringUtils.randomAlphabetic(5));

        SolrInternalEventDrivenJobImpl internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        internalEventDrivenJob.setSuccessfulReturnCodes(List.of("EventCode1", "EventCode2"));
        internalEventDrivenJob.setWorkingDirectory("workingDirectory/" + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setMinExecutionTime(System.currentTimeMillis());
        internalEventDrivenJob.setMaxExecutionTime(System.currentTimeMillis() + 3000);

        SolrContextParameterImpl contextParameter = new SolrContextParameterImpl();
        contextParameter.setType("type " + RandomStringUtils.randomAlphabetic(5));
        contextParameter.setName("name " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setContextParameters(List.of(contextParameter));
        internalEventDrivenJob.setDaysOfWeekToRun(List.of(1, 2, 3, 4, 5));
        event.setInternalEventDrivenJob(internalEventDrivenJob);

        event.setContextId("contextId " + RandomStringUtils.randomAlphabetic(5));
        event.setChildContextIds(List.of("EventChildContextId1", "EventChildContextId2"));
        event.setContextInstanceId("contextInstanceId " + RandomStringUtils.randomAlphabetic(5));

        SolrContextParameterInstanceImpl contextParameter1 = new SolrContextParameterInstanceImpl();
        contextParameter1.setType("type1 " + RandomStringUtils.randomAlphabetic(5));
        contextParameter1.setName("name1 " + RandomStringUtils.randomAlphabetic(5));
        SolrContextParameterInstanceImpl contextParameter2 = new SolrContextParameterInstanceImpl();
        contextParameter2.setType("type1 " + RandomStringUtils.randomAlphabetic(5));
        contextParameter2.setName("name1 " + RandomStringUtils.randomAlphabetic(5));
        event.setContextParameters(List.of(contextParameter1, contextParameter2));

        event.setDryRun(true);
        SolrDryRunParameters dryRunParams = new SolrDryRunParameters();
        event.setDryRunParameters(dryRunParams);

        event.setSkipped(false);

        return event;
    }

}