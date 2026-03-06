package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;

public class DashboardSessionsViewTest extends UITest {

    public void setup_expectations() {
    }

    @Test
    public void test_search_resolve_view()
    {
        UI.getCurrent().navigate("sessionsView");

        DashboardSessionsView dashboardSessionsView = _get(DashboardSessionsView.class);
        Assertions.assertNotNull(dashboardSessionsView);
    }

    @Test
    public void test_components_present() {
        UI.getCurrent().navigate("sessionsView");

        // Verify grid is present
        Grid grid = _get(Grid.class);
        Assertions.assertNotNull(grid);

        // Verify refresh button is present
        Button refreshButton = _get(Button.class, spec -> spec.withIcon(VaadinIcon.REFRESH));
        Assertions.assertNotNull(refreshButton);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("sessionsView");

        DashboardSessionsView dashboardSessionsView = _get(DashboardSessionsView.class);
        Assertions.assertTrue(dashboardSessionsView.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("sessionsView");

        DashboardSessionsView dashboardSessionsView = _get(DashboardSessionsView.class);
        Assertions.assertTrue(dashboardSessionsView.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_is_sized_full() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);
        Assertions.assertTrue(grid.getWidth().equals("100%"));
    }

    @Test
    public void test_grid_has_columns() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify grid has columns configured
        Assertions.assertTrue(grid.getColumns().size() > 0);
    }

    @Test
    public void test_end_all_sessions_button_exists() {
        UI.getCurrent().navigate("sessionsView");

        Button endAllSessionsButton = _get(Button.class, spec -> spec.withIcon(VaadinIcon.EXCLAMATION));
        Assertions.assertNotNull(endAllSessionsButton);
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("sessionsView");

        DashboardSessionsView dashboardSessionsView = _get(DashboardSessionsView.class);

        // Verify view has child components
        Assertions.assertTrue(dashboardSessionsView.getChildren().count() > 0);
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("sessionsView");

        DashboardSessionsView dashboardSessionsView = _get(DashboardSessionsView.class);

        // Verify navigation was successful
        Assertions.assertTrue(dashboardSessionsView.isAttached());
    }

    @Test
    public void test_grid_columns_configured() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify grid has multiple columns (session ID, username, size, UIs, creation time, last accessed, actions)
        Assertions.assertTrue(grid.getColumns().size() >= 6);
    }

    @Test
    public void test_grid_has_session_identifier_column() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify session identifier column exists
        Assertions.assertNotNull(grid.getColumnByKey("sessionIdentifier"));
    }

    @Test
    public void test_grid_has_username_column() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify username column exists
        Assertions.assertNotNull(grid.getColumnByKey("username"));
    }

    @Test
    public void test_grid_has_session_size_column() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify session size column exists
        Assertions.assertNotNull(grid.getColumnByKey("sessionSize"));
    }

    @Test
    public void test_grid_has_number_of_uis_column() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify number of UIs column exists
        Assertions.assertNotNull(grid.getColumnByKey("numberOfUIs"));
    }

    @Test
    public void test_grid_has_session_creation_time_column() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify session creation time column exists
        Assertions.assertNotNull(grid.getColumnByKey("sessionCreateTime"));
    }

    @Test
    public void test_grid_has_last_accessed_time_column() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify last accessed time column exists
        Assertions.assertNotNull(grid.getColumnByKey("lastAccessedTime"));
    }

    @Test
    public void test_grid_columns_are_sortable() {
        UI.getCurrent().navigate("sessionsView");

        Grid grid = _get(Grid.class);

        // Verify key columns are sortable
        Assertions.assertTrue(grid.getColumnByKey("sessionIdentifier").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("username").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("sessionSize").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("numberOfUIs").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("sessionCreateTime").isSortable());
        Assertions.assertTrue(grid.getColumnByKey("lastAccessedTime").isSortable());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("sessionsView");

        DashboardSessionsView dashboardSessionsView = _get(DashboardSessionsView.class);

        // Verify view is properly initialized
        Assertions.assertTrue(dashboardSessionsView.isAttached());
        Assertions.assertTrue(dashboardSessionsView.isVisible());
        Assertions.assertTrue(dashboardSessionsView.getWidth().equals("100%"));
    }

    @Test
    public void test_both_buttons_exist() {
        UI.getCurrent().navigate("sessionsView");

        // Verify both buttons are present
        Button refreshButton = _get(Button.class, spec -> spec.withIcon(VaadinIcon.REFRESH));
        Button endAllSessionsButton = _get(Button.class, spec -> spec.withIcon(VaadinIcon.EXCLAMATION));

        Assertions.assertNotNull(refreshButton);
        Assertions.assertNotNull(endAllSessionsButton);
    }
}
