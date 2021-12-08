package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.event.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;

import java.time.Instant;
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

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param agent
     * @param jobName
     */
    public ScheduledJobStatisticsDialog(ScheduledProcessManagementService scheduledProcessManagementService,
                                        ModuleMetaData agent, String jobName) {

        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.agent = agent;
        this.jobName = jobName;
        super.showResize(false);
        super.title.setText(getTranslation("header.scheduled-job-statistics", UI.getCurrent().getLocale()));

        this.setHeight("850px");
        this.setWidth("95%");

        FormLayout jobLayout = new FormLayout();
        this.agentNameTf = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentNameTf.setValue(this.agent.getName());
        this.agentNameTf.setEnabled(false);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        this.agentUrlLf = new TextField(getTranslation("label.agent-url", UI.getCurrent().getLocale()));
        this.agentUrlLf.setPrefixComponent(link);
        this.agentUrlLf.setValue(" ");

        jobLayout.add(this.agentNameTf, this.agentUrlLf);

        ScheduledProcessAggregateConfiguration aggregateConfiguration
            = this.scheduledProcessManagementService.getScheduleProcessAggregateConfiguration(this.agent.getName(), jobName);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setValue(this.jobName);
        this.jobNameTf.setEnabled(false);

        this.jobGroupTf = new TextField(getTranslation("label.job-group", UI.getCurrent().getLocale()));
        this.jobGroupTf.setValue(aggregateConfiguration.getJobGroup());
        this.jobGroupTf.setEnabled(false);

        jobLayout.add(this.jobNameTf, this.jobGroupTf);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setHeight("100px");
        this.jobDescriptionTa.setValue(aggregateConfiguration.getJobDescription());
        this.jobDescriptionTa.setEnabled(false);

        jobLayout.add(this.jobDescriptionTa, 2);

        Div durationChartDiv = new Div();
        durationChartDiv.add(generateDurationChart());
        durationChartDiv.setSizeFull();

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        this.numSucceessTf = new TextField(getTranslation("label.number-of-successful-executions", UI.getCurrent().getLocale()));
        this.numSucceessTf.setWidth("30vw");
        this.numSucceessTf.setEnabled(true);
        this.numSucceessTf.setValue(Integer.toString(this.numSuccess));
        this.numSucceessTf.setEnabled(false);

        this.numFailureTf = new TextField(getTranslation("label.number-of-failed-executions", UI.getCurrent().getLocale()));
        this.numFailureTf.setWidth("30vw");
        this.numFailureTf.setValue(Integer.toString(this.numFail));
        this.numFailureTf.setEnabled(false);
        this.numFailureTf.setValue(String.valueOf(this.numFail));
        this.averageExecutionTimeTf = new TextField(getTranslation("label.average-execution-time-millis", UI.getCurrent().getLocale()));
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

    /**
     * Helper method to create the scheduled job duration chart.
     *
     * @return
     */
    private Chart generateDurationChart(){
        final Chart chart = new Chart();
        chart.setClassName("ikasan-charts");
        chart.setTimeline(true);

        Configuration configuration = chart.getConfiguration();
        configuration.getTitle().setText(getTranslation("label.execution-duration-trend", UI.getCurrent().getLocale()));

        YAxis yAxis = new YAxis();
        Labels label = new Labels();
        label.setFormatter("function() { return this.value + ' " + getTranslation("label.milliseconds", UI.getCurrent().getLocale()) + "'; }");
        label.setAlign(HorizontalAlign.RIGHT);
        yAxis.setLabels(label);

        PlotLine plotLine = new PlotLine();
        plotLine.setValue(2);
        yAxis.setPlotLines(plotLine);
        configuration.addyAxis(yAxis);

        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<span>{series.name}</span>: Execution duration <b>{point.y}</b> "
            + getTranslation("label.milliseconds", UI.getCurrent().getLocale()) + "<br/>");
        tooltip.setValueDecimals(2);
        configuration.setTooltip(tooltip);

        int limit = 1000;

        ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
            = this.scheduledProcessManagementService.getScheduledProcessEvents(this.agent.getName(), null, this.jobName, 0, System.currentTimeMillis()
                , 0, limit, "asc");
        DataSeries dataSeries = new DataSeries();
        dataSeries.setName(this.jobName);

        this.populateDataSeries(scheduledProcessEventSearchResults, dataSeries);

        while(scheduledProcessEventSearchResults.getTotalNumberOfResults() > limit) {
            limit += 1000;

            scheduledProcessEventSearchResults
                = this.scheduledProcessManagementService.getScheduledProcessEvents(this.agent.getName(), null, this.jobName, 0, System.currentTimeMillis()
                    , 0, limit, "asc");

            this.populateDataSeries(scheduledProcessEventSearchResults, dataSeries);
        }

        this.averageExecutionTime = this.averageExecutionTime/dataSeries.size();

        PlotOptionsLine lineOptions = new PlotOptionsLine();
        lineOptions.setColorIndex(3);
        dataSeries.setPlotOptions(lineOptions);

        configuration.setSeries(dataSeries);

        XAxis xaxis = new XAxis();
        xaxis.setTitle(getTranslation("label.execution-date-time", UI.getCurrent().getLocale()));
        xaxis.setType(AxisType.DATETIME);
        configuration.addxAxis(xaxis);

        RangeSelector rangeSelector = new RangeSelector();
        rangeSelector.setSelected(4);
        configuration.setRangeSelector(rangeSelector);

        return chart;
    }

    /**
     * Populate the chart data series.
     *
     * @param scheduledProcessEventSearchResults
     * @param dataSeries
     */
    private void populateDataSeries(ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults, DataSeries dataSeries) {
        scheduledProcessEventSearchResults.getResultList()
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
}
