package org.ikasan.testharness.pages.scheduler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * A job plan instance (route {@code contextInstance/<id>_scheduledContextInstance}, ContextInstanceWidget):
 * header, tabs, the Job Instances grid with its per-job actions, and the instance-level Actions popup.
 */
public class ContextInstancePage
{
    /** Per-job action icons in the Job Instances grid, by title. */
    public static final String SKIP = "Skip Job";
    public static final String ENABLE = "Enable Job";
    public static final String HOLD = "Hold Job";
    public static final String RELEASE = "Release Job";
    public static final String DISABLE = "Disable Job";
    public static final String REINSTATE = "Reinstate Disabled Job";
    public static final String SUBMIT = "Submit Job";
    public static final String RESET = "Reset Job";
    public static final String ACKNOWLEDGE_ERROR = "Acknowledge Error";

    private final Page page;
    private final String id;

    public ContextInstancePage(Page page, String contextInstanceId)
    {
        this.page = page;
        this.id = contextInstanceId;
    }

    /** contextInstance/&lt;id&gt;_scheduledContextInstance[/...] -> id */
    public static String idFromUrl(String url)
    {
        return url.replaceAll(".*/contextInstance/(job/)?", "").replaceAll("_scheduledContextInstance.*", "");
    }

    public static ContextInstancePage open(Page page, String contextInstanceId)
    {
        Vaadin.gotoRoute(page, "contextInstance/" + contextInstanceId + "_scheduledContextInstance");
        return new ContextInstancePage(page, contextInstanceId);
    }

    public Page page()
    {
        return page;
    }

    public String id()
    {
        return id;
    }

    // ------------------------------------------------------------------------------------------------- tabs

    public void tab(String tabId)
    {
        page.locator("#" + tabId).click();
        Vaadin.waitForVaadin(page);
    }

    /** The Job Instances tab's grid, refreshed. */
    public Grid jobs()
    {
        tab("instanceJobsTab");
        page.locator("#jobInstanceGridRefreshButton").click();
        Vaadin.waitForVaadin(page);
        Grid grid = new Grid(page.locator("#jobInstanceGrid"));
        grid.waitForLoaded();
        return grid;
    }

    /** All rows of the Job Instances grid (refreshed first). */
    public List<Map<String, String>> jobRows()
    {
        return jobs().rows();
    }

    /** Status shown for a job in the Job Instances grid. */
    public String jobStatus(String jobName)
    {
        return jobRows().stream().filter(r -> jobName.equals(r.get("Job Name"))).map(r -> r.get("Status"))
            .findFirst().orElseThrow(() -> new AssertionError("No job " + jobName + " in instance " + id));
    }

    /** Click a job's action icon in the Job Instances grid; confirms when a confirmation is asked. */
    public void jobAction(String jobName, String iconTitle)
    {
        Grid grid = jobs();
        grid.waitForText(jobName);
        grid.inRow(jobName, "vaadin-icon[title='" + iconTitle + "']").click();
        Vaadin.waitForVaadin(page);
        if (page.locator("vaadin-confirm-dialog[opened]").count() > 0)
        {
            Vaadin.confirm(page);
        }
    }

    // ------------------------------------------------------------------------------------------------- instance actions

    /** Open the Actions popup and click one of its buttons by id (holdContextButton, resetContextButton, ...). */
    public void instanceAction(String buttonId)
    {
        page.locator("#actionsButton").click();
        Locator button = page.locator("#" + buttonId);
        assertThat(button).isVisible();
        button.click();
        Vaadin.waitForVaadin(page);
        // most instance actions ask for confirmation first
        Locator confirm = page.locator("vaadin-confirm-dialog[opened]");
        try
        {
            confirm.first().waitFor(new Locator.WaitForOptions().setState(
                com.microsoft.playwright.options.WaitForSelectorState.ATTACHED).setTimeout(2000));
            Vaadin.confirm(page);
        }
        catch (com.microsoft.playwright.TimeoutError noConfirmation)
        {
            // the action needed no confirmation
        }
    }

    /** Text of the instance header (status counters, dates). */
    public String headerText()
    {
        return page.locator("h4").first().textContent();
    }

    // ------------------------------------------------------------------------------------------------- job dialogs

    /** Submit a command execution job by hand (Submit Job -> "Command Execution Job Submission" -> Submit Job). */
    public void submitCommandJob(String jobName)
    {
        jobAction(jobName, SUBMIT);
        Locator dialog = Vaadin.dialog(page, "Command Execution Job Submission");
        Vaadin.button(dialog, "Submit Job").click();
        Vaadin.waitForVaadin(page);
        if (page.locator("vaadin-confirm-dialog[opened]").count() > 0)
        {
            Vaadin.confirm(page);
        }
        Vaadin.expectNotification(page, "Job submitted successfully!");
    }

    /** Acknowledge a job's error (Acknowledge Error dialog: Ticket ID, Reason, Acknowledge). */
    public void acknowledgeError(String jobName, String ticketId, String reason)
    {
        jobAction(jobName, ACKNOWLEDGE_ERROR);
        Locator dialog = Vaadin.dialog(page, "Acknowledge Error");
        Vaadin.fillField(Vaadin.field(dialog, "Ticket ID"), ticketId);
        Vaadin.fillField(Vaadin.field(dialog, "Reason"), reason);
        Vaadin.button(dialog, "Acknowledge").click();
        Vaadin.expectNotification(page, "The error has been successfully acknowledged!");
    }

    /** The visible action icon titles of a job's row. */
    public List<String> visibleJobActions(String jobName)
    {
        Grid grid = jobs();
        grid.waitForText(jobName);
        return grid.attributesInRow(jobName, "vaadin-icon[title]", "title");
    }

    /**
     * Open a job's log file (real agent: the job's captured output) and return the log's rendered text. The viewer is
     * an Ace editor (lit-ace) inside a shadow root, so the dialog's own text does not include it; CSS selectors pierce
     * the shadow root.
     */
    public Locator viewLog(String jobName)
    {
        jobAction(jobName, "View log file");
        // the newest editor on the page: the viewer's editor is not under the vaadin-dialog element itself
        Locator log = page.locator("lit-ace .ace_content").last();
        log.waitFor();
        return log;
    }
}
