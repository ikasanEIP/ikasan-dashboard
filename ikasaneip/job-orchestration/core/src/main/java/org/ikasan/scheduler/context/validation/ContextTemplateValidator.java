package org.ikasan.scheduler.context.validation;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.stream.Collectors;

/**
 * Class to provide validation of context templates.
 */
public class ContextTemplateValidator {
    private StringBuffer errorReport = new StringBuffer();
    private boolean inError = false;

    /**
     * Method to validate a context template.
     *
     * @param contextTemplate
     * @throws InvalidContextTemplateException
     */
    public void validate(ContextTemplate contextTemplate) throws InvalidContextTemplateException {
        this.assertThatContextsAndScheduledJobsCannotBePresentAtSameLevel(contextTemplate);
        this.assertThatContextsJobLocksCannotBeAtTheSameLevel(contextTemplate);
        this.assertJobLocksContainOnlyJobsAssociatedWithTheContext(contextTemplate);
        this.assertThatStartAndEndTimeWindowArePresent(contextTemplate);

        if(contextTemplate.getContexts() != null) {
            contextTemplate.getContexts().forEach(contextTemplate1 -> validateChildContext(contextTemplate1));
        }

        if(this.inError) {
            throw new InvalidContextTemplateException(errorReport.toString());
        }
    }

    /**
     * Helper method validate all child contexts.
     *
     * @param contextTemplate
     */
    private void validateChildContext(ContextTemplate contextTemplate) {
        this.assertThatContextsAndScheduledJobsCannotBePresentAtSameLevel(contextTemplate);
        this.assertThatContextsJobLocksCannotBeAtTheSameLevel(contextTemplate);
        this.assertThatStartAndEndTimeWindowAreNotPresent(contextTemplate);
        this.assertJobLocksContainOnlyJobsAssociatedWithTheContext(contextTemplate);
        this.assertThatContextParametersNotPresent(contextTemplate);

        if(contextTemplate.getContexts() != null) {
            contextTemplate.getContexts().forEach(contextTemplate1 -> validateChildContext(contextTemplate1));
        }
    }

    /**
     * A context either contains other contexts or scheduler jobs, but cannot contain both. This method
     * is responsible for asserting this constraint.
     *
     * @param contextTemplate
     */
    private void assertThatContextsAndScheduledJobsCannotBePresentAtSameLevel(ContextTemplate contextTemplate) {
        if(contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()
            && contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            this.inError = true;
            this.errorReport.append("Context[").append(contextTemplate.getName()).append("] contains both scheduled jobs and contexts.")
                .append(" A context can only contain either scheduled jobs or contexts, but not both.\n");
        }
    }

    /**
     * A context cannot contain contexts or job locks, but cannot contain both. This method
     * is responsible for asserting this constraint.
     *
     * @param contextTemplate
     */
    private void assertThatContextsJobLocksCannotBeAtTheSameLevel(ContextTemplate contextTemplate) {
        if(contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()
            && contextTemplate.getJobLocks() != null && !contextTemplate.getJobLocks().isEmpty()) {
            this.inError = true;
            this.errorReport.append("Context[").append(contextTemplate.getName()).append("] contains both jobs locks and contexts.")
                .append(" A context cannot contain contexts and job locks.\n");
        }
    }

    /**
     * A context cannot contain contexts or job locks, but cannot contain both. This method
     * is responsible for asserting this constraint.
     *
     * @param contextTemplate
     */
    private void assertJobLocksContainOnlyJobsAssociatedWithTheContext(ContextTemplate contextTemplate) {
        if(contextTemplate.getJobLocks() != null) {
            contextTemplate.getJobLocks().entrySet().forEach(entry -> {
                boolean jobExists = entry.getValue().stream()
                    .filter(job -> contextTemplate.getScheduledJobs().stream()
                        .filter(schedulerJob -> job.getIdentifier().equals(schedulerJob.getIdentifier()))
                        .findFirst()
                        .isPresent())
                    .collect(Collectors.toList())
                    .size() == entry.getValue().size();

                if(!jobExists) {
                    this.inError = true;
                    this.errorReport.append("Context[").append(contextTemplate.getName()).append("] contains jobs locks and and jobs, however there ")
                        .append("are job identifiers defined in job lock[").append(entry.getKey())
                        .append("] that do not reference scheduler jobs defined within the context.\n");
                }
            });
        }
    }

    /**
     * The parent context must contain the cron expressions for the start and end time window. This method is responsible for
     * asserting this constraint.
     *
     * @param contextTemplate
     */
    private void assertThatStartAndEndTimeWindowArePresent(ContextTemplate contextTemplate) {
        if(contextTemplate.getTimeWindowStart() == null || contextTemplate.getTimeWindowStart().isEmpty()) {
            this.inError = true;
            this.errorReport.append("Context[").append(contextTemplate.getName())
                .append("] must contain a time window start cron expression.\n");
        }

        if(contextTemplate.getTimeWindowEnd() == null || contextTemplate.getTimeWindowEnd().isEmpty()) {
            this.inError = true;
            this.errorReport.append("Context[").append(contextTemplate.getName())
                .append("] must contain a time window end cron expression.\n");
        }
    }

    /**
     * Child contexts must NOT contain the cron expressions for the start and end time window. This method is responsible for
     * asserting this constraint.
     *
     * @param contextTemplate
     */
    private void assertThatStartAndEndTimeWindowAreNotPresent(ContextTemplate contextTemplate) {
        if(contextTemplate.getTimeWindowStart() != null && !contextTemplate.getTimeWindowStart().isEmpty()) {
            this.inError = true;
            this.errorReport.append("Context[").append(contextTemplate.getName())
                .append("] must not contain a time window start cron expression. This field can only be present in the root context.\n");
        }

        if(contextTemplate.getTimeWindowEnd() != null && !contextTemplate.getTimeWindowEnd().isEmpty()) {
            this.inError = true;
            this.errorReport.append("Context[").append(contextTemplate.getName())
                .append("] must not contain a time window end cron expression. This field can only be present in the root context.\n");
        }
    }

    /**
     * Context parameters can only be present in the parent context. This method assists in asserting this constraint.
     *
     * @param contextTemplate
     */
    private void assertThatContextParametersNotPresent(ContextTemplate contextTemplate) {
        if(contextTemplate.getContextParameters() != null && !contextTemplate.getContextParameters().isEmpty()) {
            this.inError = true;
            this.errorReport.append("Context[").append(contextTemplate.getName())
                .append("] must not contain any context parameters. Context parameters can only be present in the root context.\n");
        }
    }
}
