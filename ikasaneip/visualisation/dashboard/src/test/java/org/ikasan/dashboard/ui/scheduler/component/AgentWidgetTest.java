package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
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
}
