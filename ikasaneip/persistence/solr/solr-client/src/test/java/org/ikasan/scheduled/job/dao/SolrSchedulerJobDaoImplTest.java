package org.ikasan.scheduled.job.dao;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class SolrSchedulerJobDaoImplTest extends SolrTestCaseJ4 {

    private SolrSchedulerJobDaoImpl dao;
    private SolrFileEventDrivenJobDaoImpl solrFileEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobDaoImpl solrQuartzScheduleDrivenJobRecordDao;
    private SolrInternalEventDrivenJobDaoImpl solrInternalEventDrivenJobRecordDao;
    private SolrInternalEventDrivenJobTemplateDaoImpl solrInternalEventDrivenJobTemplateRecordDao;
    private SolrGlobalEventJobDaoImpl solrGlobalEventJobRecordDao;

    private NodeConfig config;

    private Path tmppath;

    @Before
    public void setup()
    {
        tmppath = createTempDir();

        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();

    }

    @After
    public void teardown() throws IOException
    {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException
    {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        this.dao = new SolrSchedulerJobDaoImpl();
        this.dao.setSolrClient(server);
        this.solrFileEventDrivenJobRecordDao = new SolrFileEventDrivenJobDaoImpl();
        this.solrFileEventDrivenJobRecordDao.setSolrClient(server);
        this.solrQuartzScheduleDrivenJobRecordDao = new SolrQuartzScheduleDrivenJobDaoImpl();
        this.solrQuartzScheduleDrivenJobRecordDao.setSolrClient(server);
        this.solrInternalEventDrivenJobRecordDao = new SolrInternalEventDrivenJobDaoImpl();
        this.solrInternalEventDrivenJobRecordDao.setSolrClient(server);
        this.solrInternalEventDrivenJobTemplateRecordDao = new SolrInternalEventDrivenJobTemplateDaoImpl();
        this.solrInternalEventDrivenJobTemplateRecordDao.setSolrClient(server);
        this.solrGlobalEventJobRecordDao = new SolrGlobalEventJobDaoImpl();
        this.solrGlobalEventJobRecordDao.setSolrClient(server);
    }


    @Test
    public void test_find_by_context() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.insertFileEventRecords("id", 100, "contextId");
            this.insertFileEventRecords("idd", 1000, "context2Id");
            this.insertFileEventRecords("iddd", 267, "context3Id");

            Assert.assertEquals(10, this.dao.findByContext("contextId", 10, 0).getResultList().size());
            Assert.assertEquals(100, this.dao.findByContext("contextId", 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(100, this.dao.findByContext("context2Id", 100, 0).getResultList().size());
            Assert.assertEquals(1000, this.dao.findByContext("context2Id", 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(50, this.dao.findByContext("context3Id", 50, 0).getResultList().size());
            Assert.assertEquals(267, this.dao.findByContext("context3Id", 10, 0).getTotalNumberOfResults());
        }
    }

    @Test
    public void test_find_all() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            this.insertFileEventRecords("id", 100, "contextId");
            this.insertFileEventRecords("idd", 1000, "context2Id");
            this.insertFileEventRecords("iddd", 267, "context3Id");

            this.insertQuartzScheduleEventRecords("idq", 100, "contextId");
            this.insertQuartzScheduleEventRecords("iddq", 1000, "context2Id");
            this.insertQuartzScheduleEventRecords("idddq", 267, "context3Id");

            this.insertInternalEventDrivenRecords("idi", 100, "contextId");
            this.insertInternalEventDrivenRecords("iddi", 1000, "context2Id");
            this.insertInternalEventDrivenRecords("idddi", 267, "context3Id");

            this.insertGlobalEventRecords("idg", 100, "contextId");
            this.insertGlobalEventRecords("iddg", 1000, "context2Id");
            this.insertGlobalEventRecords("idddg", 267, "context3Id");

            Assert.assertEquals(10, this.dao.findAll(10, 0).getResultList().size());
            Assert.assertEquals(5468, this.dao.findAll( 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(100, this.dao.findAll(100, 0).getResultList().size());
            Assert.assertEquals(5468, this.dao.findAll( 10, 0).getTotalNumberOfResults());
        }
    }

    @Test
    public void test_find_by_id() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            this.insertFileEventRecords("id", 100, "contextId");
            this.insertFileEventRecords("idd", 1000, "context2Id");
            this.insertFileEventRecords("iddd", 267, "context3Id");

            this.insertQuartzScheduleEventRecords("idq", 100, "contextId");
            this.insertQuartzScheduleEventRecords("iddq", 1000, "context2Id");
            this.insertQuartzScheduleEventRecords("idddq", 267, "context3Id");

            this.insertInternalEventDrivenRecords("idi", 100, "contextId");
            this.insertInternalEventDrivenRecords("iddi", 1000, "context2Id");
            this.insertInternalEventDrivenRecords("idddi", 267, "context3Id");

            this.insertGlobalEventRecords("idg", 100, "contextId");
            this.insertGlobalEventRecords("iddg", 1000, "context2Id");
            this.insertGlobalEventRecords("idddg", 267, "context3Id");

            SchedulerJobRecord solrSchedulerJobRecord = this.dao.findById("fileEventDrivenJob_iddagentName100_jobName100_context2Id");
            SchedulerJob job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrFileEventDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findById("quartzScheduleDrivenJob_iddqagentName100_jobName100_context2Id");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrQuartzScheduleDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findById("internalEventDrivenJob_iddiagentName100_jobName100_context2Id");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrInternalEventDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findById("globalEventJob_iddgagentName100_jobName100_context2Id");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrGlobalEventJobImpl);
        }
    }

    @Test
    public void test_find_by_context_and_job_name() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            this.insertFileEventRecords("id", 100, "contextIdf");
            this.insertFileEventRecords("idd", 1000, "context2Idf");
            this.insertFileEventRecords("iddd", 267, "context3Idf");

            this.insertQuartzScheduleEventRecords("idq", 100, "contextIdq");
            this.insertQuartzScheduleEventRecords("iddq", 1000, "context2Idq");
            this.insertQuartzScheduleEventRecords("idddq", 267, "context3Idq");

            this.insertInternalEventDrivenRecords("idi", 100, "contextIdi");
            this.insertInternalEventDrivenRecords("iddi", 1000, "context2Idi");
            this.insertInternalEventDrivenRecords("idddi", 267, "context3Idi");

            this.insertGlobalEventRecords("idg", 100, "contextIdg");
            this.insertGlobalEventRecords("iddg", 1000, "context2Idg");
            this.insertGlobalEventRecords("idddg", 267, "context3Idg");

            SchedulerJobRecord solrSchedulerJobRecord = this.dao.findByContextIdAndJobName("context2Idf", "jobName100");
            SchedulerJob job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrFileEventDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findByContextIdAndJobName("context2Idq", "jobName100");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrQuartzScheduleDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findByContextIdAndJobName("context2Idi", "jobName100");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrInternalEventDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findByContextIdAndJobName("context2Idg", "jobName100");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrGlobalEventJobImpl);
        }
    }

    @Test
    public void test_findByAgent() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String agentName = "testAgent";
            String contextId = "testContext";
            int numRecords = 5;

            this.insertFileEventRecords(agentName, numRecords, contextId);
            server.commit();

            SearchResults<SchedulerJobRecord> results = this.dao.findByAgent("testAgentagentName0", 10, 0);

            Assert.assertNotNull(results);
            Assert.assertEquals(1, results.getResultList().size());
            Assert.assertEquals(1, results.getTotalNumberOfResults());
            results.getResultList().forEach(record -> {
                Assert.assertTrue(record.getAgentName().startsWith("testAgentagentName0"));
            });
        }
    }

    @Test
    public void test_findByFilter_byJobType() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String contextId = "jobTypeContext";
            this.insertFileEventRecords("fileAgent", 3, contextId);
            this.insertQuartzScheduleEventRecords("quartzAgent", 2, contextId);
            this.insertInternalEventDrivenRecords("internalAgent", 4, contextId);
            this.insertGlobalEventRecords("globalAgent", 1, contextId);
            server.commit();

            // Test filtering by FileEventDrivenJob
            SolrSchedulerJobSearchFilterImpl filterFile = new SolrSchedulerJobSearchFilterImpl();
            filterFile.setJobTypeFilter(JobConstants.FILE_EVENT_DRIVEN_JOB);
            SearchResults<SchedulerJobRecord> resultsFile = this.dao.findByFilter(filterFile, 10, 0, null, null);
            Assert.assertNotNull(resultsFile);
            Assert.assertEquals(3, resultsFile.getResultList().size());
            resultsFile.getResultList().forEach(record -> Assert.assertTrue(record.getJob() instanceof FileEventDrivenJob));

            // Test filtering by QuartzScheduleDrivenJob
            SolrSchedulerJobSearchFilterImpl filterQuartz = new SolrSchedulerJobSearchFilterImpl();
            filterQuartz.setJobTypeFilter(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);
            SearchResults<SchedulerJobRecord> resultsQuartz = this.dao.findByFilter(filterQuartz, 10, 0, null, null);
            Assert.assertNotNull(resultsQuartz);
            Assert.assertEquals(2, resultsQuartz.getResultList().size());
            resultsQuartz.getResultList().forEach(record -> Assert.assertTrue(record.getJob() instanceof QuartzScheduleDrivenJob));

            // Test filtering by InternalEventDrivenJob
            SolrSchedulerJobSearchFilterImpl filterInternal = new SolrSchedulerJobSearchFilterImpl();
            filterInternal.setJobTypeFilter(JobConstants.INTERNAL_EVENT_DRIVEN_JOB);
            SearchResults<SchedulerJobRecord> resultsInternal = this.dao.findByFilter(filterInternal, 10, 0, null, null);
            Assert.assertNotNull(resultsInternal);
            Assert.assertEquals(4, resultsInternal.getResultList().size());
            resultsInternal.getResultList().forEach(record -> Assert.assertTrue(record.getJob() instanceof InternalEventDrivenJob));

            // Test filtering by GlobalEventJob
            SolrSchedulerJobSearchFilterImpl filterGlobal = new SolrSchedulerJobSearchFilterImpl();
            filterGlobal.setJobTypeFilter(JobConstants.GLOBAL_EVENT_JOB);
            SearchResults<SchedulerJobRecord> resultsGlobal = this.dao.findByFilter(filterGlobal, 10, 0, null, null);
            Assert.assertNotNull(resultsGlobal);
            Assert.assertEquals(1, resultsGlobal.getResultList().size());
            resultsGlobal.getResultList().forEach(record -> Assert.assertTrue(record.getJob() instanceof GlobalEventJob));
        }
    }

    @Test
    public void test_findByFilter_byJobTypesList() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String contextId = "jobTypesListContext";
            this.insertFileEventRecords("fileAgentList", 3, contextId);
            this.insertQuartzScheduleEventRecords("quartzAgentList", 2, contextId);
            this.insertInternalEventDrivenRecords("internalAgentList", 4, contextId);
            this.insertGlobalEventRecords("globalAgentList", 1, contextId);
            server.commit();

            // Test filtering by a list of job types: FileEventDrivenJob and QuartzScheduleDrivenJob
            SolrSchedulerJobSearchFilterImpl filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobTypes(List.of(JobConstants.FILE_EVENT_DRIVEN_JOB, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB));
            SearchResults<SchedulerJobRecord> results = this.dao.findByFilter(filter, 10, 0, null, null);

            Assert.assertNotNull(results);
            Assert.assertEquals(5, results.getResultList().size()); // 3 FileEventDriven + 2 QuartzScheduleDriven
            results.getResultList().forEach(record -> {
                Assert.assertTrue(record.getJob() instanceof FileEventDrivenJob || record.getJob() instanceof QuartzScheduleDrivenJob);
            });

            // Test filtering by a list including a non-existent job type
            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobTypes(List.of(JobConstants.FILE_EVENT_DRIVEN_JOB, JobConstants.GLOBAL_EVENT_JOB));
            results = this.dao.findByFilter(filter, 10, 0, null, null);
            Assert.assertNotNull(results);
            Assert.assertEquals(4, results.getResultList().size()); // Only FileEventDrivenJob should be found

            // Test filtering by an empty list of job types (should return all)
            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobTypes(new ArrayList<>());
            results = this.dao.findByFilter(filter, 10, 0, null, null);
            Assert.assertNotNull(results);
            Assert.assertEquals(10, results.getResultList().size()); // All 3+2+4+1 = 10 jobs
        }
    }

    @Test
    public void test_findByFilter_byTargetsResidingContext() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String contextId = "targetsResidingContext";
            String agentName = "targetsResidingAgent";
            List<InternalEventDrivenJobRecord> recordsToSave = new ArrayList<>();

            // Jobs with targetsResidingContext = true
            for (int i = 0; i < 3; i++) {
                SolrInternalEventDrivenJobImpl job = new SolrInternalEventDrivenJobImpl();
                job.setAgentName(agentName);
                job.setJobName("jobTrue" + i);
                job.setIdentifier(agentName + "_" + "jobTrue" + i);
                job.setContextName(contextId);
                job.setTargetResidingContextOnly(true);
                job.setCommandLine("cmdTrue" + i);

                SolrInternalEventDrivenJobRecordImpl record = new SolrInternalEventDrivenJobRecordImpl();
                record.setInternalEventDrivenJob(job);
                record.setAgentName(agentName);
                record.setJobName("jobTrue" + i);
                record.setContextName(contextId);
                record.setTimestamp(System.currentTimeMillis());
                recordsToSave.add(record);
            }

            // Jobs with targetsResidingContext = false
            for (int i = 0; i < 2; i++) {
                SolrInternalEventDrivenJobImpl job = new SolrInternalEventDrivenJobImpl();
                job.setAgentName(agentName);
                job.setJobName("jobFalse" + i);
                job.setIdentifier(agentName + "_" + "jobFalse" + i);
                job.setContextName(contextId);
                job.setTargetResidingContextOnly(false);
                job.setCommandLine("cmdFalse" + i);

                SolrInternalEventDrivenJobRecordImpl record = new SolrInternalEventDrivenJobRecordImpl();
                record.setInternalEventDrivenJob(job);
                record.setAgentName(agentName);
                record.setJobName("jobFalse" + i);
                record.setContextName(contextId);
                record.setTimestamp(System.currentTimeMillis());
                recordsToSave.add(record);
            }

            this.solrInternalEventDrivenJobRecordDao.save(recordsToSave);
            server.commit();

            // Test filtering by targetsResidingContext = true
            SolrSchedulerJobSearchFilterImpl filterTrue = new SolrSchedulerJobSearchFilterImpl();
            filterTrue.setTargetResidingContextOnly(true);
            SearchResults<SchedulerJobRecord> resultsTrue = this.dao.findByFilter(filterTrue, 10, 0, null, null);
            Assert.assertNotNull(resultsTrue);
            Assert.assertEquals(3, resultsTrue.getResultList().size());
            resultsTrue.getResultList().forEach(record -> {
                Assert.assertTrue(record.getJob() instanceof InternalEventDrivenJob);
                InternalEventDrivenJob internalJob = (InternalEventDrivenJob) record.getJob();
                Assert.assertTrue(internalJob.isTargetResidingContextOnly());
            });

            // Test filtering by targetsResidingContext = false
            SolrSchedulerJobSearchFilterImpl filterFalse = new SolrSchedulerJobSearchFilterImpl();
            filterFalse.setTargetResidingContextOnly(false);
            SearchResults<SchedulerJobRecord> resultsFalse = this.dao.findByFilter(filterFalse, 10, 0, null, null);
            Assert.assertNotNull(resultsFalse);
            Assert.assertEquals(2, resultsFalse.getResultList().size());
            resultsFalse.getResultList().forEach(record -> {
                Assert.assertTrue(record.getJob() instanceof InternalEventDrivenJob);
                InternalEventDrivenJob internalJob = (InternalEventDrivenJob) record.getJob();
                Assert.assertFalse(internalJob.isTargetResidingContextOnly());
            });
        }
    }

    @Test
    public void test_findAll_withPaginationAndMixedJobTypes() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String contextId = "mixedJobTypeContext";
            this.insertFileEventRecords("fileAgent", 5, contextId);
            this.insertQuartzScheduleEventRecords("quartzAgent", 7, contextId);
            this.insertInternalEventDrivenRecords("internalAgent", 8, contextId);
            this.insertGlobalEventRecords("globalAgent", 3, contextId);
            server.commit();

            int totalExpectedRecords = 5 + 7 + 8 + 3; // 23 records

            // Test with limit 10, offset 0
            SearchResults<SchedulerJobRecord> results1 = this.dao.findAll(10, 0);
            Assert.assertNotNull(results1);
            Assert.assertEquals(10, results1.getResultList().size());
            Assert.assertEquals(totalExpectedRecords, results1.getTotalNumberOfResults());

            // Test with limit 10, offset 10
            SearchResults<SchedulerJobRecord> results2 = this.dao.findAll(10, 10);
            Assert.assertNotNull(results2);
            Assert.assertEquals(10, results2.getResultList().size());
            Assert.assertEquals(totalExpectedRecords, results2.getTotalNumberOfResults());

            // Test with limit 10, offset 20 (remaining 3 records)
            SearchResults<SchedulerJobRecord> results3 = this.dao.findAll(10, 20);
            Assert.assertNotNull(results3);
            Assert.assertEquals(3, results3.getResultList().size());
            Assert.assertEquals(totalExpectedRecords, results3.getTotalNumberOfResults());

            // Test with limit 10, offset 30 (no records)
            SearchResults<SchedulerJobRecord> results4 = this.dao.findAll(10, 30);
            Assert.assertNotNull(results4);
            Assert.assertEquals(0, results4.getResultList().size());
            Assert.assertEquals(totalExpectedRecords, results4.getTotalNumberOfResults());
        }
    }

    @Test
    public void test_delete() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String agentName = "agentForDelete";
            String jobName = "jobForDelete";
            String contextId = "contextForDelete";

            SolrFileEventDrivenJobImpl jobToDelete = new SolrFileEventDrivenJobImpl();
            jobToDelete.setAgentName(agentName);
            jobToDelete.setJobName(jobName);
            jobToDelete.setDisplayName("displayName");
            jobToDelete.setContextName(contextId);
            jobToDelete.setCronExpression("cron");
            jobToDelete.setFilePath("path");

            SolrFileEventDrivenJobRecordImpl recordToDelete = new SolrFileEventDrivenJobRecordImpl();
            recordToDelete.setAgentName(agentName);
            recordToDelete.setJobName(jobName);
            recordToDelete.setDisplayName("displayName");
            recordToDelete.setContextName(contextId);
            recordToDelete.setTimestamp(System.currentTimeMillis());
            recordToDelete.setFileEventDrivenJob(jobToDelete);

            this.solrFileEventDrivenJobRecordDao.save(recordToDelete);
            server.commit();

            String id = JobConstants.FILE_EVENT_DRIVEN_JOB + "_" + agentName + "_" + jobName + "_" + contextId;
            SchedulerJobRecord savedRecord = this.dao.findById(id);
            Assert.assertNotNull(savedRecord);

            this.dao.delete(savedRecord);
            server.commit();

            SchedulerJobRecord deletedRecord = this.dao.findById(id);
            Assert.assertNull(deletedRecord);
        }
    }

    @Test
    public void test_deleteByAgentName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String agentNameToDelete = "agentToDelete";
            String agentToKeep = "agentToKeep";
            String contextId = "testContext";

            // Insert jobs to be deleted
            this.insertFileEventRecords(agentNameToDelete, 3, contextId, agentNameToDelete);
            this.insertInternalEventDrivenRecords(agentNameToDelete, 2, contextId, agentNameToDelete);

            // Insert jobs to be kept
            this.insertInternalEventDrivenRecords(agentToKeep, 4, contextId, agentToKeep);
            this.insertFileEventRecords(agentToKeep, 1, contextId, agentToKeep);
            server.commit();

            // Verify initial state
            SearchResults<SchedulerJobRecord> resultsToDelete = this.dao.findByAgent(agentNameToDelete, 10, 0);
            Assert.assertEquals(5, resultsToDelete.getResultList().size()); // 3 file + 2 quartz
            SearchResults<SchedulerJobRecord> resultsToKeep = this.dao.findByAgent(agentToKeep, 10, 0);
            Assert.assertEquals(5, resultsToKeep.getResultList().size()); // 4 internal + 1 global

            // Perform deletion
            this.dao.deleteByAgentName(agentNameToDelete);
            server.commit();

            // Verify state after deletion
            resultsToDelete = this.dao.findByAgent(agentNameToDelete, 10, 0);
            Assert.assertEquals(0, resultsToDelete.getResultList().size());
            resultsToKeep = this.dao.findByAgent(agentToKeep, 10, 0);
            Assert.assertEquals(5, resultsToKeep.getResultList().size());
        }
    }

    @Test
    public void test_deleteByContextName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String contextNameToDelete = "contextToDelete";
            String contextToKeep = "contextToKeep";
            String agentName = "testAgent";

            // Insert jobs to be deleted
            this.insertFileEventRecords(agentName, 3, contextNameToDelete);
            this.insertQuartzScheduleEventRecords(agentName, 2, contextNameToDelete);

            // Insert jobs to be kept
            this.insertInternalEventDrivenRecords(agentName, 4, contextToKeep);
            this.insertGlobalEventRecords(agentName, 1, contextToKeep);
            server.commit();

            // Verify initial state
            SearchResults<SchedulerJobRecord> resultsToDelete = this.dao.findByContext(contextNameToDelete, 10, 0);
            Assert.assertEquals(5, resultsToDelete.getResultList().size()); // 3 file + 2 quartz
            SearchResults<SchedulerJobRecord> resultsToKeep = this.dao.findByContext(contextToKeep, 10, 0);
            Assert.assertEquals(5, resultsToKeep.getResultList().size()); // 4 internal + 1 global

            // Perform deletion
            this.dao.deleteByContextName(contextNameToDelete);
            server.commit();

            // Verify state after deletion
            resultsToDelete = this.dao.findByContext(contextNameToDelete, 10, 0);
            Assert.assertEquals(0, resultsToDelete.getResultList().size());
            resultsToKeep = this.dao.findByContext(contextToKeep, 10, 0);
            Assert.assertEquals(5, resultsToKeep.getResultList().size());
        }
    }

    @Test
    public void test_find_by_filter() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            this.insertFileEventRecords("id", 100, "contextIdf");
            this.insertFileEventRecords("idd", 1000, "context2Idf");
            this.insertFileEventRecords("iddd", 267, "context3Idf");

            this.insertQuartzScheduleEventRecords("idq", 100, "contextIdq");
            this.insertQuartzScheduleEventRecords("iddq", 1000, "context2Idq");
            this.insertQuartzScheduleEventRecords("idddq", 267, "context3Idq");

            this.insertInternalEventDrivenRecords("idi", 100, "contextIdi");
            this.insertInternalEventDrivenRecords("iddi", 1000, "context2Idi");
            this.insertInternalEventDrivenRecords("idddi", 267, "context3Idi");

            this.insertInternalEventDrivenTemplateRecords("idit", 100, "contextIdi");
            this.insertInternalEventDrivenTemplateRecords("iddit", 1000, "context2Idi");
            this.insertInternalEventDrivenTemplateRecords("idddit", 267, "context3Idi");

            this.insertGlobalEventRecords("idg", 100, "contextIdg");
            this.insertGlobalEventRecords("iddg", 1000, "context2Idg");
            this.insertGlobalEventRecords("idddg", 267, "context3Idg");

            SolrSchedulerJobSearchFilterImpl filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobNameFilter("jobName100");
            filter.setContextSearchFilter("context2Idf");

            SearchResults<SchedulerJobRecord> solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            SchedulerJob job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrFileEventDrivenJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobNameFilter("jobName100");
            filter.setContextSearchFilter("context2Idq");

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrQuartzScheduleDrivenJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobNameFilter("jobName100");
            filter.setContextSearchFilter("context2Idi");

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrInternalEventDrivenJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setHeld(true);

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);

            Assert.assertTrue(solrSchedulerJobRecords.getTotalNumberOfResults() == 274);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setSkipped(true);

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);

            Assert.assertTrue(solrSchedulerJobRecords.getTotalNumberOfResults() == 548);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobNameFilter("jobName100");
            filter.setContextSearchFilter("context2Idg");

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrGlobalEventJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setNotJobNameInFilter(List.of("jobName100"));

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            List<SchedulerJobRecord> resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(6825, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setDisplayNameFilter("displayName");

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(6835, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setDisplayNameFilter("displayName100");

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(10, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setDisplayNameFilter("displayName100");
            filter.setJobNameFilter("jobName100");

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(10, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobTypeFilter(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE);

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(1367, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setContextNames(List.of("context2Idi"));

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(2000, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setContextNames(List.of("blah"));

            solrSchedulerJobRecords = this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(0, resultList.size());
        }
    }

    private void insertFileEventRecords(String idPrefix, int num, String contextId) {
        this.insertFileEventRecords(idPrefix, num, contextId, null);
    }

    private void insertFileEventRecords(String idPrefix, int num, String contextId, String agentName) {
        List<FileEventDrivenJobRecord> records = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            if(agentName!= null) {
                solrFileEventDrivenJob.setAgentName(agentName);
            }
            else {
                solrFileEventDrivenJob.setAgentName(idPrefix + "agentName" + i);
            }
            solrFileEventDrivenJob.setJobName("jobName"+i);
            solrFileEventDrivenJob.setDisplayName("displayName"+i);
            solrFileEventDrivenJob.setContextName(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression");
            solrFileEventDrivenJob.setFilePath("filePath");

            SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
            if(agentName!= null) {
                solrFileEventDrivenJobRecord.setAgentName(agentName);
            }
            else {
                solrFileEventDrivenJobRecord.setAgentName(idPrefix + "agentName" + i);
            }
            solrFileEventDrivenJobRecord.setJobName("jobName"+i);
            solrFileEventDrivenJobRecord.setDisplayName("displayName"+i);
            solrFileEventDrivenJobRecord.setContextName(contextId);
            solrFileEventDrivenJobRecord.setTimestamp(1000000L);
            solrFileEventDrivenJobRecord.setFileEventDrivenJob(solrFileEventDrivenJob);

            records.add(solrFileEventDrivenJobRecord);
        });

        this.solrFileEventDrivenJobRecordDao.save(records);
    }

    private void insertQuartzScheduleEventRecords(String idPrefix, int num, String contextId) {
        List<QuartzScheduleDrivenJobRecord> records = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName("agentName"+i);
            solrQuartzScheduleDrivenJob.setJobName("jobName"+i);
            solrQuartzScheduleDrivenJob.setDisplayName("displayName"+i);
            solrQuartzScheduleDrivenJob.setContextName(contextId);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression");

            SolrQuartzScheduleDrivenJobRecordImpl solrQuartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
            solrQuartzScheduleDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrQuartzScheduleDrivenJobRecord.setJobName("jobName"+i);
            solrQuartzScheduleDrivenJobRecord.setContextName(contextId);
            solrQuartzScheduleDrivenJobRecord.setTimestamp(1000000L);
            solrQuartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(solrQuartzScheduleDrivenJob);

            records.add(solrQuartzScheduleDrivenJobRecord);
        });

        this.solrQuartzScheduleDrivenJobRecordDao.save(records);
    }

    private void insertInternalEventDrivenRecords(String idPrefix, int num, String contextId) {
        this.insertInternalEventDrivenRecords(idPrefix, num, contextId, null);
    }
    private void insertInternalEventDrivenRecords(String idPrefix, int num, String contextId, String agentName) {
        List<InternalEventDrivenJobRecord> records = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName("agentName"+i);
            solrInternalEventDrivenJob.setJobName("jobName"+i);
            solrInternalEventDrivenJob.setDisplayName("displayName"+i);
            solrInternalEventDrivenJob.setContextName(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -la");

            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            if(agentName!= null) {
                solrInternalEventDrivenJobRecord.setAgentName(agentName);
            }
            else {
                solrInternalEventDrivenJobRecord.setAgentName(idPrefix + "agentName" + i);
            }
            solrInternalEventDrivenJobRecord.setJobName("jobName"+i);
            solrInternalEventDrivenJobRecord.setContextName(contextId);
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);
            solrInternalEventDrivenJobRecord.setHeld(i%10==0);
            solrInternalEventDrivenJobRecord.setSkipped(i%5==0);

            records.add(solrInternalEventDrivenJobRecord);
        });

        this.solrInternalEventDrivenJobRecordDao.save(records);
    }

    private void insertInternalEventDrivenTemplateRecords(String idPrefix, int num, String contextId) {
        List<InternalEventDrivenJobRecord> records = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName("agentName"+i);
            solrInternalEventDrivenJob.setJobName("jobName"+i);
            solrInternalEventDrivenJob.setDisplayName("displayName"+i);
            solrInternalEventDrivenJob.setContextName(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -la");

            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrInternalEventDrivenJobRecord.setJobName("jobName"+i);
            solrInternalEventDrivenJobRecord.setContextName(contextId);
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);
            solrInternalEventDrivenJobRecord.setHeld(i%10==0);
            solrInternalEventDrivenJobRecord.setSkipped(i%5==0);

            records.add(solrInternalEventDrivenJobRecord);
        });

        this.solrInternalEventDrivenJobTemplateRecordDao.save(records);
    }

    private void insertGlobalEventRecords(String idPrefix, int num, String contextId) {
        List<GlobalEventJobRecord> records = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            SolrGlobalEventJobImpl solrGlobalEventJob = new SolrGlobalEventJobImpl();
            solrGlobalEventJob.setAgentName("agentName"+i);
            solrGlobalEventJob.setJobName("jobName"+i);
            solrGlobalEventJob.setDisplayName("displayName"+i);
            solrGlobalEventJob.setContextName(contextId);

            SolrGlobalEventJobRecordImpl solrGlobalEventJobRecord = new SolrGlobalEventJobRecordImpl();
            solrGlobalEventJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrGlobalEventJobRecord.setJobName("jobName"+i);
            solrGlobalEventJobRecord.setContextName(contextId);
            solrGlobalEventJobRecord.setTimestamp(1000000L);
            solrGlobalEventJobRecord.setGlobalEventJob(solrGlobalEventJob);

            records.add(solrGlobalEventJobRecord);
        });

        this.solrGlobalEventJobRecordDao.save(records);
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
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
