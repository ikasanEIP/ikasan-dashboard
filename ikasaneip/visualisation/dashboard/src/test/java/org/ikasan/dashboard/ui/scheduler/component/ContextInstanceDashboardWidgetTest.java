package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerView;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.instance.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


public class ContextInstanceDashboardWidgetTest extends AbstractSchedulerViewTest {

    @Override
    public void setup_expectations() throws IOException {
        when(this.scheduledContextService.findByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 1));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(0),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 5, 0));


        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(-1),eq(-1), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(5),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(5),eq(0), anyString(), anyString()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(1), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(1), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(1), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(2), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(2), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(3), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(3), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(4), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(4), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(0), anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(4), 0, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(1),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(3), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(2),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(2), 2, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(3),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(1), 3, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(4),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(0), 4, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(0), anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(0), 0, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(1),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(1), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(2),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(2), 2, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(3),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(3), 3, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(4),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(4), 4, 0));

        when(this.moduleMetadataService.find(anyList(), any(), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(new ArrayList<>(), 0, 1));

        Mockito.when(this.schedulerJobInstanceService.getJobStatusCountForContextInstances(Mockito.any()))
            .thenReturn(new ArrayList<>(this.getAggregateContextInstanceStatuses()));


    }

    @Test
    public void test_simple_access() {

        UI.getCurrent().navigate("scheduler");

        ContextInstanceDashboardWidget contextInstanceDashboardWidget = _get(ContextInstanceDashboardWidget.class);

        Assertions.assertNotNull(contextInstanceDashboardWidget);

        Grid<ContextInstanceAggregateJobStatus> contextInstanceAggregateJobStatusGrid
            = _get(Grid.class, spec -> spec.withId("contextInstanceAggregateJobStatusGrid"));

        Assertions.assertNotNull(contextInstanceAggregateJobStatusGrid);

        Grid preparedFutureContextInstanceGrid
            = _get(Grid.class, spec -> spec.withId("preparedFutureContextInstanceGrid"));

        Assertions.assertNotNull(preparedFutureContextInstanceGrid);

        Grid completedContextInstanceGrid
            = _get(Grid.class, spec -> spec.withId("completedContextInstanceGrid"));

        Assertions.assertNotNull(completedContextInstanceGrid);
    }

    @Test
    public void test_active_job_plan_instances_tab() throws IOException
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

        ContextInstanceAggregateJobStatus aggregateJobStatus = (ContextInstanceAggregateJobStatus) GridKt._get(contextInstanceAggregateJobStatusGrid, 0);

        Assertions.assertEquals(1, GridKt._size(contextInstanceAggregateJobStatusGrid));

        Assert.assertEquals("contextInstanceId", aggregateJobStatus.getContextInstanceId());
        Assert.assertEquals("contextName", aggregateJobStatus.getContextInstanceName());

        HorizontalLayout layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "waitingStatusCounts");
        Assert.assertNotNull(layout);

        Button waitingStatusButton = _get(layout, Button.class, spec -> spec.withId("waitingStatusButton"));
        Assert.assertEquals("1 WAITING", waitingStatusButton.getElement().getText());

        layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "completeStatusCounts");
        Assert.assertNotNull(layout);

        Button completeStatusButton = _get(layout, Button.class, spec -> spec.withId("completeStatusButton"));
        Assert.assertEquals("15 COMPLETE", completeStatusButton.getElement().getText());

        layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "runningStatusCounts");
        Assert.assertNotNull(layout);

        Button runningStatusButton = _get(layout, Button.class, spec -> spec.withId("runningStatusButton"));
        Assert.assertEquals("5 RUNNING", runningStatusButton.getElement().getText());

        layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "queuedStatusCounts");
        Assert.assertNotNull(layout);

        Button queuedStatusButton = _get(layout, Button.class, spec -> spec.withId("queuedStatusButton"));
        Assert.assertEquals("0 QUEUED", queuedStatusButton.getElement().getText());

        layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "onHoldStatusCounts");
        Assert.assertNotNull(layout);

        Button onHoldStatusButton = _get(layout, Button.class, spec -> spec.withId("onHoldStatusButton"));
        Assert.assertEquals("1 ON HOLD", onHoldStatusButton.getElement().getText());

        layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "skippedStatusCounts");
        Assert.assertNotNull(layout);

        Button skippedStatusButton = _get(layout, Button.class, spec -> spec.withId("skippedStatusButton"));
        Assert.assertEquals("0 SKIPPED", skippedStatusButton.getElement().getText());


        layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "errorStatusCounts");
        Assert.assertNotNull(layout);

        Button errorStatusButton = _get(layout, Button.class, spec -> spec.withId("errorStatusButton"));
        Assert.assertEquals("0 ERROR", errorStatusButton.getElement().getText());
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

        // The grid loads automatically. Checks its contents are as we expect.
        Assertions.assertEquals(5, GridKt._size(preparedFutureContextInstanceGrid));

        GridKt.expectRow(preparedFutureContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 1, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 3, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 4, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        // Test various sorting of the grid
        GridKt._sortByKey(preparedFutureContextInstanceGrid, "id", SortDirection.DESCENDING);

        GridKt.expectRow(preparedFutureContextInstanceGrid, 4, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 3, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 1, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 0, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        GridKt._sortByKey(preparedFutureContextInstanceGrid, "name", SortDirection.DESCENDING);

        GridKt.expectRow(preparedFutureContextInstanceGrid, 4, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 3, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 1, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 0, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        GridKt._sortByKey(preparedFutureContextInstanceGrid, "id", SortDirection.ASCENDING);

        GridKt.expectRow(preparedFutureContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 1, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 3, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 4, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        GridKt._sortByKey(preparedFutureContextInstanceGrid, "name", SortDirection.ASCENDING);

        GridKt.expectRow(preparedFutureContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 1, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 3, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(preparedFutureContextInstanceGrid, 4, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        // Filter the table based on the context name
        TextField preparedContextNameTf = _get(TextField.class, spec -> spec.withId("preparedContextNameTf"));
        preparedContextNameTf.setValue("contextName0");
        Assert.assertEquals(1, GridKt._size(preparedFutureContextInstanceGrid));
        GridKt.expectRow(preparedFutureContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        // Reset the filter
        preparedContextNameTf.setValue("");
        Assert.assertEquals(5, GridKt._size(preparedFutureContextInstanceGrid));

        TextField preparedContextInstanceIdTf = _get(TextField.class, spec -> spec.withId("preparedContextInstanceIdTf"));
        preparedContextInstanceIdTf.setValue("contextInstanceId0");
        Assert.assertEquals(1, GridKt._size(preparedFutureContextInstanceGrid));
        GridKt.expectRow(preparedFutureContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "VerticalLayout[@style='width:100%;height:100%']",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        // Reset the filter
        preparedContextInstanceIdTf.setValue("");
        Assert.assertEquals(5, GridKt._size(preparedFutureContextInstanceGrid));
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

        // The grid loads automatically. Checks its contents are as we expect.
        Assertions.assertEquals(5, GridKt._size(completedContextInstanceGrid));

        GridKt.expectRow(completedContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 1, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 3, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 4, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        // Test various sorting of the grid
        GridKt._sortByKey(completedContextInstanceGrid, "id", SortDirection.DESCENDING);

        GridKt.expectRow(completedContextInstanceGrid, 4, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 3, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 1, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 0, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        GridKt._sortByKey(completedContextInstanceGrid, "name", SortDirection.DESCENDING);

        GridKt.expectRow(completedContextInstanceGrid, 4, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 3, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 1, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 0, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        GridKt._sortByKey(completedContextInstanceGrid, "id", SortDirection.ASCENDING);

        GridKt.expectRow(completedContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 1, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 3, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 4, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        GridKt._sortByKey(completedContextInstanceGrid, "name", SortDirection.ASCENDING);

        GridKt.expectRow(completedContextInstanceGrid, 0, "contextName0", "contextInstanceId0"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 1, "contextName1", "contextInstanceId1"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 2, "contextName2", "contextInstanceId2"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 3, "contextName3", "contextInstanceId3"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");
        GridKt.expectRow(completedContextInstanceGrid, 4, "contextName4", "contextInstanceId4"
            , "02/01/1970 04:46:40.000 [Europe/London - GMT]", "N/A",
            "VerticalLayout[@style='width:100%;height:100%', @theme='padding spacing']");

        // Filter the table based on the context name
        TextField completeContextNameTf = _get(TextField.class, spec -> spec.withId("completeContextNameTf"));
        completeContextNameTf.setValue("contextName0");
        ContextInstanceDashboardWidget contextInstanceDashboardWidget = _get(ContextInstanceDashboardWidget.class);
        Assert.assertEquals("contextName0", ((ContextInstanceSearchFilter)ReflectionTestUtils.getField(contextInstanceDashboardWidget
            , "completeContextInstanceSearchFilter")).getContextSearchFilter());

        // Reset the filter
        completeContextNameTf.setValue("");
        Assert.assertEquals("", ((ContextInstanceSearchFilter)ReflectionTestUtils.getField(contextInstanceDashboardWidget
            , "completeContextInstanceSearchFilter")).getContextSearchFilter());
        Assert.assertEquals(5, GridKt._size(completedContextInstanceGrid));

        TextField completeContextInstanceIdTf = _get(TextField.class, spec -> spec.withId("completeContextInstanceIdTf"));
        completeContextInstanceIdTf.setValue("contextInstanceId0");
        Assert.assertEquals("contextInstanceId0", ((ContextInstanceSearchFilter)ReflectionTestUtils.getField(contextInstanceDashboardWidget
            , "completeContextInstanceSearchFilter")).getContextInstanceId());

        // Reset the filter
        completeContextInstanceIdTf.setValue("");
        Assert.assertEquals("", ((ContextInstanceSearchFilter)ReflectionTestUtils.getField(contextInstanceDashboardWidget
            , "completeContextInstanceSearchFilter")).getContextInstanceId());
        Assert.assertEquals(5, GridKt._size(completedContextInstanceGrid));
    }
}
