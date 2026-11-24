package org.ikasan.testharness.support;

import com.microsoft.playwright.Locator;

import java.time.Duration;
import java.util.List;

/** A vaadin-grid. */
public class Grid
{
    private final Locator root;

    public Grid(Locator root)
    {
        this.root = root;
    }

    public Locator root()
    {
        return root;
    }

    /** Number of items the grid's data provider reports, not just the rendered (virtualised) rows. */
    public int size()
    {
        Object size = root.evaluate("g => g.size ?? 0");
        return ((Number) size).intValue();
    }

    /** Text of every cell of the rendered body rows. vaadin-grid virtualises, so this is the visible rows only. */
    @SuppressWarnings("unchecked")
    public List<List<String>> visibleRowTexts()
    {
        return (List<List<String>>) root.evaluate("""
            g => Array.from(g.shadowRoot.querySelectorAll('tbody#items tr:not([hidden])')).map(r =>
                   Array.from(r.querySelectorAll('td')).map(td => {
                     const slot = td.querySelector('slot');
                     const content = slot && slot.assignedElements ? slot.assignedElements()[0] : null;
                     return ((content && content.textContent) || '').trim();
                   }))""");
    }

    /** All visible cell text joined with newlines - convenient for "contains" checks. */
    public String visibleText()
    {
        return String.join("\n", visibleRowTexts().stream().flatMap(List::stream).toList());
    }

    /** Header filter text field for a column key (FilteringGrid gives the field the column key as id). */
    public Locator filter(String columnKey)
    {
        return root.page().locator("vaadin-text-field#" + columnKey);
    }

    /** Header texts of the grid's columns (first header row), in column order. */
    @SuppressWarnings("unchecked")
    public List<String> headerTexts()
    {
        return (List<String>) root.evaluate("""
            g => Array.from(g.shadowRoot.querySelectorAll('thead tr:first-child th')).map(th => {
                   const slot = th.querySelector('slot');
                   const content = slot && slot.assignedElements ? slot.assignedElements()[0] : null;
                   return ((content && content.textContent) || '').trim();
                 })""");
    }

    /**
     * The n-th (0-based) header filter text field. Most dashboard grids put an id-less TextField in a second
     * header row for each filterable column, in column order.
     */
    public Locator headerFilter(int index)
    {
        return root.locator("vaadin-grid-cell-content vaadin-text-field").nth(index);
    }

    /** The visible cell containing the text (vaadin-grid keeps hidden, recycled cells with stale text around). */
    public Locator cell(String text)
    {
        return root.locator("vaadin-grid-cell-content")
            .filter(new Locator.FilterOptions().setHasText(text).setVisible(true)).first();
    }

    /** Double-click the rendered row containing the text - dashboard grids open their detail dialogs this way. */
    public void doubleClickRow(String text)
    {
        cell(text).dblclick();
        Vaadin.waitForVaadin(root.page());
    }

    /**
     * A control rendered in the body row whose cells contain the text, e.g. the trash button:
     * {@code grid.inRow("my-role", "vaadin-icon[icon='vaadin:trash']").click()}.
     * The matching element is tagged with a unique attribute in the page and located by it.
     */
    public Locator inRow(String rowText, String selector)
    {
        return inRow(rowText, selector, null);
    }

    /**
     * An attribute of every element matching the selector in the row that has a cell whose trimmed text is exactly
     * cellText, skipping elements that are hidden (Vaadin's setVisible(false) sets the hidden attribute).
     * The attribute {@code textContent} gives the elements' trimmed text.
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> attributesInRow(String cellText, String selector, String attribute)
    {
        Object found = root.evaluate("""
            (g, a) => {
              const rows = Array.from(g.shadowRoot.querySelectorAll('tbody#items tr:not([hidden])'));
              const cellsOf = r => Array.from(r.querySelectorAll('slot'))
                .flatMap(s => s.assignedElements ? s.assignedElements() : []);
              for (const r of rows) {
                const cells = cellsOf(r);
                if (!cells.some(c => (c.textContent || '').trim() === a.text)) continue;
                return cells.flatMap(c => Array.from(c.querySelectorAll(a.selector)))
                  .filter(e => !e.hidden && !e.closest('[hidden]'))
                  .map(e => a.attribute === 'textContent' ? (e.textContent || '').trim() : e.getAttribute(a.attribute));
              }
              return null;
            }""", java.util.Map.of("text", cellText, "selector", selector, "attribute", attribute));
        if (found == null)
        {
            throw new AssertionError("No row with a cell '" + cellText + "' in the grid");
        }
        return (java.util.List<String>) found;
    }

    /** As {@link #inRow(String, String)}, choosing the element whose trimmed text equals elementText. */
    public Locator inRow(String rowText, String selector, String elementText)
    {
        String tag = java.util.UUID.randomUUID().toString();
        Object found = root.evaluate("""
            (g, a) => {
              const rows = Array.from(g.shadowRoot.querySelectorAll('tbody#items tr:not([hidden])'));
              const cellsOf = r => Array.from(r.querySelectorAll('slot'))
                .flatMap(s => s.assignedElements ? s.assignedElements() : []);
              for (const r of rows) {
                const cells = cellsOf(r);
                if (!cells.some(c => (c.textContent || '').includes(a.text))) continue;
                for (const c of cells) {
                  const el = Array.from(c.querySelectorAll(a.selector))
                    .find(e => a.label === '' || (e.textContent || '').trim() === a.label);
                  if (el) { el.setAttribute('data-harness-target', a.tag); return true; }
                }
              }
              return false;
            }""", java.util.Map.of("text", rowText, "selector", selector, "tag", tag,
                "label", elementText == null ? "" : elementText));
        if (!Boolean.TRUE.equals(found))
        {
            throw new IllegalStateException("No row containing '" + rowText + "' with " + selector);
        }
        return root.locator("[data-harness-target='" + tag + "']");
    }

