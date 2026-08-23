package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerView;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ContextTemplateWidgetTest extends AbstractSchedulerViewTest {

    @Override
    public void setup_expectations() throws IOException {
        when(this.scheduledContextService.findByFilterLite(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecordLites(15), 15, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(0), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecordLites(15), 15, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(15), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecordLites(15), 15, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(0)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(1)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(1)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(2), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(2)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(3), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(3)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(4), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(4)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(5), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(5)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(6), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(6)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(7), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(7)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(8), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(8)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(9), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(9)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(10), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(10)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(11), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(11)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(12), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(12)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(13), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(13)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(14), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(14)), 1, 1));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(-1),eq(-1), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(0),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(5),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        when(this.moduleMetadataService.find(eq(List.of()), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(3));

        when(this.moduleMetadataService.find(eq(List.of("*agent0*")), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(1));

        Mockito.when(this.schedulerJobInstanceService.getJobStatusCountForContextInstances(Mockito.any()))
            .thenReturn(new ArrayList<>(this.getAggregateContextInstanceStatuses()));


    }

    @Test
    public void test_simple_access() {

        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        Assertions.assertNotNull(contextTemplateTab);

        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);
        Assertions.assertEquals(contextTemplateTab, schedulerDashboardTabs.getSelectedTab());

        Assertions.assertNotNull(schedulerDashboardTabs);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        Assertions.assertNotNull(contextTemplateFilteringGrid);

        Assert.assertEquals(15, GridKt._size(contextTemplateFilteringGrid));

        GridKt.expectRow(contextTemplateFilteringGrid, 0, "HorizontalLayout[@theme='spacing']", "HorizontalLayout[@theme='spacing']"
            , "HorizontalLayout[@style='width:250px', @theme='spacing']", "02/01/1970 04:46:40.000 [Europe/London - GMT]", "02/01/1970 07:33:20.000 [Europe/London - GMT]"
            , "VerticalLayout[@style='width:100%', @theme='spacing']", "HorizontalLayout[@theme='spacing']", "HorizontalLayout[@style='width:200px', @theme='spacing']");

        HorizontalLayout contextNameLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "moduleName");
        Assert.assertNotNull(contextNameLayout);
        Assert.assertEquals(Text.class, contextNameLayout.getComponentAt(0).getClass());
        Assert.assertEquals("contextName0", ((Text)contextNameLayout.getComponentAt(0)).getText());

        HorizontalLayout descriptionLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "description");
        Assert.assertNotNull(descriptionLayout);
        Assert.assertEquals(Text.class, descriptionLayout.getComponentAt(0).getClass());
        Assert.assertEquals("Description0", ((Text)descriptionLayout.getComponentAt(0)).getText());

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Assert.assertNotNull(actionsLayout);

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(0).getClass());
        Assert.assertEquals("<vaadin-icon icon=\"vaadin:trash\" title=\"Delete Job Plan\" style=\"cursor:pointer;" +
                "width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></vaadin-icon>"
            , actionsLayout.getComponentAt(0).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(1).getClass());
        Assert.assertEquals("<vaadin-icon icon=\"vaadin:play\" title=\"Enable scheduled jobs. When scheduled jobs " +
                "are enabled on a job plan, all instances of that job plan will also have their scheduled jobs enabled when they are created.\" " +
                "style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></vaadin-icon>"
            , actionsLayout.getComponentAt(1).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(2).getClass());
        Assert.assertEquals("<vaadin-icon icon=\"vaadin:ban\" title=\"Disable scheduled jobs. When scheduled jobs" +
                " are disabled on a job plan, all instances of that job plan will also have their scheduled jobs disabled when they " +
                "are created.\" style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></vaadin-icon>"
            , actionsLayout.getComponentAt(2).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(3).getClass());
        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(4).getClass());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(5).getClass());
        Assert.assertEquals("<vaadin-icon icon=\"vaadin:external-link\" title=\"Open in new window\" " +
                "style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></vaadin-icon>"
            , actionsLayout.getComponentAt(5).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(6).getClass());
        Assert.assertEquals("<vaadin-icon icon=\"vaadin:plus\" title=\"Create a new instance of the job plan.\" " +
                "style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></vaadin-icon>"
            , actionsLayout.getComponentAt(6).getElement().toString());

        VerticalLayout scheduledJobsDisabled = (VerticalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "scheduledJobsDisabled");
        Assert.assertNotNull(scheduledJobsDisabled);

        HorizontalLayout modifiedByLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "modifiedBy");
        Assert.assertNotNull(modifiedByLayout);

        HorizontalLayout isDisabledLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "isDisabled");
        Assert.assertNotNull(isDisabledLayout);

    }

    @Test
    public void test_grid_displays_correct_number_of_rows() {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        // Verify exactly 15 templates as mocked
        Assert.assertEquals(15, GridKt._size(contextTemplateFilteringGrid));
    }

    @Test
    public void test_grid_context_name_column_data() {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout contextNameLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "moduleName");
        Assert.assertNotNull(contextNameLayout);
        Assert.assertEquals("contextName0", ((Text)contextNameLayout.getComponentAt(0)).getText());
    }

    @Test
    public void test_grid_description_column_data() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout descriptionLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "description");
        Assert.assertNotNull(descriptionLayout);
        Assert.assertEquals("Description0", ((Text)descriptionLayout.getComponentAt(0)).getText());
    }

    @Test
    public void test_grid_actions_column_has_all_icons() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Assert.assertNotNull(actionsLayout);

        // Verify we have 8 action icons
        Assert.assertEquals(7, actionsLayout.getComponentCount());

        // Verify all components are icons
        for (int i = 0; i < 7; i++) {
            Assert.assertTrue(actionsLayout.getComponentAt(i) instanceof Icon);
        }
    }

    @Test
    public void test_grid_delete_icon_properties() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");

        Icon deleteIcon = (Icon) actionsLayout.getComponentAt(0);
        String iconHtml = deleteIcon.getElement().toString();

        // Verify delete icon has correct properties
        Assert.assertTrue(iconHtml.contains("vaadin:trash"));
        Assert.assertTrue(iconHtml.contains("Delete Job Plan"));
    }

    @Test
    public void test_grid_enable_scheduled_jobs_icon() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");

        Icon enableIcon = (Icon) actionsLayout.getComponentAt(1);
        String iconHtml = enableIcon.getElement().toString();

        // Verify enable icon has correct properties
        Assert.assertTrue(iconHtml.contains("vaadin:play"));
        Assert.assertTrue(iconHtml.contains("Enable scheduled jobs"));
    }

    @Test
    public void test_grid_disable_scheduled_jobs_icon() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");

        Icon disableIcon = (Icon) actionsLayout.getComponentAt(2);
        String iconHtml = disableIcon.getElement().toString();

        // Verify disable icon has correct properties
        Assert.assertTrue(iconHtml.contains("vaadin:ban"));
        Assert.assertTrue(iconHtml.contains("Disable scheduled jobs"));
    }

    @Test
    public void test_grid_open_in_new_window_icon() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");

        Icon openIcon = (Icon) actionsLayout.getComponentAt(5);
        String iconHtml = openIcon.getElement().toString();

        // Verify open in new window icon has correct properties
        Assert.assertTrue(iconHtml.contains("vaadin:external-link"));
        Assert.assertTrue(iconHtml.contains("Open in new window"));
    }

    @Test
    public void test_grid_create_instance_icon() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");

        Icon createIcon = (Icon) actionsLayout.getComponentAt(6);
        String iconHtml = createIcon.getElement().toString();

        // Verify create instance icon has correct properties
        Assert.assertTrue(iconHtml.contains("vaadin:plus"));
        Assert.assertTrue(iconHtml.contains("Create a new instance"));
    }

    @Test
    public void test_grid_scheduled_jobs_disabled_column() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        VerticalLayout scheduledJobsDisabled = (VerticalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "scheduledJobsDisabled");
        Assert.assertNotNull(scheduledJobsDisabled);
    }

    @Test
    public void test_grid_modified_by_column() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout modifiedByLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "modifiedBy");
        Assert.assertNotNull(modifiedByLayout);
    }

    @Test
    public void test_grid_is_disabled_column() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout isDisabledLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "isDisabled");
        Assert.assertNotNull(isDisabledLayout);
    }

    @Test
    public void test_context_template_tab_is_visible() {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));

        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        // Verify grid is visible after tab selection
        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        Assert.assertTrue(contextTemplateFilteringGrid.isVisible());
    }

    @Test
    public void test_grid_row_data_consistency() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        // Verify first row data
        HorizontalLayout contextNameLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "moduleName");
        HorizontalLayout descriptionLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "description");

        Assert.assertEquals("contextName0", ((Text)contextNameLayout.getComponentAt(0)).getText());
        Assert.assertEquals("Description0", ((Text)descriptionLayout.getComponentAt(0)).getText());
    }

    @Test
    public void test_all_action_icons_are_clickable() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");

        // Verify all icons have cursor:pointer style
        for (int i = 0; i < actionsLayout.getComponentCount(); i++) {
            Icon icon = (Icon) actionsLayout.getComponentAt(i);
            String iconHtml = icon.getElement().toString();
            Assert.assertTrue("Icon " + i + " should be clickable", iconHtml.contains("cursor:pointer"));
        }
    }

    @Test
    public void test_context_name_is_text_component() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout contextNameLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "moduleName");

        // Verify context name is rendered as Text component
        Assert.assertEquals(Text.class, contextNameLayout.getComponentAt(0).getClass());
    }

    @Test
    public void test_description_is_text_component() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);

        HorizontalLayout descriptionLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "description");

        // Verify description is rendered as Text component
        Assert.assertEquals(Text.class, descriptionLayout.getComponentAt(0).getClass());
    }
}
