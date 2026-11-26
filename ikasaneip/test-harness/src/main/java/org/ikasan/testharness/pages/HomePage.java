package org.ikasan.testharness.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The home dashboard (DashboardView, route ""): widgets for business streams, modules and agents, flow status,
 * exclusions (hospital events vs actioned) and error occurrences.
 */
public class HomePage
{
    public static final String FLOW_STATUS = "Flow Status";
    public static final String MODULES = "Modules and Agents";
    public static final String BUSINESS_STREAMS = "Business Streams";

    /** A flow state as the flow status widget shows it: the counter's css class. */
    public enum FlowStatus
    {
        RUNNING("running"), STOPPED("stopped"), STOPPED_IN_ERROR("stoppedInError"), RECOVERING("recovering"),
        PAUSED("paused"), UNKNOWN("unknown");

        final String cssClass;

        FlowStatus(String cssClass)
        {
            this.cssClass = cssClass;
        }
    }

    /** A chart series: its name and points (x = epoch millis, y = count). */
    public record Series(String name, List<List<Number>> points) {}

    private final Page page;

    public HomePage(Page page)
    {
        this.page = page;
    }

    public HomePage open()
    {
        Vaadin.gotoRoute(page, "");
        assertThat(widget(FLOW_STATUS)).isVisible();
        return this;
    }

    /** The widget whose header shows the text. */
    public Locator widget(String header)
    {
        return page.locator("vaadin-dashboard-widget").filter(new Locator.FilterOptions().setHasText(header)).first();
    }

    // ------------------------------------------------------------------------------------------------- flow status

    public void refreshFlowStatus()
    {
        widget(FLOW_STATUS).locator("vaadin-button:has(vaadin-icon[icon='vaadin:refresh'])").first().click();
        Vaadin.waitForVaadin(page);
    }

    /** The count a flow status counter shows, e.g. 2 for "2 running". */
    public int flowCount(FlowStatus status)
    {
        String text = widget(FLOW_STATUS).locator(".card-counter." + status.cssClass).first().innerText().trim();
        return Integer.parseInt(text.split("\\s+")[0]);
    }

    /**
     * The flows (module.flow) in a state: opens the counter's list and returns to the counters. A state without
     * flows has no list to open.
     */
    public List<String> flowsIn(FlowStatus status)
    {
        Locator open = widget(FLOW_STATUS).locator(".card-counter." + status.cssClass + " vaadin-icon");
        if (open.count() == 0)
        {
            return List.of();
        }
        open.first().click();
        Vaadin.waitForVaadin(page);
        Grid flows = new Grid(widget(FLOW_STATUS).locator("vaadin-grid"));
        flows.waitForLoaded();
        List<String> names = flows.rows().stream().map(r -> r.get("Flow Name")).toList();
        widget(FLOW_STATUS).locator("vaadin-icon[icon='vaadin:arrow-circle-left-o']").click();
        Vaadin.waitForVaadin(page);
        return names;
    }

    // ------------------------------------------------------------------------------------------------- grids

    public Grid modules()
    {
        Grid grid = new Grid(widget(MODULES).locator("vaadin-grid"));
        grid.waitForLoaded();
        return grid;
    }

    public void filterModules(String text)
    {
        Vaadin.fillField(widget(MODULES).locator("vaadin-text-field"), text);
        Vaadin.waitForVaadin(page);
    }

    public Grid businessStreams()
    {
        Grid grid = new Grid(widget(BUSINESS_STREAMS).locator("vaadin-grid"));
        grid.waitForLoaded();
        return grid;
    }

    public void filterBusinessStreams(String text)
    {
        Vaadin.fillField(widget(BUSINESS_STREAMS).locator("vaadin-text-field"), text);
        Vaadin.waitForVaadin(page);
    }

    // ------------------------------------------------------------------------------------------------- charts

    /** The exclusions chart: per day, all exclusions (outstanding and actioned) and the actioned ones. */
    public Locator exclusionsChart()
    {
        return page.locator("vaadin-chart.ikasan-charts").first();
    }

    /** The errors chart: error occurrences per minute over the last 20 minutes. */
    public Locator errorsChart()
    {
        return page.locator("vaadin-chart.live-errors").first();
    }

    /** Press the refresh button of the widget holding the chart. */
    public void refreshChart(Locator chart)
    {
        page.locator("vaadin-dashboard-widget").filter(new Locator.FilterOptions().setHas(chart)).first()
            .locator("vaadin-button:has(vaadin-icon[icon='vaadin:refresh'])").first().click();
        Vaadin.waitForVaadin(page);
    }

    /** The series of a chart, as Highcharts holds them in the browser. */
    @SuppressWarnings("unchecked")
    public List<Series> series(Locator chart)
    {
        chart.page().waitForFunction("el => el.configuration && el.configuration.series && el.configuration.series.length > 0",
            chart.elementHandle());
        List<Map<String, Object>> raw = (List<Map<String, Object>>) chart.evaluate(
            "el => el.configuration.series.map(s => ({ name: s.name, points: s.data.map(p => [p.x, p.y]) }))");
        return raw.stream().map(s -> new Series(String.valueOf(s.get("name")), (List<List<Number>>) s.get("points"))).toList();
    }
}
