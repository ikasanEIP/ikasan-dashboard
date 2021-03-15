package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.html.Div;

import java.util.Random;

public class SystemEventWidget extends Div {

    public SystemEventWidget() {
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("275px");

        final Random random = new Random();

        final Chart chart = new Chart();

        final Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);
        configuration.getTitle().setText("Live random data");

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(150);

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle("Value"));

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

        div.add(chart);

        this.add(div);
    }


}
