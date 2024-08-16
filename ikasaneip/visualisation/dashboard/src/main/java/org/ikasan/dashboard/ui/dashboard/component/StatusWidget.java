package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcastListener;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.cache.CacheStateBroadcastListener;
import org.ikasan.dashboard.cache.CacheStateBroadcaster;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.cache.ModuleMetadataCache;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.FlowListFilteringGrid;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.topology.metadata.model.FlowMetaDataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class StatusWidget extends Div implements FlowStateBroadcastListener, CacheStateBroadcastListener {
    Logger logger = LoggerFactory.getLogger(StatusWidget.class);

    private FlowListFilteringGrid flowsGrid;
    private ModuleMetaDataService moduleMetadataService;
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

    private UI ui;
    private IkasanAuthentication authentication;

    public StatusWidget(ModuleMetaDataService moduleMetadataService, UI ui) {
        this.moduleMetadataService = moduleMetadataService;
        this.ui = ui;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.createStatusView();
    }

    private void createStatusView() {
        this.removeAll();

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        Div layout = new Div();
        layout.getElement().getStyle().set("margin-top", "20px");
        layout.getElement().getStyle().set("margin-left", "10px");
        layout.setHeight("50px");

        Button refreshButton = new Button();
        refreshButton.getStyle().set("position", "absolute");
        refreshButton.getStyle().set("top", "10px");
        refreshButton.getStyle().set("right", "10px");

        refreshButton.addClickListener(buttonClickEvent -> this.recalculate());
        refreshButton.getElement().appendChild(VaadinIcon.REFRESH.create().getElement());

        layout.add(refreshButton);

        Label flows = new Label(getTranslation("label.flow-status", UI.getCurrent().getLocale()));
        flows.getElement().getStyle().set("font-size", "16pt");

        layout.add(flows);

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
            this.createGrid(this.stateMap.get(State.STOPPED_IN_ERROR_STATE));
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

        this.add(div);

        this.recalculate();
    }

    private void createGrid(List<FlowMetaData> flowsList) {
        this.removeAll();
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");

        TextField statusTextField = new TextField();
        statusTextField.setWidth("300px");

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        statusTextField.setPrefixComponent(icon);
        Div layout = new Div();
        layout.getElement().getStyle().set("margin-top", "20px");
        layout.getElement().getStyle().set("margin-left", "10px");
        layout.setHeight("50px");
        Label flows = new Label(getTranslation("label.flow-status", UI.getCurrent().getLocale()));
        flows.getElement().getStyle().set("font-size", "16pt");


        Icon returnIcon = VaadinIcon.ARROW_CIRCLE_LEFT_O.create();
        returnIcon.getElement().getStyle().set("margin-left", "5px");
        returnIcon.getElement().getStyle().set( "cursor", "pointer");
        returnIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createStatusView();
        });

        statusTextField.getElement().getStyle().set("margin-left", "auto");
        statusTextField.getElement().getStyle().set("float", "right");

        layout.add(flows, returnIcon, statusTextField);


        // Create a modulesGrid bound to the list
        this.flowSearchFilter = new FlowSearchFilter();
        this.flowsGrid = new FlowListFilteringGrid(flowsList, flowSearchFilter);
        this.flowsGrid.removeAllColumns();
        this.flowsGrid.setVisible(true);
        this.flowsGrid.setWidthFull();
        this.flowsGrid.setHeight("80%");

        this.flowsGrid.addColumn(FlowMetaData::getName)
            .setHeader(getTranslation("table-header.flow-name", UI.getCurrent().getLocale())).setKey("flowName")
            .setFlexGrow(16);

        this.flowsGrid.addColumn(new ComponentRenderer<>(moduleMetaData -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.FLOW.name() + ":" + moduleMetaData.getName());
            Anchor link = new Anchor(route, getTranslation("label.view", UI.getCurrent().getLocale()));
            link.setTarget("_blank");
            add(link);
            horizontalLayout.add(link);
            link.getStyle().set("color", "blue");

            return horizontalLayout;
        }));
        div.add(layout, this.flowsGrid);

        this.flowsGrid.init();
        this.flowsGrid.addGridFiltering(statusTextField, this.flowSearchFilter::setFlowNameFilter);

        this.add(div);

        this.recalculate();
    }

    public void recalculate() {
        this.initialiseStateMap();

        List<ModuleMetaData> moduleMetaData = ModuleMetadataCache.instance().getModuleMetadata();

        final Set<String> accessibleModules = SecurityUtils.getAccessibleModules(authentication);

        moduleMetaData.stream()
            .filter(module ->  {
                if(authentication == null || authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)) {
                    return true;
                }
                else if(authentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN) && module.getType() == ModuleType.SCHEDULER_AGENT) {
                    return true;
                }
                else {
                    return accessibleModules.contains(module.getName());
                }
            })
            .forEach(module -> module.getFlows().forEach(flow -> {
                FlowState flowState = FlowStateCache.instance().get(module,flow.getName());
                FlowMetaData flowMetaData = new FlowMetaDataImpl();
                flowMetaData.setName(module.getName() + "." + flow.getName());

                if(flowState == null) {
                    stateMap.get(State.UNKNOWN_STATE).add(flowMetaData);
                }
                else {
                    stateMap.get(flowState.getState()).add(flowMetaData);
                }
            }));

        if(ui.isAttached()) {
            ui.access(() -> {
                this.runningDiv.removeAll();
                this.runningDiv.setText(stateMap.get(State.RUNNING_STATE).size()
                    + " " + getTranslation("status-label.running", UI.getCurrent().getLocale()));
                if (stateMap.containsKey(State.RUNNING_STATE) && stateMap.get(State.RUNNING_STATE).size() > 0) {
                    this.runningDiv.add(this.runningIcon);
                }

                this.stoppedDiv.removeAll();
                this.stoppedDiv.setText(stateMap.get(State.STOPPED_STATE).size()
                    + " " + getTranslation("status-label.stopped", UI.getCurrent().getLocale()));
                if (stateMap.containsKey(State.STOPPED_STATE) && stateMap.get(State.STOPPED_STATE).size() > 0) {
                    this.stoppedDiv.add(this.stoppedIcon);
                }

                this.errorDiv.removeAll();
                this.errorDiv.setText(stateMap.get(State.STOPPED_IN_ERROR_STATE).size()
                    + " " + getTranslation("status-label.stopped-in-error", UI.getCurrent().getLocale()));
                if (stateMap.containsKey(State.STOPPED_IN_ERROR_STATE) && stateMap.get(State.STOPPED_IN_ERROR_STATE).size() > 0) {
                    this.errorDiv.add(this.errorIcon);
                }

                this.recoveringDiv.removeAll();
                this.recoveringDiv.setText(stateMap.get(State.RECOVERING_STATE).size()
                    + " " + getTranslation("status-label.recovering", UI.getCurrent().getLocale()));
                if (stateMap.containsKey(State.RECOVERING_STATE) && stateMap.get(State.RECOVERING_STATE).size() > 0) {
                    this.recoveringDiv.add(this.recoveringIcon);
                }

                this.pausedDiv.removeAll();
                this.pausedDiv.setText(stateMap.get(State.PAUSED_STATE).size()
                    + " " + getTranslation("status-label.paused", UI.getCurrent().getLocale()));
                if (stateMap.containsKey(State.PAUSED_STATE) && stateMap.get(State.PAUSED_STATE).size() > 0) {
                    this.pausedDiv.add(this.pausedDivIcon);
                }

                this.unknownDiv.removeAll();
                this.unknownDiv.setText(stateMap.get(State.UNKNOWN_STATE).size()
                    + " " + getTranslation("status-label.unknown", UI.getCurrent().getLocale()));
                if (stateMap.containsKey(State.UNKNOWN_STATE) && stateMap.get(State.UNKNOWN_STATE).size() > 0) {
                    this.unknownDiv.add(this.unknownDivIcon);
                }
            });
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
        super.onAttach(attachEvent);
        FlowStateBroadcaster.register(this);
        CacheStateBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        FlowStateBroadcaster.unregister(this);
        CacheStateBroadcaster.unregister(this);
    }

    @Override
    public void receiveFlowStateBroadcast(FlowState flowState) {
        this.recalculate();
        logger.debug("Flow state update received!" + flowState);
    }

    @Override
    public void receiveCacheStateBroadcast(FlowState flowState) {
        this.recalculate();
        logger.debug("Flow state update received!" + flowState);
    }
}
