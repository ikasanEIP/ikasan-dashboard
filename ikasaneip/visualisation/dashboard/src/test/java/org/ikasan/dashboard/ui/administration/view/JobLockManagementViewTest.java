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
}
