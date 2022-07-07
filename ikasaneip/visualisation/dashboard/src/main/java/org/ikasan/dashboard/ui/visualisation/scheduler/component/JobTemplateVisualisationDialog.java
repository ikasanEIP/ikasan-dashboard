package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextDraw2dAdapter;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class JobTemplateVisualisationDialog extends AbstractCloseableResizableDialog implements CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener {

    private Logger logger = LoggerFactory.getLogger(JobTemplateVisualisationDialog.class);

//    private Registration schedulerJobStateChangeRegistration;

    private DesignerCanvas designerCanvas;
    private VerticalLayout layout;

    private ContextDraw2dAdapter adapter = new ContextDraw2dAdapter();

    private boolean initialised = false;

    private ContextTemplate rootContextTemplate;
    private ContextTemplate contextTemplate;

    private String dynamicImagePath = ".";

    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobService schedulerJobService;
    private LogStreamingService logStreamingService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private JobInitiationService jobInitiationService;

    private ContextService contextService = new ContextService();

    public JobTemplateVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                          SchedulerJobService schedulerJobService, LogStreamingService logStreamingService,
                                          SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService) {
        this.setHeight("90%");
        this.setWidth("90%");

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

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
    }

    /**
     * @param contextTemplate
     */
    public void createSchedulerVisualisation(ContextTemplate rootContextTemplate, ContextTemplate contextTemplate) throws IOException {
        this.rootContextTemplate = rootContextTemplate;
        this.contextTemplate = contextTemplate;
        this.initialised = false;
        init();
    }

    private void init() throws IOException{
        if(!initialised && this.contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas("job-viewport-"+ UUID.randomUUID().toString(), this.dynamicImagePath, true);
            this.designerCanvas.setCanvasJson(adapter.adaptJobs(contextTemplate));
            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);

            this.designerCanvas.manageClickableItems();

            ContextTemplate parentContextInstance = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

            if(parentContextInstance != null) {
                Button gotoParentButton = new Button("Go to Parent - " + parentContextInstance.getName(), VaadinIcon.ARROW_UP.create());
                gotoParentButton.setIconAfterText(true);
                gotoParentButton.addClickListener(buttonClickEvent -> {
                    if (this.contextTemplate != null && this.rootContextTemplate != null) {
                        this.contextTemplate = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

                        if (this.contextTemplate != null) {
                            try {
                                this.close();
                                ContextTemplateVisualisationDialog contextTemplateVisualisationDialog
                                    = new ContextTemplateVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService, this.configurationRestService
                                    , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService
                                    , this.schedulerJobInstanceService, this.jobInitiationService);

                                contextTemplateVisualisationDialog.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate);
                                contextTemplateVisualisationDialog.open();

                                this.close();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                layout.add(gotoParentButton);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, gotoParentButton);
            }

            this.layout.add(initCanvasActions(), designerCanvas);

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
        zoomInButton.addClickListener(event -> this.designerCanvas.zoomIn());
        Tooltip zoomInButtonTooltip = getTooltip(zoomInButton, getTranslation("tooltip.zoom-in", UI.getCurrent().getLocale())
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        actions.add(zoomInButton, zoomInButtonTooltip);

        // Zoom out
        Button zoomOutButton = new Button();
        zoomOutButton.getElement().appendChild(IronIcons.ZOOM_OUT.create().getElement());
        zoomOutButton.addClickListener(event -> this.designerCanvas.zoomOut());
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

    public void exportPng(){
        this.designerCanvas.exportPng();
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        logger.info(canvasItemDoubleClickEvent.toString());
        SchedulerJob schedulerJob = this.contextTemplate.getScheduledJobsMap().get(canvasItemDoubleClickEvent.getFigure().getIdentifier());

        SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextIdAndJobName
            (this.rootContextTemplate.getName(), schedulerJob.getJobName());

        if(schedulerJobRecord.getJob() instanceof InternalEventDrivenJob) {
            InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(moduleMetaDataService.findById(schedulerJob.getAgentName())
                , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);

            internalEventDrivenJobDialog.setJob(schedulerJobRecord, EditMode.EDIT);
            internalEventDrivenJobDialog.open();
        }
        else if(schedulerJobRecord.getJob() instanceof FileEventDrivenJob) {
            FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(moduleMetaDataService.findById(schedulerJob.getAgentName()), this.scheduledProcessManagementService
                , this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);
            fileEventJobDialog.setJob(schedulerJobRecord, EditMode.EDIT);

            fileEventJobDialog.open();
        }
        else {
            QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(moduleMetaDataService.findById(schedulerJob.getAgentName())
                , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService
                , systemEventLogger, this.schedulerJobService);
            quartzDrivenScheduledJobDialog.setJob(schedulerJobRecord, EditMode.EDIT);

            quartzDrivenScheduledJobDialog.open();
        }
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {
        logger.info(canvasItemRightClickEvent.toString());

//        JobContextMenu jobContextMenu = new JobContextMenu(canvasItemRightClickEvent.getClickLocationX(), canvasItemRightClickEvent.getClickLocationY(),
//            this.contextTemplate.getScheduledJobsMap().get(canvasItemRightClickEvent.getFigure().getIdentifier()), this.systemEventLogger,
//            this.moduleMetaDataService, this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
//            this.metaDataRestService, this.schedulerJobService);
//        jobContextMenu.open();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
//        UI ui = attachEvent.getUI();
//        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(schedulerJobInstanceStateChangeEvent -> {
//            if (schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance() != null) {
//                logger.info("Updating scheduler visualisation job status. Scheduler Job Instance[{}], Status[{}], Status Colour[{}]",
//                    schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus().toString(),
//                    StatusColours.getInstanceStatusColour(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus()));
//                ui.access(() ->
//                    this.designerCanvas.setBackgroundColor(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier() + "_status"
//                        , StatusColours.getInstanceStatusColour(schedulerJobInstanceStateChangeEvent.getSchedulerJobInstance().getStatus())));
//            }
//        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
//        this.schedulerJobStateChangeRegistration.remove();
//        this.schedulerJobStateChangeRegistration = null;
    }
}
