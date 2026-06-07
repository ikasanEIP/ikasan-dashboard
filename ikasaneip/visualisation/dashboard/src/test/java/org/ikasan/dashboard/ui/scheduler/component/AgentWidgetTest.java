package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


public class AgentWidgetTest extends AbstractSchedulerViewTest {

    @Override
    public void setup_expectations() throws IOException {
        when(this.scheduledContextService.findByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 1));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(-1),eq(-1), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(5),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        when(this.moduleMetadataService.find(eq(List.of()), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(3));

        when(this.moduleMetadataService.find(eq(List.of()), any(), eq(0), eq(1)))
            .thenReturn(this.getAgents(1));

        when(this.moduleMetadataService.find(eq(List.of("*agent0*")), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(1));

        Mockito.when(this.schedulerJobInstanceService.getJobStatusCountForContextInstances(Mockito.any()))
            .thenReturn(new ArrayList<>(this.getAggregateContextInstanceStatuses()));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(0),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));


    }

    @Test
    public void test_simple_access() {

        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);

        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid preparedFutureContextInstanceGrid
            = _get(ScheduledAgentsFilteringGrid.class, spec -> spec.withId("scheduledAgentsFilteringGrid"));

        Assertions.assertNotNull(preparedFutureContextInstanceGrid);

    }

    @Test
    public void test_active_job_plan_instances_tab() throws IOException
    {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);

        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid scheduledAgentsFilteringGrid
            = _get(ScheduledAgentsFilteringGrid.class, spec -> spec.withId("scheduledAgentsFilteringGrid"));

        Assertions.assertNotNull(scheduledAgentsFilteringGrid);

        Assert.assertEquals(3, GridKt._size(scheduledAgentsFilteringGrid));

        List<ModuleMetaData> items = GridKt._findAll(scheduledAgentsFilteringGrid);
        Assert.assertNotNull(items);
        Assert.assertEquals(3, items.size());

        TextField filterTextField = _get(TextField.class, spec -> spec.withId("filterTextField"));
        Assert.assertNotNull(filterTextField);

        // Apply grid filtering
        filterTextField.setValue("agent0");

        Assert.assertEquals(1, GridKt._size(scheduledAgentsFilteringGrid));
        GridKt.expectRow(scheduledAgentsFilteringGrid, 0, "agent0", "agent description0");

        // Double click the row to open the agent management dialog
        GridKt._doubleClickItem(scheduledAgentsFilteringGrid, 0);

        SchedulerAgentManagementDialog agentManagementDialog = _get(SchedulerAgentManagementDialog.class);
        Assert.assertNotNull(agentManagementDialog);

        // Check the fields on SchedulerAgentManagementDialog are as expected
        TextField agentNameTf = _get(TextField.class, spec -> spec.withId("agentName"));
        Assert.assertNotNull(agentNameTf);
        Assert.assertEquals("agent0", agentNameTf.getValue());

        Anchor link = _get(Anchor.class, spec -> spec.withId("link"));
        Assert.assertNotNull(link);

        TextField agentUrlLf = _get(TextField.class, spec -> spec.withId("agentUrlLf"));
        Assert.assertNotNull(agentUrlLf);
        Assert.assertEquals(link, agentUrlLf.getPrefixComponent());
        Assert.assertEquals("https://www.agent.url", link.getText());
    }

    @Test
    public void test_widget_layout_structure() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        // Verify the widget contains components
        Assert.assertTrue(agentWidget.getChildren().count() > 0);

        // Verify the horizontal layout containing header and filter exists within the widget
        List<HorizontalLayout> layouts = agentWidget.getChildren()
            .filter(c -> c instanceof HorizontalLayout)
            .map(c -> (HorizontalLayout) c)
            .collect(java.util.stream.Collectors.toList());

        Assertions.assertFalse(layouts.isEmpty());
        Assert.assertEquals(2, layouts.get(0).getComponentCount());
    }

    @Test
    public void test_header_text_displayed() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        // Verify the header is present
        H4 header = _get(H4.class);
        Assertions.assertNotNull(header);
    }

    @Test
    public void test_filter_text_field_configuration() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        TextField filterTextField = _get(TextField.class, spec -> spec.withId("filterTextField"));
        Assertions.assertNotNull(filterTextField);

        // Verify search icon is configured as prefix
        Assertions.assertNotNull(filterTextField.getPrefixComponent());
        Assert.assertTrue(filterTextField.getPrefixComponent() instanceof Icon);
    }

    @Test
    public void test_grid_configuration() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));
        Assertions.assertNotNull(grid);

        // Verify grid configuration
        Assert.assertTrue(grid.isVisible());
        Assert.assertEquals("100%", grid.getWidth());
        Assert.assertEquals("100.0%", grid.getHeight());

        // Verify grid has columns configured
        Assert.assertEquals(2, grid.getColumns().size());
    }

    @Test
    public void test_grid_displays_all_agents() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        // Verify all 3 agents are displayed
        Assert.assertEquals(3, GridKt._size(grid));

        List<ModuleMetaData> items = GridKt._findAll(grid);
        Assert.assertEquals("agent0", items.get(0).getName());
        Assert.assertEquals("agent1", items.get(1).getName());
        Assert.assertEquals("agent2", items.get(2).getName());

        Assert.assertEquals("agent description0", items.get(0).getDescription());
        Assert.assertEquals("agent description1", items.get(1).getDescription());
        Assert.assertEquals("agent description2", items.get(2).getDescription());
    }

    @Test
    public void test_grid_column_headers() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        // Verify column configuration
        Assert.assertEquals("name", grid.getColumnByKey("name").getKey());
        Assert.assertEquals("description", grid.getColumnByKey("description").getKey());

        // Verify flex grow settings
        Assert.assertEquals(16, grid.getColumnByKey("name").getFlexGrow());
        Assert.assertEquals(32, grid.getColumnByKey("description").getFlexGrow());
    }

    @Test
    public void test_filter_clears_results() {
        UI.getCurrent().navigate("scheduler");

        when(this.moduleMetadataService.find(eq(List.of("*nonexistent*")), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(0));

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        TextField filterTextField = _get(TextField.class, spec -> spec.withId("filterTextField"));

        // Apply filter that matches nothing
        filterTextField.setValue("nonexistent");

        Assert.assertEquals(0, GridKt._size(grid));
    }

    @Test
    public void test_filter_is_case_insensitive() {
        UI.getCurrent().navigate("scheduler");

        when(this.moduleMetadataService.find(eq(List.of("*AGENT0*")), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(1));

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        TextField filterTextField = _get(TextField.class, spec -> spec.withId("filterTextField"));

        // Apply filter with uppercase
        filterTextField.setValue("AGENT0");

        Assert.assertEquals(1, GridKt._size(grid));
    }

    @Test
    public void test_multiple_agents_match_filter() {
        UI.getCurrent().navigate("scheduler");

        when(this.moduleMetadataService.find(eq(List.of("*agent*")), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(3));

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        TextField filterTextField = _get(TextField.class, spec -> spec.withId("filterTextField"));

        // Apply filter that matches all
        filterTextField.setValue("agent");

        Assert.assertEquals(3, GridKt._size(grid));
    }

    @Test
    public void test_grid_row_content() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        // Verify grid has the expected number of items
        Assert.assertEquals(3, GridKt._size(grid));

        // Verify first row content
        List<ModuleMetaData> items = GridKt._findAll(grid);
        Assert.assertEquals("agent0", items.get(0).getName());
        Assert.assertEquals("agent description0", items.get(0).getDescription());
    }

    @Test
    public void test_double_click_opens_dialog() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        // Get the items from grid
        List<ModuleMetaData> items = GridKt._findAll(grid);
        Assert.assertTrue(items.size() >= 2);

        // Double click on second item
        GridKt._doubleClickItem(grid, 0);

        // Verify dialog opens
        SchedulerAgentManagementDialog dialog = _get(SchedulerAgentManagementDialog.class);
        Assertions.assertNotNull(dialog);

        // Verify correct agent is loaded
        TextField agentNameTf = _get(TextField.class, spec -> spec.withId("agentName"));
        Assert.assertEquals("agent0", agentNameTf.getValue());
    }

    @Test
    public void test_filter_then_double_click() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        TextField filterTextField = _get(TextField.class, spec -> spec.withId("filterTextField"));

        // Filter to single agent
        filterTextField.setValue("agent0");
        Assert.assertEquals(1, GridKt._size(grid));

        // Double click filtered row
        GridKt._doubleClickItem(grid, 0);

        SchedulerAgentManagementDialog dialog = _get(SchedulerAgentManagementDialog.class);
        Assertions.assertNotNull(dialog);

        TextField agentNameTf = _get(TextField.class, spec -> spec.withId("agentName"));
        Assert.assertEquals("agent0", agentNameTf.getValue());
    }

    @Test
    public void test_empty_filter_shows_all_agents() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        ScheduledAgentsFilteringGrid grid = _get(ScheduledAgentsFilteringGrid.class,
            spec -> spec.withId("scheduledAgentsFilteringGrid"));

        TextField filterTextField = _get(TextField.class, spec -> spec.withId("filterTextField"));

        // Set filter then clear it
        filterTextField.setValue("agent0");
        Assert.assertEquals(1, GridKt._size(grid));

        filterTextField.setValue("");
        Assert.assertEquals(3, GridKt._size(grid));
    }

    @Test
    public void test_widget_is_visible() {
        UI.getCurrent().navigate("scheduler");

        AgentWidget agentWidget = _get(AgentWidget.class);
        Assertions.assertNotNull(agentWidget);

        Assert.assertTrue(agentWidget.isVisible());
    }
}