    /**
     * Type into the n-th header filter and wait until the grid has re-rendered: every visible row contains the
     * text (or there are no rows). Waiting for the text alone is not enough when the unfiltered grid already
     * showed it - the rows (and their component renderers) are replaced when the filtered data arrives.
     */
    public void filterBy(int filterIndex, String text)
    {
        Vaadin.fillField(headerFilter(filterIndex), text);
        Vaadin.waitForVaadin(root.page());
        // a row matches when it contains every alphanumeric part of the filter: backends differ in how they match
        // a value with separators (Solr requires each part, Mongo the whole text), this accepts both
        List<String> parts = java.util.Arrays.stream(text.toLowerCase().split("[^\\p{L}\\p{N}]+"))
            .filter(p -> !p.isEmpty()).toList();
        waitFor(() -> visibleRowTexts().stream().map(r -> String.join(" ", r).toLowerCase())
                .allMatch(row -> parts.stream().allMatch(row::contains)),
            "every row to match the filter '" + text + "', it shows:\n");
        waitForLoaded();
    }

    /** Wait until the rendered rows contain the text (vaadin-grid recycles cells, so check visible rows only). */
    public void waitForText(String text)
    {
        waitFor(() -> visibleText().contains(text), "grid to show '" + text + "', it shows:\n" );
    }

    /** Wait until no rendered row contains the text. */
    public void waitForNoText(String text)
    {
        waitFor(() -> !visibleText().contains(text), "grid to stop showing '" + text + "', it shows:\n");
    }

    private void waitFor(java.util.function.BooleanSupplier condition, String description)
    {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!condition.getAsBoolean())
        {
            if (System.nanoTime() > deadline)
            {
                throw new AssertionError("Timed out waiting for " + description + visibleText());
            }
            root.page().waitForTimeout(200);
        }
    }

    /** A vaadin-button with exactly the given text in the row containing rowText, e.g. "Disable" or "End Session". */
    public Locator inRowButton(String rowText, String buttonText)
    {
        return inRow(rowText, "vaadin-button", buttonText);
    }

    /** Name used for the column of search result rows that only shows the entity type icon. */
    public static final String ENTITY_TYPE = "Entity Type";

    /**
     * Every row of the grid - not just the rendered ones: vaadin-grid virtualises, so the grid is scrolled through
     * and each row is read once. A row maps column header to cell text. Columns without a header (selection
     * checkboxes, action buttons) are left out, except the search results' type icon, which is reported as
     * {@value #ENTITY_TYPE} from its data-entity-type attribute.
     */
    @SuppressWarnings("unchecked")
    public List<java.util.Map<String, String>> rows()
    {
        waitForLoaded();
        return (List<java.util.Map<String, String>>) root.evaluate("""
            async (g) => {
              const frame = () => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)));
              const settle = async () => {
                for (let i = 0; i < 200 && (g.loading || g.shadowRoot.querySelector('tbody#items tr[loading]')); i++) {
                  await new Promise(r => setTimeout(r, 50));
                }
                await frame();
              };
              const contentOf = cell => {
                const slot = cell && cell.querySelector('slot');
                return slot && slot.assignedElements ? slot.assignedElements()[0] : null;
              };
              const headers = Array.from(g.shadowRoot.querySelectorAll('thead tr:first-child th'))
                .map(th => ((contentOf(th) || {}).textContent || '').trim());
              const size = g.size || 0;
              const rows = new Map();
              for (let start = 0; start < size || start === 0; ) {
                if (size > 0) g.scrollToIndex(start);
                await settle();
                const rendered = Array.from(g.shadowRoot.querySelectorAll('tbody#items tr'))
                  .filter(tr => !tr.hidden && typeof tr.index === 'number' && tr.index < size);
                for (const tr of rendered) {
                  if (rows.has(tr.index)) continue;
                  const row = {};
                  Array.from(tr.querySelectorAll('td')).forEach((td, i) => {
                    const content = contentOf(td);
                    const typed = content && content.querySelector('[data-entity-type]');
                    if (typed) { row['Entity Type'] = typed.getAttribute('data-entity-type'); return; }
                    if (headers[i]) row[headers[i]] = ((content && content.textContent) || '').trim();
                  });
                  rows.set(tr.index, row);
                }
                if (size === 0) break;
                const last = Math.max(start, ...rendered.map(tr => tr.index));
                if (last + 1 >= size) break;
                start = last + 1;
              }
              return Array.from(rows.keys()).sort((a, b) => a - b).map(k => rows.get(k));
            }""");
    }

    /** Wait until the grid has finished loading data from the server. */
    public void waitForLoaded()
    {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (Boolean.TRUE.equals(root.evaluate("g => !!g.loading")))
        {
            if (System.nanoTime() > deadline)
            {
                throw new IllegalStateException("Grid still loading after 30s");
            }
            root.page().waitForTimeout(100);
        }
    }
}
