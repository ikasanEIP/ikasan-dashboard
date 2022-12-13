package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.EditMode;
import org.ikasan.dashboard.ui.scheduler.component.FileEventJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.InternalEventDrivenJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.QuartzDrivenScheduledJobDialog;
import org.ikasan.dashboard.ui.scheduler.listener.NewContextListener;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcaster;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonToContextTemplateAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonValidationException;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateDraw2dAdapter;
import org.ikasan.designer.CanvasInitialisedListener;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.*;
import org.ikasan.designer.function.SaveFunction;
import org.ikasan.designer.model.UserData;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class SchedulerVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener, CanvasInitialisedListener
    , CanvasItemDoubleClickEventListener, ConnectorEventListener, CanvasUpdatedListener, SaveFunction, NewContextListener, FigureDeleteEventListener
    , FigureUndoDeleteEventListener, CanvasItemSingleClickEventListener {
    private Logger logger = LoggerFactory.getLogger(SchedulerVisualisation.class);

    protected DesignerCanvas designerCanvas;

    protected String dynamicImagePath;

    protected ContextTemplate contextTemplate;
    protected ContextTemplate parentContextTemplate;

    protected boolean initialised = false;

    protected ContextTemplateDraw2dAdapter adapter = new ContextTemplateDraw2dAdapter();

    protected ModuleMetaDataService moduleMetaDataService;
    protected ScheduledProcessManagementService scheduledProcessManagementService;
    protected ConfigurationService configurationRestService;
    protected ModuleControlService moduleControlRestService;
    protected MetaDataService metaDataRestService;
    protected SystemEventLogger systemEventLogger;
    protected SchedulerJobService schedulerJobService;
    protected LogStreamingService logStreamingService;
    protected JobInitiationService jobInitiationService;
    protected ContextProfileService contextProfileService;
    protected UserService userService;
    protected SecurityService securityService;
    protected JobProvisionService jobProvisionService;
    protected ScheduledContextService scheduledContextService;

    protected ScheduledContextViewRecord scheduledContextViewRecord;

    protected Dialog parent;

    protected boolean edit;

    protected IkasanAuthentication authentication;

    protected Map<String, ContextDeletedHolder> contextDeletedHolderMap = new HashMap<>();

    protected List<String> nodeConnectionIndicators = new ArrayList<>();

    protected Map<String, String> schedulerJobExecutionEnvironmentLabel;

    public SchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, JobInitiationService jobInitiationService, ContextProfileService contextProfileService,
                                  UserService userService, SecurityService securityService, JobProvisionService jobProvisionService, ScheduledContextService scheduledContextService,
                                  Map<String, String> schedulerJobExecutionEnvironmentLabel) {

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

        this.jobProvisionService = jobProvisionService;
        if(this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;

        this.setMargin(false);
        this.setSpacing(false);
        this.setPadding(false);
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
            logger.warn("Could not initialise scheduler visualisation!", e);
        }
        this.redraw();
    }

    public void redraw() {

    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        if(canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            String identifier = ContextHelper.getIdentifier(canvasItemDoubleClickEvent.getFigure().getIdentifier());

            ContextTemplate contextTemplate = ContextHelper.getChildContextTemplate(identifier,
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
                this.openJobDialog(identifier);
            }
        }
    }

    @Override
    public void singleClickEvent(CanvasItemSingleClickEvent canvasItemDoubleClickEvent) {
        if(canvasItemDoubleClickEvent.getFigure() != null && canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            String identifier = ContextHelper.getIdentifier(canvasItemDoubleClickEvent.getFigure().getIdentifier());
            logger.info("Click event - " + identifier);
            SchedulerJob job = this.contextTemplate.getScheduledJobsMap()
                .get(identifier);

            if(job != null) {
                this.nodeConnectionIndicators.forEach(nodeConnectionIndicator
                    -> this.designerCanvas.removeFigure(nodeConnectionIndicator));

                this.nodeConnectionIndicators.clear();
                LinkedList<List<SchedulerJob>> jobs
                    = ContextHelper.traceJobThroughContext(this.parentContextTemplate, job.getJobName()
                    , this.contextTemplate.getName());

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

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {

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
                this.schedulerJobService, this.logStreamingService, this.jobInitiationService,
                this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService, this.schedulerJobExecutionEnvironmentLabel);
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
                this.schedulerJobService, this.logStreamingService, this.jobInitiationService,
                this.contextProfileService, this.userService, this.securityService, this.jobProvisionService,
                this.scheduledContextService, this.schedulerJobExecutionEnvironmentLabel);
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

        if(schedulerJob == null) return;

        SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextNameAndJobName
            (this.parentContextTemplate.getName(), schedulerJob.getJobName());

        if(schedulerJobRecord.getJob() instanceof InternalEventDrivenJob) {
            InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(moduleMetaDataService.findById(schedulerJob.getAgentName())
                , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService
                , this.contextTemplate, schedulerJobExecutionEnvironmentLabel);

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
        this.designerCanvas.addImageFigure(adapter.adaptJob(schedulerJob));
        this.designerCanvas.addLabelToFigure(schedulerJob.getIdentifier(), schedulerJob.getJobName());
        this.contextTemplate.getScheduledJobsMap().put(schedulerJob.getIdentifier(), schedulerJob);
    }

    @Override
    public void canvasInitialised() {
        this.designerCanvas.manageClickableItems();
    }

    @Override
    public void connectorEvent(ConnectorEvent connectorEvent) {
        logger.info(connectorEvent.getEventType());
        if(connectorEvent.getEventType().equals("CONNECTOR_ADDED")) {
            if(connectorEvent.getSourceUserData() != null && connectorEvent.getSourceUserData().getItemType().equals(UserData.CONTEXT)
                && connectorEvent.getTargetUserData() != null && connectorEvent.getTargetUserData().getItemType().equals(UserData.CONTEXT)) {
                ContextTemplate childContextTemplate = ContextHelper.getChildContextTemplate(connectorEvent.getTargetUserData().getContextName()
                    , this.parentContextTemplate);
                ContextHelper.removeChildContextTemplate(connectorEvent.getTargetUserData().getContextName(), this.parentContextTemplate);

                ContextTemplate contextTemplate = ContextHelper.getChildContextTemplate(connectorEvent.getSourceUserData().getContextName(), this.parentContextTemplate);

                contextTemplate.getContexts().add(childContextTemplate);
                contextTemplate.getContextsMap().put(childContextTemplate.getName(),childContextTemplate);

                this._save();
            }
        }
    }

    @Override
    public void figureDeleted(FigureDeleteEvent figureDeleteEvent) {
        if(figureDeleteEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
            logger.info("Context deleted! " + figureDeleteEvent.getFigure());
            ContextTemplate parent = ContextHelper.getParentContextTemplate(figureDeleteEvent.getFigure().getUserData().getContextName(), this.parentContextTemplate);
            ContextTemplate removed = ContextHelper.getChildContextTemplate(figureDeleteEvent.getFigure().getUserData().getContextName(), this.parentContextTemplate);
            ContextHelper.removeChildContextTemplate(figureDeleteEvent.getFigure().getUserData().getContextName(), this.parentContextTemplate);

            ContextDeletedHolder contextDeletedHolder = new ContextDeletedHolder(parent, removed);
            this.contextDeletedHolderMap.put(figureDeleteEvent.getFigure().getUserData().getContextName(), contextDeletedHolder);

            this._save();
        }
    }

    @Override
    public void undoFigureDeleted(FigureUndoDeleteEvent figureUndoDeleteEvent) {
        if(figureUndoDeleteEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
            logger.info("Context undo delete! " + figureUndoDeleteEvent.getFigure());
            ContextDeletedHolder contextDeletedHolder = this.contextDeletedHolderMap.get(figureUndoDeleteEvent.getFigure().getUserData().getContextName());
            if(contextDeletedHolder != null) {
                ContextTemplate parent = ContextHelper.getChildContextTemplate(contextDeletedHolder.parent.getName(), this.parentContextTemplate);
                parent.getContexts().add(contextDeletedHolder.getDeleted());
                this.contextDeletedHolderMap.remove(figureUndoDeleteEvent.getFigure().getUserData().getContextName());

                this._save();
            }
        }
    }

    @Override
    public void canvasUpdated(CanvasUpdatedEvent canvasUpdatedEvent) {
        logger.info(canvasUpdatedEvent.getCanvasJson());
    }

    @Override
    public void newContext(ContextTemplate context) {
        if(ContextHelper.getChildContextTemplate(context.getName(), this.parentContextTemplate) != null) {
            ConfirmDialog errorDialog = new ConfirmDialog();
            errorDialog.setHeader(getTranslation("error-dialog-header.cannot-add-context", UI.getCurrent().getLocale()));
            errorDialog.setWidth("500px");

            StringBuffer message = new StringBuffer();
            message.append("<p style=\"color:red\">" + getTranslation("error-dialog-body.cannot-add-context", UI.getCurrent().getLocale()) +
                "</p>");
            errorDialog.setText(new Html("<div>"+message.toString()+"</div>"));
            errorDialog.setConfirmText(getTranslation("button.ok", UI.getCurrent().getLocale()));
            errorDialog.open();
            return;
        }

        this.contextTemplate = ContextHelper.getChildContextTemplate(this.contextTemplate.getName(), this.parentContextTemplate);
        this.contextTemplate.getContexts().add(context);
        this.contextTemplate.getContextsMap().put(context.getName(), context);

        this.designerCanvas.addImageFigure(adapter.adaptChildContext(context));
        this.designerCanvas.addLabelToFigure(context.getName(), context.getName());
        this.designerCanvas.manageClickableItems();

        ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findByName(this.parentContextTemplate.getName());
        scheduledContextRecord.setContext(parentContextTemplate);
        this.scheduledContextService.save(scheduledContextRecord);
    }

    private void _save() {
        ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findByName(this.parentContextTemplate.getName());
        scheduledContextRecord.setContext(parentContextTemplate);
        this.scheduledContextService.save(scheduledContextRecord);
    }

    @Override
    public void save(String id, String name, String description, String payload) {
        try {
            CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
            adapter.validate(payload);

            ContextTemplate updatedContext = adapter.adapt(this.contextTemplate.getName(), payload);

            Map<String, SchedulerJob> jobsToSave = new HashMap<>();
            this.contextTemplate.getScheduledJobs().forEach(job -> {
                SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextNameAndJobName(this.parentContextTemplate.getName(), job.getJobName());
                SchedulerJob schedulerJob = schedulerJobRecord.getJob();
                schedulerJob.getChildContextNames().remove(this.contextTemplate.getName());

                jobsToSave.put(job.getIdentifier(), schedulerJob);
            });

            updatedContext.getScheduledJobs().forEach(job -> {
                if(jobsToSave.containsKey(job.getIdentifier())) {
                    jobsToSave.get(job.getIdentifier()).getChildContextNames().add(updatedContext.getName());
                }
                else {
                    SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextNameAndJobName(this.parentContextTemplate.getName(), job.getJobName());
                    SchedulerJob schedulerJob = schedulerJobRecord.getJob();
                    if(schedulerJob.getChildContextNames() == null) {
                        schedulerJob.setChildContextNames(new ArrayList<>());
                    }
                    schedulerJob.getChildContextNames().add(updatedContext.getName());
                    jobsToSave.put(job.getIdentifier(), schedulerJob);
                }
            });

            this.schedulerJobService.save(jobsToSave.values().stream()
                .collect(Collectors.toList()), authentication.getName());
            logger.info(this.parentContextTemplate.toString());

            ContextHelper.replaceChildContextTemplate(this.parentContextTemplate, updatedContext);
            this.contextTemplate = updatedContext;

//            if(this.scheduledContextViewRecord == null) {
//                this.scheduledContextViewRecord = new SolrScheduledContextViewRecordImpl();
//                this.scheduledContextViewRecord.setParentContextName(this.parentContextTemplate.getName());
//                this.scheduledContextViewRecord.setContextName(this.contextTemplate.getName());
//                this.scheduledContextViewRecord.setTimestamp(System.currentTimeMillis());
//            }
//
//            this.scheduledContextViewRecord.setContextView(payload);
//            this.scheduledContextViewRecord.setModifiedBy(authentication.getName());
//
//            this.scheduledContextService.saveContextView(scheduledContextViewRecord);

            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findByName(this.parentContextTemplate.getName());
            scheduledContextRecord.setContext(this.parentContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            ContextTemplateSavedEventBroadcaster.broadcast(this.parentContextTemplate);
            NotificationHelper.showUserNotification("Context Saved!");
        }
        catch (CanvasJsonValidationException e) {
            ConfirmDialog errorDialog = new ConfirmDialog();
            errorDialog.setHeader("Cannot save context!");
            errorDialog.setWidth("600px");


            StringBuffer message = new StringBuffer();
            message.append("<b style=\"color:red\">" + getTranslation("error-dialog-body.cannot-save-context", UI.getCurrent().getLocale()) + "</b><br/><ul>");

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
            errorDialog.setConfirmText(getTranslation("button.ok", UI.getCurrent().getLocale()));
            errorDialog.open();
        }
    }

    public ContextTemplate getContextTemplate() {
        return contextTemplate;
    }

    private class ContextDeletedHolder {
        private ContextTemplate parent;
        private ContextTemplate deleted;

        public ContextDeletedHolder(ContextTemplate parent, ContextTemplate deleted) {
            this.parent = parent;
            this.deleted = deleted;
        }

        public ContextTemplate getParent() {
            return parent;
        }

        public ContextTemplate getDeleted() {
            return deleted;
        }
    }
}
