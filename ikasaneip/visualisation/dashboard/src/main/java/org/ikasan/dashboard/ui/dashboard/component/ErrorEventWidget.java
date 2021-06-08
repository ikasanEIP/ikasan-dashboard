package org.ikasan.dashboard.ui.dashboard.component;


import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.Command;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.solr.SolrGeneralService;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ErrorEventWidget extends Div {

    private static int REPORTING_INTERVAL = 60000;

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    private DataSeries series;

    public ErrorEventWidget(SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService) {
        this.solrGeneralService = solrGeneralService;

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("325px");

        final Chart chart = new Chart();
        chart.setClassName("live-errors");

        final Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);
        configuration.getTitle().setText("Error Occurrences");

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(50);

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle("Count"));

        configuration.getTooltip().setEnabled(true);
        configuration.getLegend().setEnabled(false);

        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<span>{series.name}</span>: <b>{point.y}</b><br/>");
        tooltip.setValueDecimals(0);
        tooltip.setShared(false);
        configuration.setTooltip(tooltip);

        series = new DataSeries();
        series.setPlotOptions(new PlotOptionsSpline());
        series.setName("Error Occurrences");
        for (int i = -19; i <= 0; i++) {
            long x = System.currentTimeMillis() + TimeZone.getTimeZone(DateTimeUtil.getZoneOffset()).getRawOffset() + i * REPORTING_INTERVAL;
            series.add(new DataSeriesItem(x, this.loadData("error", x-REPORTING_INTERVAL, x)
                .getTotalNumberOfResults()));
        }

        chart.getConfiguration().setSeries(series);

        chart.setHeight("260px");
        chart.setWidth("95%");
        chart.getStyle().set("position", "absolute");
        chart.getStyle().set("top", "50px");
        chart.getStyle().set("right", "10px");
        chart.getStyle().set("left", "10px");


        Button refreshButton = new Button();
        refreshButton.getStyle().set("position", "absolute");
        refreshButton.getStyle().set("top", "10px");
        refreshButton.getStyle().set("right", "10px");

        refreshButton.addClickListener(buttonClickEvent -> {
            this.refresh();
        });
        refreshButton.getElement().appendChild(IronIcons.REFRESH.create().getElement());

        div.add(refreshButton, chart);

        this.add(div);
    }

    private void refresh() {
        for (int i = -19; i <= 0; i++) {
            long x = System.currentTimeMillis() + TimeZone.getTimeZone(DateTimeUtil.getZoneOffset()).getRawOffset() + i * REPORTING_INTERVAL;
            series.get(i+19).setX(x);
            series.get(i+19).setY(this.loadData("error", x-REPORTING_INTERVAL, x).getTotalNumberOfResults());
            series.update(series.get(i+19));
        }
    }

    private IkasanSolrDocumentSearchResults loadData(String type, long startTime, long endTime) {
        return this.solrGeneralService.search(new HashSet<String>(), new HashSet<String>(), null,
            startTime, endTime, 0, List.of(type),false, null, null);
    }

    /**
     * Runs given task repeatedly until the reference component is attached
     *
     * @param component
     * @param task
     * @param interval
     * @param initialPause
     *            a timeout after tas is started
     */
    public static void runWhileAttached(Component component, Command task,
                                        final int interval, final int initialPause) {
        component.addAttachListener(event -> {
            ScheduledExecutorService executor = Executors
                .newScheduledThreadPool(1);

            component.getUI().ifPresent(ui -> ui.setPollInterval(interval));

            final ScheduledFuture<?> scheduledFuture = executor
                .scheduleAtFixedRate(() -> {
                    component.getUI().ifPresent(ui -> ui.access(task));
                }, initialPause, interval, TimeUnit.MILLISECONDS);

            component.addDetachListener(detach -> {
                scheduledFuture.cancel(true);
                detach.getUI().setPollInterval(-1);
            });
        });
    }


}
