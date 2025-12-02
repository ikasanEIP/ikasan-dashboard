package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ContextTemplateManagementWidgetTest extends AbstractSchedulerViewTest {

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

        when(this.scheduledContextService.findByName(anyString()))
            .thenReturn(super.getScheduledContextRecord());

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

        when(this.schedulerJobInstanceService.getJobStatusCountForContextInstances(Mockito.any()))
            .thenReturn(new ArrayList<>(this.getAggregateContextInstanceStatuses()));

        when(this.schedulerJobService
            .findByContext(anyString(), eq(-1), eq(-1))).thenReturn(super.getSchedulerJobs());

        when(this.schedulerJobService
            .findByFilter(any(), eq(0), eq(0), isNull(), isNull())).thenReturn(super.getSchedulerJobs());

        when(this.contextProfileService.findByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(super.getContextProfiles());
    }

    @Test
    public void test_simple_access() {

        UI.getCurrent().navigate("scheduler");

        // We open the scheduler view
        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        // Get the tabs on the scheduler view and do some assertions in order to confirm all in good order.
        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Assertions.assertNotNull(schedulerDashboardTabs);
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        Assertions.assertNotNull(contextTemplateTab);

        // Select the context template tab and assert success.
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);
        Assertions.assertEquals(contextTemplateTab, schedulerDashboardTabs.getSelectedTab());

        // Now we gat a handle to the template grid on that screen.
        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        Assertions.assertNotNull(contextTemplateFilteringGrid);

        // Get a handle to the actions that can be performed on the context template in the first row of the grid.
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Assert.assertNotNull(actionsLayout);

        // Get the action icon that opens the context template management dialog containing the management widget.
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        // Assert that we can get the ContextTemplateManagementWidget.
        ContextTemplateManagementWidget contextTemplateManagementWidget = _get(ContextTemplateManagementWidget.class);
        Assertions.assertNotNull(contextTemplateManagementWidget);
    }
}
