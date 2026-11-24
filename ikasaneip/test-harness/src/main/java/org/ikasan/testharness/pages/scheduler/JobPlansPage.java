package org.ikasan.testharness.pages.scheduler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.scheduler.JobPlans;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The Scheduler view's Job Plans tab (ContextTemplateWidget): the job plan grid and its actions. */
public class JobPlansPage
{
    /** Row action icons, by their title (IconDecorator tooltips). */
    public static final String DELETE = "Delete Job Plan";
    public static final String CREATE_INSTANCE = "Create a new instance of the job plan.";
    public static final String OPEN_IN_NEW_WINDOW = "Open in new window";
    public static final String EXPORT = "Export jobs and associated artifacts";
    public static final String EXPORT_WITH_TOKENS = "Export jobs and associated artifacts with tokens";

    private final Page page;

    public JobPlansPage(Page page)
    {
        this.page = page;
    }

    public Page page()
    {
        return page;
    }

    public JobPlansPage open()
    {
        Vaadin.gotoRoute(page, "scheduler");
        page.locator("#contextTemplateTab").click();
        Vaadin.waitForVaadin(page);
        assertThat(page.locator("#jobPlanGrid")).isAttached();
        grid().waitForLoaded();
        return this;
    }

    public Grid grid()
    {
        return new Grid(page.locator("#jobPlanGrid"));
    }

    public void refresh()
    {
        page.locator("#refreshJobPlanGridButton").click();
        Vaadin.waitForVaadin(page);
        grid().waitForLoaded();
    }

    /** Upload a job plan fixture (data/jobplans/&lt;plan&gt;) and confirm provisioning. */
    public void upload(String plan)
    {
        upload(JobPlans.zip(plan), "Successfully provisioned context!");
        grid().waitForText(plan);
    }

    /** Upload a file and expect the given notification (success or error). */
    public void upload(Path file, String expectedNotification)
    {
        Locator dialog = openUploadDialog();
        dialog.locator("#jobPlanUpload input[type=file]").setInputFiles(file);
        // the upload completes when the file entry is marked complete
        assertThat(dialog.locator("vaadin-upload-file[complete]").first()).isAttached();
        page.locator("#jobPlanUploadSaveButton").click();
        Vaadin.waitForVaadin(page);
        if (page.locator("vaadin-confirm-dialog[opened]").count() > 0)
        {
            Vaadin.confirm(page);
        }
        Vaadin.expectNotification(page, expectedNotification);
    }

    public Locator openUploadDialog()
    {
        page.locator("#uploadJobPlanButton").click();
        return Vaadin.dialog(page, "Upload Job Plan");
    }

    /** Upload the fixture unless a plan of that name already exists. */
    public void ensureUploaded(String plan, SchedulerApi api)
    {
        if (!api.planExists(plan))
        {
            upload(plan);
        }
    }

    /**
     * Click a row action icon of a plan by its title. The grid is not filtered first: on Mongo the name filter goes
     * through Atlas Search, whose index lags a just-uploaded plan; the harness's plans all fit on screen.
     */
    public void rowAction(String plan, String iconTitle)
    {
        Grid grid = grid();
        grid.waitForText(plan);
        grid.inRow(plan, "vaadin-icon[title='" + iconTitle + "']").click();
        Vaadin.waitForVaadin(page);
    }

    /**
     * Create a new instance of the plan with the grid's + action. The dashboard opens the instance in a new tab;
     * that tab is returned as a page object.
     *
     * @param updateParameters tick "Update Parameters Prior to Initiating New Job Plan Instance"
     */
    public ContextInstancePage createInstance(String plan, boolean updateParameters)
    {
        rowAction(plan, CREATE_INSTANCE);
        if (updateParameters)
        {
            page.locator("vaadin-confirm-dialog[opened] vaadin-checkbox").first().click();
        }
        Page instance = page.context().waitForPage(() -> Vaadin.confirm(page));
        instance.waitForLoadState();
        Vaadin.waitForVaadin(instance);
        return new ContextInstancePage(instance, ContextInstancePage.idFromUrl(instance.url()));
    }

    /** Enable or disable the plan with its Enabled/Disabled buttons (confirm, then the progress dialog). */
    public void setEnabled(String plan, boolean enabled)
    {
        Grid grid = grid();
        grid.waitForText(plan);
        grid.inRowButton(plan, enabled ? "Enabled" : "Disabled").click();
        Vaadin.confirm(page);
        Vaadin.waitForVaadin(page);
        assertThat(page.locator("#pblbl")).hasCount(0);
    }

    /** Delete the plan (confirm, progress) and wait for the success notification. */
    public void delete(String plan)
    {
        rowAction(plan, DELETE);
        Vaadin.confirm(page);
        Vaadin.expectNotification(page, "The context template has been deleted successfully.");
    }

    /** Open a plan's export dialog (with or without tokens) and download one of its bundles by link text. */
    public com.microsoft.playwright.Download export(String plan, boolean withTokens, String linkText)
    {
        rowAction(plan, withTokens ? EXPORT_WITH_TOKENS : EXPORT);
        Locator dialog = Vaadin.dialog(page, linkText);
        com.microsoft.playwright.Download download = page.waitForDownload(() ->
            dialog.locator("a").filter(new Locator.FilterOptions().setHasText(linkText)).first().click());
        Vaadin.closeDialog(page, dialog);
        return download;
    }

    /** Open the New Job Plan dialog. */
    public Locator openNewPlanDialog()
    {
        page.locator("#newJobPlanButton").click();
        return Vaadin.dialog(page, "New Job Plan Template");
    }
}
