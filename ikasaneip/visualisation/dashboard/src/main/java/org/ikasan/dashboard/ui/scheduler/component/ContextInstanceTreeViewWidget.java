package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.componentfactory.explorer.ExplorerTreeGrid;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobInstanceVisualisationDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ContextInstanceTreeViewWidget extends AbstractGridSchedulerJobInstanceActionWidget {
    private static final String SKIP_ICON = "SKIP_ICON";
    private static final String ENABLE_ICON = "ENABLE_ICON";
    private static final String HOLD_ICON = "HOLD_ICON";
    private static final String RELEASE_ICON = "RELEASE_ICON";
    private static final String LOG_ICON = "LOG_ICON";
    private static final String ERROR_LOG_ICON = "ERROR_LOG_ICON";
    private Logger logger = LoggerFactory.getLogger(ContextInstanceTreeViewWidget.class);
    private Registration schedulerJobStateChangeRegistration;
    private SchedulerJobInstanceService schedulerJobInstanceService;

    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private LogStreamingService logStreamingService;
    private JobInitiationService jobInitiationService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private ContextInstance contextInstance;

    private Map<StatusImageKey, Image> statusImageKeyImageMap;
    private Map<StatusImageKey, Map<String, Icon>> schedulerJobIconMap;

    private ExplorerTreeGrid<Object> grid;

    public ContextInstanceTreeViewWidget(ContextInstance contextInstance, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                         ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                         MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                         LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                         JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService) {
        super(moduleMetaDataService, systemEventLogger, logStreamingService, contextInstance,
             schedulerJobInstanceService);
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if (this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if (this.contextInstance == null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if (this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.configurationRestService = configurationRestService;
        if (this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if (this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.moduleControlRestService = moduleControlRestService;
        if (this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if (this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }
        this.metaDataRestService = metaDataRestService;
        if (this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if (this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if (this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if (this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.statusImageKeyImageMap = new HashMap<>();
        this.schedulerJobIconMap = new HashMap<>();

        ContextHelper.enrichJobs(this.contextInstance);

        setSizeFull();
        TreeGrid<Object> grid = buildGrid();
        super.add(grid);
    }

    private TreeGrid<Object> buildGrid() {
        grid = new ExplorerTreeGrid<>();
        ValueProvider<Object, Collection<Object>> childItemProvider
            = contextInstance -> {
            List<Object> children = new ArrayList<>();
            if(contextInstance instanceof ContextInstance) {
                if (((ContextInstance)contextInstance).getContexts() != null
                    && !((ContextInstance)contextInstance).getContexts().isEmpty()) {
                    children.addAll(((ContextInstance)contextInstance).getContexts().stream()
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                }

                if (((ContextInstance)contextInstance).getScheduledJobs() != null
                    && !((ContextInstance)contextInstance).getScheduledJobs().isEmpty()) {
                    children.addAll(((ContextInstance)contextInstance).getScheduledJobs().stream()
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                }
            }
            return children;
        };

        TreeDataProvider treeDataProvider = new TreeDataProvider<>(
                    new TreeData().addItems(List.of(contextInstance), childItemProvider));

        TextField filterField = new TextField();
        filterField.setWidth("100%");
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        filterField.setSuffixComponent(filterIcon);
        filterField.setValueChangeMode(ValueChangeMode.EAGER);

        filterField.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                treeDataProvider.setFilter(null);
            } else {
                treeDataProvider.setFilter(item -> {
                    if(item instanceof ContextInstance) {
                       return ((ContextInstance)item).getName().toLowerCase()
                            .contains(event.getValue().toLowerCase());
                    }
                    else if (item instanceof SchedulerJobInstance) {
                        return ((SchedulerJobInstance)item).getJobName().toLowerCase()
                            .contains(event.getValue().toLowerCase());
                    }

                    return false;
                });
            }
            grid.expandRecursively(treeDataProvider.getTreeData().getRootItems(),
                99);
        });

        grid.setDataProvider(treeDataProvider);
        grid.addComponentHierarchyColumn(value -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            if(value instanceof ContextInstance) {
                horizontalLayout.add(VaadinIcon.COG.create()
                    , new Label(((ContextInstance)value).getName()));
            }
            if(value instanceof SchedulerJobInstance) {
                SchedulerJobInstance schedulerJobInstance = (SchedulerJobInstance)value;
                SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId()
                    , ((SchedulerJobInstance)value).getJobName(), ((SchedulerJobInstance)value).getChildContextName());

                Image image = new Image(this.getJobImage(schedulerJobInstanceRecord.getSchedulerJobInstance()), "");
                horizontalLayout.add(image);
                image.setHeight("30px");
                this.setImageBackgroundColour(image, schedulerJobInstance.getStatus());
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, image);

                this.statusImageKeyImageMap.put(new StatusImageKey(this.contextInstance.getName()
                    , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName()), image);

                Label jobNameLabel =  new Label(((SchedulerJobInstance)value).getJobName());
                horizontalLayout.add(jobNameLabel);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, jobNameLabel);

            }

            return horizontalLayout;
        })
            .setFlexGrow(1)
            .setHeader(getTranslation("table-header.context-job-name", UI.getCurrent().getLocale()))
            .setKey("name")
            .setResizable(true);

        grid.addComponentColumn(value -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            if (value instanceof ContextInstance) {
                if(((ContextInstance) value).getScheduledJobs() != null
                    && !((ContextInstance) value).getScheduledJobs().isEmpty()) {
                    horizontalLayout.add(this.createContextVisualisationIcon((ContextInstance) value));
                }
            }
            else if(value instanceof SchedulerJobInstance) {

                SchedulerJobInstance schedulerJobInstance = (SchedulerJobInstance)value;
                SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId()
                    , ((SchedulerJobInstance)value).getJobName(), ((SchedulerJobInstance)value).getChildContextName());

                schedulerJobInstanceRecord.setStatus(schedulerJobInstance.getStatus().name());
                schedulerJobInstanceRecord.getSchedulerJobInstance().setStatus(schedulerJobInstance.getStatus());

                StatusImageKey key = new StatusImageKey(this.contextInstance.getName()
                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());


                logger.info(String.format("refreshing icons JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName()
                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getContextName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(),
                    schedulerJobInstanceRecord.getStatus()));
                this.getActionsComponent(schedulerJobInstanceRecord, horizontalLayout);
                this.setIconVisibility(schedulerJobInstanceRecord, key);
            }

            return horizontalLayout;
        })
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(3);

        grid.setSizeFull();
        grid.expand(contextInstance.getContexts());

        HeaderRow hr = grid.appendHeaderRow();
        hr.getCell(grid.getColumnByKey("name")).setComponent(filterField);

        return grid;
    }

    private void setImageBackgroundColour(Image image, InstanceStatus instanceStatus) {
        image.getElement().getStyle().remove("background-color");
        if(instanceStatus.equals(InstanceStatus.COMPLETE)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
        }
        else if(instanceStatus.equals(InstanceStatus.RUNNING)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RUNNING);
        }
        else if(instanceStatus.equals(InstanceStatus.WAITING)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_WAITING);
        }
        else if(instanceStatus.equals(InstanceStatus.ERROR)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
        }
        else if(instanceStatus.equals(InstanceStatus.LOCK_QUEUED)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
        }
        else if(instanceStatus.equals(InstanceStatus.SKIPPED)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
        }
        else if(instanceStatus.equals(InstanceStatus.ON_HOLD)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
        }
    }

    private Icon createContextVisualisationIcon(ContextInstance contextInstance) {
        Icon visualisation = IconDecorator.decorate(new Icon(VaadinIcon.SITEMAP), getTranslation("tooltip.open-visualisation"
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        visualisation.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            JobInstanceVisualisationDialog jobInstanceVisualisationDialog = new JobInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
                this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService);

            if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                this.contextInstance = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId()).getContext();
            }

            ContextInstance childContext = ContextHelper.getChildContextInstance(contextInstance.getName(), this.contextInstance);

            try {
                jobInstanceVisualisationDialog.createSchedulerVisualisation(this.contextInstance, childContext);
                jobInstanceVisualisationDialog.open();
            }
            catch (IOException e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.cannot-open-visualisation", UI.getCurrent().getLocale()));
            }
        });

        return visualisation;
    }

    protected String getJobImage(SchedulerJob schedulerJob) {
        String image = "frontend/images/command_black.png";

        if(schedulerJob instanceof FileEventDrivenJob || schedulerJob instanceof FileEventDrivenJobInstance) {
            image = "frontend/images/file_black.png";
        }
        else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
            image = "frontend/images/time_black.png";
        }

        return image;
    }

    protected Component getActionsComponent(SchedulerJobInstanceRecord schedulerJobInstanceRecord, HorizontalLayout layout) {
        layout.setWidthFull();

        StatusImageKey key = new StatusImageKey(this.contextInstance.getName()
            , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());

        if(!this.schedulerJobIconMap.containsKey(key)) {
            this.schedulerJobIconMap.put(key, new HashMap<>());
        }

        Map<String, Icon> iconMap = this.schedulerJobIconMap.get(key);

        Icon modal = IconDecorator.decorate(new Icon(VaadinIcon.MODAL), getTranslation("tooltip.open-visualisation"
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        modal.addClickListener(event -> {
            SchedulerJobInstanceRecord refreshedRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(schedulerJobInstanceRecord.getContextInstanceId(),
                schedulerJobInstanceRecord.getJobName(), schedulerJobInstanceRecord.getChildContextName());
            if(refreshedRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                FileEventJobInstanceDialog fileEventJobInstanceDialog
                    = new FileEventJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName()),
                    this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService);
                fileEventJobInstanceDialog.setJob(refreshedRecord);
                fileEventJobInstanceDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance) {
                QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobInstanceDialog
                    = new QuartzDrivenScheduledJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName()),
                    this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService);
                quartzDrivenScheduledJobInstanceDialog.setJob(refreshedRecord);
                quartzDrivenScheduledJobInstanceDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                InternalEventDrivenJobInstanceDialog internalEventDrivenJobDialog
                    = new InternalEventDrivenJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName())
                    , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService
                    , this.systemEventLogger, this.schedulerJobInstanceService, this.contextInstance, this.jobInitiationService, this.moduleMetaDataService
                    , this.logStreamingService, this.jobUtilsService);
                internalEventDrivenJobDialog.setJob(refreshedRecord);
                internalEventDrivenJobDialog.open();
            }
        });

        layout.add(modal);

        Icon skip;
        if(!iconMap.containsKey(SKIP_ICON)) {
            skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");

            skip.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> this.skipJob(schedulerJobInstanceRecord));
            });

            iconMap.put(SKIP_ICON, skip);
        }
        else {
            skip = iconMap.get(SKIP_ICON);
        }

        layout.add(skip);

        Icon enable;
        if(!iconMap.containsKey(ENABLE_ICON)) {
            enable = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.enable-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            enable.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE));
            enable.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.enableJob(schedulerJobInstanceRecord));
            });

            iconMap.put(ENABLE_ICON, enable);
        }
        else {
            enable = iconMap.get(ENABLE_ICON);
        }

        layout.add(enable);

        Icon hold;
        if(!iconMap.containsKey(HOLD_ICON)) {
            hold = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            hold.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE));
            hold.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.holdJob(schedulerJobInstanceRecord));
            });

            iconMap.put(HOLD_ICON, hold);
        }
        else {
            hold = iconMap.get(HOLD_ICON);
        }

        layout.add(hold);

        Icon release;
        if(!iconMap.containsKey(RELEASE_ICON)) {
            release = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            release.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE));
            release.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.releaseJob(schedulerJobInstanceRecord));
            });

            iconMap.put(RELEASE_ICON, release);
        }
        else {
            release = iconMap.get(RELEASE_ICON);
        }

        layout.add(release);

        Icon submit = IconDecorator.decorate(new Icon(VaadinIcon.PAPERPLANE), getTranslation("tooltip.submit-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        submit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                InternalEventDrivenJobSubmissionDialog internalEventDrivenJobSubmissionDialog = new InternalEventDrivenJobSubmissionDialog(this.systemEventLogger,
                    this.moduleMetaDataService, this.contextInstance, this.jobInitiationService, (InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance());

                internalEventDrivenJobSubmissionDialog.open();
            }
            else if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-file-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.submit-file-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    try {
                        ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                        this.jobInitiationService.raiseFileEventSchedulerJob(agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getJobName());

                        logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                        this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                            , this.authentication.getName());

                        NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                    }
                });
            }
            else if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance) {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-quartz-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.submit-quartz-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    try {
                        ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                        this.jobInitiationService.raiseQuartzSchedulerJob(agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getJobName());

                        logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                        this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                            , this.authentication.getName());

                        NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                    }
                });
            }
        });

        layout.add(submit);
        submit.setVisible(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.WAITING) ||
            schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
            schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR));

        Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale())
            , "14pt", "rgba(0, 0, 0, 1.0)");
        StreamResource streamResource = new StreamResource(schedulerJobInstanceRecord.getJobName()+".json"
            , () -> {
            try {
                return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedulerJobInstanceRecord.getSchedulerJobInstance()));
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.downloading-job", UI.getCurrent().getLocale()));
                return null;
            }
        });

        FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
        exportWrapper.wrapComponent(export);
        layout.add(exportWrapper);

        Icon logFile;

        if(!iconMap.containsKey(LOG_ICON)) {
            logFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_PROCESS), getTranslation("tooltip.view-log-file"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            logFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                SchedulerJobInstanceRecord refreshedRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(schedulerJobInstanceRecord.getContextInstanceId(),
                    schedulerJobInstanceRecord.getJobName(), schedulerJobInstanceRecord.getChildContextName());
                this.streamLog(refreshedRecord, false);
            });

            iconMap.put(LOG_ICON, logFile);
        }
        else {
            logFile = iconMap.get(LOG_ICON);
        }

        layout.add(logFile);

        Icon errorLogFile;

        if(!iconMap.containsKey(ERROR_LOG_ICON)) {
            errorLogFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_REMOVE), getTranslation("tooltip.view-error-log-file"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            errorLogFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                SchedulerJobInstanceRecord refreshedRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(schedulerJobInstanceRecord.getContextInstanceId(),
                    schedulerJobInstanceRecord.getJobName(), schedulerJobInstanceRecord.getChildContextName());
                this.streamLog(refreshedRecord, true);
            });

            iconMap.put(ERROR_LOG_ICON, errorLogFile);
        }
        else {
            errorLogFile = iconMap.get(ERROR_LOG_ICON);
        }

        layout.add(errorLogFile);

        return layout;
    }

    protected void setIconVisibility(SchedulerJobInstanceRecord schedulerJobInstanceRecord, StatusImageKey key) {
        Map<String, Icon> iconMap = this.schedulerJobIconMap.get(key);
        Icon skip = iconMap.get(SKIP_ICON);


        if (skip != null) {
            if (schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                !((schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.LOCK_QUEUED))) {
                skip.setVisible(true);
            } else {
                skip.setVisible(false);

            }
        }

        Icon enable = iconMap.get(ENABLE_ICON);

        if(enable != null) {
            if (schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                    !(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED) ||
                        schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
                        schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)))) {
                enable.setVisible(false);
            } else if (schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
                enable.setVisible(true);
            }
        }


        Icon hold = iconMap.get(HOLD_ICON);
        if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.LOCK_QUEUED))) {
            hold.setVisible(false);
        }
        else if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
           hold.setVisible(true);
        }

        Icon release = iconMap.get(RELEASE_ICON);

        if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            !schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD)) {
            release.setVisible(false);
        }
        else if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
            release.setVisible(true);
        }

        Icon logFile = iconMap.get(LOG_ICON);
        logFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)));

        Icon errorLogFile = iconMap.get(ERROR_LOG_ICON);
        errorLogFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            StatusImageKey key = new StatusImageKey(jobInstanceStateChangeEvent.getContextInstance().getName(),
                jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());

            Image statusImage = this.statusImageKeyImageMap.get(key);

            if(statusImage != null) {
                ui.access(() -> {
                    logger.info(String.format("refreshing status image JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName()
                        , jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(),
                        jobInstanceStateChangeEvent.getNewStatus()));
                    this.setImageBackgroundColour(statusImage, jobInstanceStateChangeEvent.getNewStatus());
                });
            }
            else {
                logger.info("null image");
            }

            if(this.schedulerJobIconMap.containsKey(key)) {
                SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(jobInstanceStateChangeEvent.getContextInstance().getId()
                    , jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName());

                schedulerJobInstanceRecord.setStatus(jobInstanceStateChangeEvent.getNewStatus().name());
                SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
                schedulerJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
                ui.access(() -> this.setIconVisibility(schedulerJobInstanceRecord, key));
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulerJobStateChangeRegistration.remove();
        this.schedulerJobStateChangeRegistration = null;
    }

    private class StatusImageKey {
        String contextName;
        String childContextName;
        String jobName;

        public StatusImageKey(String contextName, String childContextName, String jobName) {
            this.contextName = contextName;
            this.childContextName = childContextName;
            this.jobName = jobName;
        }

        public String getContextName() {
            return contextName;
        }

        public String getChildContextName() {
            return childContextName;
        }

        public String getJobName() {
            return jobName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StatusImageKey that = (StatusImageKey) o;
            return Objects.equals(contextName, that.contextName)
                && Objects.equals(childContextName, that.childContextName)
                && Objects.equals(jobName, that.jobName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextName, childContextName, jobName);
        }

        @Override
        public String toString() {
            return "StatusImageKey{" +
                "contextName='" + contextName + '\'' +
                ", childContextName='" + childContextName + '\'' +
                ", jobName='" + jobName + '\'' +
                '}';
        }
    }
}
