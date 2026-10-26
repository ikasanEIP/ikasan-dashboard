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
import java.util.List;

public class SolrInternalEventDrivenJobDaoImplTest extends SolrTestCaseJ4 {

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
            solrInternalEventDrivenJob.setContextName("contextId");
            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName("agentName");
            solrInternalEventDrivenJobRecord.setJobName("jobName");
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);

            this.dao.save(solrInternalEventDrivenJobRecord);

            InternalEventDrivenJobRecord found = this.dao.findById("internalEventDrivenJob_agentName_jobName_contextId");

            Assert.assertEquals("internalEventDrivenJob_agentName_jobName_contextId", found.getId());
            Assert.assertEquals("agentName", found.getAgentName());
            Assert.assertEquals("jobName", found.getJobName());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_find_all() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setContextName("contextId");
            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName("agentName");
            solrInternalEventDrivenJobRecord.setJobName("jobName");
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);

            this.dao.save(solrInternalEventDrivenJobRecord);

            solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setContextName("contextId");
            solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName("agentName");
            solrInternalEventDrivenJobRecord.setJobName("jobName2");
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);

            this.dao.save(solrInternalEventDrivenJobRecord);

            List<InternalEventDrivenJobRecord> found = (List<InternalEventDrivenJobRecord>) this.dao
                .findAll(1000, 0).getResultList();

            Assert.assertEquals(2, found.size());
        }
    }

    @Test
    public void test_save_with_disabled_flag() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setContextName("contextId");
            SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
            solrInternalEventDrivenJobRecord.setAgentName("agentName");
            solrInternalEventDrivenJobRecord.setJobName("jobName");
            solrInternalEventDrivenJobRecord.setTimestamp(1000000L);
            solrInternalEventDrivenJobRecord.setDisabled(true);
            solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(solrInternalEventDrivenJob);

            this.dao.save(solrInternalEventDrivenJobRecord);

            InternalEventDrivenJobRecord found = this.dao.findById("internalEventDrivenJob_agentName_jobName_contextId");

            Assert.assertEquals("internalEventDrivenJob_agentName_jobName_contextId", found.getId());
            Assert.assertEquals("agentName", found.getAgentName());
            Assert.assertEquals("jobName", found.getJobName());
            Assert.assertEquals(1000000L, found.getTimestamp());
            Assert.assertTrue(found.isDisabled());
        }
    }

    @Test
    public void test_holdAll_clears_disabled_flag() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob1 = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob1.setContextName("contextId");
            solrInternalEventDrivenJob1.setChildContextNames(List.of("child1", "child2"));

            SolrInternalEventDrivenJobRecordImpl record1 = new SolrInternalEventDrivenJobRecordImpl();
            record1.setAgentName("agentName1");
            record1.setJobName("jobName1");
            record1.setTimestamp(1000000L);
            record1.setDisabled(true);
            record1.setSkipped(true);
            record1.setInternalEventDrivenJob(solrInternalEventDrivenJob1);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob2 = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob2.setContextName("contextId");
            solrInternalEventDrivenJob2.setChildContextNames(List.of("child3", "child4"));

            SolrInternalEventDrivenJobRecordImpl record2 = new SolrInternalEventDrivenJobRecordImpl();
            record2.setAgentName("agentName2");
            record2.setJobName("jobName2");
            record2.setTimestamp(1000000L);
            record2.setDisabled(true);
            record2.setSkipped(true);
            record2.setInternalEventDrivenJob(solrInternalEventDrivenJob2);

            this.dao.save(List.of(record1, record2));

            // Verify initial state
            InternalEventDrivenJobRecord found1 = this.dao.findById("internalEventDrivenJob_agentName1_jobName1_contextId");
            Assert.assertTrue(found1.isDisabled());
            Assert.assertTrue(found1.isSkipped());
            Assert.assertFalse(found1.isHeld());

            InternalEventDrivenJobRecord found2 = this.dao.findById("internalEventDrivenJob_agentName2_jobName2_contextId");
            Assert.assertTrue(found2.isDisabled());
            Assert.assertTrue(found2.isSkipped());
            Assert.assertFalse(found2.isHeld());

            // Execute holdAll
            this.dao.holdAll(List.of(found1, found2), "testActor");

            // Verify state after holdAll
            found1 = this.dao.findById("internalEventDrivenJob_agentName1_jobName1_contextId");
            Assert.assertFalse(found1.isDisabled());
            Assert.assertFalse(found1.isSkipped());
            Assert.assertTrue(found1.isHeld());

            found2 = this.dao.findById("internalEventDrivenJob_agentName2_jobName2_contextId");
            Assert.assertFalse(found2.isDisabled());
            Assert.assertFalse(found2.isSkipped());
            Assert.assertTrue(found2.isHeld());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
