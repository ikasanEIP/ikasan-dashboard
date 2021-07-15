package org.ikasan.dashboard.ui.dashboard.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.events.PointClickEvent;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.solr.SolrGeneralService;

import java.time.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

public class HospitalEventsWidget extends Div implements BeforeEnterObserver {

    private static long MILLI_IN_DAY = 1000 * 60 * 60 * 24;

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    private Chart chart;
    private DataSeries exclusionSeries;
    private DataSeries actionedSeries;

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
        div.setHeight("325px");

        chart = new Chart();
        chart.setClassName("ikasan-charts");

        Configuration configuration = chart.getConfiguration();

        configuration.setTitle("Hospital Events vs Actioned Hospital Events");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle("Number of Hospital Events");

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);

        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat("<span>{series.name}</span>: <b>{point.y}</b><br/>");
        tooltip.setValueDecimals(0);
        tooltip.setShared(true);
        configuration.setTooltip(tooltip);

        long midnightToday = this.getMidnightTodayMillisecondsWithZOneOffset();

        List<DayExclusion> dayExclusions = this.loadAllData(this.getMidnightTodayMillisecondsUTC());

        exclusionSeries = new DataSeries();
        exclusionSeries.setName("Exclusions");
        exclusionSeries.add(new DataSeriesItem(midnightToday - (8*MILLI_IN_DAY), dayExclusions.get(8).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (7*MILLI_IN_DAY), dayExclusions.get(7).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (6*MILLI_IN_DAY), dayExclusions.get(6).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (5*MILLI_IN_DAY), dayExclusions.get(5).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (4*MILLI_IN_DAY), dayExclusions.get(4).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (3*MILLI_IN_DAY), dayExclusions.get(3).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (2*MILLI_IN_DAY), dayExclusions.get(2).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (1*MILLI_IN_DAY), dayExclusions.get(1).exclusions));
        exclusionSeries.add(new DataSeriesItem(midnightToday - (0*MILLI_IN_DAY), dayExclusions.get(0).exclusions));

        actionedSeries = new DataSeries();
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

        chart.getConfiguration().setSeries(exclusionSeries, actionedSeries);

        chart.setHeight("260px");
        chart.setWidth("95%");
        chart.getStyle().set("position", "absolute");
        chart.getStyle().set("top", "50px");
        chart.getStyle().set("right", "10px");
        chart.getStyle().set("left", "10px");


        chart.addPointClickListener((ComponentEventListener<PointClickEvent>) pointClickEvent -> {
            String category = pointClickEvent.getCategory();
            pointClickEvent.getSeriesItemIndex();
        });

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

    private void refresh(){
        long midnightToday = this.getMidnightTodayMillisecondsWithZOneOffset();

        List<DayExclusion> dayExclusions = this.loadAllData(getMidnightTodayMillisecondsUTC());

        exclusionSeries.get(0).setY(dayExclusions.get(8).exclusions);
        exclusionSeries.get(0).setX(midnightToday - (8*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(0));
        exclusionSeries.get(1).setY(dayExclusions.get(7).exclusions);
        exclusionSeries.get(1).setX(midnightToday - (7*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(1));
        exclusionSeries.get(2).setY(dayExclusions.get(6).exclusions);
        exclusionSeries.get(2).setX(midnightToday - (6*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(2));
        exclusionSeries.get(3).setY(dayExclusions.get(5).exclusions);
        exclusionSeries.get(3).setX(midnightToday - (5*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(3));
        exclusionSeries.get(4).setY(dayExclusions.get(4).exclusions);
        exclusionSeries.get(4).setX(midnightToday - (4*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(4));
        exclusionSeries.get(5).setY(dayExclusions.get(3).exclusions);
        exclusionSeries.get(5).setX(midnightToday - (3*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(5));
        exclusionSeries.get(6).setY(dayExclusions.get(2).exclusions);
        exclusionSeries.get(6).setX(midnightToday - (2*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(6));
        exclusionSeries.get(7).setY(dayExclusions.get(1).exclusions);
        exclusionSeries.get(7).setX(midnightToday - (1*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(7));
        exclusionSeries.get(8).setY(dayExclusions.get(0).exclusions);
        exclusionSeries.get(8).setX(midnightToday - (0*MILLI_IN_DAY));
        exclusionSeries.update(exclusionSeries.get(8));

        actionedSeries.get(0).setY(dayExclusions.get(8).actionedExclusions);
        actionedSeries.get(0).setX(midnightToday - (8*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(0));
        actionedSeries.get(1).setY(dayExclusions.get(7).actionedExclusions);
        actionedSeries.get(1).setX(midnightToday - (7*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(1));
        actionedSeries.get(2).setY(dayExclusions.get(6).actionedExclusions);
        actionedSeries.get(2).setX(midnightToday - (6*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(2));
        actionedSeries.get(3).setY(dayExclusions.get(5).actionedExclusions);
        actionedSeries.get(3).setX(midnightToday - (5*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(3));
        actionedSeries.get(4).setY(dayExclusions.get(4).actionedExclusions);
        actionedSeries.get(4).setX(midnightToday - (4*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(4));
        actionedSeries.get(5).setY(dayExclusions.get(3).actionedExclusions);
        actionedSeries.get(5).setX(midnightToday - (3*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(5));
        actionedSeries.get(6).setY(dayExclusions.get(2).actionedExclusions);
        actionedSeries.get(6).setX(midnightToday - (2*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(6));
        actionedSeries.get(7).setY(dayExclusions.get(1).actionedExclusions);
        actionedSeries.get(7).setX(midnightToday - (1*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(7));
        actionedSeries.get(8).setY(dayExclusions.get(0).actionedExclusions);
        actionedSeries.get(8).setX(midnightToday - (0*MILLI_IN_DAY));
        actionedSeries.update(actionedSeries.get(8));
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

    private long getMidnightTodayMillisecondsWithZOneOffset() {
        return Instant.now().atZone(DateTimeUtil.getZoneId())
            .toLocalDate().atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.ofTotalSeconds(0))
            .toEpochMilli();
    }

    private long getMidnightTodayMillisecondsUTC() {
        return Instant.now().atZone(ZoneId.of("UTC"))
            .toLocalDate().atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.ofTotalSeconds(0))
            .toEpochMilli();
    }

    private class DayExclusion {
        long exclusions = 0;
        long actionedExclusions = 0;
    }
}
