package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
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
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.solr.BatchInsertEvent;
import org.ikasan.spec.solr.BatchInsertListener;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


public class UpcomingJobExecutionFilteringGrid extends FilteringGrid<UpcomingScheduledProcess, ScheduledProcessFilter, ScheduledProcessEventSearchResults<UpcomingScheduledProcess>> implements BatchInsertListener<ScheduledProcessEvent> {

    private Registration flowStateBroadcasterRegistration;
    private Registration cacheStateBroadcasterRegistration;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private DateFormatter dateFormatter;

    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    private HashMap<String, List<BusinessStreamMetaData>> agentJobBusinessStreams;
    private HashMap<String, ModuleMetaData> agents;

    private UI ui;
    private IkasanAuthentication authentication;

    private SystemEventLogger systemEventLogger;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param searchFilter
     */
    public UpcomingJobExecutionFilteringGrid(ScheduledProcessManagementService scheduledProcessManagementService, ScheduledProcessFilter searchFilter,
                                             DateFormatter dateFormatter, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                             MetaDataService metaDataRestService, ModuleMetaDataService moduleMetaDataService, SystemEventLogger systemEventLogger) {
        super(searchFilter);
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.systemEventLogger = systemEventLogger;

        this.ui = UI.getCurrent();
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.initGrid();
    }

