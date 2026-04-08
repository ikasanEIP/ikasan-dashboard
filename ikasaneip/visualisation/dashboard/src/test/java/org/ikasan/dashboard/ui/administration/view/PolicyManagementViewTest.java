package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;

@Ignore
public class PolicyManagementViewTest extends UITest
{
    @Override
    public void setup_expectations() {

    }

    @Test
    public void testPolicyManagementView() throws IOException
    {
        UI.getCurrent().navigate("policyManagement");

        PolicyManagementView policyManagementView = _get(PolicyManagementView.class);
        Assertions.assertNotNull(policyManagementView);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("policyManagement");

        PolicyManagementView policyManagementView = _get(PolicyManagementView.class);
        Assertions.assertTrue(policyManagementView.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("policyManagement");

        PolicyManagementView policyManagementView = _get(PolicyManagementView.class);
        Assertions.assertTrue(policyManagementView.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_exists() {
        UI.getCurrent().navigate("policyManagement");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertNotNull(grid);
    }

    @Test
    public void test_grid_is_sized_full() {
        UI.getCurrent().navigate("policyManagement");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertTrue(grid.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_has_columns() {
        UI.getCurrent().navigate("policyManagement");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertTrue(grid.getColumns().size() > 0);
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("policyManagement");

        PolicyManagementView policyManagementView = _get(PolicyManagementView.class);
        Assertions.assertTrue(policyManagementView.getChildren().count() > 0);
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("policyManagement");

        PolicyManagementView policyManagementView = _get(PolicyManagementView.class);
        Assertions.assertTrue(policyManagementView.isAttached());
    }

    @Test
    public void test_view_has_spacing() {
        UI.getCurrent().navigate("policyManagement");

        PolicyManagementView policyManagementView = _get(PolicyManagementView.class);
        Assertions.assertTrue(policyManagementView.isSpacing());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("policyManagement");

        PolicyManagementView policyManagementView = _get(PolicyManagementView.class);

        Assertions.assertTrue(policyManagementView.isAttached());
        Assertions.assertTrue(policyManagementView.isVisible());
        Assertions.assertTrue(policyManagementView.getWidth().equals("100%"));
        Assertions.assertTrue(policyManagementView.isSpacing());
    }
}
