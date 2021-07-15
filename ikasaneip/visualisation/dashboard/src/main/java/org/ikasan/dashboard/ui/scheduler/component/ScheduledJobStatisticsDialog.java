package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

public class ScheduledJobStatisticsDialog extends AbstractCloseableResizableDialog {

    // Fields to capture schedule job properties.
    private TextField agentNameTf;
    private TextField agentUrlLf;
    private TextField jobNameTf;
    private TextField jobGroupTf;
    private TextArea jobDescriptionTa;

    private TextField numSucceessTf;
    private TextField numFailureTf;
    private TextField averageExecutionTimeTf;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ModuleMetaData agent;
    private String jobName;

    private int numSuccess = 0;
    private int numFail = 0;
    private long averageExecutionTime = 0;

    public ScheduledJobStatisticsDialog(ScheduledProcessManagementService scheduledProcessManagementService,
                                        ModuleMetaData agent, String jobName) {

        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.agent = agent;
        this.jobName = jobName;
        super.showResize(false);
        super.title.setText("Scheduled Job Statistics");

        this.setHeight("850px");
        this.setWidth("95%");

        FormLayout jobLayout = new FormLayout();
        this.agentNameTf = new TextField("Agent name");
        this.agentNameTf.setValue(this.agent.getName());
        this.agentNameTf.setEnabled(false);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        this.agentUrlLf = new TextField("Agent URL");
        this.agentUrlLf.setPrefixComponent(link);
        this.agentUrlLf.setValue(" ");

        jobLayout.add(this.agentNameTf, this.agentUrlLf);

        ScheduledProcessAggregateConfiguration aggregateConfiguration
            = this.scheduledProcessManagementService.getScheduleProcessAggregateConfiguration(this.agent.getName(), jobName);

        this.jobNameTf = new TextField("Job name");
        this.jobNameTf.setValue(this.jobName);
        this.jobNameTf.setEnabled(false);

        this.jobGroupTf = new TextField("Job Group");
        this.jobGroupTf.setValue(aggregateConfiguration.getJobName());
        this.jobGroupTf.setEnabled(false);

        jobLayout.add(this.jobNameTf, this.jobGroupTf);

        this.jobDescriptionTa = new TextArea("Job description");
        this.jobDescriptionTa.setHeight("100px");
        this.jobDescriptionTa.setValue(aggregateConfiguration.getJobDescription());
        this.jobDescriptionTa.setEnabled(false);

        jobLayout.add(this.jobDescriptionTa, 2);

        Div durationChartDiv = new Div();
        durationChartDiv.add(generateDurationChart());
        durationChartDiv.setSizeFull();

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        this.numSucceessTf = new TextField("Number of successful executions");
        this.numSucceessTf.setWidth("30vw");
        Anchor successLink = new Anchor(this.buildSuccessRoute(false).getHref(),String.valueOf(this.numSuccess));
        successLink.setTarget("_blank");
        successLink.getStyle().set("color", "blue");
        this.numSucceessTf.setPrefixComponent(successLink);
        this.numSucceessTf.setEnabled(true);
        this.numSucceessTf.setValue(" ");
        this.numFailureTf = new TextField("Number of failed executions");
        this.numFailureTf.setWidth("30vw");
        this.numFailureTf.setValue(Integer.toString(this.numFail));
        Anchor failureLink = new Anchor(this.buildSuccessRoute(true).getHref(),String.valueOf(this.numFail));
        failureLink.setTarget("_blank");
        failureLink.getStyle().set("color", "blue");
        this.numFailureTf.setPrefixComponent(failureLink);
        this.numFailureTf.setEnabled(true);
        this.numFailureTf.setValue(" ");
        this.averageExecutionTimeTf = new TextField("Average execution time milliseconds");
        this.averageExecutionTimeTf.setWidth("30vw");
        this.averageExecutionTimeTf.setValue(Long.toString(this.averageExecutionTime));
        this.averageExecutionTimeTf.setEnabled(false);

        horizontalLayout.add(this.numSucceessTf, this.numFailureTf, this.averageExecutionTimeTf);
        horizontalLayout.setWidthFull();

        jobLayout.add(horizontalLayout, 2);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(jobLayout, durationChartDiv);
        super.content.add(layout);
    }

