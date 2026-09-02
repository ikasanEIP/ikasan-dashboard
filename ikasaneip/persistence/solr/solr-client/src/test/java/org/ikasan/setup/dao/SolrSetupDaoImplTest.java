package org.ikasan.setup.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.setup.model.SolrDashboardPlatformSetupImpl;
import org.ikasan.setup.model.SolrDashboardSetupItemImpl;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for SolrSetupDaoImpl
 */
public class SolrSetupDaoImplTest extends SolrTestCaseJ4 {

    private Mockery mockery = new Mockery() {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        }
    };

    private SolrClient server = mockery.mock(SolrClient.class);
    private NodeConfig config;
    private SolrSetupDaoImpl dao;

    @Before
    public void setup() {
        config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrSetupDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_retrieve_dashboard_platform_setup() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a dashboard platform setup
            SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
            setup.setId("dashboardPlatformSetup");
            setup.setType("INITIAL_SETUP");
            setup.setTimestamp(System.currentTimeMillis());
            setup.setModifiedTimestamp(System.currentTimeMillis());

            // Add setup items
            List<DashboardSetupItem> items = new ArrayList<>();
            SolrDashboardSetupItemImpl item1 = new SolrDashboardSetupItemImpl("database-schema", "COMPLETE", System.currentTimeMillis());
            SolrDashboardSetupItemImpl item2 = new SolrDashboardSetupItemImpl("solr-index", "COMPLETE", System.currentTimeMillis());
            items.add(item1);
            items.add(item2);
            setup.setPlatformSetupItems(items);

            // Save the setup
            dao.save(setup);

            // Retrieve the setup
            DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();

            // Assertions
            Assert.assertNotNull(retrieved);
            Assert.assertEquals("dashboardPlatformSetup", retrieved.getId());
            Assert.assertEquals("dashboardPlatformSetup", retrieved.getType());
            Assert.assertNotNull(retrieved.getPlatformSetupItems());
            Assert.assertEquals(2, retrieved.getPlatformSetupItems().size());
            Assert.assertEquals("database-schema", retrieved.getPlatformSetupItems().get(0).getName());
            Assert.assertEquals("COMPLETE", retrieved.getPlatformSetupItems().get(0).getStatus());
            Assert.assertEquals("solr-index", retrieved.getPlatformSetupItems().get(1).getName());
            Assert.assertEquals("COMPLETE", retrieved.getPlatformSetupItems().get(1).getStatus());
        }
    }

    @Test
    public void test_save_updates_existing_setup() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create and save initial setup
            SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
            setup.setId("dashboardPlatformSetup");
            setup.setType("INITIAL_SETUP");

            List<DashboardSetupItem> items = new ArrayList<>();
            items.add(new SolrDashboardSetupItemImpl("item1", "IN_PROGRESS", System.currentTimeMillis()));
            setup.setPlatformSetupItems(items);

            dao.save(setup);

            // Wait a moment to ensure different timestamp
            Thread.sleep(10);

            List<DashboardSetupItem> updatedItems = new ArrayList<>();
            updatedItems.add(new SolrDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
            updatedItems.add(new SolrDashboardSetupItemImpl("item2", "COMPLETE", System.currentTimeMillis()));
            setup.setPlatformSetupItems(updatedItems);

            dao.save(setup);

            // Retrieve the updated setup
            DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();

            // Assertions
            Assert.assertNotNull(retrieved);
            Assert.assertEquals("dashboardPlatformSetup", retrieved.getType());
            Assert.assertEquals(2, retrieved.getPlatformSetupItems().size());
            Assert.assertEquals("COMPLETE", retrieved.getPlatformSetupItems().get(0).getStatus());
            Assert.assertTrue(retrieved.getModifiedTimestamp() > retrieved.getTimestamp());
        }
    }

    @Test
    public void test_get_dashboard_platform_setup_returns_null_when_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Retrieve without saving anything
            DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();

            // Should return null
            Assert.assertNull(retrieved);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_throws_exception_when_null() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Try to save null
            dao.save((DashboardPlatformSetup) null);
        }
    }

    @Test
    public void test_save_sets_timestamps_for_new_setup() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create setup without timestamps
            SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
            setup.setId("dashboardPlatformSetup");
            setup.setType("NEW_SETUP");
            setup.setPlatformSetupItems(new ArrayList<>());

            // Save
            dao.save(setup);

            // Retrieve
            DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();

            // Timestamps should be set
            Assert.assertTrue(retrieved.getTimestamp() > 0);
            Assert.assertTrue(retrieved.getModifiedTimestamp() > 0);
        }
    }

    @Test
    public void test_save_with_empty_items_list() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
            setup.setId("dashboardPlatformSetup");
            setup.setType("EMPTY_SETUP");
            setup.setPlatformSetupItems(new ArrayList<>());

            dao.save(setup);

            DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();

            Assert.assertNotNull(retrieved);
            Assert.assertNotNull(retrieved.getPlatformSetupItems());
            Assert.assertEquals(0, retrieved.getPlatformSetupItems().size());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
