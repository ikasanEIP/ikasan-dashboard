package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.shared.Registration;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.util.ContextInstanceSavedEventBroadcaster;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SplitContextInstanceVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.util.ContextDurationUtils;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextInstanceWidget extends VerticalLayout implements BeforeEnterObserver {

    private static Logger logger = LoggerFactory.getLogger(ContextInstanceWidget.class);
    public static final String TREE_TAB = "treeTab";
    public static final String VISUALISATION_TAB = "visualisationTab";
    public static final String RAW_CONTEXT_TAB =  "rawContextTab";
    public static final String JOB_INSTANCE_TAB = "jobsTab";
    public static final String STATISTICS_TAB = "statisticsTab";
    public static final String AUDIT_TAB = "auditTab";
    private Registration contextInstanceStateChangeRegistration;
    private Registration schedulerJobInstanceStateChangeRegistration;

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
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ModuleMetaDataService moduleMetaDataService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private LogStreamingService logStreamingService;
    private SchedulerJobService schedulerJobService;
    private GlobalEventService globalEventService;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
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

    private Button holdContextButton;
    private Button releaseContextButton;
    private Button enableQuartzScheduledJobsButton;
    private Button disableQuartzScheduledJobsButton;
    private Button contextInstanceEndButton;
    private Button ignoreContextInstanceEndButton;
    private Button resetContextButton;

    private Tab treeTab;
    private Tab visualisationTab;
    private Tab rawContextTab;
    private Tab jobsTab;
    private Tab statisticsTab;
    private Tab auditTab;
    private Tabs tabs;
    private ContextInstance contextInstance;
    private ContextTemplate contextTemplate;
    private SchedulerStatusDiv statusDiv;
    private String selectedTab;
    private String jobStatus;
    private String jobName;

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
    public ContextInstanceWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                 ContextProfileService contextProfileService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                 String selectedTab, String jobStatus, String jobName, GlobalEventService globalEventService, ContextInstanceRegistrationService contextInstanceRegistrationService) {
        this(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService,
            logStreamingService, contextInstance, contextTemplate, schedulerJobInstanceService, jobInitiationService,
            contextProfileService, jobUtilsService, scheduledContextService, globalEventService, contextInstanceRegistrationService);
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
     * @param scheduledProcessManagementService
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
    public ContextInstanceWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                 ContextProfileService contextProfileService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                 GlobalEventService globalEventService, ContextInstanceRegistrationService contextInstanceRegistrationService) {

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
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     */
    private void init(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
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
        this.contextTtlDays.setValue(ContextDurationUtils.getDays(this.contextTemplate.getContextTtlMilliseconds()));
        this.contextTtlDays.setEnabled(false);
        this.contextTtlHours = new IntegerField(getTranslation("label.duration-hours", UI.getCurrent().getLocale()));
        this.contextTtlHours.getElement().getThemeList().add("always-float-label");
        this.contextTtlHours.setValue(ContextDurationUtils.getHours(this.contextTemplate.getContextTtlMilliseconds()));
        this.contextTtlHours.setEnabled(false);
        this.contextTtlMinutes = new IntegerField(getTranslation("label.duration-minutes", UI.getCurrent().getLocale()));
        this.contextTtlMinutes.getElement().getThemeList().add("always-float-label");
        this.contextTtlMinutes.setValue(ContextDurationUtils.getMinutes(this.contextTemplate.getContextTtlMilliseconds()));
        this.contextTtlMinutes.setEnabled(false);


        this.timezoneTf = new TextField(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        this.timezoneTf.getElement().getThemeList().add("always-float-label");
        binder.forField(this.timezoneTf)
            .bind(ContextInstance::getTimezone, ContextInstance::setTimezone);
        this.timezoneTf.setEnabled(false);

        this.startTimeTf = new TextField(getTranslation("label.start-date-time", UI.getCurrent().getLocale()));
        this.startTimeTf.getElement().getThemeList().add("always-float-label");
        if(this.contextInstance.getStartTime() > 0) {
            this.startTimeTf.setValue(DateFormatter.instance().getFormattedDate(this.contextInstance.getStartTime()));
        }
        this.startTimeTf.setEnabled(false);

        this.projectedEndTimeTf = new TextField(getTranslation("label.projected-end-date-time", UI.getCurrent().getLocale()));
        this.projectedEndTimeTf.getElement().getThemeList().add("always-float-label");
        if(this.contextInstance.getProjectedEndTime() > 0) {
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
                + " - " + this.contextTemplate.getName());
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
            new FormLayout.ResponsiveStep("0", 11)
        );

        this.formLayout.setWidth("100%");

        this.formLayout.add(this.contextInstanceId, 4);
        this.formLayout.add(this.startWindowCronExpressionTf, 2);
        this.formLayout.add(this.contextTtlDays, 1);
        this.formLayout.add(this.contextTtlHours, 1);
        this.formLayout.add(this.contextTtlMinutes, 1);
        this.formLayout.add(this.timezoneTf, 2);
        this.formLayout.add(this.descriptionTa, 4);
        this.formLayout.add(this.startTimeTf, 2);
        this.formLayout.add(this.projectedEndTimeTf, 2);
        this.formLayout.add(this.endTimeTf, 2);

        CollapsableLayout collapsableLayout = new CollapsableLayout();
        add(collapsableLayout);

        //A border to show the outline of the layout itself
        collapsableLayout.getElement().getStyle().set("border", "1px solid #aaa");

        collapsableLayout.addContentComponent(formLayout);

        //Add a header button that toggles the visibility on click
        Button collapseButton = new Button(getTranslation("button.show", UI.getCurrent().getLocale())
            , e -> collapsableLayout.toggleContentVisibility());
        collapsableLayout.addHeaderComponentAsLastAndAlignToRight(collapseButton);

        //Change the button caption based on the collapse state change
        collapsableLayout.addCollapseChangeListener(e -> {
            collapseButton.setText(e.isCurrentlyVisible() ? getTranslation("button.hide", UI.getCurrent().getLocale())
                : getTranslation("button.show", UI.getCurrent().getLocale()));
            collapsableLayout.getElement().getStyle().set("border", !e.isCurrentlyVisible() ? "1px solid #aaa" : "");
        });

        this.initialiseEditor();
        this.initialiseTree();
        this.initialiseVisualisation(moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.initialiseSchedulerJobGridWidget(scheduledContextInstanceService, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseContextInstanceAuditWidget(scheduledContextInstanceService);
        this.initialiseTabs();
        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(this.tabs);
        this.getStyle().set("padding-top", "0px");
        this.add(statusLayout, headerLayout, collapsableLayout, tabLayout, this.contextInstanceTreeViewWidget, this.aceEditor, this.splitContextInstanceVisualisation
            , this.schedulerJobInstanceGridWidget, this.contextTemplateStatisticsWidget, this.contextInstanceAuditWidget);
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
        this.treeTab = new Tab(getTranslation("tab.tree", UI.getCurrent().getLocale()));
        this.rawContextTab = new Tab(getTranslation("tab.json-raw-format", UI.getCurrent().getLocale()));
        this.jobsTab = new Tab(getTranslation("tab.job-instances", UI.getCurrent().getLocale()));
        this.statisticsTab = new Tab(getTranslation("tab.statistics", UI.getCurrent().getLocale()));
        this.auditTab = new Tab(getTranslation("tab.audit", UI.getCurrent().getLocale()));

        this.tabs = new Tabs();
        this.tabs.add(this.treeTab, this.visualisationTab, this.rawContextTab
            , this.jobsTab/**, todo will introduce statisticsTab in future iteration this.statisticsTab */, this.auditTab);

        tabs.addSelectedChangeListener(event -> {
            if(tabs.getSelectedTab().equals(this.statisticsTab)) {
                this.aceEditor.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(true);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                this.aceEditor.setVisible(true);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                this.splitContextInstanceVisualisation.initialiseVisualisation();
                this.aceEditor.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(true);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.jobsTab)) {
                this.aceEditor.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(true);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.auditTab)) {
                this.aceEditor.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(true);
                this.contextInstanceTreeViewWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.treeTab)) {
                this.aceEditor.setVisible(false);
                this.splitContextInstanceVisualisation.setVisible(false);
                this.schedulerJobInstanceGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
                this.contextInstanceAuditWidget.setVisible(false);
                this.contextInstanceTreeViewWidget.setVisible(true);
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

    private void updateJson(ContextInstance contextInstance) {
        ContextService contextService = new ContextService();

        try {
            aceEditor.setValue(contextService.getContextInstanceString(contextInstance));
        }
        catch (JsonProcessingException e) {
            logger.error("Could not update raw JSON", e);
        }
    }

    /**
     * Initial the visualisation associated with the widget.
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
    protected void initialiseVisualisation(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService) {

        this.splitContextInstanceVisualisation = new SplitContextInstanceVisualisation(scheduledContextInstanceService, moduleMetaDataService, scheduledProcessManagementService,
                configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, logStreamingService,
                contextInstance, schedulerJobInstanceService, jobInitiationService, contextProfileService, jobUtilsService, scheduledContextService, this.globalEventService);
        this.splitContextInstanceVisualisation.setHeight("100%");
    }

    /**
     * Helper method to create the button layout.
     *
     * @return
     */
    private VerticalLayout createButtonLayout() {
        Button jobLockDashboard = new Button(getTranslation("button.jobs-locks", UI.getCurrent().getLocale())
            , VaadinIcon.LOCK.create());
        jobLockDashboard.setVisible(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        jobLockDashboard.setIconAfterText(true);
        jobLockDashboard.addClickListener(event -> {
            JobLockCacheDialog jobLockCacheDialog = new JobLockCacheDialog(this.contextInstance, this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobInstanceService,
                this.logStreamingService, this.jobInitiationService, this.scheduledContextService, this.jobUtilsService, this.scheduledContextInstanceService,
                this.contextProfileService, this.globalEventService);

            jobLockCacheDialog.open();
        });

        ComponentSecurityVisibility.applySecurity(jobLockDashboard, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        this.holdContextButton = new Button(getTranslation("button.hold-context"
            , UI.getCurrent().getLocale()), VaadinIcon.HAND.create());
        this.holdContextButton.setIconAfterText(true);
        this.holdContextButton.setVisible(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        this.holdContextButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.hold-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.hold-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ContextMachine contextMachine = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId());

                if (contextMachine != null) {
                    boolean error = false;
                    try {
                        List<SchedulerJobInstanceRecord> updatedRecords = this.schedulerJobInstanceService
                            .holdJobsWithinContext(contextMachine.getContext(), contextMachine.getContext().getName());

                        if (updatedRecords.size() > 0) {
                            updatedRecords.forEach(schedulerJobInstanceRecord -> {
                                SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
                                    = new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstanceRecord.getSchedulerJobInstance(),
                                    this.contextInstance, InstanceStatus.WAITING, InstanceStatus.ON_HOLD);
                                SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
                            });
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        error = true;
                    }
                    finally {
                        if (error) {
                            NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-hold-error"
                                , UI.getCurrent().getLocale()));
                        } else {
                            NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-successfully-held"
                                , UI.getCurrent().getLocale()));
                        }
                    }
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(holdContextButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.releaseContextButton = new Button(getTranslation("button.release-all-held-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.HANDS_UP.create());
        this.releaseContextButton.setIconAfterText(true);
        this.releaseContextButton.setVisible(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));

        this.releaseContextButton.addClickListener(event -> {
            ContextMachine contextMachine = ContextMachineCache.instance()
                .getByContextInstanceId(this.contextInstance.getId());
            if (contextMachine != null) {
                List<SchedulerJobInstanceRecord> jobsToReleaseWithinContext = this.schedulerJobInstanceService
                    .getJobsToReleaseWithinContext(contextMachine.getContext(), contextMachine.getContext().getName());

                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.release-jobs-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(String.format(getTranslation("confirm-dialog.release-jobs-body", UI.getCurrent().getLocale())
                    , jobsToReleaseWithinContext.size()));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                    dialog.setWidth("600px");
                    dialog.setHeight("250px");
                    dialog.open(getTranslation("progress-dialog.release-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                        getTranslation("progress-dialog.release-all-jobs-jobs-body", UI.getCurrent().getLocale()));

                    final UI current = UI.getCurrent();
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        boolean error = false;
                        try {
                            if (jobsToReleaseWithinContext.size() > 0) {
                                for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : jobsToReleaseWithinContext) {
                                    contextMachine.releaseJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                                        schedulerJobInstanceRecord.getChildContextName());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            error = true;
                        } finally {
                            boolean finalError = error;
                            current.access(() -> {
                                dialog.close();

                                if (finalError) {
                                    NotificationHelper.showErrorNotification(getTranslation("notification.all-jobs-released-error"
                                        , UI.getCurrent().getLocale()));
                                } else {
                                    NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-successfully-released"
                                        , UI.getCurrent().getLocale()));
                                }
                            });
                        }
                    });
                });
            }
        });

        ComponentSecurityVisibility.applySecurity(releaseContextButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.enableQuartzScheduledJobsButton = new Button(getTranslation("button.enable-quartz-scheduled-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.PLAY.create());
        this.enableQuartzScheduledJobsButton.setIconAfterText(true);
        this.enableQuartzScheduledJobsButton.setVisible(false);

        this.disableQuartzScheduledJobsButton = new Button(getTranslation("button.disable-quartz-scheduled-jobs"
            , UI.getCurrent().getLocale()), VaadinIcon.BAN.create());
        this.disableQuartzScheduledJobsButton.setIconAfterText(true);
        this.disableQuartzScheduledJobsButton.setVisible(false);

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());

        if(contextMachine == null) {
            this.enableQuartzScheduledJobsButton.setVisible(false);
            this.disableQuartzScheduledJobsButton.setVisible(false);
        }
        else if(contextMachine.getContext().isQuartzScheduleDrivenJobsDisabledForContext()
            && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
            enableQuartzScheduledJobsButton.setVisible(true);
            disableQuartzScheduledJobsButton.setVisible(false);
        }
        else if(!contextMachine.getContext().isQuartzScheduleDrivenJobsDisabledForContext()
            && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)){
            enableQuartzScheduledJobsButton.setVisible(false);
            disableQuartzScheduledJobsButton.setVisible(true);
        }

        enableQuartzScheduledJobsButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.enable-scheduled-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.enable-scheduled-jobs-body", UI.getCurrent().getLocale()));
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
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_SCHEDULED_JOBS_ENABLED, String.format("Context Instance Name[%s], Context Instance Identifier[%s]"
                            , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                        ContextInstanceSavedEventBroadcaster.broadcast(ContextMachineCache.instance()
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
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.disable-scheduled-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.disable-scheduled-jobs-body", UI.getCurrent().getLocale()));
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
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_SCHEDULED_JOBS_DISABLED, String.format("Context Instance Name[%s], Context Instance Identifier[%s]"
                            , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                        ContextInstanceSavedEventBroadcaster.broadcast(ContextMachineCache.instance()
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
        this.contextInstanceEndButton.setIconAfterText(true);
        this.contextInstanceEndButton.setVisible(this.contextInstance.isRunContextUntilManuallyEnded() && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_ADMIN));
        this.contextInstanceEndButton.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.end-job-plan-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.end-job-plan-body", UI.getCurrent().getLocale()));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    try {
                        this.contextInstanceRegistrationService.deregisterManually(this.contextInstance.getId());

                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_MANUALLY_ENDED, String.format("Context Instance Name[%s], Context Instance Identifier[%s]"
                            , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                        NotificationHelper.showUserNotification(getTranslation("notification.job-plan-ended-successfully", UI.getCurrent().getLocale()));
                        ContextInstanceSavedEventBroadcaster.broadcast(contextInstance);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification(getTranslation("notification.job-plan-ended-error", UI.getCurrent().getLocale()));
                    }
                });
        });

        this.ignoreContextInstanceEndButton = new Button(getTranslation("label.ignore-job-plan-duration"
            , UI.getCurrent().getLocale()), VaadinIcon.CONTROLLER.create());
        this.ignoreContextInstanceEndButton.setIconAfterText(true);
        this.ignoreContextInstanceEndButton.setVisible(!this.contextInstance.isRunContextUntilManuallyEnded() && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_ADMIN));
        this.ignoreContextInstanceEndButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.ignore-job-plan-duration-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.ignore-job-plan-duration-body", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    this.contextInstance.setRunContextUntilManuallyEnded(true);
                    this.saveContextInstance(this.contextInstance, this.contextInstance.getStatus());
                    ignoreContextInstanceEndButton.setVisible(false);
                    contextInstanceEndButton.setVisible(true);
                    this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_DURATION_IGNORED, String.format("Context Instance Name[%s], Context Instance Identifier[%s]"
                        , contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                    NotificationHelper.showErrorNotification(getTranslation("notification.job-plan-duration-ignored", UI.getCurrent().getLocale()));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("notification.job-plan-duration-ignored-error", UI.getCurrent().getLocale()));
                }

            });
        });

        this.resetContextButton = new Button(getTranslation("button.reset-context", UI.getCurrent().getLocale()), VaadinIcon.TIME_BACKWARD.create());
        this.resetContextButton.setIconAfterText(true);
        this.resetContextButton.setVisible(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        this.resetContextButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.reset-context", UI.getCurrent().getLocale()));

            Checkbox hold = new Checkbox("Hold All Command Execution Jobs");
            VerticalLayout verticalLayout = new VerticalLayout();
            verticalLayout.setWidthFull();
            Div body = new Div();
            body.setText(getTranslation("confirm-dialog-text.reset-context", UI.getCurrent().getLocale()));
            verticalLayout.add(body, hold);
            confirmDialog.setText(verticalLayout);

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ContextMachine machine = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId());
                if (machine != null) {
                    try {
                        machine.setDryRunParameters(null);
                        machine.getContext().setEndTime(System.currentTimeMillis());
                        this.saveContextInstance(machine.getContext(), InstanceStatus.ENDED);
                        machine.getContext().getAllNestedJobLocks().forEach(jobLockInstance -> {
                            JobLockCacheImpl.instance().resetLock(jobLockInstance.getName());
                        });
                        this.statusDiv.setStatus(InstanceStatus.ENDED);
                        ContextMachineCache.instance().remove(machine);
                        machine.resetContextInstance(hold.getValue());
                        ContextMachineCache.instance().put(machine);
                        String route = RouteConfiguration.forSessionScope()
                            .getUrl(ContextInstanceView.class, ContextMachineCache.instance()
                                .getFirstByContextName(this.contextInstance.getName()).getContext().getId() + "_scheduledContextInstance");

                        getUI().ifPresent(ui -> ui.getPage().open(route));

                        resetContextButton.setVisible(false);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification(getTranslation("error.reset-context", UI.getCurrent().getLocale()));
                    }
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(resetContextButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(jobLockDashboard, holdContextButton, releaseContextButton, enableQuartzScheduledJobsButton,
            disableQuartzScheduledJobsButton, contextInstanceEndButton, ignoreContextInstanceEndButton, resetContextButton);
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);

        VerticalLayout wrapper = new VerticalLayout(buttonLayout);
        wrapper.setWidthFull();
        wrapper.setMargin(false);
        wrapper.setPadding(false);
        wrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        return wrapper;
    }

    /**
     * Method to initialise the scheduler job instance grid.
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
     */
    private void initialiseSchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService) {
        this.schedulerJobInstanceGridWidget = new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextInstance, this.schedulerJobInstanceService,
            this.jobInitiationService, this.configurationRestService, metaDataRestService, this.jobUtilsService, this.scheduledContextService, this.jobStatus, this.jobName, this.contextProfileService, this.globalEventService);
        this.schedulerJobInstanceGridWidget.setWidthFull();
        this.schedulerJobInstanceGridWidget.setHeight("100%");
        this.schedulerJobInstanceGridWidget.setVisible(false);

    }

    /**
     * Initialise the job plan instance tree.
     */
    private void initialiseTree() {
        this.contextInstanceTreeViewWidget = new ContextInstanceTreeViewWidget(this.contextInstance, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
            this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService
            , this.contextProfileService, this.globalEventService);
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
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     */
    private void initialiseContextTemplateStatisticsWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                  LogStreamingService logStreamingService) {
        this.contextTemplateStatisticsWidget = new ContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
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
        UI ui = attachEvent.getUI();

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if (contextInstanceStateChangeEvent.getContextInstance() != null) {
                if(ui.isAttached()) {
                    ui.access(() -> {
                        if (this.contextInstance.getId().equals(contextInstanceStateChangeEvent.getContextInstance().getId())) {
                            this.contextInstance = contextInstanceStateChangeEvent.getContextInstance();
                            ContextHelper.enrichJobs(this.contextInstance);
                            this.statusDiv.setStatus(contextInstanceStateChangeEvent.getNewStatus());
                        } else {
                            ScheduledContextInstanceRecord record = this.scheduledContextInstanceService.findById(this.contextInstance.getId());
                            if (record != null) {
                                this.contextInstance = record.getContextInstance();
                                ContextHelper.enrichJobs(this.contextInstance);
                            }
                        }
                        this.updateJson(this.contextInstance);

                        if(this.contextInstance.getStatus().equals(InstanceStatus.ENDED)) {
                            this.contextInstanceEndButton.setVisible(false);
                            this.resetContextButton.setVisible(false);
                            this.ignoreContextInstanceEndButton.setVisible(false);
                            this.disableQuartzScheduledJobsButton.setVisible(false);
                            this.holdContextButton.setVisible(false);
                            this.releaseContextButton.setVisible(false);
                            this.enableQuartzScheduledJobsButton.setVisible(false);
                        }
                    });
                }
            }
        });

        schedulerJobInstanceStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if(ui.isAttached()) {
                ui.access(() -> {
                    ScheduledContextInstanceRecord record = this.scheduledContextInstanceService.findById(this.contextInstance.getId());
                    if (record != null) {
                        this.contextInstance = record.getContextInstance();
                        ContextHelper.enrichJobs(this.contextInstance);
                        this.updateJson(this.contextInstance);
                    }
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.contextInstanceStateChangeRegistration != null) {
            this.contextInstanceStateChangeRegistration.remove();
            this.contextInstanceStateChangeRegistration = null;
        }

        if(this.schedulerJobInstanceStateChangeRegistration != null) {
            this.schedulerJobInstanceStateChangeRegistration.remove();
            this.schedulerJobInstanceStateChangeRegistration = null;
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.init(".", moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.setWidthFull();
    }
}