    private Chart generateDurationChart(){
        final Chart chart = new Chart();
        chart.setClassName("ikasan-charts");
        chart.setTimeline(true);

        Configuration configuration = chart.getConfiguration();
        configuration.getTitle().setText("Execution Duration Trend");

        YAxis yAxis = new YAxis();
        Labels label = new Labels();
        label.setFormatter("function() { return this.value + ' milliseconds'; }");
        label.setAlign(HorizontalAlign.RIGHT);
        yAxis.setLabels(label);

        PlotLine plotLine = new PlotLine();
        plotLine.setValue(2);
        yAxis.setPlotLines(plotLine);
        configuration.addyAxis(yAxis);

        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<span>{series.name}</span>: Execution duration <b>{point.y}</b> milliseconds<br/>");
        tooltip.setValueDecimals(2);
        configuration.setTooltip(tooltip);

        int limit = 1000;

        ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
            = this.scheduledProcessManagementService.getScheduledProcessEvents(0, System.currentTimeMillis(), this.agent.getName()
            , false, 0, limit);
        DataSeries dataSeries = new DataSeries();
        dataSeries.setName(this.jobName);

        this.populateDataSeries(scheduledProcessEventSearchResults, dataSeries);

        while(scheduledProcessEventSearchResults.getTotalNumberOfResults() > limit) {
            limit += 1000;

            scheduledProcessEventSearchResults
                = this.scheduledProcessManagementService.getScheduledProcessEvents(0, System.currentTimeMillis(), this.agent.getName()
                , false, limit - 1000, limit);

            this.populateDataSeries(scheduledProcessEventSearchResults, dataSeries);
        }

        this.averageExecutionTime = this.averageExecutionTime/dataSeries.size();

        PlotOptionsLine lineOptions = new PlotOptionsLine();
        lineOptions.setColorIndex(3);
        dataSeries.setPlotOptions(lineOptions);

        configuration.setSeries(dataSeries);

        XAxis xaxis = new XAxis();
        xaxis.setTitle("Execution date/time");
        xaxis.setType(AxisType.DATETIME);
        configuration.addxAxis(xaxis);

        RangeSelector rangeSelector = new RangeSelector();
        rangeSelector.setSelected(4);
        configuration.setRangeSelector(rangeSelector);

        return chart;
    }

    private void populateDataSeries(ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults, DataSeries dataSeries) {
        scheduledProcessEventSearchResults.getResultList().stream()
            .filter(scheduledProcessEvent -> this.jobName.equals(scheduledProcessEvent.getJobName()))
            .forEach(scheduledProcessEvent -> {
                DataSeriesItem item = new DataSeriesItem();
                item.setX(Instant.ofEpochMilli(scheduledProcessEvent.getFireTime() + TimeZone.getTimeZone(DateTimeUtil.getZoneOffset()).getRawOffset()));
                item.setY(scheduledProcessEvent.getCompletionTime() - scheduledProcessEvent.getFireTime());
                dataSeries.add(item);

                if(scheduledProcessEvent.isSuccessful()) {
                    this.numSuccess++;
                }
                else {
                    this.numFail++;
                }

                this.averageExecutionTime += (scheduledProcessEvent.getCompletionTime() - scheduledProcessEvent.getFireTime());
            });
    }

    private RouterLink buildSuccessRoute(boolean errors){

        RouteParameters routeParameters  = new RouteParameters(new RouteParam("startTime", "0"),
            new RouteParam("endTime", String.valueOf(System.currentTimeMillis())),
            new RouteParam("agentName", this.agent.getName()),
            new RouteParam("jobName", this.jobName),
            new RouteParam("errorsOnly", String.valueOf(errors)));

        return new RouterLink(null, RunningAndRecentlyCompletedJobExecutionDeepLinkView.class, routeParameters);
    }
}
