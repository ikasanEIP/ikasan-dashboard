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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobTemplateVisualisationDialog;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
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
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SchedulerJobGridWidget extends Div {

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

    /**
     * Constructor
     */
    public SchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                  ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, ContextTemplate contextTemplate, JobInitiationService jobInitiationService,
                                  JobProvisionService jobProvisionService, ContextProfileService contextProfileService, UserService userService,
                                  SecurityService securityService, ScheduledContextService scheduledContextService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.systemEventLogger = systemEventLogger;
        this.moduleMetaDataService = moduleMetaDataService;
        this.jobInitiationService = jobInitiationService;
        this.contextTemplate = contextTemplate;
        this.moduleControlRestService = moduleControlRestService;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.metaDataRestService = metaDataRestService;
        this.schedulerJobService =  schedulerJobService;
        this.jobProvisionService =  jobProvisionService;
        this.contextProfileService = contextProfileService;
        this.userService = userService;
        this.securityService = securityService;
        this.scheduledContextService = scheduledContextService;
        this.logStreamingService = logStreamingService;

        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate);

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

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService, ContextTemplate contextTemplate) {
        // Create a modulesGrid bound to the list
        SolrSchedulerJobSearchFilterImpl schedulerJobSearchFilter = new SolrSchedulerJobSearchFilterImpl();
        schedulerJobFilteringGrid = new SchedulerJobFilteringGrid(schedulerJobService, schedulerJobSearchFilter);
        schedulerJobFilteringGrid.getElement().getStyle().set("margin-top", "40px");
        schedulerJobFilteringGrid.removeAllColumns();
        schedulerJobFilteringGrid.setVisible(true);
        schedulerJobFilteringGrid.setWidthFull();
        schedulerJobFilteringGrid.setHeight("75vh");
        schedulerJobFilteringGrid.setContextName(contextTemplate.getName());


        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getJobName());

            horizontalLayout.add(text);
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
                                    jobProvisionService, scheduledContextService);
                                jobTemplateVisualisationDialog.createSchedulerVisualisation(contextTemplate, ContextHelper.getChildContextTemplate(context, contextTemplate));
                                jobTemplateVisualisationDialog.open();
                            } catch (Exception e) {
                                // todo error message
                            }

                        });

                        if(schedulerJobRecord.isSkipped()
                            && schedulerJobRecord.getJob().getSkippedContexts().containsKey(context)
                            && schedulerJobRecord.getJob().getSkippedContexts().get(context)) {
                            contextButton.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                            visualisation.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                            contextButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
                        }
                        else if(schedulerJobRecord.isHeld()
                            && schedulerJobRecord.getJob().getHeldContexts().containsKey(context)
                            && schedulerJobRecord.getJob().getHeldContexts().get(context)) {
                            contextButton.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                            visualisation.getElement().getStyle().set("color", "rgba(255, 255, 255, 1.0)");
                            contextButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
                        }

                        verticalLayout.add(contextButton);
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
                        lock.addClickListener(event -> {
                            JobLockManagementDialog jobLockManagementDialog = new JobLockManagementDialog(this.contextTemplate, this.moduleMetaDataService, this.scheduledProcessManagementService,
                                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService,
                                this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService);
                            jobLockManagementDialog.setJobLock(lockName.get());
                            jobLockManagementDialog.open();
                            jobLockManagementDialog.addOpenedChangeListener(dialogOpenedChangeEvent -> {
                                if (!dialogOpenedChangeEvent.isOpened()) {
                                    this.schedulerJobFilteringGrid.refresh();
                                }
                            });
                        });
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
                });

                confirmDialog.open();
            });

            layout.add(delete);

            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            StreamResource streamResource = new StreamResource(schedulerJobRecord.getJobName()+".json"
                , () -> {
                try {
                    return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedulerJobRecord.getJob()));
                }
                catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);
            layout.add(exportWrapper);

            Icon skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            skip.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(((InternalEventDrivenJob)schedulerJobRecord.getJob()).isTargetResidingContextOnly()
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

                            String action = String.format("Targeted Scheduler Job[%s], Parent Context[%s], was skipped in the following Child Contexts [%s]."
                                , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName(), StringUtils.join(residingContextSelectDialog.getSelectedContexts().toArray(), ","));
                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, action, authentication.getName());
                        }
                    });
                }
                else {
                    this.schedulerJobService.skip(schedulerJobRecord, schedulerJobRecord.getJob().getChildContextNames(), this.authentication.getName());
                    this.refresh();

                    String action = String.format("Scheduler Job[%s], Parent Context[%s], was skipped in the following Child Contexts [%s]."
                        , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName(), StringUtils.join(schedulerJobRecord.getJob().getChildContextNames().toArray(), ","));
                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, action, authentication.getName());
                }

            });

            if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB) && !schedulerJobRecord.isSkipped()
                && !schedulerJobRecord.isHeld()) {
                skip.setVisible(true);
            }
            else {
                skip.setVisible(false);
            }

            layout.add(skip);

            Icon enable = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.enable-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            enable.setVisible(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB));
            enable.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                this.schedulerJobService.enable(schedulerJobRecord, this.authentication.getName());
                refresh();

                String action = String.format("Scheduler Job[%s], Parent Context[%s], has been enabled."
                    , schedulerJobRecord.getJobName(), schedulerJobRecord.getContextName());
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_ENABLED, action, authentication.getName());
            });

            if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB) && !schedulerJobRecord.isSkipped()) {
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

        this.schedulerJobFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobRecord>of(
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

                if(schedulerJobRecord.isSkipped()) {
                    SchedulerStatusDiv schedulerStatusDiv = new SchedulerStatusDiv();
                    schedulerStatusDiv.getElement().getStyle().set("font-size", "10pt");
                    schedulerStatusDiv.getElement().getStyle().set("margin-top", "1px");
                    schedulerStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
                    schedulerStatusDiv.setWidth("100%");
                    schedulerStatusDiv.setStatus(InstanceStatus.SKIPPED);
                    labelLayout.add(schedulerStatusDiv);
                }

                if(schedulerJobRecord.isHeld()) {
                    SchedulerStatusDiv schedulerStatusDiv = new SchedulerStatusDiv();
                    schedulerStatusDiv.getElement().getStyle().set("font-size", "10pt");
                    schedulerStatusDiv.getElement().getStyle().set("margin-top", "1px");
                    schedulerStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
                    schedulerStatusDiv.setWidth("100%");
                    schedulerStatusDiv.setStatus(InstanceStatus.ON_HOLD);
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
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
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
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
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
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                internalEventDrivenJobDialog.setJob(event.getItem(), EditMode.EDIT);

                internalEventDrivenJobDialog.open();

                internalEventDrivenJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });

            }
        });

        HeaderRow hr = schedulerJobFilteringGrid.appendHeaderRow();
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

    private Component createButtonLayout() {
        VerticalLayout buttonWrapper = new VerticalLayout();
        buttonWrapper.setMargin(false);
        buttonWrapper.setPadding(false);
        buttonWrapper.setWidthFull();
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);
        Button refreshButton = this.createRefreshButton();
        Button enableAllSkippedButton = new Button(getTranslation("button.enabled-all-skipped", UI.getCurrent().getLocale()));
        enableAllSkippedButton.addClickListener(event -> {
            this.schedulerJobService.enableAll(this.contextTemplate.getName(), this.authentication.getName());
            this.refresh();
        });
        Button releaseAllHeldButton = new Button(getTranslation("button.release-all-held", UI.getCurrent().getLocale()));
        releaseAllHeldButton.addClickListener(event -> {
            this.schedulerJobService.releaseAll(this.contextTemplate.getName(), this.authentication.getName());
            this.refresh();
        });

        buttonLayout.add(enableAllSkippedButton, releaseAllHeldButton,  refreshButton);

        buttonWrapper.add(buttonLayout);
        buttonWrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        return buttonWrapper;
    }

    private Button createRefreshButton() {
        Button refreshJobsButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshJobsButton.setIconAfterText(true);

        refreshJobsButton.addClickListener(event -> this.schedulerJobFilteringGrid.init());

        return refreshJobsButton;
    }

    public void refresh() {
        this.schedulerJobFilteringGrid.refresh();
    }

    private MenuItem createIconItem(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);

        Button menuButton = new Button(label, icon);
        menuButton.setIconAfterText(true);

        MenuItem item = menu.addItem(menuButton);
        item.getElement().getStyle().set("padding", "0px");
        item.getElement().getStyle().set("padding-right", "5px");

        return item;
    }
}
