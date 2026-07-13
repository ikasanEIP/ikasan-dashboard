package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.scheduler.listener.ContextOpenedListener;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.CanvasInitialisedListener;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.*;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class SchedulerInstanceVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener, CanvasInitialisedListener, CanvasItemSingleClickEventListener, ContextInstanceStateChangeEventLocalBroadcastListener
    , SchedulerJobStateChangeEventLocalBroadcastListener {
    private Logger logger = LoggerFactory.getLogger(SchedulerInstanceVisualisation.class);

    protected DesignerCanvas designerCanvas;

    protected String dynamicImagePath;

    protected ContextInstance contextInstance;

    protected boolean initialised = false;

    protected ContextInstanceDraw2dAdapter adapter;

    protected ModuleMetaDataService moduleMetaDataService;
    protected ConfigurationService configurationRestService;
    protected ModuleControlService moduleControlRestService;
    protected MetaDataService metaDataRestService;
    protected SystemEventLogger systemEventLogger;
    protected LogStreamingService logStreamingService;
    protected SchedulerJobInstanceService schedulerJobInstanceService;
    protected JobInitiationService jobInitiationService;
    protected JobUtilsService jobUtilsService;
    protected GlobalEventService globalEventService;
    protected ContextInstance parentContextInstance;
    protected ScheduledContextService scheduledContextService;
    protected Dialog parent;
    protected List<String> nodeConnectionIndicators = new ArrayList<>();
    private List<ContextOpenedListener> contextOpenedListeners;
    private List<CanvasInitialisedListener> canvasInitialisedListeners;
    private UI ui;

    public SchedulerInstanceVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                          LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                          JobInitiationService jobInitiationService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                          GlobalEventService globalEventService, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                          double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {

        this.dynamicImagePath = dynamicImagePath;
        if (this.dynamicImagePath == null) {
            throw new IllegalArgumentException("dynamicImagePath cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("agent cannot be null!");
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

        this.globalEventService = globalEventService;
        if(this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }

        this.adapter = new ContextInstanceDraw2dAdapter(jobVisualisationVerticalSpacing,
            jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);

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

       init();
    }

    protected abstract void init() throws IOException;


    protected Component initCanvasActions() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setHeight("40px");
        actions.setId("canvas-actions");
        actions.setWidthFull();

        // Zoom in
        Icon zoomIn = IconDecorator.decorate(VaadinIcon.PLUS.create(), getTranslation("tooltip.zoom-in"
            , UI.getCurrent().getLocale()), "18px", IkasanColours.IKASAN_ORANGE);
        zoomIn.addClickListener(event -> this.designerCanvas.zoomIn());
        actions.add(zoomIn);

        // Zoom out
        Icon zoomOut = IconDecorator.decorate(VaadinIcon.MINUS.create(), getTranslation("tooltip.zoom-out"
            , UI.getCurrent().getLocale()), "18px", IkasanColours.IKASAN_ORANGE);
        zoomOut.addClickListener(event -> this.designerCanvas.zoomOut());
        actions.add(zoomOut);

        actions.setVerticalComponentAlignment(Alignment.CENTER, zoomIn, zoomOut);
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

            ContextMachine machine = ContextMachineCache.instance().getByContextInstanceId(this.parentContextInstance.getId());
            if(machine != null) {
                ContextInstance refreshed = machine.getContext();
                if(refreshed != null) this.parentContextInstance = refreshed;
            }

            ContextInstance contextInstance = ContextHelper.getChildContextInstance(identifier,
                this.parentContextInstance);

            if(contextInstance != null && contextInstance.getScheduledJobs() != null) {
                this.contextOpenedListeners.forEach(listener -> listener.contextOpened(contextInstance));
            }
        }
    }

    @Override
    public void singleClickEvent(CanvasItemSingleClickEvent canvasItemDoubleClickEvent) {
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

    }

    @Override
    public void canvasInitialised() {
        this.designerCanvas.manageClickableItems();
        this.canvasInitialisedListeners.forEach(listener -> listener.canvasInitialised());
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
        this.ui = attachEvent.getUI();

        ContextInstanceStateChangeEventBroadcaster.instance().register(this);
        SchedulerJobStateChangeEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.ui = null;

        ContextInstanceStateChangeEventBroadcaster.instance().unregister(this);
        SchedulerJobStateChangeEventBroadcaster.instance().unregister(this);
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if (event.getContextInstance() != null) {
            logger.debug("Updating scheduler visualisation context status. Context Instance[{}], Status[{}], Status Colour[{}]",
                event.getContextInstance().getName(), event.getContextInstance().getStatus().toString(),
                StatusColours.getInstanceStatusColour(event.getContextInstance().getStatus()));

            if(this.ui != null && this.ui.isAttached()) {
                this.ui.access(() -> {
                    if (this.designerCanvas != null) {
                        this.designerCanvas.setBackgroundColor(event.getContextInstance().getName() + "_status"
                            , StatusColours.getInstanceStatusColour(event.getContextInstance().getStatus()));
                    }
                });
            }
        }
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent) {
        if (schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance() != null
            && this.contextInstance != null
            && this.parentContextInstance != null
            && this.contextInstance.getName().equals(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName())
            && this.parentContextInstance.getId().equals(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId())) {
            logger.info("Updating scheduler visualisation job status. Scheduler Job Instance[{}], Status[{}], Status Colour[{}]",
                schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus().toString(),
                StatusColours.getInstanceStatusColour(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus()));

            if(this.ui.isAttached()) {
                this.ui.access(() -> {
                    if (this.designerCanvas != null) {
                        this.designerCanvas.setBackgroundColor(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier() + "_status"
                            , schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().isErrorAcknowledged() != null ?
                                StatusColours.getInstanceStatusColour(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus(), schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().isErrorAcknowledged()) :
                                StatusColours.getInstanceStatusColour(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus()));
                    }
                });
            }
        }
    }
}
