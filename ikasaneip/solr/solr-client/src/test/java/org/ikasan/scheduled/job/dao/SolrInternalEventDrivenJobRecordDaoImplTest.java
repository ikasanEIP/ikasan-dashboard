package org.ikasan.scheduled.job.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobRecordImpl;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SolrInternalEventDrivenJobRecordDaoImplTest extends SolrTestCaseJ4 {

    private SolrInternalEventDrivenJobDaoImpl dao;

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

        dao = new SolrInternalEventDrivenJobDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            SolrInternalEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrFileEventDrivenJobRecord.setAgentName("agentName");
            solrFileEventDrivenJobRecord.setJobName("jobName");
            solrFileEventDrivenJobRecord.setTimestamp(1000000L);
            solrFileEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);

            this.dao.save(solrFileEventDrivenJobRecord);

            InternalEventDrivenJobRecord found = this.dao.findById("internalEventDrivenJob_agentName_jobName");

            Assert.assertEquals("internalEventDrivenJob_agentName_jobName", found.getId());
            Assert.assertEquals("agentName", found.getAgentName());
            Assert.assertEquals("jobName", found.getJobName());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

//    @Test
//    public void test_find_all() throws Exception {
//
//        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
//        {
//            init(server);
//
//            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl("id"
//                , "contextName", "context", 1000000L);
//            this.dao.save(scheduledContextRecord);
//
//            scheduledContextRecord = new SolrScheduledContextRecordImpl("id2"
//                , "contextName", "context", 1000000L);
//            this.dao.save(scheduledContextRecord);
//
//            List<ScheduledContextRecord> found = (List<ScheduledContextRecord>) this.dao.findAll();
//
//            Assert.assertEquals(2, found.size());
//        }
//    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
