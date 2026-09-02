package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.scheduler.command.HoldAllCommandExecutionJobsForContextInstanceCommand;
import org.ikasan.dashboard.ui.scheduler.command.ReleaseAllCommandExecutionJobsForContextInstanceCommand;
import org.ikasan.dashboard.ui.scheduler.service.AggregateStatusCollector;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceMonitoringView;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ContextInstanceSearchFilterImpl;
import org.ikasan.job.orchestration.util.AggregateContextInstanceStatus;
import org.ikasan.job.orchestration.util.ContextHelper;
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
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.general.SchedulerService;
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

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao.SCHEDULED_CONTEXT_INSTANCE_TYPE;

/**
 * A class representing the ContextInstanceDashboardWidget.
 * This class provides methods for initializing and managing the dashboard widget for context instances.
 */
public class ContextInstanceDashboardWidget extends DashboardWidget
    implements SchedulerJobStateChangeEventLocalBroadcastListener, ContextInstanceStateChangeEventLocalBroadcastListener, ContextInstanceSavedEventLocalBroadcastListener {
    private Logger logger = LoggerFactory.getLogger(ContextInstanceDashboardWidget.class);
    private Grid<ContextInstanceAggregateJobStatus> contextInstanceAggregateJobStatusGrid;
    private Grid<PreparedFutureJobPlanInstance> preparedFutureContextInstanceGrid;
    private Grid<CompletedJobPlanInstance> completedContextInstanceGrid;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerService schedulerService;
    private SchedulerJobService schedulerJobService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private String dynamicImagePath;
    private ModuleMetaDataService moduleMetaDataService;
    private LogStreamingService logStreamingService;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private GlobalEventService globalEventService;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    private SystemEventSearchService systemEventSearchService;
    private TextField contextNameTf = new TextField();
    private TextField contextInstanceIdTf = new TextField();

    private TextField preparedContextNameTf = new TextField();
    private TextField preparedContextInstanceIdTf = new TextField();

    private TextField completeContextNameTf = new TextField();
    private TextField completeContextInstanceIdTf = new TextField();
    private StatusFilter statusFilter = new StatusFilter();
    private ContextInstanceSearchFilter contextInstanceSearchFilter = new ContextInstanceSearchFilterImpl();
    private ContextInstanceSearchFilter completeContextInstanceSearchFilter = new ContextInstanceSearchFilterImpl();
    private IkasanAuthentication ikasanAuthentication;
    private UI ui;

    private Tabs tabs;
    private Tab activeJobPlanInstancesTab;
    private Tab preparedFutureJobPlanInstancesTab;
    private Tab completedJobPlanInstancesTab;
    private VerticalLayout activeInstancesLayout;
    private VerticalLayout preparedFutureInstancesLayout;
    private VerticalLayout completedInstancesLayout;

    private DateFormatter dateFormatter = DateFormatter.instance();

    private Button waitingFilterButton;
    private Icon waitingCheck;
    private Button completeFilterButton;
    private Icon completeCheck;
    private Button runningFilterButton;
    private Icon runningCheck;
    private Button queuedFilterButton;
    private Icon queuedCheck ;
    private Button onHoldFilterButton;
    private Icon onHoldCheck;
    private Button skippedFilterButton;
    private Icon skippedCheck;
    private Button errorFilterButton ;
    private Icon errorCheck;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;


    /**
     * Creates an instance of the ContextInstanceDashboardWidget.
     *
     * @param configurationRestService                the configuration REST service (must not be null)
     * @param moduleControlRestService                the module control REST service (must not be null)
     * @param metaDataRestService                     the meta data REST service (must not be null)
     * @param systemEventLogger                       the system event logger (must not be null)
     * @param schedulerService                        the scheduler service (must not be null)
     * @param schedulerJobService                     the scheduler job service (must not be null)
     * @param schedulerJobInstanceService             the scheduler job instance service (must not be null)
     * @param scheduledContextInstanceService         the scheduled context instance service (must not be null)
     * @param dynamicImagePath                        the dynamic image path (must not be null)
     * @param moduleMetaDataService                   the module meta data service (must not be null)
     * @param logStreamingService                     the log streaming service (must not be null)
     * @param jobInitiationService                    the job initiation service (must not be null)
     * @param contextProfileService                   the context profile service (must not be null)
     * @param jobUtilsService                         the job utils service (must not be null)
     * @param scheduledContextService                 the scheduled context service (must not be null)
     * @param fullscreen                              flag to determine if widget should be displayed in fullscreen mode
     * @param globalEventService                      the global event service (must not be null)
     * @param contextInstanceRegistrationService      the context instance registration service (must not be null)
     * @param contextInstanceSchedulerService         the context instance scheduler service (must not be null)
     * @throws IllegalArgumentException               if any of the services are null
     */
    public ContextInstanceDashboardWidget(ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                          SystemEventLogger systemEventLogger, SchedulerService schedulerService, SchedulerJobService schedulerJobService,
                                          SchedulerJobInstanceService schedulerJobInstanceService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath,
                                          ModuleMetaDataService moduleMetaDataService, LogStreamingService logStreamingService,
                                          JobInitiationService jobInitiationService, ContextProfileService contextProfileService,
                                          JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService, boolean fullscreen, GlobalEventService globalEventService,
                                          ContextInstanceRegistrationService contextInstanceRegistrationService, ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService,
                                          SystemEventSearchService systemEventSearchService, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                          double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {

        this.configurationRestService = configurationRestService;
        if(this.configurationRestService ==  null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService ==  null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.metaDataRestService = metaDataRestService;
        if(this.metaDataRestService ==  null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger ==  null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.schedulerService = schedulerService;
        if(this.schedulerService ==  null) {
            throw new IllegalArgumentException("schedulerService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService ==  null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService ==  null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.dynamicImagePath = dynamicImagePath;
        if(this.dynamicImagePath ==  null) {
            throw new IllegalArgumentException("dynamicImagePath cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService ==  null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if(this.logStreamingService ==  null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if(this.jobInitiationService ==  null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if(this.contextProfileService ==  null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if(this.jobUtilsService ==  null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService ==  null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.globalEventService = globalEventService;
        if (this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
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

        this.ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.createContextInstanceAggregateJobStatusGrid();
        this.createPreparedFutureContextInstanceGrid();
        this.createCompleteContextInstanceGrid();
        this.createActiveInstancesTab(fullscreen);
        this.createPreparedFutureInstancesTab(fullscreen);
        this.createCompleteInstancesTab(fullscreen);
        this.initialiseTabs();
        Div layout = new Div();
        layout.setWidth("100%");
        layout.add(this.tabs, this.activeInstancesLayout, this.preparedFutureInstancesLayout, this.completedInstancesLayout);
        this.setContent(layout);

        if(fullscreen) {
            layout.setHeight(90, Unit.VH);
        }
        else {
            layout.setHeight(45, Unit.VH);
        }
    }

    /**
     * Initializes the tabs for active, prepared future, and completed job plan instances.
     */
    private void initialiseTabs() {
        this.activeJobPlanInstancesTab = new Tab(getTranslation("header.active-context-instances", UI.getCurrent().getLocale()));
        this.activeJobPlanInstancesTab.setId("activeJobPlanInstancesTab");
        this.preparedFutureJobPlanInstancesTab = new Tab(getTranslation("header.future-prepared-context-instances", UI.getCurrent().getLocale()));
        this.preparedFutureJobPlanInstancesTab.setId("preparedFutureJobPlanInstancesTab");
        this.completedJobPlanInstancesTab = new Tab(getTranslation("header.completed-context-instances", UI.getCurrent().getLocale()));
        this.completedJobPlanInstancesTab.setId("completedJobPlanInstances");

        this.tabs = new Tabs(this.activeJobPlanInstancesTab, this.preparedFutureJobPlanInstancesTab, this.completedJobPlanInstancesTab);
        this.tabs.addThemeVariants(TabsVariant.AURA_FILLED);
        this.tabs.setWidthFull();
        this.tabs.setId("contextInstancesTab");

        Map<Tab, Component> tabsToPages = new HashMap<>();
        tabsToPages.put(this.activeJobPlanInstancesTab, this.activeInstancesLayout);
        tabsToPages.put(this.preparedFutureJobPlanInstancesTab, this.preparedFutureInstancesLayout);
        tabsToPages.put(this.completedJobPlanInstancesTab, this.completedInstancesLayout);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });
    }

    /**
     * Creates the active instances tab with the specified fullscreen mode.
     *
     * @param fullscreen true to display the tab in fullscreen mode, false otherwise
     */
    private void createActiveInstancesTab(boolean fullscreen) {
        this.activeInstancesLayout = new VerticalLayout();
        this.activeInstancesLayout.setWidthFull();
        this.activeInstancesLayout.setHeight("95%");

        Button breakOut = new Button();
        breakOut.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        breakOut.getElement().getStyle().set("cursor", "pointer");
        breakOut.getElement().setAttribute("title", getTranslation("tooltip.breakout", UI.getCurrent().getLocale()));
        breakOut.setVisible(!fullscreen);
        breakOut.addClickListener(event -> {
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceMonitoringView.class);

            getUI().ifPresent(ui -> ui.getPage().open(route));
        });

        Button refresh = new Button(getTranslation("button.refresh"), VaadinIcon.REFRESH.create());
        refresh.setId("refresh-button");
        refresh.getElement().getStyle().set("cursor", "pointer");
        refresh.setIconAfterText(true);
        refresh.addClickListener(event -> {
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
            this.updateAggregateJobStatusFilteringCounts();
        });

        Button clearFiltersButton = new Button(getTranslation("button.clear-filters"), VaadinIcon.FILTER.create());
        clearFiltersButton.setId("clear-filters-button");
        clearFiltersButton.setIconAfterText(true);
        clearFiltersButton.getElement().getStyle().set("cursor", "pointer");
        clearFiltersButton.addClickListener(event -> {
            this.contextNameTf.setValue("");
            this.contextInstanceIdTf.setValue("");
            this.statusFilter.clearFilter();
            this.waitingCheck.setVisible(false);
            this.completeCheck.setVisible(false);
            this.errorCheck.setVisible(false);
            this.onHoldCheck.setVisible(false);
            this.queuedCheck.setVisible(false);
            this.runningCheck.setVisible(false);
            this.skippedCheck.setVisible(false);
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        Button helpIcon = new Button();
        helpIcon.getElement().appendChild(VaadinIcon.QUESTION.create().getElement());
        helpIcon.getElement().getStyle().set("cursor", "pointer");
        helpIcon.getElement().setAttribute("title", getTranslation("tooltip.help", UI.getCurrent().getLocale()));
        helpIcon.addClickListener(event ->{
            SchedulerDashboardHelpDialog dialog = new SchedulerDashboardHelpDialog();
            dialog.open();
        });

        HorizontalLayout rightSideButtons = new HorizontalLayout();
        rightSideButtons.add(clearFiltersButton, refresh, helpIcon, breakOut);

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(rightSideButtons);
        layout.setWidth("100%");
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, rightSideButtons);

        rightSideButtons.getElement().getStyle().set("margin-left", "auto");

        activeInstancesLayout.add(layout);
        activeInstancesLayout.add(this.contextInstanceAggregateJobStatusGrid);
    }

    /**
     * Creates the prepared future instances tab.
     *
     * @param fullscreen true if the tab should be displayed in fullscreen mode, false otherwise
     */
    private void createPreparedFutureInstancesTab(boolean fullscreen) {
        this.preparedFutureInstancesLayout = new VerticalLayout();
        this.preparedFutureInstancesLayout.setWidthFull();
        this.preparedFutureInstancesLayout.setHeight("95%");

        Button breakOut = new Button();
        breakOut.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        breakOut.setVisible(!fullscreen);
        breakOut.addClickListener(event -> {
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceMonitoringView.class);

            getUI().ifPresent(ui -> ui.getPage().open(route));
        });

        Button refresh = new Button(getTranslation("button.refresh"), VaadinIcon.REFRESH.create());
        refresh.setIconAfterText(true);
        refresh.addClickListener(event -> {
            this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
        });

        Button clearFiltersButton = new Button(getTranslation("button.clear-filters"), VaadinIcon.FILTER.create());
        clearFiltersButton.setIconAfterText(true);
        clearFiltersButton.addClickListener(event -> {
            this.preparedContextNameTf.setValue("");
            this.preparedContextInstanceIdTf.setValue("");
            this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout rightSideButtons = new HorizontalLayout();
        rightSideButtons.add(clearFiltersButton, refresh, breakOut);

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(rightSideButtons);
        layout.setWidth("100%");
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, rightSideButtons);

        rightSideButtons.getElement().getStyle().set("margin-left", "auto");

        preparedFutureInstancesLayout.add(layout);
        preparedFutureInstancesLayout.add(this.preparedFutureContextInstanceGrid);
        preparedFutureInstancesLayout.setVisible(false);
    }

    /**
     * Creates the complete instances tab.
     *
     * @param fullscreen whether the tab should be displayed in fullscreen mode
     */
    private void createCompleteInstancesTab(boolean fullscreen) {
        this.completedInstancesLayout = new VerticalLayout();
        this.completedInstancesLayout.setWidthFull();
        this.completedInstancesLayout.setHeight("95%");

        Button breakOut = new Button();
        breakOut.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        breakOut.setVisible(!fullscreen);
        breakOut.addClickListener(event -> {
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceMonitoringView.class);

            getUI().ifPresent(ui -> ui.getPage().open(route));
        });

        Button refresh = new Button(getTranslation("button.refresh"), VaadinIcon.REFRESH.create());
        refresh.setIconAfterText(true);
        refresh.addClickListener(event -> {
            this.completedContextInstanceGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout rightSideButtons = new HorizontalLayout();
        rightSideButtons.add(refresh, breakOut);

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(rightSideButtons);
        layout.setWidth("100%");
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, rightSideButtons);

        rightSideButtons.getElement().getStyle().set("margin-left", "auto");

        completedInstancesLayout.add(layout);
        completedInstancesLayout.add(this.completedContextInstanceGrid);
        completedInstancesLayout.setVisible(false);
    }

    /**
     * Creates the context instance aggregate job status grid.
     * This method sets up the grid and adds the necessary columns for displaying
     * the aggregate job status of context instances.
     */
    private void createContextInstanceAggregateJobStatusGrid() {
        // Create a modulesGrid bound to the list
        contextInstanceAggregateJobStatusGrid = new Grid<>();
        contextInstanceAggregateJobStatusGrid.setId("contextInstanceAggregateJobStatusGrid");
        contextInstanceAggregateJobStatusGrid.removeAllColumns();
        contextInstanceAggregateJobStatusGrid.setVisible(true);
        contextInstanceAggregateJobStatusGrid.setWidthFull();

        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                NativeLabel jobPlanName = new NativeLabel(contextInstanceAggregateJobStatus.getContextInstanceName());
                jobPlanName.getStyle().set("font-size", "8pt");
                return new HorizontalLayout(jobPlanName);
            }))
            .setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(6)
            .setResizable(true);
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button contextBreakoutButton = new Button(contextInstanceAggregateJobStatus.getContextInstanceId());
                contextBreakoutButton.getStyle().set("font-size", "8pt");
                contextBreakoutButton.setHeight("30px");
                contextBreakoutButton.setId("contextBreakOut");
                contextBreakoutButton.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class,
                            List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                contextBreakoutButton.getElement().getStyle().set("cursor", "pointer");


                contextBreakoutButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(contextBreakoutButton);
                return horizontalLayout;
            }))
            .setHeader(getTranslation("table-header.context-instance-id", UI.getCurrent().getLocale())).setKey("id")
            .setFlexGrow(7)
            .setResizable(true);
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING)
                        + " " + getTranslation(InstanceStatus.WAITING.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_WAITING, IkasanColours.BLACK,
                    contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING), "100px");
                statusButton.setId("waitingStatusButton");
                statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING)>0);
                statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.WAITING));
                statusButton.getElement().getStyle().set("cursor", "pointer");

                Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_WAITING, IkasanColours.BLACK);
                breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING)>0);
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.WAITING.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                breakOut.getElement().getStyle().set("cursor", "pointer");

                statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-dialog", UI.getCurrent().getLocale()));
                breakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(statusButton, breakOut);
                return horizontalLayout;
            }))
            .setHeader(getTranslation("table-header.job-status-counts", UI.getCurrent().getLocale()))
            .setFlexGrow(3)
            .setKey("waitingStatusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE)
                        + " " + getTranslation(InstanceStatus.COMPLETE.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_COMPLETE, IkasanColours.WHITE,
                    contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE), "100px");
                statusButton.setId("completeStatusButton");
                statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE)>0);
                statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.COMPLETE));
                statusButton.getElement().getStyle().set("cursor", "pointer");

                Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_COMPLETE, IkasanColours.WHITE);
                breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE)>0);
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.COMPLETE.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                breakOut.getElement().getStyle().set("cursor", "pointer");

                statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-dialog", UI.getCurrent().getLocale()));
                breakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(statusButton, breakOut);
                return horizontalLayout;
            }))
            .setFlexGrow(3)
            .setKey("completeStatusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING)
                        + " " + getTranslation(InstanceStatus.RUNNING.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_RUNNING, IkasanColours.WHITE,
                    contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING), "100px");
                statusButton.setId("runningStatusButton");
                statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING)>0);
                statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.RUNNING));
                if(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING)>0) statusButton.getElement().getStyle().set("cursor", "pointer");

                Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_RUNNING, IkasanColours.WHITE);
                breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING)>0);
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.RUNNING.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                breakOut.getElement().getStyle().set("cursor", "pointer");

                statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-dialog", UI.getCurrent().getLocale()));
                breakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(statusButton, breakOut);

                return horizontalLayout;
            }))
            .setFlexGrow(3)
            .setKey("runningStatusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED)
                        + " " + getTranslation(InstanceStatus.LOCK_QUEUED.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_LOCK_QUEUED, IkasanColours.WHITE,
                    contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED), "100px");
                statusButton.setId("queuedStatusButton");
                statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED)>0);
                statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.LOCK_QUEUED));
                if(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED)>0) statusButton.getElement().getStyle().set("cursor", "pointer");

                Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_LOCK_QUEUED, IkasanColours.WHITE);
                breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED)>0);
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.LOCK_QUEUED.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                breakOut.getElement().getStyle().set("cursor", "pointer");

                statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-dialog", UI.getCurrent().getLocale()));
                breakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(statusButton, breakOut);

                return horizontalLayout;
            })) .setFlexGrow(3)
            .setKey("queuedStatusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD)
                        + " " + getTranslation(InstanceStatus.ON_HOLD.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_ON_HOLD, IkasanColours.WHITE,
                    contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD), "100px");
                statusButton.setId("onHoldStatusButton");
                statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD)>0);
                statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ON_HOLD));
                if(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD)>0) statusButton.getElement().getStyle().set("cursor", "pointer");

                Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_ON_HOLD, IkasanColours.WHITE);
                breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD)>0);
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ON_HOLD.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                breakOut.getElement().getStyle().set("cursor", "pointer");

                statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-dialog", UI.getCurrent().getLocale()));
                breakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(statusButton, breakOut);
                return horizontalLayout;
            }))
            .setFlexGrow(3)
            .setKey("onHoldStatusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED)
                        + " " + getTranslation(InstanceStatus.SKIPPED.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_SKIPPED, IkasanColours.WHITE,
                    contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED), "100px");
                statusButton.setId("skippedStatusButton");
                statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED)>0);
                statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.SKIPPED));
                if(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED)>0) statusButton.getElement().getStyle().set("cursor", "pointer");

                Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_SKIPPED, IkasanColours.WHITE);
                breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED)>0);
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.SKIPPED.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                breakOut.getElement().getStyle().set("cursor", "pointer");

                statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-dialog", UI.getCurrent().getLocale()));
                breakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(statusButton, breakOut);

                return horizontalLayout;
            })).setFlexGrow(3)
            .setKey("skippedStatusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR)
                        + " " + getTranslation(InstanceStatus.ERROR.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_ERROR, IkasanColours.WHITE
                    , contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR), "70px");
                statusButton.setId("errorStatusButton");
                statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR)>0);
                statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ERROR));
                if(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR)>0) statusButton.getElement().getStyle().set("cursor", "pointer");

                Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_ERROR, IkasanColours.WHITE);
                breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR)>0);
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ERROR.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                breakOut.getElement().getStyle().set("cursor", "pointer");

                Button acknowledgedStatusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR_ACKNOWLEDGED)
                        + " " + getTranslation(InstanceStatus.ERROR_ACKNOWLEDGED.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_ERROR_ACKNOWLEDGED, IkasanColours.WHITE
                    , contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR_ACKNOWLEDGED), "70px");
                acknowledgedStatusButton.setId("errorAckedStatusButton");
                acknowledgedStatusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR_ACKNOWLEDGED)>0);
                acknowledgedStatusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ERROR));
                if(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR_ACKNOWLEDGED)>0) acknowledgedStatusButton.getElement().getStyle().set("cursor", "pointer");

                Button errorAckBreakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_ERROR_ACKNOWLEDGED, IkasanColours.WHITE);
                errorAckBreakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR_ACKNOWLEDGED)>0);
                errorAckBreakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                            , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ERROR_ACKNOWLEDGED.name()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                errorAckBreakOut.getElement().getStyle().set("cursor", "pointer");

                statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-dialog", UI.getCurrent().getLocale()));
                breakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));
                errorAckBreakOut.getElement().setAttribute("title", getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                horizontalLayout.add(statusButton, breakOut, acknowledgedStatusButton, errorAckBreakOut);
                return horizontalLayout;
            })).setFlexGrow(7)
            .setKey("errorStatusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setId("repeatingJobSuccessesButtonLayout");

                if(contextInstanceAggregateJobStatus.containsRepeatableJobs()) {
                    Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.COMPLETE)
                            + " " + getTranslation(InstanceStatus.COMPLETE.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_COMPLETE, IkasanColours.WHITE
                        , contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.COMPLETE), "100px");
                    statusButton.setEnabled(contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.COMPLETE)>0);
                    statusButton.setId("repeatingJobSuccessesButton");
                    statusButton.addClickListener(event -> {
                        RepeatingSchedulerJobExecutionHistoryDialog dialog = new RepeatingSchedulerJobExecutionHistoryDialog(this.scheduledContextInstanceService,
                            ContextMachineCache.instance().getByContextInstanceId(contextInstanceAggregateJobStatus.getContextInstanceId()).getContext()
                            , this.moduleMetaDataService, this.logStreamingService);
                        dialog.setFilterStatus(InstanceStatus.COMPLETE);
                        dialog.open();
                    });
                    statusButton.getElement().getStyle().set("cursor", "pointer");
                    horizontalLayout.add(statusButton);

                    statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-execution-dialog", UI.getCurrent().getLocale()));
                }
                else {
                    Button statusButton = this.buildStatusCountButton(getTranslation("label.not-applicable", UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_RUNNING, IkasanColours.WHITE
                        , contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.COMPLETE), "100px");
                    statusButton.setEnabled(false);
                    statusButton.setId("repeatingJobSuccessesButton");
                    horizontalLayout.add(statusButton);
                }
                return horizontalLayout;
            })).setFlexGrow(2)
            .setKey("repeatingJobsSuccess").setHeader(getTranslation("header.repeating-jobs"));
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setId("repeatingJobErrorsButtonLayout");

                if(contextInstanceAggregateJobStatus.containsRepeatableJobs()) {
                    Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.ERROR)
                            + " " + getTranslation(InstanceStatus.ERROR.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_ERROR, IkasanColours.WHITE
                        , contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.ERROR), "100px");
                    statusButton.setId("repeatingJobErrorsButton");
                    statusButton.setEnabled(contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.ERROR)>0);

                    statusButton.addClickListener(event -> {
                        RepeatingSchedulerJobExecutionHistoryDialog dialog = new RepeatingSchedulerJobExecutionHistoryDialog(this.scheduledContextInstanceService,
                            ContextMachineCache.instance().getByContextInstanceId(contextInstanceAggregateJobStatus.getContextInstanceId()).getContext()
                            , this.moduleMetaDataService, this.logStreamingService);
                        dialog.setFilterStatus(InstanceStatus.ERROR);
                        dialog.open();
                    });
                    statusButton.getElement().getStyle().set("cursor", "pointer");

                    statusButton.getElement().setAttribute("title", getTranslation("tooltip.open-job-execution-dialog", UI.getCurrent().getLocale()));

                    horizontalLayout.add(statusButton);
                }
                else {
                    Button statusButton = this.buildStatusCountButton(getTranslation("label.not-applicable", UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_ERROR, IkasanColours.WHITE
                        , contextInstanceAggregateJobStatus.repeatingJobInstanceStatusCount(InstanceStatus.ERROR), "100px");
                    statusButton.setEnabled(false);
                    statusButton.setId("repeatingJobErrorsButton");
                    horizontalLayout.add(statusButton);
                }
                return horizontalLayout;
            })).setFlexGrow(2)
            .setKey("repeatingJobsError");

        this.contextNameTf = new TextField();
        this.contextInstanceIdTf = new TextField();
        HeaderRow hr = this.contextInstanceAggregateJobStatusGrid.appendHeaderRow();
        this.addActiveContextInstanceGridFiltering(hr, "name", this.contextNameTf, this.statusFilter::setContextName);
        this.addActiveContextInstanceGridFiltering(hr, "id", this.contextInstanceIdTf, this.statusFilter::setContextInstanceId);
        this.addAggregateActiveContextInstanceGridFiltering(hr);
        DataProvider<ContextInstanceAggregateJobStatus, StatusFilter> dataProvider =
            DataProvider.fromFilteringCallbacks(
                // First callback fetches items based on a query
                query -> {
                    // The index of the first item to load
                    int offset = query.getOffset();

                    // The number of items to load
                    int limit = query.getLimit();

                    return this.filterContextInstanceAggregateJobStatus(this.statusFilter, offset, limit).stream();
                },
                // Second callback fetches the total number of items currently in the Grid.
                // The grid can then use it to properly adjust the scrollbars.
                query -> this.filterContextInstanceAggregateJobStatus(this.statusFilter, -1, -1).size());

        dataProvider.withConfigurableFilter().setFilter(this.statusFilter);
        this.contextInstanceAggregateJobStatusGrid.setDataProvider(dataProvider);
        this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
    }


    /**
     * Creates and configures a Grid for displaying prepared future context instances.
     * The Grid includes columns for job plan name, context instance id, start date/time,
     * and actions (buttons for opening context instance view, holding all command execution
     * jobs, and releasing all command execution jobs).
     * The Grid is also configured with filtering and sorting capabilities.
     */
    private void createPreparedFutureContextInstanceGrid() {
        // Create a modulesGrid bound to the list
        this.preparedFutureContextInstanceGrid = new Grid<>();
        this.preparedFutureContextInstanceGrid.setId("preparedFutureContextInstanceGrid");
        this.preparedFutureContextInstanceGrid.removeAllColumns();
        this.preparedFutureContextInstanceGrid.setVisible(true);
        this.preparedFutureContextInstanceGrid.setWidthFull();

        this.preparedFutureContextInstanceGrid.addColumn(PreparedFutureJobPlanInstance::getJobPlanName)
            .setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(2)
            .setResizable(true)
            .setSortable(true);
        this.preparedFutureContextInstanceGrid.addColumn(PreparedFutureJobPlanInstance::getContextInstanceId)
            .setHeader(getTranslation("table-header.context-instance-id", UI.getCurrent().getLocale())).setKey("id")
            .setFlexGrow(2)
            .setResizable(true)
            .setSortable(true);
        this.preparedFutureContextInstanceGrid.addColumn(LitRenderer.<PreparedFutureJobPlanInstance>of("<div style='white-space:normal'>${item.startTime}</div>")
                .withProperty("startTime", scheduledProcessEvent -> this.dateFormatter.getFormattedDate(scheduledProcessEvent.getContextInstanceStartTime())))
            .setHeader(getTranslation("table-header.context-instance-start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTime")
            .setFlexGrow(4)
            .setSortable(true);
        this.preparedFutureContextInstanceGrid.addColumn(new ComponentRenderer<>(preparedFutureJobPlanInstance -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setSpacing(false);
                layout.setMargin(false);
                layout.setPadding(false);
                layout.setSizeFull();

                ContextMachine machine = ContextMachineCache.instance().getByContextInstanceId(preparedFutureJobPlanInstance.getContextInstanceId());
                ContextInstance instance = machine != null ? machine.getContext() : null;

                if(instance != null) {
                    AggregateContextInstanceStatus aggregateContextInstanceStatus =
                        ContextHelper.getAggregateContextInstanceStatus(instance);

                    HorizontalLayout statusLayout = new HorizontalLayout();
                    statusLayout.setWidthFull();

                    if (aggregateContextInstanceStatus.isHeldJobs()) {
                        SchedulerStatusIconDiv onHoldStatusDiv = new SchedulerStatusIconDiv();
                        onHoldStatusDiv.setStatus(InstanceStatus.ON_HOLD, getTranslation("label.prepared-job-plan-contains-held-jobs", UI.getCurrent().getLocale()));
                        statusLayout.add(onHoldStatusDiv);
                    }

                    layout.add(statusLayout);
                    layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, statusLayout);
                }

                return layout;
            }))
            .setFlexGrow(1);
        this.preparedFutureContextInstanceGrid.addColumn(new ComponentRenderer<>(preparedFutureJobPlanInstance -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setMargin(false);
                layout.setSizeFull();

                Icon breakOut = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("tooltip.open-in-new-window", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(preparedFutureJobPlanInstance.getContextInstanceId() +"_scheduledContextInstance"));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });

                Icon holdButton = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-all-command-execution-jobs-in-instance", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                holdButton.addClickListener(event -> {
                    ContextInstance instance = ContextMachineCache.instance().getByContextInstanceId(preparedFutureJobPlanInstance.getContextInstanceId()).getContext();
                    HoldAllCommandExecutionJobsForContextInstanceCommand holdAllCommandExecutionJobsForContextInstanceCommand
                        = new HoldAllCommandExecutionJobsForContextInstanceCommand(instance, this.schedulerJobInstanceService,
                        this.systemEventLogger, this.ikasanAuthentication);
                    holdAllCommandExecutionJobsForContextInstanceCommand.execute();
                });

                Icon releaseButton = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-all-command-execution-jobs-in-instance", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                releaseButton.addClickListener(event -> {
                    ContextInstance instance = ContextMachineCache.instance().getByContextInstanceId(preparedFutureJobPlanInstance.getContextInstanceId()).getContext();
                    ReleaseAllCommandExecutionJobsForContextInstanceCommand releaseAllCommandExecutionJobsForContextInstanceCommand
                        = new ReleaseAllCommandExecutionJobsForContextInstanceCommand(instance, this.schedulerJobInstanceService,
                        this.systemEventLogger, this.ikasanAuthentication);
                    releaseAllCommandExecutionJobsForContextInstanceCommand.execute();
                });

                HorizontalLayout buttonLayout = new HorizontalLayout();
                buttonLayout.setMargin(false);
                buttonLayout.add(holdButton, releaseButton, breakOut);

                layout.add(buttonLayout);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

                return layout;
            }))
            .setFlexGrow(2);

        this.preparedContextNameTf = new TextField();
        this.preparedContextNameTf.setId("preparedContextNameTf");
        this.preparedContextInstanceIdTf = new TextField();
        this.preparedContextInstanceIdTf.setId("preparedContextInstanceIdTf");
        HeaderRow hr = this.preparedFutureContextInstanceGrid.appendHeaderRow();
        this.addPreparedContextInstanceGridFiltering(hr, "name", this.preparedContextNameTf, this.contextInstanceSearchFilter::setContextSearchFilter);
        this.addPreparedContextInstanceGridFiltering(hr, "id", this.preparedContextInstanceIdTf, this.contextInstanceSearchFilter::setContextInstanceId);
        this.addDateTimeGridFiltering(hr, contextInstanceSearchFilter::setStartTime, contextInstanceSearchFilter::setEndTime, null, null,
            "startTime", this.preparedFutureContextInstanceGrid);
        DataProvider<PreparedFutureJobPlanInstance, ContextInstanceSearchFilter> dataProvider =
            DataProvider.fromFilteringCallbacks(
                // First callback fetches items based on a query
                query -> {
                    // The index of the first item to load
                    int offset = query.getOffset();

                    // The number of items to load
                    int limit = query.getLimit();

                    if(query.getSortOrders().size() > 0) {
                        return this.filterPreparedContextInstances(this.contextInstanceSearchFilter, offset, limit
                            , query.getSortOrders().get(0).getSorted(), query.getSortOrders().get(0).getDirection().name()).stream();
                    }

                    return this.filterPreparedContextInstances(this.contextInstanceSearchFilter, offset, limit, null, null).stream();
                },
                // Second callback fetches the total number of items currently in the Grid.
                // The grid can then use it to properly adjust the scrollbars.
                query -> {
                    // The index of the first item to load
                    int offset = query.getOffset();

                    // The number of items to load
                    int limit = query.getLimit();

                    if(query.getSortOrders().size() > 0) {
                        return this.filterPreparedContextInstances(this.contextInstanceSearchFilter, offset, limit
                            , query.getSortOrders().get(0).getSorted(), query.getSortOrders().get(0).getDirection().name()).size();
                    }

                    return this.filterPreparedContextInstances(this.contextInstanceSearchFilter, offset, limit, null, null).size();
                });

        dataProvider.withConfigurableFilter().setFilter(this.contextInstanceSearchFilter);
        this.preparedFutureContextInstanceGrid.setDataProvider(dataProvider);
        this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
    }

    /**
     * Creates the complete context instance grid with necessary columns, filters, and data provider.
     */
    private void createCompleteContextInstanceGrid() {
        // Create a modulesGrid bound to the list
        this.completedContextInstanceGrid = new Grid<>();
        this.completedContextInstanceGrid.setId("completedContextInstanceGrid");
        this.completedContextInstanceGrid.removeAllColumns();
        this.completedContextInstanceGrid.setVisible(true);
        this.completedContextInstanceGrid.setWidthFull();


        this.completedContextInstanceGrid.addColumn(CompletedJobPlanInstance::getJobPlanName)
            .setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale())).setKey("moduleName")
            .setFlexGrow(2)
            .setResizable(true)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(CompletedJobPlanInstance::getContextInstanceId)
            .setHeader(getTranslation("table-header.context-instance-id", UI.getCurrent().getLocale())).setKey("id")
            .setFlexGrow(3)
            .setResizable(true)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(LitRenderer.<CompletedJobPlanInstance>of("<div style='white-space:normal'>${item.startTime}</div>")
                .withProperty("startTime", completedJobPlanInstance -> this.dateFormatter.getFormattedDate(completedJobPlanInstance.getContextInstanceStartTime())))
            .setHeader(getTranslation("table-header.start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTime")
            .setFlexGrow(3)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(LitRenderer.<CompletedJobPlanInstance>of("<div style='white-space:normal'>${item.endTime}</div>")
                .withProperty("endTime", completedJobPlanInstance -> this.dateFormatter.getFormattedDate(completedJobPlanInstance.getContextInstanceEndTime())))
            .setHeader(getTranslation("table-header.end-date-time", UI.getCurrent().getLocale()))
            .setKey("endTime")
            .setFlexGrow(3)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(new ComponentRenderer<>(completedJobPlanInstance -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setMargin(false);
                layout.setSizeFull();

                Icon breakOut = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("tooltip.open-in-new-window", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(completedJobPlanInstance.getContextInstanceId() +"_scheduledContextInstance"));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });

                HorizontalLayout buttonLayout = new HorizontalLayout();
                buttonLayout.setMargin(false);
                buttonLayout.add(breakOut);

                layout.add(buttonLayout);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

                return layout;
            }))
            .setFlexGrow(1);

        this.completeContextNameTf = new TextField();
        this.completeContextNameTf.setId("completeContextNameTf");
        this.completeContextInstanceIdTf = new TextField();
        this.completeContextInstanceIdTf.setId("completeContextInstanceIdTf");
        HeaderRow hr = this.completedContextInstanceGrid.appendHeaderRow();
        this.addCompletedContextInstanceGridFiltering(hr, "moduleName", this.completeContextNameTf, this.completeContextInstanceSearchFilter::setContextSearchFilter);
        this.addCompletedContextInstanceGridFiltering(hr, "id", this.completeContextInstanceIdTf, this.completeContextInstanceSearchFilter::setContextInstanceId);
        this.addDateTimeGridFiltering(hr, completeContextInstanceSearchFilter::setStartTime, completeContextInstanceSearchFilter::setEndTime,
            completeContextInstanceSearchFilter::setStartTimeStart, completeContextInstanceSearchFilter::setStartTimeEnd,"startTime", this.completedContextInstanceGrid);
        this.addDateTimeGridFiltering(hr, completeContextInstanceSearchFilter::setStartTime, completeContextInstanceSearchFilter::setEndTime,
            completeContextInstanceSearchFilter::setEndTimeStart, completeContextInstanceSearchFilter::setEndTimeEnd,"endTime", this.completedContextInstanceGrid);
        DataProvider<CompletedJobPlanInstance, ContextInstanceSearchFilter> dataProvider =
            DataProvider.fromFilteringCallbacks(
                // First callback fetches items based on a query
                query -> {
                    // The index of the first item to load
                    int offset = query.getOffset();

                    // The number of items to load
                    int limit = query.getLimit();

                    if(query.getSortOrders().size() > 0) {
                        return this.filterCompleteContextInstances(this.completeContextInstanceSearchFilter, offset, limit
                            , query.getSortOrders().get(0).getSorted(), query.getSortOrders().get(0).getDirection().name()).stream();
                    }

                    return this.filterCompleteContextInstances(this.completeContextInstanceSearchFilter, offset, limit, null, null).stream();
                },
                // Second callback fetches the total number of items currently in the Grid.
                // The grid can then use it to properly adjust the scrollbars.
                query -> (int)this.filterCompleteContextInstancesSize(this.completeContextInstanceSearchFilter));

        dataProvider.withConfigurableFilter().setFilter(this.completeContextInstanceSearchFilter);
        this.completedContextInstanceGrid.setDataProvider(dataProvider);
        this.completedContextInstanceGrid.getDataProvider().refreshAll();
    }

    /**
     * Builds a status count button with the specified label, background colour, font colour, and count.
     *
     * @param label            the label for the button
     * @param backgroundColour the background colour of the button
     * @param fontColour       the font colour of the button
     * @param count            the count displayed on the button
     * @return the built status count button
     */
    private Button buildStatusCountButton(String label, String backgroundColour, String fontColour, int count, String width) {
        Button statusButton = new Button(label);
        if(count > 0) {
            statusButton.getElement().getStyle().set("background-color", backgroundColour);
            statusButton.getElement().getStyle().set("color", fontColour);
        }

        statusButton.setWidth(width);
        statusButton.getElement().getStyle().set("font-size", "7pt");
        statusButton.setHeight("30px");
        return statusButton;
    }

    /**
     * Builds and returns a status breakout button with the specified background colour and font colour.
     *
     * @param backgroundColour The background colour of the button.
     * @param fontColour The font colour of the button.
     * @return The built status breakout button.
     */
    private Button buildStatusBreakoutButton(String backgroundColour, String fontColour) {
        Button breakOut = new Button();
        Icon breakOutIcon = VaadinIcon.EXTERNAL_LINK.create();
        breakOutIcon.setSize("8pt");
        breakOut.getElement().appendChild(breakOutIcon.getElement());
        breakOut.getElement().getStyle().set("background-color", backgroundColour);
        breakOut.getElement().getStyle().set("color", fontColour);
        breakOut.setWidth("30px");
        breakOut.setHeight("30px");

        return breakOut;
    }

    /**
     * Opens the context instance dialog for a given context instance aggregate job status, context instance widget tab,
     * and instance status.
     *
     * @param contextInstanceAggregateJobStatus The aggregate job status of the context instance.
     * @param contextInstanceWidgetTab         The widget tab of the context instance.
     * @param status                           The instance status.
     */
    private void openContextInstanceDialog(ContextInstanceAggregateJobStatus contextInstanceAggregateJobStatus, String contextInstanceWidgetTab,
                                           InstanceStatus status) {
        ContextInstance contextInstance = this.scheduledContextInstanceService
            .findById(contextInstanceAggregateJobStatus.getContextInstanceId()+ "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE).getContextInstance();
        ContextTemplate contextTemplate = this.scheduledContextService.findByName(contextInstanceAggregateJobStatus.getContextInstanceName()).getContext();
        ContextInstanceDialog contextInstanceDialog = new ContextInstanceDialog(this.scheduledContextInstanceService, this.dynamicImagePath, this.moduleMetaDataService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService, contextInstance, contextTemplate,
            this.schedulerJobInstanceService, this.jobInitiationService, this.contextProfileService, this.jobUtilsService, this.scheduledContextService, contextInstanceWidgetTab, status.name(), this.globalEventService,
            this.contextInstanceRegistrationService, this.contextInstanceSchedulerService, this.systemEventSearchService , this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance,
            this.contextVisualisationNodeDistance);

        contextInstanceDialog.open();
    }

    /**
     * Adds filtering functionality to a grid column.
     *
     * @param hr        The header row of the grid.
     * @param columnKey The key of the column to add filtering to.
     * @param textField The text field component used for filtering.
     * @param setFilter The consumer function for setting the filter value.
     */
    private void addActiveContextInstanceGridFiltering(HeaderRow hr, String columnKey, TextField textField, Consumer<String> setFilter) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev-> {
            setFilter.accept(textField.getValue());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Method to add aggregate active context instance grid filtering.
     *
     * @param hr - header row
     */
    private void addAggregateActiveContextInstanceGridFiltering(HeaderRow hr) {
        this.waitingFilterButton = this.buildStatusCountButton(1
                + " " + getTranslation(InstanceStatus.WAITING.getTranslationLabel(), UI.getCurrent().getLocale())
            , IkasanColours.SCHEDULER_WAITING, IkasanColours.BLACK, 1, "120px");
        this.waitingFilterButton.setId("waitingFilterButton");
        this.waitingFilterButton.getElement().getStyle().set("cursor", "pointer");
        this.waitingFilterButton.getElement().setAttribute("title"
            , getTranslation("tooltip.click-to-filter", UI.getCurrent().getLocale()));

        this.waitingCheck = VaadinIcon.CHECK.create();
        this.waitingCheck.setSize("8pt");
        this.waitingCheck.setVisible(false);

        this.waitingFilterButton.addClickListener(event -> {
            this.waitingCheck.setVisible(!this.waitingCheck.isVisible());
            this.statusFilter.setFilterWaiting(this.waitingCheck.isVisible());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout waitingLayout = new HorizontalLayout(this.waitingFilterButton, this.waitingCheck);
        waitingLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.waitingCheck);

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey("waitingStatusCounts")).setComponent(waitingLayout);

        this.completeFilterButton = this.buildStatusCountButton(5
                + " " + getTranslation(InstanceStatus.COMPLETE.getTranslationLabel(), UI.getCurrent().getLocale())
            , IkasanColours.SCHEDULER_COMPLETE, IkasanColours.WHITE, 5, "120px");
        this.completeFilterButton.setId("completeFilterButton");
        this.completeFilterButton.getElement().getStyle().set("cursor", "pointer");
        this.completeFilterButton.getElement().setAttribute("title"
            , getTranslation("tooltip.click-to-filter", UI.getCurrent().getLocale()));

        this.completeCheck = VaadinIcon.CHECK.create();
        this.completeCheck.setSize("8pt");
        this.completeCheck.setVisible(false);

        this.completeFilterButton.addClickListener(event -> {
            this.completeCheck.setVisible(!this.completeCheck.isVisible());
            this.statusFilter.setFilterComplete(this.completeCheck.isVisible());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout completeLayout = new HorizontalLayout(this.completeFilterButton, this.completeCheck);
        completeLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.completeCheck);

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey("completeStatusCounts")).setComponent(completeLayout);

        this.runningFilterButton = this.buildStatusCountButton(5
                + " " + getTranslation(InstanceStatus.RUNNING.getTranslationLabel(), UI.getCurrent().getLocale())
            , IkasanColours.SCHEDULER_RUNNING, IkasanColours.WHITE, 5, "120px");
        this.runningFilterButton.setId("runningFilterButton");
        this.runningFilterButton.getElement().getStyle().set("cursor", "pointer");
        this.runningFilterButton.getElement().setAttribute("title"
            , getTranslation("tooltip.click-to-filter", UI.getCurrent().getLocale()));

        this.runningCheck = VaadinIcon.CHECK.create();
        this.runningCheck.setSize("8pt");
        this.runningCheck.setVisible(false);

        this.runningFilterButton.addClickListener(event -> {
            this.runningCheck.setVisible(!this.runningCheck.isVisible());
            this.statusFilter.setFilterRunning(this.runningCheck.isVisible());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout runningLayout = new HorizontalLayout(this.runningFilterButton, this.runningCheck);
        runningLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.runningCheck);

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey("runningStatusCounts")).setComponent(runningLayout);

        this.queuedFilterButton = this.buildStatusCountButton(5
                + " " + getTranslation(InstanceStatus.LOCK_QUEUED.getTranslationLabel(), UI.getCurrent().getLocale())
            , IkasanColours.SCHEDULER_LOCK_QUEUED, IkasanColours.WHITE, 5, "120px");
        this.queuedFilterButton.setId("queuedFilterButton");
        this.queuedFilterButton.getElement().getStyle().set("cursor", "pointer");
        this.queuedFilterButton.getElement().setAttribute("title"
            , getTranslation("tooltip.click-to-filter", UI.getCurrent().getLocale()));

        this.queuedCheck = VaadinIcon.CHECK.create();
        this.queuedCheck.setSize("8pt");
        this.queuedCheck.setVisible(false);

        this.queuedFilterButton.addClickListener(event -> {
            this.queuedCheck.setVisible(!this.queuedCheck.isVisible());
            this.statusFilter.setFilterQueued(this.queuedCheck.isVisible());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout queuedLayout = new HorizontalLayout(this.queuedFilterButton, this.queuedCheck);
        queuedLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.queuedCheck);

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey("queuedStatusCounts")).setComponent(queuedLayout);

        this.onHoldFilterButton = this.buildStatusCountButton(5
                + " " + getTranslation(InstanceStatus.ON_HOLD.getTranslationLabel(), UI.getCurrent().getLocale())
            , IkasanColours.SCHEDULER_ON_HOLD, IkasanColours.WHITE, 5, "120px");
        this.onHoldFilterButton.setId("onHoldFilterButton");
        this.onHoldFilterButton.getElement().getStyle().set("cursor", "pointer");
        this.onHoldFilterButton.getElement().setAttribute("title"
            , getTranslation("tooltip.click-to-filter", UI.getCurrent().getLocale()));

        this.onHoldCheck = VaadinIcon.CHECK.create();
        this.onHoldCheck.setSize("8pt");
        this.onHoldCheck.setVisible(false);

        this.onHoldFilterButton.addClickListener(event -> {
            this.onHoldCheck.setVisible(!this.onHoldCheck.isVisible());
            this.statusFilter.setFilterOnHold(this.onHoldCheck.isVisible());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout onHoldLayout = new HorizontalLayout(this.onHoldFilterButton, this.onHoldCheck);
        onHoldLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.onHoldCheck);

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey("onHoldStatusCounts")).setComponent(onHoldLayout);

        this.skippedFilterButton = this.buildStatusCountButton(5
                + " " + getTranslation(InstanceStatus.SKIPPED.getTranslationLabel(), UI.getCurrent().getLocale())
            , IkasanColours.SCHEDULER_SKIPPED, IkasanColours.WHITE, 5, "120px");
        this.skippedFilterButton.setId("skippedFilterButton");
        this.skippedFilterButton.getElement().getStyle().set("cursor", "pointer");
        this.skippedFilterButton.getElement().setAttribute("title"
            , getTranslation("tooltip.click-to-filter", UI.getCurrent().getLocale()));

        this.skippedCheck = VaadinIcon.CHECK.create();
        this.skippedCheck.setSize("8pt");
        this.skippedCheck.setVisible(false);

        this.skippedFilterButton.addClickListener(event -> {
            this.skippedCheck.setVisible(!this.skippedCheck.isVisible());
            this.statusFilter.setFilterSkipped(this.skippedCheck.isVisible());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout skippedLayout = new HorizontalLayout(this.skippedFilterButton, this.skippedCheck);
        skippedLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.skippedCheck);

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey("skippedStatusCounts")).setComponent(skippedLayout);

        this.errorFilterButton = this.buildStatusCountButton(5
                + " " + getTranslation(InstanceStatus.ERROR.getTranslationLabel(), UI.getCurrent().getLocale())
            , IkasanColours.SCHEDULER_ERROR, IkasanColours.WHITE, 5, "120px");
        this.errorFilterButton.setId("errorFilterButton");
        this.errorFilterButton.getElement().getStyle().set("cursor", "pointer");
        this.errorFilterButton.getElement().setAttribute("title"
            , getTranslation("tooltip.click-to-filter", UI.getCurrent().getLocale()));

        this.errorCheck = VaadinIcon.CHECK.create();
        this.errorCheck.setSize("8pt");
        this.errorCheck.setVisible(false);

        this.errorFilterButton.addClickListener(event -> {
            this.errorCheck.setVisible(!this.errorCheck.isVisible());
            this.statusFilter.setFilterError(this.errorCheck.isVisible());
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout errorLayout = new HorizontalLayout(this.errorFilterButton, this.errorCheck);
        errorLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.errorCheck);

        hr.getCell(this.contextInstanceAggregateJobStatusGrid.getColumnByKey("errorStatusCounts")).setComponent(errorLayout);

        this.updateAggregateJobStatusFilteringCounts();
    }



    /**
     * Update the aggregate job status filtering counts.
     * This method retrieves the contextInstanceAggregateJobStatus list by filtering it with the given
     * statusFilter, offset, and limit.
     * Then, it calculates the counts for each InstanceStatus from the retrieved list and updates
     * the corresponding UI buttons with the count and translation.
     */
    private void updateAggregateJobStatusFilteringCounts() {
        List<ContextInstanceAggregateJobStatus> aggregateContextInstanceStatuses
            = this.filterContextInstanceAggregateJobStatus(new StatusFilter(), -1, -1);

        AtomicInteger waiting = new AtomicInteger();
        AtomicInteger complete = new AtomicInteger();
        AtomicInteger running = new AtomicInteger();
        AtomicInteger onHold = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger queued = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        aggregateContextInstanceStatuses.forEach(status -> {
            waiting.addAndGet(status.getStatusCount(InstanceStatus.WAITING));
            complete.addAndGet(status.getStatusCount(InstanceStatus.COMPLETE));
            running.addAndGet(status.getStatusCount(InstanceStatus.RUNNING));
            onHold.addAndGet(status.getStatusCount(InstanceStatus.ON_HOLD));
            skipped.addAndGet(status.getStatusCount(InstanceStatus.SKIPPED));
            queued.addAndGet(status.getStatusCount(InstanceStatus.LOCK_QUEUED));
            error.addAndGet(status.getStatusCount(InstanceStatus.ERROR));
        });

        this.waitingFilterButton.setText(waiting.get() + " " + getTranslation(InstanceStatus.WAITING.getTranslationLabel()));
        this.completeFilterButton.setText(complete.get() + " " + getTranslation(InstanceStatus.COMPLETE.getTranslationLabel()));
        this.runningFilterButton.setText(running.get() + " " + getTranslation(InstanceStatus.RUNNING.getTranslationLabel()));
        this.onHoldFilterButton.setText(onHold.get() + " " + getTranslation(InstanceStatus.ON_HOLD.getTranslationLabel()));
        this.skippedFilterButton.setText(skipped.get() + " " + getTranslation(InstanceStatus.SKIPPED.getTranslationLabel()));
        this.queuedFilterButton.setText(queued.get() + " " + getTranslation(InstanceStatus.LOCK_QUEUED.getTranslationLabel()));
        this.errorFilterButton.setText(error.get() + " " + getTranslation(InstanceStatus.ERROR.getTranslationLabel()));
    }

    /**
     * Adds filtering functionality to a grid column.
     *
     * @param hr         The header row of the grid.
     * @param columnKey  The key of the column to add filtering to.
     * @param textField  The text field component used for filtering.
     * @param setFilter  The consumer function for setting the filter value.
     */
    private void addPreparedContextInstanceGridFiltering(HeaderRow hr, String columnKey, TextField textField, Consumer<String> setFilter) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev-> {
            setFilter.accept(textField.getValue());
            this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
        });

        hr.getCell(this.preparedFutureContextInstanceGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Adds filtering functionality to a grid column.
     *
     * @param hr         The header row of the grid.
     * @param columnKey  The key of the column to add filtering to.
     * @param textField  The text field component used for filtering.
     * @param setFilter  The consumer function for setting the filter value.
     */
    private void addCompletedContextInstanceGridFiltering(HeaderRow hr, String columnKey, TextField textField, Consumer<String> setFilter) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev-> {
            setFilter.accept(textField.getValue());
            this.completedContextInstanceGrid.getDataProvider().refreshAll();
        });

        hr.getCell(this.completedContextInstanceGrid.getColumnByKey(columnKey)).setComponent(textField);
    }


    /**
     * Adds date and time filtering functionality to a grid header row.
     *
     * @param hr            The header row of the grid.
     * @param setStartTime  A Consumer that accepts a Long representing the selected start time.
     * @param setEndTime    A Consumer that accepts a Long representing the selected end time.
     * @param setTimeStart  A Consumer that accepts a Long representing the selected start time for time filtering.
     * @param setTimeEnd    A Consumer that accepts a Long representing the selected end time for time filtering.
     * @param columnKey     The key of the column to filter.
     * @param grid          The grid to add the filtering functionality to.
     */
    public void addDateTimeGridFiltering(HeaderRow hr, Consumer<Long> setStartTime, Consumer<Long> setEndTime
        , Consumer<Long> setTimeStart, Consumer<Long> setTimeEnd, String columnKey, Grid grid) {
        DatePicker startDatePicker = new DatePicker(getTranslation("label.start-date", UI.getCurrent().getLocale()));
        startDatePicker.setLocale(Locale.UK);
        startDatePicker.getElement().getThemeList().add("always-float-label");

        TimePicker startTimePicker = new TimePicker(getTranslation("label.start-time", UI.getCurrent().getLocale()));
        startTimePicker.setStep(Duration.ofMinutes(15));
        startTimePicker.setLocale(Locale.UK);
        startTimePicker.getElement().getThemeList().add("always-float-label");

        DatePicker endDatePicker = new DatePicker(getTranslation("label.end-date", UI.getCurrent().getLocale()));
        endDatePicker.setLocale(Locale.UK);
        endDatePicker.getElement().getThemeList().add("always-float-label");

        TimePicker endTimePicker = new TimePicker(getTranslation("label.end-time", UI.getCurrent().getLocale()));
        endTimePicker.setStep(Duration.ofMinutes(15));
        endTimePicker.setLocale(Locale.UK);
        endTimePicker.getElement().getThemeList().add("always-float-label");

        NativeLabel timeLabel = new NativeLabel();
        Icon closeIcon = IconDecorator.decorate(VaadinIcon.CLOSE_SMALL.create()
            , getTranslation("tooltip.close", UI.getCurrent().getLocale()), "14px", "");
        closeIcon.setSize("14px");
        closeIcon.setVisible(false);

        Dialog dateTimeDialog = new Dialog();
        dateTimeDialog.setWidth("900px");

        startDatePicker.addValueChangeListener(event -> {
            if(endDatePicker.getValue() != null && startDatePicker.getValue() != null
                && startTimePicker.getValue() != null && endTimePicker.getValue() != null) {
                long startOfDayMilli = startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long endOfDayMilli = endDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long startMilli = startTimePicker.getValue().toSecondOfDay() * 1000;
                long endMilli = endTimePicker.getValue().toSecondOfDay() * 1000;

                setStartTime.accept(startOfDayMilli + startMilli);
                if(setTimeStart!=null)setTimeStart.accept(startOfDayMilli + startMilli);
                setEndTime.accept(endOfDayMilli + endMilli);
                if(setTimeEnd!=null)setTimeEnd.accept(endOfDayMilli + endMilli);
                dateTimeDialog.close();
            }
            else {
                setStartTime.accept(-1L);
                setEndTime.accept(-1L);
                if(setTimeStart!=null)setTimeStart.accept(-1L);
                if(setTimeEnd!=null)setTimeEnd.accept(-1L);
            }

            if(startDatePicker.getValue() != null && startTimePicker.getValue() != null
                && endDatePicker.getValue() != null && endTimePicker.getValue() != null) {
                timeLabel.setText(startDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                    + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                    + " " + endDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " " + endTimePicker.getValue());
                closeIcon.setVisible(true);
            }

            grid.getDataProvider().refreshAll();
        });

        startTimePicker.addValueChangeListener(event->{
            if(endDatePicker.getValue() != null && startDatePicker.getValue() != null
                && startTimePicker.getValue() != null && endTimePicker.getValue() != null) {
                long startOfDayMilli = startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long endOfDayMilli = endDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long startMilli = startTimePicker.getValue().toSecondOfDay() * 1000;
                long endMilli = endTimePicker.getValue().toSecondOfDay() * 1000;

                setStartTime.accept(startOfDayMilli + startMilli);
                if(setTimeStart!=null)setTimeStart.accept(startOfDayMilli + startMilli);
                setEndTime.accept(endOfDayMilli + endMilli);
                if(setTimeEnd!=null)setTimeEnd.accept(endOfDayMilli + endMilli);
                dateTimeDialog.close();
            }
            else {
                setStartTime.accept(-1L);
                setEndTime.accept(-1L);
                if(setTimeStart!=null)setTimeStart.accept(-1L);
                if(setTimeEnd!=null)setTimeEnd.accept(-1L);
            }

            if(startDatePicker.getValue() != null && startTimePicker.getValue() != null
                && endDatePicker.getValue() != null && endTimePicker.getValue() != null) {
                timeLabel.setText(startDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                    + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                    + " " + endDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " " + endTimePicker.getValue());
                closeIcon.setVisible(true);
            }

            grid.getDataProvider().refreshAll();
        });

        endDatePicker.addValueChangeListener(event -> {
            if(endDatePicker.getValue() != null && startDatePicker.getValue() != null
                && startTimePicker.getValue() != null && endTimePicker.getValue() != null) {
                long startOfDayMilli = startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long endOfDayMilli = endDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long startMilli = startTimePicker.getValue().toSecondOfDay() * 1000;
                long endMilli = endTimePicker.getValue().toSecondOfDay() * 1000;

                setStartTime.accept(startOfDayMilli + startMilli);
                if(setTimeStart!=null)setTimeStart.accept(startOfDayMilli + startMilli);
                setEndTime.accept(endOfDayMilli + endMilli);
                if(setTimeEnd!=null)setTimeEnd.accept(endOfDayMilli + endMilli);
                dateTimeDialog.close();
            }
            else {
                setStartTime.accept(-1L);
                setEndTime.accept(-1L);
                if(setTimeStart!=null)setTimeStart.accept(-1L);
                if(setTimeEnd!=null)setTimeEnd.accept(-1L);
            }

            if(startDatePicker.getValue() != null && startTimePicker.getValue() != null
                && endDatePicker.getValue() != null && endTimePicker.getValue() != null) {
                timeLabel.setText(startDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                    + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                    + " " + endDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " " + endTimePicker.getValue());
                closeIcon.setVisible(true);
            }

            grid.getDataProvider().refreshAll();
        });

        endTimePicker.addValueChangeListener(event->{
            if(endDatePicker.getValue() != null && startDatePicker.getValue() != null
                && startTimePicker.getValue() != null && endTimePicker.getValue() != null) {
                long startOfDayMilli = startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long endOfDayMilli = endDatePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long startMilli = startTimePicker.getValue().toSecondOfDay() * 1000;
                long endMilli = endTimePicker.getValue().toSecondOfDay() * 1000;

                setStartTime.accept(startOfDayMilli + startMilli);
                if(setTimeStart!=null)setTimeStart.accept(startOfDayMilli + startMilli);
                setEndTime.accept(endOfDayMilli + endMilli);
                if(setTimeEnd!=null)setTimeEnd.accept(endOfDayMilli + endMilli);
                dateTimeDialog.close();
            }
            else {
                setStartTime.accept(-1L);
                setEndTime.accept(-1L);
                if(setTimeStart!=null)setTimeStart.accept(-1L);
                if(setTimeEnd!=null)setTimeEnd.accept(-1L);
            }

            if(startDatePicker.getValue() != null && startTimePicker.getValue() != null
                && endDatePicker.getValue() != null && endTimePicker.getValue() != null) {
                timeLabel.setText(startDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                    + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                    + " " + endDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " " + endTimePicker.getValue());
                closeIcon.setVisible(true);
            }

            grid.getDataProvider().refreshAll();
        });

        HorizontalLayout layout = new HorizontalLayout(startDatePicker, startTimePicker, endDatePicker, endTimePicker);
        layout.setMargin(true);

        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidthFull();
        wrapper.add(layout);
        wrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, layout);

        dateTimeDialog.add(wrapper);

        Icon icon = IconDecorator.decorate(VaadinIcon.CALENDAR_CLOCK.create(), getTranslation("tooltip.add-time-filter", UI.getCurrent().getLocale()), "16pt", "");
        icon.addClickListener(event -> {
            dateTimeDialog.open();
        });

        dateTimeDialog.addOpenedChangeListener(event -> {
            if(!event.isOpened()) {
                if(startDatePicker.getValue() != null && startTimePicker.getValue() != null
                    && endTimePicker.getValue() != null) {
                    timeLabel.setText(startDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                        + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                        + " " + endDatePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " " + endTimePicker.getValue());
                    closeIcon.setVisible(true);
                }
            }
        });

        closeIcon.addClickListener(event -> {
            closeIcon.setVisible(false);
            startDatePicker.setValue(null);
            startTimePicker.setValue(null);
            endDatePicker.setValue(null);
            endTimePicker.setValue(null);
            timeLabel.setText("");
        });

        HorizontalLayout filterLayout = new HorizontalLayout(icon, timeLabel, closeIcon);
        filterLayout.setWidth("450px");
        filterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, closeIcon);

        hr.getCell(grid.getColumnByKey(columnKey)).setComponent(filterLayout);
    }

    /**
     * Filters the list of ContextInstanceAggregateJobStatus based on the provided status filter, offset, and limit.
     *
     * @param statusFilter the status filter to apply on the job statuses
     * @param offset the offset of the result list
     * @param limit the limit of the result list
     * @return the filtered list of ContextInstanceAggregateJobStatus
     */
    private List<ContextInstanceAggregateJobStatus> filterContextInstanceAggregateJobStatus(StatusFilter statusFilter, int offset, int limit) {
        List<ContextInstanceAggregateJobStatus> jobStatuses = AggregateStatusCollector.instance().getContextInstanceAggregateJobStatuses();

        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        jobStatuses = jobStatuses.stream().filter(item -> {
                boolean filter = true;

                if(!canAccessAllJobPlans) {
                    filter = accessibleJobPlans.contains(item.getContextInstanceName());
                }

                if(statusFilter.contextName != null && !statusFilter.contextName.isEmpty() && filter) {
                    filter = item.getContextInstanceName().toLowerCase().contains(statusFilter.contextName.toLowerCase());
                }

                if(statusFilter.contextInstanceId != null && !statusFilter.contextInstanceId.isEmpty() && filter) {
                    filter = item.getContextInstanceId().toLowerCase().contains(statusFilter.contextInstanceId.toLowerCase());
                }

                if(statusFilter.isFilterWaiting() && filter) {
                    filter = item.getStatusCount(InstanceStatus.WAITING) > 0;
                }

                if(statusFilter.isFilterComplete() && filter) {
                    filter = item.getStatusCount(InstanceStatus.COMPLETE) > 0;
                }

                if(statusFilter.isFilterRunning() && filter) {
                    filter = item.getStatusCount(InstanceStatus.RUNNING) > 0;
                }

                if(statusFilter.isFilterQueued() && filter) {
                    filter = item.getStatusCount(InstanceStatus.LOCK_QUEUED) > 0;
                }

                if(statusFilter.isFilterOnHold() && filter) {
                    filter = item.getStatusCount(InstanceStatus.ON_HOLD) > 0;
                }

                if(statusFilter.isFilterSkipped() && filter) {
                    filter = item.getStatusCount(InstanceStatus.SKIPPED) > 0;
                }

                if(statusFilter.isFilterError() && filter) {
                    filter = (item.getStatusCount(InstanceStatus.ERROR) > 0
                        || item.getStatusCount(InstanceStatus.ERROR_ACKNOWLEDGED) > 0);
                }

                return filter;
            })
            .collect(Collectors.toList());

        if(offset >= 0 && limit > 0 && offset + limit >= jobStatuses.size()) {
            jobStatuses = jobStatuses.subList(offset, jobStatuses.size());
        }
        else if(offset >= 0 && limit > 0 && offset + limit < jobStatuses.size()) {
            jobStatuses = jobStatuses.subList(offset, offset + limit);
        }

        return jobStatuses;
    }

    /**
     * Filters the list of PreparedFutureJobPlanInstances based on the given search filter, offset, limit, sort field, and sort order.
     *
     * @param contextInstanceSearchFilter The search filter to apply on the ContextInstanceRecords.
     * @param offset The starting index of the filtered list.
     * @param limit The maximum number of items to include in the filtered list.
     * @param sortField The field by which to sort the filtered list. Possible values are "name", "id", and "startTime".
     * @param sortOrder The sort order to apply on the filtered list. Possible values are "ASCENDING" and "DESCENDING".
     * @return The filtered and sorted list of PreparedFutureJobPlanInstances.
     */
    private List<PreparedFutureJobPlanInstance> filterPreparedContextInstances(ContextInstanceSearchFilter contextInstanceSearchFilter, int offset, int limit,
                                                                               String sortField, String sortOrder) {
        ContextInstanceSearchFilter preparedSearchFilter = new ContextInstanceSearchFilterImpl();
        preparedSearchFilter.setStatus(InstanceStatus.PREPARED.name());

        List<ScheduledContextInstanceRecord> contextInstanceRecords = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(preparedSearchFilter, -1, -1, null, null).getResultList();

        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        contextInstanceRecords = contextInstanceRecords.stream().filter(item -> {
                boolean filter = true;

                if(!canAccessAllJobPlans) {
                    filter = accessibleJobPlans.contains(item.getContextName());
                }

                if(contextInstanceSearchFilter.getContextSearchFilter() != null && !contextInstanceSearchFilter.getContextSearchFilter().isEmpty()) {
                    filter = item.getContextName().toLowerCase().contains(contextInstanceSearchFilter.getContextSearchFilter().toLowerCase());
                }

                if(contextInstanceSearchFilter.getContextInstanceId() != null && !contextInstanceSearchFilter.getContextInstanceId().isEmpty()) {
                    filter = item.getContextInstanceId().toLowerCase().contains(contextInstanceSearchFilter.getContextInstanceId().toLowerCase());
                }

                if(contextInstanceSearchFilter.getStartTime() > 0 && contextInstanceSearchFilter.getEndTime() > 0) {
                    filter = item.getStartTime() > contextInstanceSearchFilter.getStartTime() && item.getStartTime()
                        < contextInstanceSearchFilter.getEndTime();
                }

                return filter;
            })
            .collect(Collectors.toList());

        if(sortField != null) {
            contextInstanceRecords.sort((o1, o2) -> {
                logger.info("Sorting collection");
                if(sortField.equals("name")) {
                    if(sortOrder.equals(SortDirection.ASCENDING.name())) {
                        return o1.getContextName().compareTo(o2.getContextName());
                    }
                    else {
                        return o2.getContextName().compareTo(o1.getContextName());
                    }
                }
                else if(sortField.equals("id")) {
                    if(sortOrder.equals(SortDirection.ASCENDING.name())) {
                        return o1.getContextInstanceId().compareTo(o2.getContextInstanceId());
                    }
                    else {
                        return o2.getContextInstanceId().compareTo(o1.getContextInstanceId());
                    }
                }
                else if(sortField.equals("startTime")) {
                    if(sortOrder.equals(SortDirection.ASCENDING.name())) {
                        return Long.compare(o1.getStartTime(), o2.getStartTime());
                    }
                    else {
                        return Long.compare(o2.getStartTime(), o1.getStartTime());
                    }
                }
                return 0;
            });
        }

        if(offset >= 0 && limit > 0 && offset + limit >= contextInstanceRecords.size()) {
            contextInstanceRecords = contextInstanceRecords.subList(offset, contextInstanceRecords.size());
        }
        else if(offset >= 0 && limit > 0 && offset + limit < contextInstanceRecords.size()) {
            contextInstanceRecords = contextInstanceRecords.subList(offset, offset + limit);
        }

        return contextInstanceRecords.stream().map(scheduledContextInstanceRecord -> {
            ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
            return new PreparedFutureJobPlanInstance(contextInstance.getName(), contextInstance.getId(), contextInstance.getStartTime());
        }).collect(Collectors.toList());
    }

    /**
     * Filters the complete context instances based on the provided search filter and returns a list of CompletedJobPlanInstance objects.
     *
     * @param contextInstanceSearchFilter The search filter for filtering the context instances.
     * @param offset                      The offset of the result set.
     * @param limit                       The maximum number of results to return.
     * @param sortField                   The field to sort the results by.
     * @param sortOrder                   The order to sort the results in (ASC or DESC).
     * @return The list of completed context instances.
     */
    private List<CompletedJobPlanInstance> filterCompleteContextInstances(ContextInstanceSearchFilter contextInstanceSearchFilter, int offset, int limit,
                                                                          String sortField, String sortOrder) {
        contextInstanceSearchFilter.setStatus(InstanceStatus.ENDED.name());

        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        if(!canAccessAllJobPlans) {
            contextInstanceSearchFilter.setContextInstanceNames(accessibleJobPlans.stream().collect(Collectors.toList()));
        }

        List<ScheduledContextInstanceRecord> completedContextInstances = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(contextInstanceSearchFilter, limit, offset, sortField, sortOrder).getResultList();

        return completedContextInstances.stream().map(scheduledContextInstanceRecord -> {
            ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
            return new CompletedJobPlanInstance(contextInstance.getName(), contextInstance.getId(),
                contextInstance.getStartTime(), contextInstance.getEndTime());
        }).collect(Collectors.toList());
    }

    /**
     * Filters the complete context instances based on the provided search filter and returns the size of the filtered list.
     *
     * @param contextInstanceSearchFilter The search filter for filtering the context instances.
     * @return The size of the filtered complete context instances.
     */
    private long filterCompleteContextInstancesSize(ContextInstanceSearchFilter contextInstanceSearchFilter) {
        contextInstanceSearchFilter.setStatus(InstanceStatus.ENDED.name());

        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        if(!canAccessAllJobPlans) {
            contextInstanceSearchFilter.setContextInstanceNames(accessibleJobPlans.stream().collect(Collectors.toList()));
        }

        return this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(contextInstanceSearchFilter, 0, 0, null, null).getTotalNumberOfResults();
    }

    /**
     * A class representing a status filter for context instances.
     */
    private class StatusFilter {
        private String contextName;
        private String contextInstanceId;
        private boolean filterWaiting = false;
        private boolean filterComplete = false;
        private boolean filterRunning = false;
        private boolean filterOnHold = false;
        private boolean filterQueued = false;
        private boolean filterSkipped = false;
        private boolean filterError = false;

        public String getContextName() {
            return contextName;
        }

        public void setContextName(String contextName) {
            this.contextName = contextName;
        }

        public String getContextInstanceId() {
            return contextInstanceId;
        }

        public void setContextInstanceId(String contextInstanceId) {
            this.contextInstanceId = contextInstanceId;
        }

        public boolean isFilterWaiting() {
            return filterWaiting;
        }

        public void setFilterWaiting(boolean filterWaiting) {
            this.filterWaiting = filterWaiting;
        }

        public boolean isFilterComplete() {
            return filterComplete;
        }

        public void setFilterComplete(boolean filterComplete) {
            this.filterComplete = filterComplete;
        }

        public boolean isFilterRunning() {
            return filterRunning;
        }

        public void setFilterRunning(boolean filterRunning) {
            this.filterRunning = filterRunning;
        }

        public boolean isFilterOnHold() {
            return filterOnHold;
        }

        public void setFilterOnHold(boolean filterOnHold) {
            this.filterOnHold = filterOnHold;
        }

        public boolean isFilterQueued() {
            return filterQueued;
        }

        public void setFilterQueued(boolean filterQueued) {
            this.filterQueued = filterQueued;
        }

        public boolean isFilterSkipped() {
            return filterSkipped;
        }

        public void setFilterSkipped(boolean filterSkipped) {
            this.filterSkipped = filterSkipped;
        }

        public boolean isFilterError() {
            return filterError;
        }

        public void setFilterError(boolean filterError) {
            this.filterError = filterError;
        }

        public void clearFilter() {
            this.setFilterError(false);
            this.setFilterSkipped(false);
            this.setFilterOnHold(false);
            this.setFilterQueued(false);
            this.setFilterRunning(false);
            this.setFilterComplete(false);
            this.setFilterWaiting(false);
            this.setContextInstanceId("");
            this.setContextName("");
        }
    }

    /**
     * Inner class representing a Prepared Future Job Plan instance.
     */
    private class PreparedFutureJobPlanInstance {
        private String jobPlanName;
        private String contextInstanceId;
        private long contextInstanceStartTime;

        public PreparedFutureJobPlanInstance(String jobPlanName, String contextInstanceId, long contextInstanceStartTime) {
            this.jobPlanName = jobPlanName;
            this.contextInstanceId = contextInstanceId;
            this.contextInstanceStartTime = contextInstanceStartTime;
        }

        public String getJobPlanName() {
            return jobPlanName;
        }

        public String getContextInstanceId() {
            return contextInstanceId;
        }

        public long getContextInstanceStartTime() {
            return contextInstanceStartTime;
        }
    }

    /**
     * Represents a completed job plan instance.
     */
    private class CompletedJobPlanInstance {
        private String jobPlanName;
        private String contextInstanceId;
        private long contextInstanceStartTime;
        private long contextInstanceEndTime;
        public CompletedJobPlanInstance(String jobPlanName, String contextInstanceId
            , long contextInstanceStartTime, long contextInstanceEndTime) {
            this.jobPlanName = jobPlanName;
            this.contextInstanceId = contextInstanceId;
            this.contextInstanceStartTime = contextInstanceStartTime;
            this.contextInstanceEndTime = contextInstanceEndTime;
        }

        public String getJobPlanName() {
            return jobPlanName;
        }

        public String getContextInstanceId() {
            return contextInstanceId;
        }

        public long getContextInstanceStartTime() {
            return contextInstanceStartTime;
        }

        public long getContextInstanceEndTime() {
            return contextInstanceEndTime;
        }
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if(this.ui == null || !this.ui.isAttached() || ui.isClosing() || ui.getSession() == null) {
            this.unregisterFromBroadcasters();
            return;
        }

        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                if(event.getContextInstance().getStatus().equals(InstanceStatus.PREPARED) ||
                    event.getContextInstance().getStatus().equals(InstanceStatus.WAITING) ||
                    event.getContextInstance().getStatus().equals(InstanceStatus.ENDED)) {
                    this.preparedFutureContextInstanceGrid.getDataCommunicator().reset();
                    this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
                }
            });
        }
    }

    @Override
    public void receiveBroadcast(ContextInstance event) {
        if(this.ui == null || !this.ui.isAttached() || ui.isClosing() || ui.getSession() == null) {
            this.unregisterFromBroadcasters();
            return;
        }

        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                if(event.getStatus().equals(InstanceStatus.PREPARED) ||
                    event.getStatus().equals(InstanceStatus.WAITING) ||
                    event.getStatus().equals(InstanceStatus.ENDED)) {
                    this.preparedFutureContextInstanceGrid.getDataCommunicator().reset();
                    this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
                }
            });
        }
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        if(this.ui == null || !this.ui.isAttached() || ui.isClosing() || ui.getSession() == null) {
            this.unregisterFromBroadcasters();
            return;
        }

        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                this.updateAggregateContextInstanceAggregateStatus(event.getContextInstance().getId());
            });
        }
    }

    /**
     * Updates the aggregate status of a specific context instance.
     *
     * @param contextInstanceId The ID of the context instance to update.
     */
    private void updateAggregateContextInstanceAggregateStatus(String contextInstanceId) {
        List<ContextInstanceAggregateJobStatus> statuses = this.schedulerJobInstanceService
            .getJobStatusCountForContextInstances(List.of(contextInstanceId));
        if(!statuses.isEmpty()) {
            statuses.forEach(status -> this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshItem(status));
        }
        this.updateAggregateJobStatusFilteringCounts();
    }



    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        SchedulerJobStateChangeEventBroadcaster.instance().register(this);
        ContextInstanceStateChangeEventBroadcaster.instance().register(this);
        ContextInstanceSavedEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.ui = null;
        this.unregisterFromBroadcasters();
    }

    protected void unregisterFromBroadcasters() {
        SchedulerJobStateChangeEventBroadcaster.instance().unregister(this);
        ContextInstanceStateChangeEventBroadcaster.instance().unregister(this);
        ContextInstanceSavedEventBroadcaster.instance().unregister(this);
    }
}
