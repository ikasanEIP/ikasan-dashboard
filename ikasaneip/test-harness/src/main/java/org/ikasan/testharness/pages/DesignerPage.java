package org.ikasan.testharness.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The business stream designer (BusinessStreamDesignerView, route 'designer') and the read-only business stream
 * view ('visualisationTab/BUSINESS_STREAM:&lt;name&gt;'). Both draw with draw2d inside a canvas div; the diagram's
 * figures and connections are read from draw2d in the browser ({@code el.$connector.designer}), which is also what
 * the designer saves ({@code el.$connector.exportJson()}).
 */
public class DesignerPage
{
    /** A figure on the canvas: its draw2d id and type, its text (labels) and colour (boundaries), as hex. */
    public record Figure(String id, String type, String text, String color) {}

    private static final String DESIGNER_CANVAS = "[id^='canvas-viewport-']";
    private static final String VIEWER_CANVAS = "[id^='business-stream-viewport-']";

    private final Page page;
    private String canvas = DESIGNER_CANVAS;

    public DesignerPage(Page page)
    {
        this.page = page;
    }

    public Page page()
    {
        return page;
    }

    /** Open the designer with an empty canvas. */
    public DesignerPage open()
    {
        canvas = DESIGNER_CANVAS;
        Vaadin.gotoRoute(page, "designer");
        awaitCanvas();
        return this;
    }

    /** Open the read-only view of a saved business stream. */
    public DesignerPage openStream(String name)
    {
        canvas = VIEWER_CANVAS;
        Vaadin.gotoRoute(page, "visualisationTab/BUSINESS_STREAM:" + name);
        awaitCanvas();
        return this;
    }

    /**
     * Open the visual job plan editor (route 'jobTemplateManagement/&lt;plan&gt;/&lt;child plan&gt;') and wait until the plan's
     * jobs are drawn (the diagram is imported asynchronously).
     */
    public DesignerPage openJobPlan(String plan, String childPlan, String anyJobIdentifier)
    {
        canvas = DESIGNER_CANVAS;
        Vaadin.gotoRoute(page, "jobTemplateManagement/" + plan + "/" + childPlan);
        awaitCanvas();
        awaitFigure(anyJobIdentifier);
        return this;
    }

    /** Wait until a figure with the id is on the canvas. */
    public void awaitFigure(String id)
    {
        page.waitForFunction("a => { const el = document.querySelector(a[0]); return el && el.$connector.designer.getFigures()"
            + ".asArray().some(f => f.getId() === a[1]); }", List.of(canvas, id));
    }

    /**
     * Connect a job to another, from its right-hand port to the other's left-hand port, as dragging between them does.
     * Ports are found by where they sit: jobs drawn from the stored plan name theirs (rightHybridSource,
     * leftHybridTarget), jobs just added do not.
     */
    public void connectJobs(String fromJobId, String toJobId)
    {
        page.locator(canvas).first().evaluate("(el, a) => { const c = el.$connector.designer;"
            + " const port = (id, side) => { const f = c.getFigure(id); const p = f.getPorts().asArray()"
            + ".find(p => p.getLocator().NAME === 'draw2d.layout.locator.' + side + 'Locator');"
            + " if (!p) throw new Error('no ' + side + ' port on ' + id); return p; };"
            + " c.getCommandStack().execute(new draw2d.command.CommandConnect(port(a[0], 'Right'), port(a[1], 'Left'))); }",
            List.of(fromJobId, toJobId));
    }

    /**
     * Move a figure to x, y, keeping its job group (image and label) together, as dragging it does.
     */
    public void move(String id, int x, int y)
    {
        page.locator(canvas).first().evaluate("(el, a) => { const c = el.$connector.designer; const f = c.getFigure(a[0]);"
            + " const target = f.getComposite ? (f.getComposite() || f) : f;"
            + " const dx = a[1] - f.getX(), dy = a[2] - f.getY();"
            + " const move = new draw2d.command.CommandMove(target); move.setPosition(target.getX() + dx, target.getY() + dy);"
            + " c.getCommandStack().execute(move); }",
            List.of(id, x, y));
    }

    /** Set a figure's bounds (e.g. an AND/OR grouping box). */
    public void setBounds(String id, int x, int y, int width, int height)
    {
        page.locator(canvas).first().evaluate("(el, a) => { const f = el.$connector.designer.getFigure(a[0]);"
            + " f.setPosition(a[1], a[2]); f.setDimension(a[3], a[4]); }", List.of(id, x, y, width, height));
    }

    /** A figure's bounds: x, y, width, height. */
    @SuppressWarnings("unchecked")
    public List<Number> bounds(String id)
    {
        return (List<Number>) page.locator(canvas).first().evaluate("(el, id) => { const f = el.$connector.designer.getFigure(id);"
            + " return [f.getX(), f.getY(), f.getWidth(), f.getHeight()]; }", id);
    }

    private void awaitCanvas()
    {
        page.locator(canvas + " svg").first().waitFor();
        page.waitForFunction("sel => { const el = document.querySelector(sel); return el && el.$connector && el.$connector.designer; }",
            canvas);
        Vaadin.waitForVaadin(page);
    }

    // ------------------------------------------------------------------------------------------------- palette

