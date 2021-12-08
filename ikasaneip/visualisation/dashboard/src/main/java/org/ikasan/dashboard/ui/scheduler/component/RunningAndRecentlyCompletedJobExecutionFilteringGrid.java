package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.scheduled.event.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.solr.BatchInsertEvent;
import org.ikasan.spec.solr.BatchInsertListener;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


public class RunningAndRecentlyCompletedJobExecutionFilteringGrid extends FilteringGrid<ScheduledProcessEvent, ScheduledProcessFilter, ScheduledProcessEventSearchResults<ScheduledProcessEvent>> implements BatchInsertListener<ScheduledProcessEvent> {

    private ScheduledProcessManagementService scheduledProcessManagementService;

    private DateFormatter dateFormatter;

    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    private HashMap<String, List<BusinessStreamMetaData>> agentJobBusinessStreams;

    private UI ui;
    private IkasanAuthentication authentication;

    private SystemEventLogger systemEventLogger;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param searchFilter
     */
    public RunningAndRecentlyCompletedJobExecutionFilteringGrid(ScheduledProcessManagementService scheduledProcessManagementService, ScheduledProcessFilter searchFilter,
                                                                DateFormatter dateFormatter, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                                MetaDataService metaDataRestService, ModuleMetaDataService moduleMetaDataService, SystemEventLogger systemEventLogger) {
        super(searchFilter);
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.ui = UI.getCurrent();
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.systemEventLogger = systemEventLogger;

        this.initGrid();
    }

