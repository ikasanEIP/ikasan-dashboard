package org.ikasan.scheduled.context.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.scheduled.context.model.SolrContextTemplateImpl;
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

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);

            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findById("contextName-" + SCHEDULED_CONTEXT);

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT, found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextName", found.getContext().getName());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_find_all() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName1");
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName1");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
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
