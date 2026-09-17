package org.ikasan.mongo.persistence.setup.service;

import org.ikasan.mongo.persistence.setup.dao.MongoSetupDaoImpl;
import org.ikasan.mongo.persistence.setup.model.MongoDashboardPlatformSetupImpl;
import org.ikasan.mongo.persistence.setup.model.MongoDashboardSetupItemImpl;
import org.ikasan.spec.persistence.dao.SetupDao;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive unit test for MongoSetupServiceImpl
 */
public class MongoSetupServiceImplTest {

    private Mockery mockery = new Mockery() {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        }
    };

    private SetupDao mockDao;
    private MongoSetupServiceImpl service;

    @Before
    public void setup() {
        mockDao = mockery.mock(MongoSetupDaoImpl.class);
        service = new MongoSetupServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_dao_is_null() {
        new MongoSetupServiceImpl(null);
    }

    @Test
    public void test_get_dashboard_platform_setup_returns_setup() {
        // Create expected setup
        MongoDashboardPlatformSetupImpl expectedSetup = new MongoDashboardPlatformSetupImpl();
        expectedSetup.setId("dashboardPlatformSetup");
        expectedSetup.setType("INITIAL_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("item2", "IN_PROGRESS", System.currentTimeMillis()));
        expectedSetup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).getDashboardPlatformSetup();
            will(Expectations.returnValue(expectedSetup));
        }});

        // Execute
        DashboardPlatformSetup result = service.getDashboardPlatformSetup();

        // Verify
        Assert.assertNotNull(result);
        Assert.assertEquals("dashboardPlatformSetup", result.getId());
        Assert.assertEquals("INITIAL_SETUP", result.getType());
        Assert.assertEquals(2, result.getPlatformSetupItems().size());
        Assert.assertEquals("item1", result.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("COMPLETE", result.getPlatformSetupItems().get(0).getStatus());
        Assert.assertEquals("item2", result.getPlatformSetupItems().get(1).getName());
        Assert.assertEquals("IN_PROGRESS", result.getPlatformSetupItems().get(1).getStatus());

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_get_dashboard_platform_setup_returns_null_when_not_found() {
        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).getDashboardPlatformSetup();
            will(Expectations.returnValue(null));
        }});

        // Execute
        DashboardPlatformSetup result = service.getDashboardPlatformSetup();

        // Verify
        Assert.assertNull(result);

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_save_dashboard_platform_setup() {
        // Create setup to save
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("NEW_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("database-init", "COMPLETE", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(setup);
        }});

        // Execute
        service.save(setup);

        // Verify
        mockery.assertIsSatisfied();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_throws_exception_when_setup_is_null() {
        service.save(null);
    }

    @Test
    public void test_save_with_empty_items_list() {
        // Create setup with empty items
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("EMPTY_SETUP");
        setup.setPlatformSetupItems(new ArrayList<>());

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(setup);
        }});

        // Execute
        service.save(setup);

        // Verify
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_save_and_retrieve_workflow() {
        // Create setup
        final MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("WORKFLOW_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("database-init", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("mongo-init", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("user-setup", "IN_PROGRESS", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Set expectations for save and retrieve
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(setup);

            // Then for retrieve
            oneOf(mockDao).getDashboardPlatformSetup();
            will(Expectations.returnValue(setup));
        }});

        // Execute save
        service.save(setup);

        // Execute retrieve
        DashboardPlatformSetup result = service.getDashboardPlatformSetup();

        // Verify
        Assert.assertNotNull(result);
        Assert.assertEquals("WORKFLOW_SETUP", result.getType());
        Assert.assertEquals(3, result.getPlatformSetupItems().size());
        Assert.assertEquals("database-init", result.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("COMPLETE", result.getPlatformSetupItems().get(0).getStatus());
        Assert.assertEquals("user-setup", result.getPlatformSetupItems().get(2).getName());
        Assert.assertEquals("IN_PROGRESS", result.getPlatformSetupItems().get(2).getStatus());

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_save_with_multiple_status_types() {
        // Create setup with various statuses
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("MULTI_STATUS_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("task1", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task2", "IN_PROGRESS", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task3", "PENDING", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task4", "FAILED", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task5", "SKIPPED", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(setup);
        }});

        // Execute
        service.save(setup);

        // Verify
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_get_dashboard_platform_setup_with_large_items_list() {
        // Create setup with many items
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("LARGE_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(new MongoDashboardSetupItemImpl("task-" + i, "COMPLETE", System.currentTimeMillis()));
        }
        setup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).getDashboardPlatformSetup();
            will(Expectations.returnValue(setup));
        }});

        // Execute
        DashboardPlatformSetup result = service.getDashboardPlatformSetup();

        // Verify
        Assert.assertNotNull(result);
        Assert.assertEquals(100, result.getPlatformSetupItems().size());
        Assert.assertEquals("task-0", result.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("task-99", result.getPlatformSetupItems().get(99).getName());

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_save_preserves_timestamps() {
        // Create setup with specific timestamps
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("TIMESTAMP_SETUP");
        long timestamp = System.currentTimeMillis() - 10000;
        long modifiedTimestamp = System.currentTimeMillis();
        setup.setTimestamp(timestamp);
        setup.setModifiedTimestamp(modifiedTimestamp);
        setup.setPlatformSetupItems(new ArrayList<>());

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(setup);
        }});

        // Execute
        service.save(setup);

        // Verify timestamps are preserved (DAO should handle timestamp logic)
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_save_with_special_characters_in_names() {
        // Create setup with special characters
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("SPECIAL_CHARS_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("task-with-hyphens", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task_with_underscores", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task.with.dots", "IN_PROGRESS", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task/with/slashes", "PENDING", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(setup);
        }});

        // Execute
        service.save(setup);

        // Verify
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_multiple_save_calls() {
        // Create first setup
        final MongoDashboardPlatformSetupImpl setup1 = new MongoDashboardPlatformSetupImpl();
        setup1.setId("dashboardPlatformSetup");
        setup1.setType("SETUP_1");
        setup1.setPlatformSetupItems(new ArrayList<>());

        // Create second setup (simulating update)
        final MongoDashboardPlatformSetupImpl setup2 = new MongoDashboardPlatformSetupImpl();
        setup2.setId("dashboardPlatformSetup");
        setup2.setType("SETUP_2");
        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("new-task", "COMPLETE", System.currentTimeMillis()));
        setup2.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(setup1);
            oneOf(mockDao).save(setup2);
        }});

        // Execute
        service.save(setup1);
        service.save(setup2);

        // Verify
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_get_after_multiple_saves() {
        // Create initial setup
        final MongoDashboardPlatformSetupImpl initialSetup = new MongoDashboardPlatformSetupImpl();
        initialSetup.setId("dashboardPlatformSetup");
        initialSetup.setType("INITIAL");
        initialSetup.setPlatformSetupItems(new ArrayList<>());

        // Create updated setup
        final MongoDashboardPlatformSetupImpl updatedSetup = new MongoDashboardPlatformSetupImpl();
        updatedSetup.setId("dashboardPlatformSetup");
        updatedSetup.setType("UPDATED");
        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("task1", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task2", "COMPLETE", System.currentTimeMillis()));
        updatedSetup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).save(initialSetup);
            oneOf(mockDao).save(updatedSetup);
            oneOf(mockDao).getDashboardPlatformSetup();
            will(Expectations.returnValue(updatedSetup));
        }});

        // Execute
        service.save(initialSetup);
        service.save(updatedSetup);
        DashboardPlatformSetup result = service.getDashboardPlatformSetup();

        // Verify we get the updated version
        Assert.assertNotNull(result);
        Assert.assertEquals("UPDATED", result.getType());
        Assert.assertEquals(2, result.getPlatformSetupItems().size());

        mockery.assertIsSatisfied();
    }
}
