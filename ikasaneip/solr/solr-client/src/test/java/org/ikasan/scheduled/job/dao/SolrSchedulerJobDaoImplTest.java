package org.ikasan.scheduled.job.dao;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
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

            SearchResults<SolrSchedulerJobRecordImpl> solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            SchedulerJob job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrFileEventDrivenJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobNameFilter("jobName100");
            filter.setContextSearchFilter("context2Idq");

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrQuartzScheduleDrivenJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobNameFilter("jobName100");
            filter.setContextSearchFilter("context2Idi");

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrInternalEventDrivenJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setHeld(true);

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);

            Assert.assertTrue(solrSchedulerJobRecords.getTotalNumberOfResults() == 274);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setSkipped(true);

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);

            Assert.assertTrue(solrSchedulerJobRecords.getTotalNumberOfResults() == 548);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobNameFilter("jobName100");
            filter.setContextSearchFilter("context2Idg");

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            job = solrSchedulerJobRecords.getResultList().get(0).getJob();

            Assert.assertTrue(job instanceof SolrGlobalEventJobImpl);

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setNotJobNameInFilter(List.of("jobName100"));

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            List<SolrSchedulerJobRecordImpl> resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(6825, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setDisplayNameFilter("displayName");

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(6835, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setDisplayNameFilter("displayName100");

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(10, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setDisplayNameFilter("displayName100");
            filter.setJobNameFilter("jobName100");

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(10, resultList.size());

            filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobTypeFilter(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE);

            solrSchedulerJobRecords = (SearchResults<SolrSchedulerJobRecordImpl>) this.dao.findByFilter(filter, -1, -1, null, null);
            resultList = solrSchedulerJobRecords.getResultList();

            Assert.assertEquals(1367, resultList.size());
        }
    }


    private void insertFileEventRecords(String idPrefix, int num, String contextId) {
        IntStream.range(0, num).forEach(i -> {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrFileEventDrivenJob.setJobName("jobName"+i);
            solrFileEventDrivenJob.setDisplayName("displayName"+i);
            solrFileEventDrivenJob.setContextName(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression");
            solrFileEventDrivenJob.setFilePath("filePath");

            SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
            solrFileEventDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrFileEventDrivenJobRecord.setJobName("jobName"+i);
            solrFileEventDrivenJobRecord.setDisplayName("displayName"+i);
            solrFileEventDrivenJobRecord.setContextName(contextId);
            solrFileEventDrivenJobRecord.setTimestamp(1000000L);
            solrFileEventDrivenJobRecord.setFileEventDrivenJob(solrFileEventDrivenJob);

            this.solrFileEventDrivenJobRecordDao.save(solrFileEventDrivenJobRecord);
        });
    }

    private void insertQuartzScheduleEventRecords(String idPrefix, int num, String contextId) {
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


            this.solrQuartzScheduleDrivenJobRecordDao.save(solrQuartzScheduleDrivenJobRecord);
        });
    }

    private void insertInternalEventDrivenRecords(String idPrefix, int num, String contextId) {
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

            this.solrInternalEventDrivenJobRecordDao.save(solrInternalEventDrivenJobRecord);
        });
    }

    private void insertInternalEventDrivenTemplateRecords(String idPrefix, int num, String contextId) {
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

            this.solrInternalEventDrivenJobTemplateRecordDao.save(solrInternalEventDrivenJobRecord);
        });
    }

    private void insertGlobalEventRecords(String idPrefix, int num, String contextId) {
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

            this.solrGlobalEventJobRecordDao.save(solrGlobalEventJobRecord);
        });
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
