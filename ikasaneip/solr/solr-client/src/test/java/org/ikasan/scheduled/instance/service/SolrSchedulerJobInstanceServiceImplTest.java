package org.ikasan.scheduled.instance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.context.model.SolrContextParameterImpl;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrDryRunParameters;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolrSchedulerJobInstanceServiceImplTest extends SolrTestCaseJ4 {

    private SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao;
    private SolrSchedulerJobDaoImpl solrSchedulerJobDao;
    private SchedulerJobInstanceService service;

    private SolrFileEventDrivenJobDaoImpl solrFileEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobDaoImpl solrQuartzScheduleDrivenJobRecordDao;
    private SolrInternalEventDrivenJobDaoImpl solrInternalEventDrivenJobRecordDao;

    private Path tmpPath;
    private EmbeddedSolrServer server;

    @Before
    public void setup() throws SolrServerException, IOException {
        this.tmpPath = createTempDir();
        NodeConfig config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        this.server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        this.server.request(createRequest);

        this.solrSchedulerJobInstanceDao = new SolrSchedulerJobInstanceDaoImpl();
        this.solrSchedulerJobInstanceDao.setSolrClient(server);

        this.solrSchedulerJobDao = new SolrSchedulerJobDaoImpl();
        this.solrSchedulerJobDao.setSolrClient(server);

        this.solrFileEventDrivenJobRecordDao = new SolrFileEventDrivenJobDaoImpl();
        this.solrFileEventDrivenJobRecordDao.setSolrClient(server);

        this.solrQuartzScheduleDrivenJobRecordDao = new SolrQuartzScheduleDrivenJobDaoImpl();
        this.solrQuartzScheduleDrivenJobRecordDao.setSolrClient(server);

        this.solrInternalEventDrivenJobRecordDao = new SolrInternalEventDrivenJobDaoImpl();
        this.solrInternalEventDrivenJobRecordDao.setSolrClient(server);

        this.service = new SolrSchedulerJobInstanceServiceImpl(this.solrSchedulerJobInstanceDao, this.solrSchedulerJobDao);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfSchedulerJobInstanceDaoIsNull() {
        service = new SolrSchedulerJobInstanceServiceImpl(null, this.solrSchedulerJobDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfSchedulerJobDaoIsNull() {
        service = new SolrSchedulerJobInstanceServiceImpl(this.solrSchedulerJobInstanceDao, null);
    }

    @Test
    public void test_save_and_find_success() {
        QuartzScheduleDrivenJobInstance solrSchedulerJobInstance = new SolrQuartzScheduleDrivenJobInstanceImpl();
        solrSchedulerJobInstance.setJobName("jobName");
        solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());


        SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
        schedulerJobInstanceRecord.setJobName("jobName");
        schedulerJobInstanceRecord.setContextInstanceId("contexInstance");
        schedulerJobInstanceRecord.setContextName("contextName");
        schedulerJobInstanceRecord.setChildContextName("contextContextName");
        schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
        schedulerJobInstanceRecord.setTimestamp(1000000L);
        schedulerJobInstanceRecord.setStatus("RUNNING");
        service.save(schedulerJobInstanceRecord);

        SchedulerJobInstanceRecord found = service.findById("jobName_contexInstance_contextContextName_quartzScheduleDrivenJobInstance");

        Assert.assertEquals(schedulerJobInstanceRecord.getJobName() + "_"
            + schedulerJobInstanceRecord.getContextInstanceId() + "_"
            + schedulerJobInstanceRecord.getChildContextName() + "_"
            + JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE, found.getId());

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
        filter.setContextInstanceId("contextInstance1");
        filter.setJobName("job370");

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(1, searchResults.getResultList().size());
        Assert.assertEquals(1, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_filter_with_escape_characters() {
        IntStream.range(0, 371).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance:1",
                "context:1", "job:"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance:2",
                "context:1", "job:"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance:3",
                "context:2", "context2Job:"+i));
        });

        SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context:1");

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(646, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 0, 0, null, null);

        Assert.assertEquals(0, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context:1");
        filter.setContextInstanceId("contextInstance:1");

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(371, searchResults.getResultList().size());
        Assert.assertEquals(371, searchResults.getTotalNumberOfResults());

        filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context:1");
        filter.setContextInstanceId("contextInstance:2");

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(275, searchResults.getResultList().size());
        Assert.assertEquals(275, searchResults.getTotalNumberOfResults());

        filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context:1");
        filter.setContextInstanceId("contextInstance:1");
        filter.setJobName("job:370");

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
                "context1", "job1"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            this.solrSchedulerJobInstanceDao.save(this.createSchedulerJobRecord("contextInstance2",
                "context1", "job2"+i));
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

    @Test
    public void test_initialise_scheduler_job_instances() throws SchedulerJobInstanceInitialisationException {
        this.insertFileEventRecords("file", 135, "context");
        this.insertQuartzScheduleEventRecords("quartz", 75, "context");
        this.insertInternalEventDrivenRecords("internal", 400, "context");

        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setName("context");
        contextInstance.setId("contextInstanceId");
        contextInstance.setScheduledJobs(this.solrSchedulerJobDao.findByContext("context", 1000, 0).getResultList()
            .stream()
            .map(schedulerJobRecord -> {
                try {
                    if (schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrFileEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrInternalEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrQuartzScheduleDrivenJobInstanceImpl.class);
                    }
                }
                catch (Exception e) {
                    return null;
                }
                return null;
            })
            .collect(Collectors.toList())
        );

        List<SchedulerJobInstance> schedulerJobInstances = this.service.initialiseSchedulerJobInstancesForContext
            (contextInstance);

        Assert.assertEquals(610, schedulerJobInstances.size());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service.getSchedulerJobInstancesByContextInstanceId
            ("contextInstanceId", 610, 0, null, null);

        Assert.assertEquals(610, searchResults.getResultList().size());
    }

    private SchedulerJobInstanceRecord createSchedulerJobRecord(String contextInstanceId, String contextName, String jobName) {
        SolrQuartzScheduleDrivenJobInstanceImpl solrSchedulerJobInstance = new SolrQuartzScheduleDrivenJobInstanceImpl();
        solrSchedulerJobInstance.setJobName(jobName);
        solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());
        solrSchedulerJobInstance.setChildContextName("childContextName");


        SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
        schedulerJobInstanceRecord.setJobName(jobName);
        schedulerJobInstanceRecord.setContextInstanceId(contextInstanceId);
        schedulerJobInstanceRecord.setContextName(contextName);
        schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
        schedulerJobInstanceRecord.setTimestamp(1000000L);
        schedulerJobInstanceRecord.setStatus("RUNNING");
        schedulerJobInstanceRecord.setChildContextName("childContextName");

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

    private void insertFileEventRecords(String idPrefix, int num, String contextId) {
        IntStream.range(0, num).forEach(i -> {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrFileEventDrivenJob.setJobName(idPrefix+"jobName"+i);
            solrFileEventDrivenJob.setIdentifier(solrFileEventDrivenJob.getAgentName()+"_"+solrFileEventDrivenJob.getJobName());
            solrFileEventDrivenJob.setContextId(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression");
            solrFileEventDrivenJob.setFilePath("filePath");

            SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
            solrFileEventDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrFileEventDrivenJobRecord.setJobName("jobName"+i);
            solrFileEventDrivenJobRecord.setContextId(contextId);
            solrFileEventDrivenJobRecord.setTimestamp(1000000L);
            solrFileEventDrivenJobRecord.setFileEventDrivenJob(solrFileEventDrivenJob);

            this.solrFileEventDrivenJobRecordDao.save(solrFileEventDrivenJobRecord);
        });
    }

    private void insertQuartzScheduleEventRecords(String idPrefix, int num, String contextId) {
        IntStream.range(0, num).forEach(i -> {
            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrQuartzScheduleDrivenJob.setJobName(idPrefix+"jobName"+i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName()+"_"+solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextId(contextId);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression");

            SolrQuartzScheduleDrivenJobRecordImpl solrQuartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
            solrQuartzScheduleDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrQuartzScheduleDrivenJobRecord.setJobName("jobName"+i);
            solrQuartzScheduleDrivenJobRecord.setContextId(contextId);
            solrQuartzScheduleDrivenJobRecord.setTimestamp(1000000L);
            solrQuartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(solrQuartzScheduleDrivenJob);


            this.solrQuartzScheduleDrivenJobRecordDao.save(solrQuartzScheduleDrivenJobRecord);
        });
    }

    private void insertInternalEventDrivenRecords(String idPrefix, int num, String contextId) {
        IntStream.range(0, num).forEach(i -> {
            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrInternalEventDrivenJob.setJobName(idPrefix+"jobName"+i);
            solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName()+"_"+solrInternalEventDrivenJob.getJobName());
            solrInternalEventDrivenJob.setContextId(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -la");

            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrInternalEventDrivenJobRecord.setJobName("jobName"+i);
            solrInternalEventDrivenJobRecord.setContextId(contextId);
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);


            this.solrInternalEventDrivenJobRecordDao.save(solrInternalEventDrivenJobRecord);
        });
    }

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