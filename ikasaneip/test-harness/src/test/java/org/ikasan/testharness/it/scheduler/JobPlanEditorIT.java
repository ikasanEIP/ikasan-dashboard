package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.LoginAs;
import org.ikasan.testharness.pages.DesignerPage;
import org.ikasan.testharness.pages.DesignerPage.Figure;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The visual job plan editor (route 'jobTemplateManagement/&lt;plan&gt;/&lt;child plan&gt;'): a child plan's jobs and
 * dependencies drawn from the stored plan; adding a command execution job (form validation, the job saved, drawn);
 * dependencies drawn between jobs, alone or as an AND grouping (a box around the jobs), saved into the plan; the
 * refusal to save overlapping figures; the prompt when leaving with unsaved changes; and a scheduler reader's view.
 * <p>
 * Works on its own plan, E2E-EDITOR (a copy of E2E-CHAIN: trigger, then okA, then okB), uploaded afresh before each
 * test that changes it. Mutating.
 */
@HarnessTest
@Tag("scheduler")
@Tag("editor")
@Tag("mutating")
class JobPlanEditorIT
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PLAN = "E2E-EDITOR";
    private static final String CHILD = "E2E-EDITOR-1";
    private static final String AGENT = "scheduler-agent";
    private static final String TRIGGER = AGENT + "-trigger";
    private static final String OK_A = AGENT + "-okA";
    private static final String OK_B = AGENT + "-okB";

    @Test
    @Tag("smoke")
    void theEditorDrawsTheChildPlansJobsAndDependencies(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);

        assertThat(editor.figures().stream().map(Figure::id).toList()).as("jobs drawn").contains(TRIGGER, OK_A, OK_B);
        assertThat(editor.figures().stream().map(Figure::text).toList()).as("job labels").contains("trigger", "okA", "okB");
        assertThat(editor.connections()).as("dependencies drawn").containsExactlyInAnyOrder(List.of(TRIGGER, OK_A), List.of(OK_A, OK_B));
        assertThat(Vaadin.button(page.locator("body"), "Go to Parent - " + PLAN)).isVisible();

        JsonNode plan = rawJson(page);
        assertThat(plan.path("name").asString()).isEqualTo(CHILD);
        assertThat(identifiers(plan.path("scheduledJobs"))).containsExactlyInAnyOrder(TRIGGER, OK_A, OK_B);
    }

    @Test
    void aNewCommandJobIsValidatedSavedDrawnAndMadeToDependOnAnother(DesignerPage editor, HarnessSession session, SchedulerApi api,
                                                                    Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        String job = "okC" + (System.currentTimeMillis() % 1_000_000);
        String jobId = AGENT + "-" + job;

        Locator dialog = openNewCommandJob(page);
        // every required field is checked
        dialog.locator("#scheduledJobSaveButton").click();
        for (String message : List.of("Job name is required!", "Agent is required!", "Job description is required!"))
        {
            assertThat(dialog.getByText(message).first()).isVisible();
        }
        Vaadin.fillField(dialog.locator("#jobNameTf"), "has space");
        assertThat(dialog.getByText("Job name cannot contain any whitespace").first()).isVisible();

        fillCommandJob(dialog, job, "echo " + job);
        dialog.locator("#scheduledJobSaveButton").click();
        Vaadin.expectNotification(page, "The job has been saved successfully!");
        editor.awaitFigure(jobId);
        assertThat(saveButton(page).getAttribute("style")).as("Save shows there are unsaved changes").contains("239, 83, 80");

        // the new job lands on a fixed spot: move it clear of the others, then make it depend on okB
        List<Number> okB = editor.bounds(OK_B);
        editor.move(jobId, okB.get(0).intValue() + 300, okB.get(1).intValue());
        editor.connectJobs(OK_B, jobId);
        save(page);

        JsonNode plan = rawJson(page);
        assertThat(identifiers(plan.path("scheduledJobs"))).as("jobs of the saved plan").contains(jobId);
        assertThat(dependencyOf(plan, jobId)).as("the new job's dependency").isEqualTo(Set.of(Set.of(OK_B)));

        // what was saved is what the editor shows next time
        try (BrowserContext other = session.newContext(session.authenticatedState()))
        {
            DesignerPage again = new DesignerPage(other.newPage()).openJobPlan(PLAN, CHILD, jobId);
            assertThat(again.connections()).contains(List.of(OK_B, jobId));
        }
    }

    /** okC depends on (okA AND okB): an AND box drawn around okA and okB, both connected to okC. */
    @Test
    void anAndGroupingMakesAJobDependOnAllTheJobsInIt(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        String jobId = addCommandJob(editor, page);

        // lay out: okA and okB side by side, okC to their right, the AND box around okA and okB only
        List<Number> a = editor.bounds(OK_A);
        int x = a.get(0).intValue(), y = a.get(1).intValue() + 400;
        editor.move(OK_A, x, y);
        editor.move(OK_B, x, y + 250);
        editor.move(jobId, x + 500, y + 120);
        Vaadin.button(page.locator("body"), "Add AND Grouping &&").click();
        Vaadin.waitForVaadin(page);
        String and = editor.figures().stream().map(Figure::id).filter(id -> id.startsWith("AND-")).findFirst().orElseThrow();
        editor.setBounds(and, x - 60, y - 60, 220, 470);
        editor.connectJobs(OK_A, jobId);
        editor.connectJobs(OK_B, jobId);
        save(page);

        assertThat(dependencyOf(rawJson(page), jobId)).as("the new job's dependency").isEqualTo(Set.of(Set.of(OK_A, OK_B)));
    }

    /** Two plain connections into one job, without a box: the job depends on both. */
    @Test
    void aJobConnectedFromTwoJobsDependsOnBoth(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        String jobId = addCommandJob(editor, page);
        List<Number> b = editor.bounds(OK_B);
        editor.move(jobId, b.get(0).intValue() + 300, b.get(1).intValue() + 300);
        editor.connectJobs(OK_A, jobId);
        editor.connectJobs(OK_B, jobId);
        save(page);

        assertThat(dependencyOf(rawJson(page), jobId)).as("the new job's dependency").isEqualTo(Set.of(Set.of(OK_A, OK_B)));
    }

    @Test
    void overlappingFiguresCannotBeSaved(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        // both boxes land on the same spot
        Vaadin.button(page.locator("body"), "Add AND Grouping &&").click();
        Vaadin.button(page.locator("body"), "Add OR Grouping ||").click();
        Vaadin.waitForVaadin(page);
        saveButton(page).click();
        Locator refusal = page.locator("vaadin-confirm-dialog[opened]").last();
        assertThat(refusal).containsText("Cannot save job plan!");
        assertThat(refusal).containsText("The following items overlap");
    }

    @Test
    void leavingWithUnsavedChangesAsksFirst(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        Vaadin.button(page.locator("body"), "Add AND Grouping &&").click();
        Vaadin.waitForVaadin(page);

        Vaadin.openMenuItem(page, "schedulerMenuItem");
        Locator prompt = page.locator("vaadin-confirm-dialog[opened]").last();
        assertThat(prompt).containsText("Your diagram has unsaved changes. Are you sure you want to continue?");
        prompt.locator("vaadin-button[slot='cancel-button']").click();
        Vaadin.waitForVaadin(page);
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*jobTemplateManagement/" + PLAN + "/" + CHILD + "$"));
    }

    /** A connection drawn on the canvas is a change to the plan: Save shows it, leaving asks, and Save keeps it. */
    @Test
    void aConnectionDrawnOnTheCanvasIsAnUnsavedChange(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        editor.connectJobs(TRIGGER, OK_B);
        Vaadin.waitForVaadin(page);
        assertThat(saveButton(page).getAttribute("style")).as("Save shows there are unsaved changes").contains("239, 83, 80");

        Vaadin.openMenuItem(page, "schedulerMenuItem");
        Locator prompt = page.locator("vaadin-confirm-dialog[opened]").last();
        assertThat(prompt).containsText("Your diagram has unsaved changes.");
        prompt.locator("vaadin-button[slot='cancel-button']").click();
        Vaadin.waitForVaadin(page);

        save(page);
        assertThat(dependencyOf(rawJson(page), OK_B)).as("okB's dependency").isEqualTo(Set.of(Set.of(OK_A, TRIGGER)));
    }

    /** Deleting a connection (selecting it and pressing Delete) is an unsaved change, and Save drops the dependency. */
    @Test
    void aDeletedConnectionIsRemovedFromThePlan(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        List<String> pageErrors = new java.util.concurrent.CopyOnWriteArrayList<>();
        page.onPageError(pageErrors::add);
        page.locator("[id^='canvas-viewport-']").first().evaluate("(el, a) => { const c = el.$connector.designer;"
            + " const line = c.getLines().asArray().find(l => l.getSource().getParent().getId() === a[0] && l.getTarget().getParent().getId() === a[1]);"
            + " c.getCommandStack().execute(new draw2d.command.CommandDelete(line)); }", List.of(OK_A, OK_B));
        Vaadin.waitForVaadin(page);
        assertThat(pageErrors).as("errors in the browser").isEmpty();
        assertThat(editor.connections()).doesNotContain(List.of(OK_A, OK_B));
        assertThat(saveButton(page).getAttribute("style")).as("Save shows there are unsaved changes").contains("239, 83, 80");

        save(page);
        assertThat(rawJson(page).path("jobDependencies").valueStream().map(d -> d.path("jobIdentifier").asString()))
            .as("jobs with a dependency").doesNotContain(OK_B);
    }

    @Test
    void aGlobalEventJobIsSavedWithThePlan(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        String job = "harnessGlobal" + (System.currentTimeMillis() % 1_000_000);
        Locator dialog = openNewJob(page, "Global Event Job");
        Vaadin.fillField(dialog.locator("#jobNameTf"), job);
        Vaadin.fillField(dialog.locator("#jobDescriptionTa"), "a global event for the editor tests");
        dialog.locator("#globalEventJobSaveButton").click();
        Vaadin.expectNotification(page, "The job has been saved successfully!");
        editor.awaitText(job);
        String jobId = editor.figures().stream().map(Figure::id).filter(id -> id.endsWith("-" + job)).findFirst().orElseThrow();
        List<Number> b = editor.bounds(OK_B);
        editor.move(jobId, b.get(0).intValue() + 300, b.get(1).intValue() + 300);

        save(page);
        assertThat(identifiers(rawJson(page).path("scheduledJobs"))).as("jobs of the saved plan").contains(jobId);
    }

    /** A scheduled job needs no timezone (it runs in the scheduler's), and saving it is reported as a success. */
    @Test
    void aScheduledJobIsAddedAndItsSaveReportedAsASuccess(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        freshPlan(session, api);
        editor.openJobPlan(PLAN, CHILD, OK_A);
        String job = "harnessCron" + (System.currentTimeMillis() % 1_000_000);
        Locator dialog = openNewJob(page, "Scheduled Job");
        Vaadin.fillField(dialog.locator("#jobNameTf"), job);
        Vaadin.selectComboBox(dialog.locator("#agentCb"), AGENT);
        Vaadin.fillField(dialog.locator("#jobDescriptionTa"), "a scheduled job for the editor tests");
        Vaadin.fillField(dialog.locator("#cronExpressionTf"), "0 0 0 1 1 ? *");
        dialog.locator("#scheduledJobSaveButton").click();

        String theme = Vaadin.expectNotification(page, "The job has been saved successfully!");
        assertThat(theme).as("the success message's style").doesNotContain("error");
        editor.awaitFigure(AGENT + "-" + job);
    }

    /** Links to plans that do not exist, or without a child plan holding jobs, show the page-not-found page. */
    @Test
    void badEditorLinksAreNotFound(Page page)
    {
        for (String route : List.of("jobTemplateManagement/NO-SUCH-PLAN/NO-SUCH-PLAN-1", "jobTemplateManagement/" + PLAN,
            "jobTemplateManagement/" + PLAN + "/NO-SUCH-CHILD", "jobTemplateManagement/" + PLAN + "/" + PLAN))
        {
            Vaadin.gotoRoute(page, route);
            try
            {
                assertThat(page.getByText("That page could not be loaded.").first()).isVisible();
            }
            catch (org.opentest4j.AssertionFailedError e)
            {
                throw new AssertionError("The link '" + route + "' did not show the page-not-found page", e);
            }
        }
    }

    @Test
    @LoginAs("access-reader")
    void aSchedulerReaderSeesThePlanButCannotChangeIt(DesignerPage editor, HarnessSession session, SchedulerApi api, Page page)
    {
        try (BrowserContext admin = session.newContext(session.authenticatedState()))
        {
            new JobPlansPage(admin.newPage()).open().ensureUploaded(PLAN, api);
        }
        editor.openJobPlan(PLAN, CHILD, OK_A);
        Locator body = page.locator("body");
        assertThat(Vaadin.isShown(Vaadin.button(body, "View Raw JSON"))).as("raw JSON offered").isTrue();
        for (String control : List.of("Save", "Add AND Grouping &&", "Add OR Grouping ||", "Add As Context View"))
        {
            assertThat(Vaadin.isShown(Vaadin.button(body, control))).as(control + " offered").isFalse();
        }
        assertThat(Vaadin.isShown(page.locator("vaadin-menu-bar-button").filter(new Locator.FilterOptions().setHasText("Add a Job"))))
            .as("Add a Job offered").isFalse();
    }

    // ------------------------------------------------------------------------------------------------- helpers

    /** Upload E2E-EDITOR afresh (deleting the plan a previous test changed). */
    private static void freshPlan(HarnessSession session, SchedulerApi api)
    {
        try (BrowserContext admin = session.newContext(session.authenticatedState()))
        {
            JobPlansPage plans = new JobPlansPage(admin.newPage()).open();
            if (api.planExists(PLAN))
            {
                plans.delete(PLAN);
            }
            plans.upload(PLAN);
        }
    }

    private static Locator openNewCommandJob(Page page)
    {
        return openNewJob(page, "Command Execution Job");
    }

    /** Add a Job > New Job > Job Type > the type: returns the job's dialog (titled as the type). */
    private static Locator openNewJob(Page page, String type)
    {
        page.locator("vaadin-menu-bar-button").filter(new Locator.FilterOptions().setHasText("Add a Job")).first().click();
        for (String item : List.of("New Job", "Job Type", type))
        {
            page.getByRole(com.microsoft.playwright.options.AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(item).setExact(true))
                .first().click();
        }
        return Vaadin.dialog(page, type);
    }

    private static void fillCommandJob(Locator dialog, String job, String commandLine)
    {
        Vaadin.fillField(dialog.locator("#jobNameTf"), job);
        Vaadin.selectComboBox(dialog.locator("#agentCb"), AGENT);
        Vaadin.fillField(dialog.locator("#jobDescriptionTa"), "added by the editor tests");
        dialog.locator("#commandLineTa").click();
        dialog.page().keyboard().type(commandLine);
        Vaadin.waitForVaadin(dialog.page());
    }

    /** Add a command execution job with a unique name and return its identifier. */
    private static String addCommandJob(DesignerPage editor, Page page)
    {
        String job = "okC" + (System.currentTimeMillis() % 1_000_000);
        Locator dialog = openNewCommandJob(page);
        fillCommandJob(dialog, job, "echo " + job);
        dialog.locator("#scheduledJobSaveButton").click();
        Vaadin.expectNotification(page, "The job has been saved successfully!");
        editor.awaitFigure(AGENT + "-" + job);
        return AGENT + "-" + job;
    }

    private static Locator saveButton(Page page)
    {
        return Vaadin.button(page.locator("body"), "Save");
    }

    private static void save(Page page)
    {
        saveButton(page).click();
        Vaadin.expectNotification(page, "Job plan saved");
    }

    /** The child plan as the editor holds it, from View Raw JSON. */
    private static JsonNode rawJson(Page page)
    {
        Vaadin.button(page.locator("body"), "View Raw JSON").click();
        Locator dialog = page.locator("vaadin-dialog[opened]:has(lit-ace)").last();
        dialog.locator("lit-ace").first().waitFor();
        JsonNode plan = JSON.readTree(Vaadin.editorText(dialog));
        Vaadin.closeDialog(page, dialog);
        return plan;
    }

    private static Set<String> identifiers(JsonNode jobs)
    {
        return jobs.valueStream().map(j -> j.path("identifier").asString()).collect(Collectors.toSet());
    }

    /**
     * A job's dependency as sets of the jobs that must all complete (one set per OR alternative): an AND of okA and
     * okB is {{okA, okB}}.
     */
    private static Set<Set<String>> dependencyOf(JsonNode plan, String jobId)
    {
        JsonNode dependency = plan.path("jobDependencies").valueStream().filter(d -> jobId.equals(d.path("jobIdentifier").asString()))
            .findFirst().orElseThrow(() -> new AssertionError("no dependency for " + jobId + " in " + plan.path("jobDependencies")));
        JsonNode grouping = dependency.path("logicalGrouping");
        if (grouping.path("and").isArray() && !grouping.path("and").isEmpty())
        {
            return Set.of(identifiers(grouping.path("and")));
        }
        if (grouping.path("or").isArray() && !grouping.path("or").isEmpty())
        {
            return grouping.path("or").valueStream().map(o -> Set.of(o.path("identifier").asString())).collect(Collectors.toSet());
        }
        return Set.of();
    }
}
