package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ScheduledContextDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextHelper;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SchedulerVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener {
    private Logger logger = LoggerFactory.getLogger(SchedulerVisualisation.class);

    private Registration contextInstanceStateChangeRegistration;

    private DesignerCanvas designerCanvas;

    private String dynamicImagePath;

    private ContextInstance contextInstance;

    private boolean initialised = false;

    private ScheduledContextDraw2dAdapter adapter = new ScheduledContextDraw2dAdapter();

    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobService schedulerJobService;
    private LogStreamingService logStreamingService;

    public SchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService) {

        this.dynamicImagePath = dynamicImagePath;
        if (this.dynamicImagePath == null) {
            throw new IllegalArgumentException("dynamicImagePath cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("agent cannot be null!");
        }

        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if(this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }

        this.configurationRestService = configurationRestService;
        if(this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }

        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }

        this.metaDataRestService = metaDataRestService;
        if(this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }

        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }

        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.logStreamingService = logStreamingService;
        if(this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();
    }

    /**
     * @param contextInstance
     */
    public void createSchedulerVisualisation(ContextInstance contextInstance) throws IOException {
        this.contextInstance = contextInstance;
        this.initialised = false;
        init();
    }

    private void init() throws IOException{
        if(!initialised) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas("canvas-viewport", this.dynamicImagePath, true);
            this.designerCanvas.setCanvasJson(adapter.adaptContext(contextInstance));
            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);

            this.designerCanvas.manageClickableItems();

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

    protected Component initCanvasActions() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);
        actions.setPadding(false);
        actions.setId("canvas-actions");

        // Zoom in
        Button zoomInButton = new Button();
        zoomInButton.getElement().appendChild(IronIcons.ZOOM_IN.create().getElement());
        zoomInButton.setId("canvas_zoom_in");
        Tooltip zoomInButtonTooltip = getTooltip(zoomInButton, getTranslation("tooltip.zoom-in", UI.getCurrent().getLocale())
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        actions.add(zoomInButton, zoomInButtonTooltip);

        // Zoom out
        Button zoomOutButton = new Button();
        zoomOutButton.getElement().appendChild(IronIcons.ZOOM_OUT.create().getElement());
        zoomOutButton.setId("canvas_zoom_out");
        Tooltip zoomOutButtonTooltip = getTooltip(zoomOutButton, getTranslation("tooltip.zoom-out", UI.getCurrent().getLocale())
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        actions.add(zoomOutButton, zoomOutButtonTooltip);

        // Export as selected format
        Button download = new Button();
        download.getElement().appendChild(IronIcons.FILE_DOWNLOAD.create().getElement());
        Tooltip downloadTooltip = getTooltip(download, getTranslation("tooltip.export-png", UI.getCurrent().getLocale())
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        actions.add(download, downloadTooltip);
        download.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.exportPng();
        });

        return actions;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        try {
            this.init();
        }
        catch (IOException e) {
            logger.warn("Could not initialise business stream!", e);
        }
        this.redraw();
    }

    public void redraw() {

    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {

        if(canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            ContextInstance contextInstance = ContextHelper.getChildContextInstance(canvasItemDoubleClickEvent.getFigure().getIdentifier(),
                this.contextInstance);

            if(contextInstance.getScheduledJobs() != null) {
                try {
                    JobVisualisationDialog jobVisualisationDialog = new JobVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                        this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                        this.schedulerJobService, this.logStreamingService);
                    jobVisualisationDialog.createSchedulerVisualisation(this.contextInstance, contextInstance);
                    jobVisualisationDialog.open();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    ContextInstanceVisualisationDialog contextInstanceVisualisationDialog
                        = new ContextInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                        this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                        this.schedulerJobService, this.logStreamingService);
                    contextInstanceVisualisationDialog.createSchedulerVisualisation(this.contextInstance, contextInstance);
                    contextInstanceVisualisationDialog.open();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {
//        JobContextMenu jobContextMenu = new JobContextMenu(canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY());
//        jobContextMenu.open();
    }

    public void exportPng(){
        this.designerCanvas.exportPng();
    }

    public static Tooltip getTooltip(Component component, String message, TooltipPosition position, TooltipAlignment alignment)
    {
        Tooltip tooltip = new Tooltip();

        tooltip.getElement().getStyle().set("background-color", "#232F34");
        tooltip.getElement().getStyle().set("color", "#FFFFFF");
        tooltip.getElement().getStyle().set("border-radius", "10px");
        tooltip.getElement().getStyle().set("padding", "10px");
        tooltip.getElement().getStyle().set("font-size", "8pt");
        tooltip.getElement().getStyle().set("z-index", "100");

        tooltip.attachToComponent(component);

        tooltip.setPosition(position);
        tooltip.setAlignment(alignment);

        tooltip.add(new Paragraph(message));

        return tooltip;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if(this.designerCanvas != null){
            this.redraw();
        }

        UI ui = attachEvent.getUI();
        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if(contextInstanceStateChangeEvent.getContextInstance() != null) {
                logger.info("Updating scheduler visualisation context status. Context Instance[{}], Status[{}], Status Colour[{}]",
                    contextInstanceStateChangeEvent.getContextInstance().getName(), contextInstanceStateChangeEvent.getContextInstance().getStatus().toString(),
                    StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getContextInstance().getStatus()));
                ui.access(() ->
                    this.designerCanvas.setBackgroundColor(contextInstanceStateChangeEvent.getContextInstance().getName()+"_status"
                        , StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getContextInstance().getStatus())));
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.contextInstanceStateChangeRegistration.remove();
        this.contextInstanceStateChangeRegistration = null;
    }
}
