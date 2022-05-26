package org.ikasan.scheduled.instance.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.context.model.SolrContextParameterImpl;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrDryRunParameters;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

public class SolrSchedulerJobInstanceServiceImplTest extends SolrTestCaseJ4 {

    private SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao;
    private SchedulerJobInstanceService service;

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

        solrSchedulerJobInstanceDao = new SolrSchedulerJobInstanceDaoImpl();
        solrSchedulerJobInstanceDao.setSolrClient(server);

        service = new SolrSchedulerJobInstanceServiceImpl(solrSchedulerJobInstanceDao);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfContextInstanceDaoIsNull() {
        service = new SolrSchedulerJobInstanceServiceImpl(null);
    }

    @Test
    public void test_save_and_find_success() {
        SolrSchedulerJobInstanceImpl solrSchedulerJobInstance = new SolrSchedulerJobInstanceImpl();
        solrSchedulerJobInstance.setJobName("jobName");
        solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());


        SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
        schedulerJobInstanceRecord.setJobName("jobName");
        schedulerJobInstanceRecord.setContextInstanceId("contexInstance");
        schedulerJobInstanceRecord.setContextName("contextName");
        schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
        schedulerJobInstanceRecord.setTimestamp(1000000L);
        schedulerJobInstanceRecord.setStatus("RUNNING");
        service.save(schedulerJobInstanceRecord);

        SchedulerJobInstanceRecord found = service.findById(schedulerJobInstanceRecord.getJobName() + "_"
            + schedulerJobInstanceRecord.getContextInstanceId() + "_schedulerJobInstance");

        Assert.assertEquals(schedulerJobInstanceRecord.getJobName() + "_"
            + schedulerJobInstanceRecord.getContextInstanceId() + "_schedulerJobInstance", found.getId());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("jobName", found.getSchedulerJobInstance().getJobName());
        Assert.assertTrue(found.getSchedulerJobInstance().getScheduledProcessEvent().getJobName().startsWith("Job"));
        Assert.assertTrue(found.getSchedulerJobInstance().getScheduledProcessEvent().getAgentName().startsWith("Agent"));
        Assert.assertTrue(found.getSchedulerJobInstance().getScheduledProcessEvent().getJobDescription().startsWith("JobDescription"));
        Assert.assertTrue(found.getSchedulerJobInstance().getScheduledProcessEvent().getCommandLine().startsWith("commandLine"));
        Assert.assertTrue(found.getSchedulerJobInstance().getScheduledProcessEvent().getResultOutput().startsWith("resultOutput"));
        Assert.assertTrue(found.getSchedulerJobInstance().getScheduledProcessEvent().getResultError().startsWith("resultError"));
        Assert.assertTrue(((ContextualisedScheduledProcessEvent)found.getSchedulerJobInstance().getScheduledProcessEvent())
            .getContextId().startsWith("contextId"));
        Assert.assertTrue(((ContextualisedScheduledProcessEvent)found.getSchedulerJobInstance().getScheduledProcessEvent())
            .getChildContextIds().contains("childContextId1"));
        Assert.assertTrue(((ContextualisedScheduledProcessEvent)found.getSchedulerJobInstance().getScheduledProcessEvent())
            .getChildContextIds().contains("childContextId2"));
        Assert.assertEquals(78321, found.getSchedulerJobInstance().getScheduledProcessEvent().getPid());
        Assert.assertEquals("RUNNING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());

        Assert.assertNull(service.findById("bad_id"));
    }

