package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;


public class UserDirectoriesViewTest extends UITest
{
    @Override
    public void setup_expectations() {

    }

    @Test
    public void testUserDirectoriesView() throws IOException
    {
        UI.getCurrent().navigate("userDirectories");

        UserDirectoriesView userDirectoriesView = _get(UserDirectoriesView.class);
        Assertions.assertNotNull(userDirectoriesView);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("userDirectories");

        UserDirectoriesView userDirectoriesView = _get(UserDirectoriesView.class);
        Assertions.assertTrue(userDirectoriesView.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("userDirectories");

        UserDirectoriesView userDirectoriesView = _get(UserDirectoriesView.class);
        Assertions.assertTrue(userDirectoriesView.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_exists() {
        UI.getCurrent().navigate("userDirectories");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertNotNull(grid);
    }

    @Test
    public void test_grid_is_sized_full() {
        UI.getCurrent().navigate("userDirectories");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertTrue(grid.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_has_columns() {
        UI.getCurrent().navigate("userDirectories");

        com.vaadin.flow.component.grid.Grid grid = _get(com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertTrue(grid.getColumns().size() > 0);
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("userDirectories");

        UserDirectoriesView userDirectoriesView = _get(UserDirectoriesView.class);
        Assertions.assertTrue(userDirectoriesView.getChildren().count() > 0);
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("userDirectories");

        UserDirectoriesView userDirectoriesView = _get(UserDirectoriesView.class);
        Assertions.assertTrue(userDirectoriesView.isAttached());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("userDirectories");

        UserDirectoriesView userDirectoriesView = _get(UserDirectoriesView.class);

        Assertions.assertTrue(userDirectoriesView.isAttached());
        Assertions.assertTrue(userDirectoriesView.isVisible());
        Assertions.assertTrue(userDirectoriesView.getWidth().equals("100%"));
    }
}
