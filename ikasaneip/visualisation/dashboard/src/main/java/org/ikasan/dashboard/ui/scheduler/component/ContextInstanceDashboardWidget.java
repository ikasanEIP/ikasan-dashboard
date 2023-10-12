package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.scheduler.command.HoldAllCommandExecutionJobsForContextInstanceCommand;
import org.ikasan.dashboard.ui.scheduler.command.ReleaseAllCommandExecutionJobsForContextInstanceCommand;
import org.ikasan.dashboard.ui.scheduler.util.ContextInstanceSavedEventBroadcaster;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceMonitoringView;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.AggregateContextInstanceStatus;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl.SCHEDULED_CONTEXT_INSTANCE;

public class ContextInstanceDashboardWidget extends Div
    implements SchedulerJobStateChangeEventBroadcastListener, ContextInstanceStateChangeEventBroadcastListener, ContextInstanceSavedEventBroadcastListener {
    private Grid<ContextInstanceAggregateJobStatus> contextInstanceAggregateJobStatusGrid;
    private Grid<ScheduledContextInstanceRecord> preparedFutureContextInstanceGrid;
    private Grid<ScheduledContextInstanceRecord> completedContextInstanceGrid;
    private ScheduledProcessManagementService scheduledProcessManagementService;
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
    private TextField contextNameTf = new TextField();
    private TextField contextInstanceIdTf = new TextField();

    private TextField preparedContextNameTf = new TextField();
    private TextField preparedContextInstanceIdTf = new TextField();

    private TextField completeContextNameTf = new TextField();
    private TextField completeContextInstanceIdTf = new TextField();
    private StatusFilter statusFilter = new StatusFilter();
    private ContextInstanceSearchFilter contextInstanceSearchFilter = new SolrContextInstanceSearchFilterImpl();
    private ContextInstanceSearchFilter completeContextInstanceSearchFilter = new SolrContextInstanceSearchFilterImpl();
    private IkasanAuthentication ikasanAuthentication;
    private UI ui;

    private Tabs tabs;
    private Tab activeJobPlanInstancesTab;
    private Tab preparedFutureJobPlanInstancesTab;
    private Tab completedJobPlanInstancesTab;
    private VerticalLayout activeInstancesDiv;
    private VerticalLayout preparedFutureInstancesDiv;
    private VerticalLayout completedInstancesDiv;

    private DateFormatter dateFormatter = DateFormatter.instance();

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerService
     */
    public ContextInstanceDashboardWidget(ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                          SystemEventLogger systemEventLogger, SchedulerService schedulerService, SchedulerJobService schedulerJobService,
                                          SchedulerJobInstanceService schedulerJobInstanceService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath,
                                          ModuleMetaDataService moduleMetaDataService, LogStreamingService logStreamingService,
                                          JobInitiationService jobInitiationService, ContextProfileService contextProfileService,
                                          JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService, boolean fullscreen, GlobalEventService globalEventService,
                                          ContextInstanceRegistrationService contextInstanceRegistrationService) {

        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if(this.scheduledProcessManagementService ==  null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }
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

        this.ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.createContextInstanceGrid();
        this.createPreparedFutureContextInstanceGrid();
        this.createCompleteContextInstanceGrid();
        this.createActiveInstancesTab(fullscreen);
        this.createPreparedFutureInstancesTab(fullscreen);
        this.createCompleteInstancesTab(fullscreen);
        this.initialiseTabs();
        this.add(this.tabs, this.activeInstancesDiv, this.preparedFutureInstancesDiv, this.completedInstancesDiv);
        this.addClassNames("card-counter");
        if(fullscreen) {
            this.setHeight("90vh");
            contextInstanceAggregateJobStatusGrid.setHeight("90%");
        }
        else {
            this.setHeight("800px");
            contextInstanceAggregateJobStatusGrid.setHeight("80%");
        }
    }

    private void initialiseTabs() {
        this.activeJobPlanInstancesTab = new Tab(getTranslation("header.active-context-instances", UI.getCurrent().getLocale()));
        this.activeJobPlanInstancesTab.setId("activeJobPlanInstancesTab");
        this.preparedFutureJobPlanInstancesTab = new Tab(getTranslation("header.future-prepared-context-instances", UI.getCurrent().getLocale()));
        this.preparedFutureJobPlanInstancesTab.setId("contextTemplateTab");
        this.completedJobPlanInstancesTab = new Tab(getTranslation("header.completed-context-instances", UI.getCurrent().getLocale()));
        this.completedJobPlanInstancesTab.setId("completedJobPlanInstances");

        this.tabs = new Tabs(this.activeJobPlanInstancesTab, this.preparedFutureJobPlanInstancesTab, this.completedJobPlanInstancesTab);

        Map<Tab, Component> tabsToPages = new HashMap<>();
        tabsToPages.put(this.activeJobPlanInstancesTab, this.activeInstancesDiv);
        tabsToPages.put(this.preparedFutureJobPlanInstancesTab, this.preparedFutureInstancesDiv);
        tabsToPages.put(this.completedJobPlanInstancesTab, this.completedInstancesDiv);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });
    }

    private void createActiveInstancesTab(boolean fullscreen) {
        this.activeInstancesDiv = new VerticalLayout();
        this.activeInstancesDiv.setSizeFull();

        Button breakOut = new Button();
        breakOut.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        breakOut.setVisible(!fullscreen);
        breakOut.setWidth("50px");
        breakOut.setHeight("50px");
        breakOut.addClickListener(event -> {
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceMonitoringView.class);

            getUI().ifPresent(ui -> ui.getPage().open(route));
        });

        Button refresh = new Button("Refresh", VaadinIcon.REFRESH.create());
        refresh.setIconAfterText(true);
        refresh.addClickListener(event -> {
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        Button clearFiltersButton = new Button("Clear Filters", VaadinIcon.FILTER.create());
        clearFiltersButton.setIconAfterText(true);
        clearFiltersButton.addClickListener(event -> {
            this.contextNameTf.setValue("");
            this.contextInstanceIdTf.setValue("");
            this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout rightSideButtons = new HorizontalLayout();
        rightSideButtons.add(clearFiltersButton, refresh, breakOut);

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(rightSideButtons);
        layout.setWidth("100%");
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, rightSideButtons);

        rightSideButtons.getElement().getStyle().set("margin-left", "auto");

        activeInstancesDiv.add(layout);
        activeInstancesDiv.add(this.contextInstanceAggregateJobStatusGrid);
    }

    private void createPreparedFutureInstancesTab(boolean fullscreen) {
        this.preparedFutureInstancesDiv = new VerticalLayout();
        this.preparedFutureInstancesDiv.setSizeFull();

        Button breakOut = new Button();
        breakOut.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        breakOut.setVisible(!fullscreen);
        breakOut.setWidth("50px");
        breakOut.setHeight("50px");
        breakOut.addClickListener(event -> {
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceMonitoringView.class);

            getUI().ifPresent(ui -> ui.getPage().open(route));
        });

        Button refresh = new Button("Refresh", VaadinIcon.REFRESH.create());
        refresh.setIconAfterText(true);
        refresh.addClickListener(event -> {
            this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
        });

        Button clearFiltersButton = new Button("Clear Filters", VaadinIcon.FILTER.create());
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

        preparedFutureInstancesDiv.add(layout);
        preparedFutureInstancesDiv.add(this.preparedFutureContextInstanceGrid);
    }

    private void createCompleteInstancesTab(boolean fullscreen) {
        this.completedInstancesDiv = new VerticalLayout();
        this.completedInstancesDiv.setSizeFull();

        Button breakOut = new Button();
        breakOut.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        breakOut.setVisible(!fullscreen);
        breakOut.setWidth("50px");
        breakOut.setHeight("50px");
        breakOut.addClickListener(event -> {
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceMonitoringView.class);

            getUI().ifPresent(ui -> ui.getPage().open(route));
        });

        Button refresh = new Button("Refresh", VaadinIcon.REFRESH.create());
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

        completedInstancesDiv.add(layout);
        completedInstancesDiv.add(this.completedContextInstanceGrid);
    }

    private void createContextInstanceGrid() {
        // Create a modulesGrid bound to the list
        contextInstanceAggregateJobStatusGrid = new Grid<>();
        contextInstanceAggregateJobStatusGrid.setId("contextInstanceAggregateJobStatusGrid");
        contextInstanceAggregateJobStatusGrid.removeAllColumns();
        contextInstanceAggregateJobStatusGrid.setVisible(true);
        contextInstanceAggregateJobStatusGrid.setWidthFull();

        contextInstanceAggregateJobStatusGrid.addColumn(ContextInstanceAggregateJobStatus::getContextInstanceName)
            .setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(2)
            .setResizable(true);
        contextInstanceAggregateJobStatusGrid.addColumn(ContextInstanceAggregateJobStatus::getContextInstanceId)
            .setHeader(getTranslation("table-header.context-instance-id", UI.getCurrent().getLocale())).setKey("id")
            .setFlexGrow(2)
            .setResizable(true);
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING)
                + " " + getTranslation(InstanceStatus.WAITING.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_WAITING, IkasanColours.BLACK,
                contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING));
            statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING)>0);
            statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.WAITING));

            Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_WAITING, IkasanColours.BLACK);
            breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.WAITING)>0);
            breakOut.addClickListener(event -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                        , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.WAITING.name()));

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            horizontalLayout.add(statusButton, breakOut);
            return horizontalLayout;
        }))
            .setHeader(getTranslation("table-header.job-status-counts", UI.getCurrent().getLocale()))
            .setKey("statusCounts");
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE)
                + " " + getTranslation(InstanceStatus.COMPLETE.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_COMPLETE, IkasanColours.WHITE,
                contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE));
            statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE)>0);
            statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.COMPLETE));

            Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_COMPLETE, IkasanColours.WHITE);
            breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.COMPLETE)>0);
            breakOut.addClickListener(event -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                        , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.COMPLETE.name()));

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            horizontalLayout.add(statusButton, breakOut);
            return horizontalLayout;
        }));
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING)
                + " " + getTranslation(InstanceStatus.RUNNING.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_RUNNING, IkasanColours.WHITE,
                contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING));
            statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING)>0);
            statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.RUNNING));

            Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_RUNNING, IkasanColours.WHITE);
            breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.RUNNING)>0);
            breakOut.addClickListener(event -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                        , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.RUNNING.name()));

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            horizontalLayout.add(statusButton, breakOut);

            return horizontalLayout;
        }));
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED)
                + " " + getTranslation(InstanceStatus.LOCK_QUEUED.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_LOCK_QUEUED, IkasanColours.WHITE,
                contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED));
            statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED)>0);
            statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.LOCK_QUEUED));

            Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_LOCK_QUEUED, IkasanColours.WHITE);
            breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.LOCK_QUEUED)>0);
            breakOut.addClickListener(event -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                        , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.LOCK_QUEUED.name()));

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            horizontalLayout.add(statusButton, breakOut);

            return horizontalLayout;
        }));
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD)
                + " " + getTranslation(InstanceStatus.ON_HOLD.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_ON_HOLD, IkasanColours.WHITE,
                contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD));
            statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD)>0);
            statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ON_HOLD));

            Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_ON_HOLD, IkasanColours.WHITE);
            breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ON_HOLD)>0);
            breakOut.addClickListener(event -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                        , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ON_HOLD.name()));

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            horizontalLayout.add(statusButton, breakOut);
            return horizontalLayout;
        }));
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED)
                + " " + getTranslation(InstanceStatus.SKIPPED.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_SKIPPED, IkasanColours.WHITE,
                contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED));
            statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED)>0);
            statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.SKIPPED));

            Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_SKIPPED, IkasanColours.WHITE);
            breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.SKIPPED)>0);
            breakOut.addClickListener(event -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                        , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.SKIPPED.name()));

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            horizontalLayout.add(statusButton, breakOut);

            return horizontalLayout;
        }));
        contextInstanceAggregateJobStatusGrid.addColumn(new ComponentRenderer<>(contextInstanceAggregateJobStatus -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Button statusButton = this.buildStatusCountButton(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR)
                + " " + getTranslation(InstanceStatus.ERROR.getTranslationLabel(), UI.getCurrent().getLocale()), IkasanColours.SCHEDULER_ERROR, IkasanColours.WHITE
                , contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR));
            statusButton.setEnabled(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR)>0);
            statusButton.addClickListener(event -> this.openContextInstanceDialog(contextInstanceAggregateJobStatus
                , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ERROR));

            Button breakOut = this.buildStatusBreakoutButton(IkasanColours.SCHEDULER_ERROR, IkasanColours.WHITE);
            breakOut.setVisible(contextInstanceAggregateJobStatus.getStatusCount(InstanceStatus.ERROR)>0);
            breakOut.addClickListener(event -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, List.of(contextInstanceAggregateJobStatus.getContextInstanceId() +"_scheduledContextInstance"
                        , ContextInstanceWidget.JOB_INSTANCE_TAB, InstanceStatus.ERROR.name()));

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            horizontalLayout.add(statusButton, breakOut);
            return horizontalLayout;
        }));

        this.contextNameTf = new TextField();
        this.contextInstanceIdTf = new TextField();
        HeaderRow hr = this.contextInstanceAggregateJobStatusGrid.appendHeaderRow();
        this.addGridFiltering(hr, "name", this.contextNameTf, this.statusFilter::setContextName);
        this.addGridFiltering(hr, "id", this.contextInstanceIdTf, this.statusFilter::setContextInstanceId);

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

    private void createPreparedFutureContextInstanceGrid() {
        // Create a modulesGrid bound to the list
        this.preparedFutureContextInstanceGrid = new Grid<>();
        this.preparedFutureContextInstanceGrid.setId("preparedFutureContextInstanceGrid");
        this.preparedFutureContextInstanceGrid.removeAllColumns();
        this.preparedFutureContextInstanceGrid.setVisible(true);
        this.preparedFutureContextInstanceGrid.setWidthFull();

        this.preparedFutureContextInstanceGrid.addColumn(ScheduledContextInstanceRecord::getContextName)
            .setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(2)
            .setResizable(true)
            .setSortable(true);
        this.preparedFutureContextInstanceGrid.addColumn(ScheduledContextInstanceRecord::getContextInstanceId)
            .setHeader(getTranslation("table-header.context-instance-id", UI.getCurrent().getLocale())).setKey("id")
            .setFlexGrow(2)
            .setResizable(true)
            .setSortable(true);
        this.preparedFutureContextInstanceGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceRecord>of("<div style='white-space:normal'>[[item.startTime]]</div>")
                .withProperty("startTime", scheduledProcessEvent -> this.dateFormatter.getFormattedDate(scheduledProcessEvent.getStartTime())))
            .setHeader(getTranslation("table-header.context-instance-start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTime")
            .setFlexGrow(4)
            .setSortable(true);
        this.preparedFutureContextInstanceGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setSpacing(false);
                layout.setMargin(false);
                layout.setPadding(false);
                layout.setSizeFull();

                ContextInstance instance = scheduledContextInstanceRecord.getContextInstance();

                if(ContextMachineCache.instance().containsInstanceIdentifier(scheduledContextInstanceRecord.getContextInstance().getId())) {
                    instance = ContextMachineCache.instance().getByContextInstanceId(scheduledContextInstanceRecord.getContextInstance().getId()).getContext();
                }

                AggregateContextInstanceStatus aggregateContextInstanceStatus =
                    ContextHelper.getAggregateContextInstanceStatus(instance);

                HorizontalLayout statusLayout = new HorizontalLayout();
                statusLayout.setWidthFull();

                if(aggregateContextInstanceStatus.isHeldJobs()) {
                    SchedulerStatusIconDiv onHoldStatusDiv = new SchedulerStatusIconDiv();
                    onHoldStatusDiv.setStatus(InstanceStatus.ON_HOLD, getTranslation("label.prepared-job-plan-contains-held-jobs", UI.getCurrent().getLocale()));
                    statusLayout.add(onHoldStatusDiv);
                }

                layout.add(statusLayout);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, statusLayout);

                return layout;
            }))
            .setFlexGrow(1);
        this.preparedFutureContextInstanceGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setMargin(false);
                layout.setSizeFull();

                Icon breakOut = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("tooltip.open-in-new-window", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(scheduledContextInstanceRecord.getContextInstanceId() +"_scheduledContextInstance"));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });

                Icon holdButton = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-all-command-execution-jobs-in-instance", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                holdButton.addClickListener(event -> {
                    HoldAllCommandExecutionJobsForContextInstanceCommand holdAllCommandExecutionJobsForContextInstanceCommand
                        = new HoldAllCommandExecutionJobsForContextInstanceCommand(scheduledContextInstanceRecord.getContextInstance(), this.schedulerJobInstanceService,
                            this.systemEventLogger, this.ikasanAuthentication);
                    holdAllCommandExecutionJobsForContextInstanceCommand.execute();
                });

                Icon releaseButton = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-all-command-execution-jobs-in-instance", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                releaseButton.addClickListener(event -> {
                    ReleaseAllCommandExecutionJobsForContextInstanceCommand releaseAllCommandExecutionJobsForContextInstanceCommand
                        = new ReleaseAllCommandExecutionJobsForContextInstanceCommand(scheduledContextInstanceRecord.getContextInstance(), this.schedulerJobInstanceService,
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
        this.preparedContextInstanceIdTf = new TextField();
        HeaderRow hr = this.preparedFutureContextInstanceGrid.appendHeaderRow();
        this.addPreparedContextInstanceGridFiltering(hr, "name", this.preparedContextNameTf, this.contextInstanceSearchFilter::setContextSearchFilter);
        this.addPreparedContextInstanceGridFiltering(hr, "id", this.preparedContextInstanceIdTf, this.contextInstanceSearchFilter::setContextInstanceId);
        this.addDateTimeGridFiltering(hr, contextInstanceSearchFilter::setStartTime, contextInstanceSearchFilter::setEndTime, null, null,
            "startTime", this.preparedFutureContextInstanceGrid);
        DataProvider<ScheduledContextInstanceRecord, ContextInstanceSearchFilter> dataProvider =
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
                query -> this.filterPreparedContextInstances(this.contextInstanceSearchFilter, -1, -1, null, null).size());

        dataProvider.withConfigurableFilter().setFilter(this.contextInstanceSearchFilter);
        this.preparedFutureContextInstanceGrid.setDataProvider(dataProvider);
        this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
    }

    private void createCompleteContextInstanceGrid() {
        // Create a modulesGrid bound to the list
        this.completedContextInstanceGrid = new Grid<>();
        this.completedContextInstanceGrid.setId("completedContextInstanceGrid");
        this.completedContextInstanceGrid.removeAllColumns();
        this.completedContextInstanceGrid.setVisible(true);
        this.completedContextInstanceGrid.setWidthFull();

        this.completedContextInstanceGrid.addColumn(ScheduledContextInstanceRecord::getContextName)
            .setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(2)
            .setResizable(true)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(ScheduledContextInstanceRecord::getContextInstanceId)
            .setHeader(getTranslation("table-header.context-instance-id", UI.getCurrent().getLocale())).setKey("id")
            .setFlexGrow(3)
            .setResizable(true)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceRecord>of("<div style='white-space:normal'>[[item.startTime]]</div>")
                .withProperty("startTime", scheduledProcessEvent -> this.dateFormatter.getFormattedDate(scheduledProcessEvent.getStartTime())))
            .setHeader(getTranslation("table-header.start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTime")
            .setFlexGrow(3)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceRecord>of("<div style='white-space:normal'>[[item.endTime]]</div>")
                .withProperty("endTime", scheduledProcessEvent -> this.dateFormatter.getFormattedDate(scheduledProcessEvent.getEndTime())))
            .setHeader(getTranslation("table-header.end-date-time", UI.getCurrent().getLocale()))
            .setKey("endTime")
            .setFlexGrow(3)
            .setSortable(true);
        this.completedContextInstanceGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setMargin(false);
                layout.setSizeFull();

                Icon breakOut = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("tooltip.open-in-new-window", UI.getCurrent().getLocale()
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                breakOut.addClickListener(event -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, List.of(scheduledContextInstanceRecord.getContextInstanceId() +"_scheduledContextInstance"));

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
        this.completeContextInstanceIdTf = new TextField();
        HeaderRow hr = this.completedContextInstanceGrid.appendHeaderRow();
        this.addCompletedContextInstanceGridFiltering(hr, "name", this.completeContextNameTf, this.completeContextInstanceSearchFilter::setContextSearchFilter);
        this.addCompletedContextInstanceGridFiltering(hr, "id", this.completeContextInstanceIdTf, this.completeContextInstanceSearchFilter::setContextInstanceId);
        this.addDateTimeGridFiltering(hr, completeContextInstanceSearchFilter::setStartTime, completeContextInstanceSearchFilter::setEndTime,
            completeContextInstanceSearchFilter::setStartTimeStart, completeContextInstanceSearchFilter::setStartTimeEnd,"startTime", this.completedContextInstanceGrid);
        this.addDateTimeGridFiltering(hr, completeContextInstanceSearchFilter::setStartTime, completeContextInstanceSearchFilter::setEndTime,
            completeContextInstanceSearchFilter::setEndTimeStart, completeContextInstanceSearchFilter::setEndTimeEnd,"endTime", this.completedContextInstanceGrid);
        DataProvider<ScheduledContextInstanceRecord, ContextInstanceSearchFilter> dataProvider =
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
                query -> this.filterCompleteContextInstances(this.completeContextInstanceSearchFilter, -1, -1, null, null).size());

        dataProvider.withConfigurableFilter().setFilter(this.completeContextInstanceSearchFilter);
        this.completedContextInstanceGrid.setDataProvider(dataProvider);
        this.completedContextInstanceGrid.getDataProvider().refreshAll();
    }

    private Button buildStatusCountButton(String label, String backgroundColour, String fontColour, int count) {
        Button statusButton = new Button(label);
        if(count > 0) {
            statusButton.getElement().getStyle().set("background-color", backgroundColour);
            statusButton.getElement().getStyle().set("color", fontColour);
        }
        statusButton.getElement().getStyle().set("font-size", "8pt");
        statusButton.setWidth("100%");

        return statusButton;
    }

    private Button buildStatusBreakoutButton(String backgroundColour, String fontColour) {
        Button breakOut = new Button();
        breakOut.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        breakOut.getElement().getStyle().set("background-color", backgroundColour);
        breakOut.getElement().getStyle().set("color", fontColour);
        breakOut.setWidth("50px");

        return breakOut;
    }

    private void openContextInstanceDialog(ContextInstanceAggregateJobStatus contextInstanceAggregateJobStatus, String contextInstanceWidgetTab,
                            InstanceStatus status) {
        ContextInstance contextInstance = this.scheduledContextInstanceService
            .findById(contextInstanceAggregateJobStatus.getContextInstanceId()+ "_" + SCHEDULED_CONTEXT_INSTANCE).getContextInstance();
        ContextTemplate contextTemplate = this.scheduledContextService.findByName(contextInstanceAggregateJobStatus.getContextInstanceName()).getContext();
        ContextInstanceDialog contextInstanceDialog = new ContextInstanceDialog(this.scheduledContextInstanceService, this.dynamicImagePath, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService, contextInstance, contextTemplate,
            this.schedulerJobInstanceService, this.jobInitiationService, this.contextProfileService, this.jobUtilsService, this.scheduledContextService, contextInstanceWidgetTab, status.name(), this.globalEventService,
            this.contextInstanceRegistrationService);

        contextInstanceDialog.open();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        SchedulerJobStateChangeEventBroadcaster.register(this);
        ContextInstanceStateChangeEventBroadcaster.register(this);
        ContextInstanceSavedEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.ui = null;
        SchedulerJobStateChangeEventBroadcaster.unregister(this);
        ContextInstanceStateChangeEventBroadcaster.unregister(this);
        ContextInstanceSavedEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                List<ContextInstanceAggregateJobStatus> statuses = this.schedulerJobInstanceService
                    .getJobStatusCountForContextInstances(List.of(event.getContextInstance().getId()));
                if(!statuses.isEmpty()) {
                    statuses.forEach(status -> this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshItem(status));
                }
            });
        }
    }

    private void addGridFiltering(HeaderRow hr, String columnKey, TextField textField, Consumer<String> setFilter) {
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
     * Add filtering to a column.
     *
     * @param hr
     * @param setStartTime
     * @param columnKey
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

        Label timeLabel = new Label();
        Icon clearFilter = IconDecorator.decorate(VaadinIcon.CLOSE_SMALL.create()
            , getTranslation("tooltip.clear-filter", UI.getCurrent().getLocale()), "14px", "");
        clearFilter.setSize("14px");
        clearFilter.setVisible(false);

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
                clearFilter.setVisible(true);
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
                clearFilter.setVisible(true);
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
                clearFilter.setVisible(true);
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
                clearFilter.setVisible(true);
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
                    clearFilter.setVisible(true);
                }
            }
        });

        clearFilter.addClickListener(event -> {
            clearFilter.setVisible(false);
            startDatePicker.setValue(null);
            startTimePicker.setValue(null);
            endDatePicker.setValue(null);
            endTimePicker.setValue(null);
            timeLabel.setText("");
        });

        HorizontalLayout filterLayout = new HorizontalLayout(icon, timeLabel, clearFilter);
        filterLayout.setWidth("450px");
        filterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, clearFilter);

        hr.getCell(grid.getColumnByKey(columnKey)).setComponent(filterLayout);
    }

    private List<ContextInstanceAggregateJobStatus> filterContextInstanceAggregateJobStatus(StatusFilter statusFilter, int offset, int limit) {
        List<String> contextInstanceIdentifiers = new ArrayList<>(ContextMachineCache.instance().contextInstanceIdentifiers());

        contextInstanceIdentifiers = contextInstanceIdentifiers.stream()
            .filter(id -> ContextMachineCache.instance().getByContextInstanceId(id) != null
                && !ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(InstanceStatus.PREPARED))
            .collect(Collectors.toList());

        List<ContextInstanceAggregateJobStatus> jobStatuses = this.schedulerJobInstanceService
            .getJobStatusCountForContextInstances(contextInstanceIdentifiers);

        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        jobStatuses = jobStatuses.stream().filter(item -> {
                boolean filter = true;

                if(!canAccessAllJobPlans) {
                    filter = accessibleJobPlans.contains(item.getContextInstanceName());
                }

                if(statusFilter.contextName != null && !statusFilter.contextName.isEmpty()) {
                    filter = item.getContextInstanceName().toLowerCase().contains(statusFilter.contextName.toLowerCase());
                }

                if(statusFilter.contextInstanceId != null && !statusFilter.contextInstanceId.isEmpty()) {
                    filter = item.getContextInstanceId().toLowerCase().contains(statusFilter.contextInstanceId.toLowerCase());
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

    private List<ScheduledContextInstanceRecord> filterPreparedContextInstances(ContextInstanceSearchFilter contextInstanceSearchFilter, int offset, int limit,
                                                                                String sortField, String sortOrder) {
        ContextInstanceSearchFilter preparedSearchFilter = new SolrContextInstanceSearchFilterImpl();
        preparedSearchFilter.setStatus(InstanceStatus.PREPARED.name());

        List<ScheduledContextInstanceRecord> jobStatuses = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(preparedSearchFilter, -1, -1, null, null).getResultList();

        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        jobStatuses = jobStatuses.stream().filter(item -> {
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

        if(offset >= 0 && limit > 0 && offset + limit >= jobStatuses.size()) {
            jobStatuses = jobStatuses.subList(offset, jobStatuses.size());
        }
        else if(offset >= 0 && limit > 0 && offset + limit < jobStatuses.size()) {
            jobStatuses = jobStatuses.subList(offset, offset + limit);
        }

        if(sortField != null) {
            jobStatuses.sort((o1, o2) -> {
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

        return jobStatuses;
    }

    private List<ScheduledContextInstanceRecord> filterCompleteContextInstances(ContextInstanceSearchFilter contextInstanceSearchFilter, int offset, int limit,
                                                                                String sortField, String sortOrder) {
        contextInstanceSearchFilter.setStatus(InstanceStatus.ENDED.name());

        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        if(!canAccessAllJobPlans) {
            contextInstanceSearchFilter.setContextInstanceNames(accessibleJobPlans.stream().collect(Collectors.toList()));
        }

        List<ScheduledContextInstanceRecord> completedContextInstances = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(contextInstanceSearchFilter, limit, offset, sortField, sortOrder).getResultList();


        return completedContextInstances;
    }

    private class StatusFilter {
        private String contextName;
        private String contextInstanceId;

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
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
                this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
                this.completedContextInstanceGrid.getDataProvider().refreshAll();
            });
        }
    }

    @Override
    public void receiveBroadcast(ContextInstance event) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                this.preparedFutureContextInstanceGrid.getDataProvider().refreshAll();
                this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
                this.completedContextInstanceGrid.getDataProvider().refreshAll();
            });
        }
    }
}
