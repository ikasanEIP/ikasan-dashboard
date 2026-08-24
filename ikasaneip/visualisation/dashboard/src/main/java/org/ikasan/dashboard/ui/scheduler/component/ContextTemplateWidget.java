package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.dashboard.cluster.service.JobParameterRefreshService;
import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.scheduler.view.ContextTemplateManagementView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.ContextTemplateEnableDisableEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.ContextTemplateSavedEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.SolrScheduledContextSearchFilterImpl;
import org.ikasan.job.orchestration.provision.job.JobProvisionException;
import org.ikasan.job.orchestration.provision.job.JobProvisionLockException;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.orchestration.service.context.util.ContextExportZipUtils;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ContextTemplateWidget extends VerticalLayout implements ContextInstanceSavedEventLocalBroadcastListener
    , ContextTemplateEnableDisableEventLocalBroadcastListener, ContextTemplateSavedEventLocalBroadcastListener {
    private Logger logger = LoggerFactory.getLogger(ContextTemplateWidget.class);

    private ContextTemplateFilteringGrid contextTemplateFilteringGrid;
    private ScheduledContextService scheduledContextService;
    private ContextProfileService contextProfileService;
    private JobProvisionService jobProvisionService;
    private IkasanAuthentication authentication;
    private JobUtilsService jobUtilsService;
    private SchedulerJobService schedulerJobService;
    private GlobalEventService globalEventService;
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private SystemEventSearchService systemEventSearchService;
    private SystemEventLogger systemEventLogger;
    private String zipWorkingDirectory;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private EmailNotificationDetailsService emailNotificationDetailsService;
    private EmailNotificationContextService emailNotificationContextService;
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;
    private SpringCloudConfigRefreshService springCloudConfigRefreshService;
    private JobParameterRefreshService jobParameterRefreshService;
    private LeaderElectionService leaderElectionService;
    private UI ui;
    private boolean removeTrailingPlanNameContextAfterUnderscore;
    private int jobPlanIntervalMultiple;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;
    private UserService userService;
    private SecurityService securityService;

    private boolean initialised = false;

    /**
     * Constructor
     *
     * @param scheduledContextService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param scheduledContextInstanceService
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param zipWorkingDirectory
     * @param contextProvisionService
     * @param contextProfileService
     * @param jobProvisionService
     * @param userService
     * @param securityService
     * @param jobUtilsService
     * @param provisionJobs
     * @param contextInstanceRegistrationService
     */
    public ContextTemplateWidget(ScheduledContextService scheduledContextService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                                 JobInitiationService jobInitiationService, String zipWorkingDirectory, ContextProvisionService contextProvisionService,
                                 ContextProfileService contextProfileService, JobProvisionService jobProvisionService, UserService userService,
                                 SecurityService securityService, JobUtilsService jobUtilsService, boolean provisionJobs, ContextInstanceRegistrationService contextInstanceRegistrationService,
                                 EmailNotificationDetailsService emailNotificationDetailsService, EmailNotificationContextService emailNotificationContextService,
                                 Map<String, String> schedulerJobExecutionEnvironmentLabel, SpringCloudConfigRefreshService springCloudConfigRefreshService,
                                 JobParameterRefreshService jobParameterRefreshService, GlobalEventService globalEventService,
                                 ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, ContextParametersInstanceService contextParametersInstanceService, SystemEventSearchService systemEventSearchService,
                                 boolean removeTrailingPlanNameContextAfterUnderscore, int jobPlanIntervalMultiple, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                 double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {

        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if (this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.zipWorkingDirectory = zipWorkingDirectory;
        if (this.zipWorkingDirectory == null) {
            throw new IllegalArgumentException("zipWorkingDirectory cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.jobProvisionService = jobProvisionService;
        if (this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if (this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        if (this.emailNotificationDetailsService == null) {
            throw new IllegalArgumentException("emailNotificationDetailsService cannot be null!");
        }
        this.emailNotificationContextService = emailNotificationContextService;
        if (this.emailNotificationContextService == null) {
            throw new IllegalArgumentException("emailNotificationContextService cannot be null!");
        }
        this.springCloudConfigRefreshService = springCloudConfigRefreshService;
        if (this.springCloudConfigRefreshService == null) {
            throw new IllegalArgumentException("springCloudConfigRefreshService cannot be null!");
        }
        this.jobParameterRefreshService = jobParameterRefreshService;
        if (this.jobParameterRefreshService == null) {
            throw new IllegalArgumentException("jobParameterRefreshService cannot be null!");
        }
        this.globalEventService = globalEventService;
        if (this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if (this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }
        this.contextParametersInstanceService = contextParametersInstanceService;
        if (this.contextParametersInstanceService == null) {
            throw new IllegalArgumentException("contextParametersInstanceService cannot be null!");
        }
        this.userService = userService;
        if (this.userService == null) {
            throw new IllegalArgumentException("userService cannot be null!");
        }
        this.securityService = securityService;
        if (this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        this.systemEventSearchService = systemEventSearchService;
        if (this.systemEventSearchService == null) {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }

        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;
        this.removeTrailingPlanNameContextAfterUnderscore = removeTrailingPlanNameContextAfterUnderscore;
        this.jobPlanIntervalMultiple = jobPlanIntervalMultiple;

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, scheduledContextInstanceService, schedulerJobInstanceService, jobInitiationService
            , userService, securityService);

        VerticalLayout div = new VerticalLayout();
        div.setMargin(false);
        div.setSizeFull();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidth("100%");
        H4 contextTemplates = new H4(getTranslation("label.job-plans", UI.getCurrent().getLocale()));
        headerLayout.add(contextTemplates);

        HorizontalLayout actionButtonLayout = new HorizontalLayout();
        actionButtonLayout.setMargin(false);

        Icon uploadIcon = VaadinIcon.UPLOAD_ALT.create();
        Button uploadJobPlan = new Button(getTranslation("button.upload-job-plan", UI.getCurrent().getLocale()), uploadIcon);
        uploadJobPlan.setIconAfterText(true);
        uploadJobPlan.addClickListener(buttonClickEvent -> {
            ContextImportFileDialog importer = new ContextImportFileDialog(contextProvisionService, this.userService, this.authentication);
            importer.open();

            importer.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened()) {
                    this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(authentication, uploadJobPlan
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);

        Icon newContextIcon = VaadinIcon.PLUS.create();
        Button newContextButton = new Button(getTranslation("button.new-job-plan", UI.getCurrent().getLocale()), newContextIcon);
        newContextButton.setIconAfterText(true);
        newContextButton.addClickListener(buttonClickEvent -> {
            ContextTemplateDialog contextTemplateDialog = new ContextTemplateDialog(this.scheduledContextService, this.schedulerJobService, this.contextInstanceRegistrationService
                , this.contextInstanceSchedulerService, this.systemEventLogger, getTranslation("label.new-context-template", UI.getCurrent().getLocale()), true
                , this.jobPlanIntervalMultiple, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance
                , this.contextVisualisationNodeDistance, this.userService, this.securityService);
            contextTemplateDialog.open();
            contextTemplateDialog.addOpenedChangeListener(dialogOpenedChangeEvent -> {
                if (!dialogOpenedChangeEvent.isOpened()) {
                    this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(authentication, newContextButton
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);

        Icon refreshIcon = VaadinIcon.REFRESH.create();
        Button refreshContextParamButton = new Button(getTranslation("button.refresh-job-params", UI.getCurrent().getLocale()), refreshIcon);
        refreshContextParamButton.setIconAfterText(true);
        refreshContextParamButton.addClickListener(buttonClickEvent -> {
            if (showFollowerNodeNotSupportedDialog()) return;
            try {
                jobParameterRefreshService.refreshLocalAndPropagateBestEffort();
                NotificationHelper.showUserNotification(getTranslation("message.refresh-job-params-successful", UI.getCurrent().getLocale()));
            } catch (RestClientException e) {
                NotificationHelper.showUserNotification(getTranslation("message.refresh-job-params-unsuccessful", UI.getCurrent().getLocale()));
            }
        });

        ComponentSecurityVisibility.applySecurity(authentication, refreshContextParamButton
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);

        Icon refreshJobPlansIcon = VaadinIcon.REFRESH.create();
        Button refreshJobPlansButton = new Button(getTranslation("button.refresh-grid", UI.getCurrent().getLocale()), refreshJobPlansIcon);
        refreshJobPlansButton.setIconAfterText(true);
        refreshJobPlansButton.addClickListener(buttonClickEvent -> {
            this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
        });

        ComponentSecurityVisibility.applySecurity(authentication, refreshJobPlansButton
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);

        actionButtonLayout.add(refreshContextParamButton, newContextButton, uploadJobPlan, refreshJobPlansButton);
        actionButtonLayout.getElement().getStyle().set("position", "absolute");
        actionButtonLayout.getElement().getStyle().set("right", "30px");

        headerLayout.add(actionButtonLayout);
        headerLayout.getElement().getStyle().set("margin-bottom", "10px");

        div.add(headerLayout, this.contextTemplateFilteringGrid);

        this.add(div);
        this.setSizeFull();
        this.setMargin(false);
    }

    public void init() {
        if(!initialised) {
            this.contextTemplateFilteringGrid.init();
            initialised = true;
        }
    }

    /**
     * Method to create the job plan grid.
     *
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param scheduledContextInstanceService
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param userService
     * @param securityService
     */
    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                              LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                              JobInitiationService jobInitiationService, UserService userService, SecurityService securityService) {
        // Create a modulesGrid bound to the list
        ScheduledContextSearchFilter contextSearchFilter = new SolrScheduledContextSearchFilterImpl();
        contextTemplateFilteringGrid = new ContextTemplateFilteringGrid(this.scheduledContextService, contextSearchFilter);
        contextTemplateFilteringGrid.removeAllColumns();
        contextTemplateFilteringGrid.setVisible(true);
        contextTemplateFilteringGrid.setWidthFull();


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Text text = new Text(scheduledContextRecord.getContextName());

                horizontalLayout.add(text);
                return horizontalLayout;
            })).setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale()))
            .setKey("moduleName")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(4);

        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getDescription());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.context-description", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setKey("description")
            .setFlexGrow(8);


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), getTranslation("tooltip.delete-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, delete, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_ADMIN);

            layout.add(delete);

            delete.addClickListener(iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.delete-context-template-header"
                    , UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.delete-context-template-body"
                    , UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                    dialog.open(getTranslation("progress-dialog.delete-context-template-header", UI.getCurrent().getLocale()),
                        getTranslation("progress-dialog.delete-context-template-body", UI.getCurrent().getLocale()));

                    final UI current = UI.getCurrent();
                    Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextTemplateWidget"));
                    executor.execute(() -> {
                        Exception exception = null;
                        try {
                            ScheduledContextRecord record = this.scheduledContextService.findByName(scheduledContextRecord.getContextName());
                            this.jobProvisionService.removeJobs(scheduledContextRecord.getContextName());
                            this.schedulerJobService.deleteByContextName(scheduledContextRecord.getContextName());
                            this.scheduledContextService.deleteContext(scheduledContextRecord.getContextName());
                            this.contextInstanceRegistrationService.deRegisterByName(scheduledContextRecord.getContextName()
                                , this.contextInstanceSchedulerService);
                            this.emailNotificationDetailsService.deleteByContextName(scheduledContextRecord.getContextName());
                            this.emailNotificationContextService.deleteByContextName(scheduledContextRecord.getContextName());

                            String action = String.format("Job plan [%s] has been deleted.", scheduledContextRecord.getContextName());
                            this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_DELETED, action, authentication.getName());
                            // Reuse the existing context-template refresh event; peer widgets re-query the backing store,
                            // so a deleted plan disappears without adding a delete-specific REST broadcast.
                            ContextTemplateSavedEventBroadcaster.instance().broadcast(record.getContext());

                            current.access(() -> {
                                this.contextTemplateFilteringGrid.getDataProvider().refreshAll();

                                NotificationHelper.showUserNotification(getTranslation("notification.context-deleted-successfully"
                                    , UI.getCurrent().getLocale()));
                            });
                        } catch (Exception e) {
                            logger.error(String.format("An error has occurred deleting job plan[%s]!", scheduledContextRecord.getContextName()), e);
                            exception = e;
                        }
                        finally {
                            current.access(() -> {
                                dialog.close();
                            });

                            if(exception != null) {
                                if (exception instanceof JobProvisionLockException) {
                                    current.access(() ->
                                        NotificationHelper.showErrorNotification(getTranslation("error.deleting-job-plan-due-to-lock", UI.getCurrent().getLocale())));
                                }
                                if (exception instanceof JobProvisionException) {
                                    current.access(() ->
                                        NotificationHelper.showUserNotification(getTranslation("notification.agent-not-available-when-deleting-job-plan"
                                            , current.getLocale())));
                                }
                                else {
                                    current.access(() ->
                                        NotificationHelper.showUserNotification(getTranslation("error.delete-context-template"
                                        , UI.getCurrent().getLocale())));
                                }
                            }
                        }
                    });
                });
                confirmDialog.open();
            });

            Icon clone = IconDecorator.decorate(new Icon(VaadinIcon.COPY), getTranslation("tooltip.clone-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, clone, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            clone.addClickListener(iconClickEvent -> {
                ScheduledContextRecord record = this.scheduledContextService.findByName(scheduledContextRecord.getContextName());
                CloneContextTemplateDialog cloneContextTemplateDialog = new CloneContextTemplateDialog(this.scheduledContextService
                    , this.contextTemplateFilteringGrid, this.contextInstanceRegistrationService, this.schedulerJobService
                    , this.contextProfileService, record);

                cloneContextTemplateDialog.open();
            });

            // todo clone not exposed until issues resolved
            //layout.add(clone);

            Icon enableQuartzScheduledJobsButton = IconDecorator.decorate(new Icon(VaadinIcon.PLAY)
                , getTranslation("tooltip.job-plan-scheduled-jobs-enabled", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            enableQuartzScheduledJobsButton.setVisible(false);

            layout.add(enableQuartzScheduledJobsButton);

            Icon disableQuartzScheduledJobsButton = IconDecorator.decorate(new Icon(VaadinIcon.BAN)
                , getTranslation("tooltip.job-plan-scheduled-jobs-disabled", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            disableQuartzScheduledJobsButton.setVisible(false);

            layout.add(disableQuartzScheduledJobsButton);

            if(scheduledContextRecord.isQuartzScheduleDrivenJobsDisabledForContext()
                && ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
                enableQuartzScheduledJobsButton.setVisible(true);
                disableQuartzScheduledJobsButton.setVisible(false);
            }
            else if(!scheduledContextRecord.isQuartzScheduleDrivenJobsDisabledForContext()
                && ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
                enableQuartzScheduledJobsButton.setVisible(false);
                disableQuartzScheduledJobsButton.setVisible(true);
            }

            enableQuartzScheduledJobsButton.addClickListener(event -> {
                if (showFollowerNodeNotSupportedDialog()) return;
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.enable-scheduled-jobs-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.enable-scheduled-jobs-job-plan-body", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    boolean error = false;
                    try {
                        ScheduledContextRecord record = this.scheduledContextService.findByName(scheduledContextRecord.getContextName());
                        ContextTemplate contextTemplate = record.getContext();
                        this.scheduledContextService.enableScheduledJobs(contextTemplate, authentication.getName());
                        enableQuartzScheduledJobsButton.setVisible(false);
                        disableQuartzScheduledJobsButton.setVisible(true);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_TEMPLATE_SCHEDULED_JOBS_ENABLED, String.format("Context Template Name [%s]"
                            , contextTemplate.getName()), this.authentication.getName());
                        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = true;
                    } finally {
                        if (error) {
                            NotificationHelper.showUserNotification(getTranslation("notification.disable-scheduled-jobs-error"
                                , UI.getCurrent().getLocale()));
                        } else {
                            NotificationHelper.showUserNotification(getTranslation("notification.disabled-scheduled-successfully"
                                , UI.getCurrent().getLocale()));
                        }
                    }
                });
            });

            disableQuartzScheduledJobsButton.addClickListener(event -> {
                if (showFollowerNodeNotSupportedDialog()) return;
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.disable-scheduled-jobs-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.disable-scheduled-jobs-job-plan-body", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    boolean error = false;
                    try {
                        ScheduledContextRecord record = this.scheduledContextService.findByName(scheduledContextRecord.getContextName());
                        ContextTemplate contextTemplate = record.getContext();
                        this.scheduledContextService.disableScheduledJobs(contextTemplate, authentication.getName());
                        enableQuartzScheduledJobsButton.setVisible(true);
                        disableQuartzScheduledJobsButton.setVisible(false);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_TEMPLATE_SCHEDULED_JOBS_DISABLED, String.format("Context Template Name [%s]"
                            , contextTemplate.getName()), this.authentication.getName());
                        ContextTemplateSavedEventBroadcaster.instance().broadcast(contextTemplate);
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = true;
                    } finally {
                        if (error) {
                            NotificationHelper.showUserNotification(getTranslation("notification.disable-scheduled-jobs-error"
                                , UI.getCurrent().getLocale()));
                        } else {
                            NotificationHelper.showUserNotification(getTranslation("notification.disabled-scheduled-successfully"
                                , UI.getCurrent().getLocale()));
                        }
                    }
                });
            });

            // todo at some point we will provide a statistics view.
//            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.contexts-statistics", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
//            ComponentSecurityVisibility.applySecurity(this.authentication, chart, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
//                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
//            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
//                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
//                underConstructionDialog.open();
//            });
//
//            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("tooltip.export-jobs-and-associated-artifacts", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, export, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);

            export.addClickListener(clickEvent -> {
                    Dialog downloadDialog = new Dialog();

                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidthFull();

                Anchor downloadNotSplitAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
                    -> new DownloadResponse( getZipContentsInputStream(scheduledContextRecord.getContextName(), false, false)
                    , ContextExportZipUtils.getExportZipFileName(scheduledContextRecord.getContextName()),
                    "application/zip", -1)), getTranslation("button.download-context-template", UI.getCurrent().getLocale()));

                    Anchor downloadSplitAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
                    -> new DownloadResponse( getZipContentsInputStream(scheduledContextRecord.getContextName(), false, true)
                    , ContextExportZipUtils.getExportZipFileName(scheduledContextRecord.getContextName()),
                    "application/zip", -1)), getTranslation("button.download_split-context-template", UI.getCurrent().getLocale()));

                    verticalLayout.add(downloadNotSplitAnchor, downloadSplitAnchor);
                    verticalLayout.setHorizontalComponentAlignment(Alignment.CENTER, downloadNotSplitAnchor, downloadSplitAnchor);
                    downloadDialog.add(verticalLayout);

                    downloadDialog.open();
                });

            layout.add(export);

            Icon exportWithTokens = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD), getTranslation("tooltip.export-jobs-and-associated-artifacts-with-tokens", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, export, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
            String downloadName = scheduledContextRecord.getContextName();
            if(this.removeTrailingPlanNameContextAfterUnderscore && downloadName.contains("_")) {
                downloadName = downloadName.substring(0, downloadName.lastIndexOf("_"));
            }

            String finalDownloadName = downloadName;

            exportWithTokens.addClickListener(clickEvent -> {
                Dialog downloadDialog = new Dialog();

                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidthFull();

                Anchor downloadNotSplitAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
                    -> new DownloadResponse( getZipContentsInputStream(scheduledContextRecord.getContextName(), true, false)
                    , ContextExportZipUtils.getExportZipFileName(scheduledContextRecord.getContextName()),
                    "application/zip", -1)), getTranslation("button.download-context-template", UI.getCurrent().getLocale()));

                Anchor downloadSplitAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
                    -> new DownloadResponse( getZipContentsInputStream(scheduledContextRecord.getContextName(), true, true)
                    , ContextExportZipUtils.getExportZipFileName(scheduledContextRecord.getContextName()),
                    "application/zip", -1)), getTranslation("button.download_split-context-template", UI.getCurrent().getLocale()));

                verticalLayout.add(downloadNotSplitAnchor, downloadSplitAnchor);
                verticalLayout.setHorizontalComponentAlignment(Alignment.CENTER, downloadNotSplitAnchor, downloadSplitAnchor);
                downloadDialog.add(verticalLayout);

                downloadDialog.open();
            });

            layout.add(exportWithTokens);

            Icon newWindow = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("tooltip.open-in-new-window", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, newWindow, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
            newWindow.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextTemplateManagementView.class, scheduledContextRecord.getContextName());

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            layout.add(newWindow);

            Icon newContextInstance = IconDecorator.decorate(new Icon(VaadinIcon.PLUS), getTranslation("tooltip.create-new-job-plan-instance", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, newContextInstance, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
            newContextInstance.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if (showFollowerNodeNotSupportedDialog()) return;
                ScheduledContextRecord record = this.scheduledContextService.findByName(scheduledContextRecord.getContextName());
                if((!record.getContext().isAbleToRunConcurrently()
                    && ((ContextMachineCache.instance().getFirstByContextName(scheduledContextRecord.getContextName()) != null
                    && !ContextMachineCache.instance().getFirstByContextName(scheduledContextRecord.getContextName()).getContext().getStatus().equals(InstanceStatus.PREPARED))
                    || (ContextMachineCache.instance().getAllByContextName(scheduledContextRecord.getContextName()).size() > 1)))) {
                    NotificationHelper.showUserNotification(getTranslation("notification.cannot-create-new-instance-as-job-plan-not-concurrent"
                        , UI.getCurrent().getLocale()));
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.create-new-context-instance-header", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));

                Checkbox modifyParams = new Checkbox(getTranslation("label.update-params-prior-to-initiating-job-plan-instance", UI.getCurrent().getLocale()));
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidthFull();
                Div body = new Div();
                body.setText(String.format(getTranslation("confirm-dialog.create-new-context-instance-body"
                    , UI.getCurrent().getLocale()), scheduledContextRecord.getContextName()));

                verticalLayout.add(body, modifyParams);
                confirmDialog.setText(verticalLayout);

                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    if(modifyParams.getValue()) {
                        Map<String, InternalEventDrivenJob> internalJobs = this.schedulerJobService
                            .getCommandExecutionJobsForContext(scheduledContextRecord.getContextName());

                        List<ContextParameterInstance> contextParameterInstances = this.contextParametersInstanceService
                            .getContextParameterInstancesForContext(record.getContext(), internalJobs);

                        this.initialiseContextInstanceWithModifiedContextParams(record, contextParameterInstances);
                    }
                    else {
                        this.initialiseContextInstance(record, null);
                    }
                });
            });

            layout.add(newContextInstance);

            layout.setWidth("250px");
            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setKey("actions")
            .setFlexGrow(4);

        this.contextTemplateFilteringGrid.addColumn(LitRenderer.<ScheduledContextRecordLite>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.date}</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(LitRenderer.<ScheduledContextRecordLite>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.modified}</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            VerticalLayout verticalLayout = new VerticalLayout();
            verticalLayout.setPadding(false);
            verticalLayout.setWidthFull();

            Icon disabledIcon = IconDecorator.decorate(VaadinIcon.BAN.create()
                , getTranslation("tooltip.job-plan-scheduled-jobs-disabled", UI.getCurrent().getLocale())
                , "16pt", IkasanColours.SCHEDULER_ERROR);

            if(scheduledContextRecord.isQuartzScheduleDrivenJobsDisabledForContext()) {
                verticalLayout.add(disabledIcon);
                verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, disabledIcon);
            }

            return verticalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.scheduled-jobs-disabled", UI.getCurrent().getLocale()))
        .setSortable(false).setKey("scheduledJobsDisabled")
        .setWidth("150px");

        this.contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true).setKey("modifiedBy")
        .setFlexGrow(1);

        this.contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
                Button enabled = new Button(getTranslation("button.enabled", UI.getCurrent().getLocale()));
                Button disabled = new Button(getTranslation("button.disabled", UI.getCurrent().getLocale()));
                if(!scheduledContextRecord.isDisabled()){
                    enabled.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
                    enabled.getElement().getStyle().set("color", IkasanColours.WHITE);
                    disabled.getElement().getStyle().remove("background-color");
                    disabled.getElement().getStyle().remove("color");
                    enabled.setEnabled(false);
                }
                else {
                    enabled.getElement().getStyle().remove("background-color");
                    enabled.getElement().getStyle().remove("color");
                    disabled.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
                    disabled.getElement().getStyle().set("color", IkasanColours.WHITE);
                    disabled.setEnabled(false);
                }

                UI ui = UI.getCurrent();

                enabled.addClickListener(event -> {
                    if (showFollowerNodeNotSupportedDialog()) return;
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm.enable-context-header", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm.enable-context-body", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);
                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);
                        progressIndicatorDialog.open(getTranslation("progress-dialog.enable-context-template-header", UI.getCurrent().getLocale())
                            , getTranslation("progress-dialog.enable-context-template-text", UI.getCurrent().getLocale()));

                        Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextTemplateWidget"));
                        executor.execute(() -> {
                            Exception exception = null;
                            try {
                                ScheduledContextRecord record = this.scheduledContextService.findByName(scheduledContextRecord.getContextName());
                                ContextTemplate contextTemplate = record.getContext();
                                ScheduledContextRecord refreshedScheduledContextRecord = this.scheduledContextService.findByName(contextTemplate.getName());
                                contextTemplate = refreshedScheduledContextRecord.getContext();
                                contextTemplate.setDisabled(false);
                                refreshedScheduledContextRecord.setContext(contextTemplate);
                                scheduledContextRecord.setModifiedBy(authentication.getName());
                                this.jobProvisionService.provisionJobs(this.getSchedulerJobForContext(contextTemplate.getName())
                                    , this.authentication.getName());
                                this.scheduledContextService.save(refreshedScheduledContextRecord);
                                this.contextInstanceSchedulerService.registerStartJobAndTrigger(contextTemplate,
                                    contextTemplate.getTimezone());
                                // ui.access() required: this lambda runs on the widget's background executor thread,
                                // not the Vaadin event thread. Calling refreshAll() directly from a non-Vaadin
                                // thread is a threading violation in Vaadin Flowimpl 24.x.
                                if(ui != null && ui.isAttached()) {
                                    ui.access(() -> contextTemplateFilteringGrid.getDataProvider().refreshAll());
                                }

                                String action = String.format("Job plan [%s] has been enabled.", scheduledContextRecord.getContextName());
                                this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_ENABLED, action, authentication.getName());

                                ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(record.getContext());
                                progressIndicatorDialog.close();
                            } catch (Exception e) {
                                logger.error(String.format("An error has occurred enabling job plans [%s]", scheduledContextRecord.getContextName()), e);
                                exception = e;
                            }
                            finally {
                                if (ui != null && ui.isAttached()) {
                                    ui.access(() -> {
                                        if(progressIndicatorDialog.isOpened()) {
                                            progressIndicatorDialog.close();
                                        }
                                    });

                                    if(exception != null) {
                                        if (exception instanceof JobProvisionLockException) {
                                            ui.access(() -> {
                                                NotificationHelper.showErrorNotification(getTranslation("error.enabling-context-due-to-lock", UI.getCurrent().getLocale()));
                                            });
                                        } else {
                                            ui.access(() -> {
                                                NotificationHelper.showErrorNotification(getTranslation("error.enabling-context", UI.getCurrent().getLocale()));
                                            });
                                        }
                                    }
                                }
                            }
                        });
                    });
                });

                ComponentSecurityVisibility.applyEnabledSecurity(authentication, enabled
                    , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
                    , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
                    , SecurityConstants.SCHEDULER_ALL_ADMIN);

                disabled.addClickListener(event -> {
                    if (showFollowerNodeNotSupportedDialog()) return;
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm.disable-context-header", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm.disable-context-body", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);
                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);
                        progressIndicatorDialog.open(getTranslation("progress-dialog.disable-context-template-header", UI.getCurrent().getLocale())
                            , getTranslation("progress-dialog.disable-context-template-text", UI.getCurrent().getLocale()));

                        Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextTemplateWidget"));
                        executor.execute(() -> {
                            Exception exception = null;
                            try {
                                ScheduledContextRecord record = this.scheduledContextService.findByName(scheduledContextRecord.getContextName());
                                ContextTemplate contextTemplate = record.getContext();
                                ScheduledContextRecord refreshedScheduledContextRecord = this.scheduledContextService.findByName(contextTemplate.getName());
                                contextTemplate = refreshedScheduledContextRecord.getContext();
                                contextTemplate.setDisabled(true);
                                record.setContext(contextTemplate);
                                record.setModifiedBy(authentication.getName());
                                this.jobProvisionService.removeJobs(contextTemplate.getName());
                                List<ContextMachine> contextMachines = ContextMachineCache.instance()
                                    .getAllByContextName(contextTemplate.getName());

                                for(ContextMachine contextMachine: contextMachines) {
                                    if (contextMachine != null) {
                                        ContextMachineCache.instance().remove(contextMachine);
                                        contextMachine.killRunningJobs();
                                        contextMachine.teardown();
                                    }
                                }

                                // Because we are disabling, we will clean up the job lock cache!
                                JobLockCacheImpl.instance().removeJobsLocksForContext(contextTemplate);

                                this.scheduledContextService.save(record);
                                // ui.access() required: this lambda runs on the widget's background executor thread,
                                // not the Vaadin event thread. Calling refreshAll() directly from a non-Vaadin
                                // thread is a threading violation in Vaadin Flowimpl 24.x.
                                if(ui != null && ui.isAttached()) {
                                    ui.access(() -> contextTemplateFilteringGrid.getDataProvider().refreshAll());
                                }

                                String action = String.format("Job plan [%s] has been disabled.", scheduledContextRecord.getContextName());
                                this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_DISABLED, action, authentication.getName());

                                ContextTemplateEnableDisableEventBroadcaster.instance().broadcast(record.getContext());
                                progressIndicatorDialog.close();
                            } catch (Exception e) {
                                logger.error(String.format("An error has occurred disabling job plans [%s]", scheduledContextRecord.getContextName()), e);
                                exception = e;
                            }
                            finally {
                                if(ui != null && ui.isAttached()) {
                                    ui.access(() -> {
                                        if(progressIndicatorDialog.isOpened()) {
                                            progressIndicatorDialog.close();
                                        }
                                    });
                                }

                                if(exception != null) {
                                    if (exception instanceof JobProvisionLockException) {
                                        ui.access(() -> {
                                            NotificationHelper.showErrorNotification(getTranslation("error.disabling-context-due-to-lock", UI.getCurrent().getLocale()));
                                        });
                                    } else {
                                        ui.access(() -> {
                                            NotificationHelper.showErrorNotification(getTranslation("error.disabling-context", UI.getCurrent().getLocale()));
                                        });
                                    }
                                }
                            }
                        });
                    });
                });

                ComponentSecurityVisibility.applyEnabledSecurity(authentication, disabled
                    , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
                    , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
                    , SecurityConstants.SCHEDULER_ALL_ADMIN);

                HorizontalLayout buttons = new HorizontalLayout();
                buttons.setWidth("200px");
                buttons.add(enabled, disabled);

                return buttons;
            }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.enabled-disabled", UI.getCurrent().getLocale()))
            .setSortable(false)
            .setKey("isDisabled")
            .setFlexGrow(2);

        HeaderRow hr = contextTemplateFilteringGrid.appendHeaderRow();
        this.contextTemplateFilteringGrid.addGridFiltering(hr, contextSearchFilter::setContextName, "moduleName");
    }

    public void setLeaderElectionService(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    /**
     * Shows a dialog explaining that the requested operation is not available on follower nodes.
     * Returns true if the dialog was shown (i.e. this node is not the leader), so callers can
     * short-circuit: {@code if (showFollowerNodeNotSupportedDialog()) return;}
     */
    private boolean showFollowerNodeNotSupportedDialog() {
        if (leaderElectionService == null || leaderElectionService.isLeader()) return false;
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Operation Not Available");
        dialog.setText("This operation is not currently supported on follower nodes. Please perform this action on the cluster leader.");
        dialog.setConfirmText("Close");
        dialog.open();
        return true;
    }

    /**
     * Generates an InputStream containing the contents of a ZIP file for a specified context.
     * The ZIP file may include tokens and split context data based on the provided parameters.
     *
     * @param contextName the name of the context for which the ZIP file will be generated.
     * @param withTokens a flag indicating whether to include tokens in the ZIP file.
     * @param splitContext a flag indicating whether to split the context data in the ZIP file.
     * @return an InputStream of the generated ZIP file, or {@code null} if an error occurs during generation.
     */
    private InputStream getZipContentsInputStream(String contextName, boolean withTokens, boolean splitContext) {
        try {
            ScheduledContextRecord record = this.scheduledContextService.findByName(contextName);
            ByteArrayOutputStream byteArrayOutputStream = ContextExportZipUtils.createZipFile(
                SerializationUtils.clone(record.getContext()),
                contextName,
                contextName,
                this.zipWorkingDirectory,
                this.schedulerJobService,
                this.emailNotificationDetailsService,
                this.emailNotificationContextService,
                this.contextProfileService,
                50, // limit to loop searching solr
                withTokens,
                splitContext
            );
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            logger.error("An error has occurred downloading context bundle for context[{}]!", contextName, e);
            NotificationHelper.showErrorNotification(getTranslation("error.download-context", UI.getCurrent().getLocale()));
            return null;
        }
    }

    /**
     * Helper method to reset the context instance with modified context parameters.
     *
     * @param scheduledContextRecord
     * @param contextParameterInstances
     */
    private void initialiseContextInstanceWithModifiedContextParams(ScheduledContextRecord scheduledContextRecord
        , List<ContextParameterInstance> contextParameterInstances) {
        ContextInstanceParameterDialog contextParameterDialog = new ContextInstanceParameterDialog(true);
        contextParameterDialog.initParams(contextParameterInstances);

        contextParameterDialog.open();

        contextParameterDialog.addOpenedChangeListener(event -> {
            if(!event.isOpened() && contextParameterDialog.isSaveClose()) {
                this.initialiseContextInstance(scheduledContextRecord, contextParameterDialog.getContextParameters());
            }
        });
    }

    /**
     * Helper method to reset the context instance with modified context parameters.
     *
     * @param scheduledContextRecord
     * @param contextParameterInstances
     */
    private void initialiseContextInstance(ScheduledContextRecord scheduledContextRecord
        , List<ContextParameterInstance> contextParameterInstances) {
        String contextInstanceId = null;
        try {
            contextInstanceId = this.contextInstanceRegistrationService.register(scheduledContextRecord.getContextName(),
                contextParameterInstances, this.contextInstanceSchedulerService);
            systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_MANUALLY_CREATED, String.format("Job Plan Name [%s] - New Instance Manually Created [%s]"
                , scheduledContextRecord.getContextName(), contextInstanceId), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showUserNotification(getTranslation("notification.error-creating-job-plan-instance", UI.getCurrent().getLocale()));
        }

        if(contextInstanceId != null) {
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceView.class, contextInstanceId + "_scheduledContextInstance");

            getUI().ifPresent(ui -> ui.getPage().open(route));
        }
    }

    /**
     * Helper method to get all the scheduler jobs associated with the context.
     *
     * @param contextName
     * @return
     */
    private List<SchedulerJob> getSchedulerJobForContext(String contextName) {
        List< SchedulerJobRecord> schedulerJobRecordList = this.schedulerJobService
            .findByContext(contextName, -1, -1).getResultList();

        List<String> jobIdentifiers = new ArrayList<>();

        ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findByName(contextName);

        if(scheduledContextRecord != null) {
            List<SchedulerJob> jobsInJobPlan = ContextHelper.getAllJobs(scheduledContextRecord.getContext());
            jobsInJobPlan.forEach(job -> jobIdentifiers.add(job.getIdentifier()));
        }


        return schedulerJobRecordList.stream()
            .map(schedulerJobRecord -> schedulerJobRecord.getJob())
            .filter(job -> jobIdentifiers.contains(job.getIdentifier()))
            .collect(Collectors.toList());
    }

    /**
     * Helper method to create the quick action menu item.
     *
     * @param menu
     * @param iconName
     * @param label
     * @return
     */
    private MenuItem createQuickActionMenuItem(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);
        Button quickAccessButton = new Button(label, icon);
        quickAccessButton.setIconAfterText(true);

        MenuItem item = menu.addItem(quickAccessButton);

        return item;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        ContextInstanceSavedEventBroadcaster.instance().register(this);
        ContextTemplateEnableDisableEventBroadcaster.instance().register(this);
        ContextTemplateSavedEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        ContextTemplateEnableDisableEventBroadcaster.instance().unregister(this);
        ContextInstanceSavedEventBroadcaster.instance().unregister(this);
        ContextTemplateSavedEventBroadcaster.instance().unregister(this);
    }

    @Override
    public void receiveBroadcast(ContextInstance event) {
        // ui.access() required: receiveBroadcast is called from the broadcaster's background executor
        // thread. Vaadin Flowimpl 24.x requires all UI mutations to run on the Vaadin event thread
        // (or via ui.access()). The null-check guards the race window between onDetach() setting
        // ui=null and unregister() removing this listener.
        if(this.ui != null && this.ui.isAttached()) {
            // nothing to do at the moment
        }
    }

    @Override
    public void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate) {
        // ui.access() required: called from a background broadcaster thread — see receiveBroadcast(ContextInstance).
        // init() is called first because the widget lazily initialises its data provider only when the user
        // first clicks the "Job Plans" tab. Before that, refreshAll() would be a no-op on the default
        // empty ListDataProvider. init() is idempotent (guarded by if(!initialised)) so calling it again
        // after the tab has already been opened is safe.
        if(this.ui != null && this.ui.isAttached()) {
            this.ui.access(() -> {
                this.init();
                this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
            });
        }
    }

    @Override
    public void receiveBroadcast(ContextTemplate contextTemplate) {
        // Primary fix for asymmetric cluster broadcast (node1→node2 enable/disable not reflected on node2 UI).
        // Root cause: if the user on this node has not yet clicked the "Job Plans" tab, init() has not been
        // called, so the grid still has Vaadin's default empty ListDataProvider and refreshAll() is a no-op.
        // Calling init() here (idempotent — guarded by if(!initialised)) ensures the backend data provider
        // (DataProvider.fromFilteringCallbacks → Solr) is wired up before refreshAll() triggers a re-query.
        // ui.access() required: called from a background broadcaster thread — see receiveBroadcast(ContextInstance).
        if(this.ui != null && this.ui.isAttached()) {
            this.ui.access(() -> {
                this.init();
                this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
            });
        }
    }
}
