package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.visualisation.component.FlowListFilteringGrid;
import org.ikasan.rest.dashboard.model.metadata.module.FlowMetaDataImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.ModuleType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class SchedulerStatusWidgetTest extends AbstractSchedulerViewTest {

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

        when(super.moduleMetadataService.findAll())
            .thenAnswer((Answer<List<ModuleMetaData>>) invocationOnMock -> getAgents());
    }

    @Test
    public void test_simple_access() {

        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Assertions.assertNotNull(schedulerStatusWidget);
    }

    @Test
    public void test_status_values() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);
        Assertions.assertNotNull(schedulerStatusWidget);

        this.setupFlowStates(5, State.RUNNING_STATE)
            .forEach(flowState -> {
                FlowStateCache.instance().put(flowState);
            });

        this.setupFlowStates(12, State.STOPPED_STATE)
            .forEach(flowState -> {
                FlowStateCache.instance().put(flowState);
            });

        this.setupFlowStates(3, State.STOPPED_IN_ERROR_STATE)
            .forEach(flowState -> {
                FlowStateCache.instance().put(flowState);
            });

        this.setupFlowStates(1, State.RECOVERING_STATE)
            .forEach(flowState -> {
                FlowStateCache.instance().put(flowState);
            });

        this.setupFlowStates(7, State.PAUSED_STATE)
            .forEach(flowState -> {
                FlowStateCache.instance().put(flowState);
            });

        schedulerStatusWidget.recalculate();

        Div runningDiv = _get(Div.class, spec -> spec.withId("runningDiv"));
        Assert.assertNotNull(runningDiv);
        Assert.assertEquals("5 running", runningDiv.getText());

        Div stoppedDiv = _get(Div.class, spec -> spec.withId("stoppedDiv"));
        Assert.assertNotNull(stoppedDiv);
        Assert.assertEquals("12 stopped", stoppedDiv.getText());

        Div errorDiv = _get(Div.class, spec -> spec.withId("errorDiv"));
        Assert.assertNotNull(errorDiv);
        Assert.assertEquals("3 stopped in error", errorDiv.getText());

        Div recoveringDiv = _get(Div.class, spec -> spec.withId("recoveringDiv"));
        Assert.assertNotNull(recoveringDiv);
        Assert.assertEquals("1 recovering", recoveringDiv.getText());

        Div pausedDiv = _get(Div.class, spec -> spec.withId("pausedDiv"));
        Assert.assertNotNull(pausedDiv);
        Assert.assertEquals("7 paused", pausedDiv.getText());

        Div unknownDiv = _get(Div.class, spec -> spec.withId("unknownDiv"));
        Assert.assertNotNull(unknownDiv);
        Assert.assertEquals("0 unknown", unknownDiv.getText());

        Icon runningIcon = _get(Icon.class, spec -> spec.withId("runningIcon"));
        Assert.assertNotNull(runningIcon);
        _click(runningIcon);

        FlowListFilteringGrid flowListFilteringGrid = _get(FlowListFilteringGrid.class, spec -> spec.withId("flowsGrid"));
        Assert.assertNotNull(flowListFilteringGrid);

        Assert.assertEquals(5, GridKt._size(flowListFilteringGrid));

        Icon returnIcon = _get(Icon.class, spec -> spec.withId("returnIcon"));
        Assert.assertNotNull(returnIcon);
        _click(returnIcon);

        Icon stoppedIcon = _get(Icon.class, spec -> spec.withId("stoppedIcon"));
        Assert.assertNotNull(stoppedIcon);
        _click(stoppedIcon);

        Assert.assertEquals(5, GridKt._size(flowListFilteringGrid));

        returnIcon = _get(Icon.class, spec -> spec.withId("returnIcon"));
        _click(returnIcon);

        Icon errorIcon = _get(Icon.class, spec -> spec.withId("errorIcon"));
        Assert.assertNotNull(errorIcon);
        _click(errorIcon);

        flowListFilteringGrid = _get(FlowListFilteringGrid.class, spec -> spec.withId("flowsGrid"));
        Assert.assertEquals(3, GridKt._size(flowListFilteringGrid));

        returnIcon = _get(Icon.class, spec -> spec.withId("returnIcon"));
        _click(returnIcon);

        Icon recoveringIcon = _get(Icon.class, spec -> spec.withId("recoveringIcon"));
        Assert.assertNotNull(recoveringIcon);
        _click(recoveringIcon);

        flowListFilteringGrid = _get(FlowListFilteringGrid.class, spec -> spec.withId("flowsGrid"));
        Assert.assertEquals(1, GridKt._size(flowListFilteringGrid));

        returnIcon = _get(Icon.class, spec -> spec.withId("returnIcon"));
        _click(returnIcon);

        Icon pausedDivIcon = _get(Icon.class, spec -> spec.withId("pausedDivIcon"));
        Assert.assertNotNull(pausedDivIcon);
        _click(pausedDivIcon);

        flowListFilteringGrid = _get(FlowListFilteringGrid.class, spec -> spec.withId("flowsGrid"));
        Assert.assertEquals(7, GridKt._size(flowListFilteringGrid));
    }

    private List<FlowState> setupFlowStates(int count, State state) {
        List<FlowState> flowStates = new ArrayList<>();

        for(int i=0; i<count; i++) {
            flowStates.add(new FlowState("module-name"+count
                , "flow-name"+count+state.getFlowState(), state));
        }

        return flowStates;
    }

    private List<ModuleMetaData> getAgents() {
        ArrayList<ModuleMetaData> agents = new ArrayList<>();
        agents.addAll(this.getAgents(5, State.RUNNING_STATE));
        agents.addAll(this.getAgents(12, State.STOPPED_STATE));
        agents.addAll(this.getAgents(3, State.STOPPED_IN_ERROR_STATE));
        agents.addAll(this.getAgents(1, State.RECOVERING_STATE));
        agents.addAll(this.getAgents(7, State.PAUSED_STATE));
        return agents;
    }

    private List<ModuleMetaData> getAgents(int count, State state) {
        List<ModuleMetaData> agents = new ArrayList<>();

        for(int i=0; i<count; i++) {
            ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
            moduleMetaData.setName("module-name"+count);
            moduleMetaData.setType(ModuleType.SCHEDULER_AGENT);
            FlowMetaData flowMetaData = new FlowMetaDataImpl();
            flowMetaData.setName("flow-name"+count+state.getFlowState());
            moduleMetaData.setFlows(List.of(flowMetaData));
            agents.add(moduleMetaData);
        }

        return agents;
    }
}
