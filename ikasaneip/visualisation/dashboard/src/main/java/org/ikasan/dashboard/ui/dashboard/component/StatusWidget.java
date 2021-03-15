package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.ui.visualisation.component.FlowListFilteringGrid;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StatusWidget extends Div {
    Logger logger = LoggerFactory.getLogger(StatusWidget.class);

    private FlowListFilteringGrid flowsGrid;
    private ModuleMetaDataService moduleMetadataService;

    private Registration flowStateBroadcasterRegistration;

    private HashMap<State, List<FlowMetaData>> stateMap;

    private Div runningDiv;
    private Icon runningIcon;

    private Div stoppedDiv;
    private Icon stoppedIcon;

    private Div errorDiv;
    private Icon errorIcon;

    private Div recoveringDiv;
    private Icon recoveringIcon;

    private Div pausedDiv;
    private Icon pausedDivIcon;

    private Div unknownDiv;
    private Icon unknownDivIcon;

    private FlowSearchFilter flowSearchFilter;

    public StatusWidget(ModuleMetaDataService moduleMetadataService) {
        this.moduleMetadataService = moduleMetadataService;

        this.createStatusView();
    }

    private void createStatusView() {
        this.removeAll();

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");

        TextField textField = new TextField();
        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);

        Div layout = new Div();
        layout.getElement().getStyle().set("margin-top", "20px");
        layout.getElement().getStyle().set("margin-left", "10px");
        layout.setHeight("50px");
        Label flows = new Label("Flows");
        flows.getElement().getStyle().set("font-size", "16pt");


        textField.getElement().getStyle().set("margin-left", "auto");
        textField.getElement().getStyle().set("float", "right");

        layout.add(flows, textField);

        this.runningDiv = new Div();
        this.runningDiv.addClassNames("card-counter", "running");
        this.runningDiv.setHeight("45px");
        this.runningIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        this.runningIcon.getElement().getStyle().set("margin-left", "5px");
        this.runningIcon.getElement().getStyle().set( "cursor", "pointer");
        this.runningIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid(this.stateMap.get(State.RUNNING_STATE));
        });
        this.runningDiv.add(runningIcon);

        this.stoppedDiv = new Div();
        this.stoppedDiv.addClassNames("card-counter", "stopped");
        this.stoppedDiv.setHeight("45px");
        this.stoppedDiv.setText("3 Stopped");
        this.stoppedIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        this.stoppedIcon.getElement().getStyle().set("margin-left", "5px");
        this.stoppedIcon.getElement().getStyle().set( "cursor", "pointer");
        this.stoppedIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid(this.stateMap.get(State.STOPPED_STATE));
        });
        this.stoppedDiv.add(stoppedIcon);

        this.errorDiv = new Div();
        this.errorDiv.addClassNames("card-counter", "stoppedInError");
        this.errorDiv.setHeight("45px");
        this.errorDiv.setText("2 Stopped in Error ");
        this.errorIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        this.errorIcon.getElement().getStyle().set("margin-left", "5px");
        this.errorIcon.getElement().getStyle().set( "cursor", "pointer");
        this.errorIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid(this.stateMap.get(State.STOPPED_IN_ERROR));
        });
        this.errorDiv.add(errorIcon);

        this.recoveringDiv = new Div();
        this.recoveringDiv.addClassNames("card-counter", "recovering");
        this.recoveringDiv.setHeight("45px");
        this.recoveringDiv.setText("1 Recovering ");
        this.recoveringIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        this.recoveringIcon.getElement().getStyle().set("margin-left", "5px");
        this.recoveringIcon.getElement().getStyle().set( "cursor", "pointer");
        this.recoveringIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid(this.stateMap.get(State.RECOVERING_STATE));
        });
        this.recoveringDiv.add(recoveringIcon);

        this.pausedDiv = new Div();
        this.pausedDiv.addClassNames("card-counter", "paused");
        this.pausedDiv.setHeight("45px");
        this.pausedDiv.setText("1 Paused ");
        this.pausedDivIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        this.pausedDivIcon.getElement().getStyle().set("margin-left", "5px");
        this.pausedDivIcon.getElement().getStyle().set( "cursor", "pointer");
        this.pausedDivIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid(this.stateMap.get(State.PAUSED_STATE));
        });
        this.pausedDiv.add(pausedDivIcon);

        this.unknownDiv = new Div();
        this.unknownDiv.addClassNames("card-counter", "unknown");
        this.unknownDiv.setHeight("45px");
        this.unknownDiv.setText("1 Unknown ");
        this.unknownDivIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        this.unknownDivIcon.getElement().getStyle().set("margin-left", "5px");
        this.unknownDivIcon.getElement().getStyle().set( "cursor", "pointer");
        this.unknownDivIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid(this.stateMap.get(State.UNKNOWN_STATE));
        });
        unknownDiv.add(unknownDivIcon);

        div.add(layout, runningDiv, stoppedDiv, errorDiv, recoveringDiv, pausedDiv, unknownDiv);

