package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.scheduler.model.JobExecution;
import org.ikasan.dashboard.ui.scheduler.model.UpcomingJobExecutionFilter;
import org.ikasan.dashboard.ui.scheduler.model.UpcomingJobExecutionSearchResults;
import org.ikasan.dashboard.ui.scheduler.service.JobExecutionService;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;


public class RunningAndRecentlyCompletedJobExecutionFilteringGrid extends FilteringGrid<JobExecution, UpcomingJobExecutionFilter, UpcomingJobExecutionSearchResults> {

    private JobExecutionService jobExecutionService;

    /**
     * Constructors
     *
     * @param jobExecutionService
     * @param searchFilter
     */
    public RunningAndRecentlyCompletedJobExecutionFilteringGrid(JobExecutionService jobExecutionService, UpcomingJobExecutionFilter searchFilter) {
        super(searchFilter);
        this.jobExecutionService = jobExecutionService;

        this.initGrid();
    }

    private void initGrid() {
        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.schedulerName]]</div>")
            .withProperty("schedulerName", JobExecution::getSchedulerName))
            .setHeader("Scheduler Name")
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
        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.executionTime]]</div>")
            .withProperty("executionTime", jobExecution -> jobExecution.getExecutionTime()))
            .setHeader("Execution Time")
            .setKey("executionTime")
            .setFlexGrow(3);
        super.addColumn(TemplateRenderer.<JobExecution>of("<div style='white-space:normal'>[[item.executionStatus]]</div>")
            .withProperty("executionStatus", JobExecution::getExecutionStatus))
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

    @Override
    protected UpcomingJobExecutionSearchResults getResults(UpcomingJobExecutionFilter upcomingJobExecutionFilter, int offset, int limit) {
        return this.jobExecutionService.getJobExecutions();
    }
}