    /**
     * Helper method to initialise the grid.
     */
    private void initGrid() {
        Checkbox errorCb = new Checkbox("Errors");
        addGridFiltering(errorCb, super.searchFilter::setErrorsOnly);

        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.agentHostname]]</div>")
            .withProperty("agentHostname", ScheduledProcessEvent::getAgentHostname))
            .setHeader(getTranslation("table-header.scheduled-agent-host-name", UI.getCurrent().getLocale()))
            .setKey("agentHostname")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.schedulerName]]</div>")
            .withProperty("schedulerName", ScheduledProcessEvent::getAgentName))
            .setHeader(getTranslation("table-header.scheduled-agent-name", UI.getCurrent().getLocale()))
            .setKey("schedulerName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.jobName]]</div>")
            .withProperty("jobName", ScheduledProcessEvent::getJobName))
            .setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setKey("jobName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.jobGroup]]</div>")
            .withProperty("jobGroup", ScheduledProcessEvent::getJobGroup))
            .setHeader(getTranslation("table-header.job-group", UI.getCurrent().getLocale()))
            .setKey("jobGroup")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", ScheduledProcessEvent::getJobDescription))
            .setHeader(getTranslation("table-header.job-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(5);
        super.addColumn(new ComponentRenderer<>(jobExecution -> {
            HorizontalLayout layout = new HorizontalLayout();

            if(!this.agentJobBusinessStreams.containsKey(jobExecution.getAgentName()+"."+jobExecution.getJobName())) {
                this.agentJobBusinessStreams.put(jobExecution.getAgentName()+"."+jobExecution.getJobName(), this.scheduledProcessManagementService.getBusinessStreams(
                    jobExecution.getAgentName(), jobExecution.getJobName()));
            }

            List<BusinessStreamMetaData> businessStreamMetaDataList
                = this.agentJobBusinessStreams.get(jobExecution.getAgentName()+"."+jobExecution.getJobName());

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
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.executionTime]]</div>")
            .withProperty("executionTime", scheduledProcessEvent -> this.dateFormatter.getFormattedDate(scheduledProcessEvent.getFireTime())))
            .setHeader("Execution Time")
            .setKey("executionTime")
            .setWidth("130px");
        super.addColumn(new ComponentRenderer<>(scheduledProcessEvent->
        {
            HorizontalLayout layout = new HorizontalLayout();

            Icon jobExecutionDetails = VaadinIcon.RANDOM.create();
            jobExecutionDetails.setSize("14pt");
            jobExecutionDetails.getStyle().set("cursor", "pointer");
            jobExecutionDetails.getElement().setAttribute("title", getTranslation("tooltip.job-execution-details", UI.getCurrent().getLocale()));

            jobExecutionDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ModuleMetaData agent = this.moduleMetaDataService.findById(scheduledProcessEvent.getAgentName());
                ScheduledProcessExecutionDialog scheduledProcessExecutionDialog = new ScheduledProcessExecutionDialog(scheduledProcessEvent, agent);
                scheduledProcessExecutionDialog.open();
            });

            layout.add(jobExecutionDetails);

            Icon jobDetails = VaadinIcon.CLIPBOARD_TEXT.create();
            jobDetails.setSize("14pt");
            jobDetails.getStyle().set("cursor", "pointer");
            jobDetails.getElement().setAttribute("title", getTranslation("tooltip.job-configuration", UI.getCurrent().getLocale()));

            layout.add(jobDetails);

            jobDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ScheduledProcessAggregateConfiguration configuration = this.scheduledProcessManagementService.getScheduleProcessAggregateConfiguration(scheduledProcessEvent.getAgentName(),
                    scheduledProcessEvent.getJobName());

                ModuleMetaData agent = this.moduleMetaDataService.findById(scheduledProcessEvent.getAgentName());

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
                    this.moduleMetaDataService.findById(scheduledProcessEvent.getAgentName()), scheduledProcessEvent.getJobName());

                scheduledJobStatisticsDialog.open();
            });

            layout.add(chart);

            layout.setSizeFull();
            return layout;
        }))
        .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
        .setKey("actions")
        .setWidth("70px");
        super.addColumn(new ComponentRenderer<>(scheduledProcessEvent->
        {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setMargin(false);
            layout.setPadding(false);

            if(scheduledProcessEvent.isSuccessful()) {
                Icon check = VaadinIcon.CHECK.create();
                check.getStyle().set("color", "#66bb6a");
                check.getStyle().set("font-size", "32pt");
                check.getElement().setAttribute("title", getTranslation("tooltip.job-successful", UI.getCurrent().getLocale()));
                layout.add(check);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, check);
            }
            else {
                Icon exclamation = VaadinIcon.EXCLAMATION.create();
                exclamation.getStyle().set("color", "#ef5350");
                exclamation.getStyle().set("font-size", "32pt");
                layout.add(exclamation);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, exclamation);
                exclamation.getElement().setAttribute("title", getTranslation("tooltip.job-failed", UI.getCurrent().getLocale()));
            }

            layout.setSizeFull();
            return layout;
        }))
        .setHeader(errorCb)
        .setKey("executionStatus")
        .setWidth("40px");

        this.getColumns().forEach(column -> column.setClassNameGenerator(item -> {
            if(item.isSuccessful()) return "running";

            return "stoppedInError";
        }));

        super.init();
    }

    /**
     * Add time and date filtering to the grid.
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

            startTimeFilter.accept(epochMilli
                + (startTime.getValue().toSecondOfDay()*1000));


            endTimeFilter.accept(epochMilli
                + (endTime.getValue().toSecondOfDay()*1000));


            filteredDataProvider.refreshAll();
        });

        startTime.addValueChangeListener(ev->{
            long epochMilli = date.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000;

            startTimeFilter.accept(epochMilli
                + (startTime.getValue().toSecondOfDay()*1000));


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
     * Add ability to filter on errors.
     *
     * @param errors
     * @param errorFilter
     */
    public void addGridFiltering(Checkbox errors, Consumer<Boolean> errorFilter)
    {
        errors.addValueChangeListener(ev-> {
            errorFilter.accept(ev.getValue());
            filteredDataProvider.refreshAll();
        });
    }

    @Override
    protected ScheduledProcessEventSearchResults<ScheduledProcessEvent> getResults(ScheduledProcessFilter scheduledProcessFilter, int offset, int limit, String sortField, String sortOrder) {
        agentJobBusinessStreams = new HashMap<>();

        if(this.authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY) || this.authentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN)) {
            return this.scheduledProcessManagementService.getScheduledProcessEvents(null,
                scheduledProcessFilter.getStartTime(), scheduledProcessFilter.getEndTime(), scheduledProcessFilter.getAgentName(),
                scheduledProcessFilter.isErrorsOnly(), offset, limit, "desc");
        }
        else {
            return this.scheduledProcessManagementService.getScheduledProcessEvents(new ArrayList<>(SecurityUtils.getAccessibleModules(this.authentication)),
                scheduledProcessFilter.getStartTime(), scheduledProcessFilter.getEndTime(), scheduledProcessFilter.getAgentName(),
                scheduledProcessFilter.isErrorsOnly(), offset, limit, "desc");
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.scheduledProcessManagementService.addBatchInsertListener(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.scheduledProcessManagementService.removeBatchInsertListener(this);
    }

    @Override
    public void onBatchInsert(BatchInsertEvent<ScheduledProcessEvent> batchInsertEvent) {
        ui.access(() -> super.refresh());
    }
}
