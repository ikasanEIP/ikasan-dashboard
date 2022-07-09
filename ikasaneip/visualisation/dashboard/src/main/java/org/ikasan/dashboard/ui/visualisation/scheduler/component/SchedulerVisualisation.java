package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.scheduler.component.EditMode;
import org.ikasan.dashboard.ui.scheduler.component.FileEventJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.InternalEventDrivenJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.QuartzDrivenScheduledJobDialog;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextHelper;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.job.orchestration.model.context.ContextImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
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

public class SchedulerVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener {
    private Logger logger = LoggerFactory.getLogger(SchedulerVisualisation.class);

    private DesignerCanvas designerCanvas;

    private String dynamicImagePath;

    private ContextTemplate contextTemplate;
    private ContextTemplate parentContextTemplate;

    private boolean initialised = false;

    private ContextDraw2dAdapter adapter = new ContextDraw2dAdapter();

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

    private Dialog parent;

    public SchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                  JobInitiationService jobInitiationService) {

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

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();
        this.setId("schedulerVisualisation");
    }

    /**
     * @param contextTemplate
     */
    public void createSchedulerVisualisation(ContextTemplate parentContext, ContextTemplate contextTemplate, Dialog parent) throws IOException {
        this.parentContextTemplate = parentContext;
        this.contextTemplate = contextTemplate;
        this.parent = parent;
        this.initialised = false;
        init();
    }

    private void init() throws IOException {
        if(!initialised && contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas("canvas-viewport-"+ UUID.randomUUID().toString(), this.dynamicImagePath, true);

            if(contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
                this.designerCanvas.setCanvasJson(adapter.adaptContext(contextTemplate));
            }
            else if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
                this.designerCanvas.setCanvasJson(adapter.adaptJobs(contextTemplate));
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);

            this.designerCanvas.manageClickableItems();

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

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

//        // Export as selected format
//        Button download = new Button();
//        download.getElement().appendChild(IronIcons.FILE_DOWNLOAD.create().getElement());
//        Tooltip downloadTooltip = getTooltip(download, getTranslation("tooltip.export-png", UI.getCurrent().getLocale())
//            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
//        actions.add(download, downloadTooltip);
//        download.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
//            this.exportPng();
//        });

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
        this.redraw();
    }

    public void redraw() {

    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {

        if(canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            ContextTemplate contextTemplate = ContextHelper.getChildContextTemplate(canvasItemDoubleClickEvent.getFigure().getIdentifier(),
                this.contextTemplate);

            if(contextTemplate != null && contextTemplate.getScheduledJobs() != null) {
                this.openJobVisualisation(contextTemplate);
            }
            else if(contextTemplate != null) {
                this.openContextVisualisation(contextTemplate);
            }
            else {
                this.openJobDialog(canvasItemDoubleClickEvent.getFigure().getIdentifier());
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

    private void openJobVisualisation(ContextTemplate contextTemplate) {
        try {
            JobTemplateVisualisationDialog jobTemplateVisualisationDialog = new JobTemplateVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                this.schedulerJobService, this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService);
            jobTemplateVisualisationDialog.createSchedulerVisualisation(this.parentContextTemplate, contextTemplate);
            jobTemplateVisualisationDialog.open();

            if(parent != null) {
                parent.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openContextVisualisation(ContextTemplate contextTemplate) {
        try {
            ContextTemplateVisualisationDialog contextTemplateVisualisationDialog
                = new ContextTemplateVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                this.schedulerJobService, this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService);
            contextTemplateVisualisationDialog.createSchedulerVisualisation(this.parentContextTemplate, contextTemplate);
            contextTemplateVisualisationDialog.open();

            if(parent != null) {
                parent.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openJobDialog(String identifier) {
        SchedulerJob schedulerJob = this.contextTemplate.getScheduledJobsMap().get(identifier);

        SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextIdAndJobName
            (this.parentContextTemplate.getName(), schedulerJob.getJobName());

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
    protected void onAttach(AttachEvent attachEvent) {
        if(this.designerCanvas != null){
            this.redraw();
        }
    }
}