    /**
     * Create the upcoming jobs grid
     */
    private void initGrid() {
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.agentHostname]]</div>")
            .withProperty("agentHostname", UpcomingScheduledProcess::getAgentHostname))
            .setHeader(getTranslation("table-header.scheduled-agent-host-name", UI.getCurrent().getLocale()))
            .setKey("agentHostname")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.schedulerName]]</div>")
            .withProperty("schedulerName", UpcomingScheduledProcess::getAgentName))
            .setHeader(getTranslation("table-header.scheduled-agent-name", UI.getCurrent().getLocale()))
            .setKey("schedulerName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.jobName]]</div>")
            .withProperty("jobName", UpcomingScheduledProcess::getJobName))
            .setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setKey("jobName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.jobGroup]]</div>")
            .withProperty("jobGroup", UpcomingScheduledProcess::getJobGroup))
            .setHeader(getTranslation("table-header.job-group", UI.getCurrent().getLocale()))
            .setKey("jobGroup")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", UpcomingScheduledProcess::getJobDescription))
            .setHeader(getTranslation("table-header.job-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(5);
        super.addColumn(new ComponentRenderer<>(upcomingScheduledProcess -> {
            HorizontalLayout layout = new HorizontalLayout();

            if(!this.agentJobBusinessStreams.containsKey(upcomingScheduledProcess.getAgentName()+"."+upcomingScheduledProcess.getJobName())) {
                this.agentJobBusinessStreams.put(upcomingScheduledProcess.getAgentName()+"."+upcomingScheduledProcess.getJobName(), this.scheduledProcessManagementService.getBusinessStreams(
                    upcomingScheduledProcess.getAgentName(), upcomingScheduledProcess.getJobName()));
            }

            List<BusinessStreamMetaData> businessStreamMetaDataList
                = this.agentJobBusinessStreams.get(upcomingScheduledProcess.getAgentName()+"."+upcomingScheduledProcess.getJobName());

            businessStreamMetaDataList.forEach(businessStreamMetaData -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.BUSINESS_STREAM.name() + ":" + businessStreamMetaData.getName());
                Anchor link = new Anchor(route, businessStreamMetaData.getName());
                link.setTarget("_blank");
                layout.add(link);
                link.getStyle().set("color", "blue");
            });

            return layout;
        }))
            .setHeader(getTranslation("table-header.related-business-streams", UI.getCurrent().getLocale()))
            .setKey("businessStreams")
            .setFlexGrow(5);
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.nextExecutionTime]]</div>")
            .withProperty("nextExecutionTime", upcomingScheduledProcess -> this.dateFormatter.getFormattedDate(upcomingScheduledProcess.getFireTime())) )
            .setHeader(getTranslation("table-header.next-job-execution-time", UI.getCurrent().getLocale()))
            .setKey("nextExecutionTime")
            .setWidth("130px");
        super.addColumn(new ComponentRenderer<>(upcomingScheduledProcess->
        {
            HorizontalLayout layout = new HorizontalLayout();

            Icon jobExecutionDetails = VaadinIcon.RANDOM.create();
            jobExecutionDetails.setSize("14pt");
            jobExecutionDetails.getStyle().set("cursor", "pointer");
            jobExecutionDetails.getElement().setAttribute("title", getTranslation("tooltip.job-execution-details", UI.getCurrent().getLocale()));

            jobExecutionDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    ModuleMetaData agent = this.moduleMetaDataService.findById(upcomingScheduledProcess.getAgentName());
                    UpcomingJobExecutionDialog upcomingJobExecutionDialog = new UpcomingJobExecutionDialog(upcomingScheduledProcess, agent);
                    upcomingJobExecutionDialog.open();
            });

            layout.add(jobExecutionDetails);

            Icon jobDetails = VaadinIcon.CLIPBOARD_TEXT.create();
            jobDetails.setSize("14pt");
            jobDetails.getStyle().set("cursor", "pointer");
            jobDetails.getElement().setAttribute("title", getTranslation("tooltip.job-configuration", UI.getCurrent().getLocale()));

            layout.add(jobDetails);

            jobDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    ScheduledProcessAggregateConfiguration configuration = this.scheduledProcessManagementService.getScheduleProcessAggregateConfiguration(upcomingScheduledProcess.getAgentName(),
                        upcomingScheduledProcess.getJobName());

                    ModuleMetaData agent = this.moduleMetaDataService.findById(upcomingScheduledProcess.getAgentName());

                    ScheduledJobDialog scheduledJobDialog = new ScheduledJobDialog(agent,
                        this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                        this.metaDataRestService, this.systemEventLogger);

                    scheduledJobDialog.setScheduleProcessAggregateConfiguration(configuration, EditMode.READONLY);
                    scheduledJobDialog.open();
            });

            Icon chart = VaadinIcon.CHART.create();
            chart.setSize("14pt");
            chart.getStyle().set("cursor", "pointer");
            chart.getElement().setAttribute("title", getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()));

            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ScheduledJobStatisticsDialog scheduledJobStatisticsDialog = new ScheduledJobStatisticsDialog(this.scheduledProcessManagementService,
                    this.moduleMetaDataService.findById(upcomingScheduledProcess.getAgentName()), upcomingScheduledProcess.getJobName());

                scheduledJobStatisticsDialog.open();
            });

            layout.add(chart);

            layout.setSizeFull();
            return layout;
        }))
        .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
        .setKey("actions")
        .setWidth("70px");
        super.addColumn(new ComponentRenderer<>(upcomingScheduledProcess-> {
            VerticalLayout layout = new VerticalLayout();
            layout.setMargin(false);
            layout.setPadding(false);
            layout.setSpacing(false);

            if(!this.agents.containsKey(upcomingScheduledProcess.getAgentName())) {
                this.agents.put(upcomingScheduledProcess.getAgentName(), moduleMetaDataService.findById(upcomingScheduledProcess.getAgentName()));
            }

            FlowState flowState = FlowStateCache.instance().get(this.agents.get(upcomingScheduledProcess.getAgentName())
                , upcomingScheduledProcess.getJobName());

            if(flowState == null || flowState.getState() == State.UNKNOWN_STATE) {
                Icon unknown = VaadinIcon.QUESTION.create();
                unknown.setSize("14pt");
                unknown.getStyle().set("color", "rgba(210, 215, 211, 1)");
                unknown.getElement().setAttribute("title", getTranslation("status-label.unknown", UI.getCurrent().getLocale()));
                layout.add(unknown);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, unknown);
            }
            else if(flowState.getState() == State.RUNNING_STATE) {
                Icon running = VaadinIcon.CHECK.create();
                running.setSize("14pt");
                running.getStyle().set("color", "#66bb6a");
                running.getElement().setAttribute("title", getTranslation("status-label.running", UI.getCurrent().getLocale()));
                layout.add(running);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, running);
            }
            else if(flowState.getState() == State.STOPPED_STATE) {
                Icon stopped = VaadinIcon.STOP.create();
                stopped.setSize("14pt");
                stopped.getStyle().set("color", "#000000");
                stopped.getElement().setAttribute("title", getTranslation("status-label.stopped", UI.getCurrent().getLocale()));
                layout.add(stopped);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, stopped);
            }
            else if(flowState.getState() == State.RECOVERING_STATE) {
                Icon recovering = VaadinIcon.RECYCLE.create();
                recovering.setSize("14pt");
                recovering.getStyle().set("color", "rgba(241, 90, 35, 1.0)");
                recovering.getElement().setAttribute("title", getTranslation("status-label.recovering", UI.getCurrent().getLocale()));
                layout.add(recovering);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, recovering);
            }
            else if(flowState.getState() == State.STOPPED_IN_ERROR_STATE) {
                Icon stoppedInError = VaadinIcon.EXCLAMATION.create();
                stoppedInError.setSize("14pt");
                stoppedInError.getStyle().set("color", "#ef5350");
                stoppedInError.getElement().setAttribute("title", getTranslation("status-label.stopped-in-error", UI.getCurrent().getLocale()));
                layout.add(stoppedInError);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, stoppedInError);
            }
            else if(flowState.getState() == State.PAUSED_STATE) {
                Icon paused = VaadinIcon.PAUSE.create();
                paused.setSize("14pt");
                paused.getStyle().set("color", "rgba(133,181,225,1.0)");
                paused.getElement().setAttribute("title", getTranslation("status-label.paused", UI.getCurrent().getLocale()));
                layout.add(paused);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, paused);
            }

            layout.setSizeFull();
            return layout;
        }))
        .setHeader(getTranslation("table-header.status", UI.getCurrent().getLocale()))
        .setKey("status")
        .setWidth("30px");
        super.init();
    }

    /**
     * Add filtering to the grid.
     *
     * @param date
     * @param startTime
     * @param endTime
     * @param startTimeFilter
     * @param endTimeFilter
     */
    public void addGridFiltering(DatePicker date, TimePicker startTime, TimePicker endTime, Consumer<Long> startTimeFilter, Consumer<Long> endTimeFilter)
    {
        date.addValueChangeListener(ev->{
            long epochMilli = date.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000;
            if((epochMilli
                + (startTime.getValue().toSecondOfDay()*1000)) < System.currentTimeMillis()) {
                startTimeFilter.accept(System.currentTimeMillis());
            }
            else {
                startTimeFilter.accept(epochMilli
                    + (startTime.getValue().toSecondOfDay()*1000));
            }

            endTimeFilter.accept(epochMilli
                + (endTime.getValue().toSecondOfDay()*1000));



            filteredDataProvider.refreshAll();
        });

        startTime.addValueChangeListener(ev->{
            long epochMilli = date.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000;
            if((epochMilli
                + (startTime.getValue().toSecondOfDay()*1000)) < System.currentTimeMillis()) {
                startTimeFilter.accept(System.currentTimeMillis());
            }
            else {
                startTimeFilter.accept(epochMilli
                    + (startTime.getValue().toSecondOfDay()*1000));
            }

            filteredDataProvider.refreshAll();
        });

        endTime.addValueChangeListener(ev->{
            long epochMilli = date.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000;
            endTimeFilter.accept(epochMilli
                + (endTime.getValue().toSecondOfDay()*1000));

            filteredDataProvider.refreshAll();
        });
    }

    /**
     * Add filtering to the grid.
     *
     * @param agentSelect
     * @param jobSelect
     * @param agentFilter
     * @param jobFilter
     */
    public void addGridFiltering(Select<String> agentSelect, Select<FlowMetaData> jobSelect, Consumer<String> agentFilter, Consumer<String> jobFilter)
    {
        agentSelect.addValueChangeListener(ev->{
            agentFilter.accept(ev.getValue());
            filteredDataProvider.refreshAll();
        });

        jobSelect.addValueChangeListener(ev -> {
            if(ev.getValue() != null) {
                jobFilter.accept(ev.getValue().getName());
                filteredDataProvider.refreshAll();
            }
        });
    }


    @Override
    protected ScheduledProcessEventSearchResults<UpcomingScheduledProcess> getResults(ScheduledProcessFilter scheduledProcessFilter, int offset, int limit) {
        this.agentJobBusinessStreams = new HashMap<>();
        this.agents = new HashMap<>();

        return this.scheduledProcessManagementService.getUpComingScheduledProcesses(scheduledProcessFilter.getAgentName(), scheduledProcessFilter.getJobName(), scheduledProcessFilter.getStartTime()
                , scheduledProcessFilter.getEndTime(), offset, limit);
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

        this.scheduledProcessManagementService.addBatchInsertListener(this);
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
        this.scheduledProcessManagementService.removeBatchInsertListener(this);
    }

    @Override
    public void onBatchInsert(BatchInsertEvent<ScheduledProcessEvent> batchInsertEvent) {
        ui.access(() -> super.refresh());
    }
}
