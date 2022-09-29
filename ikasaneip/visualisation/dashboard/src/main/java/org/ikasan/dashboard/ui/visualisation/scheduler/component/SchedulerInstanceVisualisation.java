package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.scheduler.component.FileEventJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.component.InternalEventDrivenJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.component.QuartzDrivenScheduledJobInstanceDialog;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.CanvasInitialisedListener;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
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
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SchedulerInstanceVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener, CanvasInitialisedListener {
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
    protected SchedulerJobService schedulerJobService;
    protected LogStreamingService logStreamingService;
    protected SchedulerJobInstanceService schedulerJobInstanceService;
    protected JobInitiationService jobInitiationService;
    protected JobUtilsService jobUtilsService;
    protected ContextInstance parentContextInstance;
    protected ScheduledContextService scheduledContextService;

    protected ScheduledContextViewRecord scheduledContextViewRecord;

    protected Dialog parent;


    public SchedulerInstanceVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
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

        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
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
        this.contextInstance = contextInstance;
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

        // Zoom in
        IronIcons.Icon zoomIn = IconDecorator.decorate(IronIcons.ZOOM_IN.create(), getTranslation("tooltip.zoom-in", UI.getCurrent().getLocale()), "25px", IkasanColours.IKASAN_ORANGE);
        zoomIn.addClickListener(event -> this.designerCanvas.zoomIn());
        actions.add(zoomIn);

        // Zoom out
        IronIcons.Icon zoomOut = IconDecorator.decorate(IronIcons.ZOOM_OUT.create(), getTranslation("tooltip.zoom-out", UI.getCurrent().getLocale()), "25px", IkasanColours.IKASAN_ORANGE);
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

        if(canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {

            if(ContextMachineCache.instance().containsInstanceIdentifier(this.parentContextInstance.getId())) {
                this.parentContextInstance = ContextMachineCache.instance().getByContextInstanceId(this.parentContextInstance.getId()).getContext();
            }

            ContextInstance contextInstance = ContextHelper.getChildContextInstance(canvasItemDoubleClickEvent.getFigure().getIdentifier(),
                this.parentContextInstance);

            if(contextInstance != null && contextInstance.getScheduledJobs() != null) {
                try {
                    JobInstanceVisualisationDialog jobInstanceVisualisationDialog = new JobInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                        this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                        this.schedulerJobService, this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService);
                    jobInstanceVisualisationDialog.createSchedulerVisualisation(this.parentContextInstance, contextInstance);
                    jobInstanceVisualisationDialog.open();

                    if(parent != null) {
                        parent.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(contextInstance != null){
                try {
                    ContextInstanceVisualisationDialog contextInstanceVisualisationDialog
                        = new ContextInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                        this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                        this.schedulerJobService, this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService,
                        this.jobUtilsService, this.scheduledContextService);
                    contextInstanceVisualisationDialog.createSchedulerVisualisation(this.parentContextInstance, contextInstance);
                    contextInstanceVisualisationDialog.open();

                    if(parent != null) {
                        parent.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                SchedulerJobInstance schedulerJob = this.contextInstance.getScheduledJobsMap().get(canvasItemDoubleClickEvent.getFigure().getIdentifier());

                SchedulerJobInstanceRecord schedulerJobRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName
                    (this.parentContextInstance.getId(), schedulerJob.getJobName(), this.contextInstance.getName());

                if(schedulerJobRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    InternalEventDrivenJobInstanceDialog internalEventDrivenJobInstanceDialog = new InternalEventDrivenJobInstanceDialog(moduleMetaDataService.findById(schedulerJob.getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, this.schedulerJobInstanceService, this.parentContextInstance
                        , this.jobInitiationService, moduleMetaDataService, this.logStreamingService, this.jobUtilsService);

                    internalEventDrivenJobInstanceDialog.setJob(schedulerJobRecord);
                    internalEventDrivenJobInstanceDialog.open();
                }
                else if(schedulerJobRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                    FileEventJobInstanceDialog fileEventJobDialog = new FileEventJobInstanceDialog(moduleMetaDataService.findById(schedulerJob.getAgentName())
                        , this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService);
                    fileEventJobDialog.setJob(schedulerJobRecord);

                    fileEventJobDialog.open();
                }
                else {
                    QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobInstanceDialog(moduleMetaDataService.findById(schedulerJob.getAgentName())
                        , this.jobInitiationService, systemEventLogger, this.schedulerJobInstanceService);
                    quartzDrivenScheduledJobDialog.setJob(schedulerJobRecord);

                    quartzDrivenScheduledJobDialog.open();
                }
            }
        }
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {
//        JobContextMenu jobContextMenu = new JobContextMenu(canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY());
//        jobContextMenu.open();
    }

    @Override
    public void canvasInitialised() {
        this.designerCanvas.manageClickableItems();
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if (contextInstanceStateChangeEvent.getContextInstance() != null) {
                logger.info("Updating scheduler visualisation context status. Context Instance[{}], Status[{}], Status Colour[{}]",
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
