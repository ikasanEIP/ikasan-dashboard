package org.ikasan.dashboard.ui.home.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.events.PointClickEvent;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.flow.component.html.Div;

public class HospitalEventsWidget extends Div {

    public HospitalEventsWidget() {
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("275px");

        final Chart chart = new Chart();

        Configuration configuration = chart.getConfiguration();

        configuration.setTitle("Hospital Events vs Actioned Hospital Events");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle("Number of Hospital Events");

        Legend legend = configuration.getLegend();
        legend.setLayout(LayoutDirection.VERTICAL);
        legend.setVerticalAlign(VerticalAlign.MIDDLE);
        legend.setAlign(HorizontalAlign.RIGHT);

        PlotOptionsSeries plotOptionsSeries = new PlotOptionsSeries();
        plotOptionsSeries.setPointStart(2010);
        configuration.setPlotOptions(plotOptionsSeries);


        configuration.addSeries(new ListSeries("Hospital Events", 27, 21, 44, 12, 34, 47, 34, 12));
        configuration.addSeries(new ListSeries("Actioned Hospital Events", 15, 20, 38, 12, 11, 46, 21, 11));

        chart.setHeight("260px");
        chart.setWidthFull();

        chart.addPointClickListener((ComponentEventListener<PointClickEvent>) pointClickEvent -> {
           pointClickEvent.getCategory();
        });

        div.add(chart);

        this.add(div);
    }

}
