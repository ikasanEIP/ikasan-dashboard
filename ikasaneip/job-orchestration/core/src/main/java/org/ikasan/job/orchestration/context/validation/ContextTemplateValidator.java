package org.ikasan.job.orchestration.context.validation;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to provide validation of context templates.
 */
public class ContextTemplateValidator {
    private StringBuffer errorReport = new StringBuffer("The context template is invalid!\n");
    private boolean inError = false;

    /**
     * Method to validate a context template.
     *
     * @param contextTemplate
     * @throws InvalidContextTemplateException
     */
    public void validate(ContextTemplate contextTemplate) throws InvalidContextTemplateException {
        this.assertThatContextJobsPresentInContextForAllJobDependencies(contextTemplate);

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
        this.assertThatContextJobsPresentInContextForAllJobDependencies(contextTemplate);

        if(contextTemplate.getContexts() != null) {
            contextTemplate.getContexts().forEach(contextTemplate1 -> validateChildContext(contextTemplate1));
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
            && contextTemplate.getJobLocksMap() != null && !contextTemplate.getJobLocksMap().isEmpty()) {
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
        if(contextTemplate.getJobLocksMap() != null) {
            contextTemplate.getJobLocksMap().entrySet().forEach(entry -> {
                boolean jobExists = entry.getValue().getJobs().values().stream().flatMap(Collection::stream)
                    .filter(job -> contextTemplate.getScheduledJobs().stream()
                        .filter(schedulerJob -> job.getIdentifier().equals(schedulerJob.getIdentifier()))
                        .findFirst()
                        .isPresent())
                    .collect(Collectors.toList())
                    .size() == entry.getValue().getJobs().size();

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

//        if(contextTemplate.getTimeWindowEnd() == null || contextTemplate.getTimeWindowEnd().isEmpty()) {
//            this.inError = true;
//            this.errorReport.append("Context[").append(contextTemplate.getName())
//                .append("] must contain a time window end cron expression.\n");
//        }
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

//        if(contextTemplate.getTimeWindowEnd() != null && !contextTemplate.getTimeWindowEnd().isEmpty()) {
//            this.inError = true;
//            this.errorReport.append("Context[").append(contextTemplate.getName())
//                .append("] must not contain a time window end cron expression. This field can only be present in the root context.\n");
//        }
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

    /**
     * Context parameters can only be present in the parent context. This method assists in asserting this constraint.
     *
     * @param contextTemplate
     */
    private void assertThatContextJobsPresentInContextForAllJobDependencies(ContextTemplate contextTemplate) {
        Set<String> jobs = new HashSet<>();

        if(contextTemplate.getJobDependencies() != null) {
            contextTemplate.getJobDependencies().forEach(jobDependency -> {
                jobs.add(jobDependency.getJobIdentifier());
                if(jobDependency.getLogicalGrouping() != null) {
                    this.manageLogicalGrouping(jobDependency.getLogicalGrouping(), jobs);
                }
            });
        }

        Set<String> contextJobs = new HashSet<>();
        contextTemplate.getScheduledJobs().forEach(schedulerJob -> contextJobs.add(schedulerJob.getIdentifier()));

        contextJobs.removeAll(jobs);

        if(!contextJobs.isEmpty()) {
            this.inError = true;
            contextJobs.forEach(jobIdentifier -> this.errorReport.append("Context[").append(contextTemplate.getName())
                .append("] The following job [").append(jobIdentifier).append("] appears in the scheduler jobs collection" +
                    ", but is not defined in any job dependencies.\n"));
        }

        Set<String> contextJobs2 = new HashSet<>();
        contextTemplate.getScheduledJobs().forEach(schedulerJob -> contextJobs2.add(schedulerJob.getIdentifier()));

        jobs.removeAll(contextJobs2);
        if(!jobs.isEmpty()) {
            this.inError = true;
            jobs.forEach(jobIdentifier -> this.errorReport.append("Context[").append(contextTemplate.getName())
                .append("] The following job [").append(jobIdentifier).append("] appears in a job dependency" +
                    ", but is not defined in the scheduler job collection.\n"));
        }
    }

    private void manageLogicalGrouping(LogicalGrouping logicalGrouping, Set<String> jobs) {
        if(logicalGrouping.getAnd() != null) {
            logicalGrouping.getAnd().forEach(and -> {
                jobs.add(and.getIdentifier());
                if(logicalGrouping.getLogicalGrouping() != null) {
                    manageLogicalGrouping(logicalGrouping.getLogicalGrouping(), jobs);
                }
            });
        }

        if(logicalGrouping.getOr() != null) {
            logicalGrouping.getOr().forEach(or -> {
                jobs.add(or.getIdentifier());
                if(logicalGrouping.getLogicalGrouping() != null) {
                    manageLogicalGrouping(logicalGrouping, jobs);
                }
            });
        }

        if(logicalGrouping.getNot() != null) {
            logicalGrouping.getNot().forEach(not -> {
                jobs.add(not.getIdentifier());
                if(logicalGrouping.getLogicalGrouping() != null) {
                    manageLogicalGrouping(logicalGrouping, jobs);
                }
            });
        }
    }
}
