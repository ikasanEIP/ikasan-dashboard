package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.GeneratedVaadinDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.cache.CacheStateBroadcaster;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.model.AgentJobFilter;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


public class AgentJobFilteringGrid extends FilteringGrid<ScheduledProcessAggregateConfiguration, AgentJobFilter
    , ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration>> {

    private ScheduledProcessManagementService scheduledProcessManagementService;

    private Registration flowStateBroadcasterRegistration;
    private Registration cacheStateBroadcasterRegistration;

    private DateFormatter dateFormatter;

    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    private ModuleMetaData agent;
    private UI ui;

    private IkasanAuthentication authentication;

    private SystemEventLogger systemEventLogger;

    private SchedulerService schedulerService;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param searchFilter
     */
    public AgentJobFilteringGrid(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService, AgentJobFilter searchFilter,
                                 DateFormatter dateFormatter, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, ModuleMetaDataService moduleMetaDataService, SystemEventLogger systemEventLogger,
                                 SchedulerService schedulerService) {
        super(searchFilter);
        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerService = schedulerService;

        this.ui = UI.getCurrent();
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.initGrid();
    }

    private void initGrid() {

        super.addColumn(TemplateRenderer.<ScheduledProcessAggregateConfiguration>of("<div style='white-space:normal; text-align:top;'>[[item.jobName]]</div>")
            .withProperty("jobName", ScheduledProcessAggregateConfiguration::getJobName))
            .setHeader("Job Name")
            .setKey("jobName")
            .setTextAlign(ColumnTextAlign.START)
            .setFlexGrow(1);

        super.addColumn(TemplateRenderer.<ScheduledProcessAggregateConfiguration>of("<div style='white-space:normal'>[[item.jobGroup]]</div>")
            .withProperty("jobGroup", ScheduledProcessAggregateConfiguration::getJobGroup))
            .setHeader("Job Group")
            .setKey("jobGroup")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessAggregateConfiguration>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", ScheduledProcessAggregateConfiguration::getJobDescription))
            .setHeader("Job Description")
            .setKey("description")
            .setFlexGrow(5);
        super.addColumn(new ComponentRenderer<>(scheduledProcessAggregateConfiguration -> {
            HorizontalLayout layout = new HorizontalLayout();

            scheduledProcessAggregateConfiguration.getBusinessStreamMetaData().forEach(businessStreamMetaData -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.BUSINESS_STREAM.name() + ":" + businessStreamMetaData.getName());
                Anchor link = new Anchor(route, businessStreamMetaData.getName());
                link.setTarget("_blank");
                layout.add(link);
                link.getStyle().set("color", "blue");
            });

            return layout;
        }))
            .setHeader("Related Business Streams")
            .setKey("businessStreams")
            .setFlexGrow(3);
        super.addColumn(new ComponentRenderer<>(scheduledProcessAggregateConfiguration -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = VaadinIcon.EDIT.create();
            edit.setSize("14pt");
            edit.getStyle().set("cursor", "pointer");
            edit.getElement().setAttribute("title", "Edit job");
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ScheduledJobDialog scheduledJobDialog = new ScheduledJobDialog(agent,
                    this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                    this.metaDataRestService, this.systemEventLogger);

                scheduledJobDialog.setScheduleProcessAggregateConfiguration(scheduledProcessAggregateConfiguration, EditMode.EDIT);
                scheduledJobDialog.open();

                scheduledJobDialog.addOpenedChangeListener((ComponentEventListener<GeneratedVaadinDialog.OpenedChangeEvent<Dialog>>)
                    dialogOpenedChangeEvent -> {
                    if(!dialogOpenedChangeEvent.isOpened()) {
                        this.dataProvider.refreshAll();
                        this.filteredDataProvider.refreshAll();
                    }
                });
            });

            layout.add(edit);

            Icon view = VaadinIcon.EYE.create();
            view.setSize("14pt");
            view.getStyle().set("cursor", "pointer");
            view.getElement().setAttribute("title", "View job");
            ComponentSecurityVisibility.applySecurity(this.authentication, view, SecurityConstants.SCHEDULER_READ);

            view.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ScheduledJobDialog scheduledJobDialog = new ScheduledJobDialog(agent,
                    this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                    this.metaDataRestService, systemEventLogger);

                scheduledJobDialog.setScheduleProcessAggregateConfiguration(scheduledProcessAggregateConfiguration, EditMode.READONLY);
                scheduledJobDialog.open();
            });

            layout.add(view);

            Icon delete = VaadinIcon.TRASH.create();
            delete.setSize("14pt");
            delete.getStyle().set("cursor", "pointer");
            delete.getElement().setAttribute("title", "Delete job");
            ComponentSecurityVisibility.applySecurity(this.authentication, delete, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            layout.add(delete);

            delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog dialog = new ConfirmDialog("Confirm delete",
                    "Are you sure you want to delete this job? This operation cannot be reversed.", "Delete", (ComponentEventListener<ConfirmDialog.ConfirmEvent>) confirmEvent -> {
                    try {
                        this.deleteScheduledJobFlow(scheduledProcessAggregateConfiguration);

                        String action = String.format("Deleted scheduled job [%s].", scheduledProcessAggregateConfiguration);
                        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

                        this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_DELETED, action, authentication.getName());
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification("An error has occurred deleting a scheduled job. Please contact Ikasan support.");
                    }
                    this.dataProvider.refreshAll();
                    this.filteredDataProvider.refreshAll();
                    }, "Cancel", (ComponentEventListener<ConfirmDialog.CancelEvent>) cancelEvent -> {});
                dialog.setConfirmButtonTheme("error primary");

                dialog.open();
            });

            Icon chart = VaadinIcon.CHART.create();
            chart.setSize("14pt");
            chart.getStyle().set("cursor", "pointer");
            chart.getElement().setAttribute("title", "Job statistics");
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ScheduledJobStatisticsDialog scheduledJobStatisticsDialog = new ScheduledJobStatisticsDialog(this.scheduledProcessManagementService,
                    this.moduleMetaDataService.findById(scheduledProcessAggregateConfiguration.getAgentName()), scheduledProcessAggregateConfiguration.getJobName());

                scheduledJobStatisticsDialog.open();
            });

            layout.add(chart);

            FlowState flowState = FlowStateCache.instance().get(this.agent
                , scheduledProcessAggregateConfiguration.getJobName());

            if(flowState == null) {
                return layout;
            }

            if(flowState.getState() == State.RUNNING_STATE) {
                Icon stop = VaadinIcon.STOP.create();
                stop.setSize("14pt");
                stop.getElement().setAttribute("title", "Stop scheduled job");
                stop.getStyle().set("cursor", "pointer");
                layout.add(stop);

                ComponentSecurityVisibility.applySecurity(this.authentication, stop, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                stop.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName(), "stop");
                });

                Icon pause = VaadinIcon.PAUSE.create();
                pause.setSize("14pt");
                pause.getStyle().set("color", "rgba(133,181,225,1.0)");
                pause.getElement().setAttribute("title", "Pause scheduled job");
                pause.getStyle().set("cursor", "pointer");
                layout.add(pause);

                ComponentSecurityVisibility.applySecurity(this.authentication, pause, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                pause.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName(), "pause");
                });
            }
            else if(flowState.getState() == State.RECOVERING_STATE) {
                Icon stop = VaadinIcon.STOP.create();
                stop.setSize("14pt");
                stop.getElement().setAttribute("title", "Stop scheduled job");
                stop.getStyle().set("cursor", "pointer");
                layout.add(stop);

                ComponentSecurityVisibility.applySecurity(this.authentication, stop, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                stop.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName(), "stop");
                });
            }
            else if(flowState.getState() == State.STOPPED_IN_ERROR_STATE
                    || flowState.getState() == State.STOPPED_STATE) {
                Icon start = VaadinIcon.PLAY.create();
                start.setSize("14pt");
                start.getStyle().set("color", "#66bb6a");
                start.getElement().setAttribute("title", "Start scheduled job");
                start.getStyle().set("cursor", "pointer");
                layout.add(start);

                ComponentSecurityVisibility.applySecurity(this.authentication, start, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                start.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName(), "start");
                });

                Icon pause = VaadinIcon.PAUSE.create();
                pause.setSize("14pt");
                pause.getStyle().set("cursor", "pointer");
                pause.getStyle().set("color", "rgba(133,181,225,1.0)");
                pause.getElement().setAttribute("title", "Pause scheduled job");
                layout.add(pause);

                ComponentSecurityVisibility.applySecurity(this.authentication, pause, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                pause.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName(), "pause");
                });
            }
            else if(flowState.getState() == State.PAUSED_STATE) {
                Icon start = VaadinIcon.PLAY.create();
                start.getStyle().set("cursor", "pointer");
                start.setSize("14pt");
                start.getStyle().set("color", "#66bb6a");
                start.getElement().setAttribute("title", "Start scheduled job");
                layout.add(start);

                ComponentSecurityVisibility.applySecurity(this.authentication, start, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                start.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName(), "start");
                });

                Icon stop = VaadinIcon.STOP.create();
                stop.setSize("14pt");
                stop.getStyle().set("cursor", "pointer");
                stop.getElement().setAttribute("title", "Stop scheduled job");
                layout.add(stop);

                ComponentSecurityVisibility.applySecurity(this.authentication, stop, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                stop.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName(), "stop");
                });
            }

            if(flowState.getState() == State.RUNNING_STATE) {
                Icon fireJob = VaadinIcon.ROCKET.create();
                fireJob.setSize("14pt");
                fireJob.getElement().setAttribute("title", "Fire job immediately");
                fireJob.getStyle().set("cursor", "pointer");
                layout.add(fireJob);

                ComponentSecurityVisibility.applySecurity(this.authentication, fireJob, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

                fireJob.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    if(this.schedulerService.triggerFlowNow(agent.getUrl(), agent.getName(), scheduledProcessAggregateConfiguration.getJobName())) {
                        NotificationHelper.showUserNotification("Job triggered successfully.");
                    }
                    else {
                        NotificationHelper.showUserNotification("An error occurred triggering job. Please contact Ikasan support.");
                    }
                });
            }

            layout.setSizeFull();
            return layout;
        }))
        .setHeader("Actions")
        .setKey("actions")
        .setFlexGrow(2);
        super.addColumn(new ComponentRenderer<>(scheduledProcessAggregateConfiguration -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setMargin(false);
            layout.setPadding(false);
            layout.setSpacing(false);

            FlowState flowState = FlowStateCache.instance().get(this.agent
                , scheduledProcessAggregateConfiguration.getJobName());

            if(flowState == null || flowState.getState() == State.UNKNOWN_STATE) {
                Icon unknown = VaadinIcon.QUESTION.create();
                unknown.setSize("14pt");
                unknown.getStyle().set("color", "rgba(210, 215, 211, 1)");
                unknown.getElement().setAttribute("title", "Unknown");
                layout.add(unknown);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, unknown);
            }
            else if(flowState.getState() == State.RUNNING_STATE) {
                Icon running = VaadinIcon.CHECK.create();
                running.setSize("14pt");
                running.getStyle().set("color", "#66bb6a");
                running.getElement().setAttribute("title", "Running");
                layout.add(running);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, running);
            }
            else if(flowState.getState() == State.STOPPED_STATE) {
                Icon stopped = VaadinIcon.STOP.create();
                stopped.setSize("14pt");
                stopped.getStyle().set("color", "#000000");
                stopped.getElement().setAttribute("title", "Stopped");
                layout.add(stopped);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, stopped);
            }
            else if(flowState.getState() == State.RECOVERING_STATE) {
                Icon recovering = VaadinIcon.RECYCLE.create();
                recovering.setSize("14pt");
                recovering.getStyle().set("color", "rgba(241, 90, 35, 1.0)");
                recovering.getElement().setAttribute("title", "Recovering");
                layout.add(recovering);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, recovering);
            }
            else if(flowState.getState() == State.STOPPED_IN_ERROR_STATE) {
                Icon stoppedInError = VaadinIcon.EXCLAMATION.create();
                stoppedInError.setSize("14pt");
                stoppedInError.getStyle().set("color", "#ef5350");
                stoppedInError.getElement().setAttribute("title", "Stopped in error");
                layout.add(stoppedInError);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, stoppedInError);
            }
            else if(flowState.getState() == State.PAUSED_STATE) {
                Icon paused = VaadinIcon.PAUSE.create();
                paused.setSize("14pt");
                paused.getStyle().set("color", "rgba(133,181,225,1.0)");
                paused.getElement().setAttribute("title", "Paused");
                layout.add(paused);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, paused);
            }

            layout.setSizeFull();
            return layout;
        }))
        .setHeader("Status")
        .setKey("status")
        .setWidth("20px");

        super.init();
    }


    @Override
    public void addGridFiltering(DatePicker date, TimePicker startTime, TimePicker endTime, Consumer<Long> startTimeFilter, Consumer<Long> endTimeFilter) {
        // not required make abstract parent
    }

    @Override
    protected ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> getResults(AgentJobFilter agentJobFilter, int offset, int limit) {
        return this.scheduledProcessManagementService.getScheduleProcessAggregateConfigurations(this.agent.getName(), agentJobFilter.getFilter());
    }


    public void deleteScheduledJobFlow(ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        // Get the module configuration from the module.
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration
            = this.configurationRestService.getModuleConfiguration(this.agent.getUrl());

        if(moduleConfiguration == null) {
            throw new RuntimeException(String.format("Could not find module configuration for agent[%s]", agent));
        }

        // Get the flowDefinitions from the configuration metadata.
        moduleConfiguration.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
            .findFirst().ifPresentOrElse(flowDefinitions -> {
            // Add the new job flow to the map.
            Map<String, String> configurationMap = (Map<String, String>)flowDefinitions.getValue();
            configurationMap.remove(scheduleProcessAggregateConfiguration.getJobName());
            flowDefinitions.setValue(configurationMap);

            // update the configuration back onto the module.
            this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConfiguration);
        }, () -> {throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));});

        // Delete all the configurations.
        this.configurationRestService.delete(this.agent.getUrl(), getConfigurationIdFlowComponent
            (agent, scheduleProcessAggregateConfiguration.getJobName(), ScheduledProcessConstants.SCHEDULED_CONSUMER));
        this.configurationRestService.delete(this.agent.getUrl(),  getConfigurationIdFlowComponent
            (agent, scheduleProcessAggregateConfiguration.getJobName(), ScheduledProcessConstants.PROCESS_EXECUTION_BROKER));
        this.configurationRestService.delete(this.agent.getUrl(),  getConfigurationIdFlowComponent
            (agent, scheduleProcessAggregateConfiguration.getJobName(), ScheduledProcessConstants.BLACKOUT_ROUTER));

        // We need to deactivate and activate the module so the new flow is initialised
        boolean deactivateSuccess = this.moduleControlRestService.changeModuleActivationState(this.agent.getUrl(), this.agent.getName(), "deactivate");
        if(!deactivateSuccess) {
            throw new RuntimeException(String.format("Could not deactivate agent[%s]", agent));
        }
        boolean activateSuccess = this.moduleControlRestService.changeModuleActivationState(this.agent.getUrl(), this.agent.getName(), "activate");
        if(!activateSuccess) {
            throw new RuntimeException(String.format("Could not activate agent[%s]", agent));
        }
    }

    private String getConfigurationIdFlowComponent(ModuleMetaData agent, String flow, String component) {
        Optional<ModuleMetaData> moduleMetaData = this.metaDataRestService.getModuleMetadata(agent.getUrl(), agent.getName());

        AtomicReference<String> configurationMetaData = new AtomicReference<>();

        moduleMetaData.ifPresentOrElse(metaData -> {
            metaData.getFlows().stream()
                .filter(flowMetaData -> flowMetaData.getName().equals(flow))
                .findFirst().get()
                .getFlowElements().stream()
                .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals(component))
                .findFirst().ifPresentOrElse(id -> configurationMetaData.set(id.getConfigurationId())
                , () -> {throw new RuntimeException(String.format("Could not get configuration iid for agent[%s], flow[%s], component[%s]!"
                    , agent.getName(), flow, component));});
        },() -> {
            throw new RuntimeException(String.format("Could not load module metadata for agent[%s] at url[%s]!", agent.getName(), agent.getUrl()));
        });


        return configurationMetaData.get();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {

        this.flowStateBroadcasterRegistration = FlowStateBroadcaster.register(flowState -> {
            ui.access(() -> {
                this.dataProvider.refreshAll();
                this.filteredDataProvider.refreshAll();
            });
        });

        this.cacheStateBroadcasterRegistration = CacheStateBroadcaster.register(flowState -> {
            ui.access(() -> {
                this.dataProvider.refreshAll();
                this.filteredDataProvider.refreshAll();
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.flowStateBroadcasterRegistration != null) {
            this.flowStateBroadcasterRegistration.remove();
            this.flowStateBroadcasterRegistration = null;
        }

        if(this.cacheStateBroadcasterRegistration != null) {
            this.cacheStateBroadcasterRegistration.remove();
            this.cacheStateBroadcasterRegistration = null;
        }
    }
}
