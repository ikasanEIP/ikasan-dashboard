package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;

public class GroupManagementViewTest extends UITest {

    @Override
    public void setup_expectations() {

    }

    @Test
    public void testGroupManagementView() throws IOException
    {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);
        Assertions.assertNotNull(groupManagementView);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);
        Assertions.assertTrue(groupManagementView.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);
        Assertions.assertTrue(groupManagementView.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_exists() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertNotNull(grid);
    }

    @Test
    public void test_grid_is_sized_full() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);
        Assertions.assertTrue(grid.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_has_columns() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify grid has columns (name, type, description)
        Assertions.assertTrue(grid.getColumns().size() > 0);
    }

    @Test
    public void test_grid_has_name_column() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify name column exists
        Assertions.assertNotNull(grid.getColumnByKey("name"));
    }

    @Test
    public void test_grid_has_type_column() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify type column exists
        Assertions.assertNotNull(grid.getColumnByKey("type"));
    }

    @Test
    public void test_grid_has_description_column() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify description column exists
        Assertions.assertNotNull(grid.getColumnByKey("description"));
    }

    @Test
    public void test_grid_columns_are_sortable() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify all columns are sortable
        Assertions.assertTrue(grid.getColumnByKey("name").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("type").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("description").isSortable());
    }

    @Test
    public void test_grid_has_header_row() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify grid has header rows (includes filter row)
        Assertions.assertTrue(grid.getHeaderRows().size() > 0);
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        // Verify view has child components
        Assertions.assertTrue(groupManagementView.getChildren().count() > 0);
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        // Verify navigation was successful
        Assertions.assertTrue(groupManagementView.isAttached());
    }

    @Test
    public void test_view_has_spacing() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        // Verify view has spacing enabled
        Assertions.assertTrue(groupManagementView.isSpacing());
    }

    @Test
    public void test_grid_columns_count() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify grid has exactly 3 columns (name, type, description)
        Assertions.assertEquals(3, grid.getColumns().size());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        // Verify view is properly initialized
        Assertions.assertTrue(groupManagementView.isAttached());
        Assertions.assertTrue(groupManagementView.isVisible());
        Assertions.assertTrue(groupManagementView.getWidth().equals("100%"));
        Assertions.assertTrue(groupManagementView.isSpacing());
    }

    @Test
    public void test_header_exists() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.html.H2 header = _get(groupManagementView, com.vaadin.flow.component.html.H2.class);
        Assertions.assertNotNull(header);
    }

    @Test
    public void test_grid_has_my_grid_class() {
        UI.getCurrent().navigate("groupManagement");

        GroupManagementView groupManagementView = _get(GroupManagementView.class);

        com.vaadin.flow.component.grid.Grid grid = _get(groupManagementView, com.vaadin.flow.component.grid.Grid.class);

        // Verify grid has the custom class name
        Assertions.assertTrue(grid.getClassNames().contains("my-grid"));
    }
}
