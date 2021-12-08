package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobRecordImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

public class SolrFileEventDrivenJobRecordDaoImplTest extends SolrTestCaseJ4 {

    private SolrFileEventDrivenJobRecordDaoImpl dao;

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

        dao = new SolrFileEventDrivenJobRecordDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.insertRecords("id", 1, "contextId");

            FileEventDrivenJobRecord found = this.dao.findById("id0");

            Assert.assertEquals("id0", found.getId());
            Assert.assertEquals("agentName0", found.getAgentName());
            Assert.assertEquals("jobName0", found.getJobName());
            Assert.assertEquals("contextId", found.getContextId());
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


    private void insertRecords(String idPrefix, int num, String contextId) {
        IntStream.range(0, num).forEach(i -> {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName("agentName"+i);
            solrFileEventDrivenJob.setJobName("jobName"+i);
            solrFileEventDrivenJob.setContextId(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression");
            solrFileEventDrivenJob.setFilePath("filePath");

            SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
            solrFileEventDrivenJobRecord.setId(idPrefix+i);
            solrFileEventDrivenJobRecord.setAgentName("agentName"+i);
            solrFileEventDrivenJobRecord.setJobName("jobName"+i);
            solrFileEventDrivenJobRecord.setContextId(contextId);
            solrFileEventDrivenJobRecord.setTimestamp(1000000L);
            try {
                solrFileEventDrivenJobRecord.setFileEventDrivenJob(solrFileEventDrivenJob);
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            this.dao.save(solrFileEventDrivenJobRecord);
        });
    }
    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
