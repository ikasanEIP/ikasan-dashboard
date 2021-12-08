package org.ikasan.scheduled.context.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.scheduled.context.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.spec.scheduled.context.model.ScheduledContextInstanceRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SolrScheduledContextInstanceDaoTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceDaoImpl dao;

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

        dao = new SolrScheduledContextInstanceDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl("id"
                , "contextName", "contextInstance", 1000000L);
            scheduledContextRecord.setStatus("RUNNING");
            this.dao.save(scheduledContextRecord);

            ScheduledContextInstanceRecord found = this.dao.findById("id");

            Assert.assertEquals("id", found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextInstance", found.getContextInstance());
            Assert.assertEquals("RUNNING", found.getStatus());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_status_update_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrScheduledContextInstanceRecordImpl scheduledContextInstanceRecord = new SolrScheduledContextInstanceRecordImpl("id"
                , "contextName", "contextInstance", 1000000L);
            scheduledContextInstanceRecord.setStatus("WAITING");
            this.dao.save(scheduledContextInstanceRecord);

            ScheduledContextInstanceRecord found = this.dao.findById("id");

            Assert.assertEquals("id", found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextInstance", found.getContextInstance());
            Assert.assertEquals("WAITING", found.getStatus());
            Assert.assertEquals(1000000L, found.getTimestamp());

            found.setStatus("RUNNING");

            this.dao.save(found);

            found = this.dao.findById("id");

            Assert.assertEquals("id", found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextInstance", found.getContextInstance());
            Assert.assertEquals("RUNNING", found.getStatus());
            Assert.assertEquals(1000000L, found.getTimestamp());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
