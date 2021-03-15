package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.events.PointClickEvent;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.solr.SolrGeneralService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

public class HospitalEventsWidget extends Div implements BeforeEnterObserver {

    private static long MILLI_IN_DAY = 1000 * 60 * 60 * 24;

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    private Chart chart;

    public HospitalEventsWidget(SolrGeneralService solrGeneralService) {
        this.solrGeneralService = solrGeneralService;

        init();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.init();
    }

    private void init() {
        this.removeAll();
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("275px");

        chart = new Chart();
        chart.setClassName("hospital-events");

        Configuration configuration = chart.getConfiguration();

        configuration.setTitle("Hospital Events vs Actioned Hospital Events");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle("Number of Hospital Events");

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);

        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<span>{series.name}</span>: <b>{point.y}</b><br/>");
        tooltip.setValueDecimals(2);
        tooltip.setShared(true);
        configuration.setTooltip(tooltip);

        long midnightToday = this.getMidnightTodayMilliseconds();

        List<DayExclusion> dayExclusions = this.loadAllData(midnightToday);

        final DataSeries series = new DataSeries();
        series.setName("Exclusions");
        series.add(new DataSeriesItem(midnightToday - (8*MILLI_IN_DAY), dayExclusions.get(8).exclusions));
        series.add(new DataSeriesItem(midnightToday - (7*MILLI_IN_DAY), dayExclusions.get(7).exclusions));
        series.add(new DataSeriesItem(midnightToday - (6*MILLI_IN_DAY), dayExclusions.get(6).exclusions));
        series.add(new DataSeriesItem(midnightToday - (5*MILLI_IN_DAY), dayExclusions.get(5).exclusions));
        series.add(new DataSeriesItem(midnightToday - (4*MILLI_IN_DAY), dayExclusions.get(4).exclusions));
        series.add(new DataSeriesItem(midnightToday - (3*MILLI_IN_DAY), dayExclusions.get(3).exclusions));
        series.add(new DataSeriesItem(midnightToday - (2*MILLI_IN_DAY), dayExclusions.get(2).exclusions));
        series.add(new DataSeriesItem(midnightToday - (1*MILLI_IN_DAY), dayExclusions.get(1).exclusions));
        series.add(new DataSeriesItem(midnightToday - (0*MILLI_IN_DAY), dayExclusions.get(0).exclusions));

        configuration.addSeries(series);

        final DataSeries actionedSeries = new DataSeries();
        actionedSeries.setName("Actioned Exclusions");
        actionedSeries.add(new DataSeriesItem(midnightToday - (8*MILLI_IN_DAY), dayExclusions.get(8).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (7*MILLI_IN_DAY), dayExclusions.get(7).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (6*MILLI_IN_DAY), dayExclusions.get(6).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (5*MILLI_IN_DAY), dayExclusions.get(5).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (4*MILLI_IN_DAY), dayExclusions.get(4).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (3*MILLI_IN_DAY), dayExclusions.get(3).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (2*MILLI_IN_DAY), dayExclusions.get(2).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (1*MILLI_IN_DAY), dayExclusions.get(1).actionedExclusions));
        actionedSeries.add(new DataSeriesItem(midnightToday - (0*MILLI_IN_DAY), dayExclusions.get(0).actionedExclusions));

        configuration.addSeries(actionedSeries);

        chart.setHeight("260px");
        chart.setWidthFull();

        chart.addPointClickListener((ComponentEventListener<PointClickEvent>) pointClickEvent -> {
            pointClickEvent.getCategory();
        });

        div.add(chart);

        this.add(div);
    }

    private List<DayExclusion> loadAllData(long midnightToday) {
        ArrayList<DayExclusion> dayExclusions = new ArrayList<>();

        IntStream.range(0,9)
            .forEach(i -> {
            DayExclusion dayExclusion = new DayExclusion();
            dayExclusion.actionedExclusions = loadData("exclusionEventAction",midnightToday - (i*MILLI_IN_DAY),
                midnightToday - ((i-1)*MILLI_IN_DAY)).getTotalNumberOfResults();
            dayExclusion.exclusions = dayExclusion.actionedExclusions + loadData("exclusion",midnightToday - (i*MILLI_IN_DAY),
                midnightToday - ((i-1)*MILLI_IN_DAY)).getTotalNumberOfResults();

            dayExclusions.add(dayExclusion);
        });

        return dayExclusions;
    }

    private IkasanSolrDocumentSearchResults loadData(String type, long startTime, long endTime) {
        return this.solrGeneralService.search(new HashSet<String>(), new HashSet<String>(), null,
             startTime, endTime, 0, List.of(type),false, null, null);
    }

    private long getMidnightTodayMilliseconds() {
        LocalDateTime zdt = LocalDate.now().atTime(LocalTime.MIDNIGHT);

        return zdt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private class DayExclusion {
        long exclusions = 0;
        long actionedExclusions = 0;
    }
}
