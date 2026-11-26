package org.ikasan.testharness.it.stream;

import org.ikasan.testharness.support.Timeouts;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.module.FakeIkasanModule;
import org.ikasan.testharness.pages.DesignerPage;
import org.ikasan.testharness.pages.DesignerPage.Figure;
import org.ikasan.testharness.pages.HomePage;
import org.ikasan.testharness.seed.Seeder;
import org.ikasan.testharness.support.DashboardApi;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Business streams: drawing one in the designer from a module's flows (and a message channel), saving it, opening it
 * again; viewing it with each flow framed in its state's colour, following a module's state reports; deleting one;
 * and the home dashboard's list of streams.
 * <p>
 * The flows belong to a {@link FakeIkasanModule}, {@code bs-orders}. Mutating: every stream the tests create is named
 * "Harness ..." and removed before and after.
 */
@HarnessTest
@Tag("stream")
@Tag("mutating")
class BusinessStreamIT
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration WAIT = Timeouts.seconds(30);
    private static final String MODULE = "bs-orders";
    private static final String INTAKE = "Order Intake";
    private static final String DISPATCH = "Order Dispatch";
    private static final String RUNNING = "#05E36C";
    private static final String STOPPED_IN_ERROR = "#FF0000";

    private static FakeIkasanModule module;

    @BeforeAll
    static void registerModule(HarnessSession session)
    {
        removeHarnessStreams(session);
        HarnessConfig config = session.config();
        module = new FakeIkasanModule(config.scheduler().fakeAgentHost(), FakeIkasanModule.configuredPort(),
            config.dashboard().restUsername(), config.dashboard().restPassword()).start();
        module.flowState(MODULE, INTAKE, "running");
        module.flowState(MODULE, DISPATCH, "stoppedInError");
        DashboardApi dashboard = new DashboardApi(config);
        module.register(dashboard, MODULE, "Orders for the business stream tests", List.of(INTAKE, DISPATCH));
        reportState(dashboard, INTAKE, "running");
        reportState(dashboard, DISPATCH, "stoppedInError");
    }

    @AfterAll
    static void stopModule(HarnessSession session)
    {
        if (module != null)
        {
            module.close();
        }
        removeHarnessStreams(session);
    }

    @Test
    @Tag("smoke")
    void aStreamDrawnFromFlowsIsSavedAndOpensAgain(DesignerPage designer, HarnessSession session)
    {
        String name = "Harness Order Stream";
        designer.open();
        designer.addFlow(MODULE, INTAKE);
        designer.addFlow(MODULE, DISPATCH);
        designer.addMessageChannel("orders.queue");
        String intake = designer.flowFigureId(MODULE, INTAKE);
        String dispatch = designer.flowFigureId(MODULE, DISPATCH);
        designer.connect(intake, dispatch);

        designer.saveAs(name, "Orders from intake to dispatch");
        Vaadin.expectNotification(designer.page(), "Business stream saved");

        // what is stored: the stream's name and description, and the diagram as draw2d JSON
        Map<String, Object> stored = stream(session, name).orElseThrow(() -> new AssertionError("no stored stream " + name));
        assertThat(stored).containsEntry("moduleName", name).containsEntry("flowName", "Orders from intake to dispatch");
        JsonNode diagram = JSON.readTree(String.valueOf(stored.get("payload")));
        List<String> ids = diagram.valueStream().map(n -> n.path("id").asString()).toList();
        assertThat(ids).contains(intake, dispatch).anyMatch(id -> id.startsWith("MESSAGE_CHANNEL:orders.queue:"));
        assertThat(diagram.valueStream().filter(n -> n.path("type").asString().equals("draw2d.Connection")))
            .as("connections saved").singleElement()
            .satisfies(c -> assertThat(List.of(c.path("source").path("node").asString(), c.path("target").path("node").asString()))
                .containsExactly(intake, dispatch));

        // Save now saves to the stream just saved as, without asking for a name again
        designer.addMessageChannel("orders.dlq");
        designer.file("Save");
        assertThat(designer.page().locator("vaadin-dialog[opened]").filter(new Locator.FilterOptions()
            .setHasText("Business Stream Management"))).hasCount(0);
        awaitStoredDiagram(session, name, ids2 -> ids2.stream().anyMatch(id -> id.startsWith("MESSAGE_CHANNEL:orders.dlq:")),
            "the stream saved again with the orders.dlq channel");

        // a new canvas, then the saved stream opened again
        designer.file("New");
        assertThat(designer.figures()).as("a new canvas").isEmpty();
        designer.openSaved(name);
        designer.awaitText(MODULE + "." + INTAKE);
        assertThat(designer.figures()).extracting(Figure::text).contains(MODULE + "." + INTAKE, MODULE + "." + DISPATCH, "orders.queue",
            "orders.dlq");
        assertThat(designer.connections()).containsExactly(List.of(intake, dispatch));
    }

    @Test
    void aStreamShowsItsFlowsInTheColoursOfTheirStates(DesignerPage viewer, HarnessSession session)
    {
        String name = "Harness Viewed Stream";
        String intake = "FLOW:" + MODULE + "." + INTAKE + ":11111111-1111-1111-1111-111111111111";
        String dispatch = "FLOW:" + MODULE + "." + DISPATCH + ":22222222-2222-2222-2222-222222222222";
        seedStream(session, name, "Seen in the stream view", List.of(
            flowImage(intake, 200, "hybrid3", "draw2d.layout.locator.OutputPortLocator"),
            flowImage(dispatch, 500, "hybrid0", "draw2d.layout.locator.InputPortLocator"),
            Map.of("type", "draw2d.Connection", "id", "harness-connection",
                "router", "draw2d.layout.connection.InteractiveManhattanConnectionRouter",
                "source", Map.of("node", intake, "port", "hybrid3"), "target", Map.of("node", dispatch, "port", "hybrid0"))));

        viewer.openStream(name);
        Page page = viewer.page();
        assertThat(page.locator("h2").filter(new Locator.FilterOptions().setHasText(name)).first()).isVisible();
        assertThat(page.getByText("Seen in the stream view").first()).isVisible();
        assertThat(viewer.connections()).containsExactly(List.of(intake, dispatch));
        awaitFrames(viewer, List.of(RUNNING, STOPPED_IN_ERROR));

        DashboardApi dashboard = new DashboardApi(session.config());
        try
        {
            // the module reports the failed flow running again: its frame follows
            module.flowState(MODULE, DISPATCH, "running");
            reportState(dashboard, DISPATCH, "running");
            awaitFrames(viewer, List.of(RUNNING, RUNNING));
        }
        finally
        {
            module.flowState(MODULE, DISPATCH, "stoppedInError");
            reportState(dashboard, DISPATCH, "stoppedInError");
        }
    }

    @Test
    void saveAsNeedsANameAndADescription(DesignerPage designer)
    {
        designer.open();
        designer.addMessageChannel("orders.queue");
        designer.file("Save as");
        Locator dialog = Vaadin.dialog(designer.page(), "Business Stream Management");
        Vaadin.button(dialog, "Save").click();
        assertThat(dialog.getByText("A business stream name must be provided!")).isVisible();
        Vaadin.fillField(Vaadin.field(dialog, "Business Stream Name"), "Harness Unsaved Stream");
        Vaadin.button(dialog, "Save").click();
        assertThat(dialog.getByText("A business stream description must be provided!")).isVisible();
    }

    @Test
    void aStreamIsDeletedFromManage(DesignerPage designer, HarnessSession session)
    {
        String name = "Harness Stream To Delete";
        seedStream(session, name, "To be deleted", List.of(flowImage("FLOW:" + MODULE + "." + INTAKE
            + ":33333333-3333-3333-3333-333333333333", 200, "hybrid3", "draw2d.layout.locator.OutputPortLocator")));

        designer.open();
        designer.file("Manage");
        Locator dialog = Vaadin.dialog(designer.page(), "Business Streams");
        Grid streams = new Grid(dialog.locator("vaadin-grid").first());
        streams.waitForText(name);
        streams.inRow(name, "vaadin-icon[icon='vaadin:trash']").click();
        // deleting asks first
        assertThat(designer.page().locator("vaadin-confirm-dialog[opened]").getByText(
            "You are about to delete a business stream. Are you sure you would like to proceed?")).isVisible();
        assertThat(stream(session, name)).as("the stream before the delete is confirmed").isPresent();
        Vaadin.confirm(designer.page());
        streams.waitForNoText(name);
        assertThat(stream(session, name)).as("the deleted stream in the store").isEmpty();
    }

    @Test
    void theHomeDashboardListsStreamsAndLinksToThem(HomePage home, HarnessSession session)
    {
        String name = "Harness Listed Stream";
        seedStream(session, name, "Listed on the home dashboard", List.of(flowImage("FLOW:" + MODULE + "." + INTAKE
            + ":44444444-4444-4444-4444-444444444444", 200, "hybrid3", "draw2d.layout.locator.OutputPortLocator")));

        home.open();
        Grid grid = home.businessStreams();
        grid.waitForText(name);
        home.filterBusinessStreams("Listed");
        grid.waitForText(name);
        assertThat(grid.rows()).extracting(r -> r.get("Name")).containsExactly(name);
        String href = home.widget(HomePage.BUSINESS_STREAMS).locator("a").filter(new Locator.FilterOptions().setHasText("view"))
            .first().getAttribute("href");
        assertThat(java.net.URLDecoder.decode(href, java.nio.charset.StandardCharsets.UTF_8))
            .as("the stream's view link").endsWith("visualisationTab/BUSINESS_STREAM:" + name);
    }

    /** The designer is offered (and opens) only to users with business stream administration. */
    @Test
    @org.ikasan.testharness.junit.LoginAs("access-reader")
    void theDesignerTurnsAwayUsersWithoutBusinessStreamAccess(Page page)
    {
        Vaadin.gotoRoute(page, "designer");
        assertThat(page.getByText("Flow Status", new Page.GetByTextOptions().setExact(true)).first()).isVisible();
        assertThat(page.locator("[id^='canvas-viewport-']")).hasCount(0);
    }

    @Test
    void anIntegratedSystemIsAddedByDoubleClickingItsImage(DesignerPage designer)
    {
        designer.open();
        designer.openPalette("Integrated Systems");
        designer.page().locator("#canvas-palette img[src$='computer.png']").first().dblclick();
        Vaadin.waitForVaadin(designer.page());
        designer.page().waitForFunction("() => document.querySelector(\"[id^='canvas-viewport-']\").$connector.designer"
            + ".getFigures().asArray().some(f => f.getId().startsWith('INTEGRATED_SYSTEM:'))");
        assertThat(designer.figures()).anyMatch(f -> f.id().startsWith("INTEGRATED_SYSTEM:"));
    }

    // ------------------------------------------------------------------------------------------------- helpers

    /** Wait until the stored diagram's figure ids satisfy the condition (the save is asynchronous). */
    private static void awaitStoredDiagram(HarnessSession session, String name, java.util.function.Predicate<List<String>> condition,
                                           String description)
    {
        long deadline = System.nanoTime() + WAIT.toNanos();
        List<String> ids = List.of();
        do
        {
            Optional<Map<String, Object>> stored = stream(session, name);
            if (stored.isPresent())
            {
                ids = JSON.readTree(String.valueOf(stored.get().get("payload"))).valueStream().map(n -> n.path("id").asString()).toList();
                if (condition.test(ids))
                {
                    return;
                }
            }
            sleep();
        }
        while (System.nanoTime() < deadline);
        throw new AssertionError("Not " + description + " after " + WAIT + ": " + ids);
    }

    private static void sleep()
    {
        try
        {
            Thread.sleep(250);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void reportState(DashboardApi dashboard, String flow, String state)
    {
        dashboard.request("PUT", "/rest/flowStates/cache",
            JSON.writeValueAsString(Map.of("moduleName", MODULE, "flowName", flow, "state", state)));
    }

    /** Wait until the flows' frames (the boundaries drawn around them) have exactly these colours, in any order. */
    private static void awaitFrames(DesignerPage viewer, List<String> colours)
    {
        long deadline = System.nanoTime() + WAIT.toNanos();
        List<String> frames;
        do
        {
            frames = viewer.figures().stream().filter(f -> f.type().equals("draw2d.shape.basic.Rectangle"))
                .map(f -> String.valueOf(f.color()).toUpperCase()).sorted().toList();
            if (frames.equals(colours.stream().map(String::toUpperCase).sorted().toList()))
            {
                return;
            }
            viewer.page().waitForTimeout(500);
        }
        while (System.nanoTime() < deadline);
        throw new AssertionError("Flow frames " + frames + " are not " + colours + " after " + WAIT);
    }

    /** An Ikasan flow on a diagram: its image, with one port. */
    private static Map<String, Object> flowImage(String id, int x, String port, String locator)
    {
        Map<String, Object> image = new LinkedHashMap<>();
        image.put("type", "draw2d.shape.basic.Image");
        image.put("id", id);
        image.put("x", x);
        image.put("y", 200);
        image.put("width", 95);
        image.put("height", 63);
        image.put("path", "frontend/images/flow.png");
        image.put("ports", List.of(Map.of("type", "draw2d.HybridPort", "id", id + "-" + port, "name", port,
            "port", "draw2d.HybridPort", "locator", locator)));
        return image;
    }

    /** Write a business stream document: id and name are the same, the description is kept in flowName. */
    private static void seedStream(HarnessSession session, String name, String description, List<Map<String, Object>> diagram)
    {
        Map<String, Object> stream = new LinkedHashMap<>();
        stream.put("id", name);
        stream.put("type", "businessStreamMetaData");
        stream.put("moduleName", name);
        stream.put("flowName", description);
        stream.put("payload", JSON.writeValueAsString(diagram));
        stream.put("timestamp", System.currentTimeMillis());
        try (Seeder seeder = Seeder.create(session.config()))
        {
            seeder.load(List.of(stream));
        }
    }

    private static Optional<Map<String, Object>> stream(HarnessSession session, String name)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            return seeder.dump(List.of("businessStreamMetaData")).stream().filter(d -> name.equals(d.get("moduleName"))).findFirst();
        }
    }

    private static void removeHarnessStreams(HarnessSession session)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            List<String> ids = seeder.dump(List.of("businessStreamMetaData")).stream()
                .filter(d -> String.valueOf(d.get("moduleName")).startsWith("Harness "))
                .map(d -> String.valueOf(d.get("id"))).toList();
            if (!ids.isEmpty())
            {
                seeder.delete(ids);
            }
        }
    }
}
