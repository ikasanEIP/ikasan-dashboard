package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceMonitoringView;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
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
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl.SCHEDULED_CONTEXT_INSTANCE;

public class ContextInstanceDashboardWidget extends Div {
    private Registration schedulerJobStateChangeRegistration;
    private Grid<ContextInstanceAggregateJobStatus> contextInstanceAggregateJobStatusGrid;
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
    private StatusFilter statusFilter = new StatusFilter();
    private IkasanAuthentication ikasanAuthentication;

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

        this.createGrid();

        Div div = new Div();
        div.addClassNames("card-counter");
        if(fullscreen) {
            div.setHeight("90vh");
            contextInstanceAggregateJobStatusGrid.setHeight("90%");
        }
        else {
            div.setHeight("600px");
            contextInstanceAggregateJobStatusGrid.setHeight("80%");
        }

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
        H4 modules = new H4(getTranslation("header.active-context-instances", UI.getCurrent().getLocale()));
        layout.add(modules, rightSideButtons);
        layout.setWidth("100%");
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, rightSideButtons);

        rightSideButtons.getElement().getStyle().set("margin-left", "auto");

        div.add(layout);
        div.add(this.contextInstanceAggregateJobStatusGrid);

        this.add(div);
    }

    private void createGrid() {
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

                    return this.filter(this.statusFilter, offset, limit).stream();
                },
                // Second callback fetches the total number of items currently in the Grid.
                // The grid can then use it to properly adjust the scrollbars.
                query -> this.filter(this.statusFilter, -1, -1).size());

        dataProvider.withConfigurableFilter().setFilter(this.statusFilter);
        this.contextInstanceAggregateJobStatusGrid.setDataProvider(dataProvider);
        this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
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
        UI ui = attachEvent.getUI();
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if(ui.isAttached()) {
                ui.access(() -> {
                    this.contextInstanceAggregateJobStatusGrid.getDataProvider().refreshAll();
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulerJobStateChangeRegistration.remove();
        this.schedulerJobStateChangeRegistration = null;
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

    private List<ContextInstanceAggregateJobStatus> filter(StatusFilter statusFilter, int offset, int limit) {
        List<String> contextInstanceIdentifiers = new ArrayList<>(ContextMachineCache.instance().contextInstanceIdentifiers());

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
}
