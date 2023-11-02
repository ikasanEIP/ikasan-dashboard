package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.provider.Query;
import org.apache.commons.io.IOUtils;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.scheduler.component.ScheduledAgentsFilteringGrid;
import org.ikasan.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.module.ModuleType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * This test class provides a high level set of tests of the scheduler view. There are
 * more detailed tests for each of the components that the view is comprised of.
 */
public class SchedulerViewTest extends AbstractSchedulerViewTest {

    @Override
    public void setup_expectations() throws IOException {
        when(this.scheduledContextService.findByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 1));

        when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        when(this.schedulerJobInstanceService.getJobStatusCountForContextInstances(Mockito.any()))
            .thenReturn(new ArrayList<>(this.getAggregateContextInstanceStatuses()));

        when(super.moduleMetadataService.find(Mockito.any(ArrayList.class), Mockito.any(ModuleType.class),
            Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(this.getAgents(1));

    }

    @Test
    public void test_scheduler_view_scheduler_agent_instances_card() throws IOException
    {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab schedulerDashboardTab = _get(Tab.class, spec -> spec.withId("schedulerDashboardTab"));
        Assertions.assertNotNull(schedulerDashboardTab);

        schedulerDashboardTabs.setSelectedTab(schedulerDashboardTab);
        Assertions.assertEquals(schedulerDashboardTab, schedulerDashboardTabs.getSelectedTab());

        Assertions.assertNotNull(schedulerDashboardTabs);

        ScheduledAgentsFilteringGrid agentsFilteringGrid = _get(ScheduledAgentsFilteringGrid.class);

        Assertions.assertNotNull(agentsFilteringGrid);

        Assertions.assertEquals(1, agentsFilteringGrid.getDataProvider().size(new Query<>()));
    }

    @Test
    public void test_scheduler_view_context_instances_card_active_job_plan_instances_tab() throws IOException
    {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab schedulerDashboardTab = _get(Tab.class, spec -> spec.withId("schedulerDashboardTab"));
        Assertions.assertNotNull(schedulerDashboardTab);

        schedulerDashboardTabs.setSelectedTab(schedulerDashboardTab);
        Assertions.assertEquals(schedulerDashboardTab, schedulerDashboardTabs.getSelectedTab());

        Assertions.assertNotNull(schedulerDashboardTabs);

        Tabs contextInstanceTabs = _get(Tabs.class, spec -> spec.withId("contextInstancesTab"));
        Assert.assertNotNull(contextInstanceTabs);

        Tab activeJobPlanInstancesTab = _get(Tab.class, spec -> spec.withId("activeJobPlanInstancesTab"));
        Assertions.assertNotNull(contextInstanceTabs);

        contextInstanceTabs.setSelectedTab(activeJobPlanInstancesTab);
        Assertions.assertEquals(activeJobPlanInstancesTab, contextInstanceTabs.getSelectedTab());

        Grid contextInstanceAggregateJobStatusGrid = _get(Grid.class, spec -> spec.withId("contextInstanceAggregateJobStatusGrid"));
        Assertions.assertNotNull(contextInstanceAggregateJobStatusGrid);
    }

    @Test
    public void test_scheduler_view_context_instances_card_prepared_future_job_plan_instances_tab() throws IOException
    {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab schedulerDashboardTab = _get(Tab.class, spec -> spec.withId("schedulerDashboardTab"));
        Assertions.assertNotNull(schedulerDashboardTab);

        schedulerDashboardTabs.setSelectedTab(schedulerDashboardTab);
        Assertions.assertEquals(schedulerDashboardTab, schedulerDashboardTabs.getSelectedTab());

        Assertions.assertNotNull(schedulerDashboardTabs);

        Tabs contextInstanceTabs = _get(Tabs.class, spec -> spec.withId("contextInstancesTab"));
        Assert.assertNotNull(contextInstanceTabs);

        Tab preparedFutureContextInstancesTab = _get(Tab.class, spec -> spec.withId("preparedFutureJobPlanInstancesTab"));
        Assertions.assertNotNull(contextInstanceTabs);

        contextInstanceTabs.setSelectedTab(preparedFutureContextInstancesTab);
        Assertions.assertEquals(preparedFutureContextInstancesTab, contextInstanceTabs.getSelectedTab());

        Grid preparedFutureContextInstanceGrid = _get(Grid.class, spec -> spec.withId("preparedFutureContextInstanceGrid"));
        Assertions.assertNotNull(preparedFutureContextInstanceGrid);
    }

    @Test
    public void test_scheduler_view_context_instances_card_completed_job_plan_instances_tab() throws IOException
    {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab schedulerDashboardTab = _get(Tab.class, spec -> spec.withId("schedulerDashboardTab"));
        Assertions.assertNotNull(schedulerDashboardTab);

        schedulerDashboardTabs.setSelectedTab(schedulerDashboardTab);
        Assertions.assertEquals(schedulerDashboardTab, schedulerDashboardTabs.getSelectedTab());

        Assertions.assertNotNull(schedulerDashboardTabs);

        Tabs contextInstanceTabs = _get(Tabs.class, spec -> spec.withId("contextInstancesTab"));
        Assert.assertNotNull(contextInstanceTabs);

        Tab completedContextInstancesTab = _get(Tab.class, spec -> spec.withId("completedJobPlanInstances"));
        Assertions.assertNotNull(contextInstanceTabs);

        contextInstanceTabs.setSelectedTab(completedContextInstancesTab);
        Assertions.assertEquals(completedContextInstancesTab, contextInstanceTabs.getSelectedTab());

        Grid completedContextInstanceGrid = _get(Grid.class, spec -> spec.withId("completedContextInstanceGrid"));
        Assertions.assertNotNull(completedContextInstanceGrid);
    }
}
