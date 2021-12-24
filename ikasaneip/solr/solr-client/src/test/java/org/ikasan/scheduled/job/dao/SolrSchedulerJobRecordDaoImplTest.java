package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

public class SolrSchedulerJobRecordDaoImplTest extends SolrTestCaseJ4 {

    private SolrSchedulerJobRecordDaoImpl dao;
    private SolrFileEventDrivenJobRecordDaoImpl solrFileEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobRecordDaoImpl solrQuartzScheduleDrivenJobRecordDao;
    private SolrInternalEventDrivenJobRecordDaoImpl solrInternalEventDrivenJobRecordDao;

    private NodeConfig config;

    private Path tmppath;

    @Before
    public void setup()
    {
        tmppath = createTempDir();

        SolrResourceLoader loader = new SolrResourceLoader(tmppath);
        config = new NodeConfig.NodeConfigBuilder("testnode", loader)
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

        this.dao = new SolrSchedulerJobRecordDaoImpl();
        this.dao.setSolrClient(server);
        this.solrFileEventDrivenJobRecordDao = new SolrFileEventDrivenJobRecordDaoImpl();
        this.solrFileEventDrivenJobRecordDao.setSolrClient(server);
        this.solrQuartzScheduleDrivenJobRecordDao = new SolrQuartzScheduleDrivenJobRecordDaoImpl();
        this.solrQuartzScheduleDrivenJobRecordDao.setSolrClient(server);
        this.solrInternalEventDrivenJobRecordDao = new SolrInternalEventDrivenJobRecordDaoImpl();
        this.solrInternalEventDrivenJobRecordDao.setSolrClient(server);
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

            Assert.assertEquals(10, this.dao.findAll(10, 0).getResultList().size());
            Assert.assertEquals(4101, this.dao.findAll( 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(100, this.dao.findAll(100, 0).getResultList().size());
            Assert.assertEquals(4101, this.dao.findAll( 10, 0).getTotalNumberOfResults());
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

            SchedulerJobRecord solrSchedulerJobRecord = this.dao.findById("fileEventDrivenJob_iddagentName100_jobName100");
            SchedulerJob job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrFileEventDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findById("quartzScheduleDrivenJob_iddqagentName100_jobName100");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrQuartzScheduleDrivenJobImpl);

            solrSchedulerJobRecord = this.dao.findById("internalEventDrivenJob_iddiagentName100_jobName100");
            job = solrSchedulerJobRecord.getJob();

            Assert.assertTrue(job instanceof SolrInternalEventDrivenJobImpl);
        }
    }


    private void insertFileEventRecords(String idPrefix, int num, String contextId) {
        IntStream.range(0, num).forEach(i -> {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(idPrefix+"agentName"+i);
            solrFileEventDrivenJob.setJobName("jobName"+i);
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
            solrQuartzScheduleDrivenJob.setAgentName("agentName"+i);
            solrQuartzScheduleDrivenJob.setJobName("jobName"+i);
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
            solrInternalEventDrivenJob.setAgentName("agentName"+i);
            solrInternalEventDrivenJob.setJobName("jobName"+i);
            solrInternalEventDrivenJob.setContextId(contextId);

            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrInternalEventDrivenJobRecord.setJobName("jobName"+i);
            solrInternalEventDrivenJobRecord.setContextId(contextId);
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);


            this.solrInternalEventDrivenJobRecordDao.save(solrInternalEventDrivenJobRecord);
        });
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
