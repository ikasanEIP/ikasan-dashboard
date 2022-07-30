package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.EditMode;
import org.ikasan.dashboard.ui.scheduler.component.FileEventJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.InternalEventDrivenJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.QuartzDrivenScheduledJobDialog;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonToContextTemplateAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonValidationException;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextHelper;
import org.ikasan.designer.CanvasInitialisedListener;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.*;
import org.ikasan.designer.function.SaveFunction;
import org.ikasan.scheduled.context.model.SolrScheduledContextViewRecordImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SchedulerVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener, CanvasInitialisedListener
    , CanvasItemDoubleClickEventListener, ConnectorEventListener, CanvasUpdatedListener, SaveFunction {
    private Logger logger = LoggerFactory.getLogger(SchedulerVisualisation.class);

    private DesignerCanvas designerCanvas;

    private String dynamicImagePath;

    private ContextTemplate contextTemplate;
    private ContextTemplate parentContextTemplate;

    private boolean initialised = false;

    private ContextTemplateDraw2dAdapter adapter = new ContextTemplateDraw2dAdapter();

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
    private ContextProfileService contextProfileService;
    private UserService userService;
    private SecurityService securityService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private JobProvisionService jobProvisionService;
    private ScheduledContextService scheduledContextService;

    private ScheduledContextViewRecord scheduledContextViewRecord;

    private Dialog parent;

    private boolean edit;

    private IkasanAuthentication authentication;

    public SchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                  JobInitiationService jobInitiationService, ContextProfileService contextProfileService, UserService userService, SecurityService securityService,
                                  ScheduledContextInstanceService scheduledContextInstanceService, JobProvisionService jobProvisionService, ScheduledContextService scheduledContextService) {

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

        this.contextProfileService = contextProfileService;
        if(this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }

        this.userService = userService;
        if(this.userService == null) {
            throw new IllegalArgumentException("userService cannot be null!");
        }

        this.securityService = securityService;
        if(this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }

        this.jobProvisionService = jobProvisionService;
        if(this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();
        this.setId("schedulerVisualisation");
    }

    /**
     * @param contextTemplate
     */
    public void createSchedulerVisualisation(ContextTemplate parentContext, ContextTemplate contextTemplate, Dialog parent, boolean edit) throws IOException {
        this.parentContextTemplate = parentContext;
        this.contextTemplate = contextTemplate;
        this.parent = parent;
        this.initialised = false;
        this.edit = edit;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.scheduledContextViewRecord = this.scheduledContextService.getContextView(parentContext.getName(), contextTemplate.getName());

        init();
    }

    private void init() throws IOException {
        if(!initialised && contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas(this, null, "canvas-viewport-"+ UUID.randomUUID().toString(), this.dynamicImagePath, !this.edit);
            this.designerCanvas.addCanvasInitialisedListener(this);

            if(contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
                this.designerCanvas.setCanvasJson(adapter.adaptContext(contextTemplate));
            }
            else if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
                if(this.scheduledContextViewRecord == null) {
                    SearchResults<SchedulerJobRecord> jobs = this.schedulerJobService.findByContext(parentContextTemplate.getName(), -1, -1);

                    Map<String, SchedulerJob> schedulerJobs = jobs.getResultList().stream()
                        .map(record -> record.getJob())
                        .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity()));

                    this.designerCanvas.setCanvasJson(adapter.adaptJobs(contextTemplate, schedulerJobs));
                }
                else {
                    this.designerCanvas.setCanvasJson(this.scheduledContextViewRecord.getContextView());
                }
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addConnectorEventListener(this);
            this.designerCanvas.addCanvasUpdatedListener(this);

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

        // Bring selected items to front
        IronIcons.Icon toFront = IconDecorator.decorate(IronIcons.FLIP_TO_FRONT.create(), getTranslation("tooltip.bring-to-front", UI.getCurrent().getLocale()), "25px", IkasanColours.IKASAN_ORANGE);
        toFront.addClickListener(buttonClickEvent -> this.designerCanvas.bringToFront());
        actions.add(toFront);


        // Send selected items to back
        IronIcons.Icon toBack = IconDecorator.decorate(IronIcons.FLIP_TO_BACK.create(), getTranslation("tooltip.send-to-back", UI.getCurrent().getLocale()), "25px", IkasanColours.IKASAN_ORANGE);
        toBack.addClickListener(buttonClickEvent -> {
            this.designerCanvas.sendToBack();
        });
        actions.add(toBack);

        actions.setVerticalComponentAlignment(Alignment.END, zoomIn, zoomOut, toFront, toBack);
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
                this.parentContextTemplate);

            if(contextTemplate != null && contextTemplate.getScheduledJobs() != null) {
                this.designerCanvas.deselectAllFigures();
                this.openJobVisualisation(contextTemplate);
            }
            else if(contextTemplate != null) {
                this.designerCanvas.deselectAllFigures();
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

    public void addAndGrouping() {
        this.designerCanvas.addBoundaryStyled("AND-"+UUID.randomUUID().toString(), 200, 200, "--", IkasanColours.SCHEDULER_AND, 3);
    }

    public void addOrGrouping() {
        this.designerCanvas.addBoundaryStyled("OR-"+UUID.randomUUID().toString(), 200, 200, "--..", IkasanColours.SCHEDULER_OR, 3);
    }

    public void save() {
        this.designerCanvas.save("", "", "");
    }

    private void openJobVisualisation(ContextTemplate contextTemplate) {
        try {
            JobTemplateVisualisationDialog jobTemplateVisualisationDialog = new JobTemplateVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                this.schedulerJobService, this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService,
                this.contextProfileService, this.userService, this.securityService, this.scheduledContextInstanceService, this.jobProvisionService, this.scheduledContextService);
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
                this.schedulerJobService, this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService,
                this.contextProfileService, this.userService, this.securityService, this.scheduledContextInstanceService, this.jobProvisionService,
                this.scheduledContextService);
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

    public void addJob(SchedulerJob schedulerJob) {
        this.parentContextTemplate.getScheduledJobsMap().put(schedulerJob.getIdentifier(), schedulerJob);
        this.contextTemplate.getScheduledJobsMap().put(schedulerJob.getIdentifier(), schedulerJob);
        this.designerCanvas.addImageFigure(adapter.adaptJob(schedulerJob));
        this.designerCanvas.addLabelToFigure(schedulerJob.getIdentifier(), schedulerJob.getJobName());
    }

    @Override
    public void canvasInitialised() {
        if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty() && this.scheduledContextViewRecord == null) {
            this.contextTemplate.getScheduledJobs().forEach(job -> this.designerCanvas.addLabelToFigure(job.getIdentifier(), job.getJobName()));
        }
        else {
            Map<String, Context> contextMap = ContextHelper.getAllContexts(this.contextTemplate);
            contextMap.entrySet().forEach(entry -> this.designerCanvas.addLabelToFigure(entry.getKey(), entry.getKey()));
        }
    }

    @Override
    public void connectorEvent(ConnectorEvent connectorEvent) {
        if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            if(connectorEvent.getEventType().equals(ConnectorEvent.CONNECTOR_ADDED_EVENT_TYPE)) {
            }
        }
    }

    @Override
    public void canvasUpdated(CanvasUpdatedEvent canvasUpdatedEvent) {
        logger.info(canvasUpdatedEvent.getCanvasJson());
    }

    @Override
    public void save(String id, String name, String description, String payload) {
        try {
            CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
            adapter.validate(payload);

            this.contextTemplate = adapter.adapt(this.contextTemplate.getName(), payload);

            logger.info(this.parentContextTemplate.toString());

            ContextHelper.replaceChildContextTemplate(this.parentContextTemplate, this.contextTemplate);

            if(this.scheduledContextViewRecord == null) {
                this.scheduledContextViewRecord = new SolrScheduledContextViewRecordImpl();
                this.scheduledContextViewRecord.setParentContextName(this.parentContextTemplate.getName());
                this.scheduledContextViewRecord.setContextName(this.contextTemplate.getName());
                this.scheduledContextViewRecord.setTimestamp(System.currentTimeMillis());
            }

            this.scheduledContextViewRecord.setContextView(payload);
            this.scheduledContextViewRecord.setModifiedBy(authentication.getName());

            this.scheduledContextService.saveContextView(scheduledContextViewRecord);

            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findByName(this.parentContextTemplate.getName());
            scheduledContextRecord.setContext(this.parentContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            NotificationHelper.showUserNotification("Context Saved!");
        }
        catch (CanvasJsonValidationException e) {
            ConfirmDialog errorDialog = new ConfirmDialog();
            errorDialog.setHeader("Cannot save context!");
            errorDialog.setWidth("600px");


            StringBuffer message = new StringBuffer();
            message.append("<b style=\"color:red\">An error has occurred! The following items overlap.</b><br/><ul>");

            e.getOverlappingItems().forEach(item -> {
                String value;

                if(item.getUserData() != null && item.getUserData().getJobName() != null && !item.getUserData().getJobName().isEmpty()) {
                    value = item.getUserData().getJobName();
                }
                else {
                    value = item.getId();
                }

                message.append("<li>" + value + "</li>");
            });

            errorDialog.setText(new Html("<div>"+message.toString()+"<ul></div>"));
            errorDialog.setConfirmText("OK");
            errorDialog.open();
        }
    }

    public ContextTemplate getContextTemplate() {
        return contextTemplate;
    }
}
