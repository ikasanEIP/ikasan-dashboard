package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.GeneratedVaadinDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.general.component.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.general.component.TableButton;
import org.ikasan.dashboard.ui.scheduler.model.JobExecution;
import org.ikasan.dashboard.ui.scheduler.model.UpcomingJobExecutionFilter;
import org.ikasan.dashboard.ui.scheduler.model.UpcomingJobExecutionSearchResults;
import org.ikasan.dashboard.ui.scheduler.service.JobExecutionService;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamUploadDialog;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;


public class UpcomingJobExecutionFilteringGrid extends FilteringGrid<JobExecution, UpcomingJobExecutionFilter, UpcomingJobExecutionSearchResults> {

    private JobExecutionService jobExecutionService;

    /**
     * Constructors
     *
     * @param jobExecutionService
     * @param searchFilter
     */
    public UpcomingJobExecutionFilteringGrid(JobExecutionService jobExecutionService, UpcomingJobExecutionFilter searchFilter) {
        super(searchFilter);
        this.jobExecutionService = jobExecutionService;

        this.initGrid();
    }

    private void initGrid() {
        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.schedulerName]]</div>")
            .withProperty("schedulerName", JobExecution::getSchedulerName))
            .setHeader("Agent Name")
            .setKey("schedulerName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.jobName]]</div>")
            .withProperty("jobName", JobExecution::getJobName))
            .setHeader("Job Name")
            .setKey("jobName")
            .setFlexGrow(1);
        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", JobExecution::getDescription))
            .setHeader("Job Description")
            .setKey("description")
            .setFlexGrow(5);
        super.addColumn(new ComponentRenderer<>(jobExecution -> {
            HorizontalLayout layout = new HorizontalLayout();

            jobExecution.getRelatedBusinessStreams().forEach(businessStreamMetaData -> {
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
            .setFlexGrow(5);
        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.nextExecutionTime]]</div>")
            .withProperty("nextExecutionTime", jobExecution -> jobExecution.getNextExecution()) )
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
        super.addColumn(new ComponentRenderer<>(businessStreamMetaData->
        {
            Button editButton = new TableButton(VaadinIcon.EDIT.create());
            editButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                SchedulerConfigurationDialog schedulerConfigurationDialog = new SchedulerConfigurationDialog();
                schedulerConfigurationDialog.open();
            });
//
//            ComponentSecurityVisibility.applySecurity(editButton, SecurityConstants.PLATORM_CONFIGURATON_ADMIN,
//                SecurityConstants.PLATORM_CONFIGURATON_WRITE, SecurityConstants.ALL_AUTHORITY);

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(editButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, editButton);
            return layout;
        })).setWidth("30px");
        super.addColumn(new ComponentRenderer<>(businessStreamMetaData->
        {
            Button downloadButton = new TableButton(VaadinIcon.DOWNLOAD.create());
//            StreamResource streamResource = new StreamResource(businessStreamMetaData.getName().concat(".json")
//                , () -> new ByteArrayInputStream(businessStreamMetaData.getJson().getBytes()));
//
//            FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(streamResource);
//            buttonWrapper.wrapComponent(downloadButton);

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(downloadButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, downloadButton);
            return layout;
        })).setWidth("30px");
        super.addColumn(new ComponentRenderer<>(businessStreamMetaData->
        {
            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
//            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
//            {
//                this.businessStreamMetaDataService.delete(businessStreamMetaData.getId());
//                this.populateBusinessStreamGrid();
//            });
//
//            ComponentSecurityVisibility.applySecurity(deleteButton, SecurityConstants.PLATORM_CONFIGURATON_ADMIN,
//                SecurityConstants.PLATORM_CONFIGURATON_WRITE, SecurityConstants.ALL_AUTHORITY);

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setWidth("30px");

        super.init();
    }

    @Override
    protected UpcomingJobExecutionSearchResults getResults(UpcomingJobExecutionFilter upcomingJobExecutionFilter, int offset, int limit) {
        return this.jobExecutionService.getJobExecutions();
    }
}
