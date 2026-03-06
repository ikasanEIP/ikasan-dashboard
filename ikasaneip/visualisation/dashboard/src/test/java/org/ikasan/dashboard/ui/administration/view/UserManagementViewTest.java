package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;


public class UserManagementViewTest extends UITest
{
    @Override
    public void setup_expectations() {

    }

    @Test
    public void testUserManagementView() throws IOException
    {
        UI.getCurrent().navigate("userManagement");

        UserManagementView userManagementView = _get(UserManagementView.class);
        Assertions.assertNotNull(userManagementView);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("userManagement");

        UserManagementView userManagementView = _get(UserManagementView.class);
        Assertions.assertTrue(userManagementView.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("userManagement");

        UserManagementView userManagementView = _get(UserManagementView.class);
        Assertions.assertTrue(userManagementView.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_exists() {
        UI.getCurrent().navigate("userManagement");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertNotNull(grid);
    }

    @Test
    public void test_grid_is_sized_full() {
        UI.getCurrent().navigate("userManagement");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertTrue(grid.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_has_columns() {
        UI.getCurrent().navigate("userManagement");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertTrue(grid.getColumns().size() > 0);
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("userManagement");

        UserManagementView userManagementView = _get(UserManagementView.class);
        Assertions.assertTrue(userManagementView.getChildren().count() > 0);
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("userManagement");

        UserManagementView userManagementView = _get(UserManagementView.class);
        Assertions.assertTrue(userManagementView.isAttached());
    }

    @Test
    public void test_view_has_spacing() {
        UI.getCurrent().navigate("userManagement");

        UserManagementView userManagementView = _get(UserManagementView.class);
        Assertions.assertTrue(userManagementView.isSpacing());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("userManagement");

        UserManagementView userManagementView = _get(UserManagementView.class);

        Assertions.assertTrue(userManagementView.isAttached());
        Assertions.assertTrue(userManagementView.isVisible());
        Assertions.assertTrue(userManagementView.getWidth().equals("100%"));
        Assertions.assertTrue(userManagementView.isSpacing());
    }
}
