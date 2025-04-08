package org.ikasan.scheduled.instance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.context.model.SolrContextParameterImpl;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrDryRunParameters;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditAggregateDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.dao.*;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolrSchedulerJobInstanceServiceImplTest extends SolrTestCaseJ4 {

    private SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao;
    private SolrSchedulerJobDaoImpl solrSchedulerJobDao;
    private SchedulerJobInstanceService service;
    private SolrFileEventDrivenJobDaoImpl solrFileEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobDaoImpl solrQuartzScheduleDrivenJobRecordDao;
    private SolrInternalEventDrivenJobDaoImpl solrInternalEventDrivenJobRecordDao;
    private SolrGlobalEventJobDaoImpl solrGlobalEventJobRecordDao;
    private SolrContextStartJobDaoImpl solrContextStartJobDao;
    private SolrContextTerminalJobDaoImpl solrContextTerminalJobDao;
    private SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao;
    private SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao;
    private SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao;
    private ScheduledContextInstanceService scheduledContextInstanceService;

    ContextService contextService = new ContextService();

    private Path tmpPath;
    private EmbeddedSolrServer server;

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

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
        
        this.solrGlobalEventJobRecordDao = new SolrGlobalEventJobDaoImpl();
        this.solrGlobalEventJobRecordDao.setSolrClient(server);

        this.solrContextTerminalJobDao = new SolrContextTerminalJobDaoImpl();
        this.solrContextTerminalJobDao.setSolrClient(server);

        this.solrContextStartJobDao = new SolrContextStartJobDaoImpl();
        this.solrContextStartJobDao.setSolrClient(server);

        this.scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
        this.scheduledContextInstanceDao.setSolrClient(this.server);

        this.scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        this.scheduledContextInstanceAuditAggregateDao.setSolrClient(this.server);

        this.scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
        this.scheduledContextInstanceAuditDao.setSolrClient(this.server);

        this.scheduledContextInstanceService = new SolrScheduledContextInstanceServiceImpl(this.scheduledContextInstanceDao, this.scheduledContextInstanceAuditDao
            , this.scheduledContextInstanceAuditAggregateDao, true, true);

        this.service = new SolrSchedulerJobInstanceServiceImpl(this.solrSchedulerJobInstanceDao
            , this.scheduledContextInstanceAuditAggregateDao, this.solrSchedulerJobDao, this.scheduledContextInstanceService
            , SolrSchedulerJobInstanceServiceImplTest.getSchedulerJobExecutionEnvironmentLabel()
            , true);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfSchedulerJobInstanceDaoIsNull() {
        service = new SolrSchedulerJobInstanceServiceImpl(null, this.scheduledContextInstanceAuditAggregateDao, this.solrSchedulerJobDao
            , this.scheduledContextInstanceService, SolrSchedulerJobInstanceServiceImplTest.getSchedulerJobExecutionEnvironmentLabel(), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfScheduledContextInstanceAuditAggregateDaoIsNull() {
        service = new SolrSchedulerJobInstanceServiceImpl(this.solrSchedulerJobInstanceDao, null, this.solrSchedulerJobDao
            , this.scheduledContextInstanceService, SolrSchedulerJobInstanceServiceImplTest.getSchedulerJobExecutionEnvironmentLabel(), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfSchedulerJobDaoIsNull() {
        service = new SolrSchedulerJobInstanceServiceImpl(this.solrSchedulerJobInstanceDao, this.scheduledContextInstanceAuditAggregateDao, null
            , this.scheduledContextInstanceService, SolrSchedulerJobInstanceServiceImplTest.getSchedulerJobExecutionEnvironmentLabel(), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfScheduledContextInstanceIsNull() {
        service = new SolrSchedulerJobInstanceServiceImpl(this.solrSchedulerJobInstanceDao, this.scheduledContextInstanceAuditAggregateDao, this.solrSchedulerJobDao
            , null, SolrSchedulerJobInstanceServiceImplTest.getSchedulerJobExecutionEnvironmentLabel(), true);
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
        schedulerJobInstanceRecord.setManuallySubmittedBy("manualUser");
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
            .getContextName().startsWith("contextId"));
        Assert.assertTrue(((ContextualisedScheduledProcessEvent)found.getSchedulerJobInstance().getScheduledProcessEvent())
            .getChildContextNames().contains("childContextId1"));
        Assert.assertTrue(((ContextualisedScheduledProcessEvent)found.getSchedulerJobInstance().getScheduledProcessEvent())
            .getChildContextNames().contains("childContextId2"));
        Assert.assertEquals(78321, found.getSchedulerJobInstance().getScheduledProcessEvent().getPid());
        Assert.assertEquals("RUNNING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());
        Assert.assertEquals("manualUser", found.getManuallySubmittedBy());
        Assert.assertEquals(1669397423771L, found.getStartTime());
        Assert.assertEquals(1669397429771L, found.getEndTime());

        Assert.assertNull(service.findById("bad_id"));
    }

    @Test
    public void test_find_by_filter() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job"+i, "display"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context1", "job"+i, "display"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context2", "context2Job"+i, "display2"+i));
        });

        this.service.save(schedulerJobInstanceRecords);

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

        searchResults = this.service
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(1, searchResults.getResultList().size());
        Assert.assertEquals(1, searchResults.getTotalNumberOfResults());

        filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context1");
        filter.setContextInstanceId("contextInstance1");
        filter.setDisplayNameFilter("display370");

        searchResults = this.service
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(1, searchResults.getResultList().size());
        Assert.assertEquals(1, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_filter_with_without_start_and_terminal_jobs() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job"+i, "display"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context1", "job"+i, "display"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context2", "context2Job"+i, "display2"+i));
        });

        IntStream.range(0, 475).forEach(i -> {
            schedulerJobInstanceRecords.addAll(this.createContextStartJobInstanceRecord("contextInstance1",
                "context1", "startJob-1" + i, List.of("child1", "child2")));
            schedulerJobInstanceRecords.addAll(this.createContextTerminalJobInstanceRecord("contextInstance1",
                "context1", "terminal-1" + i, List.of("child1", "child2")));
        });

        this.service.save(schedulerJobInstanceRecords);
        SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setIncludeStartAndTerminalJobsInSearchResults(false);
        filter.setContextName("context1");

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(646, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 0, 0, null, null);

        Assert.assertEquals(0, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        filter.setIncludeStartAndTerminalJobsInSearchResults(true);
        filter.setContextName("context1");

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 5000, 0, null, null);

        Assert.assertEquals(2546, searchResults.getResultList().size());
        Assert.assertEquals(2546, searchResults.getTotalNumberOfResults());

        searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 0, 0, null, null);

        Assert.assertEquals(0, searchResults.getResultList().size());
        Assert.assertEquals(2546, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_filter_with_escape_characters() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance:1",
                "context:1", "job:"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance:2",
                "context:1", "job:"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance:3",
                "context:2", "context2Job:"+i));
        });

        this.service.save(schedulerJobInstanceRecords);

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

        searchResults = this.service
            .getScheduledContextInstancesByFilter(filter, 1000, 0, null, null);

        Assert.assertEquals(1, searchResults.getResultList().size());
        Assert.assertEquals(1, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_context_name() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context2", "context2Job"+i));
        });

        this.service.save(schedulerJobInstanceRecords);

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getSchedulerJobInstancesByContextName("context1", 1000, 0, null, null);

        Assert.assertEquals(646, searchResults.getResultList().size());
        Assert.assertEquals(646, searchResults.getTotalNumberOfResults());

        searchResults = this.service
            .getSchedulerJobInstancesByContextName("context2", 1000, 0, null, null);

        Assert.assertEquals(275, searchResults.getResultList().size());
        Assert.assertEquals(275, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_context_id_job_name_and_child_context_name() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context2", "context2Job"+i));
        });

        this.service.save(schedulerJobInstanceRecords);


        SchedulerJobInstanceRecord searchResult = this.service
            .findByContextIdJobNameChildContextName("contextInstance1", "job69", "childContextName");

        Assert.assertNotNull(searchResult);
    }


    @Test
    public void test_find_by_context_instance_id() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context1", "job"+i));
        });

        IntStream.range(0, 167).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context2", "context2Job"+i));
        });

        this.service.save(schedulerJobInstanceRecords);


        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service
            .getSchedulerJobInstancesByContextInstanceId("contextInstance1", 1000, 0, null, null);

        Assert.assertEquals(371, searchResults.getResultList().size());
        Assert.assertEquals(371, searchResults.getTotalNumberOfResults());

        searchResults = this.service
            .getSchedulerJobInstancesByContextInstanceId("contextInstance2", 1000, 0, null, null);

        Assert.assertEquals(275, searchResults.getResultList().size());
        Assert.assertEquals(275, searchResults.getTotalNumberOfResults());

        searchResults = this.service
            .getSchedulerJobInstancesByContextInstanceId("contextInstance3", 1000, 0, null, null);

        Assert.assertEquals(167, searchResults.getResultList().size());
        Assert.assertEquals(167, searchResults.getTotalNumberOfResults());
    }

    @Test
    public void test_find_context_instance_status_aggregate() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job1"+i));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context2", "job2"+i));
        });

        IntStream.range(0, 167).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context3", "context2Job"+i));
        });

        this.service.save(schedulerJobInstanceRecords);


        List<ContextInstanceAggregateJobStatus> searchResults = this.service
            .getJobStatusCountForContextInstances(List.of("contextInstance1", "contextInstance2", "contextInstance3"));

        Assert.assertNotNull(searchResults);
        Assert.assertEquals(3, searchResults.size());
        Assert.assertEquals("contextInstance1", searchResults.get(0).getContextInstanceId());
        Assert.assertEquals("context1", searchResults.get(0).getContextInstanceName());
        Assert.assertEquals(371, searchResults.get(0).getStatusCount(InstanceStatus.RUNNING));
        Assert.assertEquals(0, searchResults.get(0).getStatusCount(InstanceStatus.ERROR));
        Assert.assertEquals(0, searchResults.get(0).getStatusCount(InstanceStatus.COMPLETE));
        Assert.assertEquals("contextInstance2", searchResults.get(1).getContextInstanceId());
        Assert.assertEquals("context2", searchResults.get(1).getContextInstanceName());
        Assert.assertEquals(275, searchResults.get(1).getStatusCount(InstanceStatus.RUNNING));
        Assert.assertEquals(0, searchResults.get(1).getStatusCount(InstanceStatus.ERROR));
        Assert.assertEquals(0, searchResults.get(1).getStatusCount(InstanceStatus.COMPLETE));
        Assert.assertEquals("contextInstance3", searchResults.get(2).getContextInstanceId());
        Assert.assertEquals("context3", searchResults.get(2).getContextInstanceName());
        Assert.assertEquals(167, searchResults.get(2).getStatusCount(InstanceStatus.RUNNING));
        Assert.assertEquals(0, searchResults.get(2).getStatusCount(InstanceStatus.ERROR));
        Assert.assertEquals(0, searchResults.get(2).getStatusCount(InstanceStatus.COMPLETE));
    }

    @Test
    public void test_find_context_instance_status_aggregate_consider_job_in_multiple_contexts() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job"+i));
            schedulerJobInstanceRecords.addAll(this.createCommandExecutionJobInstanceRecord("contextInstance1", "context1", "jobNonTar"+i,
                List.of("child1", "child2"), false, InstanceStatus.WAITING));
            schedulerJobInstanceRecords.addAll(this.createCommandExecutionJobInstanceRecord("contextInstance1", "context1", "jobTar"+i,
                List.of("child3", "child4"), true, InstanceStatus.COMPLETE));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context2", "job"+i));
        });

        IntStream.range(0, 167).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context3", "context3Job"+i));
        });

        this.service.save(schedulerJobInstanceRecords);

        List<ContextInstanceAggregateJobStatus> searchResults = this.service
            .getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(List.of("contextInstance1", "contextInstance2", "contextInstance3"));

        Assert.assertNotNull(searchResults);
        Assert.assertEquals(3, searchResults.size());
        Assert.assertEquals("contextInstance1", searchResults.get(0).getContextInstanceId());
        Assert.assertEquals("context1", searchResults.get(0).getContextInstanceName());
        Assert.assertEquals(371, searchResults.get(0).getStatusCount(InstanceStatus.RUNNING));
        Assert.assertEquals(371, searchResults.get(0).getStatusCount(InstanceStatus.WAITING));
        Assert.assertEquals(742, searchResults.get(0).getStatusCount(InstanceStatus.COMPLETE));
        Assert.assertEquals("contextInstance2", searchResults.get(1).getContextInstanceId());
        Assert.assertEquals("context2", searchResults.get(1).getContextInstanceName());
        Assert.assertEquals(275, searchResults.get(1).getStatusCount(InstanceStatus.RUNNING));
        Assert.assertEquals(0, searchResults.get(1).getStatusCount(InstanceStatus.ERROR));
        Assert.assertEquals(0, searchResults.get(1).getStatusCount(InstanceStatus.COMPLETE));
        Assert.assertEquals("contextInstance3", searchResults.get(2).getContextInstanceId());
        Assert.assertEquals("context3", searchResults.get(2).getContextInstanceName());
        Assert.assertEquals(167, searchResults.get(2).getStatusCount(InstanceStatus.RUNNING));
        Assert.assertEquals(0, searchResults.get(2).getStatusCount(InstanceStatus.ERROR));
        Assert.assertEquals(0, searchResults.get(2).getStatusCount(InstanceStatus.COMPLETE));
    }

    @Test
    public void test_initialise_scheduler_job_instances() throws SchedulerJobInstanceInitialisationException {
        this.insertFileEventRecords("file", 135, "context");
        this.insertQuartzScheduleEventRecords("quartz", 75, "context");
        this.insertInternalEventDrivenRecords("internal", 400, "context", null);
        this.insertGlobalEventRecords("global", 10, "context", false);

        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setName("context");
        contextInstance.setId("contextInstanceId");
        contextInstance.setScheduledJobs(this.solrSchedulerJobDao.findByContext("context", 1000, 0).getResultList()
            .stream()
            .map(schedulerJobRecord -> {
                try {
                    if (schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrFileEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrInternalEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrQuartzScheduleDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrGlobalEventJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrGlobalEventJobInstanceImpl.class);
                    }
                }
                catch (Exception e) {
                    return null;
                }
                return null;
            })
            .collect(Collectors.toList())
        );

        SchedulerJobInstancesInitialisationParameters parameters = new SolrSchedulerJobInstancesInitialisationParametersImpl(false);

        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("context");

        List<SchedulerJobInstance> schedulerJobInstances = this.service.initialiseSchedulerJobInstancesForContext
            (contextTemplate, contextInstance, parameters);

        Assert.assertEquals(620, schedulerJobInstances.size());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service.getSchedulerJobInstancesByContextInstanceId
            ("contextInstanceId", 1000, 0, null, null);

        Assert.assertEquals(620, searchResults.getResultList().size());
    }

    @Test
    public void test_initialise_scheduler_job_instances_with_automatically_created_start_and_terminal_jobs() throws SchedulerJobInstanceInitialisationException, IOException {
        List<InternalEventDrivenJobRecord> internalEventDrivenJobRecords = this.loadInternalEventDrivenJobRecords("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal",
            "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal");
        this.solrInternalEventDrivenJobRecordDao.save(internalEventDrivenJobRecords);

        List<QuartzScheduleDrivenJobRecord> quartzScheduleDrivenJobRecords = this.loadQuartzScheduleDrivenJobRecords("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/quartz",
            "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/quartz");
        this.solrQuartzScheduleDrivenJobRecordDao.save(quartzScheduleDrivenJobRecords);

        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate contextTemplate = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance = this.contextService.getContextInstance(contextJson);

        SchedulerJobInstancesInitialisationParameters parameters = new SolrSchedulerJobInstancesInitialisationParametersImpl(false);
        List<SchedulerJobInstance> schedulerJobInstances = this.service.initialiseSchedulerJobInstancesForContext
            (contextTemplate, contextInstance, parameters);

        Assert.assertEquals(65, schedulerJobInstances.size());
        Assert.assertEquals(11, schedulerJobInstances.stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .collect(Collectors.toList()).size());
        Assert.assertEquals(26, schedulerJobInstances.stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
            .collect(Collectors.toList()).size());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service.getSchedulerJobInstancesByContextInstanceId
            (contextInstance.getId(), 1000, 0, null, null);

        Assert.assertEquals(65, searchResults.getResultList().size());
        Assert.assertEquals(11, searchResults.getResultList().stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .collect(Collectors.toList()).size());
        Assert.assertEquals(26, searchResults.getResultList().stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
            .collect(Collectors.toList()).size());
        searchResults.getResultList().forEach(schedulerJobInstanceRecord -> {
            Assert.assertFalse(schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName().isEmpty());
        });
    }

    @Test
    public void test_initialise_scheduler_job_instances_with_automatically_created_start_bridging_and_terminal_jobs() throws SchedulerJobInstanceInitialisationException, IOException {
        List<InternalEventDrivenJobRecord> internalEventDrivenJobRecords = this.loadInternalEventDrivenJobRecords("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal",
            "/data/bundles/TEST_IK_GLOB_WITH_BRIDGING_START_AND_TERMINAL_JOBS/jobs/internal");
        this.solrInternalEventDrivenJobRecordDao.save(internalEventDrivenJobRecords);

        List<QuartzScheduleDrivenJobRecord> quartzScheduleDrivenJobRecords = this.loadQuartzScheduleDrivenJobRecords("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/quartz",
            "/data/bundles/TEST_IK_GLOB_WITH_BRIDGING_START_AND_TERMINAL_JOBS/jobs/quartz");
        this.solrQuartzScheduleDrivenJobRecordDao.save(quartzScheduleDrivenJobRecords);

        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_BRIDGING_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate contextTemplate = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance = this.contextService.getContextInstance(contextJson);

        SchedulerJobInstancesInitialisationParameters parameters = new SolrSchedulerJobInstancesInitialisationParametersImpl(false);
        List<SchedulerJobInstance> schedulerJobInstances = this.service.initialiseSchedulerJobInstancesForContext
            (contextTemplate, contextInstance, parameters);

        Assert.assertEquals(66, schedulerJobInstances.size());
        Assert.assertEquals(11, schedulerJobInstances.stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .collect(Collectors.toList()).size());
        Assert.assertEquals(26, schedulerJobInstances.stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
            .collect(Collectors.toList()).size());
        Assert.assertEquals(1, schedulerJobInstances.stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getAgentName().equals(JobConstants.BRIDGING_JOB))
            .collect(Collectors.toList()).size());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service.getSchedulerJobInstancesByContextInstanceId
            (contextInstance.getId(), 1000, 0, null, null);

        Assert.assertEquals(66, searchResults.getResultList().size());
        Assert.assertEquals(11, searchResults.getResultList().stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .collect(Collectors.toList()).size());
        Assert.assertEquals(26, searchResults.getResultList().stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
            .collect(Collectors.toList()).size());
        Assert.assertEquals(1, schedulerJobInstances.stream()
            .filter(schedulerJobInstance -> schedulerJobInstance.getAgentName().equals(JobConstants.BRIDGING_JOB))
            .collect(Collectors.toList()).size());
        searchResults.getResultList().forEach(schedulerJobInstanceRecord -> {
            Assert.assertFalse(schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName().isEmpty());
        });
    }

    @Test
    public void test_initialise_scheduler_job_instances_with_skipped_global_jobs() throws SchedulerJobInstanceInitialisationException {
        this.insertFileEventRecords("file", 135, "context");
        this.insertQuartzScheduleEventRecords("quartz", 75, "context");
        this.insertInternalEventDrivenRecords("internal", 400, "context", null);
        this.insertGlobalEventRecords("global", 10, "context", true);

        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setName("context");
        contextInstance.setId("contextInstanceId");
        contextInstance.setScheduledJobs(this.solrSchedulerJobDao.findByContext("context", 1000, 0).getResultList()
            .stream()
            .map(schedulerJobRecord -> {
                try {
                    if (schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrFileEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrInternalEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrQuartzScheduleDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrGlobalEventJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrGlobalEventJobInstanceImpl.class);
                    }
                }
                catch (Exception e) {
                    return null;
                }
                return null;
            })
            .collect(Collectors.toList())
        );

        SchedulerJobInstancesInitialisationParameters parameters = new SolrSchedulerJobInstancesInitialisationParametersImpl(false);
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("context");

        List<SchedulerJobInstance> schedulerJobInstances = this.service.initialiseSchedulerJobInstancesForContext
            (contextTemplate, contextInstance, parameters);

        Assert.assertEquals(620, schedulerJobInstances.size());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service.getSchedulerJobInstancesByContextInstanceId
            ("contextInstanceId", 620, 0, null, null);

        Assert.assertEquals(620, searchResults.getResultList().size());

        searchResults.getResultList().forEach(job -> {
            if(job.getSchedulerJobInstance() instanceof GlobalEventJobInstance) {
                Assert.assertEquals(InstanceStatus.SKIPPED, job.getSchedulerJobInstance().getStatus());
                Assert.assertEquals(InstanceStatus.SKIPPED.name(), job.getStatus());
            }
        });
    }

    @Test
    public void test_initialise_scheduler_job_instances_with_jobs_on_hold() throws SchedulerJobInstanceInitialisationException {
        this.insertFileEventRecords("file", 135, "context");
        this.insertQuartzScheduleEventRecords("quartz", 75, "context");
        this.insertInternalEventDrivenRecords("internal", 400, "context", null);
        this.insertGlobalEventRecords("global", 10, "context", false);

        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setName("context");
        contextInstance.setId("contextInstanceId");
        contextInstance.setScheduledJobs(this.solrSchedulerJobDao.findByContext("context", 1000, 0).getResultList()
            .stream()
            .map(schedulerJobRecord -> {
                try {
                    if (schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrFileEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrInternalEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrQuartzScheduleDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrGlobalEventJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                            , SolrGlobalEventJobInstanceImpl.class);
                    }
                }
                catch (Exception e) {
                    return null;
                }
                return null;
            })
            .collect(Collectors.toList())
        );

        SchedulerJobInstancesInitialisationParameters parameters = new SolrSchedulerJobInstancesInitialisationParametersImpl(true);
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("context");

        List<SchedulerJobInstance> schedulerJobInstances = this.service.initialiseSchedulerJobInstancesForContext
            (contextTemplate, contextInstance, parameters);

        Assert.assertEquals(620, schedulerJobInstances.size());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service.getSchedulerJobInstancesByContextInstanceId
            ("contextInstanceId", 620, 0, null, null);

        Assert.assertEquals(620, searchResults.getResultList().size());

        searchResults.getResultList().forEach(job -> {
            if(job.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                Assert.assertEquals(true, job.getSchedulerJobInstance().isHeld());
                Assert.assertEquals(InstanceStatus.ON_HOLD, job.getSchedulerJobInstance().getStatus());
                if(job.getSchedulerJobInstance().getChildContextNames() != null) {
                    job.getSchedulerJobInstance().getChildContextNames()
                        .forEach(name -> Assert.assertTrue(job.getSchedulerJobInstance().getHeldContexts().get(name)));
                }
            }
        });
    }

    @Test
    public void test_put_jobs_on_hold_for_context_instance_the_query_for_jobs_to_release() throws SchedulerJobInstanceInitialisationException {
        this.insertFileEventRecords("file", 135, "context");
        this.insertQuartzScheduleEventRecords("quartz", 75, "context");
        this.insertInternalEventDrivenRecords("internal", 400, "context",null);
        this.insertGlobalEventRecords("global", 10, "context", false);

        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setName("context");
        contextInstance.setId("contextInstanceId");
        contextInstance.setScheduledJobs(this.solrSchedulerJobDao.findByContext("context", 1000, 0).getResultList()
            .stream()
            .map(schedulerJobRecord -> {
                try {
                    SchedulerJobInstance schedulerJobInstance = null;
                    if (schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                        schedulerJobInstance = objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrFileEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                        schedulerJobInstance = objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrInternalEventDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                        schedulerJobInstance = objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrQuartzScheduleDrivenJobInstanceImpl.class);
                    } else if (schedulerJobRecord.getJob() instanceof SolrGlobalEventJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrGlobalEventJobInstanceImpl.class);
                    }
                    schedulerJobInstance.setChildContextName("context");
                    return schedulerJobInstance;
                }
                catch (Exception e) {
                    return null;
                }
            })
            .collect(Collectors.toList())
        );

        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("context");

        service.initialiseSchedulerJobInstancesForContext(contextTemplate, contextInstance, new SolrSchedulerJobInstancesInitialisationParametersImpl(false));

        SearchResults<SchedulerJobInstanceRecord> schedulerJobInstanceRecords
            = this.service.getSchedulerJobInstancesByContextInstanceId(contextInstance.getId()
            , -1, -1, null, null);


        schedulerJobInstanceRecords.getResultList()
            .forEach(job -> {
                if(job.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    Assert.assertEquals(false, job.getSchedulerJobInstance().isHeld());
                    Assert.assertEquals(InstanceStatus.WAITING, job.getSchedulerJobInstance().getStatus());
                    Assert.assertEquals(InstanceStatus.WAITING.name(), job.getStatus());
                }
            });

        this.service.holdJobsWithinContext(contextInstance, contextInstance.getName());

       schedulerJobInstanceRecords = this.service.getSchedulerJobInstancesByContextInstanceId
           (contextInstance.getId(), -1, -1, null, null);

        schedulerJobInstanceRecords.getResultList()
            .forEach(job -> {
                if(job.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    Assert.assertEquals(true, job.getSchedulerJobInstance().isHeld());
                    Assert.assertEquals(InstanceStatus.ON_HOLD, job.getSchedulerJobInstance().getStatus());
                    Assert.assertEquals(InstanceStatus.ON_HOLD.name(), job.getStatus());
                }
            });

        List<SchedulerJobInstanceRecord> jobsToRelease
            = this.service.getJobsToReleaseWithinContext(contextInstance, contextInstance.getName());

        Assert.assertEquals(400, jobsToRelease.size());
    }

    @Test
    public void test_initialise_scheduler_job_instances_with_custom_execution_environments() throws SchedulerJobInstanceInitialisationException {
        this.insertFileEventRecords("file", 1, "context");
        this.insertQuartzScheduleEventRecords("quartz", 1, "context");
        this.insertInternalEventDrivenRecords("internal", 1, "context", "POWERSHELL"); //expect to be change
        this.insertInternalEventDrivenRecords("internalB", 1, "context", "some-non-replace-value"); // should not change
        this.insertGlobalEventRecords("global", 1, "context", false);

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
                    } else if (schedulerJobRecord.getJob() instanceof SolrGlobalEventJobImpl) {
                        return (SchedulerJobInstance)objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrGlobalEventJobInstanceImpl.class);
                    }
                }
                catch (Exception e) {
                    return null;
                }
                return null;
            })
            .collect(Collectors.toList())
        );

        SchedulerJobInstancesInitialisationParameters parameters = new SolrSchedulerJobInstancesInitialisationParametersImpl(false);

        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("context");

        List<SchedulerJobInstance> schedulerJobInstances = this.service.initialiseSchedulerJobInstancesForContext
            (contextTemplate, contextInstance, parameters);

        Assert.assertEquals(5, schedulerJobInstances.size());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service.getSchedulerJobInstancesByContextInstanceId
            ("contextInstanceId", 5, 0, null, null);

        Assert.assertEquals(5, searchResults.getResultList().size());

        searchResults.getResultList()
            .forEach(job -> {
                if(job.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    if (job.getJobName().equals("internaljobName0")) {
                        // Should change based on the "getSchedulerJobExecutionEnvironmentLabel" config in this test class
                        Assert.assertEquals("powershell.exe|-Command", ((InternalEventDrivenJobInstance) job.getSchedulerJobInstance()).getExecutionEnvironmentProperties());
                    } else if (job.getJobName().equals("internalBjobName0")) {
                        // Leave the value from the job setup
                        Assert.assertEquals("some-non-replace-value", ((InternalEventDrivenJobInstance) job.getSchedulerJobInstance()).getExecutionEnvironmentProperties());
                    } else {
                        Assert.fail("Something went wrong in the test, so failing the test");
                    }
                }
            });
    }

    @Test
    public void test_delete_by_context_instance_id() {
        List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
        IntStream.range(0, 371).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance1",
                "context1", "job1"+i));
            schedulerJobInstanceRecords.addAll(this.createCommandExecutionJobInstanceRecord("contextInstance1",
                "context1", "job1-1"+i, List.of(), false, InstanceStatus.WAITING));
            schedulerJobInstanceRecords.addAll(this.createCommandExecutionJobInstanceRecord("contextInstance1",
                "context1", "job1-1"+i, List.of(), false, InstanceStatus.WAITING));
            schedulerJobInstanceRecords.addAll(this.createContextStartJobInstanceRecord("contextInstance1",
                    "context1", "startJob-1"+i, List.of("child1", "child2")));
            schedulerJobInstanceRecords.addAll(this.createContextTerminalJobInstanceRecord("contextInstance1",
                "context1", "terminal-1"+i, List.of("child1", "child2")));
            schedulerJobInstanceRecords.addAll(this.createBridgingJobInstanceRecord("contextInstance1",
                "context1", "bridging-1"+i, List.of("child1", "child2")));
        });

        IntStream.range(0, 275).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance2",
                "context1", "job2"+i));
        });

        IntStream.range(0, 167).forEach(i -> {
            schedulerJobInstanceRecords.add(this.createQuartzSchedulerJobInstanceRecord("contextInstance3",
                "context2", "context2Job"+i));
        });

        this.service.save(schedulerJobInstanceRecords);

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.service
            .getSchedulerJobInstancesByContextInstanceId("contextInstance1", 1000, 0, null, null);

        Assert.assertEquals(2597, searchResults.getTotalNumberOfResults());

        this.service.deleteSchedulerJobInstances("contextInstance1");

       searchResults = this.service
            .getSchedulerJobInstancesByContextInstanceId("contextInstance1", 1000, 0, null, null);

        Assert.assertEquals(0, searchResults.getResultList().size());
        Assert.assertEquals(0, searchResults.getTotalNumberOfResults());

        searchResults = this.service
            .getSchedulerJobInstancesByContextInstanceId("contextInstance2", 1000, 0, null, null);

        Assert.assertEquals(275, searchResults.getResultList().size());
        Assert.assertEquals(275, searchResults.getTotalNumberOfResults());

        searchResults = this.service
            .getSchedulerJobInstancesByContextInstanceId("contextInstance3", 1000, 0, null, null);

        Assert.assertEquals(167, searchResults.getResultList().size());
        Assert.assertEquals(167, searchResults.getTotalNumberOfResults());
    }

    private SchedulerJobInstanceRecord createQuartzSchedulerJobInstanceRecord(String contextInstanceId, String contextName
        , String jobName) {
        return this.createQuartzSchedulerJobInstanceRecord(contextInstanceId, contextName, jobName, "");
    }
    private SchedulerJobInstanceRecord createQuartzSchedulerJobInstanceRecord(String contextInstanceId, String contextName
        , String jobName, String displayName) {
        SolrQuartzScheduleDrivenJobInstanceImpl solrSchedulerJobInstance = new SolrQuartzScheduleDrivenJobInstanceImpl();
        solrSchedulerJobInstance.setJobName(jobName);
        solrSchedulerJobInstance.setDisplayName(displayName);
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

    private List<SchedulerJobInstanceRecord> createCommandExecutionJobInstanceRecord(String contextInstanceId, String contextName, String jobName
        , List<String> childContextNames, boolean targetResiding, InstanceStatus status) {
        List<SchedulerJobInstanceRecord> records = new ArrayList<>();
        childContextNames.forEach(child -> {
            SolrInternalEventDrivenJobInstanceImpl solrSchedulerJobInstance = new SolrInternalEventDrivenJobInstanceImpl();
            solrSchedulerJobInstance.setJobName(jobName);
            solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());
            solrSchedulerJobInstance.setChildContextName(child);
            solrSchedulerJobInstance.setTargetResidingContextOnly(targetResiding);

            SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
            schedulerJobInstanceRecord.setJobName(jobName);
            schedulerJobInstanceRecord.setContextInstanceId(contextInstanceId);
            schedulerJobInstanceRecord.setContextName(contextName);
            schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
            schedulerJobInstanceRecord.setTimestamp(1000000L);
            schedulerJobInstanceRecord.setStatus(status.name());
            schedulerJobInstanceRecord.setChildContextName(child);
            schedulerJobInstanceRecord.setTargetResidingContextOnly(targetResiding);

            records.add(schedulerJobInstanceRecord);
        });

        return records;
    }

    private List<SchedulerJobInstanceRecord> createContextStartJobInstanceRecord(String contextInstanceId, String contextName, String jobName
        , List<String> childContextNames) {
        List<SchedulerJobInstanceRecord> records = new ArrayList<>();
        childContextNames.forEach(child -> {
            ContextStartJobInstance solrSchedulerJobInstance = new SolrContextStartJobInstanceImpl();
            solrSchedulerJobInstance.setJobName(jobName);
            solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());
            solrSchedulerJobInstance.setChildContextName(child);

            SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
            schedulerJobInstanceRecord.setJobName(jobName);
            schedulerJobInstanceRecord.setContextInstanceId(contextInstanceId);
            schedulerJobInstanceRecord.setContextName(contextName);
            schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
            schedulerJobInstanceRecord.setTimestamp(1000000L);
            schedulerJobInstanceRecord.setChildContextName(child);

            records.add(schedulerJobInstanceRecord);
        });

        return records;
    }

    private List<SchedulerJobInstanceRecord> createContextTerminalJobInstanceRecord(String contextInstanceId, String contextName, String jobName
        , List<String> childContextNames) {
        List<SchedulerJobInstanceRecord> records = new ArrayList<>();
        childContextNames.forEach(child -> {
            ContextTerminalJobInstance solrSchedulerJobInstance = new SolrContextTerminalJobInstanceImpl();
            solrSchedulerJobInstance.setJobName(jobName);
            solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());
            solrSchedulerJobInstance.setChildContextName(child);

            SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
            schedulerJobInstanceRecord.setJobName(jobName);
            schedulerJobInstanceRecord.setContextInstanceId(contextInstanceId);
            schedulerJobInstanceRecord.setContextName(contextName);
            schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
            schedulerJobInstanceRecord.setTimestamp(1000000L);
            schedulerJobInstanceRecord.setChildContextName(child);

            records.add(schedulerJobInstanceRecord);
        });

        return records;
    }

    private List<SchedulerJobInstanceRecord> createBridgingJobInstanceRecord(String contextInstanceId, String contextName, String jobName
        , List<String> childContextNames) {
        List<SchedulerJobInstanceRecord> records = new ArrayList<>();
        childContextNames.forEach(child -> {
            BridgingJobInstance solrSchedulerJobInstance = new SolrBridgingJobInstanceImpl();
            solrSchedulerJobInstance.setJobName(jobName);
            solrSchedulerJobInstance.setScheduledProcessEvent(createProcessEvent());
            solrSchedulerJobInstance.setChildContextName(child);

            SolrSchedulerJobInstanceRecordImpl schedulerJobInstanceRecord = new SolrSchedulerJobInstanceRecordImpl();
            schedulerJobInstanceRecord.setJobName(jobName);
            schedulerJobInstanceRecord.setContextInstanceId(contextInstanceId);
            schedulerJobInstanceRecord.setContextName(contextName);
            schedulerJobInstanceRecord.setSchedulerJobInstance(solrSchedulerJobInstance);
            schedulerJobInstanceRecord.setTimestamp(1000000L);
            schedulerJobInstanceRecord.setChildContextName(child);

            records.add(schedulerJobInstanceRecord);
        });

        return records;
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
        event.setFireTime(1669397423771L);
        event.setNextFireTime(System.currentTimeMillis() + 1000);
        event.setCompletionTime(1669397429771L);
        event.setDryRun(true);
        event.setContextName("contextId " + RandomStringUtils.randomAlphabetic(5));
        event.setChildContextNames(List.of("childContextId1", "childContextId2"));
        event.setContextInstanceId("contextInstanceId " + RandomStringUtils.randomAlphabetic(5));
        event.setJobStarting(true);

        SolrDryRunParameters dryRunParams = new SolrDryRunParameters();
        event.setDryRunParameters(dryRunParams);

        event.setSkipped(false);

        SolrInternalEventDrivenJobInstanceImpl internalEventDrivenJob = new SolrInternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setSuccessfulReturnCodes(List.of("Code1", "Code2"));
        internalEventDrivenJob.setWorkingDirectory("workingDirectory/" + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setMinExecutionTime(System.currentTimeMillis());
        internalEventDrivenJob.setMaxExecutionTime(System.currentTimeMillis() + 3000);

        SolrContextParameterImpl contextParameter = new SolrContextParameterImpl();
        contextParameter.setDefaultValue("defaultValue " + RandomStringUtils.randomAlphabetic(5));
        contextParameter.setName("name " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setContextParameters(List.of(contextParameter));
        internalEventDrivenJob.setDaysOfWeekToRun(List.of(1, 2, 3, 4, 5));

        event.setInternalEventDrivenJob(internalEventDrivenJob);

        return event;
    }

    private void insertFileEventRecords(String idPrefix, int num, String contextId) {
        List<FileEventDrivenJobRecord> jobRecords = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrFileEventDrivenJob.setJobName(idPrefix+"jobName"+i);
            solrFileEventDrivenJob.setIdentifier(solrFileEventDrivenJob.getAgentName()+"_"+solrFileEventDrivenJob.getJobName());
            solrFileEventDrivenJob.setContextName(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression");
            solrFileEventDrivenJob.setFilePath("filePath");

            SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
            solrFileEventDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrFileEventDrivenJobRecord.setJobName("jobName"+i);
            solrFileEventDrivenJobRecord.setContextName(contextId);
            solrFileEventDrivenJobRecord.setTimestamp(1000000L);
            solrFileEventDrivenJobRecord.setFileEventDrivenJob(solrFileEventDrivenJob);

            jobRecords.add(solrFileEventDrivenJobRecord);
        });

        this.solrFileEventDrivenJobRecordDao.save(jobRecords);
    }

    private void insertQuartzScheduleEventRecords(String idPrefix, int num, String contextId) {
        List<QuartzScheduleDrivenJobRecord> jobRecords = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrQuartzScheduleDrivenJob.setJobName(idPrefix+"jobName"+i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName()+"_"+solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextName(contextId);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression");

            SolrQuartzScheduleDrivenJobRecordImpl solrQuartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
            solrQuartzScheduleDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrQuartzScheduleDrivenJobRecord.setJobName("jobName"+i);
            solrQuartzScheduleDrivenJobRecord.setContextName(contextId);
            solrQuartzScheduleDrivenJobRecord.setTimestamp(1000000L);
            solrQuartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(solrQuartzScheduleDrivenJob);

            jobRecords.add(solrQuartzScheduleDrivenJobRecord);
        });
        this.solrQuartzScheduleDrivenJobRecordDao.save(jobRecords);
    }

    private void insertInternalEventDrivenRecords(String idPrefix, int num, String contextId, String executionEnvironmentProperty) {
        List<InternalEventDrivenJobRecord> jobRecords = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrInternalEventDrivenJob.setJobName(idPrefix+"jobName"+i);
            solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName()+"_"+solrInternalEventDrivenJob.getJobName());
            solrInternalEventDrivenJob.setContextName(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -la");
            solrInternalEventDrivenJob.setChildContextNames(List.of(contextId));
            if (StringUtils.isNotBlank(executionEnvironmentProperty)) {
                solrInternalEventDrivenJob.setExecutionEnvironmentProperties(executionEnvironmentProperty);
            }

            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrInternalEventDrivenJobRecord.setJobName("jobName"+i);
            solrInternalEventDrivenJobRecord.setContextName(contextId);
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);

            jobRecords.add(solrInternalEventDrivenJobRecord);
        });
        this.solrInternalEventDrivenJobRecordDao.save(jobRecords);
    }

    private void insertGlobalEventRecords(String idPrefix, int num, String contextId, boolean skip) {
        List<GlobalEventJobRecord> jobRecords = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrGlobalEventJobImpl solrGlobalEventJob = new SolrGlobalEventJobImpl();
            solrGlobalEventJob.setAgentName(idPrefix+"agentName"+i);
            solrGlobalEventJob.setJobName(idPrefix+"jobName"+i);
            solrGlobalEventJob.setIdentifier(solrGlobalEventJob.getAgentName()+"_"+solrGlobalEventJob.getJobName());
            solrGlobalEventJob.setContextName(contextId);

            if(skip) {
                solrGlobalEventJob.setSkippedContexts(Map.of(contextId, true));
            }

            SolrGlobalEventJobRecordImpl solrGlobalEventJobRecord = new SolrGlobalEventJobRecordImpl();
            solrGlobalEventJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrGlobalEventJobRecord.setJobName("jobName"+i);
            solrGlobalEventJobRecord.setContextName(contextId);
            solrGlobalEventJobRecord.setTimestamp(1000000L);
            solrGlobalEventJobRecord.setGlobalEventJob(solrGlobalEventJob);
            
            jobRecords.add(solrGlobalEventJobRecord);
        });

        this.solrGlobalEventJobRecordDao.save(jobRecords);
    }

    public List<InternalEventDrivenJobRecord> loadInternalEventDrivenJobRecords(String directory, String jobsBase) throws IOException {
        List<InternalEventDrivenJob> files = Files.list(Path.of(directory)).map(path -> {
            InternalEventDrivenJob internalEventDrivenJob = null;
            try {
                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
                internalEventDrivenJob = objectMapper.readValue(jobJson, SolrInternalEventDrivenJobImpl.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return internalEventDrivenJob;
        }).collect(Collectors.toList());

        return files.stream()
            .map(internalEventDrivenJob -> {
                SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
                solrInternalEventDrivenJobRecord.setAgentName(internalEventDrivenJob.getAgentName());
                solrInternalEventDrivenJobRecord.setJobName(internalEventDrivenJob.getJobName());
                solrInternalEventDrivenJobRecord.setContextName(internalEventDrivenJob.getContextName());
                solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
                solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
                return solrInternalEventDrivenJobRecord;
            }).collect(Collectors.toList());
    }

    public List<QuartzScheduleDrivenJobRecord> loadQuartzScheduleDrivenJobRecords(String directory, String jobsBase) throws IOException {
        List<QuartzScheduleDrivenJob> files = Files.list(Path.of(directory)).map(path -> {
            QuartzScheduleDrivenJob quartzScheduleDrivenJob = null;
            try {
                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
                quartzScheduleDrivenJob = objectMapper.readValue(jobJson, SolrQuartzScheduleDrivenJobImpl.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return quartzScheduleDrivenJob;
        }).collect(Collectors.toList());

        return files.stream()
            .map(internalEventDrivenJob -> {
                QuartzScheduleDrivenJobRecord solrQuartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
                solrQuartzScheduleDrivenJobRecord.setAgentName(internalEventDrivenJob.getAgentName());
                solrQuartzScheduleDrivenJobRecord.setJobName(internalEventDrivenJob.getJobName());
                solrQuartzScheduleDrivenJobRecord.setContextName(internalEventDrivenJob.getContextName());
                solrQuartzScheduleDrivenJobRecord.setTimestamp(1000000L);
                solrQuartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(internalEventDrivenJob);
                return solrQuartzScheduleDrivenJobRecord;
            }).collect(Collectors.toList());
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

    private static Map<String, String> getSchedulerJobExecutionEnvironmentLabel() {
        Map<String, String> values = new HashMap<>();
        values.put("CMD", "cmd.exe|/c");
        values.put("BASH", "/bin/bash|-c");
        values.put("POWERSHELL", "powershell.exe|-Command");
        return values;
    }
}