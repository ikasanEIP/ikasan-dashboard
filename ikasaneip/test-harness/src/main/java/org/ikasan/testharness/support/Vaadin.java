package org.ikasan.testharness.support;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Helpers for driving Vaadin Flow components. Playwright CSS selectors pierce shadow DOM, so component
 * internals can be reached directly; these helpers wrap the common interactions.
 */
public final class Vaadin
{
    private static final Locator.WaitForOptions ATTACHED =
        new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED);

    private Vaadin() {}

    /** Wait until no Vaadin client is busy with a server round trip. */
    public static void waitForVaadin(Page page)
    {
        page.waitForFunction("""
            () => {
              const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients;
              if (!clients) return false;
              return Object.values(clients).every(c => typeof c.isActive !== 'function' || !c.isActive());
            }""");
    }

    /** Navigate to a dashboard route (relative to the base url) and wait for the UI to settle. */
    public static void gotoRoute(Page page, String route)
    {
        page.navigate(route.startsWith("/") ? route : "/" + route);
        waitForVaadin(page);
    }

    /**
     * Set the value of a vaadin text-like field (text-field, text-area, ...) and commit it. Enter is only pressed
     * for single-line inputs: in a text area it would add a newline to the value.
     */
    public static void fillField(Locator field, String value)
    {
        // accept the vaadin field element or its input (getByLabel resolves to the input itself)
        Object tag = field.evaluate("el => el.tagName");
        Locator input = "INPUT".equals(tag) || "TEXTAREA".equals(tag) ? field : field.locator("input, textarea").first();
        input.fill(value);
        if ("INPUT".equals(input.evaluate("el => el.tagName")))
        {
            input.press("Enter");
        }
        input.blur();
        input.evaluate("el => el.dispatchEvent(new Event('change', { bubbles: true }))");
    }

    /** Set a vaadin-date-picker by ISO date (yyyy-MM-dd), independent of the display locale. */
    public static void setDatePicker(Locator picker, String isoDate)
    {
        picker.evaluate("(el, v) => { el.value = v; el.dispatchEvent(new CustomEvent('change', { bubbles: true })); }", isoDate);
    }

    /** Set a vaadin-time-picker by HH:mm. */
    public static void setTimePicker(Locator picker, String time)
    {
        picker.evaluate("(el, v) => { el.value = v; el.dispatchEvent(new CustomEvent('change', { bubbles: true })); }", time);
    }

    /** Set a toggle button that exposes {@code aria-pressed} to the wanted state. */
    public static void setPressed(Locator button, boolean pressed)
    {
        if (!String.valueOf(pressed).equals(button.getAttribute("aria-pressed")))
        {
            button.click();
            assertThat(button).hasAttribute("aria-pressed", String.valueOf(pressed));
        }
    }

    /** Set a vaadin-checkbox to the wanted state. */
    public static void setChecked(Locator checkbox, boolean checked)
    {
        if (!Boolean.valueOf(checked).equals(checkbox.evaluate("c => !!c.checked")))
        {
            checkbox.click();
        }
    }

    /**
     * Click a side-menu item by id. The drawer opens on hover of its toggle; items under a parent
     * (e.g. Administration, {@code #adminMenuItem}) are revealed by expanding the parent first.
     */
    public static void openMenuItem(Page page, String itemId)
    {
        openMenuItem(page, itemId, null);
    }

    public static void openMenuItem(Page page, String itemId, String parentItemId)
    {
        Locator item = page.locator("#" + itemId);
        if (!item.isVisible())
        {
            page.locator("vaadin-drawer-toggle").hover();
        }
        if (parentItemId != null && !item.isVisible())
        {
            Locator parent = page.locator("#" + parentItemId);
            parent.waitFor();
            // vaadin-side-nav-item expands via its toggle button
            Locator toggle = parent.locator("button[part='toggle-button']");
            if (toggle.count() > 0)
            {
                toggle.first().click();
            }
            else
            {
                parent.click();
            }
        }
        item.click();
        waitForVaadin(page);
    }

    /**
     * The most recently opened dialog. Vaadin 25 renders dialog content in the light DOM of
     * {@code vaadin-dialog[opened]}, whose host element has no layout box of its own (the overlay renders in the
     * top layer), so the dialog is awaited as attached and visibility is asserted on its content. Earlier
     * versions teleport the content into a {@code vaadin-dialog-overlay}; both are matched.
     */
    public static Locator dialog(Page page)
    {
        Locator dialog = page.locator("vaadin-dialog[opened], vaadin-dialog-overlay[opened]").last();
        dialog.waitFor(ATTACHED);
        return dialog;
    }

    /** Wait for a dialog with the given text (e.g. its title) and return it. */
    public static Locator dialog(Page page, String containingText)
    {
        Locator dialog = page.locator("vaadin-dialog[opened], vaadin-dialog-overlay[opened]")
            .filter(new Locator.FilterOptions().setHasText(containingText)).last();
        dialog.waitFor(ATTACHED);
        assertThat(dialog.getByText(containingText).first()).isVisible();
        return dialog;
    }

    /** Close the dialog with its header close button (AbstractCloseableResizableDialog), else Escape. */
    public static void closeDialog(Page page, Locator dialog)
    {
        Locator close = dialog.locator("vaadin-icon[icon='vaadin:close-small']");
        if (close.count() > 0)
        {
            close.last().click();
        }
        else
        {
            page.keyboard().press("Escape");
        }
        waitForVaadin(page);
    }

    /**
     * Installed in every page (see HarnessSession): records each notification the page shows, with its theme, in
     * {@code window.__harnessNotifications}. The dashboard closes notifications after a few seconds, which a slow
     * machine can spend between an action and the check that follows it; the record keeps them.
     */
    public static final String NOTIFICATION_RECORDER = """
        (() => {
          if (window.__harnessNotifications) return;
          window.__harnessNotifications = [];
          // a card's text can sit in its shadow DOM or be slotted into it: read it as rendered, across both
          const text = node => {
            if (node.nodeType === Node.TEXT_NODE) return node.textContent;
            if (node.nodeType !== Node.ELEMENT_NODE && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE) return '';
            let t = '';
            if (node.shadowRoot) t += ' ' + [...node.shadowRoot.childNodes].map(text).join(' ');
            if (node.localName === 'slot') t += ' ' + node.assignedNodes({ flatten: true }).map(text).join(' ');
            else t += ' ' + [...node.childNodes].map(text).join(' ');
            return t;
          };
          const cards = () => [...document.querySelectorAll('vaadin-notification-card'),
            ...[...document.querySelectorAll('vaadin-notification-container')]
              .flatMap(c => c.shadowRoot ? [...c.shadowRoot.querySelectorAll('vaadin-notification-card')] : [])];
          const seen = new WeakMap();
          // polled, not observed: a mutation observer does not see into shadow roots. Notifications stay for seconds.
          setInterval(() => cards().forEach(card => {
            const t = text(card).replace(/\\s+/g, ' ').trim();
            if (t && seen.get(card) !== t) {
              seen.set(card, t);
              window.__harnessNotifications.push({ text: t, theme: card.getAttribute('theme') || '' });
            }
          }), 100);
        })();
        """;

    /**
     * Assert a notification containing the text has been shown (even if it has closed since), and return its theme
     * (e.g. "error"). Each shown notification satisfies one check: a later check for the same text needs a new one.
     */
    public static String expectNotification(Page page, String text)
    {
        try
        {
            Object theme = page.waitForFunction("t => { const n = window.__harnessNotifications || [];"
                + " const i = n.findIndex(x => x.text.includes(t)); if (i < 0) return false;"
                + " return n.splice(i, 1)[0].theme || ' '; }", normalise(text),
                new Page.WaitForFunctionOptions().setTimeout(Timeouts.millis(
                    org.ikasan.testharness.config.ConfigLoader.get().run().expectTimeoutSeconds() * 1000.0))).jsonValue();
            return String.valueOf(theme).trim();
        }
        catch (com.microsoft.playwright.TimeoutError e)
        {
            throw new AssertionError("no notification containing \"" + text + "\" was shown; shown: "
                + page.evaluate("() => window.__harnessNotifications ? window.__harnessNotifications.map(n => n.text)"
                + " : 'no recorder in ' + location.href")
                + "; on screen: " + page.locator("vaadin-notification-card").allInnerTexts(), e);
        }
    }

    /**
     * Whether a notification containing the text is shown within a few seconds (the recorder polls); like
     * expectNotification, it uses it up. For checks where no notification is also an acceptable outcome.
     */
    public static boolean wasNotified(Page page, String text)
    {
        waitForVaadin(page);
        try
        {
            page.waitForFunction("t => { const n = window.__harnessNotifications || [];"
                + " const i = n.findIndex(x => x.text.includes(t)); if (i < 0) return false; n.splice(i, 1); return true; }",
                normalise(text), new Page.WaitForFunctionOptions().setTimeout(Timeouts.millis(3000)));
            return true;
        }
        catch (com.microsoft.playwright.TimeoutError e)
        {
            return false;
        }
    }

    private static String normalise(String text)
    {
        return text.replaceAll("\\s+", " ").trim();
    }

    /** A button by its visible text inside the given scope (exact match on the accessible name). */
    public static Locator button(Locator scope, String text)
    {
        return scope.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(text).setExact(true));
    }

    /** A Vaadin field (text-field, password-field, text-area, combo-box, ...) by its label inside the scope. */
    public static Locator field(Locator scope, String label)
    {
        return scope.getByLabel(label, new Locator.GetByLabelOptions().setExact(true)).first();
    }

    /** Expand an accordion panel / details by its summary text. */
    public static void expandPanel(Locator scope, String summary)
    {
        scope.locator("vaadin-accordion-heading, vaadin-details-summary")
            .filter(new Locator.FilterOptions().setHasText(summary)).first().click();
        waitForVaadin(scope.page());
    }

    /** Click the confirm button of the open vaadin-confirm-dialog. */
    public static void confirm(Page page)
    {
        // the buttons are light-DOM children of vaadin-confirm-dialog; the overlay's shadow DOM re-slots them,
        // so match the vaadin-button itself, not just any [slot=confirm-button] element
        page.locator("vaadin-confirm-dialog[opened] vaadin-button[slot='confirm-button']").last().click();
        waitForVaadin(page);
    }

    /** Choose an item of a vaadin-combo-box: type to filter, then click the item whose text is exactly the value. */
    public static void selectComboBox(Locator comboBox, String item)
    {
        // Items render lazily in a virtualised overlay, so clicking one is fragile: filter by typing, let the
        // server answer, then take the first filtered item with the keyboard and check it was selected.
        Locator input = comboBox.locator("input").first();
        Page page = comboBox.page();
        input.click();
        input.fill(item);
        waitForVaadin(page);
        page.waitForFunction("c => c.filteredItems && c.filteredItems.length > 0 && !c.loading", comboBox.elementHandle());
        input.press("ArrowDown");
        input.press("Enter");
        waitForVaadin(page);
        assertThat(input).hasValue(java.util.regex.Pattern.compile(".*" + item.replaceAll("[.*+?^${}()|\\[\\]\\\\/]", "\\\\$0") + ".*"));
    }

    /**
     * Whether a component is shown to the user, judged by the dashboard's own visibility: it is rendered and neither
     * it nor an ancestor has {@code hidden} (Vaadin's {@code setVisible(false)}). Unlike {@code isVisible} this does not
     * depend on a collapsed menu or drawer, so it answers "may this user see it?" for menu items and controls alike.
     * Waits for the view to settle first.
     */
    public static boolean isShown(Locator locator)
    {
        waitForVaadin(locator.page());
        if (locator.count() == 0)
        {
            return false;
        }
        return (Boolean) locator.first().evaluate(
            "el => { for (let e = el; e; e = e.parentElement) { if (e.hasAttribute('hidden')) return false; } return true; }");
    }

    /** The text of the (read-only) Ace editor in the scope, e.g. an event dialog's payload. */
    public static String editorText(Locator scope)
    {
        waitForVaadin(scope.page());
        // lit-ace keeps the text in its Ace editor instance
        return (String) scope.locator("lit-ace").first().evaluate("el => el.editor ? el.editor.getValue() : el.value");
    }

    /** Choose an item of a vaadin-select by its text: open it with its value button, then pick the option. */
    public static void selectOption(Locator select, String item)
    {
        Locator button = select.locator("vaadin-select-value-button");
        (button.count() > 0 ? button.first() : select).click();
        select.page().getByRole(com.microsoft.playwright.options.AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(item).setExact(true)).first().click();
        waitForVaadin(select.page());
    }
}