    @Test
    public void test_find_by_filter() {
        IntStream.range(0, 371).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance1",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance2",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance3",
                "context2", "context2Job"+i));
        });

        SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context1");

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(646, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 0, 0, null, null);

        Assert.assertEquals(0, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context1");
        filter.setContextInstanceId("contextInstance1");

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(371, searchResults.getResultList().size());
        Assert.assertEquals(371, searchResults.getTotalNumberOfResults());

        filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context1");
        filter.setContextInstanceId("contextInstance2");

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(275, searchResults.getResultList().size());
        Assert.assertEquals(275, searchResults.getTotalNumberOfResults());

        filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context1");
        filter.setContextInstanceId("contextInstance2");
        filter.setJobName("job1");

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(1, searchResults.getResultList().size());
        Assert.assertEquals(1, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_context_name() {
        IntStream.range(0, 371).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance1",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance2",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance3",
                "context2", "context2Job"+i));
        });


        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getSchedulerJobInstancesByContextName("context1", 1000, 0, null, null);

        Assert.assertEquals(646, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        searchResults = this.solrSchedulerJobInstanceDao
            .getSchedulerJobInstancesByContextName("context2", 1000, 0, null, null);

        Assert.assertEquals(275, searchResults.getResultList().size());
        Assert.assertEquals(275, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_context_instance_id() {
        IntStream.range(0, 371).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance1",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance2",
                "context1", "job"+i));
        });

        IntStream.range(0, 167).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance3",
                "context2", "context2Job"+i));
        });


        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getSchedulerJobInstancesByContextInstanceId("contextInstance1", 1000, 0, null, null);

        Assert.assertEquals(371, searchResults.getResultList().size());
        Assert.assertEquals(371, searchResults.getTotalNumberOfResults());

        searchResults = this.solrSchedulerJobInstanceDao
            .getSchedulerJobInstancesByContextInstanceId("contextInstance2", 1000, 0, null, null);

        Assert.assertEquals(275, searchResults.getResultList().size());
        Assert.assertEquals(275, searchResults.getTotalNumberOfResults());

        searchResults = this.solrSchedulerJobInstanceDao
            .getSchedulerJobInstancesByContextInstanceId("contextInstance3", 1000, 0, null, null);

        Assert.assertEquals(167, searchResults.getResultList().size());
        Assert.assertEquals(167, searchResults.getTotalNumberOfResults());
    }

    private SchedulerJobInstanceRecord createSchedulerJobRecord(String contextInstanceId, String contextName, String jobName) {
        SolrSchedulerJobInstanceImpl solrSchedulerJobInstance = new SolrSchedulerJobInstanceImpl();
        solrSchedulerJobInstance.setJobName(jobName);
        solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());


        SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
        schedulerJobInstanceRecord.setJobName(jobName);
        schedulerJobInstanceRecord.setContextInstanceId(contextInstanceId);
        schedulerJobInstanceRecord.setContextName(contextName);
        schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
        schedulerJobInstanceRecord.setTimestamp(1000000L);
        schedulerJobInstanceRecord.setStatus("RUNNING");

        return schedulerJobInstanceRecord;
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

