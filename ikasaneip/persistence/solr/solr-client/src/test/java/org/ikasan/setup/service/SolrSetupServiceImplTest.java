package org.ikasan.setup.service;

import org.ikasan.setup.dao.SolrSetupDaoImpl;
import org.ikasan.setup.model.DashboardPlatformSetup;
import org.ikasan.setup.model.DashboardSetupItem;
import org.ikasan.setup.model.SolrDashboardPlatformSetupImpl;
import org.ikasan.setup.model.SolrDashboardSetupItemImpl;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for SolrSetupServiceImpl
 */
public class SolrSetupServiceImplTest {

    private Mockery mockery = new Mockery() {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        }
    };

    private SolrSetupDaoImpl mockDao;
    private SolrSetupServiceImpl service;

    @Before
    public void setup() {
        mockDao = mockery.mock(SolrSetupDaoImpl.class);
        service = new SolrSetupServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_dao_is_null() {
        new SolrSetupServiceImpl(null);
    }

    @Test
    public void test_get_dashboard_platform_setup() {
        // Create expected setup
        SolrDashboardPlatformSetupImpl expectedSetup = new SolrDashboardPlatformSetupImpl();
        expectedSetup.setId("dashboardPlatformSetup");
        expectedSetup.setType("INITIAL_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new SolrDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
        expectedSetup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).setSolrUsername(null);
            oneOf(mockDao).setSolrPassword(null);
            oneOf(mockDao).getDashboardPlatformSetup();
            will(returnValue(expectedSetup));
        }});

        // Execute
        DashboardPlatformSetup result = service.getDashboardPlatformSetup();

        // Verify
        Assert.assertNotNull(result);
        Assert.assertEquals("dashboardPlatformSetup", result.getId());
        Assert.assertEquals("INITIAL_SETUP", result.getType());
        Assert.assertEquals(1, result.getPlatformSetupItems().size());

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_get_dashboard_platform_setup_returns_null() {
        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).setSolrUsername(null);
            oneOf(mockDao).setSolrPassword(null);
            oneOf(mockDao).getDashboardPlatformSetup();
            will(returnValue(null));
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
        SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("NEW_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new SolrDashboardSetupItemImpl("item1", "IN_PROGRESS", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).setSolrUsername(null);
            oneOf(mockDao).setSolrPassword(null);
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
    public void test_save_with_credentials() {
        // Create setup to save
        SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("SECURE_SETUP");
        setup.setPlatformSetupItems(new ArrayList<>());

        // Set Solr credentials on service
        service.setSolrUsername("solrUser");
        service.setSolrPassword("solrPassword");

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).setSolrUsername("solrUser");
            oneOf(mockDao).setSolrPassword("solrPassword");
            oneOf(mockDao).save(setup);
        }});

        // Execute
        service.save(setup);

        // Verify
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_get_dashboard_platform_setup_with_credentials() {
        // Create expected setup
        SolrDashboardPlatformSetupImpl expectedSetup = new SolrDashboardPlatformSetupImpl();
        expectedSetup.setId("dashboardPlatformSetup");
        expectedSetup.setType("SECURE_SETUP");
        expectedSetup.setPlatformSetupItems(new ArrayList<>());

        // Set Solr credentials on service
        service.setSolrUsername("solrUser");
        service.setSolrPassword("solrPassword");

        // Set expectations
        mockery.checking(new Expectations() {{
            oneOf(mockDao).setSolrUsername("solrUser");
            oneOf(mockDao).setSolrPassword("solrPassword");
            oneOf(mockDao).getDashboardPlatformSetup();
            will(returnValue(expectedSetup));
        }});

        // Execute
        DashboardPlatformSetup result = service.getDashboardPlatformSetup();

        // Verify
        Assert.assertNotNull(result);
        Assert.assertEquals("SECURE_SETUP", result.getType());

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_save_and_retrieve_full_workflow() {
        // Create setup
        final SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
        setup.setId("dashboardPlatformSetup");
        setup.setType("WORKFLOW_SETUP");

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new SolrDashboardSetupItemImpl("database-init", "COMPLETE", System.currentTimeMillis()));
        items.add(new SolrDashboardSetupItemImpl("solr-init", "COMPLETE", System.currentTimeMillis()));
        items.add(new SolrDashboardSetupItemImpl("user-setup", "IN_PROGRESS", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        // Set expectations for save
        mockery.checking(new Expectations() {{
            oneOf(mockDao).setSolrUsername(null);
            oneOf(mockDao).setSolrPassword(null);
            oneOf(mockDao).save(setup);

            // Then for retrieve
            oneOf(mockDao).setSolrUsername(null);
            oneOf(mockDao).setSolrPassword(null);
            oneOf(mockDao).getDashboardPlatformSetup();
            will(returnValue(setup));
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
}
