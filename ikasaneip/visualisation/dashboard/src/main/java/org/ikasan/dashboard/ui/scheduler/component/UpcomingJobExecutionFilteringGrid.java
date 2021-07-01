package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;

import java.util.function.Consumer;


public class UpcomingJobExecutionFilteringGrid extends FilteringGrid<UpcomingScheduledProcess, ScheduledProcessFilter, ScheduledProcessEventSearchResults<UpcomingScheduledProcess>> {

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private DateFormatter dateFormatter;

    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    /**
     * Constructors
     *
     * @param scheduledProcessManagementService
     * @param searchFilter
     */
    public UpcomingJobExecutionFilteringGrid(ScheduledProcessManagementService scheduledProcessManagementService, ScheduledProcessFilter searchFilter,
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
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.schedulerName]]</div>")
            .withProperty("schedulerName", UpcomingScheduledProcess::getAgentName))
            .setHeader("Agent Name")
            .setKey("schedulerName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.jobName]]</div>")
            .withProperty("jobName", UpcomingScheduledProcess::getJobName))
            .setHeader("Job Name")
            .setKey("jobName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", UpcomingScheduledProcess::getJobDescription))
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
        super.addColumn(TemplateRenderer.<UpcomingScheduledProcess>of("<div style='white-space:normal'>[[item.nextExecutionTime]]</div>")
            .withProperty("nextExecutionTime", upcomingScheduledProcess -> this.dateFormatter.getFormattedDate(upcomingScheduledProcess.getFireTime())) )
            .setHeader("Next Execution Time")
            .setKey("nextExecutionTime")
            .setFlexGrow(3);
//        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.schedulerStatus]]</div>")
//            .withProperty("schedulerStatus", JobExecution::getSchedulerStatus))
//            .setHeader("Scheduler Status")
//            .setKey("schedulerStatus")
//            .setFlexGrow(1);
        super.addColumn(new ComponentRenderer<>(jobExecution -> {
            VerticalLayout layout = new VerticalLayout();

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
//        super.addColumn(new ComponentRenderer<>(businessStreamMetaData->
//        {
//            Button editButton = new TableButton(VaadinIcon.EDIT.create());
//            editButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
//            {
//                SchedulerConfigurationDialog schedulerConfigurationDialog = new SchedulerConfigurationDialog();
//                schedulerConfigurationDialog.open();
//            });
////
////            ComponentSecurityVisibility.applySecurity(editButton, SecurityConstants.PLATORM_CONFIGURATON_ADMIN,
////                SecurityConstants.PLATORM_CONFIGURATON_WRITE, SecurityConstants.ALL_AUTHORITY);
//
//            VerticalLayout layout = new VerticalLayout();
//            layout.setSizeFull();
//            layout.add(editButton);
//            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, editButton);
//            return layout;
//        })).setWidth("30px");
//        super.addColumn(new ComponentRenderer<>(businessStreamMetaData->
//        {
//            Button downloadButton = new TableButton(VaadinIcon.DOWNLOAD.create());
////            StreamResource streamResource = new StreamResource(businessStreamMetaData.getName().concat(".json")
////                , () -> new ByteArrayInputStream(businessStreamMetaData.getJson().getBytes()));
////
////            FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(streamResource);
////            buttonWrapper.wrapComponent(downloadButton);
//
//            VerticalLayout layout = new VerticalLayout();
//            layout.setSizeFull();
//            layout.add(downloadButton);
//            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, downloadButton);
//            return layout;
//        })).setWidth("30px");
//        super.addColumn(new ComponentRenderer<>(businessStreamMetaData->
//        {
//            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
////            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
////            {
////                this.businessStreamMetaDataService.delete(businessStreamMetaData.getId());
////                this.populateBusinessStreamGrid();
////            });
////
////            ComponentSecurityVisibility.applySecurity(deleteButton, SecurityConstants.PLATORM_CONFIGURATON_ADMIN,
////                SecurityConstants.PLATORM_CONFIGURATON_WRITE, SecurityConstants.ALL_AUTHORITY);
//
//            VerticalLayout layout = new VerticalLayout();
//            layout.setSizeFull();
//            layout.add(deleteButton);
//            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
//            return layout;
//        })).setWidth("30px");

        super.init();
    }

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

    @Override
    protected ScheduledProcessEventSearchResults<UpcomingScheduledProcess> getResults(ScheduledProcessFilter scheduledProcessFilter, int offset, int limit) {
        ScheduledProcessEventSearchResults<UpcomingScheduledProcess> results =  this.scheduledProcessManagementService.getUpComingScheduledProcesses(scheduledProcessFilter.getStartTime()
            , scheduledProcessFilter.getEndTime());

        return new ScheduledProcessEventSearchResults(offset+limit > results.getResultList().size() ?results.getResultList().subList(offset, results.getResultList().size()):results.getResultList().subList(offset, offset+limit)
            , results.getTotalNumberOfResults(), results.getQueryResponseTime());
    }
}
