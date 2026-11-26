package org.ikasan.testharness.it.home;

import org.ikasan.testharness.support.Timeouts;
import com.microsoft.playwright.Locator;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.module.FakeIkasanModule;
import org.ikasan.testharness.pages.HomePage;
import org.ikasan.testharness.pages.HomePage.FlowStatus;
import org.ikasan.testharness.pages.HomePage.Series;
import org.ikasan.testharness.seed.Seeder;
import org.ikasan.testharness.support.DashboardApi;
import org.ikasan.testharness.support.Grid;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The home dashboard's widgets against known data:
 * <ul>
 *     <li>flow status: flows of two fake modules, their states asked of the modules by the dashboard, then changed
 *     by a module reporting a new state ({@code PUT /rest/flowStates/cache})</li>
 *     <li>modules and agents, and business streams: the grids and their filters</li>
 *     <li>exclusions per day (all vs actioned) and errors per minute: the charts' values against the store</li>
 * </ul>
 * Mutating: registers modules and adds a business stream, an actioned exclusion and errors (removing the documents it
 * added).
 */
@HarnessTest
@Tag("home")
@Tag("mutating")
class HomeDashboardIT
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration WAIT = Timeouts.seconds(60);
    private static final String ORDERS = "home-orders";
    private static final String PAYMENTS = "home-payments";
    private static final String INTAKE = ORDERS + ".Order Intake";
    private static final String DISPATCH = ORDERS + ".Order Dispatch";
    private static final String CAPTURE = PAYMENTS + ".Payment Capture";
    private static final String STREAM_ID = "home-order-to-cash";

    private static FakeIkasanModule modules;

    @BeforeAll
    static void registerModules(HarnessSession session)
    {
        HarnessConfig config = session.config();
        modules = new FakeIkasanModule(config.scheduler().fakeAgentHost(), FakeIkasanModule.configuredPort(),
            config.dashboard().restUsername(), config.dashboard().restPassword()).start();
        modules.flowState(ORDERS, "Order Intake", "running");
        modules.flowState(ORDERS, "Order Dispatch", "stopped");
        modules.flowState(PAYMENTS, "Payment Capture", "stoppedInError");
        DashboardApi dashboard = new DashboardApi(config);
        modules.register(dashboard, ORDERS, "Takes and dispatches orders", List.of("Order Intake", "Order Dispatch"));
        modules.register(dashboard, PAYMENTS, "Captures payments", List.of("Payment Capture"));
        // a module reports its flows' states as they change; start from the states the fake module reports
        reportState(dashboard, ORDERS, "Order Intake", "running");
    }

    @AfterAll
    static void stopModules()
    {
        if (modules != null)
        {
            modules.close();
        }
    }

    @Test
    @Tag("smoke")
    void flowStatusCountsTheModulesFlowsByState(HomePage home)
    {
        home.open();
        awaitFlows(home, FlowStatus.RUNNING, INTAKE, true);
        awaitFlows(home, FlowStatus.STOPPED, DISPATCH, true);
        awaitFlows(home, FlowStatus.STOPPED_IN_ERROR, CAPTURE, true);
        for (FlowStatus status : FlowStatus.values())
        {
            assertThat(home.flowCount(status)).as("the " + status + " counter matches its list of flows")
                .isEqualTo(home.flowsIn(status).size());
        }
    }

    @Test
    void aFlowStateReportedByItsModuleIsShown(HomePage home, HarnessSession session)
    {
        DashboardApi dashboard = new DashboardApi(session.config());
        home.open();
        awaitFlows(home, FlowStatus.RUNNING, INTAKE, true);
        try
        {
            modules.flowState(ORDERS, "Order Intake", "paused");
            reportState(dashboard, ORDERS, "Order Intake", "paused");
            awaitFlows(home, FlowStatus.PAUSED, INTAKE, true);
            assertThat(home.flowsIn(FlowStatus.RUNNING)).as("running flows once Order Intake is paused").doesNotContain(INTAKE);
        }
        finally
        {
            modules.flowState(ORDERS, "Order Intake", "running");
            reportState(dashboard, ORDERS, "Order Intake", "running");
        }
    }

    @Test
    void modulesAndAgentsAreListedAndFiltered(HomePage home)
    {
        home.open();
        Grid grid = home.modules();
        grid.waitForText(ORDERS);
        assertThat(grid.rows()).as("modules and agents")
            .anySatisfy(r -> assertThat(r).containsEntry("Module Name", ORDERS).containsEntry("Description", "Takes and dispatches orders"))
            .anySatisfy(r -> assertThat(r).containsEntry("Module Name", PAYMENTS).containsEntry("Description", "Captures payments"));

        home.filterModules("payments");
        grid.waitForNoText(ORDERS);
        assertThat(grid.rows()).extracting(r -> r.get("Module Name")).containsExactly(PAYMENTS);
        assertThat(home.widget(HomePage.MODULES).locator("a").filter(new Locator.FilterOptions().setHasText("view")).first()
            .getAttribute("href")).as("the module's view link").contains(PAYMENTS);
    }

    @Test
    void businessStreamsAreListedAndFiltered(HomePage home, HarnessSession session)
    {
        Map<String, Object> stream = new LinkedHashMap<>();
        stream.put("id", STREAM_ID);
        stream.put("type", "businessStreamMetaData");
        // the business stream's name and description are kept in the module and flow name fields
        stream.put("moduleName", "Order to Cash");
        stream.put("flowName", "Orders from intake to payment");
        stream.put("payload", JSON.writeValueAsString(Map.of("name", "Order to Cash", "description", "Orders from intake to payment",
            "flows", List.of(), "links", List.of())));
        stream.put("timestamp", System.currentTimeMillis());
        load(session, List.of(stream));
        try
        {
            home.open();
            Grid grid = home.businessStreams();
            grid.waitForText("Order to Cash");
            assertThat(grid.rows()).anySatisfy(r -> assertThat(r).containsEntry("Name", "Order to Cash")
                .containsEntry("Description", "Orders from intake to payment"));
            home.filterBusinessStreams("no-such-stream");
            grid.waitForNoText("Order to Cash");
        }
        finally
        {
            delete(session, STREAM_ID);
        }
    }

    @Test
    void theExclusionsChartCountsAllAndActionedExclusionsPerDay(HomePage home, HarnessSession session)
    {
        long now = System.currentTimeMillis();
        Map<String, Object> actioned = new LinkedHashMap<>();
        actioned.put("id", "harness-module-a:exclusion:home-actioned");
        actioned.put("type", "exclusionEventAction");
        actioned.put("moduleName", "harness-module-a");
        actioned.put("flowName", "Order Flow");
        actioned.put("errorUri", "harness-module-a:exclusion:home-actioned");
        actioned.put("hospitalEventAction", "comment[home dashboard], action[re-submitted], actioned by[admin]");
        actioned.put("payload", "<order><id>ORDER-999</id></order>");
        actioned.put("timestamp", now - Duration.ofMinutes(10).toMillis());
        actioned.put("expiry", now + Duration.ofDays(30).toMillis());
        load(session, List.of(actioned));
        try
        {
            home.open();
            Locator chart = home.exclusionsChart();
            awaitChart(home, chart, () -> {
                List<Series> series = home.series(chart);
                // one point per day: x is the day's start; the day runs to the next point
                List<Long> days = series.get(0).points().stream().map(p -> p.get(0).longValue()).toList();
                long actionedPerDay[] = days.stream().mapToLong(d -> count(session, "exclusionEventAction", d, d + Duration.ofDays(1).toMillis())).toArray();
                List<Number> expectedAll = new java.util.ArrayList<>();
                List<Number> expectedActioned = new java.util.ArrayList<>();
                for (int i = 0; i < days.size(); i++)
                {
                    long day = days.get(i);
                    expectedActioned.add(actionedPerDay[i]);
                    expectedAll.add(actionedPerDay[i] + count(session, "exclusion", day, day + Duration.ofDays(1).toMillis()));
                }
                return Map.of("Exclusions", expectedAll, "Actioned exclusions", expectedActioned);
            });
            assertThat(total(home.series(chart).get(1))).as("actioned exclusions charted").isPositive();
        }
        finally
        {
            delete(session, "harness-module-a:exclusion:home-actioned");
        }
    }

    @Test
    void theErrorsChartCountsErrorsPerMinute(HomePage home, HarnessSession session)
    {
        long now = System.currentTimeMillis();
        List<Map<String, Object>> errors = List.of(error("home-error-1", now - Duration.ofSeconds(90).toMillis()),
            error("home-error-2", now - Duration.ofSeconds(150).toMillis()));
        load(session, errors);
        try
        {
            home.open();
            Locator chart = home.errorsChart();
            awaitChart(home, chart, () -> {
                // one point per minute: x is the minute's end
                List<Number> expected = home.series(chart).get(0).points().stream()
                    .map(p -> (Number) count(session, "error", p.get(0).longValue() - 60_000, p.get(0).longValue())).toList();
                return Map.of("Error Occurrences", expected);
            });
            assertThat(total(home.series(chart).get(0))).as("errors charted (at least the two just added)").isGreaterThanOrEqualTo(2);
        }
        finally
        {
            delete(session, "harness-module-a-error-home-error-1", "harness-module-a-error-home-error-2");
        }
    }

    // ------------------------------------------------------------------------------------------------- helpers

    /** A module reporting a flow's new state to the dashboard, as Ikasan modules do. */
    private static void reportState(DashboardApi dashboard, String module, String flow, String state)
    {
        dashboard.request("PUT", "/rest/flowStates/cache",
            JSON.writeValueAsString(Map.of("moduleName", module, "flowName", flow, "state", state)));
    }

    /** Refresh the flow status until the flow is (or is not) listed under the state: states arrive asynchronously. */
    private static void awaitFlows(HomePage home, FlowStatus status, String flow, boolean listed)
    {
        long deadline = System.nanoTime() + WAIT.toNanos();
        List<String> flows;
        do
        {
            home.refreshFlowStatus();
            flows = home.flowsIn(status);
            if (flows.contains(flow) == listed)
            {
                return;
            }
        }
        while (System.nanoTime() < deadline);
        throw new AssertionError(flow + (listed ? " is not" : " is still") + " listed as " + status + " after " + WAIT + ": " + flows);
    }

    /**
     * Refresh the chart until every series shows the values expected of the store (the index may lag new documents).
     * The expected values are computed from the points' own times, so the chart's window is whatever it rendered.
     */
    private static void awaitChart(HomePage home, Locator chart, Supplier<Map<String, List<Number>>> expected)
    {
        long deadline = System.nanoTime() + WAIT.toNanos();
        String mismatch;
        do
        {
            home.refreshChart(chart);
            Map<String, List<Number>> want = expected.get();
            Map<String, List<Long>> got = new LinkedHashMap<>();
            home.series(chart).forEach(s -> got.put(s.name(), s.points().stream().map(p -> p.get(1).longValue()).toList()));
            Map<String, List<Long>> wantLong = new LinkedHashMap<>();
            want.forEach((k, v) -> wantLong.put(k, v.stream().map(Number::longValue).toList()));
            if (got.equals(wantLong))
            {
                return;
            }
            mismatch = "chart " + got + ", store " + wantLong;
        }
        while (System.nanoTime() < deadline);
        throw new AssertionError("The chart does not match the store after " + WAIT + ": " + mismatch);
    }

    private static long total(Series series)
    {
        return series.points().stream().mapToLong(p -> p.get(1).longValue()).sum();
    }

    /** Documents of a type whose timestamp is within [from, to], as the widgets' searches count them. */
    private static long count(HarnessSession session, String type, long from, long to)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            return seeder.dump(List.of(type)).stream()
                .map(d -> ((Number) d.get("timestamp")).longValue())
                .filter(t -> t >= from && t <= to).count();
        }
    }

    private static Map<String, Object> error(String uri, long timestamp)
    {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("id", "harness-module-a-error-" + uri);
        e.put("errorUri", uri);
        e.put("type", "error");
        e.put("moduleName", "harness-module-a");
        e.put("flowName", "Order Flow");
        e.put("componentName", "orderProducer");
        e.put("event", "EVT-" + uri);
        e.put("payload", "<order/>");
        e.put("timestamp", timestamp);
        e.put("harvestReceivedTimestamp", timestamp);
        e.put("expiry", timestamp + Duration.ofDays(30).toMillis());
        e.put("errorAction", "Stop");
        e.put("errorMessage", "home dashboard error");
        return e;
    }

    private static void load(HarnessSession session, List<Map<String, Object>> documents)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            seeder.load(documents);
        }
    }

    private static void delete(HarnessSession session, String... ids)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            seeder.delete(List.of(ids));
        }
    }
}
