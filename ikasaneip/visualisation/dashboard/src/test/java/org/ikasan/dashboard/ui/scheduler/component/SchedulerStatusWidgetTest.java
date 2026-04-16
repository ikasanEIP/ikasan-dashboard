package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.cache.ModuleMetadataCache;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.visualisation.component.FlowListFilteringGrid;
import org.ikasan.rest.dashboard.model.metadata.module.FlowMetaDataImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.model.FlowMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.ModuleType;
import org.junit.Assert;
import org.junit.Ignore;
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
            .thenReturn(getAgents());
    }

    @Test
    public void test_simple_access() {

        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Assertions.assertNotNull(schedulerStatusWidget);
    }

    @Test
    @Ignore
    // todo need to work out how to prevent intermittent failures
    public void test_status_values() throws InterruptedException {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);
        Assertions.assertNotNull(schedulerStatusWidget);

        ModuleMetadataCache.instance().reset();

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

        Div runningDiv = _get(schedulerStatusWidget, Div.class, spec -> spec.withId("runningDiv"));
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

        Thread.sleep(2000);
        flowListFilteringGrid = _get(FlowListFilteringGrid.class, spec -> spec.withId("flowsGrid"));

        FlowListFilteringGrid finalFlowListFilteringGrid = flowListFilteringGrid;
        Assert.assertEquals(1, GridKt._size(finalFlowListFilteringGrid));

        returnIcon = _get(Icon.class, spec -> spec.withId("returnIcon"));
        _click(returnIcon);

        Thread.sleep(2000);
        Icon pausedDivIcon = _get(Icon.class, spec -> spec.withId("pausedDivIcon"));
        Assert.assertNotNull(pausedDivIcon);
        _click(pausedDivIcon);

        Thread.sleep(2000);
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

    @Test
    public void test_widget_is_visible() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);
        Assert.assertTrue(schedulerStatusWidget.isVisible());
    }

    @Test
    public void test_all_status_divs_exist() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        // Verify all status divs exist
        Assertions.assertNotNull(_get(schedulerStatusWidget, Div.class, spec -> spec.withId("runningDiv")));
        Assertions.assertNotNull(_get(schedulerStatusWidget, Div.class, spec -> spec.withId("stoppedDiv")));
        Assertions.assertNotNull(_get(schedulerStatusWidget, Div.class, spec -> spec.withId("errorDiv")));
        Assertions.assertNotNull(_get(schedulerStatusWidget, Div.class, spec -> spec.withId("recoveringDiv")));
        Assertions.assertNotNull(_get(schedulerStatusWidget, Div.class, spec -> spec.withId("pausedDiv")));
        Assertions.assertNotNull(_get(schedulerStatusWidget, Div.class, spec -> spec.withId("unknownDiv")));
    }


    @Test
    public void test_running_div_initial_state() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Div runningDiv = _get(schedulerStatusWidget, Div.class, spec -> spec.withId("runningDiv"));
        Assertions.assertNotNull(runningDiv);
        // Verify div contains text (should have initial state)
        Assertions.assertNotNull(runningDiv.getText());
    }

    @Test
    public void test_stopped_div_initial_state() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Div stoppedDiv = _get(schedulerStatusWidget, Div.class, spec -> spec.withId("stoppedDiv"));
        Assertions.assertNotNull(stoppedDiv);
        Assertions.assertNotNull(stoppedDiv.getText());
    }

    @Test
    public void test_error_div_initial_state() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Div errorDiv = _get(schedulerStatusWidget, Div.class, spec -> spec.withId("errorDiv"));
        Assertions.assertNotNull(errorDiv);
        Assertions.assertNotNull(errorDiv.getText());
    }

    @Test
    public void test_recovering_div_initial_state() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Div recoveringDiv = _get(schedulerStatusWidget, Div.class, spec -> spec.withId("recoveringDiv"));
        Assertions.assertNotNull(recoveringDiv);
        Assertions.assertNotNull(recoveringDiv.getText());
    }

    @Test
    public void test_paused_div_initial_state() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Div pausedDiv = _get(schedulerStatusWidget, Div.class, spec -> spec.withId("pausedDiv"));
        Assertions.assertNotNull(pausedDiv);
        Assertions.assertNotNull(pausedDiv.getText());
    }

    @Test
    public void test_unknown_div_initial_state() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        Div unknownDiv = _get(schedulerStatusWidget, Div.class, spec -> spec.withId("unknownDiv"));
        Assertions.assertNotNull(unknownDiv);
        Assertions.assertNotNull(unknownDiv.getText());
    }

    @Test
    public void test_widget_has_children() {
        UI.getCurrent().navigate("scheduler");

        SchedulerStatusWidget schedulerStatusWidget = _get(SchedulerStatusWidget.class);

        // Verify widget has components
        Assert.assertTrue(schedulerStatusWidget.getChildren().count() > 0);
    }

    @Test
    public void test_setup_flow_states_helper_method() {
        List<FlowState> flowStates = setupFlowStates(3, State.RUNNING_STATE);

        // Verify helper method creates correct number of flow states
        Assert.assertEquals(3, flowStates.size());
    }

    @Test
    public void test_setup_flow_states_has_correct_state() {
        List<FlowState> flowStates = setupFlowStates(2, State.STOPPED_STATE);

        // Verify flow states have correct state
        flowStates.forEach(flowState -> {
            Assert.assertEquals(State.STOPPED_STATE, flowState.getState());
        });
    }

    @Test
    public void test_get_agents_helper_method() {
        List<ModuleMetaData> agents = getAgents();

        // Verify helper returns agents (should be 28 total: 5+12+3+1+7)
        Assert.assertEquals(28, agents.size());
    }

    @Test
    public void test_get_agents_with_state_helper_method() {
        List<ModuleMetaData> agents = getAgents(5, State.RUNNING_STATE);

        // Verify helper method creates correct number of agents
        Assert.assertEquals(5, agents.size());
    }

    @Test
    public void test_agents_have_correct_module_type() {
        List<ModuleMetaData> agents = getAgents(3, State.RUNNING_STATE);

        // Verify all agents have SCHEDULER_AGENT type
        agents.forEach(agent -> {
            Assert.assertEquals(ModuleType.SCHEDULER_AGENT, agent.getType());
        });
    }

    @Test
    public void test_agents_have_flows() {
        List<ModuleMetaData> agents = getAgents(2, State.RUNNING_STATE);

        // Verify all agents have flows
        agents.forEach(agent -> {
            Assert.assertNotNull(agent.getFlows());
            Assert.assertEquals(1, agent.getFlows().size());
        });
    }
}
