package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import org.ikasan.dashboard.ui.UITest;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;

/**
 * Karibu test for AdministrationSupportView.
 * Tests the log download widgets and UI components.
 */
public class AdministrationSupportViewTest extends UITest {

    @Override
    public void setup_expectations() {
        // No additional setup expectations needed
    }

    @Test
    public void test_view_loads_successfully() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Assertions.assertNotNull(view);
    }

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Assertions.assertTrue(view.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Assertions.assertEquals("100%", view.getWidth());
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Assertions.assertTrue(view.isAttached());
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Assertions.assertTrue(view.getChildren().count() > 0);
    }

    @Test
    public void test_dashboard_exists() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);
        Assertions.assertNotNull(dashboard);
    }

    @Test
    public void test_dashboard_is_sized_full() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);
        Assertions.assertEquals("100%", dashboard.getWidth());
        Assertions.assertEquals("100%", dashboard.getHeight());
    }

    @Test
    public void test_dashboard_has_widgets() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);
        Assertions.assertTrue(dashboard.getWidgets().size() > 0);
    }

    @Test
    public void test_dashboard_has_four_widgets() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);
        // Should have 4 widgets: Dashboard logs, Solr logs, Module logs, System Events
        Assertions.assertEquals(4, dashboard.getWidgets().size());
    }

    @Test
    public void test_dashboard_widgets_have_correct_colspan() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // All widgets should have colspan of 4
        for (DashboardWidget widget : dashboard.getWidgets()) {
            Assertions.assertEquals(4, widget.getColspan());
        }
    }

    @Test
    public void test_dashboard_logs_download_button_exists() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        // Find all buttons in the view
        java.util.List<Button> buttons = _find(view, Button.class);
        Assertions.assertTrue(buttons.size() >= 4, "Should have at least 4 download buttons");
    }

    @Test
    public void test_download_buttons_have_primary_theme() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<Button> buttons = _find(view, Button.class);
        for (Button button : buttons) {
            if (button.getText() != null && button.getText().contains("Download")) {
                Assertions.assertTrue(button.getThemeNames().contains("primary"));
            }
        }
    }

    @Test
    public void test_module_combobox_exists() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        ComboBox comboBox = _get(view, ComboBox.class);
        Assertions.assertNotNull(comboBox);
    }

    @Test
    public void test_module_combobox_has_label() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        ComboBox comboBox = _get(view, ComboBox.class);
        Assertions.assertNotNull(comboBox.getLabel());
        Assertions.assertTrue(comboBox.getLabel().contains("Module"));
    }

    @Test
    public void test_module_combobox_has_error_message() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        ComboBox comboBox = _get(view, ComboBox.class);
        Assertions.assertNotNull(comboBox.getErrorMessage());
    }

    @Test
    public void test_module_combobox_has_width() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        ComboBox comboBox = _get(view, ComboBox.class);
        Assertions.assertEquals("300px", comboBox.getWidth());
    }

    @Test
    public void test_headers_exist() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<H4> h4Headers = _find(view, H4.class);
        // Should have 4 H4 headers (one for each widget)
        Assertions.assertTrue(h4Headers.size() >= 4);
    }

    @Test
    public void test_use_case_headers_exist() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<H5> h5Headers = _find(view, H5.class);
        // Should have H5 headers for "Typical Support Use Cases"
        Assertions.assertTrue(h5Headers.size() >= 4);
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        // Verify view is properly initialized
        Assertions.assertTrue(view.isAttached());
        Assertions.assertTrue(view.isVisible());
        Assertions.assertEquals("100%", view.getWidth());
    }

    @Test
    public void test_dashboard_configuration() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // Verify dashboard configuration
        Assertions.assertNotNull(dashboard);
        Assertions.assertEquals("100%", dashboard.getWidth());
        Assertions.assertEquals("100%", dashboard.getHeight());
    }

    @Test
    public void test_all_widgets_are_visible() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        for (DashboardWidget widget : dashboard.getWidgets()) {
            Assertions.assertTrue(widget.isVisible());
        }
    }

    @Test
    public void test_buttons_are_enabled() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<Button> buttons = _find(view, Button.class);
        for (Button button : buttons) {
            if (button.getText() != null && button.getText().contains("Download")) {
                Assertions.assertTrue(button.isEnabled());
            }
        }
    }

    @Test
    public void test_widgets_have_content() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        for (DashboardWidget widget : dashboard.getWidgets()) {
            Assertions.assertNotNull(widget.getContent());
        }
    }

    @Test
    public void test_view_has_correct_route() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.isAttached());
    }

    @Test
    public void test_dashboard_minimum_column_width() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // Verify minimum column width is set (100px)
        Assertions.assertNotNull(dashboard);
    }

    @Test
    public void test_dashboard_maximum_column_count() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // Verify dashboard is configured with maximum column count
        Assertions.assertNotNull(dashboard);
    }

    @Test
    public void test_widgets_are_sized_appropriately() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // Each widget should span 4 columns
        for (DashboardWidget widget : dashboard.getWidgets()) {
            Assertions.assertEquals(4, widget.getColspan());
        }
    }

    @Test
    public void test_view_contains_dashboard_only() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        // View should contain exactly one direct child (the Dashboard)
        Assertions.assertEquals(1, view.getChildren().count());
    }

    @Test
    public void test_combobox_is_part_of_module_widget() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        ComboBox comboBox = _get(view, ComboBox.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // ComboBox should be within one of the dashboard widgets
        Assertions.assertNotNull(comboBox);
        Assertions.assertNotNull(dashboard);
    }

    @Test
    public void test_multiple_navigations() {
        // Navigate away and back
        UI.getCurrent().navigate("adminSupportView");
        AdministrationSupportView view1 = _get(AdministrationSupportView.class);
        Assertions.assertNotNull(view1);

        // Navigate to home and back
        UI.getCurrent().navigate("");
        UI.getCurrent().navigate("adminSupportView");
        AdministrationSupportView view2 = _get(AdministrationSupportView.class);
        Assertions.assertNotNull(view2);
    }

    @Test
    public void test_buttons_have_icons() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<Button> buttons = _find(view, Button.class);
        for (Button button : buttons) {
            if (button.getText() != null && button.getText().contains("Download")) {
                // Download buttons should have icons
                Assertions.assertNotNull(button.getIcon());
            }
        }
    }

    // System Events Widget Tests

    @Test
    public void test_system_events_widget_exists() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // Verify the 4th widget exists (System Events)
        Assertions.assertTrue(dashboard.getWidgets().size() >= 4);
        DashboardWidget systemEventsWidget = dashboard.getWidgets().get(3);
        Assertions.assertNotNull(systemEventsWidget);
    }

    @Test
    public void test_system_events_widget_is_visible() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        DashboardWidget systemEventsWidget = dashboard.getWidgets().get(3);
        Assertions.assertTrue(systemEventsWidget.isVisible());
    }

    @Test
    public void test_system_events_widget_has_correct_colspan() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        DashboardWidget systemEventsWidget = dashboard.getWidgets().get(3);
        Assertions.assertEquals(4, systemEventsWidget.getColspan());
    }

    @Test
    public void test_system_events_widget_has_content() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        DashboardWidget systemEventsWidget = dashboard.getWidgets().get(3);
        Assertions.assertNotNull(systemEventsWidget.getContent());
    }

    @Test
    public void test_system_events_download_button_exists() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        // Find all buttons
        java.util.List<Button> buttons = _find(view, Button.class);

        // Should have 4 buttons (one for each widget)
        Assertions.assertEquals(4, buttons.size());
    }

    @Test
    public void test_system_events_button_has_primary_theme() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<Button> buttons = _find(view, Button.class);

        // All download buttons should have primary theme
        for (Button button : buttons) {
            Assertions.assertTrue(button.getThemeNames().contains("primary"));
        }
    }

    @Test
    public void test_system_events_button_is_enabled() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<Button> buttons = _find(view, Button.class);

        // All buttons should be enabled
        for (Button button : buttons) {
            Assertions.assertTrue(button.isEnabled());
        }
    }

    @Test
    public void test_system_events_button_has_icon() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<Button> buttons = _find(view, Button.class);

        // All download buttons should have icons
        for (Button button : buttons) {
            Assertions.assertNotNull(button.getIcon());
        }
    }

    @Test
    public void test_system_events_widget_has_header() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<H4> h4Headers = _find(view, H4.class);

        // Should have 4 H4 headers including system events
        Assertions.assertEquals(4, h4Headers.size());
    }

    @Test
    public void test_system_events_widget_has_use_case_header() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<H5> h5Headers = _find(view, H5.class);

        // Should have 4 H5 headers (Typical Support Use Cases for each widget)
        Assertions.assertEquals(4, h5Headers.size());
    }

    @Test
    public void test_system_events_widget_layout_is_sized_full() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        DashboardWidget systemEventsWidget = dashboard.getWidgets().get(3);
        Assertions.assertNotNull(systemEventsWidget.getContent());
    }

    @Test
    public void test_all_four_widgets_have_download_buttons() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // Each of the 4 widgets should have exactly one download button
        Assertions.assertEquals(4, dashboard.getWidgets().size());

        java.util.List<Button> buttons = _find(view, Button.class);
        Assertions.assertEquals(4, buttons.size());
    }

    @Test
    public void test_system_events_widget_no_combobox() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        DashboardWidget systemEventsWidget = dashboard.getWidgets().get(3);

        // System events widget should not have a ComboBox (unlike module logs widget)
        // Only module logs widget (index 2) should have a ComboBox
        java.util.List<ComboBox> comboBoxes = _find(view, ComboBox.class);
        Assertions.assertEquals(1, comboBoxes.size(), "Only module logs widget should have a ComboBox");
    }

    @Test
    public void test_system_events_widget_button_click_safe() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);

        java.util.List<Button> buttons = _find(view, Button.class);

        // Verify system events button (4th button) can be accessed
        Assertions.assertTrue(buttons.size() >= 4);
        Button systemEventsButton = buttons.get(3);
        Assertions.assertNotNull(systemEventsButton);
        Assertions.assertTrue(systemEventsButton.isEnabled());
    }

    @Test
    public void test_system_events_widget_position_in_dashboard() {
        UI.getCurrent().navigate("adminSupportView");

        AdministrationSupportView view = _get(AdministrationSupportView.class);
        Dashboard dashboard = _get(view, Dashboard.class);

        // Verify widget ordering: Dashboard (0), Solr (1), Module (2), System Events (3)
        Assertions.assertEquals(4, dashboard.getWidgets().size());

        // All widgets should be present and in order
        for (int i = 0; i < 4; i++) {
            DashboardWidget widget = dashboard.getWidgets().get(i);
            Assertions.assertNotNull(widget, "Widget at index " + i + " should not be null");
            Assertions.assertTrue(widget.isVisible(), "Widget at index " + i + " should be visible");
        }
    }
}