//        textField.addValueChangeListener(ev->{
//            this.createGrid();
//        });

        this.add(div);

        this.recalculate();
    }

    private void createGrid(List<FlowMetaData> flowsList) {
        this.removeAll();
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");

        TextField textField = new TextField();
        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        Div layout = new Div();
        layout.getElement().getStyle().set("margin-top", "20px");
        layout.getElement().getStyle().set("margin-left", "10px");
        layout.setHeight("50px");
        Label flows = new Label("Flows");
        flows.getElement().getStyle().set("font-size", "16pt");


        Icon returnIcon = VaadinIcon.ARROW_CIRCLE_LEFT_O.create();
        returnIcon.getElement().getStyle().set("margin-left", "5px");
        returnIcon.getElement().getStyle().set( "cursor", "pointer");
        returnIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createStatusView();
        });

        textField.getElement().getStyle().set("margin-left", "auto");
        textField.getElement().getStyle().set("float", "right");

        layout.add(flows, returnIcon, textField);


        // Create a modulesGrid bound to the list
        this.flowSearchFilter = new FlowSearchFilter();
        this.flowsGrid = new FlowListFilteringGrid(flowsList, flowSearchFilter);
        this.flowsGrid.removeAllColumns();
        this.flowsGrid.setVisible(true);
        this.flowsGrid.setWidthFull();
        this.flowsGrid.setHeight("80%");

        this.flowsGrid.addColumn(FlowMetaData::getName)
            .setHeader("Flow Name").setKey("flowName")
            .setFlexGrow(16);

        this.flowsGrid.addColumn(new ComponentRenderer<>(moduleMetaData -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.FLOW.name() + ":" + moduleMetaData.getName());
            Anchor link = new Anchor(route, "view");
            link.setTarget("_blank");
            add(link);
            horizontalLayout.add(link);
            link.getStyle().set("color", "blue");

            return horizontalLayout;
        }));
        div.add(layout, this.flowsGrid);

        this.flowsGrid.init();

        this.add(div);
    }

    public void recalculate() {
        this.initialiseStateMap();

        List<ModuleMetaData> moduleMetaData = this.moduleMetadataService.findAll();

        moduleMetaData.forEach(module -> {
            module.getFlows().forEach(flow -> {
                FlowState flowState = FlowStateCache.instance().get(module,flow.getName());

                flow.setName(module.getName() + "." + flow.getName());

                if(flowState == null) {
                    stateMap.get(State.UNKNOWN_STATE).add(flow);
                }
                else {
                    stateMap.get(flowState.getState()).add(flow);
                }
            });
        });

        this.runningDiv.removeAll();
        this.runningDiv.setText(stateMap.get(State.RUNNING_STATE).size() + " Running");
        if(stateMap.get(State.RUNNING_STATE).size() > 0){
            this.runningDiv.add(this.runningIcon);
        }

        this.stoppedDiv.removeAll();
        this.stoppedDiv.setText(stateMap.get(State.STOPPED_STATE).size() + " Stopped");
        if(stateMap.get(State.STOPPED_STATE).size() > 0){
            this.stoppedDiv.add(this.stoppedIcon);
        }

        this.errorDiv.removeAll();
        this.errorDiv.setText(stateMap.get(State.STOPPED_IN_ERROR_STATE).size() + " Stopped In Error");
        if(stateMap.get(State.STOPPED_IN_ERROR_STATE).size() > 0){
            this.errorDiv.add(this.errorIcon);
        }

        this.recoveringDiv.removeAll();
        this.recoveringDiv.setText(stateMap.get(State.RECOVERING_STATE).size() + " Recovering");
        if(stateMap.get(State.RECOVERING_STATE).size() > 0){
            this.recoveringDiv.add(this.recoveringIcon);
        }

        this.pausedDiv.removeAll();
        this.pausedDiv.setText(stateMap.get(State.PAUSED_STATE).size() + " Paused");
        if(stateMap.get(State.PAUSED_STATE).size() > 0){
            this.pausedDiv.add(this.pausedDivIcon);
        }

        this.unknownDiv.removeAll();
        this.unknownDiv.setText(stateMap.get(State.UNKNOWN_STATE).size() + " Unknown");
        if(stateMap.get(State.UNKNOWN_STATE).size() > 0){
            this.unknownDiv.add(this.unknownDivIcon);
        }
    }

    private void initialiseStateMap() {
        this.stateMap = new HashMap<>();

        stateMap.put(State.RUNNING_STATE, new ArrayList<>());
        stateMap.put(State.STOPPED_STATE, new ArrayList<>());
        stateMap.put(State.STOPPED_IN_ERROR_STATE, new ArrayList<>());
        stateMap.put(State.RECOVERING_STATE, new ArrayList<>());
        stateMap.put(State.UNKNOWN_STATE, new ArrayList<>());
        stateMap.put(State.PAUSED_STATE, new ArrayList<>());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.recalculate();

        flowStateBroadcasterRegistration = FlowStateBroadcaster.register(flowState -> this.recalculate());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.flowStateBroadcasterRegistration != null) {
            this.flowStateBroadcasterRegistration.remove();
            this.flowStateBroadcasterRegistration = null;
        }
    }

}
