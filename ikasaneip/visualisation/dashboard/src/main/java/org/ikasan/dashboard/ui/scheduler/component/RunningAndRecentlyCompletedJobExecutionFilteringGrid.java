package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.service.SolrScheduledProcessServiceImpl;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.function.Consumer;


public class RunningAndRecentlyCompletedJobExecutionFilteringGrid extends FilteringGrid<ScheduledProcessEvent, ScheduledProcessFilter, ScheduledProcessEventSearchResults<ScheduledProcessEvent>> {

    private ScheduledProcessManagementService scheduledProcessManagementService;

    private DateFormatter dateFormatter;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param searchFilter
     */
    public RunningAndRecentlyCompletedJobExecutionFilteringGrid(ScheduledProcessManagementService scheduledProcessManagementService, ScheduledProcessFilter searchFilter,
                                                                DateFormatter dateFormatter) {
        super(searchFilter);
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;

        this.initGrid();
    }

    private void initGrid() {
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
            .setFlexGrow(3);
        super.addColumn(TemplateRenderer.<ScheduledProcessEvent>of("<div style='white-space:normal'>[[item.executionStatus]]</div>")
            .withProperty("executionStatus", scheduledProcessEvent -> this.dateFormatter.getFormattedDate(scheduledProcessEvent.getFireTime())))
            .setHeader("Execution Status")
            .setKey("executionStatus")
            .setFlexGrow(1);
        super.addColumn(new ComponentRenderer<>(jobExecution -> {
            HorizontalLayout layout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.BUSINESS_STREAM.name() + ":blah");
            Anchor link = new Anchor(route, "view");
            link.setTarget("_blank");
            layout.add(link);
            link.getStyle().set("color", "blue");

            return layout;
        }))
            .setHeader("Scheduler Statistics")
            .setKey("schedulerStatistics")
            .setFlexGrow(1);

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

    @Override
    protected ScheduledProcessEventSearchResults<ScheduledProcessEvent> getResults(ScheduledProcessFilter scheduledProcessFilter, int offset, int limit) {
        ScheduledProcessEventSearchResults<ScheduledProcessEvent> results =  this.scheduledProcessManagementService.getScheduledProcessEvents(scheduledProcessFilter.getStartTime(), scheduledProcessFilter.getEndTime());

        return new ScheduledProcessEventSearchResults(offset+limit > results.getResultList().size() ?results.getResultList().subList(offset, results.getResultList().size()):results.getResultList().subList(offset, offset+limit)
            , results.getTotalNumberOfResults(), results.getQueryResponseTime());
    }
}
