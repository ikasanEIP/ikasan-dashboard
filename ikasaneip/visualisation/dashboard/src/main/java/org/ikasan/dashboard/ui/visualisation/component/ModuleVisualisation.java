package org.ikasan.dashboard.ui.visualisation.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcastListener;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.cache.CacheStateBroadcastListener;
import org.ikasan.dashboard.cache.CacheStateBroadcaster;
import org.ikasan.dashboard.ui.general.component.FlowControlManagementDialog;
import org.ikasan.dashboard.ui.visualisation.layout.IkasanFlowLayoutManager;
import org.ikasan.dashboard.ui.visualisation.model.flow.*;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.ikasan.spec.metadata.model.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.StartupType;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.module.client.TriggerService;
import org.ikasan.spec.persistence.BatchInsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModuleVisualisation extends VerticalLayout implements BeforeEnterObserver
    , FlowStateBroadcastListener, CacheStateBroadcastListener, CanvasItemDoubleClickEventListener
{
    private final Logger logger = LoggerFactory.getLogger(ModuleVisualisation.class);
    private Map<String, Flow> flowMap;
    private DesignerCanvas designerCanvas;
    private Flow currentFlow;
    private Module module;

    private ModuleControlService moduleControlRestService;
    private ConfigurationService configurationRestService;
    private TriggerService triggerRestService;
    private MetaDataService metaDataApplicationRestService;

    private BatchInsert<ModuleMetaData> moduleMetaDataService;

    private Draw2DLayout draw2DLayout;

    private UI current;

    private boolean initialised = false;

    public  ModuleVisualisation(ModuleControlService moduleControlRestService
        , ConfigurationService configurationRestService
        , TriggerService triggerRestService, MetaDataService metaDataApplicationRestService
        , BatchInsert<ModuleMetaData> moduleMetaDataService)
    {
        this.moduleControlRestService = moduleControlRestService;
        this.configurationRestService = configurationRestService;
        this.triggerRestService = triggerRestService;
        this.metaDataApplicationRestService = metaDataApplicationRestService;
        this.moduleMetaDataService = moduleMetaDataService;

        this.setSizeFull();
        this.setMargin(false);
        this.setSpacing(false);
        this.flowMap = new HashMap<>();

        this.current = UI.getCurrent();
    }

    public void addModule(Module module)
    {
        for(Flow flow: module.getFlows())
        {
            add(flow);
        }

        this.module = module;
    }

    protected void add(Flow flow)
    {
        logger.debug("Adding flow [{}] to visualisation.", flow.getName());
        this.flowMap.put(flow.getName(), flow);
        logger.debug("Finished adding flow [{}] to visualisation.", flow.getName());
    }

    private void drawFlowStatus(State state) {
        logger.info("Updating state - " + state.getStateColour());
        this.designerCanvas.removeFigure(this.currentFlow.getName() + "_status");

        this.designerCanvas.addBoundaryStyledXY(this.currentFlow.getName() + "_status", this.currentFlow.getX(), this.currentFlow.getY()
            , this.currentFlow.getW(), this.currentFlow.getH(), "", state.getStateColour(), 5, 20);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        this.init();
    }

    /**
     * Redraws the flow control visualization for the current flow on the designer canvas.
     *
     * This method updates the graphical representation of the flow control by removing the existing
     * figure associated with the current flow's start-up and creating a new image figure with updated
     * attributes. The new figure is based on the flow's current control coordinates, dimensions,
     * and startup type, which determines the image path to be used (manual, automatic, or disabled).
     *
     * The method generates an image figure using the ImageBuilder and associates metadata,
     * such as flow control type, with it. It then serializes the image configuration to JSON format
     * and adds the new figure to the designer canvas. If JSON processing fails, a runtime exception is thrown.
     *
     * Throws:
     * - RuntimeException if there is an error during JSON processing while building the image figure.
     */
    public void redrawFlowControl() {
        try {
            this.designerCanvas.removeFigure(this.currentFlow.getName() + "-start-up");
            ImageBuilder startupBuilder = new ImageBuilder()
                .withId(this.currentFlow.getName() + "-start-up")
                .withX(this.currentFlow.getControlX())
                .withY(this.currentFlow.getControlY())
                .withWidth(60)
                .withHeight(60)
                .withSelectable(true)
                .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.FLOW_START_UP_CONTROL)
                    .build());
            if(this.currentFlow.getStartupType().equals(StartupType.MANUAL)) {
               startupBuilder
                    .withPath(FlowStartup.FLOW_MANUAL_IMAGE);
            }
            else if(this.currentFlow.getStartupType().equals(StartupType.AUTOMATIC)) {
                startupBuilder
                    .withPath(FlowStartup.FLOW_AUTO_IMAGE);
            }
            else if(this.currentFlow.getStartupType().equals(StartupType.DISABLED)) {
                startupBuilder
                    .withPath(FlowStartup.FLOW_DISABLED_IMAGE);
            }

            this.designerCanvas.addImageFigureWithXYOfImageProvided(new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(startupBuilder.build()));
        } catch (Exception e) {
            logger.error("An error has occurred redrawing the flow control for flow[{}]", currentFlow, e);
        }
    }

    /**
     * Initializes the module visualization by setting up the designer canvas.
     *
     * This method ensures that the designer canvas is properly removed if it already exists
     * and initializes a new instance of the DesignerCanvas with a unique identifier. Once the
     * canvas is created, it is added to the visualization layout. This method also updates the
     * initialization flag to indicate that the setup has been completed.
     *
     * The method has no effect if the initialization flag is already set to true.
     */
    private void init()
    {
        if(!initialised) {
            if (this.designerCanvas != null) {
                this.remove(designerCanvas);
            }

            this.designerCanvas = new DesignerCanvas("module-viewport-"+ UUID.randomUUID().toString()
                , "", true, UI.getCurrent(), false);

            this.add(designerCanvas);
            this.initialised = true;
        }
    }

    /**
     * Sets the current flow to the specified flow and updates its recording status based on the
     * configuration parameters retrieved from the server. If the flow name has changed, this method
     * fetches the configuration data for the flow and applies it accordingly. It also initializes
     * the diagram contents for visualization.
     *
     * @param currentFlow the flow to be set as the current flow
     * @throws RuntimeException if an error occurs while setting the current flow or fetching its configuration
     */
    public void setCurrentFlow(Flow currentFlow) {
        try {
            if (this.currentFlow == null || !this.currentFlow.getName().equals(currentFlow.getName())) {
                ConfigurationMetaData<List<ConfigurationParameterMetaData>> flowConfiguration = this.configurationRestService
                    .getFlowConfiguration(module.getUrl(), module.getName(), currentFlow.getName());

                if (flowConfiguration != null) {
                    flowConfiguration.getParameters().stream()
                        .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("isRecording"))
                        .findFirst()
                        .ifPresent(configurationParameterMetaData ->
                            currentFlow.setRecording((Boolean) configurationParameterMetaData.getValue()));
                }

                this.currentFlow = currentFlow;
                this.setDigramContents();
            }
        }
        catch (Exception e) {
            logger.error("An error has occurred setting the current flow! Flow name[{}].", currentFlow.getName(), e);
            throw new RuntimeException(String.format("An error has occurred setting the current flow! Flow name[%s].", currentFlow.getName()), e);
        }
    }

    /**
     * Initializes and updates the contents of the diagram visualization associated with the current flow.
     * This method sets up the layout using a flow layout manager, applies the layout
     * in JSON format to the designer canvas, manages clickable items, and configures event listeners
     * for canvas item double-click actions.
     *
     * @throws IOException if an error occurs during the initialization or JSON import process.
     */
    private void setDigramContents() throws IOException {
        this.init();
        IkasanFlowLayoutManager layoutManager = new IkasanFlowLayoutManager(this.currentFlow);
        this.draw2DLayout = layoutManager.layout();
        this.designerCanvas.setCanvasJson(this.draw2DLayout.getDraw2dJson());
        this.designerCanvas.manageClickableItems();
        designerCanvas.clear();
        designerCanvas.importJson(false);
        designerCanvas.addCanvasItemDoubleClickEventListener(this);
    }

    /**
     * Updates the flow status visualization in the UI based on the provided flow state.
     * This method ensures that the UI is updated only if it is currently attached and
     * if the provided flow state matches with the current flow and module details.
     *
     * @param ui        the UI in which the flow status should be updated
     * @param flowState the state of the flow containing module name, flow name, and the new state
     */
    protected void drawFlowStatus(UI ui, FlowState flowState)
    {
        if(ui.isAttached()) {
            ui.access(() ->
            {
                if (currentFlow != null && flowState.getFlowName().equals(currentFlow.getName())
                    && module != null && flowState.getModuleName().equals(module.getName())) {
                    this.drawFlowStatus(flowState.getState());
                }
            });
        }
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        logger.info(canvasItemDoubleClickEvent.toString());

        if(canvasItemDoubleClickEvent.getFigure().getUserData() == null) {
            if(canvasItemDoubleClickEvent.getFigure().getIdentifier().endsWith("_flow_background")) {
                FlowOptionsDialog flowOptionsDialog = new FlowOptionsDialog(module, currentFlow, configurationRestService, this.designerCanvas);
                flowOptionsDialog.open();
            }
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(FlowItemTypes.BEFORE_WIRETAP)) {
            WiretapManagementDialog wiretapManagementDialog = new WiretapManagementDialog(this.triggerRestService,
                this.getModule(), this.currentFlow,
                canvasItemDoubleClickEvent.getFigure(), this.designerCanvas,
                WiretapManagementDialog.WIRETAP, WiretapManagementDialog.BEFORE);

            wiretapManagementDialog.open();
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(FlowItemTypes.BEFORE_LOGGING_WIRETAP)) {
            WiretapManagementDialog wiretapManagementDialog = new WiretapManagementDialog(this.triggerRestService,
                this.getModule(), this.currentFlow,
                canvasItemDoubleClickEvent.getFigure(), this.designerCanvas,
                WiretapManagementDialog.LOG, WiretapManagementDialog.BEFORE);

            wiretapManagementDialog.open();
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(FlowItemTypes.AFTER_WIRETAP)) {
            WiretapManagementDialog wiretapManagementDialog = new WiretapManagementDialog(this.triggerRestService,
                this.getModule(), this.currentFlow,
                canvasItemDoubleClickEvent.getFigure(), this.designerCanvas,
                WiretapManagementDialog.WIRETAP, WiretapManagementDialog.AFTER);

            wiretapManagementDialog.open();
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(FlowItemTypes.AFTER_LOGGING_WIRETAP)) {
            WiretapManagementDialog wiretapManagementDialog = new WiretapManagementDialog(this.triggerRestService,
                this.getModule(), this.currentFlow,
                canvasItemDoubleClickEvent.getFigure(), this.designerCanvas,
                WiretapManagementDialog.LOG, WiretapManagementDialog.AFTER);

            wiretapManagementDialog.open();
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(FlowItemTypes.FLOW_COMPONENT)) {
                ComponentOptionsDialog componentNodeActionDialog = new ComponentOptionsDialog(this.module,
                    this.currentFlow.getName(), canvasItemDoubleClickEvent.getFigure().getUserData().getComponentName(),
                    this.module.getComponentMap().get(canvasItemDoubleClickEvent.getFigure().getUserData().getComponentName()).isConfigurable(), this.configurationRestService,
                    this.triggerRestService,
                    (AbstractWiretapNode) this.draw2DLayout.getFlowComponent(canvasItemDoubleClickEvent.getFigure().getUserData().getComponentName()),
                    this.metaDataApplicationRestService,
                    this.moduleMetaDataService,
                    this.designerCanvas);

                componentNodeActionDialog.open();
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(FlowItemTypes.FLOW_START_UP_CONTROL)) {
            FlowControlManagementDialog flowControlManagementDialog = new FlowControlManagementDialog(this.module, this.currentFlow,
                this.moduleControlRestService, this);

            flowControlManagementDialog.open();
        }
    }

    /**
     * Retrieves the current module associated with this visualization.
     *
     * @return the module instance currently linked to this visualization
     */
    public Module getModule() {
        return this.module;
    }

    /**
     * Retrieves the current flow associated with this module visualization.
     *
     * @return the current Flow instance representing the active flow in the visualization
     */
    public Flow getCurrentFlow() {
        return this.currentFlow;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        this.current = attachEvent.getUI();
        FlowStateBroadcaster.instance().register(this);
        CacheStateBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent)
    {
        FlowStateBroadcaster.instance().unregister(this);
        CacheStateBroadcaster.instance().unregister(this);
    }

    @Override
    public void receiveFlowStateBroadcast(FlowState flowState) {
        logger.debug("Received flow state: " + flowState);
        this.drawFlowStatus(current, flowState);
    }

    @Override
    public void receiveCacheStateBroadcast(FlowState flowState) {
        logger.debug("Received flow state: " + flowState);
        this.drawFlowStatus(current, flowState);
    }
}
