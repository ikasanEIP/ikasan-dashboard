package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.util.*;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.scheduler.view.ContextTemplateManagementView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.orchestration.service.context.util.ContextExportZipUtils;
import org.ikasan.scheduled.context.model.ScheduledContextSearchFilterImpl;
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
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcastListener;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClientException;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextTemplateWidget extends VerticalLayout implements ContextInstanceSavedEventBroadcastListener
    , ContextTemplateEnableDisableEventBroadcastListener, ContextTemplateSavedEventBroadcastListener {

    private ContextTemplateFilteringGrid contextTemplateFilteringGrid;
    private ScheduledContextService scheduledContextService;
    private ContextProfileService contextProfileService;
    private JobProvisionService jobProvisionService;
    private IkasanAuthentication authentication;
    private JobUtilsService jobUtilsService;
    private SchedulerJobService schedulerJobService;
    private GlobalEventService globalEventService;
    private ContextInstanceSchedulerService contextInstanceSchedulerService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private SystemEventLogger systemEventLogger;
    private String zipWorkingDirectory;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private EmailNotificationDetailsService emailNotificationDetailsService;
    private EmailNotificationContextService emailNotificationContextService;
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;
    private SubMenu activeContextSubMenu;
    private SpringCloudConfigRefreshService springCloudConfigRefreshService;
    private UI ui;
    private boolean removeTrailingPlanNameContextAfterUnderscore;
    private int jobPlanIntervalMultiple;

    /**
     * Constructor
     *
     * @param scheduledContextService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
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
    public ContextTemplateWidget(ScheduledContextService scheduledContextService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                                 JobInitiationService jobInitiationService, String zipWorkingDirectory, ContextProvisionService contextProvisionService,
                                 ContextProfileService contextProfileService, JobProvisionService jobProvisionService, UserService userService,
                                 SecurityService securityService, JobUtilsService jobUtilsService, boolean provisionJobs, ContextInstanceRegistrationService contextInstanceRegistrationService,
                                 EmailNotificationDetailsService emailNotificationDetailsService, EmailNotificationContextService emailNotificationContextService,
                                 Map<String, String> schedulerJobExecutionEnvironmentLabel, SpringCloudConfigRefreshService springCloudConfigRefreshService, GlobalEventService globalEventService,
                                 ContextInstanceSchedulerService contextInstanceSchedulerService, ContextParametersInstanceService contextParametersInstanceService,
                                 boolean removeTrailingPlanNameContextAfterUnderscore, int jobPlanIntervalMultiple) {

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

        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;
        this.removeTrailingPlanNameContextAfterUnderscore = removeTrailingPlanNameContextAfterUnderscore;
        this.jobPlanIntervalMultiple = jobPlanIntervalMultiple;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
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

        MenuBar quickAccessMenu = this.createQuickAccessMenu();

        Icon uploadIcon = VaadinIcon.UPLOAD_ALT.create();
        Button uploadJobPlan = new Button(getTranslation("button.upload-job-plan", UI.getCurrent().getLocale()), uploadIcon);
        uploadJobPlan.setIconAfterText(true);
        uploadJobPlan.addClickListener(buttonClickEvent -> {
            ContextImportFileDialog importer = new ContextImportFileDialog(contextProvisionService, provisionJobs);
            importer.open();
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
                , this.jobPlanIntervalMultiple);
            contextTemplateDialog.open();
            contextTemplateDialog.addOpenedChangeListener(dialogOpenedChangeEvent -> this.updateActiveContextMenu());
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
            try {
                springCloudConfigRefreshService.actuatorRefresh();
                NotificationHelper.showUserNotification(getTranslation("message.refresh-job-params-successful", UI.getCurrent().getLocale()));
            } catch (RestClientException e) {
                NotificationHelper.showUserNotification(getTranslation("message.refresh-job-params-unsuccessful", UI.getCurrent().getLocale()));
            }
        });

        ComponentSecurityVisibility.applySecurity(authentication, refreshContextParamButton
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);

        actionButtonLayout.add(refreshContextParamButton, newContextButton, uploadJobPlan, quickAccessMenu);
        actionButtonLayout.getElement().getStyle().set("position", "absolute");
        actionButtonLayout.getElement().getStyle().set("right", "30px");

        headerLayout.add(actionButtonLayout);

        div.add(headerLayout, this.contextTemplateFilteringGrid);

        this.contextTemplateFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
        this.setMargin(false);
    }

    /**
     * Method to create the job plan grid.
     *
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
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
    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                              LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                              JobInitiationService jobInitiationService, UserService userService, SecurityService securityService) {
        // Create a modulesGrid bound to the list
        ScheduledContextSearchFilter contextSearchFilter = new ScheduledContextSearchFilterImpl();
        contextTemplateFilteringGrid = new ContextTemplateFilteringGrid(this.scheduledContextService, contextSearchFilter);
        contextTemplateFilteringGrid.removeAllColumns();
        contextTemplateFilteringGrid.setVisible(true);
        contextTemplateFilteringGrid.setWidthFull();
        contextTemplateFilteringGrid.setHeightFull();


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

            Text text = new Text(scheduledContextRecord.getContext().getDescription());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.context-description", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setFlexGrow(8);


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = IconDecorator.decorate(new Icon(VaadinIcon.MODAL), getTranslation("tooltip.manage-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            edit.setId("editScheduledJob");
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE
                , SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ, SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE
                , SecurityConstants.SCHEDULER_ALL_READ);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ContextTemplateManagementDialog contextTemplateManagementDialog
                    = new ContextTemplateManagementDialog(this.scheduledContextService, scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, logStreamingService, scheduledContextRecord.getContext(), schedulerJobInstanceService, jobInitiationService, this.contextProfileService
                    , this.jobProvisionService, userService, securityService, this.jobUtilsService, this.zipWorkingDirectory, this.emailNotificationDetailsService
                    , this.emailNotificationContextService, this.schedulerJobExecutionEnvironmentLabel, this.globalEventService, this.contextInstanceRegistrationService
                    , this.contextInstanceSchedulerService, this.springCloudConfigRefreshService, this.removeTrailingPlanNameContextAfterUnderscore, this.jobPlanIntervalMultiple
                );
                contextTemplateManagementDialog.open();
            });

            layout.add(edit);

            Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), getTranslation("tooltip.delete-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, delete, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_ADMIN);

            layout.add(delete);

            delete.addClickListener(iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.delete-context-template-header"
                    , UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.delete-context-template-body"
                    , UI.getCurrent().getLocale()));
                confirmDialog.setCancelable(true);
                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                    dialog.setWidth("700px");
                    dialog.setHeight("250px");
                    dialog.open(getTranslation("progress-dialog.delete-context-template-header", UI.getCurrent().getLocale()),
                        getTranslation("progress-dialog.delete-context-template-body", UI.getCurrent().getLocale()));

                    final UI current = UI.getCurrent();
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        try {
                            this.jobProvisionService.removeJobs(scheduledContextRecord.getContextName());
                            this.schedulerJobService.deleteByContextName(scheduledContextRecord.getContextName());
                            this.scheduledContextService.deleteContext(scheduledContextRecord.getContextName());
                            this.contextInstanceRegistrationService.deRegisterByName(scheduledContextRecord.getContextName());
                            this.emailNotificationDetailsService.deleteByContextName(scheduledContextRecord.getContextName());
                            this.emailNotificationContextService.deleteByContextName(scheduledContextRecord.getContextName());

                            String action = String.format("Job plan [%s] has been deleted.", scheduledContextRecord.getContextName());
                            this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_DELETED, action, authentication.getName());

                            current.access(() -> {
                                this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
                                this.updateActiveContextMenu();

                                NotificationHelper.showUserNotification(getTranslation("notification.context-deleted-successfully"
                                    , UI.getCurrent().getLocale()));
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            current.access(() -> {
                                NotificationHelper.showUserNotification(getTranslation("error.delete-context-template"
                                    , UI.getCurrent().getLocale()));
                            });
                        }
                        finally {
                            current.access(() -> {
                                dialog.close();
                            });
                        }
                    });
                });
                confirmDialog.open();
            });

            Icon clone = IconDecorator.decorate(new Icon(VaadinIcon.COPY), getTranslation("tooltip.clone-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, clone, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            clone.addClickListener(iconClickEvent -> {
                CloneContextTemplateDialog cloneContextTemplateDialog = new CloneContextTemplateDialog(this.scheduledContextService
                    , this.contextTemplateFilteringGrid, this.contextInstanceRegistrationService, this.schedulerJobService
                    , this.contextProfileService, scheduledContextRecord);

                cloneContextTemplateDialog.addOpenedChangeListener(event -> this.updateActiveContextMenu());
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

            if(scheduledContextRecord.getContext().isQuartzScheduleDrivenJobsDisabledForContext()
                && ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
                enableQuartzScheduledJobsButton.setVisible(true);
                disableQuartzScheduledJobsButton.setVisible(false);
            }
            else if(!scheduledContextRecord.getContext().isQuartzScheduleDrivenJobsDisabledForContext()
                && ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
                enableQuartzScheduledJobsButton.setVisible(false);
                disableQuartzScheduledJobsButton.setVisible(true);
            }

            enableQuartzScheduledJobsButton.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.enable-scheduled-jobs-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.enable-scheduled-jobs-job-plan-body", UI.getCurrent().getLocale()));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    boolean error = false;
                    try {
                        ContextTemplate contextTemplate = scheduledContextRecord.getContext();
                        this.scheduledContextService.enableScheduledJobs(contextTemplate, authentication.getName());
                        enableQuartzScheduledJobsButton.setVisible(false);
                        disableQuartzScheduledJobsButton.setVisible(true);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_TEMPLATE_SCHEDULED_JOBS_ENABLED, String.format("Context Template Name [%s]"
                            , contextTemplate.getName()), this.authentication.getName());
                        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);
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
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.disable-scheduled-jobs-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.disable-scheduled-jobs-job-plan-body", UI.getCurrent().getLocale()));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    boolean error = false;
                    try {
                        ContextTemplate contextTemplate = scheduledContextRecord.getContext();
                        this.scheduledContextService.disableScheduledJobs(contextTemplate, authentication.getName());
                        enableQuartzScheduledJobsButton.setVisible(true);
                        disableQuartzScheduledJobsButton.setVisible(false);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_TEMPLATE_SCHEDULED_JOBS_DISABLED, String.format("Context Template Name [%s]"
                            , contextTemplate.getName()), this.authentication.getName());
                        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);
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
            StreamResource streamResource = new StreamResource(ContextExportZipUtils.getExportZipFileName(scheduledContextRecord.getContextName()), () -> {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = ContextExportZipUtils.createZipFile(
                        SerializationUtils.clone(scheduledContextRecord.getContext()),
                        scheduledContextRecord.getContextName(),
                        this.zipWorkingDirectory,
                        this.schedulerJobService,
                        this.emailNotificationDetailsService,
                        this.emailNotificationContextService,
                        this.contextProfileService,
                        50, // limit to loop searching solr
                        false
                    );
                    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.download-context", UI.getCurrent().getLocale()));
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);

            layout.add(exportWrapper);

            Icon exportWithTokens = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD), getTranslation("tooltip.export-jobs-and-associated-artifacts-with-tokens", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, export, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
            String downloadName = scheduledContextRecord.getContextName();
            if(this.removeTrailingPlanNameContextAfterUnderscore && downloadName.contains("_")) {
                downloadName = downloadName.substring(0, downloadName.lastIndexOf("_"));
            }
                String finalDownloadName = downloadName;
                StreamResource streamResourceWithTokens = new StreamResource(ContextExportZipUtils.getExportZipFileName(downloadName), () -> {
                try {
                    ContextTemplate downloadClone = SerializationUtils.clone(scheduledContextRecord.getContext());
                    downloadClone.setName(finalDownloadName);
                    ByteArrayOutputStream byteArrayOutputStream = ContextExportZipUtils.createZipFile(
                        downloadClone,
                        scheduledContextRecord.getContextName(),
                        this.zipWorkingDirectory,
                        this.schedulerJobService,
                        this.emailNotificationDetailsService,
                        this.emailNotificationContextService,
                        this.contextProfileService,
                        50, // limit to loop searching solr
                        true
                    );
                    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.download-context", UI.getCurrent().getLocale()));
                    return null;
                }
            });

            FileDownloadWrapper exportWrapperWithTokens = new FileDownloadWrapper(streamResourceWithTokens);
            exportWrapperWithTokens.wrapComponent(exportWithTokens);

            layout.add(exportWrapperWithTokens);

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
                if((!scheduledContextRecord.getContext().isAbleToRunConcurrently()
                    && ((ContextMachineCache.instance().getFirstByContextName(scheduledContextRecord.getContextName()) != null
                    && !ContextMachineCache.instance().getFirstByContextName(scheduledContextRecord.getContextName()).getContext().getStatus().equals(InstanceStatus.PREPARED))
                    || (ContextMachineCache.instance().getAllByContextName(scheduledContextRecord.getContextName()).size() > 1)))) {
                    NotificationHelper.showUserNotification(getTranslation("notification.cannot-create-new-instance-as-job-plan-not-concurrent"
                        , UI.getCurrent().getLocale()));
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.create-new-context-instance-header", UI.getCurrent().getLocale()));

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
                            .getContextParameterInstancesForContext(scheduledContextRecord.getContext(), internalJobs);

                        this.initialiseContextInstanceWithModifiedContextParams(scheduledContextRecord, contextParameterInstances);
                    }
                    else {
                        this.initialiseContextInstance(scheduledContextRecord, null);
                    }
                });
            });

            layout.add(newContextInstance);

            layout.setWidth("250px");
            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(4);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">[[item.modified]]</div>")
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
        .setSortable(false)
        .setWidth("150px");

        this.contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true)
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
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm.enable-context-header", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm.enable-context-body", UI.getCurrent().getLocale()));
                    confirmDialog.setCancelable(true);
                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);
                        progressIndicatorDialog.setWidth("550px");
                        progressIndicatorDialog.open(getTranslation("progress-dialog.enable-context-template-header", UI.getCurrent().getLocale())
                            , getTranslation("progress-dialog.enable-context-template-text", UI.getCurrent().getLocale()));

                        Executor executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> {
                            try {
                                ContextTemplate contextTemplate = scheduledContextRecord.getContext();
                                ScheduledContextRecord refreshedScheduledContextRecord = this.scheduledContextService.findByName(contextTemplate.getName());
                                contextTemplate = refreshedScheduledContextRecord.getContext();
                                contextTemplate.setDisabled(false);
                                refreshedScheduledContextRecord.setContext(contextTemplate);
                                scheduledContextRecord.setModifiedBy(authentication.getName());
                                this.jobProvisionService.provisionJobs(this.getSchedulerJobForContext(contextTemplate.getName())
                                    , this.authentication.getName());
                                this.scheduledContextService.save(refreshedScheduledContextRecord);
                                this.contextInstanceSchedulerService.registerStartJobAndTrigger(contextTemplate.getName(), contextTemplate.getTimeWindowStart(),
                                    contextTemplate.getTimezone());
                                contextTemplateFilteringGrid.getDataProvider().refreshAll();
                                this.updateActiveContextMenu();

                                String action = String.format("Job plan [%s] has been enabled.", scheduledContextRecord.getContextName());
                                this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_ENABLED, action, authentication.getName());

                                ContextTemplateEnableDisableEventBroadcaster.broadcast(scheduledContextRecord.getContext());
                                progressIndicatorDialog.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (ui != null && ui.isAttached()) {
                                    ui.access(() -> {
                                        progressIndicatorDialog.close();
                                        NotificationHelper.showErrorNotification(getTranslation("error.enabling-context", UI.getCurrent().getLocale()));
                                    });
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
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm.disable-context-header", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm.disable-context-body", UI.getCurrent().getLocale()));
                    confirmDialog.setCancelable(true);
                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);
                        progressIndicatorDialog.setWidth("550px");
                        progressIndicatorDialog.open(getTranslation("progress-dialog.disable-context-template-header", UI.getCurrent().getLocale())
                            , getTranslation("progress-dialog.disable-context-template-text", UI.getCurrent().getLocale()));

                        Executor executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> {
                            try {
                                ContextTemplate contextTemplate = scheduledContextRecord.getContext();
                                ScheduledContextRecord refreshedScheduledContextRecord = this.scheduledContextService.findByName(contextTemplate.getName());
                                contextTemplate = refreshedScheduledContextRecord.getContext();
                                contextTemplate.setDisabled(true);
                                scheduledContextRecord.setContext(contextTemplate);
                                scheduledContextRecord.setModifiedBy(authentication.getName());
                                this.jobProvisionService.removeJobs(contextTemplate.getName());
                                List<ContextMachine> contextMachines = ContextMachineCache.instance()
                                    .getAllByContextName(contextTemplate.getName());

                                for(ContextMachine contextMachine: contextMachines) {
                                    if (contextMachine != null) {
                                        ContextMachineCache.instance().remove(contextMachine);
                                        contextMachine.teardown();
                                    }
                                }

                                this.scheduledContextService.save(scheduledContextRecord);
                                contextTemplateFilteringGrid.getDataProvider().refreshAll();
                                this.updateActiveContextMenu();

                                String action = String.format("Job plan [%s] has been disabled.", scheduledContextRecord.getContextName());
                                this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_DISABLED, action, authentication.getName());

                                ContextTemplateEnableDisableEventBroadcaster.broadcast(scheduledContextRecord.getContext());
                                progressIndicatorDialog.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                                if(ui != null && ui.isAttached()) {
                                    ui.access(() -> {
                                        progressIndicatorDialog.close();
                                        NotificationHelper.showErrorNotification(getTranslation("error.disabling-context", UI.getCurrent().getLocale()));
                                    });
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
                contextParameterInstances);
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
     * Helper method to create the quick action menu bar.
     *
     * @return
     */
    private MenuBar createQuickAccessMenu() {
        MenuBar quickStartMenuBar = new MenuBar();
        quickStartMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem quickAccess = createQuickActionMenuItem(quickStartMenuBar
            , VaadinIcon.COG, getTranslation("menu-item.quick-access", UI.getCurrent().getLocale()));

        SubMenu activeContextInstancesSubMenu = quickAccess.getSubMenu();
        MenuItem activeContexts = activeContextInstancesSubMenu
            .addItem(getTranslation("menu-item.active-contexts", UI.getCurrent().getLocale()));
        this.activeContextSubMenu = activeContexts.getSubMenu();
        this.updateActiveContextMenu();

        return quickStartMenuBar;
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

        return schedulerJobRecordList.stream()
            .map(schedulerJobRecord -> schedulerJobRecord.getJob())
            .collect(Collectors.toList());
    }

    /**
     * Helper method to update the contents of the active context menu.
     */
    private void updateActiveContextMenu() {
        this.activeContextSubMenu.removeAll();
        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(authentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(authentication);

        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(identifier -> {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(identifier);
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) return;
            if (canAccessAllJobPlans || accessibleJobPlans.contains(contextMachine.getContext().getName())) {
                this.activeContextSubMenu.addItem(contextMachine.getContext().getName() + " (" + identifier + ")", itemClickEvent -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, contextMachine.getContext().getId() + "_scheduledContextInstance");

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
            }
        });
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
        this.ui = attachEvent.getUI();

        ContextInstanceSavedEventBroadcaster.register(this);
        ContextTemplateEnableDisableEventBroadcaster.register(this);
        ContextTemplateSavedEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.ui = null;

        ContextTemplateEnableDisableEventBroadcaster.unregister(this);
        ContextInstanceSavedEventBroadcaster.unregister(this);
        ContextTemplateSavedEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(ContextInstance event) {
        this.updateActiveContextMenu();
    }

    @Override
    public void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> this.contextTemplateFilteringGrid.getDataProvider().refreshAll());
        }
    }

    @Override
    public void receiveBroadcast(ContextTemplate contextTemplate) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> this.contextTemplateFilteringGrid.getDataProvider().refreshAll());
        }
    }
}
