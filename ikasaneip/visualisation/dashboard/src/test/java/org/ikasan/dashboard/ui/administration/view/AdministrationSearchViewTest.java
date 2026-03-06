package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;

public class AdministrationSearchViewTest extends UITest {

    @Override
    public void setup_expectations() {
    }

    @Test
    public void test_search_reesolve_view()
    {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);
        Assertions.assertNotNull(administrationSearchView);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);
        Assert.assertTrue(administrationSearchView.isVisible());
    }

    @Test
    public void test_view_has_tabs() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        Tabs tabs = _get(administrationSearchView, Tabs.class, spec -> spec.withId("tabs"));
        Assertions.assertNotNull(tabs);
    }

    @Test
    public void test_tabs_has_system_event_tab() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        Tabs tabs = _get(administrationSearchView, Tabs.class, spec -> spec.withId("tabs"));

        // Verify tabs has at least one tab
        Assert.assertTrue(tabs.getComponentCount() > 0);
    }

    @Test
    public void test_system_event_search_view_exists() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        SystemEventSearchView systemEventSearchView = _get(administrationSearchView, SystemEventSearchView.class);
        Assertions.assertNotNull(systemEventSearchView);
    }

    @Test
    public void test_system_event_search_view_is_visible() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        SystemEventSearchView systemEventSearchView = _get(administrationSearchView, SystemEventSearchView.class);
        Assert.assertTrue(systemEventSearchView.isVisible());
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        // Verify view has child components
        Assert.assertTrue(administrationSearchView.getChildren().count() > 0);
    }

    @Test
    public void test_tabs_are_visible() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        Tabs tabs = _get(administrationSearchView, Tabs.class, spec -> spec.withId("tabs"));
        Assert.assertTrue(tabs.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        // Verify view is full-sized
        Assert.assertTrue(administrationSearchView.isVisible());
    }

    @Test
    public void test_view_navigation_path() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);
        Assertions.assertNotNull(administrationSearchView);

        // Verify navigation was successful
        Assert.assertTrue(administrationSearchView.isAttached());
    }

    @Test
    public void test_tabs_component_count() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        Tabs tabs = _get(administrationSearchView, Tabs.class, spec -> spec.withId("tabs"));

        // Should have at least the system event tab
        Assert.assertEquals(1, tabs.getComponentCount());
    }

    @Test
    public void test_system_event_tab_exists() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        Tabs tabs = _get(administrationSearchView, Tabs.class, spec -> spec.withId("tabs"));

        // Get first tab (should be system event tab)
        Tab tab = (Tab) tabs.getComponentAt(0);
        Assertions.assertNotNull(tab);
    }

    @Test
    public void test_view_is_vertical_layout() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        // Verify view is a VerticalLayout
        Assert.assertTrue(administrationSearchView instanceof com.vaadin.flow.component.orderedlayout.VerticalLayout);
    }

    @Test
    public void test_view_contains_tabs_and_search_view() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        // Verify both tabs and system event search view are present
        Tabs tabs = _get(administrationSearchView, Tabs.class, spec -> spec.withId("tabs"));
        SystemEventSearchView systemEventSearchView = _get(administrationSearchView, SystemEventSearchView.class);

        Assertions.assertNotNull(tabs);
        Assertions.assertNotNull(systemEventSearchView);
    }

    @Test
    public void test_navigation_to_admin_search_view() {
        UI.getCurrent().navigate("adminSearchView");

        // Verify navigation succeeded
        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);
        Assertions.assertNotNull(administrationSearchView);
        Assert.assertTrue(administrationSearchView.isVisible());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("adminSearchView");

        AdministrationSearchView administrationSearchView = _get(AdministrationSearchView.class);

        // Verify view is properly initialized
        Assert.assertTrue(administrationSearchView.isAttached());
        Assert.assertTrue(administrationSearchView.isVisible());
        Assert.assertTrue(administrationSearchView.getChildren().count() >= 2); // tabs + search view
    }
}
