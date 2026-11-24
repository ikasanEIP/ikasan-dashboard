package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Creating, enabling, disabling and deleting job plans. Scenarios: expectations/scheduler/job-plans.yaml. */
@HarnessTest
@Tag("scheduler")
@Tag("mutating")
class JobPlanLifecycleIT
{
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36).toUpperCase();

    @Test
    @SuppressWarnings("unchecked")
    void newJobPlanFieldsAreValidated(JobPlansPage jobPlans, Page page)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "new job plan validation");
        Locator dialog = jobPlans.open().openNewPlanDialog();

        page.locator("#saveNewContextButton").click();
        Vaadin.waitForVaadin(page);

        ((Map<String, Object>) s.expect().get("errors")).forEach((label, error) ->
            assertThat(dialog.getByText(String.valueOf(error)).first()).isVisible());
        Vaadin.closeDialog(page, dialog);
    }

    /** Fill the New Job Plan dialog from the scenario input (duration fields missing from the input stay empty). */
    private static Locator fillNewPlan(JobPlansPage jobPlans, Page page, Map<String, Object> in, String name)
    {
        Locator dialog = jobPlans.open().openNewPlanDialog();

        Vaadin.fillField(Vaadin.field(dialog, "Job Plan Name"), name);
        Vaadin.fillField(Vaadin.field(dialog, "Job Plan description"), String.valueOf(in.get("description")));
        Vaadin.fillField(Vaadin.field(dialog, "Time Window Start Cron Expression"), String.valueOf(in.get("cron")));
        for (String field : java.util.List.of("Days", "Hours", "Minutes"))
        {
            Object value = in.get("duration" + field);
            if (value != null)
            {
                Vaadin.fillField(Vaadin.field(dialog, "Duration " + field), String.valueOf(value));
            }
        }
        Vaadin.selectComboBox(page.locator("#timezoneCb"), String.valueOf(in.get("timezone")));
        return dialog;
    }

    /** Create a plan with the New Job Plan dialog and wait for it in the grid. */
    private static void createPlan(JobPlansPage jobPlans, Page page, Scenario s, String name)
    {
        Locator dialog = fillNewPlan(jobPlans, page, s.input(), name);
        page.locator("#saveNewContextButton").click();

        Vaadin.expectNotification(page, String.valueOf(s.expect().get("created")));
        assertThat(dialog).isHidden();
        jobPlans.refresh();
        jobPlans.grid().waitForText(name);
    }

    /**
     * The plan's state as the user sees it (ContextTemplateWidget): the current state's button is coloured (green
     * Enabled / red Disabled) and disabled, and the other button can be clicked.
     */
    private static void assertState(JobPlansPage jobPlans, String plan, boolean enabled)
    {
        jobPlans.refresh();
        Grid grid = jobPlans.grid();
        java.util.regex.Pattern coloured = java.util.regex.Pattern.compile(".*background-color.*");
        Locator current = grid.inRowButton(plan, enabled ? "Enabled" : "Disabled");
        Locator other = grid.inRowButton(plan, enabled ? "Disabled" : "Enabled");
        assertThat(current).hasAttribute("style", coloured);
        assertThat(current).isDisabled();
        assertThat(other).not().hasAttribute("style", coloured);
        assertThat(other).isEnabled();
    }

    /** New plans are created disabled ("new context templates should be disabled", ContextTemplateDialog#saveNew). */
    @Test
    void newJobPlanIsCreatedDisabled(JobPlansPage jobPlans, Page page)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "job plan lifecycle");
        String name = s.input().get("name") + "-NEW-" + SUFFIX;
        createPlan(jobPlans, page, s, name);

        assertState(jobPlans, name, false);
    }

    /** New plan -> delete. */
    @Test
    void newJobPlanCanBeDeleted(JobPlansPage jobPlans, Page page, SchedulerApi api)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "job plan lifecycle");
        String name = s.input().get("name") + "-DEL-" + SUFFIX;
        createPlan(jobPlans, page, s, name);

        jobPlans.delete(name);
        jobPlans.refresh();
        jobPlans.grid().waitForNoText(name);
        org.assertj.core.api.Assertions.assertThat(api.planExists(name)).as("plan exists after delete").isFalse();
    }

    /** A plan without jobs can be enabled: there is nothing to provision (JobProvisionServiceImpl#provisionJobs). */
    @Test
    void newEmptyJobPlanCanBeEnabled(JobPlansPage jobPlans, Page page)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "job plan lifecycle");
        String name = s.input().get("name") + "-EMPTY-" + SUFFIX;
        createPlan(jobPlans, page, s, name);

        jobPlans.setEnabled(name, true);
        assertState(jobPlans, name, true);
    }

    /** Uploaded plan (enabled) -> disable -> enable. */
    @Test
    void jobPlanCanBeDisabledAndEnabled(JobPlansPage jobPlans, SchedulerApi api)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "job plan can be disabled and enabled");
        String plan = String.valueOf(s.input().get("plan"));
        jobPlans.open().ensureUploaded(plan, api);
        assertState(jobPlans, plan, true);

        jobPlans.setEnabled(plan, false);
        assertState(jobPlans, plan, false);

        jobPlans.setEnabled(plan, true);
        assertState(jobPlans, plan, true);
    }

    /** A plan's name is its id: a new plan under an existing name is refused and the existing plan is unchanged. */
    @Test
    void newJobPlanCannotReuseAnExistingName(JobPlansPage jobPlans, Page page, SchedulerApi api)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "a new job plan cannot reuse an existing name");
        String existing = String.valueOf(s.input().get("name"));
        jobPlans.open().ensureUploaded(existing, api);

        Locator dialog = fillNewPlan(jobPlans, page, s.input(), existing);
        page.locator("#saveNewContextButton").click();

        String error = String.valueOf(s.expect().get("error"));
        Vaadin.expectNotification(page, error);
        assertThat(dialog.getByText(error).first()).isVisible();   // on the name field, and the dialog stays open
        Vaadin.closeDialog(page, dialog);

        jobPlans.refresh();
        assertThat(jobPlans.grid().inRow(existing, "*").first()).isVisible();
        org.assertj.core.api.Assertions.assertThat(jobPlans.grid().rows().stream()
                .filter(r -> existing.equals(r.get("Job Plan Name"))).map(r -> r.get("Description")).toList())
            .as("the existing plan").containsExactly(String.valueOf(s.expect().get("description")));
    }

    /** An empty duration field is a validation error (it used to throw a NullPointerException in validate()). */
    @Test
    void emptyDurationIsAValidationError(JobPlansPage jobPlans, Page page)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "an empty duration is a validation error");
        Locator dialog = fillNewPlan(jobPlans, page, s.input(), s.input().get("name") + "-" + SUFFIX);
        page.locator("#saveNewContextButton").click();

        Vaadin.expectNotification(page, String.valueOf(s.expect().get("notification")));
        assertThat(dialog.getByText(String.valueOf(s.expect().get("fieldError"))).first()).isVisible();
        Vaadin.closeDialog(page, dialog);
    }

    @Test
    void scheduledJobsCanBeDisabledAndEnabled(JobPlansPage jobPlans, Page page, SchedulerApi api)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "scheduled jobs can be disabled and enabled");
        String plan = String.valueOf(s.input().get("plan"));
        jobPlans.open().ensureUploaded(plan, api);

        jobPlans.grid().inRow(plan, "vaadin-icon[title^='Disable scheduled jobs']").click();
        Vaadin.confirm(page);
        Vaadin.expectNotification(page, String.valueOf(s.expect().get("disabled")));

        jobPlans.refresh();
        jobPlans.grid().inRow(plan, "vaadin-icon[title^='Enable scheduled jobs']").click();
        Vaadin.confirm(page);
        Vaadin.expectNotification(page, String.valueOf(s.expect().get("enabled")));
    }
}