//    @Test
//    public void test_status_update_success() {
//
//        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus("WAITING");
//        service.save(scheduledContextRecord);
//
//        ScheduledContextInstanceRecord found = service.findById(contextInstance.getId() + "_scheduledContextInstance");
//
//        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
//        Assert.assertEquals("contextName", found.getContextName());
//        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
//        Assert.assertEquals("WAITING", found.getStatus());
//        Assert.assertEquals(1000000L, found.getTimestamp());
//
//        found.setStatus("RUNNING");
//
//        service.save(found);
//
//        found = service.findById(contextInstance.getId() + "_scheduledContextInstance");
//
//        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
//        Assert.assertEquals("contextName", found.getContextName());
//        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
//        Assert.assertEquals("RUNNING", found.getStatus());
//        Assert.assertEquals(1000000L, found.getTimestamp());
//    }
//
//    @Test
//    public void test_find_by_status_success() {
//
//        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
//        service.save(scheduledContextRecord);
//
//        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ERROR)).getResultList().size());
//        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RELEASED)).getResultList().size());
//        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.COMPLETE)).getResultList().size());
//        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ON_HOLD)).getResultList().size());
//        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RUNNING)).getResultList().size());
//        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING)).getResultList().size());
//
//        Assert.assertEquals(8, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
//            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED)).getResultList().size());
//    }
//
//    @Test
//    public void test_find_by_status_success_limit_offset() {
//
//        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000000L);
//        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
//        service.save(scheduledContextRecord);
//
//        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
//            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED), 2, 0).getResultList().size());
//
//        Assert.assertEquals(8, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
//            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED), -1, -1).getResultList().size());
//
//        Assert.assertEquals(0, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
//            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED), 10, 10).getResultList().size());
//    }
//
//    @Test
//    public void test_find_by_content_name_limit_offset_sort() {
//
//        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000001L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000002L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000003L);
//        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000004L);
//        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000005L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000006L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000007L);
//        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000008L);
//        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
//        service.save(scheduledContextRecord);
//
//        Assert.assertEquals(2, service.getScheduledContextInstancesByContextName("contextName1", 2, 0, null, null).getResultList().size());
//        Assert.assertEquals(4, service.getScheduledContextInstancesByContextName("contextName1", -1, -1, null, null).getResultList().size());
//
//        SearchResults<ScheduledContextInstanceRecord> searchResults = service.getScheduledContextInstancesByContextName
//            ("contextName1", -1, -1, SolrDaoBase.CREATED_DATE_TIME, "desc");
//
//        Assert.assertEquals(4, searchResults.getResultList().size());
//        Assert.assertEquals(1000004L, searchResults.getResultList().get(0).getTimestamp());
//
//        searchResults = service.getScheduledContextInstancesByContextName
//            ("contextName1", -1, -1, SolrDaoBase.CREATED_DATE_TIME, "asc");
//
//        Assert.assertEquals(4, searchResults.getResultList().size());
//        Assert.assertEquals(1000001L, searchResults.getResultList().get(0).getTimestamp());
//    }
//
//    @Test
//        public void test_find_by_content_name_limit_offset_sort_timestamp() {
//
//        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000001L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000002L);
//        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000003L);
//        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName1");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000004L);
//        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000005L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000006L);
//        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000007L);
//        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
//        service.save(scheduledContextRecord);
//
//        contextInstance = new SolrContextInstanceImpl();
//        contextInstance.setName("contextInstance");
//        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        scheduledContextRecord.setContextName("contextName2");
//        scheduledContextRecord.setContextInstance(contextInstance);
//        scheduledContextRecord.setTimestamp(1000008L);
//        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
//        service.save(scheduledContextRecord);
//
//        Assert.assertEquals(2, service.getScheduledContextInstancesByContextName("contextName1", 0, 1200001L,2, 0, null, null).getResultList().size());
//        Assert.assertEquals(4, service.getScheduledContextInstancesByContextName("contextName1", 0, 1200001L,-1, -1, null, null).getResultList().size());
//
//        SearchResults<ScheduledContextInstanceRecord> searchResults = service.getScheduledContextInstancesByContextName
//            ("contextName1", 0, 1200001L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "DESCENDING");
//
//        Assert.assertEquals(4, searchResults.getResultList().size());
//        Assert.assertEquals(1000004L, searchResults.getResultList().get(0).getTimestamp());
//
//        searchResults = service.getScheduledContextInstancesByContextName
//            ("contextName1",0, 1200001L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "ASCENDING");
//
//        Assert.assertEquals(4, searchResults.getResultList().size());
//        Assert.assertEquals(1000001L, searchResults.getResultList().get(0).getTimestamp());
//
//        searchResults = service.getScheduledContextInstancesByContextName
//            ("contextName1",1000001L, 1000003L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "ASCENDING");
//
//        Assert.assertEquals(3, searchResults.getResultList().size());
//        Assert.assertEquals(1000001L, searchResults.getResultList().get(0).getTimestamp());
//
//        searchResults = service.getScheduledContextInstancesByContextName
//            ("contextName1",1000001L, 1000003L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "DESCENDING");
//
//        Assert.assertEquals(3, searchResults.getResultList().size());
//        Assert.assertEquals(1000003L, searchResults.getResultList().get(0).getTimestamp());
//    }
//
//    @Test
//    @Ignore
//    public void test_save() throws IOException {
//        SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
//        scheduledContextInstanceDao.setSolrUsername("ikasan");
//        scheduledContextInstanceDao.setSolrPassword("1ka5an");
//        scheduledContextInstanceDao.initStandalone("http://localhost:8983/solr", 365);
//
//        SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
//        scheduledContextInstanceAuditDao.setSolrUsername("ikasan");
//        scheduledContextInstanceAuditDao.setSolrPassword("1ka5an");
//        scheduledContextInstanceAuditDao.initStandalone("http://localhost:8983/solr", 365);
//
//        IntStream.range(0, 1000).forEach(i -> {
//
//        SolrScheduledContextInstanceServiceImpl service = new SolrScheduledContextInstanceServiceImpl(scheduledContextInstanceDao
//            , scheduledContextInstanceAuditDao, true);
//
//            String data = null;
//            try {
//                data = loadDataFile("/data/contexts/CONTEXT-369160711-with-or-logic.json");
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            ContextService contextService = new ContextService();
//            ContextInstance contextInstance = null;
//            try {
//                contextInstance = contextService.getContextInstance(data);
//            }
//            catch (JsonProcessingException e) {
//                e.printStackTrace();
//            }
//            SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//            scheduledContextRecord.setContextName("CONTEXT-369160711");
//            scheduledContextRecord.setContextInstance(contextInstance);
//            scheduledContextRecord.setTimestamp(System.currentTimeMillis() - (i * 5000));
//            scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
//
//            service.save(scheduledContextRecord);
//        });
//    }
//
//    private ScheduledContextInstanceAuditRecord createAuditRecord() {
//        ScheduledContextInstanceAudit audit = new SolrScheduledContextInstanceAuditImpl();
//
//        ContextualisedScheduledProcessEvent<String, DryRunParameters> processEventInstance = createProcessEvent();
//        audit.setProcessEvent(processEventInstance);
//
//        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> jobInitiationEvent1 = createJobInitiationEvent();
//        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> jobInitiationEvent2 = createJobInitiationEvent();
//        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> jobInitiationEvent3 = createJobInitiationEvent();
//        List<SchedulerJobInitiationEvent> jobInitiationEvents = List.of(jobInitiationEvent1, jobInitiationEvent2, jobInitiationEvent3);
//        audit.setSchedulerJobInitiationEvents(jobInitiationEvents);
//
//        SolrContextInstanceImpl previousContextInstance = new SolrContextInstanceImpl();
//        previousContextInstance.setName("contextInstance");
//        SolrScheduledContextInstanceRecordImpl previousScheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        previousScheduledContextRecord.setContextName("contextName");
//        previousScheduledContextRecord.setContextInstance(previousContextInstance);
//        previousScheduledContextRecord.setTimestamp(1000000L);
//        previousScheduledContextRecord.setStatus("RUNNING");
//
//        audit.setPreviousContextInstance(previousContextInstance);
//
//        SolrContextInstanceImpl updatedContextInstance = new SolrContextInstanceImpl();
//        updatedContextInstance.setId(previousContextInstance.getId());
//        updatedContextInstance.setName("contextInstance");
//        SolrScheduledContextInstanceRecordImpl updatedScheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
//        updatedScheduledContextRecord.setContextName("contextName");
//        updatedScheduledContextRecord.setContextInstance(updatedContextInstance);
//        updatedScheduledContextRecord.setTimestamp(1000000L);
//        updatedScheduledContextRecord.setStatus("COMPLETE");
//
//        audit.setUpdatedContextInstance(updatedContextInstance);
//
//        ScheduledContextInstanceAuditRecord record = new SolrScheduledContextInstanceAuditRecordImpl();
//        record.setContextName("contextName");
//        record.setScheduledContextInstanceAudit(audit);
//        return record;
//    }
//
//    private ContextualisedScheduledProcessEvent<String, DryRunParameters> createProcessEvent() {
//        ContextualisedScheduledProcessEvent event = new SolrContextualisedScheduledProcessEventImpl();
//        event.setJobName("Job " + RandomStringUtils.randomAlphabetic(5));
//        event.setAgentName("Agent " + RandomStringUtils.randomAlphabetic(5));
//        event.setJobGroup("JobGroup " + RandomStringUtils.randomAlphabetic(5));
//        event.setJobDescription("JobDescription " + RandomStringUtils.randomAlphabetic(5));
//        event.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
//        event.setReturnCode(0);
//        event.setSuccessful(true);
//        event.setOutcome("Outcome: " + RandomStringUtils.randomAlphabetic(5));
//        event.setResultOutput("resultOutput " + RandomStringUtils.randomAlphabetic(5));
//        event.setResultError("resultError " + RandomStringUtils.randomAlphabetic(5));
//        event.setPid(78321);
//        event.setUser("User " + RandomStringUtils.randomAlphabetic(5));
//        event.setFireTime(System.currentTimeMillis());
//        event.setNextFireTime(System.currentTimeMillis() + 1000);
//        event.setCompletionTime(System.currentTimeMillis() + 2000);
//        event.setDryRun(true);
//        event.setContextId("contextId " + RandomStringUtils.randomAlphabetic(5));
//        event.setChildContextIds(List.of("childContextId1", "childContextId2"));
//        event.setContextInstanceId("contextInstanceId " + RandomStringUtils.randomAlphabetic(5));
//        event.setJobStarting(true);
//
//        SolrDryRunParameters dryRunParams = new SolrDryRunParameters();
//        event.setDryRunParameters(dryRunParams);
//
//        event.setSkipped(false);
//
//        SolrInternalEventDrivenJobImpl internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
//        internalEventDrivenJob.setSuccessfulReturnCodes(List.of("Code1", "Code2"));
//        internalEventDrivenJob.setWorkingDirectory("workingDirectory/" + RandomStringUtils.randomAlphabetic(5));
//        internalEventDrivenJob.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
//        internalEventDrivenJob.setMinExecutionTime(System.currentTimeMillis());
//        internalEventDrivenJob.setMaxExecutionTime(System.currentTimeMillis() + 3000);
//
//        SolrContextParameterImpl contextParameter = new SolrContextParameterImpl();
//        contextParameter.setType("type " + RandomStringUtils.randomAlphabetic(5));
//        contextParameter.setName("name " + RandomStringUtils.randomAlphabetic(5));
//        internalEventDrivenJob.setContextParameters(List.of(contextParameter));
//        internalEventDrivenJob.setDaysOfWeekToRun(List.of(1, 2, 3, 4, 5));
//
//        event.setInternalEventDrivenJob(internalEventDrivenJob);
//
//        return event;
//    }
//
//    private SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJob, DryRunParameters> createJobInitiationEvent() {
//        SchedulerJobInitiationEvent event = new SolrSchedulerJobInitiationEventImpl();
//        event.setAgentName("Agent " + RandomStringUtils.randomAlphabetic(5));
//        event.setAgentUrl("AgentUrl " + RandomStringUtils.randomAlphabetic(5));
//        event.setJobName("Job " + RandomStringUtils.randomAlphabetic(5));
//
//        SolrInternalEventDrivenJobImpl internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
//        internalEventDrivenJob.setSuccessfulReturnCodes(List.of("EventCode1", "EventCode2"));
//        internalEventDrivenJob.setWorkingDirectory("workingDirectory/" + RandomStringUtils.randomAlphabetic(5));
//        internalEventDrivenJob.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
//        internalEventDrivenJob.setMinExecutionTime(System.currentTimeMillis());
//        internalEventDrivenJob.setMaxExecutionTime(System.currentTimeMillis() + 3000);
//
//        SolrContextParameterImpl contextParameter = new SolrContextParameterImpl();
//        contextParameter.setType("type " + RandomStringUtils.randomAlphabetic(5));
//        contextParameter.setName("name " + RandomStringUtils.randomAlphabetic(5));
//        internalEventDrivenJob.setContextParameters(List.of(contextParameter));
//        internalEventDrivenJob.setDaysOfWeekToRun(List.of(1, 2, 3, 4, 5));
//        event.setInternalEventDrivenJob(internalEventDrivenJob);
//
//        event.setContextId("contextId " + RandomStringUtils.randomAlphabetic(5));
//        event.setChildContextIds(List.of("EventChildContextId1", "EventChildContextId2"));
//        event.setContextInstanceId("contextInstanceId " + RandomStringUtils.randomAlphabetic(5));
//
//        SolrContextParameterInstanceImpl contextParameter1 = new SolrContextParameterInstanceImpl();
//        contextParameter1.setType("type1 " + RandomStringUtils.randomAlphabetic(5));
//        contextParameter1.setName("name1 " + RandomStringUtils.randomAlphabetic(5));
//        SolrContextParameterInstanceImpl contextParameter2 = new SolrContextParameterInstanceImpl();
//        contextParameter2.setType("type1 " + RandomStringUtils.randomAlphabetic(5));
//        contextParameter2.setName("name1 " + RandomStringUtils.randomAlphabetic(5));
//        event.setContextParameters(List.of(contextParameter1, contextParameter2));
//
//        event.setDryRun(true);
//        SolrDryRunParameters dryRunParams = new SolrDryRunParameters();
//        event.setDryRunParameters(dryRunParams);
//
//        event.setSkipped(false);
//
//        return event;
//    }

    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}