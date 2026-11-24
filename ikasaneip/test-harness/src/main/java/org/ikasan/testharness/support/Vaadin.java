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

    /** Assert a notification containing the text is shown (notifications render as vaadin-notification-card). */
    public static void expectNotification(Page page, String text)
    {
        assertThat(page.locator("vaadin-notification-card").filter(new Locator.FilterOptions().setHasText(text)).first())
            .isVisible();
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
}
