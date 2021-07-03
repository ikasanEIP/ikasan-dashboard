package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.function.Consumer;


public class RunningAndRecentlyCompletedJobExecutionFilteringGrid extends FilteringGrid<ScheduledProcessEvent, ScheduledProcessFilter, ScheduledProcessEventSearchResults<ScheduledProcessEvent>> {

    private ScheduledProcessManagementService scheduledProcessManagementService;

    private DateFormatter dateFormatter;

    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param searchFilter
     */
    public RunningAndRecentlyCompletedJobExecutionFilteringGrid(ScheduledProcessManagementService scheduledProcessManagementService, ScheduledProcessFilter searchFilter,
                                                                DateFormatter dateFormatter, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                                MetaDataService metaDataRestService, ModuleMetaDataService moduleMetaDataService) {
        super(searchFilter);
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;

        this.initGrid();
    }

    private void initGrid() {
        Checkbox errorCb = new Checkbox("Errors");
        addGridFiltering(errorCb, super.searchFilter::setErrorsOnly);

        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.schedulerName]]</div>")
            .withProperty("schedulerName", ScheduledProcessEvent::getAgentName))
            .setHeader("Scheduler Name")
            .setKey("schedulerName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.jobName]]</div>")
            .withProperty("jobName", ScheduledProcessEvent::getJobName))
            .setHeader("Job Name")
            .setKey("jobName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.jobGroup]]</div>")
            .withProperty("jobGroup", ScheduledProcessEvent::getJobGroup))
            .setHeader("Job Group")
            .setKey("jobGroup")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", ScheduledProcessEvent::getJobDescription))
            .setHeader("Job Description")
            .setKey("description")
            .setFlexGrow(5);
        super.addColumn(new ComponentRenderer<>(jobExecution -> {
            HorizontalLayout layout = new HorizontalLayout();

//            jobExecution.getRelatedBusinessStreams().forEach(businessStreamMetaData -> {
//                String route = RouteConfiguration.forSessionScope()
//                    .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.BUSINESS_STREAM.name() + ":" + businessStreamMetaData.getName());
//                Anchor link = new Anchor(route, businessStreamMetaData.getName());
//                link.setTarget("_blank");
//                layout.add(link);
//                link.getStyle().set("color", "blue");
//            });

            return layout;
        }))
            .setHeader("Related Business Streams")
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
            jobExecutionDetails.getElement().setAttribute("title", "Job execution details");

            jobExecutionDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(iconClickEvent.getClickCount() == 2) {
                    ModuleMetaData agent = this.moduleMetaDataService.findById(scheduledProcessEvent.getAgentName());
                    ScheduledProcessExecutionDialog scheduledProcessExecutionDialog = new ScheduledProcessExecutionDialog(scheduledProcessEvent, agent);
                    scheduledProcessExecutionDialog.open();
                }
            });

            layout.add(jobExecutionDetails);

            Icon jobDetails = VaadinIcon.CLIPBOARD_TEXT.create();
            jobDetails.setSize("14pt");
            jobDetails.getStyle().set("cursor", "pointer");
            jobDetails.getElement().setAttribute("title", "Job configuration");

            layout.add(jobDetails);

            jobDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(iconClickEvent.getClickCount() == 2) {
                    ScheduledProcessAggregateConfiguration configuration = this.scheduledProcessManagementService.getScheduleProcessAggregateConfiguration(scheduledProcessEvent.getAgentName(),
                        scheduledProcessEvent.getJobName());

                    ModuleMetaData agent = this.moduleMetaDataService.findById(scheduledProcessEvent.getAgentName());

                    ScheduledJobDialog scheduledJobDialog = new ScheduledJobDialog(agent,
                        this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                        this.metaDataRestService);

                    scheduledJobDialog.setScheduleProcessAggregateConfiguration(configuration, EditMode.READONLY);
                    scheduledJobDialog.open();
                }
            });

            Icon chart = VaadinIcon.CHART.create();
            chart.setSize("14pt");
            chart.getStyle().set("cursor", "pointer");
            chart.getElement().setAttribute("title", "Job statistics");

            layout.add(chart);

            layout.setSizeFull();
            return layout;
        }))
        .setHeader("Actions")
        .setKey("actions")
        .setWidth("70px");
        super.addColumn(new ComponentRenderer<>(scheduledProcessEvent->
        {
            VerticalLayout layout = new VerticalLayout();
            if(scheduledProcessEvent.isSuccessful()) {
                Icon check = VaadinIcon.CHECK.create();
                check.getStyle().set("color", "#66bb6a");
                check.getStyle().set("font-size", "32pt");
                check.getElement().setAttribute("title", "Successful");
                layout.add(check);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, check);
            }
            else {
                Icon exclamation = VaadinIcon.EXCLAMATION.create();
                exclamation.getStyle().set("color", "#ef5350");
                exclamation.getStyle().set("font-size", "32pt");
                layout.add(exclamation);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, exclamation);
                exclamation.getElement().setAttribute("title", "Job failed!");
            }

            layout.setSizeFull();
            return layout;
        }))
        .setHeader(errorCb)
        .setKey("executionStatus")
        .setWidth("40px");

        super.init();
    }

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

    public void addGridFiltering(Checkbox errors, Consumer<Boolean> errorFilter)
    {
        errors.addValueChangeListener(ev-> {
            errorFilter.accept(ev.getValue());
            filteredDataProvider.refreshAll();
        });
    }

    @Override
    protected ScheduledProcessEventSearchResults<ScheduledProcessEvent> getResults(ScheduledProcessFilter scheduledProcessFilter, int offset, int limit) {
        ScheduledProcessEventSearchResults<ScheduledProcessEvent> results =  this.scheduledProcessManagementService.getScheduledProcessEvents(scheduledProcessFilter.getStartTime()
            , scheduledProcessFilter.getEndTime(), scheduledProcessFilter.getFilter(), scheduledProcessFilter.isErrorsOnly());

        return new ScheduledProcessEventSearchResults(offset+limit > results.getResultList().size() ?results.getResultList().subList(offset, results.getResultList().size()):results.getResultList().subList(offset, offset+limit)
            , results.getTotalNumberOfResults(), results.getQueryResponseTime());
    }
}
