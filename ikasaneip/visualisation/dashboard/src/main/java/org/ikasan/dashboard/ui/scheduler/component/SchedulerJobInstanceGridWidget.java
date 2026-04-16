package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.administration.component.SystemEventDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobInstanceSplitVisualisationDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.context.local.LocalEventServiceImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.*;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.systemevent.model.SolrSystemEventSearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SchedulerJobInstanceGridWidget extends Div
    implements SchedulerJobStateChangeEventBroadcastListener, ContextInstanceSavedEventBroadcastListener {

    Logger logger = LoggerFactory.getLogger(SchedulerJobInstanceGridWidget.class);

    private SchedulerJobInstanceFilteringGrid schedulerJobInstanceFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private ContextInstance contextInstance;
    private SystemEventLogger systemEventLogger;
    private JobInitiationService jobInitiationService;
    private ModuleMetaDataService moduleMetaDataService;
    private LogStreamingService logStreamingService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationService;
    private ModuleControlService moduleControlService;
    private MetaDataService metaDataService;
    private SchedulerJobService schedulerJobService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private ContextProfileService contextProfileService;
    private GlobalEventService globalEventService;
    private LocalEventService localEventService;
    private String jobStatus;
    private String jobName;
    private UI ui;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;
    private SystemEventSearchService systemEventSearchService;

    /**
     * Constructor
     *
     * @param scheduledContextInstanceService
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param contextInstance
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param configurationService
     * @param metaDataService
     * @param jobUtilsService
     */
    public SchedulerJobInstanceGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                          LogStreamingService logStreamingService, ContextInstance contextInstance, SchedulerJobInstanceService schedulerJobInstanceService,
                                          JobInitiationService jobInitiationService, ConfigurationService configurationService,
                                          MetaDataService metaDataService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService, String jobStatus, String jobName,
                                          ContextProfileService contextProfileService, GlobalEventService globalEventService, SystemEventSearchService systemEventSearchService, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                          double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService ==  null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if(this.contextInstance ==  null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger ==  null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if(this.jobInitiationService ==  null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService ==  null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if(this.logStreamingService ==  null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if(this.scheduledProcessManagementService ==  null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }
        this.configurationService = configurationService;
        if(this.configurationService ==  null) {
            throw new IllegalArgumentException("configurationService cannot be null!");
        }
        this.moduleControlService = moduleControlRestService;
        if(this.moduleControlService ==  null) {
            throw new IllegalArgumentException("moduleControlService cannot be null!");
        }
        this.metaDataService = metaDataService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService ==  null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if(this.jobUtilsService ==  null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService ==  null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if(this.contextProfileService ==  null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.globalEventService = globalEventService;
        if(this.globalEventService ==  null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.systemEventSearchService = systemEventSearchService;
        if(this.systemEventSearchService ==  null) {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }

        this.localEventService = new LocalEventServiceImpl();

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;

        this.jobStatus = jobStatus;
        this.jobName = jobName;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.createGrid(moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService
            , metaDataRestService, systemEventLogger, contextInstance);

        Div div = new Div();
        div.setSizeFull();

        HorizontalLayout layout = new HorizontalLayout();

        Button refresh = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()));
        refresh.setIcon(VaadinIcon.REFRESH.create());
        refresh.setIconAfterText(true);
        refresh.addClickListener(event -> this.schedulerJobInstanceFilteringGrid.init());
        refresh.getElement().getStyle().set("margin-left", "auto");

        layout.add(refresh);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, refresh);

        div.add(layout);
        div.add(this.schedulerJobInstanceFilteringGrid);

        this.schedulerJobInstanceFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    /**
     * Create the grid that is presented to the user.
     *
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param contextInstance
     */
    private void createGrid(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                            ContextInstance contextInstance) {
        // Create a modulesGrid bound to the list
        SchedulerJobInstanceSearchFilter schedulerJobSearchFilter = new SolrSchedulerJobInstanceSearchFilterImpl();
        schedulerJobSearchFilter.setJobName(this.jobName);
        schedulerJobSearchFilter.setStatus(this.jobStatus);
        schedulerJobInstanceFilteringGrid = new SchedulerJobInstanceFilteringGrid(this.schedulerJobInstanceService, schedulerJobSearchFilter);
        schedulerJobInstanceFilteringGrid.removeAllColumns();
        schedulerJobInstanceFilteringGrid.setVisible(true);
        schedulerJobInstanceFilteringGrid.setWidthFull();
        schedulerJobInstanceFilteringGrid.setHeight("75vh");
        schedulerJobInstanceFilteringGrid.setContextInstanceId(contextInstance.getId());

        if(this.contextInstance.isUseDisplayName()) {
            schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();

                    if (schedulerJobInstanceRecord.getDisplayName() != null && !schedulerJobInstanceRecord.getDisplayName().isEmpty()) {
                        Label displayNameLabel = new Label(schedulerJobInstanceRecord.getDisplayName());
                        horizontalLayout.add(displayNameLabel);
                    } else {
                        Label displayNameLabel = new Label(getTranslation("label.not-defined", UI.getCurrent().getLocale()));
                        horizontalLayout.add(displayNameLabel);
                    }

                    return horizontalLayout;
                })).setHeader(getTranslation("table-header.job-name-alias", UI.getCurrent().getLocale()))
                .setResizable(true)
                .setSortable(true)
                .setKey("alias")
                .setFlexGrow(8);
        }

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Label jobNameLabel = new Label(schedulerJobInstanceRecord.getJobName());
            horizontalLayout.add(jobNameLabel);

            if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                ((InternalEventDrivenJobInstance)schedulerJobInstanceRecord.getSchedulerJobInstance()).isJobRepeatable()) {
                Image repeatable = new Image("frontend/images/repeating.png", "");
                horizontalLayout.add(repeatable);
                repeatable.getElement().setAttribute("title"
                    , getTranslation("tooltip.repeating-job", UI.getCurrent().getLocale()));
                repeatable.setHeight("20px");
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, repeatable);
            }

            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("moduleName")
            .setFlexGrow(12);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(SolrSchedulerJobInstanceSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(schedulerJobInstanceRecord.getType()));

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("type")
            .setFlexGrow(6);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobInstanceRecord.getChildContextName());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.child-context-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("childContextName")
            .setFlexGrow(8);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setSpacing(false);
                verticalLayout.setPadding(false);

                if(schedulerJobRecord.isParticipatesInLock()) {
                    Icon isInLock = IconDecorator.decorate(new Icon(VaadinIcon.LOCK), getTranslation("tooltip.target-residing-context"
                        , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                    verticalLayout.add(isInLock);
                    verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, isInLock);
                }

                return verticalLayout;
            })).setHeader("In Lock")
            .setResizable(true)
            .setSortable(false)
            .setKey("isInLock")
            .setWidth("70px");

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setSpacing(false);
                verticalLayout.setPadding(false);

                if(schedulerJobRecord.isTargetResidingContextOnly()) {
                    Icon targeted = IconDecorator.decorate(new Icon(VaadinIcon.BULLSEYE), getTranslation("tooltip.target-residing-context"
                        , UI.getCurrent().getLocale()), "14pt", IkasanColours.SCHEDULER_ERROR);
                    verticalLayout.add(targeted);
                    verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, targeted);
                }

                return verticalLayout;
            })).setHeader(getTranslation("table-header.targeted", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(false)
            .setKey("targeted")
            .setWidth("70px");

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setWidth("300px");

            Icon skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            skip.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!canPerformAction()) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.skipJob(schedulerJobInstanceRecord));
            });

            if((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                !((schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.LOCK_QUEUED)) &&
                    ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                        SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                        SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)) {
                skip.setVisible(true);
            }
            else {
                skip.setVisible(false);
            }

            layout.add(skip);

            Icon enable = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.enable-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            enable.setVisible((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            enable.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!canPerformAction()) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.enableJob(schedulerJobInstanceRecord));
            });

            if((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                    !(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)))) {
                enable.setVisible(false);
            }

            layout.add(enable);

            Icon hold = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            hold.setVisible((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            hold.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!canPerformAction()) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.holdJob(schedulerJobInstanceRecord));
            });

            if((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
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

            layout.add(hold);

            Icon release = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            release.setVisible((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            release.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!canPerformAction()) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.releaseJob(schedulerJobInstanceRecord));
            });

            if((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                !schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD)) {
                release.setVisible(false);
            }

            layout.add(release);

            Icon submit = IconDecorator.decorate(new Icon(VaadinIcon.PAPERPLANE), getTranslation("tooltip.submit-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            submit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!canPerformAction()) {
                    return;
                }
                if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    InternalEventDrivenJobSubmissionDialog internalEventDrivenJobSubmissionDialog = new InternalEventDrivenJobSubmissionDialog(this.systemEventLogger,
                        this.moduleMetaDataService, this.contextInstance, this.jobInitiationService, schedulerJobInstanceRecord, this.schedulerJobInstanceService);

                    internalEventDrivenJobSubmissionDialog.open();
                }
                else if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-file-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-file-job", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                            this.jobInitiationService.raiseFileEventSchedulerJob(
                                agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getSchedulerJobInstance(), schedulerJobInstanceRecord.getContextInstanceId());

                            logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                                , this.authentication.getName());

                            schedulerJobInstanceRecord.setManuallySubmittedBy(authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

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
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                            this.jobInitiationService.raiseQuartzSchedulerJob(agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getSchedulerJobInstance()
                                , schedulerJobInstanceRecord.getContextInstanceId());

                            logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                                , this.authentication.getName());

                            schedulerJobInstanceRecord.setManuallySubmittedBy(authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                }
                else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof GlobalEventJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-global-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-global-job", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            GlobalEventJobInstance globalEventJobInstance = (GlobalEventJobInstance)schedulerJobInstanceRecord
                                .getSchedulerJobInstance();

                            this.globalEventService.raiseGlobalEventJob(globalEventJobInstance,
                                this.contextInstance.getId(), SecurityContextHolder.getContext().getAuthentication().getName());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                                , this.authentication.getName());

                            globalEventJobInstance.setStatus(InstanceStatus.COMPLETE);
                            schedulerJobInstanceRecord.setSchedulerJobInstance(globalEventJobInstance);
                            schedulerJobInstanceRecord.setStatus(InstanceStatus.COMPLETE.toString());
                            schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                }
                else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof LocalEventJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-local-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-local-job", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            LocalEventJobInstance localEventJobInstance = (LocalEventJobInstance)schedulerJobInstanceRecord
                                .getSchedulerJobInstance();

                            localEventService.raiseLocalEventJob(localEventJobInstance,
                                this.contextInstance.getId(), SecurityContextHolder.getContext().getAuthentication().getName());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                                , this.authentication.getName());

                            localEventJobInstance.setStatus(InstanceStatus.COMPLETE);
                            schedulerJobInstanceRecord.setSchedulerJobInstance(localEventJobInstance);
                            schedulerJobInstanceRecord.setStatus(InstanceStatus.COMPLETE.toString());
                            schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));

                            if(this.ui.isAttached()) {
                                this.ui.access(() -> this.schedulerJobInstanceFilteringGrid.refreshItem(schedulerJobInstanceRecord));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                }
            });

            layout.add(submit);
            submit.setVisible((schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.WAITING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)) &&
                        ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

            Icon acknowledgeError = IconDecorator.decorate(new Icon(VaadinIcon.THUMBS_UP), getTranslation("tooltip.acknowledge-error"
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            acknowledgeError.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction()) {
                    return;
                }

                AcknowledgeErrorDialog errorDialog = new AcknowledgeErrorDialog(this.contextInstance, this.schedulerJobInstanceService,
                    schedulerJobInstanceRecord, this.systemEventLogger);
                errorDialog.open();
            });

            layout.add(acknowledgeError);
            acknowledgeError.setVisible(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
                    (schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged() == null
                        || !schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged()) &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale())
                , "14pt", "rgba(0, 0, 0, 1.0)");
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            layout.add(chart);
            // todo expose the chart when we have something built.
            chart.setVisible(false);

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

            ComponentSecurityVisibility.applySecurity(authentication, exportWrapper, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

            Icon visualisation = IconDecorator.decorate(new Icon(VaadinIcon.SITEMAP), getTranslation("tooltip.open-visualisation"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            visualisation.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                JobInstanceSplitVisualisationDialog jobInstanceSplitVisualisationDialog = new JobInstanceSplitVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                    this.configurationService, this.moduleControlService, this.metaDataService, this.systemEventLogger, this.logStreamingService,
                    this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService,
                    this.contextProfileService, this.globalEventService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance,
                    this.contextVisualisationNodeDistance);

                if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                    this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext();
                }

                ContextInstance childContext = ContextHelper.getChildContextInstance(schedulerJobInstanceRecord.getChildContextName(), this.contextInstance);

                try {
                    jobInstanceSplitVisualisationDialog.createSchedulerVisualisation(this.contextInstance, childContext);
                    jobInstanceSplitVisualisationDialog.open();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.cannot-open-visualisation", UI.getCurrent().getLocale()));
                }
            });

            layout.add(visualisation);

            ComponentSecurityVisibility.applySecurity(authentication, visualisation, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

            Icon logFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_PROCESS), getTranslation("tooltip.view-log-file"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            logFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                this.streamLog(schedulerJobInstanceRecord, false);
            });

            layout.add(logFile);
            logFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)) &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

            Icon errorLogFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_REMOVE), getTranslation("tooltip.view-error-log-file"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            errorLogFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                this.streamLog(schedulerJobInstanceRecord, true);
            });

            layout.add(errorLogFile);
            errorLogFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)) &&
                    ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                        SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                        SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

            Icon logFileHistory = IconDecorator.decorate(new Icon(VaadinIcon.CLIPBOARD_HEART), getTranslation("tooltip.view-job-execution-history"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            logFileHistory.addClickListener(event -> {
                LogFileHistoryDialog logFileHistoryDialog = new LogFileHistoryDialog(scheduledContextInstanceService
                    , this.contextInstance, schedulerJobInstanceRecord.getSchedulerJobInstance()
                    , this.moduleMetaDataService, this.logStreamingService);
                logFileHistoryDialog.open();
            });

            layout.add(logFileHistory);
            logFileHistory.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

            Icon event = IconDecorator.decorate(new Icon(VaadinIcon.CALENDAR_CLOCK), getTranslation("tooltip.view-scheduled-process-event"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            event.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                JsonViewerDialog dialog = new JsonViewerDialog(schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent()
                    , getTranslation("header.scheduled-process-event", UI.getCurrent().getLocale()));
                dialog.open();
            });

            layout.add(event);
            event.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

            Icon executionDetails = IconDecorator.decorate(new Icon(VaadinIcon.COG), getTranslation("tooltip.view-process-execution-details"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            executionDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                TextViewerDialog dialog = new TextViewerDialog(schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent().getExecutionDetails()
                    , getTranslation("header.process-execution-details", UI.getCurrent().getLocale()));
                dialog.open();
            });

            layout.add(executionDetails);
            executionDetails.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null &&
                schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent().getExecutionDetails() != null &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setWidth("230px");

        this.schedulerJobInstanceFilteringGrid.addColumn(LitRenderer.<SchedulerJobInstanceRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.date}</div>")
            .withProperty("date",
                jobInstanceRecord -> DateFormatter.instance().getFormattedDate(jobInstanceRecord.getStartTime())))
            .setHeader(getTranslation("label.start-time", UI.getCurrent().getLocale()))
            .setKey("startTime")
            .setResizable(true)
            .setSortable(true)
            .setWidth("220px");

        this.schedulerJobInstanceFilteringGrid.addColumn(LitRenderer.<SchedulerJobInstanceRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.modified}</div>")
            .withProperty("modified",
                jobInstanceRecord -> DateFormatter.instance().getFormattedDate(jobInstanceRecord.getEndTime())))
            .setHeader(getTranslation("label.end-time", UI.getCurrent().getLocale()))
            .setKey("endTime")
            .setResizable(true)
            .setSortable(true)
            .setWidth("220px");

        this.schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true)
        .setFlexGrow(2);

        this.schedulerJobInstanceFilteringGrid.addComponentColumn(schedulerJobInstanceRecord -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                if(schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
                    Button manuallySubmittedBy = new Button(schedulerJobInstanceRecord.getManuallySubmittedBy());
                    horizontalLayout.add(manuallySubmittedBy);

                    manuallySubmittedBy.addClickListener(event -> {
                        SolrSystemEventSearchFilter searchFilter = new SolrSystemEventSearchFilter();
                        searchFilter.setSubject(SystemEventConstants.SCHEDULED_JOB_SUBMITTED);
                        searchFilter.setActor(schedulerJobInstanceRecord.getManuallySubmittedBy());
                        searchFilter.setSearchTerm(schedulerJobInstanceRecord.getJobName());
                        searchFilter.setAction(schedulerJobInstanceRecord.getContextInstanceId());
                        SearchResults<SystemEvent> searchResults = this.systemEventSearchService
                            .findByFilter(searchFilter, -1, -1, null, null);

                        if(searchResults.getTotalNumberOfResults() > 0) {
                            SystemEventDialog systemEventDialog = new SystemEventDialog(DateFormatter.instance());
                            systemEventDialog.populate(searchResults.getResultList().get(0));
                        }
                    });
                }

                return horizontalLayout;
            })
            .setResizable(true)
            .setHeader(getTranslation("table-header.manually-submitted-by", UI.getCurrent().getLocale()))
            .setFlexGrow(2);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            SchedulerStatusDiv schedulerStatusDiv = new SchedulerStatusDiv();
            schedulerStatusDiv.getElement().getStyle().set("font-size", "10pt");
            schedulerStatusDiv.getElement().getStyle().set("margin-top", "1px");
            schedulerStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
            schedulerStatusDiv.setWidth("100%");

            if(this.contextInstance.isQuartzScheduleDrivenJobsDisabledForContext() &&
                schedulerJobInstanceRecord.getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                schedulerStatusDiv.setStatus(InstanceStatus.DISABLED);
            }
            else {
                if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                    ((InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance()).isKilled()) {
                    schedulerStatusDiv.setStatus(InstanceStatus.KILLED);
                }
                else {
                    schedulerStatusDiv.setStatus(schedulerJobInstanceRecord.getStatus()
                        , schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged());
                }
            }

            Button systemEventButton = new Button();
            systemEventButton.getElement().appendChild(VaadinIcon.ELLIPSIS_DOTS_V.create().getElement());
            systemEventButton.getElement().setAttribute("title", getTranslation("tab-label.system-events", UI.getCurrent().getLocale()));
            systemEventButton.addClickListener(event -> {
                JobSystemEventHistoryDialog systemEventHistoryDialog = new JobSystemEventHistoryDialog(this.contextInstance,
                    schedulerJobInstanceRecord.getSchedulerJobInstance(), this.systemEventSearchService);
                systemEventHistoryDialog.open();
            });

            horizontalLayout.add(schedulerStatusDiv,systemEventButton);

            if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
                schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged() != null &&
                schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged()) {
                Icon acknowledgedIcon = VaadinIcon.THUMBS_UP.create();
                acknowledgedIcon.getStyle().set("cursor", "pointer");
                acknowledgedIcon.getElement().setAttribute("title", getTranslation("tooltip.view-acknowledgement-details"));

                acknowledgedIcon.addClickListener(event -> {
                    SchedulerJobInstanceRecord dbRecord = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
                    ErrorAcknowledgedPositionedDialog errorAcknowledgedPositionedDialog = new ErrorAcknowledgedPositionedDialog(dbRecord,
                        this.contextInstance, this.scheduledContextInstanceService, this.moduleMetaDataService, this.logStreamingService,
                        this.schedulerJobInstanceService);
                    PositionedDialog.Position position = new PositionedDialog.Position(event.getScreenY(), event.getScreenX());
                    errorAcknowledgedPositionedDialog.setPosition(position);
                    errorAcknowledgedPositionedDialog.open();
                });
                horizontalLayout.add(acknowledgedIcon);
            }

            return horizontalLayout;
        })).setHeader(getTranslation("table-header.status", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("status")
            .setFlexGrow(6);

        HeaderRow hr = schedulerJobInstanceFilteringGrid.appendHeaderRow();
        if(this.contextInstance.isUseDisplayName()) {
            this.schedulerJobInstanceFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setDisplayNameFilter, "alias");
        }
        this.schedulerJobInstanceFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setJobName, "moduleName", this.jobName);
        this.schedulerJobInstanceFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setChildContextName, "childContextName");
        this.schedulerJobInstanceFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setJobType
            , SolrSchedulerJobInstanceSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");
        this.schedulerJobInstanceFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setStatus
            , Arrays.asList(InstanceStatus.values()).stream().map(instanceStatus -> instanceStatus.name())
                    .filter(instanceStatus -> !instanceStatus.equals(InstanceStatus.SKIPPED_COMPLETE.name())
                        && !instanceStatus.equals(InstanceStatus.SKIPPED_RUNNING.name())
                        && !instanceStatus.equals(InstanceStatus.DISABLED.name()))
                    .collect(Collectors.toList()), "status");
        this.schedulerJobInstanceFilteringGrid.addCheckboxGridFiltering(hr, schedulerJobSearchFilter::setTargetResidingContextOnly, "targeted");
        this.schedulerJobInstanceFilteringGrid.addCheckboxGridFiltering(hr, schedulerJobSearchFilter::setParticipatesInLock, "isInLock");
        this.schedulerJobInstanceFilteringGrid.addDateTimeGridFiltering(hr, schedulerJobSearchFilter::setStartTimeWindowStart
            , schedulerJobSearchFilter::setStartTimeWindowEnd, "startTime");
        this.schedulerJobInstanceFilteringGrid.addDateTimeGridFiltering(hr, schedulerJobSearchFilter::setStartTimeWindowStart
            , schedulerJobSearchFilter::setStartTimeWindowEnd, "endTime");

        this.schedulerJobInstanceFilteringGrid.addItemDoubleClickListener(event -> {
            if(event.getItem().getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) {
                FileEventJobInstanceDialog fileEventJobDialog = new FileEventJobInstanceDialog(moduleMetaDataService.findById(event.getItem().getSchedulerJobInstance().getAgentName())
                    , this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService, this.contextInstance);
                fileEventJobDialog.setJob(event.getItem());

                fileEventJobDialog.open();
            }
            else if(event.getItem().getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobInstanceDialog(moduleMetaDataService.findById(event.getItem().getSchedulerJobInstance().getAgentName())
                    , this.jobInitiationService, systemEventLogger, this.schedulerJobInstanceService, this.contextInstance);
                quartzDrivenScheduledJobDialog.setJob(event.getItem());

                quartzDrivenScheduledJobDialog.open();
            }
            else if(event.getItem().getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
                InternalEventDrivenJobInstanceDialog internalEventDrivenJobDialog = new InternalEventDrivenJobInstanceDialog(moduleMetaDataService.findById(event.getItem().getSchedulerJobInstance().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, this.schedulerJobInstanceService, this.contextInstance
                    , this.jobInitiationService, moduleMetaDataService, this.logStreamingService, this.jobUtilsService, this.scheduledContextInstanceService);

                internalEventDrivenJobDialog.setJob(this.schedulerJobInstanceService.findById(event.getItem().getId()));

                internalEventDrivenJobDialog.open();
            }
            else if(event.getItem().getType().equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE)) {
                GlobalEventJobInstanceDialog globalEventJobInstanceDialog = new GlobalEventJobInstanceDialog(systemEventLogger, schedulerJobInstanceService, this.globalEventService
                    , this.contextInstance);
                globalEventJobInstanceDialog.setJob(event.getItem());

                globalEventJobInstanceDialog.open();
            }
            else if(event.getItem().getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) {
                LocalEventJobInstanceDialog localEventJobInstanceDialog = new LocalEventJobInstanceDialog(systemEventLogger, schedulerJobInstanceService
                    , this.contextInstance);
                localEventJobInstanceDialog.setJob(event.getItem());

                localEventJobInstanceDialog.open();
            }
        });

        this.jobName = null;
    }

    /**
     * Helper method to confirm that actions can be performed on a job plan
     * @return
     */
    private boolean canPerformAction() {
        if(!ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
            if(this.contextInstance.getStatus().equals(InstanceStatus.ENDED)) {
                NotificationHelper.showUserNotification(getTranslation("notification.cannot-perform-action-against-ended-plan"
                    , UI.getCurrent().getLocale()));
                return false;
            }
            else {
                NotificationHelper.showErrorNotification(getTranslation("error.cannot-locate-job-plan-instance-in-cache-and-is-not-ended"
                    , UI.getCurrent().getLocale()));
                return false;
            }
        }

        return true;
    }

    /**
     * Helper method to stream job log files.
     *
     * @param schedulerJobInstanceRecord
     * @param getErrorLog
     */
    private void streamLog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, boolean getErrorLog) {
        boolean displayLog = false;
        String host = null;
        String endPoint = null;
        String outputLog = null;

        ModuleMetaData agent = moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
        ScheduledProcessEvent scheduledProcessEvent = schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent();

        if (scheduledProcessEvent != null && agent != null) {
            host = agent.getUrl();
            endPoint = "/rest/logs";
            outputLog = getErrorLog ? scheduledProcessEvent.getResultError() : scheduledProcessEvent.getResultOutput();
            logger.info(String.format("Streaming log for host %s, endPoint %s, log %s", host, endPoint, outputLog));
            if (outputLog != null && host != null) {
                displayLog = true;
            }
        }

        if (displayLog) {
            SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
            schedulerJobLogFileViewerDialog.open();
        } else {
            String message = "There is no " + (getErrorLog ? "error" : "output") + " log for the job";
            NotificationHelper.showUserNotification(message);
        }
    }

    /**
     * Helper method to skip the job.
     *
     * @return
     */
    private boolean skipJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-skipped", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), true);
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.SKIPPED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.skipped-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to enable the job.
     *
     * @return
     */
    private boolean enableJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-enabled", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), false);
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), false)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.enabled-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to hold the job.
     *
     * @return
     */
    private boolean holdJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-held", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.holdJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.ON_HOLD);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, String.format("Agent Name[%s], Scheduled Job Name[%s], Held[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.held-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to release the job.
     *
     * @return
     */
    private boolean releaseJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-released", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.releaseJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RELEASED, String.format("Agent Name[%s], Scheduled Job Name[%s], Released[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.released-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to update the state of a job and to broadcast that state change.
     *
     * @param schedulerJobInstanceRecord
     * @param newStatus
     */
    private void updateJobState(SchedulerJobInstanceRecord schedulerJobInstanceRecord, InstanceStatus newStatus) {
        InstanceStatus previousStatus = schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus();
        SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
        schedulerJobInstance.setStatus(newStatus);
        schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
        updateScheduledJob(schedulerJobInstanceRecord, this.authentication);

        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance,
            this.contextInstance, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
    }

    /**
     * Update a scheduled job.
     *
     * @param schedulerJobInstanceRecord
     * @param authentication
     */
    private void updateScheduledJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord, IkasanAuthentication authentication) {

        schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        schedulerJobInstanceRecord.setStatus(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().name());

        this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        SchedulerJobStateChangeEventBroadcaster.register(this);
        ContextInstanceSavedEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        SchedulerJobStateChangeEventBroadcaster.unregister(this);
        ContextInstanceSavedEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent jobInstanceStateChangeEvent) {
        if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null) {
            SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
            filter.setContextInstanceId(jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId());
            filter.setJobName(jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());
            filter.setChildContextName(jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName());

            SearchResults<SchedulerJobInstanceRecord> searchResults = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter
                (filter, 1, 0, null, null);

            if(searchResults.getResultList().size() == 1) {
                SchedulerJobInstanceRecord record = searchResults.getResultList().get(0);
                record.setStatus(jobInstanceStateChangeEvent.getNewStatus().name());
                SchedulerJobInstance instance = record.getSchedulerJobInstance();
                instance.setStatus(jobInstanceStateChangeEvent.getNewStatus());

                if(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null) {
                    record.setStartTime(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime());
                    record.setEndTime(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime());
                }

                record.setSchedulerJobInstance(instance);
                if(this.ui.isAttached()) {
                    this.ui.access(() -> this.schedulerJobInstanceFilteringGrid.refreshItem(record));
                }
            }
        }
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                if(this.contextInstance.getName().equals(contextInstance.getName())) {
                    this.contextInstance = contextInstance;
                    this.refresh();
                }
            });
        }
    }

    public void refresh() {
        this.schedulerJobInstanceFilteringGrid.getDataCommunicator().reset();
        this.schedulerJobInstanceFilteringGrid.getDataProvider().refreshAll();
    }
}
