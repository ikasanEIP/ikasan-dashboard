package org.ikasan.scheduled.job.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.job.dao.*;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SolrSchedulerJobServiceImplTest extends SolrTestCaseJ4 {

    private SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao;
    private SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao;
    private SolrGlobalEventJobDaoImpl globalEventJobRecordDao;
    private SolrContextStartJobDaoImpl solrContextStartJobDao;
    private SolrContextTerminalJobDaoImpl solrContextTerminalJobDao;
    private SolrSchedulerJobDaoImpl schedulerJobRecordDao;
    private SolrInternalEventDrivenJobTemplateDaoImpl solrInternalEventDrivenJobTemplateDao;
    private Path tmpPath;
    private EmbeddedSolrServer server;
    private SchedulerJobService service;

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

        this.fileEventDrivenJobRecordDao = new SolrFileEventDrivenJobDaoImpl();
        this.fileEventDrivenJobRecordDao.setSolrClient(server);

        this.internalEventDrivenJobRecordDao = new SolrInternalEventDrivenJobDaoImpl();
        this.internalEventDrivenJobRecordDao.setSolrClient(server);

        this.quartzScheduleDrivenJobRecordDao = new SolrQuartzScheduleDrivenJobDaoImpl();
        this.quartzScheduleDrivenJobRecordDao.setSolrClient(server);

        this.globalEventJobRecordDao = new SolrGlobalEventJobDaoImpl();
        this.globalEventJobRecordDao.setSolrClient(server);

        this.solrContextStartJobDao = new SolrContextStartJobDaoImpl();
        this.solrContextStartJobDao.setSolrClient(server);

        this.solrContextTerminalJobDao = new SolrContextTerminalJobDaoImpl();
        this.solrContextTerminalJobDao.setSolrClient(server);

        this.schedulerJobRecordDao = new SolrSchedulerJobDaoImpl();
        this.schedulerJobRecordDao.setSolrClient(server);

        this.solrInternalEventDrivenJobTemplateDao = new SolrInternalEventDrivenJobTemplateDaoImpl();
        this.solrInternalEventDrivenJobTemplateDao.setSolrClient(server);


        this.service = new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, this.globalEventJobRecordDao,
            this.solrContextStartJobDao, this.solrContextTerminalJobDao, this.schedulerJobRecordDao,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test
    public void test_save_scheduler_records_null_records_should_not_npe() {
        service.save(null, "system");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_fileEventDrivenJobDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            null, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, this.globalEventJobRecordDao,
            this.solrContextStartJobDao, this.solrContextTerminalJobDao, this.schedulerJobRecordDao,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_internalEventDrivenJobDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, null,
            this.quartzScheduleDrivenJobRecordDao, this.globalEventJobRecordDao,
            this.solrContextStartJobDao, this.solrContextTerminalJobDao, this.schedulerJobRecordDao,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_quartzScheduleDrivenJobDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            null, this.globalEventJobRecordDao,
            this.solrContextStartJobDao, this.solrContextTerminalJobDao, this.schedulerJobRecordDao,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_globalEventJobDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, null,
            this.solrContextStartJobDao, this.solrContextTerminalJobDao, this.schedulerJobRecordDao,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solrContextStartJobDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, this.globalEventJobRecordDao,
            null, this.solrContextTerminalJobDao, this.schedulerJobRecordDao,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solrContextTerminalJobDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, this.globalEventJobRecordDao,
            this.solrContextStartJobDao, null, this.schedulerJobRecordDao,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_schedulerJobRecordDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, this.globalEventJobRecordDao,
            this.solrContextStartJobDao, this.solrContextTerminalJobDao, null,
            this.solrInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solrInternalEventDrivenJobTemplateDao_throwsException() {
        new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, this.globalEventJobRecordDao,
            this.solrContextStartJobDao, this.solrContextTerminalJobDao, this.schedulerJobRecordDao,
            null
        );
    }

    @Test
    public void test_findById() throws SolrServerException, IOException {
        String contextName = "contextName";
        InternalEventDrivenJob solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        solrInternalEventDrivenJob.setAgentName(contextName + "agentName");
        solrInternalEventDrivenJob.setJobName(contextName + "jobNameInternal");
        solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
        solrInternalEventDrivenJob.setContextName(contextName);
        solrInternalEventDrivenJob.setCommandLine("ls -al");
        solrInternalEventDrivenJob.setChildContextNames(List.of("child"));

        this.service.saveInternalEventDrivenJob(solrInternalEventDrivenJob, "tester");
        this.server.commit();

        String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + solrInternalEventDrivenJob.getAgentName() + "_"
            + solrInternalEventDrivenJob.getJobName() + "_" + solrInternalEventDrivenJob.getContextName();

        SchedulerJobRecord result = this.service.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals(id, result.getId());
        Assert.assertEquals(solrInternalEventDrivenJob.getAgentName(), result.getAgentName());
        Assert.assertEquals(solrInternalEventDrivenJob.getJobName(), result.getJobName());
        Assert.assertEquals(solrInternalEventDrivenJob.getContextName(), result.getContextName());
        Assert.assertTrue(result.getJob() instanceof InternalEventDrivenJob);
        InternalEventDrivenJob foundJob = (InternalEventDrivenJob) result.getJob();
        Assert.assertEquals(solrInternalEventDrivenJob.getCommandLine(), foundJob.getCommandLine());
    }

    @Test
    public void test_findByAgent() throws SolrServerException, IOException {
        String agentName = "testAgent";
        String contextName = "testContext";
        List<SchedulerJob> jobsToSave = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            InternalEventDrivenJob job = new SolrInternalEventDrivenJobImpl();
            job.setAgentName(agentName);
            job.setJobName("jobName" + i);
            job.setIdentifier(job.getAgentName() + "_" + job.getJobName());
            job.setContextName(contextName);
            job.setCommandLine("command" + i);
            jobsToSave.add(job);
        }

        this.service.save(jobsToSave, "tester");
        this.server.commit();

        SearchResults<SchedulerJobRecord> results = this.service.findByAgent(agentName, 10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(5, results.getResultList().size());
        for (int i = 0; i < 5; i++) {
            SchedulerJobRecord record = results.getResultList().get(i);
            Assert.assertEquals(agentName, record.getAgentName());
            Assert.assertEquals("jobName" + i, record.getJobName());
            Assert.assertEquals(contextName, record.getContextName());
            Assert.assertTrue(record.getJob() instanceof InternalEventDrivenJob);
            InternalEventDrivenJob foundJob = (InternalEventDrivenJob) record.getJob();
            Assert.assertEquals("command" + i, foundJob.getCommandLine());
        }
    }

    @Test
    public void test_findByFilter() throws SolrServerException, IOException {
        String agentName1 = "agent1";
        String agentName2 = "agent2";
        String contextName1 = "context1";
        String contextName2 = "context2";
        String jobName1 = "job1";
        String jobName2 = "job2";

        List<SchedulerJob> jobsToSave = new ArrayList<>();

        // Job 1: InternalEventDrivenJob, agent1, context1, job1
        InternalEventDrivenJob job1 = new SolrInternalEventDrivenJobImpl();
        job1.setAgentName(agentName1);
        job1.setJobName(jobName1);
        job1.setIdentifier(job1.getAgentName() + "_" + job1.getJobName());
        job1.setContextName(contextName1);
        job1.setCommandLine("cmd1");
        jobsToSave.add(job1);

        // Job 2: FileEventDrivenJob, agent1, context1, job2
        FileEventDrivenJob job2 = new SolrFileEventDrivenJobImpl();
        job2.setAgentName(agentName1);
        job2.setJobName(jobName2);
        job2.setIdentifier(job2.getAgentName() + "_" + job2.getJobName());
        job2.setContextName(contextName1);
        job2.setFilePath("path1");
        jobsToSave.add(job2);

        // Job 3: InternalEventDrivenJob, agent2, context2, job1
        InternalEventDrivenJob job3 = new SolrInternalEventDrivenJobImpl();
        job3.setAgentName(agentName2);
        job3.setJobName(jobName1);
        job3.setIdentifier(job3.getAgentName() + "_" + job3.getJobName());
        job3.setContextName(contextName2);
        job3.setCommandLine("cmd2");
        jobsToSave.add(job3);

        // Job 4: QuartzScheduleDrivenJob, agent2, context2, job2
        QuartzScheduleDrivenJob job4 = new SolrQuartzScheduleDrivenJobImpl();
        job4.setAgentName(agentName2);
        job4.setJobName(jobName2);
        job4.setIdentifier(job4.getAgentName() + "_" + job4.getJobName());
        job4.setContextName(contextName2);
        job4.setCronExpression("cron1");
        jobsToSave.add(job4);

        this.service.save(jobsToSave, "tester");
        this.server.commit();

        // Test 1: Filter by contextName2 and jobType InternalEventDrivenJob
        SchedulerJobSearchFilter filter2 = new SolrSchedulerJobSearchFilterImpl();
        filter2.setContextSearchFilter(contextName2);
        filter2.setJobTypeFilter(JobConstants.INTERNAL_EVENT_DRIVEN_JOB);
        SearchResults<SchedulerJobRecord> results2 = this.service.findByFilter(filter2, 10, 0, null, null);
        Assert.assertNotNull(results2);
        Assert.assertEquals(1, results2.getResultList().size());
        Assert.assertEquals(agentName2, results2.getResultList().get(0).getAgentName());
        Assert.assertEquals(jobName1, results2.getResultList().get(0).getJobName());
        Assert.assertTrue(results2.getResultList().get(0).getJob() instanceof InternalEventDrivenJob);

        // Test 2: Filter by jobName2
        SchedulerJobSearchFilter filter3 = new SolrSchedulerJobSearchFilterImpl();
        filter3.setJobNameFilter(jobName2);
        SearchResults<SchedulerJobRecord> results3 = this.service.findByFilter(filter3, 10, 0, null, null);
        Assert.assertNotNull(results3);
        Assert.assertEquals(2, results3.getResultList().size());
        Assert.assertTrue(results3.getResultList().stream().allMatch(r -> r.getJobName().equals(jobName2)));

        // Test 3: No matching results
        SchedulerJobSearchFilter filter4 = new SolrSchedulerJobSearchFilterImpl();
        filter4.setJobNameFilter("nonExistentJob");
        SearchResults<SchedulerJobRecord> results4 = this.service.findByFilter(filter4, 10, 0, null, null);
        Assert.assertNotNull(results4);
        Assert.assertEquals(0, results4.getResultList().size());
    }

    @Test
    public void test_delete() throws SolrServerException, IOException {
        String contextName = "contextNameForDelete";
        InternalEventDrivenJob jobToDelete = new SolrInternalEventDrivenJobImpl();
        jobToDelete.setAgentName(contextName + "agentName");
        jobToDelete.setJobName(contextName + "jobNameInternal");
        jobToDelete.setIdentifier(jobToDelete.getAgentName() + "_" + jobToDelete.getJobName());
        jobToDelete.setContextName(contextName);
        jobToDelete.setCommandLine("ls -al");
        jobToDelete.setChildContextNames(List.of("child"));

        this.service.saveInternalEventDrivenJob(jobToDelete, "tester");
        this.server.commit();

        String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + jobToDelete.getAgentName() + "_"
            + jobToDelete.getJobName() + "_" + jobToDelete.getContextName();

        SchedulerJobRecord savedRecord = this.service.findById(id);
        Assert.assertNotNull(savedRecord);

        this.service.delete(savedRecord);
        this.server.commit();

        SchedulerJobRecord deletedRecord = this.service.findById(id);
        Assert.assertNull(deletedRecord);
    }

    @Test
    public void test_deleteByAgentName() throws SolrServerException, IOException {
        String agentNameToDelete = "agentToDelete";
        String agentToKeep = "agentToKeep";
        String contextName = "testContext";
        List<SchedulerJob> jobsToSave = new ArrayList<>();

        // Jobs to delete
        for (int i = 0; i < 3; i++) {
            InternalEventDrivenJob job = new SolrInternalEventDrivenJobImpl();
            job.setAgentName(agentNameToDelete);
            job.setJobName("jobToDelete" + i);
            job.setIdentifier(job.getAgentName() + "_" + job.getJobName());
            job.setContextName(contextName);
            job.setCommandLine("cmd" + i);
            jobsToSave.add(job);
        }

        // Jobs to keep
        for (int i = 0; i < 2; i++) {
            InternalEventDrivenJob job = new SolrInternalEventDrivenJobImpl();
            job.setAgentName(agentToKeep);
            job.setJobName("jobToKeep" + i);
            job.setIdentifier(job.getAgentName() + "_" + job.getJobName());
            job.setContextName(contextName);
            job.setCommandLine("cmd" + i);
            jobsToSave.add(job);
        }

        this.service.save(jobsToSave, "tester");
        this.server.commit();

        // Verify jobs exist before deletion
        SearchResults<SchedulerJobRecord> resultsBeforeDelete = this.service.findByAgent(agentNameToDelete, 10, 0);
        Assert.assertEquals(3, resultsBeforeDelete.getResultList().size());
        SearchResults<SchedulerJobRecord> resultsToKeepBeforeDelete = this.service.findByAgent(agentToKeep, 10, 0);
        Assert.assertEquals(2, resultsToKeepBeforeDelete.getResultList().size());

        // Delete by agent name
        this.service.deleteByAgentName(agentNameToDelete);
        this.server.commit();

        // Verify jobs are deleted and others remain
        SearchResults<SchedulerJobRecord> resultsAfterDelete = this.service.findByAgent(agentNameToDelete, 10, 0);
        Assert.assertEquals(0, resultsAfterDelete.getResultList().size());
        SearchResults<SchedulerJobRecord> resultsToKeepAfterDelete = this.service.findByAgent(agentToKeep, 10, 0);
        Assert.assertEquals(2, resultsToKeepAfterDelete.getResultList().size());
    }

    @Test
    public void test_saveInternalEventDrivenJobTemplateRecord() throws SolrServerException, IOException {
        String contextName = "templateContext";
        String agentName = "templateAgent";
        String jobName = "templateJob";
        String commandLine = "templateCmd";

        InternalEventDrivenJob internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName(agentName);
        internalEventDrivenJob.setJobName(jobName);
        internalEventDrivenJob.setIdentifier(agentName + "_" + jobName);
        internalEventDrivenJob.setContextName(contextName);
        internalEventDrivenJob.setCommandLine(commandLine);
        internalEventDrivenJob.setTemplateJob(true);

        InternalEventDrivenJobRecord recordToSave = new SolrInternalEventDrivenJobRecordImpl();
        recordToSave.setInternalEventDrivenJob(internalEventDrivenJob);
        recordToSave.setAgentName(agentName);
        recordToSave.setJobName(jobName);
        recordToSave.setContextName(contextName);
        recordToSave.setModifiedBy("tester");
        recordToSave.setTimestamp(System.currentTimeMillis());

        this.service.saveInternalEventDrivenJobTemplateRecord(recordToSave, "tester");
        this.server.commit();

        String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + agentName + "_" + jobName + "_" + contextName;
        InternalEventDrivenJobRecord foundRecord = this.solrInternalEventDrivenJobTemplateDao.findById(id);

        Assert.assertNotNull(foundRecord);
        Assert.assertEquals(id, foundRecord.getId());
        Assert.assertEquals(agentName, foundRecord.getAgentName());
        Assert.assertEquals(jobName, foundRecord.getJobName());
        Assert.assertEquals(contextName, foundRecord.getContextName());
        Assert.assertEquals(commandLine, foundRecord.getInternalEventDrivenJob().getCommandLine());
        Assert.assertTrue(foundRecord.getInternalEventDrivenJob().isTemplateJob());
    }

    @Test
    public void test_saveInternalEventDrivenJobTemplates() throws SolrServerException, IOException {
        String contextName = "templateContextBatch";
        String agentName = "templateAgentBatch";
        List<InternalEventDrivenJob> jobsToSave = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            InternalEventDrivenJob job = new SolrInternalEventDrivenJobImpl();
            job.setAgentName(agentName);
            job.setJobName("jobName" + i);
            job.setIdentifier(job.getAgentName() + "_" + job.getJobName());
            job.setContextName(contextName);
            job.setCommandLine("command" + i);
            job.setTemplateJob(true);
            jobsToSave.add(job);
        }

        this.service.saveInternalEventDrivenJobTemplates(jobsToSave, "batchTester");
        this.server.commit();

        for (int i = 0; i < 3; i++) {
            InternalEventDrivenJob savedJob = jobsToSave.get(i);
            String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + savedJob.getAgentName() + "_"
                + savedJob.getJobName() + "_" + savedJob.getContextName();

            InternalEventDrivenJobRecord foundRecord = this.solrInternalEventDrivenJobTemplateDao.findById(id);

            Assert.assertNotNull(foundRecord);
            Assert.assertEquals(id, foundRecord.getId());
            Assert.assertEquals(savedJob.getAgentName(), foundRecord.getAgentName());
            Assert.assertEquals(savedJob.getJobName(), foundRecord.getJobName());
            Assert.assertEquals(savedJob.getContextName(), foundRecord.getContextName());
            Assert.assertEquals(savedJob.getCommandLine(), foundRecord.getInternalEventDrivenJob().getCommandLine());
            Assert.assertTrue(foundRecord.getInternalEventDrivenJob().isTemplateJob());
            Assert.assertEquals("batchTester", foundRecord.getModifiedBy());
        }
    }

    @Test
    public void test_saveInternalEventDrivenJobTemplateRecords() throws SolrServerException, IOException {
        String contextName = "templateContextRecords";
        String agentName = "templateAgentRecords";
        List<InternalEventDrivenJobRecord> recordsToSave = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            InternalEventDrivenJob internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            internalEventDrivenJob.setAgentName(agentName);
            internalEventDrivenJob.setJobName("jobName" + i);
            internalEventDrivenJob.setIdentifier(agentName + "_" + "jobName" + i);
            internalEventDrivenJob.setContextName(contextName);
            internalEventDrivenJob.setCommandLine("command" + i);
            internalEventDrivenJob.setTemplateJob(true);

            InternalEventDrivenJobRecord record = new SolrInternalEventDrivenJobRecordImpl();
            record.setInternalEventDrivenJob(internalEventDrivenJob);
            record.setAgentName(agentName);
            record.setJobName("jobName" + i);
            record.setContextName(contextName);
            record.setModifiedBy("recordsTester");
            record.setTimestamp(System.currentTimeMillis());
            recordsToSave.add(record);
        }

        this.service.saveInternalEventDrivenJobTemplateRecords(recordsToSave);
        this.server.commit();

        for (int i = 0; i < 3; i++) {
            InternalEventDrivenJobRecord savedRecord = recordsToSave.get(i);
            String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + savedRecord.getAgentName() + "_"
                + savedRecord.getJobName() + "_" + savedRecord.getContextName();

            InternalEventDrivenJobRecord foundRecord = this.solrInternalEventDrivenJobTemplateDao.findById(id);

            Assert.assertNotNull(foundRecord);
            Assert.assertEquals(id, foundRecord.getId());
            Assert.assertEquals(savedRecord.getAgentName(), foundRecord.getAgentName());
            Assert.assertEquals(savedRecord.getJobName(), foundRecord.getJobName());
            Assert.assertEquals(savedRecord.getContextName(), foundRecord.getContextName());
            Assert.assertEquals(savedRecord.getInternalEventDrivenJob().getCommandLine(), foundRecord.getInternalEventDrivenJob().getCommandLine());
            Assert.assertTrue(foundRecord.getInternalEventDrivenJob().isTemplateJob());
            Assert.assertEquals("recordsTester", foundRecord.getModifiedBy());
        }
    }

    @Test
    public void test_skip_globalEventJob() throws SolrServerException, IOException {
        String contextName = "globalEventContext";
        String agentName = JobConstants.GLOBAL_EVENT; // GlobalEventJob has a fixed agent name
        String jobName = "globalJob";
        String actor = "testActor";
        List<String> childContextNames = List.of("child1", "child2");

        GlobalEventJob globalEventJob = new SolrGlobalEventJobImpl();
        globalEventJob.setAgentName(agentName);
        globalEventJob.setJobName(jobName);
        globalEventJob.setIdentifier(agentName + "_" + jobName);
        globalEventJob.setContextName(contextName);
        globalEventJob.setChildContextNames(childContextNames);

        this.service.saveGlobalEventJob(globalEventJob, actor);
        this.server.commit();

        String id = JobConstants.GLOBAL_EVENT_JOB + "_" + agentName + "_" + jobName + "_" + contextName;
        SchedulerJobRecord savedRecord = this.service.findById(id);
        Assert.assertNotNull(savedRecord);
        Assert.assertFalse(savedRecord.isSkipped());
        Assert.assertTrue(savedRecord.getJob().getSkippedContexts().isEmpty());

        this.service.skip(savedRecord, childContextNames, actor);
        this.server.commit();

        SchedulerJobRecord skippedRecord = this.service.findById(id);
        Assert.assertNotNull(skippedRecord);
        Assert.assertEquals(actor, skippedRecord.getModifiedBy());
        Assert.assertEquals(2, skippedRecord.getJob().getSkippedContexts().size());
        Assert.assertTrue(skippedRecord.getJob().getSkippedContexts().containsKey("child1"));
        Assert.assertTrue(skippedRecord.getJob().getSkippedContexts().containsKey("child2"));
    }

    @Test
    public void test_enable_globalEventJob() throws SolrServerException, IOException {
        String contextName = "globalEventContextEnable";
        String agentName = JobConstants.GLOBAL_EVENT;
        String jobName = "globalJobEnable";
        String actor = "testActorEnable";
        String childContextToEnable = "childToEnable";
        List<String> childContextNames = List.of(childContextToEnable, "anotherChild");

        GlobalEventJob globalEventJob = new SolrGlobalEventJobImpl();
        globalEventJob.setAgentName(agentName);
        globalEventJob.setJobName(jobName);
        globalEventJob.setIdentifier(agentName + "_" + jobName);
        globalEventJob.setContextName(contextName);
        globalEventJob.setChildContextNames(childContextNames);

        this.service.saveGlobalEventJob(globalEventJob, actor);
        this.server.commit();

        String id = JobConstants.GLOBAL_EVENT_JOB + "_" + agentName + "_" + jobName + "_" + contextName;
        SchedulerJobRecord savedRecord = this.service.findById(id);
        Assert.assertNotNull(savedRecord);
        Assert.assertFalse(savedRecord.isSkipped());
        Assert.assertTrue(savedRecord.getJob().getSkippedContexts().isEmpty());

        // First, skip the job for a child context
        this.service.skip(savedRecord, List.of(childContextToEnable), actor);
        this.server.commit();

        SchedulerJobRecord skippedRecord = this.service.findById(id);
        Assert.assertNotNull(skippedRecord);
        Assert.assertTrue(skippedRecord.getJob().getSkippedContexts().containsKey(childContextToEnable));

        // Now, enable the job for that child context
        this.service.enable(skippedRecord, childContextToEnable, actor);
        this.server.commit();

        SchedulerJobRecord enabledRecord = this.service.findById(id);
        Assert.assertNotNull(enabledRecord);
        Assert.assertFalse(enabledRecord.isSkipped());
        Assert.assertFalse(enabledRecord.getJob().getSkippedContexts().containsKey(childContextToEnable));
        Assert.assertEquals(0, enabledRecord.getJob().getSkippedContexts().size());
    }

    @Test
    public void test_holdAll() throws SolrServerException, IOException {
        String contextName = "contextForHoldAll";
        String actor = "holdAllActor";
        List<SchedulerJob> jobsToSave = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            InternalEventDrivenJob job = new SolrInternalEventDrivenJobImpl();
            job.setAgentName(contextName + "agent" + i);
            job.setJobName("job" + i);
            job.setIdentifier(job.getAgentName() + "_" + job.getJobName());
            job.setContextName(contextName);
            job.setCommandLine("cmd" + i);
            jobsToSave.add(job);
        }

        this.service.save(jobsToSave, actor);
        this.server.commit();

        // Verify jobs are not held initially
        SearchResults<SchedulerJobRecord> resultsBeforeHold = this.service.findByContext(contextName, 10, 0);
        Assert.assertEquals(3, resultsBeforeHold.getResultList().size());
        resultsBeforeHold.getResultList().forEach(record -> Assert.assertFalse(record.isHeld()));

        // Call holdAll
        this.service.holdAll(contextName, actor);
        this.server.commit();

        // Verify all jobs are now held
        SearchResults<SchedulerJobRecord> resultsAfterHold = this.service.findByContext(contextName, 10, 0);
        Assert.assertEquals(3, resultsAfterHold.getResultList().size());
        resultsAfterHold.getResultList().forEach(record -> {
            Assert.assertTrue(record.isHeld());
            Assert.assertEquals(actor, record.getModifiedBy());
        });
    }

    @Test
    public void test_save_scheduler_records_and_delete_by_context_name() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);
        String contextId2 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);
        List<SchedulerJob> listOfRecords2 = createListOfRecords(contextId2);

        SearchResults results = service.findByContext(contextId1, 100, 0);
        assertEquals(0, results.getResultList().size());
        results = service.findByContext(contextId2, 100, 0);
        assertEquals(0, results.getResultList().size());

        service.save(listOfRecords1, "system");
        service.save(listOfRecords2, "system");

        results = service.findByContext(contextId1, 100, 0);
        validateResults(results, 18, contextId1);
        results = service.findByContext(contextId2, 100, 0);
        validateResults(results, 18, contextId2);

        service.deleteByContextName(contextId1);

        results = service.findByContext(contextId1, 100, 0);
        validateResults(results, 0, contextId1);
        results = service.findByContext(contextId2, 100, 0);
        validateResults(results, 18, contextId2);

        service.deleteByContextName(contextId2);

        results = service.findByContext(contextId1, 100, 0);
        validateResults(results, 0, contextId1);
        results = service.findByContext(contextId2, 100, 0);
        validateResults(results, 0, contextId2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_skip_wrong_scheduler_job_type() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof QuartzScheduleDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            this.service.findByContextNameAndJobName(contextId1, job.getJobName());
        });
    }

    @Test
    public void test_get_internal_event_job_map() {
        String contextId1 = "Context-Name";

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1, 50);

        service.save(listOfRecords1, "system");

        Map<String, InternalEventDrivenJob> results = this.service.getCommandExecutionJobsForContext(contextId1);

        Assert.assertEquals(50, results.size());
    }

    @Test
    public void test_skip_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().get("child"));
        });
    }

    @Test
    public void test_skip_and_enable_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().get("child"));
        });

        internalEventDrivenJobs.forEach(job ->
            this.service.enable(job, "context", "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().isEmpty());
        });
    }

    @Test
    public void test_skip_and_enable_all_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().get("child"));
        });

        this.service.enableAll(contextId1, "actor");

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().isEmpty());
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_hold_wrong_scheduler_job_type() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof FileEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            this.service.findByContextNameAndJobName(contextId1, job.getJobName());
        });
    }

    @Test
    public void test_hold_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().get("child"));
        });
    }

    @Test
    public void test_hold_and_release_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().get("child"));
        });

        internalEventDrivenJobs.forEach(job ->
            this.service.release(job, "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().isEmpty());
        });
    }

    @Test
    public void test_hold_and_release_all_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().get("child"));
        });

        this.service.releaseAll(contextId1, "actor");

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().isEmpty());
        });
    }

    @Test
    public void test_rename_context_on_scheduler_jobs() {
        String contextName = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextName);

        service.save(listOfRecords1, "system");

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextName, -1, -1);

        Assert.assertEquals(18, results.getResultList().size());

        this.service.renameContextForJobs(contextName, "newContextName", "actor");

        results = this.service.findByContext(contextName, -1, -1);

        Assert.assertEquals(0, results.getResultList().size());

        results = this.service.findByContext("newContextName", -1, -1);

        Assert.assertEquals(18, results.getResultList().size());

        results.getResultList().forEach(schedulerJobRecord -> {
            Assert.assertEquals("newContextName", schedulerJobRecord.getContextName());
            Assert.assertEquals("newContextName", schedulerJobRecord.getJob().getContextName());
        });
    }

    @Test
    public void test_save_internal_event_driven_job() {
        String contextName = "contextName";
        InternalEventDrivenJob solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        solrInternalEventDrivenJob.setAgentName(contextName + "agentName");
        solrInternalEventDrivenJob.setJobName(contextName + "jobNameInternal");
        solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
        solrInternalEventDrivenJob.setContextName(contextName);
        solrInternalEventDrivenJob.setCommandLine("ls -al");
        solrInternalEventDrivenJob.setChildContextNames(List.of("child"));

        this.service.saveInternalEventDrivenJob(solrInternalEventDrivenJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobNameInternal");
        InternalEventDrivenJob internalEventDrivenJob = (InternalEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(internalEventDrivenJob);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("ls -al", internalEventDrivenJob.getCommandLine());

        internalEventDrivenJob.setCommandLine("pwd");

        this.service.saveInternalEventDrivenJob(internalEventDrivenJob, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobNameInternal");
        internalEventDrivenJob = (InternalEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(internalEventDrivenJob);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("pwd", internalEventDrivenJob.getCommandLine());
    }

    @Test
    public void test_save_internal_event_driven_job_template() {
        String contextName = "contextName";
        InternalEventDrivenJob solrInternalEventDrivenJobTemplate = new SolrInternalEventDrivenJobImpl();
        solrInternalEventDrivenJobTemplate.setAgentName(contextName + "agentName");
        solrInternalEventDrivenJobTemplate.setJobName(contextName + "jobNameInternal");
        solrInternalEventDrivenJobTemplate.setIdentifier(solrInternalEventDrivenJobTemplate.getAgentName() + "_" + solrInternalEventDrivenJobTemplate.getJobName());
        solrInternalEventDrivenJobTemplate.setContextName(contextName);
        solrInternalEventDrivenJobTemplate.setCommandLine("ls -al");
        solrInternalEventDrivenJobTemplate.setChildContextNames(List.of("child"));

        this.service.saveInternalEventDrivenJobTemplate(solrInternalEventDrivenJobTemplate, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobNameInternal");
        InternalEventDrivenJob internalEventDrivenJob = (InternalEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(internalEventDrivenJob);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("ls -al", internalEventDrivenJob.getCommandLine());
        Assert.assertEquals(true, internalEventDrivenJob.isTemplateJob());

        internalEventDrivenJob.setCommandLine("pwd");

        this.service.saveInternalEventDrivenJobTemplate(internalEventDrivenJob, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobNameInternal");
        internalEventDrivenJob = (InternalEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(internalEventDrivenJob);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("pwd", internalEventDrivenJob.getCommandLine());
    }

    @Test
    public void test_save_quartz_driven_job() {
        String contextName = "contextName";
        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setAgentName(contextName + "agentName");
        quartzScheduleDrivenJob.setJobName(contextName + "jobName");
        quartzScheduleDrivenJob.setIdentifier(quartzScheduleDrivenJob.getAgentName() + "_" + quartzScheduleDrivenJob.getJobName());
        quartzScheduleDrivenJob.setContextName(contextName);
        quartzScheduleDrivenJob.setCronExpression("cronExpression");
        quartzScheduleDrivenJob.setChildContextNames(List.of("child"));

        this.service.saveQuartzScheduledJob(quartzScheduleDrivenJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        QuartzScheduleDrivenJob found = (QuartzScheduleDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("cronExpression", found.getCronExpression());

        found.setCronExpression("updatedCronExpression");

        this.service.saveQuartzScheduledJob(found, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        found = (QuartzScheduleDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("updatedCronExpression", found.getCronExpression());
    }

    @Test
    public void test_save_file_driven_job() {
        String contextName = "contextName";
        FileEventDrivenJob fileEventDrivenJob = new SolrFileEventDrivenJobImpl();
        fileEventDrivenJob.setAgentName(contextName + "agentName");
        fileEventDrivenJob.setJobName(contextName + "jobName");
        fileEventDrivenJob.setIdentifier(fileEventDrivenJob.getAgentName() + "_" + fileEventDrivenJob.getJobName());
        fileEventDrivenJob.setContextName(contextName);
        fileEventDrivenJob.setCronExpression("cronExpression");
        fileEventDrivenJob.setChildContextNames(List.of("child"));

        this.service.saveFileEventDrivenJob(fileEventDrivenJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        FileEventDrivenJob found = (FileEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("cronExpression", found.getCronExpression());

        found.setCronExpression("updatedCronExpression");

        this.service.saveFileEventDrivenJob(found, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        found = (FileEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("updatedCronExpression", found.getCronExpression());
    }

    @Test
    public void test_save_global_event_job() {
        String contextName = "contextName";
        GlobalEventJob globalEventJob = new SolrGlobalEventJobImpl();
        globalEventJob.setAgentName(contextName + "agentName");
        globalEventJob.setJobName(contextName + "jobName");
        globalEventJob.setIdentifier(globalEventJob.getAgentName() + "_" + globalEventJob.getJobName());
        globalEventJob.setContextName(contextName);
        globalEventJob.setChildContextNames(List.of("child"));

        this.service.saveGlobalEventJob(globalEventJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        GlobalEventJob found = (GlobalEventJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertTrue(found.getChildContextNames().contains("child"));

        found.setChildContextNames(List.of("child2"));

        this.service.saveGlobalEventJob(found, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        found = (GlobalEventJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertTrue(found.getChildContextNames().contains("child2"));
        Assert.assertFalse(found.getChildContextNames().contains("child"));
    }

    @Test
    public void test_save_context_start_job() {
        String contextName = "contextName";
        ContextStartJob contextStartJob = new SolrContextStartJobImpl();
        contextStartJob.setJobName(contextName + "jobName");
        contextStartJob.setIdentifier(contextStartJob.getAgentName() + "_" + contextStartJob.getJobName());
        contextStartJob.setContextName(contextName);
        contextStartJob.setChildContextNames(List.of("child"));

        this.service.saveContextStartJob(contextStartJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        ContextStartJob found = (ContextStartJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertTrue(found.getChildContextNames().contains("child"));

        found.setChildContextNames(List.of("child2"));

        this.service.saveContextStartJob(found, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        found = (ContextStartJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertTrue(found.getChildContextNames().contains("child2"));
        Assert.assertFalse(found.getChildContextNames().contains("child"));
    }

    @Test
    public void test_save_context_terminal_job() {
        String contextName = "contextName";
        ContextTerminalJob contextTerminalJob = new SolrContextTerminalJobImpl();
        contextTerminalJob.setJobName(contextName + "jobName");
        contextTerminalJob.setIdentifier(contextTerminalJob.getAgentName() + "_" + contextTerminalJob.getJobName());
        contextTerminalJob.setContextName(contextName);
        contextTerminalJob.setChildContextNames(List.of("child"));

        this.service.saveContextTerminalJob(contextTerminalJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        ContextTerminalJob found = (ContextTerminalJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertTrue(found.getChildContextNames().contains("child"));

        found.setChildContextNames(List.of("child2"));

        this.service.saveContextTerminalJob(found, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        found = (ContextTerminalJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertTrue(found.getChildContextNames().contains("child2"));
        Assert.assertFalse(found.getChildContextNames().contains("child"));
    }

    private void validateResults(SearchResults results, int expectedCount, String contextId) {
        assertEquals(expectedCount, results.getResultList().size());
        if (expectedCount > 0) {
            int resetCount = 0;
            for (int i = 0; i < results.getResultList().size(); i++) {
                SchedulerJobRecord job = (SolrSchedulerJobRecordImpl) results.getResultList().get(i);
                if(job.getJob() instanceof GlobalEventJob) {
                    assertEquals(JobConstants.GLOBAL_EVENT, job.getAgentName());
                }
                else if(job.getJob() instanceof ContextStartJob) {
                    assertEquals(JobConstants.CONTEXT_START_JOB, job.getAgentName());
                }
                else if(job.getJob() instanceof ContextTerminalJob) {
                    assertEquals(JobConstants.CONTEXT_TERMINAL_JOB, job.getAgentName());
                }
                else {
                    assertEquals(contextId + "agentName" + resetCount, job.getAgentName());
                }
                assertEquals(contextId, job.getContextName());
                if (job.getJob() instanceof SolrFileEventDrivenJobImpl) {
                    FileEventDrivenJob fileJob = (FileEventDrivenJob) job.getJob();
                    assertEquals(contextId + "jobNameFile" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), fileJob.getIdentifier());
                    assertEquals("cronExpression" + resetCount, fileJob.getCronExpression());
                    assertEquals("filePath" + resetCount, fileJob.getFilePath());
                }
                else if (job.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                    InternalEventDrivenJob internalEventDrivenJob = (InternalEventDrivenJob) job.getJob();
                    assertEquals(contextId + "jobNameInternal" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), internalEventDrivenJob.getIdentifier());
                    assertEquals("ls -al" + resetCount, internalEventDrivenJob.getCommandLine());
                }
                else if (job.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = (QuartzScheduleDrivenJob) job.getJob();
                    assertEquals(contextId + "jobNameQuartz" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), quartzScheduleDrivenJob.getIdentifier());
                    assertEquals("cronExpression" + resetCount, quartzScheduleDrivenJob.getCronExpression());
                }
                else if (job.getJob() instanceof SolrGlobalEventJobImpl) {
                    GlobalEventJob globalEventJob = (GlobalEventJob) job.getJob();
                    assertEquals(contextId + "jobNameGlobal" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), globalEventJob.getIdentifier());
                }
                else if (job.getJob() instanceof ContextStartJob) {
                    ContextStartJob contextStartJob = (ContextStartJob) job.getJob();
                    assertEquals(contextId + "jobNameContextStartJob" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "-" + job.getJobName(), contextStartJob.getIdentifier());
                }
                else if (job.getJob() instanceof ContextTerminalJob) {
                    ContextTerminalJob contextTerminalJob = (ContextTerminalJob) job.getJob();
                    assertEquals(contextId + "jobNameContextTerminalJob" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "-" + job.getJobName(), contextTerminalJob.getIdentifier());
                }
                resetCount++;
                if (resetCount == 3) {
                    resetCount = 0;
                }
            }
        }
    }

    private List<SchedulerJob> createListOfRecords(String contextId) {
        List<SchedulerJob> jobs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(contextId + "agentName" + i);
            solrFileEventDrivenJob.setJobName(contextId + "jobNameFile" + i);
            solrFileEventDrivenJob.setIdentifier(solrFileEventDrivenJob.getAgentName() + "_" + solrFileEventDrivenJob.getJobName());
            solrFileEventDrivenJob.setContextName(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression" + i);
            solrFileEventDrivenJob.setFilePath("filePath" + i);
            solrFileEventDrivenJob.setChildContextNames(List.of("child"));

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(contextId + "agentName" + i);
            solrInternalEventDrivenJob.setJobName(contextId + "jobNameInternal" + i);
            solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
            solrInternalEventDrivenJob.setContextName(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -al" + i);
            solrInternalEventDrivenJob.setChildContextNames(List.of("child"));

            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(contextId + "agentName" + i);
            solrQuartzScheduleDrivenJob.setJobName(contextId + "jobNameQuartz" + i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextName(contextId);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression" + i);
            solrQuartzScheduleDrivenJob.setChildContextNames(List.of("child"));

            SolrGlobalEventJobImpl solrGlobalEventJob = new SolrGlobalEventJobImpl();
            solrGlobalEventJob.setAgentName(contextId + "agentName" + i);
            solrGlobalEventJob.setJobName(contextId + "jobNameGlobal" + i);
            solrGlobalEventJob.setIdentifier(solrGlobalEventJob.getAgentName() + "_" + solrGlobalEventJob.getJobName());
            solrGlobalEventJob.setContextName(contextId);
            solrGlobalEventJob.setChildContextNames(List.of("child"));

            SolrContextStartJobImpl contextStartJob = new SolrContextStartJobImpl();
            contextStartJob.setAgentName(contextId + "agentName" + i);
            contextStartJob.setJobName(contextId + "jobNameContextStartJob" + i);
            contextStartJob.setIdentifier(contextStartJob.getAgentName() + "_" + contextStartJob.getJobName());
            contextStartJob.setContextName(contextId);
            contextStartJob.setChildContextNames(List.of("child"));

            SolrContextTerminalJobImpl contextTerminalJob = new SolrContextTerminalJobImpl();
            contextTerminalJob.setAgentName(contextId + "agentName" + i);
            contextTerminalJob.setJobName(contextId + "jobNameContextTerminalJob" + i);
            contextTerminalJob.setIdentifier(contextTerminalJob.getAgentName() + "_" + contextTerminalJob.getJobName());
            contextTerminalJob.setContextName(contextId);
            contextTerminalJob.setChildContextNames(List.of("child"));

            jobs.add(solrFileEventDrivenJob);
            jobs.add(solrInternalEventDrivenJob);
            jobs.add(solrQuartzScheduleDrivenJob);
            jobs.add(solrGlobalEventJob);
            jobs.add(contextStartJob);
            jobs.add(contextTerminalJob);
        }

        return jobs;
    }

    private List<SchedulerJob> createListOfRecords(String contextId, int count) {
        List<SchedulerJob> jobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(contextId + "agentName" + i);
            solrFileEventDrivenJob.setJobName(contextId + "jobNameFile" + i);
            solrFileEventDrivenJob.setIdentifier(solrFileEventDrivenJob.getAgentName() + "_" + solrFileEventDrivenJob.getJobName());
            solrFileEventDrivenJob.setContextName(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression" + i);
            solrFileEventDrivenJob.setFilePath("filePath" + i);
            solrFileEventDrivenJob.setChildContextNames(List.of("child1", "child2", "child3"));

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(contextId + "agentName" + i);
            solrInternalEventDrivenJob.setJobName(contextId + "jobNameInternal" + i);
            solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
            solrInternalEventDrivenJob.setContextName(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -al" + i);
            solrInternalEventDrivenJob.setChildContextNames(List.of("child", "child2", "child3"));

            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(contextId + "agentName" + i);
            solrQuartzScheduleDrivenJob.setJobName(contextId + "jobNameQuartz" + i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextName(contextId);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression" + i);
            solrQuartzScheduleDrivenJob.setChildContextNames(List.of("child", "child2", "child3"));

            jobs.add(solrFileEventDrivenJob);
            jobs.add(solrInternalEventDrivenJob);
            jobs.add(solrQuartzScheduleDrivenJob);
        }

        return jobs;
    }
}