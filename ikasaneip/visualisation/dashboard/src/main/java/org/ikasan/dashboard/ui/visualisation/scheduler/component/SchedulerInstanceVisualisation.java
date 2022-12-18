package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.scheduler.component.FileEventJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.component.InternalEventDrivenJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.component.QuartzDrivenScheduledJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.listener.ContextOpenedListener;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.CanvasInitialisedListener;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.*;
import org.ikasan.designer.model.UserData;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public abstract class SchedulerInstanceVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener, CanvasInitialisedListener, CanvasItemSingleClickEventListener {
    private Logger logger = LoggerFactory.getLogger(SchedulerInstanceVisualisation.class);

    protected Registration contextInstanceStateChangeRegistration;
    protected Registration schedulerJobStateChangeRegistration;
    protected DesignerCanvas designerCanvas;

    protected String dynamicImagePath;

    protected ContextInstance contextInstance;

    protected boolean initialised = false;

    protected ContextInstanceDraw2dAdapter adapter = new ContextInstanceDraw2dAdapter();

    protected ModuleMetaDataService moduleMetaDataService;
    protected ScheduledProcessManagementService scheduledProcessManagementService;
    protected ConfigurationService configurationRestService;
    protected ModuleControlService moduleControlRestService;
    protected MetaDataService metaDataRestService;
    protected SystemEventLogger systemEventLogger;
    protected LogStreamingService logStreamingService;
    protected SchedulerJobInstanceService schedulerJobInstanceService;
    protected JobInitiationService jobInitiationService;
    protected JobUtilsService jobUtilsService;
    protected ContextInstance parentContextInstance;
    protected ScheduledContextService scheduledContextService;

    protected ScheduledContextViewRecord scheduledContextViewRecord;
    protected Dialog parent;
    protected List<String> nodeConnectionIndicators = new ArrayList<>();
    private List<ContextOpenedListener> contextOpenedListeners;
    private List<CanvasInitialisedListener> canvasInitialisedListeners;

    public SchedulerInstanceVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                          LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                          JobInitiationService jobInitiationService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService) {

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

        this.logStreamingService = logStreamingService;
        if(this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }

        this.jobInitiationService = jobInitiationService;
        if(this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }

        this.jobUtilsService = jobUtilsService;
        if(this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.contextOpenedListeners = new ArrayList<>();
        this.canvasInitialisedListeners = new ArrayList<>();

        this.setMargin(false);
        this.setSpacing(false);
        this.setPadding(false);
        this.setSizeFull();
        this.setId("schedulerVisualisation");
    }

    /**
     *
     * @param parentContextInstance
     * @param contextInstance
     * @throws IOException
     */
    public void createSchedulerVisualisation(ContextInstance parentContextInstance, ContextInstance contextInstance, Dialog parent) throws IOException {
        this.parentContextInstance = parentContextInstance;
        ContextHelper.enrichJobs(this.parentContextInstance);
        this.contextInstance = contextInstance;
        ContextHelper.enrichJobs(this.contextInstance);
        this.parent = parent;
        this.initialised = false;

        this.scheduledContextViewRecord = this.scheduledContextService.getContextView(parentContextInstance.getName(), contextInstance.getName());

        init();
    }

    protected abstract void init() throws IOException;


    protected Component initCanvasActions() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setHeight("40px");
        actions.setId("canvas-actions");
        actions.setWidthFull();

        // Zoom in
        IronIcons.Icon zoomIn = IconDecorator.decorate(IronIcons.ZOOM_IN.create(), getTranslation("tooltip.zoom-in"
            , UI.getCurrent().getLocale()), "25px", IkasanColours.IKASAN_ORANGE);
        zoomIn.addClickListener(event -> this.designerCanvas.zoomIn());
        actions.add(zoomIn);

        // Zoom out
        IronIcons.Icon zoomOut = IconDecorator.decorate(IronIcons.ZOOM_OUT.create(), getTranslation("tooltip.zoom-out"
            , UI.getCurrent().getLocale()), "25px", IkasanColours.IKASAN_ORANGE);
        zoomOut.addClickListener(event -> this.designerCanvas.zoomOut());
        actions.add(zoomOut);

        actions.setVerticalComponentAlignment(Alignment.END, zoomIn, zoomOut);
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
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {

        if(canvasItemDoubleClickEvent.getFigure().getIdentifier() != null &&
            canvasItemDoubleClickEvent.getFigure().getUserData() != null) {
            String identifier = ContextHelper.getIdentifier(canvasItemDoubleClickEvent.getFigure()
                .getUserData().getIdentifier());

            if(ContextMachineCache.instance().containsInstanceIdentifier(this.parentContextInstance.getId())) {
                this.parentContextInstance = ContextMachineCache.instance()
                    .getByContextInstanceId(this.parentContextInstance.getId()).getContext();
            }

            ContextInstance contextInstance = ContextHelper.getChildContextInstance(identifier,
                this.parentContextInstance);

            if(contextInstance != null && contextInstance.getScheduledJobs() != null) {
                this.contextOpenedListeners.forEach(listener -> listener.contextOpened(contextInstance));
            }
//            else if(contextInstance != null){
//                try {
//                    ContextInstanceVisualisationDialog contextInstanceVisualisationDialog
//                        = new ContextInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
//                        this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
//                        this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService,
//                        this.jobUtilsService, this.scheduledContextService);
//                    contextInstanceVisualisationDialog.createSchedulerVisualisation(this.parentContextInstance, contextInstance);
//                    contextInstanceVisualisationDialog.open();
//
//                    if(parent != null) {
//                        parent.close();
//                    }
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }

    @Override
    public void singleClickEvent(CanvasItemSingleClickEvent canvasItemDoubleClickEvent) {
        if(canvasItemDoubleClickEvent.getFigure() != null && canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            String identifier = ContextHelper.getIdentifier(canvasItemDoubleClickEvent.getFigure().getIdentifier());
            logger.debug("Click event - " + identifier);

            this.nodeConnectionIndicators.forEach(nodeConnectionIndicator
                -> this.designerCanvas.removeFigure(nodeConnectionIndicator));

            this.nodeConnectionIndicators.clear();

            if(canvasItemDoubleClickEvent.getFigure().getUserData() != null &&
                canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
                canvasItemDoubleClickEvent.getFigure().getUserData().getSubsequentJobIdentifiers().forEach(id -> {
                    String nodeConnectorIndicator = UUID.randomUUID().toString();
                    this.nodeConnectionIndicators.add(nodeConnectorIndicator);
                    this.designerCanvas.addImageToFigure(id, nodeConnectorIndicator,
                        "frontend/images/mr-squid-head.png", 49.6, 37.8);
                });
            }

            SchedulerJobInstance job = this.contextInstance.getScheduledJobsMap()
                .get(identifier);

            if(job != null) {

                List<String> residingContexts = ContextHelper.getContextsWhereJobFilterMatchResides
                    (this.parentContextInstance, job.getJobName());

                residingContexts.forEach(context -> {
                    Map<String, SchedulerJob> lastJobs = ContextHelper.getJobsOutsideLogicalGrouping(this.contextInstance);

                    if(lastJobs.containsKey(job.getIdentifier())) {
                        String nodeConnectorIndicator = UUID.randomUUID().toString();
                        this.nodeConnectionIndicators.add(nodeConnectorIndicator);
                        this.designerCanvas.addImageToFigure(context + "_out", nodeConnectorIndicator,
                            "frontend/images/mr-squid-head.png", 49.6, 37.8);
                    }
                });

                LinkedList<List<SchedulerJob>> jobs
                    = ContextHelper.traceJobThroughContext(this.parentContextInstance, job.getJobName()
                    , this.contextInstance.getName());

                if (!jobs.isEmpty()) {
                    jobs.get(0).forEach(downstreamJob -> {
                        String nodeConnectorIndicator = UUID.randomUUID().toString();
                        this.nodeConnectionIndicators.add(nodeConnectorIndicator);
                        this.designerCanvas.addImageToFigure(downstreamJob.getIdentifier(), nodeConnectorIndicator,
                            "frontend/images/mr-squid-head.png", 49.6, 37.8);
                    });
                }
            }
        }
    }

    public void addBoundaryToItem(String itemIdentifier, boolean scrollTo) {
        this.nodeConnectionIndicators.forEach(nodeConnectionIndicator
            -> this.designerCanvas.removeFigure(nodeConnectionIndicator));

        this.nodeConnectionIndicators.clear();

        String nodeConnectorIndicator = UUID.randomUUID().toString();
        this.nodeConnectionIndicators.add(nodeConnectorIndicator);
        String boundaryIdentifier = UUID.randomUUID().toString();
        this.nodeConnectionIndicators.add(boundaryIdentifier);
        this.designerCanvas.addBoundaryToFigure(itemIdentifier, boundaryIdentifier, 200, 200,
            "--", IkasanColours.IKASAN_ORANGE_50, scrollTo);
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {
//        JobContextMenu jobContextMenu = new JobContextMenu(canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY());
//        jobContextMenu.open();
    }

    @Override
    public void canvasInitialised() {
        this.designerCanvas.manageClickableItems();
        this.canvasInitialisedListeners.forEach(listener -> listener.canvasInitialised());
        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
            //this.contextInstance.getScheduledJobs().forEach(job -> this.designerCanvas.addLabelToFigure(job.getIdentifier(), job.getJobName()));
//            this.designerCanvas.stopSpinner();
        }
        else {
            Map<String, Context> contextMap = ContextHelper.getAllContexts(this.contextInstance);
            //contextMap.entrySet().forEach(entry -> this.designerCanvas.addLabelToFigure(entry.getKey(), entry.getKey()));
//            this.designerCanvas.stopSpinner();
        }
    }

    /**
     * Add a context open listener.
     *
     * @param contextOpenedListener
     */
    public void addContextOpenListener(ContextOpenedListener contextOpenedListener) {
        this.contextOpenedListeners.add(contextOpenedListener);
    }

    /**
     * Add a canvas initialised listener.
     *
     * @param listener
     */
    public void addCanvasInitialisedListener(CanvasInitialisedListener listener) {
        this.canvasInitialisedListeners.add(listener);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if (contextInstanceStateChangeEvent.getContextInstance() != null) {
                logger.debug("Updating scheduler visualisation context status. Context Instance[{}], Status[{}], Status Colour[{}]",
                    contextInstanceStateChangeEvent.getContextInstance().getName(), contextInstanceStateChangeEvent.getContextInstance().getStatus().toString(),
                    StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getContextInstance().getStatus()));

                ui.access(() -> {
                    if(this.designerCanvas != null) {
                    this.designerCanvas.setBackgroundColor(contextInstanceStateChangeEvent.getContextInstance().getName() + "_status"
                        , StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getContextInstance().getStatus()));
                    }
                });
            }
        });

        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(schedulerJobInstanceStateChangeEvent -> {
            if (schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance() != null
                && this.contextInstance != null
                && this.parentContextInstance != null
                && this.contextInstance.getName().equals(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName())
                && this.parentContextInstance.getId().equals(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId())) {
                logger.info("Updating scheduler visualisation job status. Scheduler Job Instance[{}], Status[{}], Status Colour[{}]",
                    schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus().toString(),
                    StatusColours.getInstanceStatusColour(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus()));

                ui.access(() -> {
                    if(this.designerCanvas != null) {
                        this.designerCanvas.setBackgroundColor(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier() + "_status"
                            , StatusColours.getInstanceStatusColour(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus()));
                    }
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.contextInstanceStateChangeRegistration != null) {
            this.contextInstanceStateChangeRegistration.remove();
            this.contextInstanceStateChangeRegistration = null;
        }

        if(this.schedulerJobStateChangeRegistration != null) {
            this.schedulerJobStateChangeRegistration.remove();
            this.schedulerJobStateChangeRegistration  = null;
        }
    }


}
