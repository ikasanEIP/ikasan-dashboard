package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.model.BlackoutWindowDateTimePair;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcaster;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.ContextSchedulerVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
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
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ContextTemplateManagementWidget extends VerticalLayout {
    private Registration contextSaveBroadcasterRegistration;
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
    private FormLayout formLayout;
    private IkasanAuthentication authentication;
    private AceEditor aceEditor;
    private SchedulerVisualisation schedulerVisualisation;
    private ContextInstanceGridWidget contextInstanceGridWidget;
    private SchedulerJobGridWidget schedulerJobGridWidget;
    private ContextTemplateStatisticsWidget contextTemplateStatisticsWidget;
    private JobInitiationService jobInitiationService;
    private ModuleMetaDataService moduleMetaDataService;
    private LogStreamingService logStreamingService;
    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private TextField endWindowCronExpressionTf;
    private Div schedulerVisualisationDiv;
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
     */
    public ContextTemplateManagementWidget(ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath,
                                           ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService,
                                           JobInitiationService jobInitiationService, ContextProfileService contextProfileService, JobProvisionService jobProvisionService,
                                           UserService userService, SecurityService securityService, JobUtilsService jobUtilsService) {

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

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.setMargin(false);
        this.setSpacing(false);
        this.setPadding(false);

        this.init(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, jobInitiationService);

        this.setWidthFull();
    }

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

        this.endWindowCronExpressionTf = new TextField(getTranslation("label.time-window-end", UI.getCurrent().getLocale()));
        this.endWindowCronExpressionTf.setEnabled(false);
        binder.forField(endWindowCronExpressionTf)
            .bind(ContextTemplate::getTimeWindowEnd, ContextTemplate::setTimeWindowEnd);

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
        H4 contextTemplateManagementLabel = new H4(String.format(getTranslation("label.context-template-management", UI.getCurrent().getLocale())));
        contextTemplateManagementLabel.getElement().getStyle().set("margin-top", "10px");
        HorizontalLayout labelLayout = new HorizontalLayout();
        labelLayout.setWidth("100%");
        labelLayout.setMargin(false);
        labelLayout.setPadding(false);
        labelLayout.add(contextTemplateManagementLabel);
        headerLayout.add(labelLayout, createButtonLayout());

        this.formLayout = new FormLayout();
        this.formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 20)
        );
        this.formLayout.add(this.contextNameTf, this.startWindowCronExpressionTf, this.endWindowCronExpressionTf
            , this.timezoneCb, this.descriptionTa, this.blackoutWindowsGrid);
        this.formLayout.setColspan(this.contextNameTf, 8);
        this.formLayout.setColspan(this.startWindowCronExpressionTf, 4);
        this.formLayout.setColspan(this.endWindowCronExpressionTf, 4);
        this.formLayout.setColspan(this.timezoneCb, 4);
        this.formLayout.setColspan(this.descriptionTa, 8);
        this.formLayout.setColspan(this.blackoutWindowsGrid, 12);

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
        this.add(statusLayout, headerLayout, collapsableLayout, tabLayout, this.aceEditor, this.schedulerVisualisationDiv
            , this.contextInstanceGridWidget, this.schedulerJobGridWidget, this.contextTemplateStatisticsWidget);
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
            , this.contextInstancesTab, this.jobTemplatesTab, this.statisticsTab);

        tabs.addSelectedChangeListener(event -> {
            try {
                if(tabs.getSelectedTab().equals(this.contextInstancesTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(true);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.statisticsTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(true);
                }
                else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(true);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.jobTemplatesTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(true);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    /**
     * Initialise the JSON editor.
     */
    protected void initialiseEditor()
    {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.json);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setHeight("75vh");
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
        aceEditor.setVisible(false);
        aceEditor.getElement().getStyle().set("margin-bottom", "30px");

        ContextService contextService = new ContextService();

        try {
            aceEditor.setValue(contextService.getContextTemplateString(this.contextTemplate));
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
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

        this.schedulerVisualisationDiv = new Div();
        this.schedulerVisualisationDiv.setSizeFull();

        this.schedulerVisualisation = new ContextSchedulerVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService
            , this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService);
        this.schedulerVisualisation.setWidthFull();
        this.schedulerVisualisation.setHeight("75vh");

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

            VerticalLayout buttonWrapper = new VerticalLayout();
            buttonWrapper.setMargin(false);
            buttonWrapper.setPadding(false);
            buttonWrapper.setWidthFull();
            HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setMargin(false);
            buttonLayout.setPadding(false);

            Button addContextButton = new Button(getTranslation("button.add-context", UI.getCurrent().getLocale()), VaadinIcon.PLUS.create());
            addContextButton.setIconAfterText(true);
            addContextButton.addClickListener(event -> {
                AddChildContextDialog addChildContextDialog = new AddChildContextDialog();
                addChildContextDialog.open();
                addChildContextDialog.addNewContextListener(this.schedulerVisualisation);
            });

            buttonLayout.add(addContextButton, this.contextViewMenuBar());

            buttonWrapper.add(buttonLayout);
            buttonWrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

            this.schedulerVisualisation.getElement().getStyle().set("margin-top", "0px");
            this.schedulerVisualisation.getElement().getStyle().set("margin-bottom", "30px");
            this.schedulerVisualisationDiv.add(buttonWrapper, this.schedulerVisualisation);
        }
        catch (IOException e) {
            // todo raise message
            e.printStackTrace();
        }
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
            jobInitiationService, this.contextProfileService, this.jobUtilsService, this.scheduledContextService);
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
            this.jobInitiationService, this.jobProvisionService, this.contextProfileService, this.userService, this.securityService, this.scheduledContextService);
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
        MenuBar contextViewsMenuBar = new ContextTemplateViewMenuBar(this.contextTemplate, this.contextProfileService, this.schedulerVisualisation);

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
        Button provisionButton = this.createSynchroniseJobsButton();

        Button downloadContextTemplateButton = new Button(getTranslation("button.download-context-template", UI.getCurrent().getLocale()), VaadinIcon.DOWNLOAD_ALT.create());
        downloadContextTemplateButton.setIconAfterText(true);

        Button editButton =  new Button(getTranslation("button.edit-context-template", UI.getCurrent().getLocale()), VaadinIcon.EDIT.create());
        editButton.setIconAfterText(true);
        editButton.addClickListener(event -> {
            ContextTemplateDialog contextTemplateDialog = new ContextTemplateDialog(this.scheduledContextService, this.schedulerJobService
                , getTranslation("header.manage-context-template", UI.getCurrent().getLocale()), false);
            contextTemplateDialog.setContextTemplate(this.contextTemplate);
            contextTemplateDialog.open();
        });

        Button manageJobLocksButton =  new Button(getTranslation("button.manage-job-locks", UI.getCurrent().getLocale()), VaadinIcon.LOCK.create());
        manageJobLocksButton.setIconAfterText(true);
        manageJobLocksButton.addClickListener(event -> {
            JobLockManagementDialog jobLockManagementDialog = new JobLockManagementDialog(this.contextTemplate, this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService,
                this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService);
            jobLockManagementDialog.open();
        });

        buttonLayout.add(editButton, manageJobLocksButton, this.createJobUploadMenuBar(), this.createNewJobMenuBar(), downloadContextTemplateButton, provisionButton);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, provisionButton);

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
        synchroniseJobsButton.getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
        synchroniseJobsButton.getStyle().set("color","white");
        synchroniseJobsButton.getElement().setAttribute("title", getTranslation("tooltip.synch-jobs-required", UI.getCurrent().getLocale()));
        synchroniseJobsButton.setIconAfterText(true);

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
                    try {
                        SearchResults<SchedulerJobRecord> jobRecords = this.schedulerJobService.findByContext(this.contextTemplate.getName(), -1, -1);

                        List<SchedulerJob> schedulerJobs = jobRecords.getResultList().stream()
                            .map(record -> record.getJob())
                            .collect(Collectors.toList());

                        this.jobProvisionService.provisionJobs(schedulerJobs, this.authentication.getName());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        current.access(() -> NotificationHelper.showErrorNotification(getTranslation("error.provisioning-jobs", UI.getCurrent().getLocale())));
                    }
                    finally {
                        current.access(() -> {
                            dialog.close();
                            NotificationHelper.showUserNotification(getTranslation("notification.provisioned-jobs", UI.getCurrent().getLocale()));
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
    private MenuBar createJobUploadMenuBar() {
        MenuBar uploadJobMenuBar = new MenuBar();
        uploadJobMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem jobUploadMenutItem = createIconItem(uploadJobMenuBar, VaadinIcon.UPLOAD_ALT, getTranslation("menu-item.upload-job-template", UI.getCurrent().getLocale()));

        SubMenu activeContextInstancesSubMenu = jobUploadMenutItem.getSubMenu();
        MenuItem jobTypesMenuItem = activeContextInstancesSubMenu.addItem(getTranslation("menu-item.job-type", UI.getCurrent().getLocale()));
        SubMenu jobTypesSubMenu = jobTypesMenuItem.getSubMenu();

        jobTypesSubMenu.addItem(getTranslation("menu-item.command-execution-job", UI.getCurrent().getLocale())
            , event -> {SchedulerJobUploadDialog schedulerJobUploadDialog = new SchedulerJobUploadDialog(this.contextTemplate, this.schedulerJobService
                , InternalEventDrivenJob.class, getTranslation("label.command-job-upload", UI.getCurrent().getLocale())); schedulerJobUploadDialog.open();});
        jobTypesSubMenu.addItem(getTranslation("menu-item.file-watcher-job", UI.getCurrent().getLocale())
            , event -> {SchedulerJobUploadDialog schedulerJobUploadDialog = new SchedulerJobUploadDialog(this.contextTemplate, this.schedulerJobService
                , FileEventDrivenJob.class, getTranslation("label.file-watcher-job-upload", UI.getCurrent().getLocale())); schedulerJobUploadDialog.open();});
        jobTypesSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale())
            , event -> {SchedulerJobUploadDialog schedulerJobUploadDialog = new SchedulerJobUploadDialog(this.contextTemplate, this.schedulerJobService
                , QuartzScheduleDrivenJob.class, getTranslation("label.scheduled-job-upload", UI.getCurrent().getLocale())); schedulerJobUploadDialog.open();});

        return uploadJobMenuBar;
    }

    private MenuBar createNewJobMenuBar() {
        MenuBar newJobMenuBar = new MenuBar();
        newJobMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem newJobMenuItem = createIconItem(newJobMenuBar, VaadinIcon.PLUS, getTranslation("menu-item.create-new-job", UI.getCurrent().getLocale()));

        SubMenu newJobSubMenu = newJobMenuItem.getSubMenu();
        MenuItem jobTypeMenuItem = newJobSubMenu.addItem(getTranslation("menu-item.job-type", UI.getCurrent().getLocale()));
        SubMenu jobTypesSubMenu = jobTypeMenuItem.getSubMenu();

        jobTypesSubMenu.addItem(getTranslation("menu-item.command-execution-job", UI.getCurrent().getLocale()), event -> {
            InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

            InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
            internalEventDrivenJob.setContextName(this.contextTemplate.getName());

            internalEventDrivenJobDialog.setJob(internalEventDrivenJob, EditMode.NEW);
            internalEventDrivenJobDialog.open();

            internalEventDrivenJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened()) {
                    this.schedulerJobGridWidget.refresh();
                }
            });
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.file-watcher-job", UI.getCurrent().getLocale()), event -> {
            FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

            FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
            fileEventDrivenJob.setContextName(contextTemplate.getName());

            fileEventJobDialog.setJob(fileEventDrivenJob, EditMode.NEW);

            fileEventJobDialog.open();

            fileEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened()) {
                    this.schedulerJobGridWidget.refresh();
                }
            });
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale()), event -> {
            QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

            QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
            quartzScheduleDrivenJob.setContextName(this.contextTemplate.getName());

            quartzDrivenScheduledJobDialog.setJob(quartzScheduleDrivenJob, EditMode.NEW);

            quartzDrivenScheduledJobDialog.open();

            quartzDrivenScheduledJobDialog  .addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened()) {
                    this.schedulerJobGridWidget.refresh();
                }
            });
        });

        return newJobMenuBar;
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
                blackoutWindowDateTimePairs.add(new BlackoutWindowDateTimePair(entry.getKey(), entry.getValue()));
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        this.contextSaveBroadcasterRegistration = ContextTemplateSavedEventBroadcaster.register(contextTemplate -> {
            this.contextTemplate = contextTemplate;
            ui.access(() -> binder.readBean(contextTemplate));
            this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(contextTemplate.getTimezone()));
            this.blackoutWindowDateTimePairs.clear();
            this.populateBlackoutWindowPairs(contextTemplate);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.contextSaveBroadcasterRegistration != null) {
            this.contextSaveBroadcasterRegistration.remove();
            this.contextSaveBroadcasterRegistration = null;
        }
    }
}
