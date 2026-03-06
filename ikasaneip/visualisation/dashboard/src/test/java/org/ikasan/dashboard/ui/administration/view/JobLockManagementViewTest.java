package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;

public class JobLockManagementViewTest extends UITest {

    public void setup_expectations() {
    }

    @Test
    public void test_search_resolve_view()
    {
        UI.getCurrent().navigate("jobLockView");

        JobLockManagementView jobLockManagementView = _get(JobLockManagementView.class);
        Assertions.assertNotNull(jobLockManagementView);
    }

    @Test
    public void test_components_present() {
        UI.getCurrent().navigate("jobLockView");

        // Verify grid is present
        Grid grid = _get(Grid.class);
        Assertions.assertNotNull(grid);

        // Verify refresh button is present
        Button refreshButton = _get(Button.class, spec -> spec.withIcon(VaadinIcon.REFRESH));
        Assertions.assertNotNull(refreshButton);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("jobLockView");

        JobLockManagementView jobLockManagementView = _get(JobLockManagementView.class);
        Assertions.assertTrue(jobLockManagementView.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("jobLockView");

        JobLockManagementView jobLockManagementView = _get(JobLockManagementView.class);
        Assertions.assertTrue(jobLockManagementView.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_is_sized_full() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);
        Assertions.assertTrue(grid.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_has_columns() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);

        // Verify grid has columns configured
        Assertions.assertTrue(grid.getColumns().size() > 0);
    }

    @Test
    public void test_header_exists() {
        UI.getCurrent().navigate("jobLockView");

        JobLockManagementView jobLockManagementView = _get(JobLockManagementView.class);

        com.vaadin.flow.component.html.H4 header = _get(jobLockManagementView, com.vaadin.flow.component.html.H4.class);
        Assertions.assertNotNull(header);
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("jobLockView");

        JobLockManagementView jobLockManagementView = _get(JobLockManagementView.class);

        // Verify view has child components
        Assertions.assertTrue(jobLockManagementView.getChildren().count() > 0);
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("jobLockView");

        JobLockManagementView jobLockManagementView = _get(JobLockManagementView.class);

        // Verify navigation was successful
        Assertions.assertTrue(jobLockManagementView.isAttached());
    }

    @Test
    public void test_grid_has_environment_column() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);

        // Verify environment column exists
        Assertions.assertNotNull(grid.getColumnByKey("environment"));
    }

    @Test
    public void test_grid_has_lock_name_column() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);

        // Verify lock name column exists
        Assertions.assertNotNull(grid.getColumnByKey("lockName"));
    }

    @Test
    public void test_grid_has_lock_count_column() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);

        // Verify lock count column exists
        Assertions.assertNotNull(grid.getColumnByKey("lockCount"));
    }

    @Test
    public void test_grid_columns_are_sortable() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);

        // Verify key columns are sortable
        Assertions.assertTrue(grid.getColumnByKey("environment").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("lockName").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("lockCount").isSortable());
    }

    @Test
    public void test_grid_columns_are_resizable() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);

        // Verify key columns are resizable
        Assertions.assertTrue(grid.getColumnByKey("environment").isResizable());
        Assertions.assertTrue(grid.getColumnByKey("lockName").isResizable());
        Assertions.assertTrue(grid.getColumnByKey("lockCount").isResizable());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("jobLockView");

        JobLockManagementView jobLockManagementView = _get(JobLockManagementView.class);

        // Verify view is properly initialized
        Assertions.assertTrue(jobLockManagementView.isAttached());
        Assertions.assertTrue(jobLockManagementView.isVisible());
        Assertions.assertTrue(jobLockManagementView.getWidth().equals("100%"));
    }

    @Test
    public void test_refresh_button_exists() {
        UI.getCurrent().navigate("jobLockView");

        Button refreshButton = _get(Button.class, spec -> spec.withIcon(VaadinIcon.REFRESH));
        Assertions.assertNotNull(refreshButton);
    }

    @Test
    public void test_grid_has_at_least_three_columns() {
        UI.getCurrent().navigate("jobLockView");

        Grid grid = _get(Grid.class);

        // Verify grid has at least 3 columns (environment, lock name, lock count)
        Assertions.assertTrue(grid.getColumns().size() >= 3);
    }
}
