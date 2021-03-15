package org.ikasan.dashboard.ui.dashboard.component;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.Command;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.solr.SolrGeneralService;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SystemEventWidget extends Div {

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    public SystemEventWidget(SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService) {
        this.solrGeneralService = solrGeneralService;

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("275px");

        final Random random = new Random();

        final Chart chart = new Chart();
        chart.setClassName("live-errors");

        final Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);
        configuration.getTitle().setText("Live Error Occurrences");

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(50);

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle("Count"));

        configuration.getTooltip().setEnabled(false);
        configuration.getLegend().setEnabled(false);

        final DataSeries series = new DataSeries();
        series.setPlotOptions(new PlotOptionsSpline());
        series.setName("Random data");
        for (int i = -19; i <= 0; i++) {
            series.add(new DataSeriesItem(System.currentTimeMillis() + i * 1000, random.nextDouble()));
        }

        chart.setHeight("260px");
        chart.setWidthFull();

        configuration.setSeries(series);

        runWhileAttached(chart, () -> {
            final long x = System.currentTimeMillis();
            series.add(new DataSeriesItem(x, this.loadData("error", x-5000, x)
                .getTotalNumberOfResults()), true, true);
        }, 5000, 1000);

        div.add(chart);

        this.add(div);
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
