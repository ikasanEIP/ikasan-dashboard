package org.ikasan.scheduled.context.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.context.model.SolrScheduledContextViewRecordImpl;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SolrScheduledContextViewDaoImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextViewDaoImpl dao;

    private NodeConfig config;

    private Path tmppath;

    @Before
    public void setup() {
        tmppath = createTempDir();

        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();
    }

    @After
    public void teardown() throws IOException {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrScheduledContextViewDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_get_context_view() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrScheduledContextViewRecordImpl record = new SolrScheduledContextViewRecordImpl();
            record.setParentContextName("parentContext1");
            record.setContextName("childContext1");
            record.setContextView("{ \"view\": \"data\" }");
            record.setTimestamp(1000000L);
            record.setModifiedBy("testUser");

            this.dao.save(record);

            ScheduledContextViewRecord found = this.dao.getContextView("parentContext1", "childContext1");

            Assert.assertNotNull(found);
            Assert.assertEquals("parentContext1", found.getParentContextName());
            Assert.assertEquals("childContext1", found.getContextName());
            Assert.assertEquals("{ \"view\": \"data\" }", found.getContextView());
            Assert.assertEquals("testUser", found.getModifiedBy());
            Assert.assertEquals(1000000L, found.getTimestamp());
        }
    }

    @Test
    public void test_get_context_view_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrScheduledContextViewRecordImpl record = new SolrScheduledContextViewRecordImpl();
            record.setParentContextName("parentContext1");
            record.setContextName("childContext1");
            record.setContextView("{ \"view\": \"data\" }");
            record.setTimestamp(1000000L);
            record.setModifiedBy("testUser");

            this.dao.save(record);

            ScheduledContextViewRecord found = this.dao.getContextView("nonExistent", "childContext1");

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_update_context_view() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrScheduledContextViewRecordImpl record = new SolrScheduledContextViewRecordImpl();
            record.setParentContextName("parentContext1");
            record.setContextName("childContext1");
            record.setContextView("{ \"view\": \"data\" }");
            record.setTimestamp(1000000L);
            record.setModifiedBy("testUser");

            this.dao.save(record);

            ScheduledContextViewRecord found = this.dao.getContextView("parentContext1", "childContext1");
            Assert.assertEquals("{ \"view\": \"data\" }", found.getContextView());

            // Update the record
            record.setContextView("{ \"view\": \"updated data\" }");
            record.setModifiedBy("anotherUser");
            this.dao.save(record);

            found = this.dao.getContextView("parentContext1", "childContext1");

            Assert.assertNotNull(found);
            Assert.assertEquals("{ \"view\": \"updated data\" }", found.getContextView());
            Assert.assertEquals("anotherUser", found.getModifiedBy());
        }
    }

    @Test
    public void test_save_multiple_context_views() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 5; i++) {
                SolrScheduledContextViewRecordImpl record = new SolrScheduledContextViewRecordImpl();
                record.setParentContextName("parentContext" + i);
                record.setContextName("childContext" + i);
                record.setContextView("{ \"view\": \"data" + i + "\" }");
                record.setTimestamp(1000000L + i);
                record.setModifiedBy("user" + i);

                this.dao.save(record);
            }

            // Verify each context view can be retrieved
            for (int i = 0; i < 5; i++) {
                ScheduledContextViewRecord found = this.dao.getContextView("parentContext" + i, "childContext" + i);

                Assert.assertNotNull(found);
                Assert.assertEquals("parentContext" + i, found.getParentContextName());
                Assert.assertEquals("childContext" + i, found.getContextName());
                Assert.assertEquals("{ \"view\": \"data" + i + "\" }", found.getContextView());
            }
        }
    }

    @Test
    public void test_save_same_child_context_different_parents() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Save same child context under different parents
            SolrScheduledContextViewRecordImpl record1 = new SolrScheduledContextViewRecordImpl();
            record1.setParentContextName("parentContext1");
            record1.setContextName("childContext");
            record1.setContextView("{ \"parent\": \"1\" }");
            record1.setTimestamp(1000000L);
            record1.setModifiedBy("user1");

            SolrScheduledContextViewRecordImpl record2 = new SolrScheduledContextViewRecordImpl();
            record2.setParentContextName("parentContext2");
            record2.setContextName("childContext");
            record2.setContextView("{ \"parent\": \"2\" }");
            record2.setTimestamp(2000000L);
            record2.setModifiedBy("user2");

            this.dao.save(record1);
            this.dao.save(record2);

            // Verify both can be retrieved independently
            ScheduledContextViewRecord found1 = this.dao.getContextView("parentContext1", "childContext");
            Assert.assertNotNull(found1);
            Assert.assertEquals("{ \"parent\": \"1\" }", found1.getContextView());

            ScheduledContextViewRecord found2 = this.dao.getContextView("parentContext2", "childContext");
            Assert.assertNotNull(found2);
            Assert.assertEquals("{ \"parent\": \"2\" }", found2.getContextView());
        }
    }

    @Test
    public void test_save_with_null_modified_by() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrScheduledContextViewRecordImpl record = new SolrScheduledContextViewRecordImpl();
            record.setParentContextName("parentContext1");
            record.setContextName("childContext1");
            record.setContextView("{ \"view\": \"data\" }");
            record.setTimestamp(1000000L);
            record.setModifiedBy(null);

            this.dao.save(record);

            ScheduledContextViewRecord found = this.dao.getContextView("parentContext1", "childContext1");

            Assert.assertNotNull(found);
            Assert.assertEquals("parentContext1", found.getParentContextName());
            Assert.assertEquals("childContext1", found.getContextName());
        }
    }

    @Test
    public void test_save_with_empty_context_view() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrScheduledContextViewRecordImpl record = new SolrScheduledContextViewRecordImpl();
            record.setParentContextName("parentContext1");
            record.setContextName("childContext1");
            record.setContextView("");
            record.setTimestamp(1000000L);
            record.setModifiedBy("testUser");

            this.dao.save(record);

            ScheduledContextViewRecord found = this.dao.getContextView("parentContext1", "childContext1");

            Assert.assertNotNull(found);
            Assert.assertEquals("", found.getContextView());
        }
    }

    @Test
    public void test_save_with_complex_json_context_view() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            String complexJson = "{ \"nodes\": [ { \"id\": \"1\", \"name\": \"Node1\" }, { \"id\": \"2\", \"name\": \"Node2\" } ], \"edges\": [ { \"from\": \"1\", \"to\": \"2\" } ] }";

            SolrScheduledContextViewRecordImpl record = new SolrScheduledContextViewRecordImpl();
            record.setParentContextName("parentContext1");
            record.setContextName("childContext1");
            record.setContextView(complexJson);
            record.setTimestamp(1000000L);
            record.setModifiedBy("testUser");

            this.dao.save(record);

            ScheduledContextViewRecord found = this.dao.getContextView("parentContext1", "childContext1");

            Assert.assertNotNull(found);
            Assert.assertEquals(complexJson, found.getContextView());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
