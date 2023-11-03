package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import liquibase.pro.packaged.V;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerView;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ContextTemplateWidgetTest extends AbstractSchedulerViewTest {

    @Override
    public void setup_expectations() throws IOException {
        when(this.scheduledContextService.findByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecords(15), 15, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(0), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecords(15), 15, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(15), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecords(15), 15, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(0)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(1)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(1)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(2), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(2)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(3), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(3)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(4), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(4)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(5), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(5)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(6), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(6)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(7), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(7)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(8), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(8)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(9), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(9)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(10), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(10)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(11), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(11)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(12), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(12)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(13), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(13)), 1, 1));

        when(this.scheduledContextService.findByFilter(any(), eq(1), eq(14), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecords(15).get(14)), 1, 1));

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
        Assert.assertEquals("<iron-icon icon=\"vaadin:modal\" title=\"Manage Job Plan\" id=\"editScheduledJob\" style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></iron-icon>"
            , actionsLayout.getComponentAt(0).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(1).getClass());
        Assert.assertEquals("<iron-icon icon=\"vaadin:trash\" title=\"Delete Job Plan\" style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></iron-icon>"
            , actionsLayout.getComponentAt(1).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(2).getClass());
        Assert.assertEquals("<iron-icon icon=\"vaadin:play\" title=\"Enable scheduled jobs. When scheduled jobs are enabled on a job plan, all instances of that job plan will also have their scheduled jobs enabled when they are created.\" style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></iron-icon>"
            , actionsLayout.getComponentAt(2).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(3).getClass());
        Assert.assertEquals("<iron-icon icon=\"vaadin:ban\" title=\"Disable scheduled jobs. When scheduled jobs are disabled on a job plan, all instances of that job plan will also have their scheduled jobs disabled when they are created.\" style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></iron-icon>"
            , actionsLayout.getComponentAt(3).getElement().toString());

        Assert.assertEquals(FileDownloadWrapper.class, actionsLayout.getComponentAt(4).getClass());
        Assert.assertEquals(FileDownloadWrapper.class, actionsLayout.getComponentAt(5).getClass());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(6).getClass());
        Assert.assertEquals("<iron-icon icon=\"vaadin:external-link\" title=\"Open in new window\" style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></iron-icon>"
            , actionsLayout.getComponentAt(6).getElement().toString());

        Assert.assertEquals(Icon.class, actionsLayout.getComponentAt(7).getClass());
        Assert.assertEquals("<iron-icon icon=\"vaadin:plus\" title=\"Create a new instance of the job plan.\" style=\"cursor:pointer;width:16pt;color:rgba(0, 0, 0, 1.0);height:16pt\"></iron-icon>"
            , actionsLayout.getComponentAt(7).getElement().toString());

        VerticalLayout scheduledJobsDisabled = (VerticalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "scheduledJobsDisabled");
        Assert.assertNotNull(scheduledJobsDisabled);

        HorizontalLayout modifiedByLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "modifiedBy");
        Assert.assertNotNull(modifiedByLayout);

        HorizontalLayout isDisabledLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "isDisabled");
        Assert.assertNotNull(isDisabledLayout);

    }
}
