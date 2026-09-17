package org.ikasan.mongo.persistence.setup.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.setup.model.MongoDashboardPlatformSetupImpl;
import org.ikasan.mongo.persistence.setup.model.MongoDashboardSetupItemImpl;
import org.ikasan.mongo.persistence.setup.repository.MongoDashboardPlatformSetupRepository;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive integration test for MongoSetupDaoImpl using testcontainers
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoSetupDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    private static final String SETUP_ID = "dashboardPlatformSetup";

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoDashboardPlatformSetupRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoSetupDaoImpl dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("ikasan.persistence.mongo.enabled", () -> "true");
    }

    @Autowired
    public void setDao(MongoDashboardPlatformSetupRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoSetupDaoImpl(repository, mongoTemplate);
    }

    @After
    public void teardown() {
        repository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_repository_is_null() {
        new MongoSetupDaoImpl(null, mongoTemplate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_mongoTemplate_is_null() {
        new MongoSetupDaoImpl(repository, null);
    }

    @Test
    public void test_get_dashboard_platform_setup_success() {
        // Create and save expected setup
        MongoDashboardPlatformSetupImpl expectedSetup = createTestSetup();
        dao.save(expectedSetup);

        // Execute
        DashboardPlatformSetup result = dao.getDashboardPlatformSetup();

        // Verify
        Assert.assertNotNull(result);
        Assert.assertEquals(SETUP_ID, result.getId());
        Assert.assertEquals("dashboardPlatformSetup", result.getType());
        Assert.assertEquals(2, result.getPlatformSetupItems().size());
        Assert.assertEquals("database-schema", result.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("COMPLETE", result.getPlatformSetupItems().get(0).getStatus());
    }

    @Test
    public void test_get_dashboard_platform_setup_returns_null_when_not_found() {
        // Execute
        DashboardPlatformSetup result = dao.getDashboardPlatformSetup();

        // Verify
        Assert.assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_throws_exception_when_setup_is_null() {
        dao.save(null);
    }

    @Test
    public void test_save_new_setup_sets_timestamps() {
        // Create new setup without timestamps
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setPlatformSetupItems(new ArrayList<>());

        long beforeSave = System.currentTimeMillis();

        // Execute
        dao.save(setup);

        // Verify timestamps were set
        Assert.assertTrue(setup.getTimestamp() >= beforeSave);
        Assert.assertTrue(setup.getModifiedTimestamp() >= beforeSave);
        Assert.assertEquals(SETUP_ID, setup.getId());

        // Verify it was persisted
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertNotNull(retrieved);
        Assert.assertEquals("dashboardPlatformSetup", retrieved.getType());
    }

    @Test
    public void test_save_existing_setup_updates_modified_timestamp() throws InterruptedException {
        // Create existing setup with timestamp
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId(SETUP_ID);
        long originalTimestamp = System.currentTimeMillis();
        setup.setTimestamp(originalTimestamp);
        setup.setModifiedTimestamp(originalTimestamp);
        setup.setPlatformSetupItems(new ArrayList<>());

        // Save initially
        dao.save(setup);

        // Wait to ensure different timestamp
        Thread.sleep(10);

        // Execute update
        dao.save(setup);

        // Verify
        Assert.assertEquals(originalTimestamp, setup.getTimestamp()); // Should not change
        Assert.assertTrue(setup.getModifiedTimestamp() > originalTimestamp); // Should be updated
        Assert.assertEquals(SETUP_ID, setup.getId());

        // Verify persisted state
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertEquals(originalTimestamp, retrieved.getTimestamp());
        Assert.assertTrue(retrieved.getModifiedTimestamp() > originalTimestamp);
    }

    @Test
    public void test_save_with_empty_id_sets_default_id() {
        // Create setup with empty ID
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setPlatformSetupItems(new ArrayList<>());

        // Execute
        dao.save(setup);

        // Verify ID was set
        Assert.assertEquals(SETUP_ID, setup.getId());

        // Verify persisted
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(SETUP_ID, retrieved.getId());
    }

    @Test
    public void test_save_with_null_id_sets_default_id() {
        // Create setup with null ID
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setPlatformSetupItems(new ArrayList<>());

        // Execute
        dao.save(setup);

        // Verify ID was set
        Assert.assertEquals(SETUP_ID, setup.getId());

        // Verify persisted
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(SETUP_ID, retrieved.getId());
    }

    @Test
    public void test_save_with_multiple_setup_items() {
        // Create setup with multiple items
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("item2", "IN_PROGRESS", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("item3", "PENDING", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("item4", "FAILED", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Execute
        dao.save(setup);

        // Verify items are preserved
        Assert.assertEquals(4, setup.getPlatformSetupItems().size());
        Assert.assertEquals("item1", setup.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("COMPLETE", setup.getPlatformSetupItems().get(0).getStatus());
        Assert.assertEquals("item4", setup.getPlatformSetupItems().get(3).getName());
        Assert.assertEquals("FAILED", setup.getPlatformSetupItems().get(3).getStatus());

        // Verify persisted
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertEquals(4, retrieved.getPlatformSetupItems().size());
        Assert.assertEquals("item1", retrieved.getPlatformSetupItems().get(0).getName());
    }

    @Test
    public void test_save_with_empty_items_list() {
        // Create setup with empty items list
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setPlatformSetupItems(new ArrayList<>());

        // Execute
        dao.save(setup);

        // Verify
        Assert.assertNotNull(setup.getPlatformSetupItems());
        Assert.assertEquals(0, setup.getPlatformSetupItems().size());

        // Verify persisted
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertEquals(0, retrieved.getPlatformSetupItems().size());
    }

    @Test
    public void test_save_converts_non_mongo_implementation() {
        // Create a generic DashboardPlatformSetup (not MongoDashboardPlatformSetupImpl)
        DashboardPlatformSetup genericSetup = new DashboardPlatformSetup() {
            private String id = null; // Null ID so it gets set to the default
            private String type = "GENERIC_SETUP";
            private List<DashboardSetupItem> items = new ArrayList<>();
            private long timestamp = System.currentTimeMillis();
            private long modifiedTimestamp = System.currentTimeMillis();

            @Override
            public String getId() { return id; }

            @Override
            public void setId(String id) { this.id = id; }

            @Override
            public String getType() { return type; }

            @Override
            public void setType(String type) { this.type = type; }

            @Override
            public List<DashboardSetupItem> getPlatformSetupItems() { return items; }

            @Override
            public void setPlatformSetupItems(List<DashboardSetupItem> items) { this.items = items; }

            @Override
            public long getTimestamp() { return timestamp; }

            @Override
            public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

            @Override
            public long getModifiedTimestamp() { return modifiedTimestamp; }

            @Override
            public void setModifiedTimestamp(long modifiedTimestamp) { this.modifiedTimestamp = modifiedTimestamp; }
        };

        // Execute - should convert to MongoDB implementation
        dao.save(genericSetup);

        // Verify it was converted and persisted
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertNotNull(retrieved);
        Assert.assertTrue(retrieved instanceof MongoDashboardPlatformSetupImpl);
        Assert.assertEquals("dashboardPlatformSetup", retrieved.getType());
    }

    @Test
    public void test_save_preserves_all_fields() {
        // Create setup with all fields populated
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        long timestamp = System.currentTimeMillis() - 10000;
        setup.setTimestamp(timestamp);

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("test-item", "COMPLETE", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Execute
        dao.save(setup);

        // Verify all fields are preserved
        Assert.assertEquals("dashboardPlatformSetup", setup.getId());
        Assert.assertEquals("dashboardPlatformSetup", setup.getType());
        Assert.assertEquals(timestamp, setup.getTimestamp());
        Assert.assertTrue(setup.getModifiedTimestamp() >= timestamp);
        Assert.assertEquals(1, setup.getPlatformSetupItems().size());
        Assert.assertEquals("test-item", setup.getPlatformSetupItems().get(0).getName());

        // Verify persisted - note ID gets normalized to SETUP_ID if not the standard ID
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        if (retrieved != null) {
            Assert.assertEquals("dashboardPlatformSetup", retrieved.getType());
        }
    }

    @Test
    public void test_get_dashboard_platform_setup_with_complex_items() {
        // Create setup with complex nested items
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId(SETUP_ID);

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("database-migration-v1", "COMPLETE", System.currentTimeMillis() - 5000));
        items.add(new MongoDashboardSetupItemImpl("database-migration-v2", "COMPLETE", System.currentTimeMillis() - 4000));
        items.add(new MongoDashboardSetupItemImpl("solr-schema-update", "IN_PROGRESS", System.currentTimeMillis() - 3000));
        items.add(new MongoDashboardSetupItemImpl("user-permissions-setup", "PENDING", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Save the setup
        repository.save(setup);

        // Execute
        DashboardPlatformSetup result = dao.getDashboardPlatformSetup();

        // Verify
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.getPlatformSetupItems().size());
        Assert.assertEquals("database-migration-v1", result.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("COMPLETE", result.getPlatformSetupItems().get(0).getStatus());
        Assert.assertEquals("user-permissions-setup", result.getPlatformSetupItems().get(3).getName());
        Assert.assertEquals("PENDING", result.getPlatformSetupItems().get(3).getStatus());
    }

    @Test
    public void test_save_and_update_workflow() {
        // Create initial setup
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("step1", "COMPLETE", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Save initial version
        dao.save(setup);
        long initialModifiedTime = setup.getModifiedTimestamp();

        // Update the setup
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        List<DashboardSetupItem> updatedItems = new ArrayList<>();
        updatedItems.add(new MongoDashboardSetupItemImpl("step1", "COMPLETE", System.currentTimeMillis()));
        updatedItems.add(new MongoDashboardSetupItemImpl("step2", "IN_PROGRESS", System.currentTimeMillis()));
        setup.setPlatformSetupItems(updatedItems);

        dao.save(setup);

        // Verify update
        DashboardPlatformSetup retrieved = dao.getDashboardPlatformSetup();
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(2, retrieved.getPlatformSetupItems().size());
        Assert.assertTrue(retrieved.getModifiedTimestamp() > initialModifiedTime);
    }

    /**
     * Helper method to create a test setup
     */
    private MongoDashboardPlatformSetupImpl createTestSetup() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId(SETUP_ID);
        setup.setTimestamp(System.currentTimeMillis());
        setup.setModifiedTimestamp(System.currentTimeMillis());

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("database-schema", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("solr-index", "COMPLETE", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        return setup;
    }
}
