package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouteConfiguration;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.apache.commons.lang3.time.StopWatch;
import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.command.HoldAllCommandExecutionJobsForContextInstanceCommand;
import org.ikasan.dashboard.ui.scheduler.command.ReleaseAllCommandExecutionJobsForContextInstanceCommand;
import org.ikasan.dashboard.ui.scheduler.service.AggregateStatusCollector;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SplitContextInstanceVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.dag.component.DagComponent;
import org.ikasan.dashboard.ui.visualisation.scheduler.dag.component.DagNode;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateToDagConverter;
import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.ContextDurationUtils;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.core.JacksonException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl.SCHEDULED_CONTEXT_INSTANCE_TYPE;

public class ContextInstanceWidget extends VerticalLayout
    implements BeforeEnterObserver, ContextInstanceStateChangeEventLocalBroadcastListener
    , SchedulerJobStateChangeEventLocalBroadcastListener {

    private static Logger logger = LoggerFactory.getLogger(ContextInstanceWidget.class);
    public static final String TREE_TAB = "treeTab";
    public static final String VISUALISATION_TAB = "visualisationTab";
    public static final String DAG_TAB = "dagTab";
    public static final String RAW_CONTEXT_TAB =  "rawContextTab";
    public static final String JOB_INSTANCE_TAB = "jobsTab";
    public static final String STATISTICS_TAB = "statisticsTab";
    public static final String AUDIT_TAB = "auditTab";

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private FormLayout formLayout;
    private IkasanAuthentication authentication;
    private AceEditor aceEditor;
    private SchedulerJobInstanceGridWidget schedulerJobInstanceGridWidget;
    private ContextTemplateStatisticsWidget contextTemplateStatisticsWidget;
    private ContextInstanceAuditWidget contextInstanceAuditWidget;
    private ContextInstanceTreeViewWidget contextInstanceTreeViewWidget;
    private SplitContextInstanceVisualisation splitContextInstanceVisualisation;
    private DeadLetterQueueManagementWidget deadLetterQueueManagementWidget;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private ModuleMetaDataService moduleMetaDataService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private LogStreamingService logStreamingService;
    private SchedulerJobService schedulerJobService;
    private GlobalEventService globalEventService;
    private SystemEventSearchService systemEventSearchService;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    private TextField contextInstanceId;
    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private IntegerField contextTtlMinutes;
    private IntegerField contextTtlHours;
    private IntegerField contextTtlDays;
    private TextField startTimeTf;
    private TextField projectedEndTimeTf;
    private TextField endTimeTf;
    private TextField timezoneTf;
    private TextField environmentTf;
    private Checkbox isAbleToRunConcurrentlyCb;
    private Checkbox useDisplayNameCb;
    private Checkbox renderLogicalBoundariesCb;
    private Checkbox renderOrLogicalBoundariesOnlyCb;
    private Checkbox useAutoformattingCb;
    private Checkbox endJobPlanWhenComplete;
    private CollapsableLayout contextInstanceDetailsCollapsableLayout;

    private Button holdContextButton;
    private Button releaseContextButton;
    private Button enableQuartzScheduledJobsButton;
    private Button disableQuartzScheduledJobsButton;
    private Button contextInstanceEndButton;
    private Button ignoreContextInstanceEndButton;
    private Button resetContextButton;
    private LeaderElectionService leaderElectionService;
    private Button contextInstanceParameterButton;
    private Button jobLockDashboard;
    private Tab treeTab;
    private Tab visualisationTab;
    private Tab dagTab;
    private Tab rawContextTab;
    private Tab jobsTab;
    private Tab statisticsTab;
    private Tab auditTab;
    private Tab dlqTab;
    private Tabs tabs;
    private ContextInstance contextInstance;
    private ContextTemplate contextTemplate;
    private SchedulerStatusDiv statusDiv;
    private String selectedTab;
    private String jobStatus;
    private String jobName;
    private UI ui;

    private DagComponent dagComponent;
    private Div ikasanMinimapContainer;

    private SchedulerStatusFreeTextDiv waitingStatus = new SchedulerStatusFreeTextDiv();
    private SchedulerStatusFreeTextDiv completeStatus = new SchedulerStatusFreeTextDiv();
    private SchedulerStatusFreeTextDiv runningStatus = new SchedulerStatusFreeTextDiv();
    private SchedulerStatusFreeTextDiv queuedStatus = new SchedulerStatusFreeTextDiv();
    private SchedulerStatusFreeTextDiv onHoldStatus = new SchedulerStatusFreeTextDiv();
    private SchedulerStatusFreeTextDiv skippedStatus = new SchedulerStatusFreeTextDiv();
    private SchedulerStatusFreeTextDiv errorStatus = new SchedulerStatusFreeTextDiv();
    private SchedulerStatusFreeTextDiv errorAckedStatus = new SchedulerStatusFreeTextDiv();

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    /**
     * Constructor
     *
     * @param scheduledContextInstanceService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param contextInstance
     * @param contextTemplate
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param contextProfileService
     * @param jobUtilsService
     * @param scheduledContextService
     * @param selectedTab
     * @param jobStatus
     */
    public ContextInstanceWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                 ContextProfileService contextProfileService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                 String selectedTab, String jobStatus, String jobName, GlobalEventService globalEventService,
                                 ContextInstanceRegistrationService contextInstanceRegistrationService, ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, SystemEventSearchService systemEventSearchService,
                                 double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        this(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService,
            logStreamingService, contextInstance, contextTemplate, schedulerJobInstanceService, jobInitiationService,
            contextProfileService, jobUtilsService, scheduledContextService, globalEventService, contextInstanceRegistrationService, contextInstanceSchedulerService, systemEventSearchService,
            jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);
        this.selectedTab = selectedTab;
        this.jobStatus = jobStatus;
        this.jobName = jobName;
    }

    /**
     * Constructor
     *
     * @param scheduledContextInstanceService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param contextInstance
     * @param contextTemplate
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param contextProfileService
     * @param jobUtilsService
     * @param scheduledContextService
     */
    public ContextInstanceWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                 ContextProfileService contextProfileService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                 GlobalEventService globalEventService, ContextInstanceRegistrationService contextInstanceRegistrationService,
                                 ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, SystemEventSearchService systemEventSearchService,
                                 double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if (this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if (this.contextInstance == null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }
        this.contextTemplate = contextTemplate;
        if (this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if (this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
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
        this.schedulerJobService = schedulerJobService;
        if (this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.globalEventService = globalEventService;
        if (this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }
        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }
        this.systemEventSearchService = systemEventSearchService;
        if (this.systemEventSearchService == null) {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.setMargin(false);
        this.setSpacing(false);
        this.setPadding(false);
    }

    /**
     * Method to initialise the widget.
     *
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     */
    private void init(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                      ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                      MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                      LogStreamingService logStreamingService) {
        Binder<ContextInstance> binder = new Binder<>(ContextInstance.class);

        this.contextInstanceId = new TextField(getTranslation("label.context-instance-id", UI.getCurrent().getLocale()));
        this.contextInstanceId.getElement().getThemeList().add("always-float-label");
        binder.forField(contextInstanceId)
            .bind(ContextInstance::getId, ContextInstance::setId);
        this.contextInstanceId.setEnabled(false);

        this.contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        this.contextNameTf.getElement().getThemeList().add("always-float-label");
        binder.forField(contextNameTf)
            .bind(ContextInstance::getName, ContextInstance::setName);
        this.contextNameTf.setEnabled(false);

        this.descriptionTa = new TextArea(getTranslation("table-header.description", UI.getCurrent().getLocale()));
        this.descriptionTa.getElement().getThemeList().add("always-float-label");
        binder.forField(descriptionTa)
            .bind(ContextInstance::getDescription, ContextInstance::setDescription);
        this.descriptionTa.setEnabled(false);

        this.startWindowCronExpressionTf = new TextField(getTranslation("label.time-window-start", UI.getCurrent().getLocale()));
        this.startWindowCronExpressionTf.getElement().getThemeList().add("always-float-label");
        binder.forField(startWindowCronExpressionTf)
            .bind(ContextInstance::getTimeWindowStart, ContextInstance::setTimeWindowStart);
        this.startWindowCronExpressionTf.setEnabled(false);

        this.contextTtlDays = new IntegerField(getTranslation("label.duration-days", UI.getCurrent().getLocale()));
        this.contextTtlDays.getElement().getThemeList().add("always-float-label");
        this.contextTtlDays.setValue(ContextDurationUtils.getDays(this.contextInstance.getContextTtlMilliseconds()));
        this.contextTtlDays.setEnabled(false);
        this.contextTtlHours = new IntegerField(getTranslation("label.duration-hours", UI.getCurrent().getLocale()));
        this.contextTtlHours.getElement().getThemeList().add("always-float-label");
        this.contextTtlHours.setValue(ContextDurationUtils.getHours(this.contextInstance.getContextTtlMilliseconds()));
        this.contextTtlHours.setEnabled(false);
        this.contextTtlMinutes = new IntegerField(getTranslation("label.duration-minutes", UI.getCurrent().getLocale()));
        this.contextTtlMinutes.getElement().getThemeList().add("always-float-label");
        this.contextTtlMinutes.setValue(ContextDurationUtils.getMinutes(this.contextInstance.getContextTtlMilliseconds()));
        this.contextTtlMinutes.setEnabled(false);

        this.isAbleToRunConcurrentlyCb = new Checkbox(getTranslation("label.concurrent", UI.getCurrent().getLocale()));
        this.isAbleToRunConcurrentlyCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.isAbleToRunConcurrentlyCb)
            .bind(ContextInstance::isAbleToRunConcurrently, ContextInstance::setAbleToRunConcurrently);
        this.isAbleToRunConcurrentlyCb.setEnabled(false);

        this.useDisplayNameCb = new Checkbox(getTranslation("label.use-display-name", UI.getCurrent().getLocale()));
        this.useDisplayNameCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.useDisplayNameCb)
            .bind(ContextInstance::isUseDisplayName, ContextInstance::setUseDisplayName);
        this.useDisplayNameCb.setEnabled(false);

        this.renderLogicalBoundariesCb = new Checkbox(getTranslation("label.render-logical-boundaries"));
        this.renderLogicalBoundariesCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.renderLogicalBoundariesCb)
            .bind(ContextInstance::isRenderLogicalBoundaries, ContextInstance::setRenderLogicalBoundaries);
        this.renderLogicalBoundariesCb.setEnabled(false);

        this.renderOrLogicalBoundariesOnlyCb = new Checkbox(getTranslation("label.render-or-logical-boundaries-only"));
        this.renderOrLogicalBoundariesOnlyCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.renderOrLogicalBoundariesOnlyCb)
            .bind(ContextInstance::isRenderOrLogicalBoundariesOnly, ContextInstance::setRenderOrLogicalBoundariesOnly);
        this.renderOrLogicalBoundariesOnlyCb.setEnabled(false);

        this.useAutoformattingCb = new Checkbox(getTranslation("label.use-auto-formatting"));
        this.useAutoformattingCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.useAutoformattingCb)
            .bind(ContextInstance::isUseAutoLayout, ContextInstance::setUseAutoLayout);
        this.useAutoformattingCb.setEnabled(false);

        this.endJobPlanWhenComplete = new Checkbox(getTranslation("label.end-job-plan-when-complete", UI.getCurrent().getLocale()));
        this.endJobPlanWhenComplete.getElement().getThemeList().add("always-float-label");
        binder.forField(this.endJobPlanWhenComplete)
            .bind(ContextInstance::isEndJobPlanUponCompletion, ContextInstance::setEndJobPlanUponCompletion);
        this.endJobPlanWhenComplete.setEnabled(false);


        this.timezoneTf = new TextField(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        this.timezoneTf.getElement().getThemeList().add("always-float-label");
        binder.forField(this.timezoneTf)
            .bind(ContextInstance::getTimezone, ContextInstance::setTimezone);
        this.timezoneTf.setEnabled(false);

        this.environmentTf = new TextField(getTranslation("label.environment", UI.getCurrent().getLocale()));
        this.environmentTf.getElement().getThemeList().add("always-float-label");
        binder.forField(this.environmentTf)
            .bind(ContextInstance::getEnvironmentGroup, ContextInstance::setEnvironmentGroup);
        this.environmentTf.setEnabled(false);

        this.startTimeTf = new TextField(getTranslation("label.start-date-time", UI.getCurrent().getLocale()));
        this.startTimeTf.getElement().getThemeList().add("always-float-label");
        if(this.contextInstance.getStartTime() > 0) {
            this.startTimeTf.setValue(DateFormatter.instance().getFormattedDate(this.contextInstance.getStartTime()));
        }
        this.startTimeTf.setEnabled(false);

        this.projectedEndTimeTf = new TextField(getTranslation("label.projected-end-date-time", UI.getCurrent().getLocale()));
        this.projectedEndTimeTf.getElement().getThemeList().add("always-float-label");
        if(this.contextInstance.isRunContextUntilManuallyEnded()) {
            this.projectedEndTimeTf.setValue("This instance must be ended manually!");
        }
        else if(this.contextInstance.getProjectedEndTime() > 0) {
            this.projectedEndTimeTf.setValue(DateFormatter.instance().getFormattedDate(this.contextInstance.getProjectedEndTime()));
        }
        this.projectedEndTimeTf.setEnabled(false);

        this.endTimeTf = new TextField(getTranslation("label.end-date-time", UI.getCurrent().getLocale()));
        this.endTimeTf.getElement().getThemeList().add("always-float-label");
        if(this.contextInstance.getEndTime() > 0) {
            this.endTimeTf.setValue(DateFormatter.instance().getFormattedDate(this.contextInstance.getEndTime()));
        }
        this.endTimeTf.setEnabled(false);

        binder.readBean(this.contextInstance);

        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.getElement().getStyle().set("padding-top", "0px");
        statusLayout.getElement().getStyle().set("padding-bottom", "10px");
        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.setStatus(this.contextInstance.getStatus());

        statusLayout.add(this.statusDiv);
        statusLayout.setWidth("100%");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidth("100%");
        headerLayout.setMargin(false);
        headerLayout.setPadding(false);
        H4 contextInstanceManagementLabel
            = new H4(String.format(getTranslation("label.context-instance", UI.getCurrent().getLocale()))
                + " - " + this.contextInstance.getName());
        contextInstanceManagementLabel.getElement().getStyle().set("margin-top", "10px");
        HorizontalLayout labelLayout = new HorizontalLayout();
        labelLayout.setWidth("100%");
        labelLayout.setMargin(false);
        labelLayout.setPadding(false);
        labelLayout.add(contextInstanceManagementLabel);
        headerLayout.add(labelLayout, createButtonLayout());

        this.formLayout = new FormLayout();
        formLayout.getStyle().set("padding-top", "0px");

        formLayout.setResponsiveSteps(
            // Use four columns by default
            new FormLayout.ResponsiveStep("0", 22)
        );

        this.formLayout.setWidth("100%");

        VerticalLayout cbLayout = new VerticalLayout(this.isAbleToRunConcurrentlyCb
            , this.useDisplayNameCb, this.useAutoformattingCb, this.renderLogicalBoundariesCb
            , this.renderOrLogicalBoundariesOnlyCb, this.endJobPlanWhenComplete);
        cbLayout.setMargin(false);
        cbLayout.getElement().getThemeList().remove("padding");
        cbLayout.getElement().getThemeList().remove("spacing");

        this.formLayout.add(this.contextInstanceId, 6);
        this.formLayout.add(this.startWindowCronExpressionTf, 4);
        this.formLayout.add(this.contextTtlDays, 2);
        this.formLayout.add(this.contextTtlHours, 2);
        this.formLayout.add(this.contextTtlMinutes, 2);
        this.formLayout.add(this.timezoneTf, 3);
        this.formLayout.add(environmentTf, 3);
        this.formLayout.add(this.descriptionTa, 6);
        this.formLayout.add(this.startTimeTf, 4);
        this.formLayout.add(this.projectedEndTimeTf, 4);
        this.formLayout.add(this.endTimeTf, 4);
        this.formLayout.add(cbLayout, 3);

        HorizontalLayout jobStatusLayout = new HorizontalLayout();
        jobStatusLayout.setWidthFull();
        this.waitingStatus.setSizeFull();
        this.completeStatus.setSizeFull();
        this.runningStatus.setSizeFull();
        this.queuedStatus.setSizeFull();
        this.onHoldStatus.setSizeFull();
        this.skippedStatus.setSizeFull();
        this.errorStatus.setSizeFull();
        this.errorAckedStatus.setSizeFull();
        Button refreshButton = new Button();
        refreshButton.setWidth("50px");
        refreshButton.getElement().appendChild(VaadinIcon.REFRESH.create().getElement());
        refreshButton.addClickListener(buttonClickEvent -> {
            this.refreshJobStatusWidget();
        });
        jobStatusLayout.add(this.waitingStatus, this.completeStatus, this.runningStatus, this.queuedStatus
            , this.onHoldStatus, this.skippedStatus, this.errorStatus, this.errorAckedStatus, refreshButton);

        this.formLayout.add(jobStatusLayout, 22);

        this.contextInstanceDetailsCollapsableLayout = new CollapsableLayout();
        add(this.contextInstanceDetailsCollapsableLayout);

        //A border to show the outline of the layout itself
        this.contextInstanceDetailsCollapsableLayout.getElement().getStyle().set("border", "1px solid #aaa");

        this.contextInstanceDetailsCollapsableLayout.addContentComponent(formLayout);

        //Add a header button that toggles the visibility on click
        Button collapseButton = new Button(getTranslation("button.show", UI.getCurrent().getLocale())
            , e -> this.contextInstanceDetailsCollapsableLayout.toggleContentVisibility());
        if(this.contextInstance.isRunContextUntilManuallyEnded()) {
            this.addManuallyEndMessage();
        }
        this.contextInstanceDetailsCollapsableLayout.addHeaderComponentAsLastAndAlignToRight(collapseButton);

        //Change the button caption based on the collapse state change
        this.contextInstanceDetailsCollapsableLayout.addCollapseChangeListener(e -> {
            collapseButton.setText(e.isCurrentlyVisible() ? getTranslation("button.hide", UI.getCurrent().getLocale())
                : getTranslation("button.show", UI.getCurrent().getLocale()));
            contextInstanceDetailsCollapsableLayout.getElement().getStyle().set("border", !e.isCurrentlyVisible() ? "1px solid #aaa" : "");
        });

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        this.initialiseEditor();
        stopWatch.stop();
        logger.info(String.format("Initialising context instance editor. Context Instance Name:[%s], Context Instance Id:[%s], Elapsed mill:[%s]"
            , contextInstance.getName(), contextInstance.getId(), stopWatch.getTime()));
        stopWatch.reset();

        stopWatch.start();
        this.initialiseTree();
        stopWatch.stop();
        logger.info(String.format("Initialising context instance tree view. Context Instance Name:[%s], Context Instance Id:[%s], Elapsed mill:[%s]"
            , contextInstance.getName(), contextInstance.getId(), stopWatch.getTime()));
        stopWatch.reset();

        stopWatch.start();
        this.initialiseVisualisation(moduleMetaDataService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, logStreamingService);
        stopWatch.stop();
        logger.info(String.format("Initialising context instance visualisation. Context Instance Name:[%s], Context Instance Id:[%s], Elapsed mill:[%s]"
            , contextInstance.getName(), contextInstance.getId(), stopWatch.getTime()));
        stopWatch.reset();

        stopWatch.start();
        this.initialiseSchedulerJobGridWidget(scheduledContextInstanceService, moduleMetaDataService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        stopWatch.stop();
        logger.info(String.format("Initialising context instance job grid. Context Instance Name:[%s], Context Instance Id:[%s], Elapsed mill:[%s]"
            , contextInstance.getName(), contextInstance.getId(), stopWatch.getTime()));
        stopWatch.reset();

        stopWatch.start();
        this.initialiseContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseContextInstanceAuditWidget(scheduledContextInstanceService);
        stopWatch.stop();
        logger.info(String.format("Initialising context instance audit table. Context Instance Name:[%s], Context Instance Id:[%s], Elapsed mill:[%s]"
            , contextInstance.getName(), contextInstance.getId(), stopWatch.getTime()));
        stopWatch.reset();

        stopWatch.start();
        this.deadLetterQueueManagementWidget = new DeadLetterQueueManagementWidget(this.contextInstance, this.systemEventLogger);
        this.deadLetterQueueManagementWidget.setVisible(false);
        stopWatch.stop();
        logger.info(String.format("Initialising context instance DLQ tab. Context Instance Name:[%s], Context Instance Id:[%s], Elapsed mill:[%s]"
            , contextInstance.getName(), contextInstance.getId(), stopWatch.getTime()));
        stopWatch.reset();

        stopWatch.start();
        this.initialiseTabs();
        stopWatch.stop();
        logger.info(String.format("Initialising context instance tabs. Context Instance Name:[%s], Context Instance Id:[%s], Elapsed mill:[%s]"
            , contextInstance.getName(), contextInstance.getId(), stopWatch.getTime()));

        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(this.tabs);
        this.getStyle().set("padding-top", "0px");

        this.add(statusLayout, headerLayout, contextInstanceDetailsCollapsableLayout, tabLayout, this.contextInstanceTreeViewWidget, this.aceEditor, this.splitContextInstanceVisualisation
            , this.schedulerJobInstanceGridWidget, this.contextTemplateStatisticsWidget, this.contextInstanceAuditWidget, this.deadLetterQueueManagementWidget);
        this.expand(this.splitContextInstanceVisualisation, this.aceEditor);
        this.setHeight("100%");

        // Hack to make the tree widget full height.
        this.tabs.setSelectedTab(this.auditTab);
        this.tabs.setSelectedTab(this.treeTab);


        if(this.selectedTab != null) {
            if(this.selectedTab.equals(ContextInstanceWidget.JOB_INSTANCE_TAB)) {
                this.tabs.setSelectedTab(this.jobsTab);
            }
            else if(this.selectedTab.equals(ContextInstanceWidget.AUDIT_TAB)) {
                this.tabs.setSelectedTab(this.auditTab);
            }
            else if(this.selectedTab.equals(ContextInstanceWidget.RAW_CONTEXT_TAB)) {
                this.tabs.setSelectedTab(this.rawContextTab);
            }
            else if(this.selectedTab.equals(ContextInstanceWidget.STATISTICS_TAB)) {
                this.tabs.setSelectedTab(this.statisticsTab);
            }
            else if(this.selectedTab.equals(ContextInstanceWidget.VISUALISATION_TAB)) {
                this.tabs.setSelectedTab(this.visualisationTab);
            }
            else if(this.selectedTab.equals(ContextInstanceWidget.DAG_TAB)) {
                this.tabs.setSelectedTab(this.dagTab);
            }
            else if(this.selectedTab.equals(ContextInstanceWidget.TREE_TAB)) {
                this.tabs.setSelectedTab(this.treeTab);
            }
        }
    }

    /**
     * Initialise the tabs associated with the widget.
     */
    private void initialiseTabs() {
        this.visualisationTab = new Tab(getTranslation("tab.visualisation", UI.getCurrent().getLocale()));
        this.dagTab = new Tab(getTranslation("tab.dag-view", UI.getCurrent().getLocale()));
        this.treeTab = new Tab(getTranslation("tab.tree", UI.getCurrent().getLocale()));
        this.rawContextTab = new Tab(getTranslation("tab.json-raw-format", UI.getCurrent().getLocale()));
        this.jobsTab = new Tab(getTranslation("tab.job-instances", UI.getCurrent().getLocale()));
        this.statisticsTab = new Tab(getTranslation("tab.statistics", UI.getCurrent().getLocale()));
        this.auditTab = new Tab(getTranslation("tab.audit", UI.getCurrent().getLocale()));
        this.dlqTab = new Tab(getTranslation("tab.dlq", UI.getCurrent().getLocale()));

        this.tabs = new Tabs();
        this.tabs.add(this.treeTab, this.visualisationTab, this.dagTab, this.rawContextTab
            , this.jobsTab/**, todo will introduce statisticsTab in future iteration this.statisticsTab */
            , this.auditTab, this.dlqTab);

        tabs.addSelectedChangeListener(event -> {
            if(tabs.getSelectedTab().equals(this.statisticsTab)) {
                this.aceEditor.setVisible(false);
                if(dagComponent!= null)this.dagComponent.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(true);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
                this.deadLetterQueueManagementWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                this.updateJson(this.contextInstance);
                if(dagComponent!= null)this.dagComponent.setVisible(false);
                this.aceEditor.setVisible(true);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
                this.deadLetterQueueManagementWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                this.splitContextInstanceVisualisation.initialiseVisualisation();
                if(dagComponent!= null)this.dagComponent.setVisible(false);
                this.aceEditor.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(true);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
                this.deadLetterQueueManagementWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.dagTab)) {
                if(this.dagComponent == null) {
                    try {
                        ContextTemplateToDagConverter contextTemplateToDagConverter = new ContextTemplateToDagConverter();
                        List<DagNode> dagNodes = contextTemplateToDagConverter.convert(contextInstance);
                        this.dagComponent = new DagComponent(ObjectMapperFactory.newInstance().writeValueAsString(dagNodes), this.moduleMetaDataService,
                            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService, this.schedulerJobInstanceService,
                            this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService, this.contextProfileService, this.globalEventService,
                            this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance, this.contextInstance);

                        this.add(dagComponent);
                    } catch (JacksonException e) {
                        throw new RuntimeException(e);
                    }
                }
                this.dagComponent.setVisible(true);
                this.aceEditor.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
                this.deadLetterQueueManagementWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.jobsTab)) {
                this.aceEditor.setVisible(false);
                if(dagComponent!= null)this.dagComponent.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(true);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
                this.deadLetterQueueManagementWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.auditTab)) {
                this.aceEditor.setVisible(false);
                if(dagComponent!= null)this.dagComponent.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(true);
                this.contextInstanceTreeViewWidget.setVisible(false);
                this.deadLetterQueueManagementWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.treeTab)) {
                this.aceEditor.setVisible(false);
                if(dagComponent!= null)this.dagComponent.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(true);
                this.deadLetterQueueManagementWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.dlqTab)) {
                this.aceEditor.setVisible(false);
                if(dagComponent!= null)this.dagComponent.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
                this.deadLetterQueueManagementWidget.setVisible(true);
            }
        });
    }

    /**
     * Initialise the editor associated with the widget.
     */
    protected void initialiseEditor()
    {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.json);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setWidth("100%");
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
        aceEditor.setVisible(false);

        this.updateJson(this.contextInstance);
    }

    private void refreshJobStatusWidget() {
        Optional<ContextInstanceAggregateJobStatus> status = AggregateStatusCollector.instance().getContextInstanceAggregateJobStatuses().stream()
            .filter(contextInstanceAggregateJobStatus -> contextInstanceAggregateJobStatus.getContextInstanceId().equals(this.contextInstance.getId()))
            .findFirst();

        if(this.ui.isAttached() && status.isPresent()) {
            ui.access(() -> {
                waitingStatus.setStatus(InstanceStatus.WAITING, status.get().getStatusCount(InstanceStatus.WAITING) + " " + getTranslation(InstanceStatus.WAITING.getTranslationLabel(), UI.getCurrent().getLocale()));
                completeStatus.setStatus(InstanceStatus.COMPLETE, status.get().getStatusCount(InstanceStatus.COMPLETE) + " " + getTranslation(InstanceStatus.COMPLETE.getTranslationLabel(), UI.getCurrent().getLocale()));
                runningStatus.setStatus(InstanceStatus.RUNNING, status.get().getStatusCount(InstanceStatus.RUNNING) + " " + getTranslation(InstanceStatus.RUNNING.getTranslationLabel(), UI.getCurrent().getLocale()));
                queuedStatus.setStatus(InstanceStatus.LOCK_QUEUED, status.get().getStatusCount(InstanceStatus.LOCK_QUEUED) + " " + getTranslation(InstanceStatus.LOCK_QUEUED.getTranslationLabel(), UI.getCurrent().getLocale()));
                onHoldStatus.setStatus(InstanceStatus.ON_HOLD, status.get().getStatusCount(InstanceStatus.ON_HOLD) + " " + getTranslation(InstanceStatus.ON_HOLD.getTranslationLabel(), UI.getCurrent().getLocale()));
                skippedStatus.setStatus(InstanceStatus.SKIPPED, status.get().getStatusCount(InstanceStatus.SKIPPED) + " " + getTranslation(InstanceStatus.SKIPPED.getTranslationLabel(), UI.getCurrent().getLocale()));
                errorStatus.setStatus(InstanceStatus.ERROR, status.get().getStatusCount(InstanceStatus.ERROR) + " " + getTranslation(InstanceStatus.ERROR.getTranslationLabel(), UI.getCurrent().getLocale()));
                errorAckedStatus.setStatus(InstanceStatus.ERROR_ACKNOWLEDGED, status.get().getStatusCount(InstanceStatus.ERROR_ACKNOWLEDGED) + " " + getTranslation(InstanceStatus.ERROR_ACKNOWLEDGED.getTranslationLabel(), UI.getCurrent().getLocale()));
            });
        }
        else if(this.ui.isAttached() && !status.isPresent()) {
            ui.access(() -> {
                waitingStatus.setStatus(InstanceStatus.WAITING, 0 + " " + getTranslation(InstanceStatus.WAITING.getTranslationLabel(), UI.getCurrent().getLocale()));
                completeStatus.setStatus(InstanceStatus.COMPLETE, 0 + " " + getTranslation(InstanceStatus.COMPLETE.getTranslationLabel(), UI.getCurrent().getLocale()));
                runningStatus.setStatus(InstanceStatus.RUNNING, 0 + " " + getTranslation(InstanceStatus.RUNNING.getTranslationLabel(), UI.getCurrent().getLocale()));
                queuedStatus.setStatus(InstanceStatus.LOCK_QUEUED, 0 + " " + getTranslation(InstanceStatus.LOCK_QUEUED.getTranslationLabel(), UI.getCurrent().getLocale()));
                onHoldStatus.setStatus(InstanceStatus.ON_HOLD, 0 + " " + getTranslation(InstanceStatus.ON_HOLD.getTranslationLabel(), UI.getCurrent().getLocale()));
                skippedStatus.setStatus(InstanceStatus.SKIPPED, 0 + " " + getTranslation(InstanceStatus.SKIPPED.getTranslationLabel(), UI.getCurrent().getLocale()));
                errorStatus.setStatus(InstanceStatus.ERROR, 0 + " " + getTranslation(InstanceStatus.ERROR.getTranslationLabel(), UI.getCurrent().getLocale()));
                errorAckedStatus.setStatus(InstanceStatus.ERROR_ACKNOWLEDGED, 0 + " " + getTranslation(InstanceStatus.ERROR_ACKNOWLEDGED.getTranslationLabel(), UI.getCurrent().getLocale()));
            });
        }
    }

    private void addManuallyEndMessage() {
        H4 endManually = new H4("This instance must be ended manually!".toUpperCase());
        endManually.getElement().getStyle().set("color", IkasanColours.SCHEDULER_ERROR);
        endManually.getStyle().set("position", "absolute");
        endManually.getStyle().set("left", "50%");
        endManually.getStyle().set("margin-left", "-220px");
        this.contextInstanceDetailsCollapsableLayout.addHeaderComponent(endManually);
    }

    private void updateJson(ContextInstance contextInstance) {
        ContextService contextService = new ContextService();

        try {
            aceEditor.setValue(contextService.getContextInstanceString(contextInstance));
        }
        catch (JacksonException e) {
            logger.error("Could not update raw JSON", e);
        }
    }

    /**
     * Initial the visualisation associated with the widget.
     *
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param logStreamingService
     */
    protected void initialiseVisualisation(ModuleMetaDataService moduleMetaDataService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                           LogStreamingService logStreamingService) {

        this.splitContextInstanceVisualisation = new SplitContextInstanceVisualisation(this.scheduledContextInstanceService, moduleMetaDataService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, logStreamingService,
            this.contextInstance, this.schedulerJobInstanceService, this.jobInitiationService, this.contextProfileService, this.jobUtilsService, this.scheduledContextService, this.globalEventService,
            this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);
        this.splitContextInstanceVisualisation.setHeight("100%");
    }

    /**
     * Helper method to create the button layout.
     *
     * @return
     */
    private VerticalLayout createButtonLayout() {
        Dialog actionPopup = new Dialog();
        actionPopup.setId("actionPopup");

        jobLockDashboard = new Button(getTranslation("button.jobs-locks", UI.getCurrent().getLocale())
            , VaadinIcon.LOCK.create());
        jobLockDashboard.setId("jobLockDashboard");
        jobLockDashboard.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        jobLockDashboard.setIconAfterText(true);
        jobLockDashboard.addClickListener(event -> {
            actionPopup.close();
            JobLockCacheDialog jobLockCacheDialog = new JobLockCacheDialog(this.contextInstance, this.moduleMetaDataService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobInstanceService,
                this.logStreamingService, this.jobInitiationService, this.scheduledContextService, this.jobUtilsService, this.scheduledContextInstanceService,
                this.contextProfileService, this.globalEventService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing,
                this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);

            jobLockCacheDialog.open();
        });

        ComponentSecurityVisibility.applySecurity(jobLockDashboard, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        this.holdContextButton = new Button(getTranslation("button.hold-context"
            , UI.getCurrent().getLocale()), VaadinIcon.HAND.create());
        this.holdContextButton.setId("holdContextButton");
        this.holdContextButton.setIconAfterText(true);
        this.holdContextButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        this.holdContextButton.addClickListener(event -> {
            actionPopup.close();
            HoldAllCommandExecutionJobsForContextInstanceCommand holdAllCommandExecutionJobsForContextInstanceCommand
                = new HoldAllCommandExecutionJobsForContextInstanceCommand(this.contextInstance, this.schedulerJobInstanceService,
                this.systemEventLogger, this.authentication);
            holdAllCommandExecutionJobsForContextInstanceCommand.execute();
        });

        ComponentSecurityVisibility.applySecurity(holdContextButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.releaseContextButton = new Button(getTranslation("button.release-all-held-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.HANDS_UP.create());
        this.releaseContextButton.setId("releaseContextButton");
        this.releaseContextButton.setIconAfterText(true);
        this.releaseContextButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));

        this.releaseContextButton.addClickListener(event -> {
            actionPopup.close();
            ReleaseAllCommandExecutionJobsForContextInstanceCommand releaseAllCommandExecutionJobsForContextInstanceCommand
                = new ReleaseAllCommandExecutionJobsForContextInstanceCommand(this.contextInstance, this.schedulerJobInstanceService,
                this.systemEventLogger, this.authentication);
            releaseAllCommandExecutionJobsForContextInstanceCommand.execute();
        });

        ComponentSecurityVisibility.applySecurity(releaseContextButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.enableQuartzScheduledJobsButton = new Button(getTranslation("button.enable-quartz-scheduled-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.PLAY.create());
        this.enableQuartzScheduledJobsButton.setId("enableQuartzScheduledJobsButton");
        this.enableQuartzScheduledJobsButton.setIconAfterText(true);
        this.enableQuartzScheduledJobsButton.setVisible(false);
        this.enableQuartzScheduledJobsButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));

        this.disableQuartzScheduledJobsButton = new Button(getTranslation("button.disable-quartz-scheduled-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.BAN.create());
        this.disableQuartzScheduledJobsButton.setId("disableQuartzScheduledJobsButton");
        this.disableQuartzScheduledJobsButton.setIconAfterText(true);
        this.disableQuartzScheduledJobsButton.setVisible(false);
        this.disableQuartzScheduledJobsButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
        ContextInstance liveContext = contextMachine != null ? contextMachine.getContext() : null;

        if(liveContext == null) {
            this.enableQuartzScheduledJobsButton.setVisible(false);
            this.disableQuartzScheduledJobsButton.setVisible(false);
        }
        else if(liveContext.isQuartzScheduleDrivenJobsDisabledForContext()
            && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
            enableQuartzScheduledJobsButton.setVisible(true);
            disableQuartzScheduledJobsButton.setVisible(false);
        }
        else if(!liveContext.isQuartzScheduleDrivenJobsDisabledForContext()
            && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
            enableQuartzScheduledJobsButton.setVisible(false);
            disableQuartzScheduledJobsButton.setVisible(true);
        }

        enableQuartzScheduledJobsButton.addClickListener(event -> {
            actionPopup.close();
            if (showFollowerNodeNotSupportedDialog()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.enable-scheduled-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.enable-scheduled-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ContextMachine machine = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId());

                if (machine != null) {
                    boolean error = false;
                    try {
                        machine.enableQuartzBasedJobs();
                        enableQuartzScheduledJobsButton.setVisible(false);
                        disableQuartzScheduledJobsButton.setVisible(true);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_SCHEDULED_JOBS_ENABLED, String.format("Job Plan Name[%s], Job Plan Identifier[%s]"
                            , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                        ContextInstanceSavedEventBroadcaster.instance().broadcast(ContextMachineCache.instance()
                            .getByContextInstanceId(this.contextInstance.getId()).getContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = true;
                    } finally {
                        if (error) {
                            NotificationHelper.showUserNotification(getTranslation("notification.enable-scheduled-jobs-error"
                                , UI.getCurrent().getLocale()));
                        } else {
                            NotificationHelper.showUserNotification(getTranslation("notification.enabled-scheduled-successfully"
                                , UI.getCurrent().getLocale()));
                        }
                    }
                }
            });
        });

        disableQuartzScheduledJobsButton.addClickListener(event -> {
            actionPopup.close();
            if (showFollowerNodeNotSupportedDialog()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.disable-scheduled-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.disable-scheduled-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ContextMachine machine = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId());

                if (machine != null) {
                    boolean error = false;
                    try {
                        machine.disableQuartzBasedJobs();
                        enableQuartzScheduledJobsButton.setVisible(true);
                        disableQuartzScheduledJobsButton.setVisible(false);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_SCHEDULED_JOBS_DISABLED, String.format("Job Plan Instance Name[%s], Job Plan Instance Identifier[%s]"
                            , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                        ContextInstanceSavedEventBroadcaster.instance().broadcast(ContextMachineCache.instance()
                            .getByContextInstanceId(this.contextInstance.getId()).getContext());
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
                }
            });
        });

        this.contextInstanceEndButton = new Button(getTranslation("label.manually-end-job-plan", UI.getCurrent().getLocale()), VaadinIcon.STOP.create());
        this.contextInstanceEndButton.setId("contextInstanceEndButton");
        this.contextInstanceEndButton.setIconAfterText(true);
        this.contextInstanceEndButton.setVisible(this.contextInstance.isRunContextUntilManuallyEnded() && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.contextInstanceEndButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        this.contextInstanceEndButton.addClickListener(event -> {
            actionPopup.close();
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.end-job-plan-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.end-job-plan-body", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.open(getTranslation("progress-dialog.manually-end-job-plan-instance-header", UI.getCurrent().getLocale()),
                    getTranslation("progress-dialog.manually-end-job-plan-instance-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextTemplateManagementWidget"));
                executor.execute(() -> {
                    boolean success = true;
                    try {
                        this.contextInstanceRegistrationService.deregisterManually(this.contextInstance.getId());

                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_MANUALLY_ENDED, String.format("Job Plan Name[%s], Job Plan Instance Identifier[%s]"
                            , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                        ContextInstanceSavedEventBroadcaster.instance().broadcast(contextInstance);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        success = false;
                        dialog.close();
                        current.access(() -> NotificationHelper.showErrorNotification(getTranslation("notification.job-plan-ended-error", UI.getCurrent().getLocale())));
                    }

                    if(success) {
                        current.access(() -> {
                            dialog.close();
                            NotificationHelper.showUserNotification(getTranslation("notification.job-plan-ended-successfully", UI.getCurrent().getLocale()));
                        });
                    }
                });
            });
        });

        this.ignoreContextInstanceEndButton = new Button(getTranslation("label.ignore-job-plan-duration"
            , UI.getCurrent().getLocale()), VaadinIcon.CONTROLLER.create());
        this.ignoreContextInstanceEndButton.setId("ignoreContextInstanceEndButton");
        this.ignoreContextInstanceEndButton.setIconAfterText(true);
        this.ignoreContextInstanceEndButton.setVisible(!this.contextInstance.isRunContextUntilManuallyEnded() && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.ignoreContextInstanceEndButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        this.ignoreContextInstanceEndButton.addClickListener(event -> {
            actionPopup.close();
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.ignore-job-plan-duration-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.ignore-job-plan-duration-body", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    ContextMachine runUntilMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                    if(runUntilMachine != null) {
                        runUntilMachine.runContextUntilManuallyEnded();
                        ignoreContextInstanceEndButton.setVisible(false);
                        contextInstanceEndButton.setVisible(true);

                        this.projectedEndTimeTf.setValue("This instance must be ended manually!");
                    }
                    this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_DURATION_IGNORED, String.format("Job Plan Name[%s], Job Plan Instance Identifier[%s]"
                        , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                    NotificationHelper.showUserNotification(getTranslation("notification.job-plan-duration-ignored", UI.getCurrent().getLocale()));
                    this.addManuallyEndMessage();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showUserNotification(getTranslation("notification.job-plan-duration-ignored-error", UI.getCurrent().getLocale()));
                }

            });
        });

        this.resetContextButton = new Button(getTranslation("button.reset-context"
            , UI.getCurrent().getLocale()), VaadinIcon.TIME_BACKWARD.create());
        this.resetContextButton.setId("resetContextButton");
        this.resetContextButton.setIconAfterText(true);
        this.resetContextButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        this.resetContextButton.addClickListener(event -> {
            actionPopup.close();
            if (showFollowerNodeNotSupportedDialog()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.reset-context", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));

            Checkbox hold = new Checkbox(getTranslation("label.hold-all-command-execution-jobs", UI.getCurrent().getLocale()));
            Checkbox initiateSameParams = new Checkbox(getTranslation("label.initiate-job-plan-instance-with-same-params", UI.getCurrent().getLocale()));
            Checkbox modifyParams = new Checkbox(getTranslation("label.update-params-prior-to-initiating-job-plan-instance", UI.getCurrent().getLocale()));
            VerticalLayout verticalLayout = new VerticalLayout();
            verticalLayout.setWidthFull();
            Div body = new Div();
            body.setText(getTranslation("confirm-dialog-text.reset-context", UI.getCurrent().getLocale()));
            verticalLayout.add(body, hold, initiateSameParams, modifyParams);
            confirmDialog.setText(verticalLayout);

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(modifyParams.getValue()) {
                    this.resetContextInstanceWithModifiedContextParams(hold.getValue(), initiateSameParams.getValue());
                }
                else {
                    this.resetContextInstance(hold.getValue(), initiateSameParams.getValue(), null);
                }
            });
        });

        this.contextInstanceParameterButton = new Button(getTranslation("button.context-parameters", UI.getCurrent().getLocale()), VaadinIcon.LINES.create());
        this.contextInstanceParameterButton.setIconAfterText(true);
        this.contextInstanceParameterButton.setEnabled(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        this.contextInstanceParameterButton.addClickListener(event -> {
            actionPopup.close();
            ContextInstanceParameterDialog contextInstanceParameterDialog =  new ContextInstanceParameterDialog(true);
            contextInstanceParameterDialog.initParams(this.contextInstance.getContextParameters());
            contextInstanceParameterDialog.open();

            contextInstanceParameterDialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened() && contextInstanceParameterDialog.isSaveClose()) {
                    try {
                        ContextMachine paramMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                        if (paramMachine != null) {
                            paramMachine.updateContextParameters(contextInstanceParameterDialog.getContextParameters());
                            NotificationHelper.showUserNotification(getTranslation("notification.job-plan-instance-parameters-updated-successfully"
                                , UI.getCurrent().getLocale()));
                        } else {
                            NotificationHelper.showUserNotification(getTranslation("notification.job-plan-instance-parameters-not-updated-no-context-instance"
                                , UI.getCurrent().getLocale()));
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();

                        NotificationHelper.showUserNotification(getTranslation("notification.error-updating-context-instance-parameters"
                            , UI.getCurrent().getLocale()));
                    }
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(resetContextButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_ADMIN,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE);

        Button synchronisePlanWithAgentButton = new Button(getTranslation("button.synchronise-instance-with-agent"), VaadinIcon.REFRESH.create());
        synchronisePlanWithAgentButton.addClickListener(event -> {
            if (showFollowerNodeNotSupportedDialog()) {
                return;
            }
            try {
                contextMachine.propagateContextInstanceToAgents();
            }
            catch (Exception e) {
                NotificationHelper.showErrorNotification(getTranslation("notification.synchronise-instance-with-agent-error"));
                return;
            }

            NotificationHelper.showUserNotification(getTranslation("notification.synchronise-instance-with-agent-success"));
        });

        ComponentSecurityVisibility.applySecurity(synchronisePlanWithAgentButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_ADMIN);

        Button systemEventsButton = new Button(getTranslation("button.system-events"), VaadinIcon.CROSSHAIRS.create());
        systemEventsButton.addClickListener(event -> {
            ContextInstanceSystemEventHistoryDialog contextInstanceSystemEventHistoryDialog
                = new ContextInstanceSystemEventHistoryDialog(this.contextInstance, this.systemEventSearchService);
            contextInstanceSystemEventHistoryDialog.open();
        });

        Button actionsButton = new Button(getTranslation("button.actions", UI.getCurrent().getLocale()), VaadinIcon.MENU.create());
        actionsButton.setId("actionsButton");
        actionsButton.addClickListener(buttonClickEvent -> {
            actionPopup.open();
            actionPopup
                .getElement()
                .executeJs(
                    "$0.$.overlay.$.overlay.style['align-self']='flex-start';" +
                        "$0.$.overlay.$.overlay.style['position']='absolute';" +
                        "$0.$.overlay.$.overlay.style['top']= ($1.getBoundingClientRect().top + $1.getBoundingClientRect().height )+ 'px';" +
                        "$0.$.overlay.$.overlay.style['left']= ($1.getBoundingClientRect().left - 300) + 'px'",
                    actionPopup,
                    actionsButton
                );
            actionPopup.open();
        });

        VerticalLayout popupLayout = new VerticalLayout();
        popupLayout.setSizeFull();
        popupLayout.add(jobLockDashboard, holdContextButton, releaseContextButton, enableQuartzScheduledJobsButton,
            disableQuartzScheduledJobsButton, contextInstanceEndButton, ignoreContextInstanceEndButton,
            contextInstanceParameterButton, resetContextButton);
        actionPopup.add(popupLayout);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(synchronisePlanWithAgentButton, systemEventsButton, actionsButton);
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);

        VerticalLayout wrapper = new VerticalLayout(buttonLayout);
        wrapper.setWidthFull();
        wrapper.setMargin(false);
        wrapper.setPadding(false);
        wrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        return wrapper;
    }

    public void setLeaderElectionService(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    /**
     * Shows a dialog explaining that the requested operation is not available on follower nodes.
     * Returns true if the dialog was shown so callers can short-circuit:
     * {@code if (showFollowerNodeNotSupportedDialog()) return;}
     *
     * Strategy (two-layer):
     * 1. ZooKeeper via the registered LeaderProvider — authoritative; correctly handles the
     *    stale-former-leader case where local cache entries still exist after losing leadership.
     *    Available when clustering is properly configured (fixed-role or ZooKeeper).
     * 2. LeaderElectionService injected directly (setter) — used when available, same authority.
     * 3. isLocal() fallback — reliable for deployments where neither leader-election source has
     *    been wired in (e.g. default ZooKeeper config with ZooKeeper disabled). Misses the
     *    stale-former-leader case but is safe for standard single/unconfigured setups.
     */
    private boolean showFollowerNodeNotSupportedDialog() {
        boolean isFollower = !ContextMachineCache.instance().isLeader();
        if (!isFollower && leaderElectionService != null) {
            isFollower = !leaderElectionService.isLeader();
        }
        if (!isFollower) {
            ContextMachine machine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
            isFollower = machine != null && !machine.isLocal();
        }
        if (!isFollower) return false;
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Operation Not Available");
        dialog.setText("This operation is not currently supported on follower nodes. Please perform this action on the cluster leader.");
        dialog.setConfirmText("Close");
        dialog.open();
        return true;
    }

    /**
     * Helper method to reset the context instance with modified context parameters.
     *
     * @param hold - hold all command execution jobs.
     * @param initiateSameParams - init the new context instance with parameters from the one it is replacing.
     */
    private void resetContextInstanceWithModifiedContextParams(boolean hold, boolean initiateSameParams) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(this.contextInstance.getId())) return;
        ContextMachine machine = ContextMachineCache.instance()
            .getByContextInstanceId(this.contextInstance.getId());
        if (machine != null) {
            ContextInstanceParameterDialog contextParameterDialog = new ContextInstanceParameterDialog(true);
            contextParameterDialog.initParams(machine.getContext().getContextParameters());

            contextParameterDialog.open();

            contextParameterDialog.addOpenedChangeListener(event -> {
                if(!event.isOpened() && contextParameterDialog.isSaveClose()) {
                    this.resetContextInstance(hold, initiateSameParams, contextParameterDialog.getContextParameters());
                }
            });
        }
    }

    /**
     * Helper method to reset the context instance.
     *
     * @param hold - hold all command execution jobs.
     * @param initiateSameParams - init the new context instance with parameters from the one it is replacing.
     */
    private void resetContextInstance(boolean hold, boolean initiateSameParams, List<ContextParameterInstance> contextParameterInstances) {
        if (!ContextMachineCache.instance().isLeaderForContextInstance(this.contextInstance.getId())) return;
        ContextMachine machine = ContextMachineCache.instance()
            .getByContextInstanceId(this.contextInstance.getId());
        if (machine != null) {
            try {
                machine.setDryRunParameters(null);
                machine.getContext().setEndTime(System.currentTimeMillis());
                InstanceStatus previousStatus = machine.getContext().getStatus();
                this.saveContextInstance(machine.getContext(), InstanceStatus.ENDED);
                // saveContextInstance sets status=ENDED on the in-memory object, so the internal
                // state-change event inside resetContextInstance() sees (ENDED→ENDED) and is
                // suppressed. Broadcast explicitly here so cluster peers see the ENDED transition.
                ContextInstanceStateChangeEventBroadcaster.instance().broadcast(new ContextInstanceStateChangeEventImpl(
                    machine.getContext().getId(), machine.getContext(), previousStatus, InstanceStatus.ENDED));
                machine.getContext().getAllNestedJobLocks().forEach(jobLockInstance -> {
                    JobLockCacheImpl.instance().resetLock(jobLockInstance.getName(), this.contextInstance.getEnvironmentGroup());
                });
                this.statusDiv.setStatus(InstanceStatus.ENDED);
                ContextMachineCache.instance().remove(machine);
                machine.resetContextInstance(hold, initiateSameParams, contextParameterInstances);
                ContextMachineCache.instance().put(machine);
                ContextInstance newInstance = ContextMachineCache.instance()
                    .getByContextInstanceId(machine.getContext().getId()).getContext();

                this.contextInstanceSchedulerService.registerEndJobAndTrigger(newInstance.getName(), CronUtils.buildCronFromOriginal(newInstance.getProjectedEndTime(), newInstance.getTimezone())
                    , newInstance.getTimezone(), newInstance.getId());

                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, newInstance.getId() + "_scheduledContextInstance");

                getUI().ifPresent(ui -> ui.getPage().open(route));

                resetContextButton.setVisible(false);

                this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_RESET, String.format("Job Plan Instance Name[%s], Job Plan Instance Identifier[%s] " +
                    "has been reset and replaced with Job Plan Instance Identifier[%s]", contextInstance.getName(), contextInstance.getId(), newInstance.getId() ), this.authentication.getName());
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.reset-context", UI.getCurrent().getLocale()));
            }
        }
    }

    /**
     * Method to initialise the scheduler job instance grid.
     *
     * @param scheduledContextInstanceService
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     */
    private void initialiseSchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, ModuleMetaDataService moduleMetaDataService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService) {
        this.schedulerJobInstanceGridWidget = new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetaDataService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextInstance, this.schedulerJobInstanceService,
            this.jobInitiationService, this.configurationRestService, metaDataRestService, this.jobUtilsService, this.scheduledContextService, this.jobStatus, this.jobName, this.contextProfileService,
            this.globalEventService, this.systemEventSearchService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);
        this.schedulerJobInstanceGridWidget.setWidthFull();
        this.schedulerJobInstanceGridWidget.setHeight("100%");
        this.schedulerJobInstanceGridWidget.setVisible(false);

    }

    /**
     * Initialise the job plan instance tree.
     */
    private void initialiseTree() {
        this.contextInstanceTreeViewWidget = new ContextInstanceTreeViewWidget(this.contextInstance, this.moduleMetaDataService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
            this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService,
            this.contextProfileService, this.globalEventService, this.systemEventSearchService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance,
            this.contextVisualisationNodeDistance);
        this.contextInstanceTreeViewWidget.setSizeFull();
        this.contextInstanceTreeViewWidget.setVisible(false);
        this.contextInstanceTreeViewWidget.setVisible(true);
    }

    /**
     * Initialise the job plan statistics widget.
     *
     * @param scheduledContextInstanceService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     */
    private void initialiseContextTemplateStatisticsWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                  LogStreamingService logStreamingService) {
        this.contextTemplateStatisticsWidget = new ContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextTemplate);
        this.contextTemplateStatisticsWidget.setWidthFull();
        this.contextTemplateStatisticsWidget.setHeight("75vh");
        this.contextTemplateStatisticsWidget.setVisible(false);

    }

    /**
     * Helper method to initialise the job plan instance audit widget.
     *
     * @param scheduledContextInstanceService
     */
    private void initialiseContextInstanceAuditWidget(ScheduledContextInstanceService scheduledContextInstanceService) {
        ScheduledContextInstanceAuditAggregateSearchFilter contextInstanceSearchFilter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        contextInstanceSearchFilter.setContextInstanceId(this.contextInstance.getId());
        this.contextInstanceAuditWidget = new ContextInstanceAuditWidget(scheduledContextInstanceService
            , contextInstanceSearchFilter, false);
        this.contextInstanceAuditWidget.setWidthFull();
        this.contextInstanceAuditWidget.setHeight("75vh");
        this.contextInstanceAuditWidget.setVisible(true);

    }

    /**
     * Helper method to save the job plan instance.
     *
     * @param contextInstance
     * @param instanceStatus
     */
    protected void saveContextInstance(ContextInstance contextInstance, InstanceStatus instanceStatus) {
        contextInstance.setStatus(instanceStatus);
        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(contextInstance);
        scheduledContextInstanceRecord.setTimestamp(contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        this.refreshJobStatusWidget();

        ContextInstanceStateChangeEventBroadcaster.instance().register(this);
        SchedulerJobStateChangeEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        ContextInstanceStateChangeEventBroadcaster.instance().unregister(this);
        SchedulerJobStateChangeEventBroadcaster.instance().unregister(this);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.init(".", moduleMetaDataService, configurationRestService, moduleControlRestService,
            metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.setWidthFull();
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if (event.getContextInstance() != null) {
            if(this.ui != null && this.ui.isAttached() && this.contextInstance.getId().equals(event.getContextInstance().getId())) {
                this.ui.access(() -> {
                    this.contextInstance = event.getContextInstance();
                    ContextHelper.enrichJobs(this.contextInstance);
                    this.statusDiv.setStatus(event.getNewStatus());


                    if(this.contextInstance.getStatus().equals(InstanceStatus.ENDED)) {
                        this.jobLockDashboard.setEnabled(false);
                        this.contextInstanceEndButton.setEnabled(false);
                        this.resetContextButton.setEnabled(false);
                        this.ignoreContextInstanceEndButton.setEnabled(false);
                        this.disableQuartzScheduledJobsButton.setEnabled(false);
                        this.holdContextButton.setEnabled(false);
                        this.releaseContextButton.setEnabled(false);
                        this.enableQuartzScheduledJobsButton.setEnabled(false);
                        this.contextInstanceParameterButton.setEnabled(false);
                    }
                });
            }
        }
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        if(this.ui != null && this.ui.isAttached() && event.getContextInstance().getId().equals(this.contextInstance.getId())) {
            this.ui.access(() -> {
                ScheduledContextInstanceRecord record = this.scheduledContextInstanceService
                    .findById(this.contextInstance.getId() + "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);
                if (record != null) {
                    this.contextInstance = record.getContextInstance();
                    ContextHelper.enrichJobs(this.contextInstance);
                    this.refreshJobStatusWidget();
                }
            });
        }
    }
}