    public void openPalette(String name)
    {
        Locator heading = page.locator("#canvas-palette vaadin-accordion-heading").filter(new Locator.FilterOptions().setHasText(name)).first();
        Locator panel = heading.locator("xpath=ancestor::vaadin-accordion-panel[1]");
        if (panel.getAttribute("opened") == null)
        {
            heading.click();
            Vaadin.waitForVaadin(page);
        }
    }

    /** Add an Ikasan flow: the General palette's flow item opens a list of the integration modules' flows. */
    public void addFlow(String module, String flow)
    {
        openPalette("General");
        page.locator("#canvas-palette img[src$='flow.png']").first().dblclick();
        Locator dialog = Vaadin.dialog(page, "Please select a flow");
        Grid flows = new Grid(dialog.locator("vaadin-grid").first());
        flows.waitForText(module);
        // several modules can have a flow of this name: choose the module's
        flows.doubleClickRowWhere(module, flow);
        awaitText(module + "." + flow);
    }

    /** Add a message channel with the name (the General palette's channel item asks for it). */
    public void addMessageChannel(String name)
    {
        openPalette("General");
        page.locator("#canvas-palette img[src$='message-channel.png']").first().dblclick();
        Locator dialog = Vaadin.dialog(page, "Provide message channel name");
        Vaadin.fillField(Vaadin.field(dialog, "Message channel name"), name);
        Vaadin.button(dialog, "OK").click();
        Vaadin.waitForVaadin(page);
        awaitText(name);
    }

    /** Wait until a label with the text is on the canvas. */
    public void awaitText(String text)
    {
        page.waitForFunction("a => { const el = document.querySelector(a[0]); return el && el.$connector.designer.getFigures().asArray()"
            + ".some(f => f.getText && f.getText() === a[1]); }", List.of(canvas, text));
    }

    /**
     * Connect two figures, output port to input port, as dragging between their ports does (draw2d's connect
     * command, so it can be undone and is saved like a drawn connection).
     */
    public void connect(String fromFigureId, String toFigureId)
    {
        page.locator(canvas).first().evaluate("(el, a) => { const c = el.$connector.designer;"
            + " c.getCommandStack().execute(new draw2d.command.CommandConnect("
            + "c.getFigure(a[0]).getPorts().get(3), c.getFigure(a[1]).getPorts().get(0))); }", List.of(fromFigureId, toFigureId));
    }

    // ------------------------------------------------------------------------------------------------- reading the diagram

    @SuppressWarnings("unchecked")
    public List<Figure> figures()
    {
        List<Map<String, Object>> raw = (List<Map<String, Object>>) page.locator(canvas).first().evaluate(
            "el => el.$connector.designer.getFigures().asArray().map(f => ({ id: f.getId(), type: f.NAME,"
                + " text: f.getText ? f.getText() : null, color: f.getColor ? f.getColor().hash() : null }))");
        return raw.stream().map(f -> new Figure(String.valueOf(f.get("id")), String.valueOf(f.get("type")),
            (String) f.get("text"), (String) f.get("color"))).toList();
    }

    /** The connections: source and target figure ids. */
    @SuppressWarnings("unchecked")
    public List<List<String>> connections()
    {
        return (List<List<String>>) page.locator(canvas).first().evaluate(
            "el => el.$connector.designer.getLines().asArray().map(l => [l.getSource().getParent().getId(), l.getTarget().getParent().getId()])");
    }

    /** The id of the flow figure (an image whose id is FLOW:&lt;module&gt;.&lt;flow&gt;:&lt;uuid&gt;). */
    public String flowFigureId(String module, String flow)
    {
        return figures().stream().map(Figure::id).filter(id -> id.startsWith("FLOW:" + module + "." + flow + ":")).findFirst()
            .orElseThrow(() -> new AssertionError("no figure for flow " + module + "." + flow + " in " + figures()));
    }

    // ------------------------------------------------------------------------------------------------- menus

    /** Choose File > item (New, Open, Save, Save as, Manage). */
    public void file(String item)
    {
        page.locator("#canvas-menu vaadin-menu-bar-button").filter(new Locator.FilterOptions().setHasText("File")).first().click();
        page.getByRole(com.microsoft.playwright.options.AriaRole.MENUITEM,
            new Page.GetByRoleOptions().setName(item).setExact(true)).first().click();
        Vaadin.waitForVaadin(page);
    }

    /** File > Save as: name and describe the stream and save it. */
    public void saveAs(String name, String description)
    {
        file("Save as");
        Locator dialog = Vaadin.dialog(page, "Business Stream Management");
        Vaadin.fillField(Vaadin.field(dialog, "Business Stream Name"), name);
        Vaadin.fillField(Vaadin.field(dialog, "Business Stream Description"), description);
        Vaadin.button(dialog, "Save").click();
        Vaadin.waitForVaadin(page);
    }

    /** File > Open: double-click the stream in the list. */
    public void openSaved(String name)
    {
        file("Open");
        Locator dialog = Vaadin.dialog(page, "Business Streams");
        Grid streams = new Grid(dialog.locator("vaadin-grid").first());
        streams.waitForText(name);
        streams.root().locator("vaadin-grid-cell-content").filter(new Locator.FilterOptions().setHasText(name)).first().dblclick();
        Vaadin.waitForVaadin(page);
        assertThat(page.locator(canvas + " svg").first()).isVisible();
    }
}
