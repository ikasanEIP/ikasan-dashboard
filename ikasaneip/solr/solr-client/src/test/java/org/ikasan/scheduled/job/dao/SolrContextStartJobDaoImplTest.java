package org.ikasan.scheduled.job.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.job.model.SolrContextStartJobImpl;
import org.ikasan.scheduled.job.model.SolrContextStartJobRecordImpl;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.ContextStartJobRecord;
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
import java.util.stream.IntStream;

public class SolrContextStartJobDaoImplTest extends SolrTestCaseJ4 {

    SolrContextStartJobDaoImpl dao;

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

        dao = new SolrContextStartJobDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.insertRecords("id", 1, "contextId");

            ContextStartJobRecord found = this.dao.findById("CONTEXT_START_JOB_jobName0_contextId");

            Assert.assertEquals("CONTEXT_START_JOB_jobName0_contextId", found.getId());
            Assert.assertEquals("CONTEXT_START_JOB", found.getAgentName());
            Assert.assertEquals("jobName0", found.getJobName());
            Assert.assertEquals("contextId", found.getContextName());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_save_and_find_success_save_as_list() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.insertRecordsAsList("id", 1, "contextId");

            ContextStartJobRecord found = this.dao.findById("CONTEXT_START_JOB_jobName0_contextId");

            Assert.assertEquals("CONTEXT_START_JOB_jobName0_contextId", found.getId());
            Assert.assertEquals("CONTEXT_START_JOB", found.getAgentName());
            Assert.assertEquals("jobName0", found.getJobName());
            Assert.assertEquals("contextId", found.getContextName());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_find_by_context() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.insertRecords("id", 100, "contextId");
            this.insertRecords("idd", 1000, "context2Id");
            this.insertRecords("iddd", 267, "context3Id");

            Assert.assertEquals(10, this.dao.findByContext("contextId", 10, 0).getResultList().size());
            Assert.assertEquals(100, this.dao.findByContext("contextId", 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(100, this.dao.findByContext("context2Id", 100, 0).getResultList().size());
            Assert.assertEquals(1000, this.dao.findByContext("context2Id", 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(50, this.dao.findByContext("context3Id", 50, 0).getResultList().size());
            Assert.assertEquals(267, this.dao.findByContext("context3Id", 10, 0).getTotalNumberOfResults());
        }
    }

    @Test
    public void test_find_by_context_save_as_list() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.insertRecordsAsList("id", 100, "contextId");
            this.insertRecordsAsList("idd", 1000, "context2Id");
            this.insertRecordsAsList("iddd", 267, "context3Id");

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

            this.insertRecords("id", 100, "contextId");
            this.insertRecords("idd", 1000, "context2Id");
            this.insertRecords("iddd", 267, "context3Id");

            Assert.assertEquals(10, this.dao.findAll(10, 0).getResultList().size());
            Assert.assertEquals(1367, this.dao.findAll( 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(100, this.dao.findAll(100, 0).getResultList().size());
            Assert.assertEquals(1367, this.dao.findAll( 10, 0).getTotalNumberOfResults());
        }
    }

    @Test
    public void test_find_all_save_as_list() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            this.insertRecordsAsList("id", 100, "contextId");
            this.insertRecordsAsList("idd", 1000, "context2Id");
            this.insertRecordsAsList("iddd", 267, "context3Id");

            Assert.assertEquals(10, this.dao.findAll(10, 0).getResultList().size());
            Assert.assertEquals(1367, this.dao.findAll( 10, 0).getTotalNumberOfResults());
            Assert.assertEquals(100, this.dao.findAll(100, 0).getResultList().size());
            Assert.assertEquals(1367, this.dao.findAll( 10, 0).getTotalNumberOfResults());
        }
    }

    private void insertRecords(String idPrefix, int num, String contextId) {
        IntStream.range(0, num).forEach(i -> {
            ContextStartJob solrContextStartJob = new SolrContextStartJobImpl();
            solrContextStartJob.setAgentName(idPrefix+"agentName"+i);
            solrContextStartJob.setJobName("jobName"+i);
            solrContextStartJob.setContextName(contextId);

            ContextStartJobRecord solrContextStartJobRecord = new SolrContextStartJobRecordImpl();
            solrContextStartJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrContextStartJobRecord.setJobName("jobName"+i);
            solrContextStartJobRecord.setContextName(contextId);
            solrContextStartJobRecord.setTimestamp(1000000L);
            solrContextStartJobRecord.setContextStartJob(solrContextStartJob);


            this.dao.save(solrContextStartJobRecord);
        });
    }

    private void insertRecordsAsList(String idPrefix, int num, String contextId) {
        List<ContextStartJobRecord> records = new ArrayList<>();
        IntStream.range(0, num).forEach(i -> {
            ContextStartJob solrContextStartJob = new SolrContextStartJobImpl();
            solrContextStartJob.setAgentName(idPrefix+"agentName"+i);
            solrContextStartJob.setJobName("jobName"+i);
            solrContextStartJob.setContextName(contextId);

            ContextStartJobRecord solrContextStartJobRecord = new SolrContextStartJobRecordImpl();
            solrContextStartJobRecord.setAgentName(idPrefix+"agentName"+i);
            solrContextStartJobRecord.setJobName("jobName"+i);
            solrContextStartJobRecord.setContextName(contextId);
            solrContextStartJobRecord.setTimestamp(1000000L);
            solrContextStartJobRecord.setContextStartJob(solrContextStartJob);

            records.add(solrContextStartJobRecord);
        });

        this.dao.save(records);
    }
    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
