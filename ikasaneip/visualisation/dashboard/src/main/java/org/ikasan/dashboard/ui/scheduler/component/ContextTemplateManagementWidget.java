package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.listener.JobSynchronisationRequiredListener;
import org.ikasan.dashboard.ui.scheduler.model.BlackoutWindowDateTimePair;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcastListener;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcaster;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.ContextSchedulerVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.ContextDurationUtils;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.orchestration.service.context.util.ContextExportZipUtils;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
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
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ContextTemplateManagementWidget extends VerticalLayout implements JobSynchronisationRequiredListener, ContextTemplateSavedEventBroadcastListener {

    Logger logger = LoggerFactory.getLogger(ContextTemplateManagementWidget.class);

    private ScheduledContextService scheduledContextService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ContextProfileService contextProfileService;
    private JobProvisionService jobProvisionService;
    private UserService userService;
    private SecurityService securityService;
    private JobUtilsService jobUtilsService;
    private SchedulerJobService schedulerJobService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private EmailNotificationDetailsService emailNotificationDetailsService;
    private EmailNotificationContextService emailNotificationContextService;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private FormLayout formLayout;
    private IkasanAuthentication authentication;
    private JobPlanEditorWidget jobPlanEditorWidget;
    private SchedulerVisualisation schedulerVisualisation;
    private ContextInstanceGridWidget contextInstanceGridWidget;
    private SchedulerJobGridWidget schedulerJobGridWidget;
    private ContextTemplateStatisticsWidget contextTemplateStatisticsWidget;
    private JobInitiationService jobInitiationService;
    private ModuleMetaDataService moduleMetaDataService;
    private LogStreamingService logStreamingService;
    private GlobalEventService globalEventService;
    private SpringCloudConfigRefreshService springCloudConfigRefreshService;
    private ContextInstanceSchedulerService contextInstanceSchedulerService;
    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private IntegerField contextTtlMinutes;
    private IntegerField contextTtlHours;
    private IntegerField contextTtlDays;
    private IntegerField treeViewExpandLevel;
    private VerticalLayout schedulerVisualisationDiv;
    private Tab visualisationTab;
    private Tab rawContextTab;
    private Tab contextInstancesTab;
    private Tab jobTemplatesTab;
    private Tab statisticsTab;
    private Tabs tabs;
    private ComboBox<DateTimeUtil.TimezonePair> timezoneCb;
    private ContextTemplate contextTemplate;
    private Binder<ContextTemplate> binder;
    private List<BlackoutWindowDateTimePair> blackoutWindowDateTimePairs;
    private Grid<BlackoutWindowDateTimePair> blackoutWindowsGrid;
    private String zipWorkingDirectory;
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;
    private Button synchroniseJobsButton;
    private ComboBox<String> searchCb;
    private Checkbox isAbleToRunConcurrentlyCb;
    private UI ui;

    private boolean removeTrailingPlanNameContextAfterUnderscore;
    private int jobPlanIntervalMultiple;

    /**
     * Constructor
     *
     * @param scheduledContextService
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
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param contextProfileService
     * @param jobProvisionService
     * @param userService
     * @param securityService
     * @param jobUtilsService
     * @param zipWorkingDirectory
     * @param emailNotificationDetailsService
     * @param emailNotificationContextService
     * @param schedulerJobExecutionEnvironmentLabel
     * @param globalEventService
     * @param contextInstanceRegistrationService
     * @param springCloudConfigRefreshService
     * @param removeTrailingPlanNameContextAfterUnderscore
     */
    public ContextTemplateManagementWidget(ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath,
                                           ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService,
                                           JobInitiationService jobInitiationService, ContextProfileService contextProfileService, JobProvisionService jobProvisionService,
                                           UserService userService, SecurityService securityService, JobUtilsService jobUtilsService, String zipWorkingDirectory,
                                           EmailNotificationDetailsService emailNotificationDetailsService, EmailNotificationContextService emailNotificationContextService,
                                           Map<String, String> schedulerJobExecutionEnvironmentLabel, GlobalEventService globalEventService,
                                           ContextInstanceRegistrationService contextInstanceRegistrationService, ContextInstanceSchedulerService contextInstanceSchedulerService,
                                           SpringCloudConfigRefreshService springCloudConfigRefreshService, boolean removeTrailingPlanNameContextAfterUnderscore, int jobPlanIntervalMultiple) {

        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if (this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextTemplate = contextTemplate;
        if (this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if (this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if (this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if (this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.jobProvisionService = jobProvisionService;
        if (this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }
        this.userService = userService;
        if (this.userService == null) {
            throw new IllegalArgumentException("userService cannot be null!");
        }
        this.securityService = securityService;
        if (this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if (this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if (this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if (this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }
        this.configurationRestService = configurationRestService;
        if (this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.moduleControlRestService = moduleControlRestService;
        if (this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.metaDataRestService = metaDataRestService;
        if (this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if (this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.zipWorkingDirectory = zipWorkingDirectory;
        if (this.zipWorkingDirectory == null) {
            throw new IllegalArgumentException("zipWorkingDirectory cannot be null!");
        }
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        if (this.emailNotificationDetailsService == null) {
            throw new IllegalArgumentException("emailNotificationDetailsService cannot be null!");
        }
        this.emailNotificationContextService = emailNotificationContextService;
        if (this.emailNotificationContextService == null) {
            throw new IllegalArgumentException("emailNotificationContextService cannot be null!");
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

        this.springCloudConfigRefreshService = springCloudConfigRefreshService;
        if (this.springCloudConfigRefreshService == null) {
            throw new IllegalArgumentException("springCloudConfigRefreshService cannot be null!");
        }

        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;
        this.removeTrailingPlanNameContextAfterUnderscore = removeTrailingPlanNameContextAfterUnderscore;
        this.jobPlanIntervalMultiple = jobPlanIntervalMultiple;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.setMargin(false);
        this.setSpacing(false);
        this.setPadding(false);

        this.init(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, jobInitiationService);

        this.setSizeFull();
    }

    /**
     * General widget initialisation.
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
     * @param jobInitiationService
     */
    private void init(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                      ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                      MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                      LogStreamingService logStreamingService, JobInitiationService jobInitiationService) {
        this.blackoutWindowDateTimePairs = new ArrayList<>();
        this.binder = new Binder<>(ContextTemplate.class);

        this.contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        this.contextNameTf.setEnabled(false);
        binder.forField(contextNameTf)
            .bind(ContextTemplate::getName, ContextTemplate::setName);
        this.descriptionTa = new TextArea(getTranslation("label.context-description", UI.getCurrent().getLocale()));
        this.descriptionTa.setEnabled(false);
        binder.forField(descriptionTa)
            .bind(ContextTemplate::getDescription, ContextTemplate::setDescription);

        this.startWindowCronExpressionTf = new TextField(getTranslation("label.time-window-start", UI.getCurrent().getLocale()));
        this.startWindowCronExpressionTf.setEnabled(false);
        binder.forField(startWindowCronExpressionTf)
            .bind(ContextTemplate::getTimeWindowStart, ContextTemplate::setTimeWindowStart);

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

        this.timezoneCb = new ComboBox<>(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        ComboBox.ItemFilter<DateTimeUtil.TimezonePair> filter = (element, filterString) ->
            element.zoneId.toLowerCase().contains(filterString.toLowerCase());
        this.timezoneCb.setId("timezoneCb");
        this.timezoneCb.getElement().getThemeList().add("always-float-label");
        this.timezoneCb.setItems(filter, DateTimeUtil.getAllZoneIdsAndItsOffSet());
        this.timezoneCb.setItemLabelGenerator((ItemLabelGenerator<DateTimeUtil.TimezonePair>) s -> String.format("%35s (UTC%s) %n", s.zoneId, s.offset).trim());
        this.timezoneCb.setClearButtonVisible(true);
        this.timezoneCb.setPlaceholder(getTranslation("label.choose-a-timezone", UI.getCurrent().getLocale()));
        this.timezoneCb.setErrorMessage(getTranslation("error.timezone-required", UI.getCurrent().getLocale()));
        this.timezoneCb.setEnabled(false);

        if(contextTemplate.getTimezone() != null) {
            this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(contextTemplate.getTimezone()));
        }
        else {
            this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(ZoneId.systemDefault().getId()));
        }

        this.treeViewExpandLevel = new IntegerField(getTranslation("label.tree-view-expand-level", UI.getCurrent().getLocale()));
        this.treeViewExpandLevel.getElement().getThemeList().add("always-float-label");
        binder.forField(this.treeViewExpandLevel)
            .bind(ContextTemplate::getTreeViewExpandLevel, ContextTemplate::setTreeViewExpandLevel);
        this.treeViewExpandLevel.setEnabled(false);
        this.isAbleToRunConcurrentlyCb = new Checkbox(getTranslation("label.concurrent", UI.getCurrent().getLocale()));
        this.isAbleToRunConcurrentlyCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.isAbleToRunConcurrentlyCb)
            .bind(ContextTemplate::isAbleToRunConcurrently, ContextTemplate::setAbleToRunConcurrently);
        this.isAbleToRunConcurrentlyCb.setEnabled(false);

        this.initialiseBlackoutWindowGrid();
        this.populateBlackoutWindowPairs(contextTemplate);

        binder.readBean(this.contextTemplate);

        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.getElement().getStyle().set("padding-top", "0px");
        statusLayout.getElement().getStyle().set("padding-bottom", "10px");
        ContextTemplateStatusDiv statusDiv = new ContextTemplateStatusDiv();
        statusDiv.setHeight("45px");
        statusDiv.setWidth("100%");
        statusDiv.setStatus(!this.contextTemplate.isDisabled());

        statusLayout.add(statusDiv);
        statusLayout.setWidth("100%");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidth("100%");
        headerLayout.setMargin(false);
        headerLayout.setPadding(false);
        H4 contextTemplateManagementLabel
            = new H4(String.format(getTranslation("label.context-template-management", UI.getCurrent().getLocale()))
                + " - " + this.contextTemplate.getName());
        contextTemplateManagementLabel.getElement().getStyle().set("margin-top", "10px");
        HorizontalLayout labelLayout = new HorizontalLayout();
        labelLayout.setWidth("100%");
        labelLayout.setMargin(false);
        labelLayout.setPadding(false);
        labelLayout.add(contextTemplateManagementLabel);
        headerLayout.add(labelLayout, createButtonLayout());

        this.formLayout = new FormLayout();
        this.formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 40)
        );
        this.formLayout.add(this.contextNameTf, this.startWindowCronExpressionTf, this.contextTtlDays, this.contextTtlHours
            , this.contextTtlMinutes, this.timezoneCb, this.treeViewExpandLevel, this.isAbleToRunConcurrentlyCb, this.descriptionTa, this.blackoutWindowsGrid);
        this.formLayout.setColspan(this.contextNameTf, 12);
        this.formLayout.setColspan(this.startWindowCronExpressionTf, 6);
        this.formLayout.setColspan(this.contextTtlDays, 3);
        this.formLayout.setColspan(this.contextTtlHours, 3);
        this.formLayout.setColspan(this.contextTtlMinutes, 3);
        this.formLayout.setColspan(this.timezoneCb, 5);
        this.formLayout.setColspan(this.treeViewExpandLevel, 4);
        this.formLayout.setColspan(this.isAbleToRunConcurrentlyCb, 3);
        this.formLayout.setColspan(this.descriptionTa, 12);
        this.formLayout.setColspan(blackoutWindowsGrid, 25);

        CollapsableLayout collapsableLayout = new CollapsableLayout();
        add(collapsableLayout);

        //A border to show the outline of the layout itself
        collapsableLayout.getElement().getStyle().set("border", "1px solid #aaa");

        collapsableLayout.addContentComponent(formLayout);

        //Add a header button that toggles the visibility on click
        Button collapseButton = new Button(getTranslation("button.show", UI.getCurrent().getLocale()), e -> collapsableLayout.toggleContentVisibility());
        collapsableLayout.addHeaderComponentAsLastAndAlignToRight(collapseButton);

        //Change the button caption based on the collapse state change
        collapsableLayout.addCollapseChangeListener(e -> {
            collapseButton.setText(e.isCurrentlyVisible() ? getTranslation("button.hide", UI.getCurrent().getLocale()) : getTranslation("button.show", UI.getCurrent().getLocale()));
            collapsableLayout.getElement().getStyle().set("border", !e.isCurrentlyVisible() ? "1px solid #aaa" : "");
        });

        this.initialiseEditor();
        this.initialiseVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.initialiseContextInstanceGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
             configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService, jobInitiationService);
        this.initialiseSchedulerJobGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseTabs();
        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(this.tabs);
        this.getElement().getStyle().set("padding-top", "0px");
        this.add(statusLayout, headerLayout, collapsableLayout, tabLayout, this.jobPlanEditorWidget, this.schedulerVisualisationDiv
            , this.contextInstanceGridWidget, this.schedulerJobGridWidget, this.contextTemplateStatisticsWidget);
        this.expand(this.jobPlanEditorWidget);
    }

    /**
     * Initialise all the tabs.
     */
    private void initialiseTabs() {
        this.visualisationTab = new Tab(getTranslation("tab.visualisation", UI.getCurrent().getLocale()));
        this.rawContextTab = new Tab(getTranslation("tab.json-raw-format", UI.getCurrent().getLocale()));
        this.contextInstancesTab = new Tab(getTranslation("tab.context-instances", UI.getCurrent().getLocale()));
        this.jobTemplatesTab = new Tab(getTranslation("tab.job-templates", UI.getCurrent().getLocale()));
        this.statisticsTab = new Tab(getTranslation("tab.statistics", UI.getCurrent().getLocale()));

        this.tabs = new Tabs();
        this.tabs.add(this.visualisationTab, this.rawContextTab
            , this.contextInstancesTab, this.jobTemplatesTab/**, todo will introduce statisticsTab in future iteration this.statisticsTab*/);

        tabs.addSelectedChangeListener(event -> {
            if(tabs.getSelectedTab().equals(this.contextInstancesTab)) {
                this.jobPlanEditorWidget.setVisible(false);
                this.schedulerVisualisationDiv.setVisible(false);
                this.contextInstanceGridWidget.setVisible(true);
                this.schedulerJobGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.statisticsTab)) {
                this.jobPlanEditorWidget.setVisible(false);
                this.schedulerVisualisationDiv.setVisible(false);
                this.contextInstanceGridWidget.setVisible(false);
                this.schedulerJobGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(true);
            }
            else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                this.jobPlanEditorWidget.setVisible(true);
                this.schedulerVisualisationDiv.setVisible(false);
                this.contextInstanceGridWidget.setVisible(false);
                this.schedulerJobGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                this.jobPlanEditorWidget.setVisible(false);
                this.schedulerVisualisationDiv.setVisible(true);
                this.contextInstanceGridWidget.setVisible(false);
                this.schedulerJobGridWidget.setVisible(false);
                this.contextTemplateStatisticsWidget.setVisible(false);
            }
            else if(tabs.getSelectedTab().equals(this.jobTemplatesTab)) {
                this.jobPlanEditorWidget.setVisible(false);
                this.schedulerVisualisationDiv.setVisible(false);
                this.contextInstanceGridWidget.setVisible(false);
                this.schedulerJobGridWidget.setVisible(true);
                this.contextTemplateStatisticsWidget.setVisible(false);
            }
        });
    }

    /**
     * Initialise the JSON editor.
     */
    protected void initialiseEditor()
    {
        this.jobPlanEditorWidget = new JobPlanEditorWidget(this.contextTemplate
            , this.scheduledContextService, this.schedulerJobService, this.systemEventLogger);

        this.jobPlanEditorWidget.setVisible(false);
    }

    /**
     * Initialise the visualisation.
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
    protected void initialiseVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService) {

        this.schedulerVisualisationDiv = new VerticalLayout();
        this.schedulerVisualisationDiv.setSizeFull();
        this.schedulerVisualisationDiv.setMargin(false);
        this.schedulerVisualisationDiv.setPadding(false);

        this.schedulerVisualisation = new ContextSchedulerVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService
            , this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService
            , this.schedulerJobExecutionEnvironmentLabel);
        this.schedulerVisualisation.addJobSynchronisationRequiredListener(this);
        this.schedulerVisualisation.setWidthFull();

        try {
            ContextProfileSearchFilter searchFilter = new SolrContextProfileSearchFilterImpl();
            searchFilter.setContextName(this.contextTemplate.getName());
            searchFilter.setOwner(ContextProfileRecord.SYSTEM_OWNER);

            SearchResults<ContextProfileRecord> results = this.contextProfileService.findByFilter(searchFilter, -1, -1, null, null);

            if(results.getResultList().size() > 0 && results.getResultList().get(0).getContextProfile().getDefaultContext() != null
                && !results.getResultList().get(0).getContextProfile().getDefaultContext().isEmpty()){
                ContextTemplate childContextTemplate = ContextHelper.getChildContextTemplate(results.getResultList()
                    .get(0).getContextProfile().getDefaultContext(), this.contextTemplate);

                this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate, childContextTemplate, null, true);
            }
            else {
                this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate, this.contextTemplate, null, true);
            }

            HorizontalLayout buttonWrapper = new HorizontalLayout();
            buttonWrapper.setMargin(false);
            buttonWrapper.setPadding(false);
            buttonWrapper.setWidthFull();

            HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setMargin(false);
            buttonLayout.setPadding(false);
            buttonLayout.getStyle().set("position", "absolute");
            buttonLayout.getStyle().set("right", "10px");

            this.searchCb = new ComboBox<>();
            this.searchCb.setPlaceholder(getTranslation("label.search-job-plan", UI.getCurrent().getLocale()));
            this.initialiseSearchCb();

            buttonWrapper.add(searchCb);

            Button addContextButton = new Button(getTranslation("button.add-context", UI.getCurrent().getLocale()), VaadinIcon.PLUS.create());
            addContextButton.setIconAfterText(true);
            addContextButton.addClickListener(event -> {
                AddChildContextDialog addChildContextDialog = new AddChildContextDialog();
                addChildContextDialog.open();
                addChildContextDialog.addNewContextListener(this.schedulerVisualisation);
            });

            ComponentSecurityVisibility.applySecurity(addContextButton, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            buttonLayout.add(addContextButton, this.contextViewMenuBar());

            buttonWrapper.add(buttonLayout);

            this.schedulerVisualisation.getElement().getStyle().set("margin-top", "0px");
            this.schedulerVisualisationDiv.add(buttonWrapper, this.schedulerVisualisation);
            this.schedulerVisualisationDiv.expand(this.schedulerVisualisation);
        }
        catch (IOException e) {
            // todo raise message
            e.printStackTrace();
        }
    }

    private void initialiseSearchCb() {
        Map<String, Context> contextMap = ContextHelper.getAllContexts(this.contextTemplate);
        searchCb.setItems(contextMap.keySet());
        searchCb.setWidth("500px");
        searchCb.addValueChangeListener(event -> {
            if(searchCb.getValue() != null) {
                ContextTemplate child = (ContextTemplate) ContextHelper.getChildContext(searchCb.getValue(), this.contextTemplate);

                if (child != null) {
                    try {
                        this.schedulerVisualisation.addBoundaryToItem(child.getName(), true);
                    } catch (Exception e) {
                        NotificationHelper.showUserNotification(getTranslation("notification.could-not-open-child-job-plan", UI.getCurrent().getLocale()));
                    }
                } else {
                    NotificationHelper.showUserNotification(getTranslation("notification.could-not-find-child-job-plan", UI.getCurrent().getLocale()));
                }
            }
        });
    }

    /**
     * Initialise the context instance grid widget.
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
     * @param jobInitiationService
     */
    private void initialiseContextInstanceGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService, JobInitiationService jobInitiationService) {
        this.contextInstanceGridWidget = new ContextInstanceGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextTemplate, this.schedulerJobInstanceService,
            jobInitiationService, this.contextProfileService, this.jobUtilsService, this.scheduledContextService, this.globalEventService, this.contextInstanceRegistrationService);
        this.contextInstanceGridWidget.setWidthFull();
        this.contextInstanceGridWidget.setHeight("75vh");
        this.contextInstanceGridWidget.setVisible(false);
        this.contextInstanceGridWidget.getElement().getStyle().set("margin-bottom", "30px");

    }

    /**
     * Initialise the scheduler job grid widget.
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
    private void initialiseSchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService) {
        this.schedulerJobGridWidget = new SchedulerJobGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextTemplate,
            this.jobInitiationService, this.jobProvisionService, this.contextProfileService, this.userService, this.securityService, this.scheduledContextService,
            this.schedulerJobExecutionEnvironmentLabel);
        this.schedulerJobGridWidget.setWidthFull();
        this.schedulerJobGridWidget.setHeight("75vh");
        this.schedulerJobGridWidget.setVisible(false);
        this.schedulerJobGridWidget.getElement().getStyle().set("margin-bottom", "30px");

    }

    /**
     * Initialise the context template statistics widget.
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
     * Initialise the context view menu bar.
     *
     * @return
     */
    private MenuBar contextViewMenuBar() {
        MenuBar contextViewsMenuBar = new ContextTemplateViewMenuBar(this.contextTemplate
            , this.contextProfileService, this.schedulerVisualisation);

        ComponentSecurityVisibility.applySecurity(contextViewsMenuBar, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        return contextViewsMenuBar;
    }

    /**
     * Initialise the action buttons layout.
     *
     * @return
     */
    private Component createButtonLayout() {
        VerticalLayout buttonWrapper = new VerticalLayout();
        buttonWrapper.setMargin(false);
        buttonWrapper.setPadding(false);
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);
        this.synchroniseJobsButton = this.createSynchroniseJobsButton();

        MenuBar actionsMenuBar = new MenuBar();
        actionsMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem actionsMenuItem = this.createIconItem(actionsMenuBar, VaadinIcon.MENU, "Actions");
        SubMenu actions = actionsMenuItem.getSubMenu();

        actions.addItem(getTranslation("button.edit-context-template", UI.getCurrent().getLocale()),
            menuItemClickEvent -> {
                ContextTemplateDialog contextTemplateDialog = new ContextTemplateDialog(this.scheduledContextService, this.schedulerJobService
                    , this.contextInstanceRegistrationService, this.contextInstanceSchedulerService, this.systemEventLogger
                    , getTranslation("header.manage-context-template", UI.getCurrent().getLocale()), false, this.jobPlanIntervalMultiple);
                contextTemplateDialog.setContextTemplate(this.contextTemplate);
                contextTemplateDialog.open();
            })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        actions.addItem(getTranslation("button.manage-context-parameters", UI.getCurrent().getLocale()),
                menuItemClickEvent -> {
                    ContextParameterDialog contextTemplateDialog = new ContextParameterDialog(true);
                    contextTemplateDialog.initParams(this.contextTemplate.getContextParameters());
                    contextTemplateDialog.open();

                    contextTemplateDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened() && contextTemplateDialog.isSaveClose()) {
                            this.contextTemplate.setContextParameters(contextTemplateDialog.getContextParameters());

                            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService
                                .findByName(this.contextTemplate.getName());
                            scheduledContextRecord.setContext(this.contextTemplate);
                            this.scheduledContextService.save(scheduledContextRecord);
                        }
                    });
                })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        actions.addItem(getTranslation("label.encrypt-context-parameters-values", UI.getCurrent().getLocale()),
                menuItemClickEvent -> {
                    ContextParameterEncryptDialog contextParameterEncryptDialog = new ContextParameterEncryptDialog(springCloudConfigRefreshService);
                    contextParameterEncryptDialog.open();
                })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

        actions.addItem(getTranslation("button.manage-job-locks", UI.getCurrent().getLocale()),
            menuItemClickEvent -> {
                JobLockManagementDialog jobLockManagementDialog = new JobLockManagementDialog(this.contextTemplate, this.moduleMetaDataService, this.scheduledProcessManagementService,
                    this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService,
                    this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService,
                    this.schedulerJobExecutionEnvironmentLabel);
                jobLockManagementDialog.open();
            })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

        this.createJobUploadMenuBar(actions);
        this.createNewJobMenuBar(actions);
        Anchor download = new Anchor(new StreamResource(this.contextTemplate.getName() + ".zip", () -> this.getContextBundleStreamResource(false))
            , getTranslation("button.download-context-template", UI.getCurrent().getLocale()));
        download.getElement().setAttribute("download", true);
        actions.addItem(download);

        String downloadFileName = this.contextTemplate.getName();
        if(this.removeTrailingPlanNameContextAfterUnderscore && downloadFileName.contains("_")) {
            downloadFileName = downloadFileName.substring(0, downloadFileName.lastIndexOf("_"));
        }

        Anchor downloadWithTokens = new Anchor(new StreamResource(downloadFileName + ".zip", () -> this.getContextBundleStreamResource(true))
            , getTranslation("button.download-context-template-with-tokens", UI.getCurrent().getLocale()));
        downloadWithTokens.getElement().setAttribute("download", true);
        actions.addItem(downloadWithTokens);

        buttonLayout.add(actionsMenuBar, this.synchroniseJobsButton);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, this.synchroniseJobsButton);

        buttonWrapper.add(buttonLayout);
        buttonWrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        return buttonWrapper;
    }

    /**
     * Create the synchronise-jobs menu bar.
     *
     * @return
     */
    private Button createSynchroniseJobsButton() {
        Button synchroniseJobsButton = new Button(getTranslation("button.synchronise-jobs", UI.getCurrent().getLocale()), VaadinIcon.COGS.create());
        synchroniseJobsButton.setIconAfterText(true);

        ComponentSecurityVisibility.applySecurity(synchroniseJobsButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        synchroniseJobsButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setCancelable(true);
            confirmDialog.setHeader(getTranslation("confirm-dialog.provision-job-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.provision-job-body", UI.getCurrent().getLocale()));

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.setWidth("600px");
                dialog.setHeight("250px");
                dialog.open(getTranslation("progress-dialog.provision-job-header", UI.getCurrent().getLocale()),
                    getTranslation("progress-dialog.provision-job-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    boolean success = true;
                    try {
                        SearchResults<SchedulerJobRecord> jobRecords = this.schedulerJobService.findByContext(this.contextTemplate.getName(), -1, -1);

                        List<SchedulerJob> schedulerJobs = jobRecords.getResultList().stream()
                            .map(record -> record.getJob())
                            .collect(Collectors.toList());

                        this.jobProvisionService.provisionJobs(schedulerJobs, this.authentication.getName());
                        this.contextInstanceRegistrationService.register(this.contextTemplate.getName(), null);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        success = false;
                        dialog.close();
                        current.access(() -> NotificationHelper.showErrorNotification(getTranslation("error.provisioning-jobs", UI.getCurrent().getLocale())));
                    }

                    if(success) {
                        current.access(() -> {
                            dialog.close();
                            NotificationHelper.showUserNotification(getTranslation("notification.provisioned-jobs", UI.getCurrent().getLocale()));
                            this.synchroniseJobsButton.getStyle().set("background-color", "white");
                            this.synchroniseJobsButton.getStyle().set("color", IkasanColours.IKASAN_ORANGE);
                            this.synchroniseJobsButton.getElement().setAttribute("title", "");
                        });
                    }
                });

            });
        });

        return synchroniseJobsButton;
    }

    /**
     * Create the upload job menu bar.
     *
     * @return
     */
    private void createJobUploadMenuBar(SubMenu actions) {
        MenuItem jobUploadMenutItem = actions.addItem(getTranslation("menu-item.upload-job-template", UI.getCurrent().getLocale()));

        SubMenu activeContextInstancesSubMenu = jobUploadMenutItem.getSubMenu();
        MenuItem jobTypesMenuItem = activeContextInstancesSubMenu.addItem(getTranslation("menu-item.job-type", UI.getCurrent().getLocale()));
        SubMenu jobTypesSubMenu = jobTypesMenuItem.getSubMenu();

        jobTypesSubMenu.addItem(getTranslation("menu-item.command-execution-job", UI.getCurrent().getLocale())
            , event -> {SchedulerJobUploadDialog schedulerJobUploadDialog = new SchedulerJobUploadDialog(this.contextTemplate, this.schedulerJobService
                , InternalEventDrivenJob.class, getTranslation("label.command-job-upload", UI.getCurrent().getLocale())); schedulerJobUploadDialog.open();})
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        jobTypesSubMenu.addItem(getTranslation("menu-item.file-watcher-job", UI.getCurrent().getLocale())
            , event -> {SchedulerJobUploadDialog schedulerJobUploadDialog = new SchedulerJobUploadDialog(this.contextTemplate, this.schedulerJobService
                , FileEventDrivenJob.class, getTranslation("label.file-watcher-job-upload", UI.getCurrent().getLocale())); schedulerJobUploadDialog.open();})
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        jobTypesSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale())
            , event -> {SchedulerJobUploadDialog schedulerJobUploadDialog = new SchedulerJobUploadDialog(this.contextTemplate, this.schedulerJobService
                , QuartzScheduleDrivenJob.class, getTranslation("label.scheduled-job-upload", UI.getCurrent().getLocale())); schedulerJobUploadDialog.open();})
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
    }

    private void createNewJobMenuBar(SubMenu actions) {
        MenuItem newJobMenuItem = actions.addItem(getTranslation("menu-item.create-new-job", UI.getCurrent().getLocale()));

        SubMenu newJobSubMenu = newJobMenuItem.getSubMenu();
        MenuItem jobTypeMenuItem = newJobSubMenu.addItem(getTranslation("menu-item.job-type", UI.getCurrent().getLocale()));
        SubMenu jobTypesSubMenu = jobTypeMenuItem.getSubMenu();

        jobTypesSubMenu.addItem(getTranslation("menu-item.command-execution-job", UI.getCurrent().getLocale()), event -> {
                InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                    this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.contextTemplate, this.contextTemplate, this.schedulerJobExecutionEnvironmentLabel);

                InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
                internalEventDrivenJob.setContextName(this.contextTemplate.getName());

                internalEventDrivenJobDialog.setJob(internalEventDrivenJob, EditMode.NEW);
                internalEventDrivenJobDialog.addJobSynchronisationRequiredListener(this);
                internalEventDrivenJobDialog.open();

                internalEventDrivenJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobGridWidget.refresh();
                    }
                });
            })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        jobTypesSubMenu.addItem(getTranslation("menu-item.file-watcher-job", UI.getCurrent().getLocale()), event -> {
                FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                    this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

                FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
                fileEventDrivenJob.setContextName(contextTemplate.getName());

                fileEventJobDialog.setJob(fileEventDrivenJob, EditMode.NEW);
                fileEventJobDialog.addJobSynchronisationRequiredListener(this);

                fileEventJobDialog.open();

                fileEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobGridWidget.refresh();
                    }
                });
            })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        jobTypesSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale()), event -> {
                QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(null, this.scheduledProcessManagementService,
                    this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

                QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
                quartzScheduleDrivenJob.setContextName(this.contextTemplate.getName());

                quartzDrivenScheduledJobDialog.setJob(quartzScheduleDrivenJob, EditMode.NEW);
                quartzDrivenScheduledJobDialog.addJobSynchronisationRequiredListener(this);

                quartzDrivenScheduledJobDialog.open();

                quartzDrivenScheduledJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobGridWidget.refresh();
                    }
                });
            })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        jobTypesSubMenu.addItem(getTranslation("menu-item.global-job", UI.getCurrent().getLocale()), event -> {
                GlobalEventJobDialog globalEventJobDialog = new GlobalEventJobDialog(null, this.scheduledProcessManagementService,
                    this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

                GlobalEventJob globalEventJob = new GlobalEventJobImpl();
                globalEventJob.setContextName(JobConstants.GLOBAL_EVENT);

                globalEventJobDialog.setJob(globalEventJob, EditMode.NEW);
                globalEventJobDialog.open();

                globalEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobGridWidget.refresh();
                    }
                });
            })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
    }

    /**
     * Initialise the blackout window grid.
     *
     */
    private void initialiseBlackoutWindowGrid() {
        blackoutWindowsGrid = new Grid<>();
        blackoutWindowsGrid.removeAllColumns();
        blackoutWindowsGrid.setVisible(true);
        blackoutWindowsGrid.setWidthFull();
        blackoutWindowsGrid.setHeight("250px");

        blackoutWindowsGrid.addColumn(new ComponentRenderer<>(blackoutWindowDateTimePair -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(blackoutWindowDateTimePair.getBlackoutWindowStartTime());
                blackoutWindowDateTimePair.getBlackoutWindowStartTime().setEnabled(false);
                return horizontalLayout;
            }))
            .setHeader(getTranslation("label.blackout-window-start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTimeDate")
            .setFlexGrow(20);
        blackoutWindowsGrid.addColumn(new ComponentRenderer<>(blackoutWindowDateTimePair -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(blackoutWindowDateTimePair.getBlackoutWindowEndTime());
                blackoutWindowDateTimePair.getBlackoutWindowEndTime().setEnabled(false);
                return horizontalLayout;
            }))
            .setHeader(getTranslation("label.blackout-window-end-date-time", UI.getCurrent().getLocale()))
            .setKey("endTimeDate")
            .setFlexGrow(20);

        blackoutWindowsGrid.setItems(this.blackoutWindowDateTimePairs);
        blackoutWindowsGrid.addClassName("small-header");
    }

    private void populateBlackoutWindowPairs(ContextTemplate contextTemplate) {
        if(contextTemplate.getBlackoutWindowDateTimeRanges() != null) {
            contextTemplate.getBlackoutWindowDateTimeRanges().entrySet().forEach(entry -> {
                blackoutWindowDateTimePairs.add(new BlackoutWindowDateTimePair(entry.getKey()
                    , entry.getValue(), contextTemplate.getTimezone()));
            });
        }

        this.blackoutWindowsGrid.getDataProvider().refreshAll();
        this.blackoutWindowsGrid.setVisible(this.blackoutWindowDateTimePairs.size() > 0);
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

    private InputStream getContextBundleStreamResource(boolean withTokens) {
        try {
            ContextTemplate clone = SerializationUtils.clone(this.contextTemplate);

            if(this.removeTrailingPlanNameContextAfterUnderscore && clone.getName().contains("_") && withTokens) {
                clone.setName(clone.getName().substring(0, clone.getName().lastIndexOf("_")));
            }

            ByteArrayOutputStream byteArrayOutputStream = ContextExportZipUtils.createZipFile(
                clone,
                this.zipWorkingDirectory,
                this.schedulerJobService,
                this.emailNotificationDetailsService,
                this.emailNotificationContextService,
                this.contextProfileService,
                50, // limit to loop searching solr
                withTokens
            );
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.download-context", UI.getCurrent().getLocale()));
            return null;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        ContextTemplateSavedEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.ui = null;
        ContextTemplateSavedEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate) {
        if(this.contextTemplate.getName().equals(contextTemplate.getName())) {
            this.contextTemplate = contextTemplate;
            if (this.ui.isAttached()) {
                this.ui.access(() -> {
                    this.binder.readBean(contextTemplate);

                    this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(contextTemplate.getTimezone()));
                    this.blackoutWindowDateTimePairs.clear();
                    this.populateBlackoutWindowPairs(contextTemplate);
                    this.jobPlanEditorWidget.updateRawContextTemplate(contextTemplate);

                    try {
                        this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate, this.contextTemplate, null, true, ui);
                        this.initialiseSearchCb();
                    } catch (Exception e) {
                        logger.error("Could not recreate visualisation upon context update!", e);
                    }
                });
            }
        }
    }

    @Override
    public void jobSynchronisationRequired() {
        this.synchroniseJobsButton.getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
        this.synchroniseJobsButton.getStyle().set("color","white");
        this.synchroniseJobsButton.getElement().setAttribute("title"
            , getTranslation("tooltip.synch-jobs-required", UI.getCurrent().getLocale()));
    }
}
