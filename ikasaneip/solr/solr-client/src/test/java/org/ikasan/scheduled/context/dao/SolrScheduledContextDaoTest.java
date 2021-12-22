package org.ikasan.scheduled.context.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl.SCHEDULED_CONTEXT;

public class SolrScheduledContextDaoTest extends SolrTestCaseJ4 {

    private SolrScheduledContextDaoImpl dao;

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

        dao = new SolrScheduledContextDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl("id"
                , "contextName", "context", 1000000L);
            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findById("id-" + SCHEDULED_CONTEXT);

            Assert.assertEquals("id-" + SCHEDULED_CONTEXT, found.getId());
            Assert.assertEquals("contextName", found.getContextName());
//            Assert.assertEquals("context", found.getContext());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_find_all() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl("id"
                , "contextName", "context", 1000000L);
            this.dao.save(scheduledContextRecord);

            scheduledContextRecord = new SolrScheduledContextRecordImpl("id2"
                , "contextName", "context", 1000000L);
            this.dao.save(scheduledContextRecord);

            List<ScheduledContextRecord> found = (List<ScheduledContextRecord>) this.dao.findAll();

            Assert.assertEquals(2, found.size());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
