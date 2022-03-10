package org.ikasan.scheduled.job.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SolrQuartzScheduleEventDrivenJobRecordDaoImplTest extends SolrTestCaseJ4 {

    private SolrQuartzScheduleDrivenJobDaoImpl dao;

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

        dao = new SolrQuartzScheduleDrivenJobDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setContextId("contextId");
            SolrQuartzScheduleDrivenJobRecordImpl solrQuartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
            solrQuartzScheduleDrivenJobRecord.setAgentName("agentName");
            solrQuartzScheduleDrivenJobRecord.setJobName("jobName");
            solrQuartzScheduleDrivenJobRecord.setTimestamp(1000000L);
            solrQuartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(solrQuartzScheduleDrivenJob);

            this.dao.save(solrQuartzScheduleDrivenJobRecord);

            QuartzScheduleDrivenJobRecord found = this.dao.findById("quartzScheduleDrivenJob_agentName_jobName_contextId");

            Assert.assertEquals("quartzScheduleDrivenJob_agentName_jobName_contextId", found.getId());
            Assert.assertEquals("agentName", found.getAgentName());
            Assert.assertEquals("jobName", found.getJobName());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }


    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
