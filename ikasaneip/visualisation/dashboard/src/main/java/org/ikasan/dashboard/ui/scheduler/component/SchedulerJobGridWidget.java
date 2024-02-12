package org.ikasan.dashboard.ui.scheduler.component;

import com.cronutils.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import liquibase.pro.packaged.L;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcastListener;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcaster;
import org.ikasan.dashboard.ui.scheduler.util.NewSchedulerJobEventBroadcastListener;
import org.ikasan.dashboard.ui.scheduler.util.NewSchedulerJobEventBroadcaster;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobTemplateVisualisationDialog;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
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
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SchedulerJobGridWidget extends Div implements ContextTemplateSavedEventBroadcastListener
    , NewSchedulerJobEventBroadcastListener {

    private SchedulerJobFilteringGrid schedulerJobFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private SystemEventLogger systemEventLogger;
    private ModuleMetaDataService moduleMetaDataService;
    private JobInitiationService jobInitiationService;
    private ContextTemplate contextTemplate;
    private ModuleControlService moduleControlRestService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private MetaDataService metaDataRestService;
    private SchedulerJobService schedulerJobService;
    private JobProvisionService jobProvisionService;

    private ContextProfileService contextProfileService;

    private UserService userService;

    private SecurityService securityService;

    private ScheduledContextService scheduledContextService;

    private LogStreamingService logStreamingService;

    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    private Button enableQuartzScheduledJobsButton;
    private Button disableQuartzScheduledJobsButton;
    private UI ui;

    /**
     * Constructor
     *
     * @param scheduledContextInstanceService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param contextTemplate
     * @param jobInitiationService
     * @param jobProvisionService
     * @param contextProfileService
     * @param userService
     * @param securityService
     * @param scheduledContextService
     */
    public SchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                  ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, ContextTemplate contextTemplate, JobInitiationService jobInitiationService,
                                  JobProvisionService jobProvisionService, ContextProfileService contextProfileService, UserService userService,
                                  SecurityService securityService, ScheduledContextService scheduledContextService, Map<String, String> schedulerJobExecutionEnvironmentLabel
    ) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if (this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if (this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if (this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.contextTemplate = contextTemplate;
        if (this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
        this.moduleControlRestService = moduleControlRestService;
        if (this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if (this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }
        this.configurationRestService = configurationRestService;
        if (this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.metaDataRestService = metaDataRestService;
        if (this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }
        this.schedulerJobService =  schedulerJobService;
        if (this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.jobProvisionService =  jobProvisionService;
        if (this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.userService = userService;
        if (this.userService == null) {
            throw new IllegalArgumentException("userService cannot be null!");
        }
        this.securityService = securityService;
        if (this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if (this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.createGrid(moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService
            , metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);

        this.schedulerJobFilteringGrid.init();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.add(this.createButtonLayout(), this.schedulerJobFilteringGrid);

        this.add(layout);
        this.setSizeFull();
    }

    /**
     * Create the scheduler job grid.
     *
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     */
    private void createGrid(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService) {
        // Create a modulesGrid bound to the list
        SolrSchedulerJobSearchFilterImpl schedulerJobSearchFilter = new SolrSchedulerJobSearchFilterImpl();
        schedulerJobFilteringGrid = new SchedulerJobFilteringGrid(schedulerJobService, schedulerJobSearchFilter);
        schedulerJobFilteringGrid.getElement().getStyle().set("margin-top", "40px");
        schedulerJobFilteringGrid.removeAllColumns();
        schedulerJobFilteringGrid.setVisible(true);
        schedulerJobFilteringGrid.setWidthFull();
        schedulerJobFilteringGrid.setHeight("75vh");
        schedulerJobFilteringGrid.setContextName(contextTemplate.getName());

        if(this.contextTemplate.isUseDisplayName()) {
            schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();

                    if (schedulerJobRecord.getDisplayName() != null && !schedulerJobRecord.getDisplayName().isEmpty()) {
                        Label displayNameLabel = new Label(schedulerJobRecord.getDisplayName());
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
                .setFlexGrow(6);
        }

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Label jobNameLabel = new Label(schedulerJobRecord.getJobName());

            horizontalLayout.add(jobNameLabel);

            if(schedulerJobRecord.getJob() instanceof InternalEventDrivenJob &&
                ((InternalEventDrivenJob)schedulerJobRecord.getJob()).isJobRepeatable()) {
                Image repeatable = new Image("frontend/images/repeating.png", "");
                repeatable.getElement().setAttribute("title"
                    , getTranslation("tooltip.repeating-job", UI.getCurrent().getLocale()));
                horizontalLayout.add(repeatable);
                repeatable.setHeight("20px");
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, repeatable);
            }

            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("flowName")
            .setFlexGrow(6);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(schedulerJobRecord.getType()));

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("type")
            .setFlexGrow(2);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setSpacing(false);
                verticalLayout.setPadding(false);

                if(schedulerJobRecord.getJob() != null && schedulerJobRecord.getJob().getChildContextNames() != null) {
                    schedulerJobRecord.getJob().getChildContextNames().forEach(context -> {
                        if(ContextHelper.getChildContext(context, this.contextTemplate) != null) {
                            Icon visualisation = IconDecorator.decorate(new Icon(VaadinIcon.SITEMAP), getTranslation("tooltip.open-visualisation", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                            Button contextButton = new Button(context);
                            contextButton.getElement().getStyle().set("font-size", "9pt");
                            contextButton.getElement().getStyle().set("color", "rgba(0, 0, 0, 1.0)");
                            contextButton.getElement().getStyle().set("margin-bottom", "5px");
                            contextButton.setIcon(visualisation);
                            contextButton.addClickListener(event -> {
                                try {
                                    JobTemplateVisualisationDialog jobTemplateVisualisationDialog = new JobTemplateVisualisationDialog(moduleMetaDataService, scheduledProcessManagementService,
                                        configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService,
                                        jobInitiationService, contextProfileService, userService, securityService,
                                        jobProvisionService, scheduledContextService, schedulerJobExecutionEnvironmentLabel);
                                    jobTemplateVisualisationDialog.createSchedulerVisualisation(contextTemplate, ContextHelper.getChildContextTemplate(context, contextTemplate));
                                    jobTemplateVisualisationDialog.open();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    NotificationHelper.showErrorNotification(getTranslation("notification.error-opening-visualisation"
                                        , UI.getCurrent().getLocale()));
                                }

                            });

                            if (schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB) && schedulerJobRecord.isSkipped()
                                && schedulerJobRecord.getJob().getSkippedContexts().containsKey(context)
                                && schedulerJobRecord.getJob().getSkippedContexts().get(context)) {
                                contextButton.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                                visualisation.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                                contextButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
                            } else if (schedulerJobRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB)
                                && schedulerJobRecord.getJob().getSkippedContexts().containsKey(this.contextTemplate.getName())) {
                                contextButton.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                                visualisation.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                                contextButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
                            }

                            if (schedulerJobRecord.isHeld()
                                && schedulerJobRecord.getJob().getHeldContexts().containsKey(context)
                                && schedulerJobRecord.getJob().getHeldContexts().get(context)) {
                                contextButton.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                                visualisation.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                                contextButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
                            }

                            ComponentSecurityVisibility.applyEnabledSecurity(authentication, contextButton, SecurityConstants.ALL_AUTHORITY,
                                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

                            verticalLayout.add(contextButton);
                        }
                    });
                }

                return verticalLayout;
            })).setHeader(getTranslation("table-header.residing-contexts", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("childContexts")
            .setFlexGrow(6);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setSpacing(false);
                verticalLayout.setPadding(false);

                if(schedulerJobRecord.getJob() instanceof InternalEventDrivenJob) {
                    if(schedulerJobRecord.isParticipatesInLock()) {
                        AtomicReference<String> lockName = new AtomicReference<>();
                        this.contextTemplate.getJobLocks().forEach(jobLock -> {
                            jobLock.getJobs().entrySet().forEach(entry -> {
                                entry.getValue().forEach(job -> {
                                    if (job.getIdentifier().equals(schedulerJobRecord.getJob().getIdentifier())) {
                                        lockName.set(jobLock.getName());
                                    }
                                });
                            });
                        });

                        Icon lock = IconDecorator.decorate(new Icon(VaadinIcon.LOCK), lockName.get(), "14pt", "rgba(0, 0, 0, 1.0)");

                        if(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ)) {
                            lock.addClickListener(event -> {
                                JobLockManagementDialog jobLockManagementDialog = new JobLockManagementDialog(this.contextTemplate, this.moduleMetaDataService, this.scheduledProcessManagementService,
                                    this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService,
                                    this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService, this.schedulerJobExecutionEnvironmentLabel);
                                jobLockManagementDialog.setJobLock(lockName.get());
                                jobLockManagementDialog.open();
                                jobLockManagementDialog.addOpenedChangeListener(dialogOpenedChangeEvent -> {
                                    if (!dialogOpenedChangeEvent.isOpened()) {
                                        this.schedulerJobFilteringGrid.refresh();
                                    }
                                });
                            });
                        }

                        verticalLayout.add(lock);
                        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, lock);
                    }
                }

                return verticalLayout;
            })).setHeader("In Lock")
            .setResizable(true)
            .setSortable(false)
            .setKey("isInLock")
            .setFlexGrow(1);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
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
            .setFlexGrow(1);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), getTranslation("tooltip.delete-job-template", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setCancelable(true);
                confirmDialog.setHeader(getTranslation("confirm-dialog.delete-job-template-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.delete-job-template-body", UI.getCurrent().getLocale()));

                confirmDialog.addConfirmListener(event -> {
                    // todo only delete jobs that no longer belong to the context.
                    this.schedulerJobService.delete(schedulerJobRecord);
                    this.schedulerJobFilteringGrid.refresh();

                    String action = String.format("Scheduled Job Deleted[%s], Parent Job Plan[%s]."
                        , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName());
                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_DELETED, action, authentication.getName());
                });

                confirmDialog.open();
            });

            ComponentSecurityVisibility.applySecurity(delete, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            layout.add(delete);

            // todo add statistics MVP2
//            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
//            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
//                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
//                underConstructionDialog.open();
//            });
//
//            ComponentSecurityVisibility.applySecurity(chart, SecurityConstants.ALL_AUTHORITY,
//                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
//                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);
//
//            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT)
                , getTranslation("label.download-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            StreamResource streamResource = new StreamResource(schedulerJobRecord.getJobName()+".json"
                , () -> {
                try {
                    return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsBytes(schedulerJobRecord.getJob()));
                }
                catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);

            ComponentSecurityVisibility.applySecurity(exportWrapper, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

            layout.add(exportWrapper);

            Icon skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-job"
                , UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            skip.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(schedulerJobRecord.getJob() instanceof InternalEventDrivenJob && ((InternalEventDrivenJob)schedulerJobRecord.getJob()).isTargetResidingContextOnly()
                    && schedulerJobRecord.getJob().getChildContextNames().size() > 1) {
                    ResidingContextSelectDialog residingContextSelectDialog
                        = new ResidingContextSelectDialog((InternalEventDrivenJob)schedulerJobRecord.getJob()
                        , ResidingContextSelectDialog.Action.SKIP);
                    residingContextSelectDialog.open();
                    residingContextSelectDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened() && residingContextSelectDialog.getSelectedContexts() != null &&
                            !residingContextSelectDialog.getSelectedContexts().isEmpty()) {
                            this.schedulerJobService.skip(schedulerJobRecord, residingContextSelectDialog.getSelectedContexts(), this.authentication.getName());
                            this.refresh();

                            String action = String.format("Targeted Scheduler Job[%s], Parent Job Plan[%s], was skipped in the following Child Contexts [%s]."
                                , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName(), StringUtils.join(residingContextSelectDialog.getSelectedContexts().toArray(), ","));
                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, action, authentication.getName());
                        }
                    });
                }
                else {
                    if(schedulerJobRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB)) {
                        this.schedulerJobService.skip(schedulerJobRecord, List.of(this.contextTemplate.getName()), this.authentication.getName());

                        String action = String.format("Global Scheduler Job[%s] was skipped in the following Job Plan [%s]."
                            , schedulerJobRecord.getJobName(), this.contextTemplate.getName());
                        this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, action, authentication.getName());
                    }
                    else {
                        this.schedulerJobService.skip(schedulerJobRecord, schedulerJobRecord.getJob().getChildContextNames(), this.authentication.getName());

                        String action = String.format("Scheduler Job[%s], Parent Job Plan[%s], was skipped in the following Child Job Plan [%s]."
                            , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName(), StringUtils.join(schedulerJobRecord.getJob().getChildContextNames().toArray(), ","));
                        this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, action, authentication.getName());
                    }
                    this.refresh();
                }

            });

            if((schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB))
                && !schedulerJobRecord.isSkipped()
                && !schedulerJobRecord.isHeld()) {
                ComponentSecurityVisibility.applySecurity(skip, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            }
            else if(schedulerJobRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB) &&
                !schedulerJobRecord.getJob().getSkippedContexts().containsKey(this.contextTemplate.getName())) {
                ComponentSecurityVisibility.applySecurity(skip, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            }
            else {
                skip.setVisible(false);
            }

            layout.add(skip);

            Icon enable = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.enable-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            enable.setVisible(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)
             || schedulerJobRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB));
            enable.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                this.schedulerJobService.enable(schedulerJobRecord, this.contextTemplate.getName(), this.authentication.getName());
                refresh();

                String action = String.format("Scheduler Job[%s], Parent Job Plan[%s], has been enabled."
                    , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName());
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_ENABLED, action, authentication.getName());
            });

            if((schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB) ||
                schedulerJobRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB))) {
                ComponentSecurityVisibility.applySecurity(enable, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            }

            if((schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB))
                && !schedulerJobRecord.isSkipped()) {
                enable.setVisible(false);
            }
            else if(schedulerJobRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB) &&
                !schedulerJobRecord.getJob().getSkippedContexts().containsKey(this.contextTemplate.getName())) {
                enable.setVisible(false);
            }

            layout.add(enable);

            Icon hold = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            hold.setVisible(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB));
            hold.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(((InternalEventDrivenJob)schedulerJobRecord.getJob()).isTargetResidingContextOnly()
                    && schedulerJobRecord.getJob().getChildContextNames().size() > 1) {
                    ResidingContextSelectDialog residingContextSelectDialog
                        = new ResidingContextSelectDialog((InternalEventDrivenJob)schedulerJobRecord.getJob()
                        , ResidingContextSelectDialog.Action.HOLD);
                    residingContextSelectDialog.open();
                    residingContextSelectDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened() && residingContextSelectDialog.getSelectedContexts() != null &&
                            !residingContextSelectDialog.getSelectedContexts().isEmpty()) {
                            this.schedulerJobService.hold(schedulerJobRecord, residingContextSelectDialog.getSelectedContexts(), this.authentication.getName());
                            this.refresh();

                            String action = String.format("Targeted Scheduler Job[%s], Parent Context[%s], was held in the following Child Contexts [%s]."
                                , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName(), StringUtils.join(residingContextSelectDialog.getSelectedContexts().toArray(), ","));
                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, action, authentication.getName());
                        }
                    });
                }
                else {
                    this.schedulerJobService.hold(schedulerJobRecord, schedulerJobRecord.getJob().getChildContextNames(), this.authentication.getName());
                    this.refresh();

                    String action = String.format("Scheduler Job[%s], Parent Context[%s], was held in the following Child Contexts [%s]."
                        , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName(), StringUtils.join(schedulerJobRecord.getJob().getChildContextNames().toArray(), ","));
                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, action, authentication.getName());
                }
            });

            if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)) {
                ComponentSecurityVisibility.applySecurity(hold, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            }

            if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)
                && (schedulerJobRecord.isHeld() || schedulerJobRecord.isSkipped())) {
                hold.setVisible(false);
            }

            layout.add(hold);

            Icon release = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            release.setVisible(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB));
            release.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                this.schedulerJobService.release(schedulerJobRecord, this.authentication.getName());
                refresh();

                String action = String.format("Scheduler Job[%s], Parent Context[%s], has been released."
                    , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName());
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RELEASED, action, authentication.getName());
            });

            if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)) {
                ComponentSecurityVisibility.applySecurity(release, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            }

            if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB) && !schedulerJobRecord.isHeld()) {
                release.setVisible(false);
            }

            layout.add(release);

            layout.setWidth("300px");
            return layout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
        .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(LitRenderer.<SchedulerJobRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(LitRenderer.<SchedulerJobRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">[[item.modified]]</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true)
        .setFlexGrow(1);
        this.schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
                VerticalLayout labelLayout = new VerticalLayout();
                SchedulerStatusDiv schedulerStatusDiv = new SchedulerStatusDiv();
                schedulerStatusDiv.getElement().getStyle().set("font-size", "10pt");
                schedulerStatusDiv.getElement().getStyle().set("margin-top", "1px");
                schedulerStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
                schedulerStatusDiv.setWidth("100%");


                if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)) {
                    if (schedulerJobRecord.isSkipped()) {
                        schedulerStatusDiv.setStatus(InstanceStatus.SKIPPED);
                        labelLayout.add(schedulerStatusDiv);
                    }

                    if (schedulerJobRecord.isHeld()) {
                        schedulerStatusDiv.setStatus(InstanceStatus.ON_HOLD);
                        labelLayout.add(schedulerStatusDiv);
                    }
                }
                else if(schedulerJobRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB)) {
                    if (schedulerJobRecord.getJob().getSkippedContexts().containsKey(contextTemplate.getName())) {
                        schedulerStatusDiv.setStatus(InstanceStatus.SKIPPED);
                        labelLayout.add(schedulerStatusDiv);
                    }
                }

                if(this.contextTemplate.isQuartzScheduleDrivenJobsDisabledForContext() &&
                    schedulerJobRecord.getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB)) {
                    schedulerStatusDiv.setStatus(InstanceStatus.DISABLED);
                    labelLayout.add(schedulerStatusDiv);
                }

                return labelLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.skip-hold", UI.getCurrent().getLocale()))
        .setSortable(true)
        .setKey("status")
        .setFlexGrow(1);

        this.schedulerJobFilteringGrid.addItemDoubleClickListener(event -> {
            if(event.getItem().getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB)) {
                FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(moduleMetaDataService.findById(event.getItem().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, this.contextTemplate.isUseDisplayName());
                fileEventJobDialog.setJob(event.getItem(), EditMode.EDIT);

                fileEventJobDialog.open();

                fileEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });
            }
            else if(event.getItem().getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB)) {
                QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(moduleMetaDataService.findById(event.getItem().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService
                    , this.contextTemplate.isUseDisplayName());
                quartzDrivenScheduledJobDialog.setJob(event.getItem(), EditMode.EDIT);

                quartzDrivenScheduledJobDialog.open();

                quartzDrivenScheduledJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });
            }
            else if(event.getItem().getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)) {
                InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(moduleMetaDataService.findById(event.getItem().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService
                    , schedulerJobExecutionEnvironmentLabel, this.contextTemplate.isUseDisplayName());
                internalEventDrivenJobDialog.setJob(event.getItem(), EditMode.EDIT);

                internalEventDrivenJobDialog.open();

                internalEventDrivenJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });

            }
            else if(event.getItem().getType().equals(JobConstants.GLOBAL_EVENT_JOB)) {
                GlobalEventJobDialog globalEventJobDialog = new GlobalEventJobDialog(moduleMetaDataService.findById(event.getItem().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService
                    , this.contextTemplate.isUseDisplayName());
                globalEventJobDialog.setJob(event.getItem(), EditMode.EDIT);

                globalEventJobDialog.open();

                globalEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });

            }
        });

        HeaderRow hr = schedulerJobFilteringGrid.appendHeaderRow();
        if(this.contextTemplate.isUseDisplayName()) {
            this.schedulerJobFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setDisplayNameFilter, "alias");
        }
        this.schedulerJobFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setJobNameFilter, "flowName");
        this.schedulerJobFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setJobTypeFilter
            , SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");

        HashMap<String, String> heldSkippedMap = new HashMap<>();
        heldSkippedMap.put(getTranslation("filter-label.held", UI.getCurrent().getLocale()), InstanceStatus.ON_HOLD.name());
        heldSkippedMap.put(getTranslation("filter-label.skipped", UI.getCurrent().getLocale()), InstanceStatus.SKIPPED.name());

        this.schedulerJobFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setStatus
            , heldSkippedMap.entrySet(), "status");
        this.schedulerJobFilteringGrid.getElement().getStyle().set("margin-top", "0px");

        HashMap<String, String> targetedMap = new HashMap<>();
        targetedMap.put(getTranslation("filter-label.targeted", UI.getCurrent().getLocale()), "targeted");

        this.schedulerJobFilteringGrid.addCheckboxGridFiltering(hr, schedulerJobSearchFilter::setTargetResidingContextOnly
            , "targeted");

        this.schedulerJobFilteringGrid.addCheckboxGridFiltering(hr, schedulerJobSearchFilter::setParticipatesInLock
            , "isInLock");
    }

    /**
     * Helper method to create the button layout.
     *
     * @return
     */
    private Component createButtonLayout() {
        VerticalLayout buttonWrapper = new VerticalLayout();
        buttonWrapper.setMargin(false);
        buttonWrapper.setPadding(false);
        buttonWrapper.setWidthFull();
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);

        Button enableAllSkippedButton = new Button(getTranslation("button.enabled-all-skipped", UI.getCurrent().getLocale()),
            VaadinIcon.PLAY.create());
        enableAllSkippedButton.setIconAfterText(true);
        enableAllSkippedButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.enabled-all-skipped-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.enabled-all-skipped-body", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();
            confirmDialog.addConfirmListener(confirmEvent -> {
                boolean error = false;
                try {
                    this.schedulerJobService.enableAll(this.contextTemplate.getName(), this.authentication.getName());
                    this.systemEventLogger.logEvent(SystemEventConstants.All_SCHEDULED_JOBS_ENABLED_FOR_JOB_PLAN,
                        String.format("Enabling all skipped jobs for job plan. Job Plan Name[%s]", this.contextTemplate.getName())
                        , this.authentication.getName());
                    this.refresh();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    error = true;
                }
                finally {
                    if(error) {
                        NotificationHelper.showErrorNotification(getTranslation("notification.all-jobs-enable-error"
                            , UI.getCurrent().getLocale()));
                    }
                    else {
                        NotificationHelper.showErrorNotification(getTranslation("notification.all-jobs-successfully-enabled"
                            , UI.getCurrent().getLocale()));
                    }
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(enableAllSkippedButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        Button holdAllButton = new Button(getTranslation("button.hold-all", UI.getCurrent().getLocale()),
            VaadinIcon.HAND.create());
        holdAllButton.setIconAfterText(true);
        holdAllButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.hold-all-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.hold-all-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();
            confirmDialog.addConfirmListener(confirmEvent -> {
                boolean error = false;
                try {
                    this.schedulerJobService.holdAll(this.contextTemplate.getName(), this.authentication.getName());
                    this.systemEventLogger.logEvent(SystemEventConstants.All_SCHEDULED_JOBS_HELD_FOR_JOB_PLAN,
                        String.format("Holding all jobs for job plan. Job Plan Name[%s]", this.contextTemplate.getName())
                        , this.authentication.getName());
                    this.refresh();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    error = true;
                }
                finally {
                    if(error) {
                        NotificationHelper.showErrorNotification(getTranslation("notification.all-jobs-hold-error"
                            , UI.getCurrent().getLocale()));
                    }
                    else {
                        NotificationHelper.showErrorNotification(getTranslation("notification.all-jobs-successfully-held"
                            , UI.getCurrent().getLocale()));
                    }
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(holdAllButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        Button releaseAllHeldButton = new Button(getTranslation("button.release-all-held", UI.getCurrent().getLocale()),
            VaadinIcon.HANDS_UP.create());
        releaseAllHeldButton.setIconAfterText(true);
        releaseAllHeldButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.release-all-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.release-all-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();
            confirmDialog.addConfirmListener(confirmEvent -> {
                boolean error = false;
                try {
                    this.schedulerJobService.releaseAll(this.contextTemplate.getName(), this.authentication.getName());
                    this.systemEventLogger.logEvent(SystemEventConstants.All_SCHEDULED_JOBS_RELEASED_FOR_JOB_PLAN,
                        String.format("All scheduled jobs released. Job Plan Name[%s]", this.contextTemplate.getName())
                        , this.authentication.getName());
                    this.refresh();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    error = true;
                }
                finally {
                    if(error) {
                        NotificationHelper.showErrorNotification(getTranslation("notification.all-jobs-released-error"
                            , UI.getCurrent().getLocale()));
                    }
                    else {
                        NotificationHelper.showErrorNotification(getTranslation("notification.all-jobs-successfully-released"
                            , UI.getCurrent().getLocale()));
                    }
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(releaseAllHeldButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.enableQuartzScheduledJobsButton = new Button(getTranslation("button.enable-quartz-scheduled-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.PLAY.create());
        enableQuartzScheduledJobsButton.setIconAfterText(true);
        enableQuartzScheduledJobsButton.setVisible(false);

        this.disableQuartzScheduledJobsButton = new Button(getTranslation("button.disable-quartz-scheduled-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.BAN.create());
        disableQuartzScheduledJobsButton.setIconAfterText(true);
        disableQuartzScheduledJobsButton.setVisible(false);

        this.setButtonVisibility();

        enableQuartzScheduledJobsButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.enable-scheduled-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.enable-scheduled-jobs-job-plan-body", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                boolean error = false;
                try {
                    this.scheduledContextService.enableScheduledJobs(contextTemplate, authentication.getName());
                    enableQuartzScheduledJobsButton.setVisible(false);
                    disableQuartzScheduledJobsButton.setVisible(true);
                    this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_TEMPLATE_SCHEDULED_JOBS_ENABLED, String.format("Enabling all scheduled jobs. Job Plan Name[%s]"
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
                        this.scheduledContextService.disableScheduledJobs(contextTemplate, authentication.getName());
                        enableQuartzScheduledJobsButton.setVisible(true);
                        disableQuartzScheduledJobsButton.setVisible(false);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_TEMPLATE_SCHEDULED_JOBS_DISABLED, String.format("Disabling all scheduled jobs. Job Plan Name[%s]"
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

        Button refreshButton = this.createRefreshButton();

        buttonLayout.add(enableAllSkippedButton, holdAllButton, releaseAllHeldButton
            , enableAllSkippedButton, disableQuartzScheduledJobsButton, enableQuartzScheduledJobsButton, refreshButton);

        buttonWrapper.add(buttonLayout);
        buttonWrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        return buttonWrapper;
    }

    /**
     * Helper method to create the refresh button.
     *
     * @return
     */
    private Button createRefreshButton() {
        Button refreshJobsButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshJobsButton.setIconAfterText(true);

        refreshJobsButton.addClickListener(event -> this.refresh());

        return refreshJobsButton;
    }

    private void setButtonVisibility() {
        if(this.contextTemplate.isQuartzScheduleDrivenJobsDisabledForContext()
            && ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
            enableQuartzScheduledJobsButton.setVisible(true);
            disableQuartzScheduledJobsButton.setVisible(false);
        }
        else if(!this.contextTemplate.isQuartzScheduleDrivenJobsDisabledForContext()
            && ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
            enableQuartzScheduledJobsButton.setVisible(false);
            disableQuartzScheduledJobsButton.setVisible(true);
        }
    }

    /**
     * Refresh the grid contents.
     */
    public void refresh() {
        this.schedulerJobFilteringGrid.refresh();
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();

        ContextTemplateSavedEventBroadcaster.register(this);
        NewSchedulerJobEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.ui = null;

        ContextTemplateSavedEventBroadcaster.unregister(this);
        NewSchedulerJobEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                this.contextTemplate = contextTemplate;
                this.setButtonVisibility();
                this.schedulerJobFilteringGrid.refresh();
            });
        }
    }

    @Override
    public void receiveBroadcast(SchedulerJob schedulerJob) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> this.schedulerJobFilteringGrid.getDataProvider().refreshAll());
        }
    }
}
