package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;


@CssImport("./styles/dashboard-view.css")
public class DurationWidget extends Div {

    public DurationWidget() {

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("380px");

        div.add(greatDurationChart());

        this.add(div);
    }

    private Chart greatDurationChart(){
        Chart chart = new Chart();
        Configuration configuration = chart.getConfiguration();

        configuration.setTitle("Execution Duration Trend");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle("Execution Time Milliseconds");

        Legend legend = configuration.getLegend();
        legend.setLayout(LayoutDirection.VERTICAL);
        legend.setVerticalAlign(VerticalAlign.MIDDLE);
        legend.setAlign(HorizontalAlign.RIGHT);

        PlotOptionsSeries plotOptionsSeries = new PlotOptionsSeries();
        plotOptionsSeries.setPointStart(2010);
        configuration.setPlotOptions(plotOptionsSeries);

        configuration.addSeries(new ListSeries("Agent 1", 43934, 52503, 57177, 69658, 97031, 119931, 137133, 154175));
        configuration.addSeries(new ListSeries("Agent 2", 24916, 24064, 29742, 29851, 32490, 30282, 38121, 40434));
        configuration.addSeries(new ListSeries("Agent 3", 11744, 17722, 16005, 19771, 20185, 24377, 32147, 39387));
        configuration.addSeries(new ListSeries("Agent 4", null, null, 7988, 12169, 15112, 22452, 34400, 34227));
        configuration.addSeries(new ListSeries("Agent 5", 12908, 5948, 8105, 11248, 8989, 11816, 18274, 18111));


        return chart;
    }

}
