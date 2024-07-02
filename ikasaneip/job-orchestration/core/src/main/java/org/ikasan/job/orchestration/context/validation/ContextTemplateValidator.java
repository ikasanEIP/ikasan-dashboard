package org.ikasan.job.orchestration.context.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class to provide validation of context templates.
 */
public class ContextTemplateValidator {
    private StringBuffer errorReport = new StringBuffer("The context template is invalid!\n");
    private List<ContextError> errors = new ArrayList<>();
    private boolean inError = false;
    private  List<String> childContextName = new ArrayList<>();
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    /**
     * Method to validate a context template.
     *
     * @param contextTemplate
     * @throws InvalidContextTemplateException
     */
    public void validate(ContextTemplate contextTemplate) throws InvalidContextTemplateException {
        this.inError = false;
        this.errors = new ArrayList<>();
        this.errorReport = new StringBuffer("The context template is invalid!\n");

        this.assertThatContextJobsPresentInContextForAllJobDependencies(contextTemplate);

        childContextName.add(contextTemplate.getName());
        if(contextTemplate.getContexts() != null) {
            contextTemplate.getContexts().forEach(contextTemplate1 -> validateChildContext(contextTemplate1));
        }

        assertThatAllContextNamesAreUnique();
        
        if(this.inError) {
            throw new InvalidContextTemplateException(errorReport.toString(), this.errors);
        }
    }

    public void validateJobs(ContextTemplate contextTemplate, List<SchedulerJob> jobTemplates)
        throws InvalidContextTemplateException {
        this.inError = false;
        this.errors = new ArrayList<>();
        this.errorReport = new StringBuffer("The context template is invalid!\n");

        List<SchedulerJob> schedulerJobsFromJobPlan = ContextHelper.getAllJobs(contextTemplate);
        List<String> jobDependencyIdentifiers = ContextHelper.getAllJobDependencyIdentifiers(contextTemplate);

        this.assertAllJobIdentifiersInJobPlanMapToJobs(contextTemplate, schedulerJobsFromJobPlan, jobTemplates);
        this.assertAllJobDependencyIdentifiersInJobPlanMapToJobs(contextTemplate, jobDependencyIdentifiers, jobTemplates);

        Set<String> jobTemplatesSet = jobTemplates.stream().map(job -> {
            if(job.getJobName() == null || job.getJobName().isEmpty()) {
                try {
                    this.reportError(contextTemplate.getName(), String.format("Job[%s] sourced from the job definition artefact" +
                        " is missing a job name. This is a mandatory field!\n", this.objectMapper.writeValueAsString(job)), "");
                }
                catch (JsonProcessingException e) {
                    this.reportError(contextTemplate.getName(), String.format("Job[%s] sourced from the job definition artefact" +
                        " is missing a job name. This is a mandatory field!\n", job.getIdentifier()), "");
                }
                return "";
            }
            else {
                return job.getJobName();
            }
        }).filter(jobName -> !jobName.isEmpty()).collect(Collectors.toSet());
        Set<String> contextTemplatesJobSet = schedulerJobsFromJobPlan.stream().map(job -> {
            if(job.getJobName() == null || job.getJobName().isEmpty()) {
                try {
                    this.reportError(contextTemplate.getName(), String.format("Job[%s] sourced from the job plan template" +
                        " is missing a job name. This is a mandatory field!\n", this.objectMapper.writeValueAsString(job)), "");
                }
                catch (JsonProcessingException e) {
                    this.reportError(contextTemplate.getName(), String.format("Job[%s] sourced from the job plan template" +
                        " is missing a job name. This is a mandatory field!\n", job.getIdentifier()), "");
                }
                return "";
            }
            else {
                return job.getJobName();
            }
        }).filter(jobName -> !jobName.isEmpty()).collect(Collectors.toSet());

        contextTemplatesJobSet.removeAll(jobTemplatesSet);

        Map<String, SchedulerJob> schedulerJobsFromContext = contextTemplate.getAllSchedulerJobs().stream()
            .collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (first, second) -> first));

        if(!contextTemplatesJobSet.isEmpty()) {
            contextTemplatesJobSet.forEach(jobName -> {
                List<String> contexts = ContextHelper.getContextsWhereJobFilterMatchResides(contextTemplate, jobName);

                contexts.forEach(contextName -> {
                    ContextTemplate child = ContextHelper.getChildContextTemplate(contextName, contextTemplate);
                    AtomicBoolean reportError = new AtomicBoolean(true);

                    child.getScheduledJobs().forEach(schedulerJob -> {
                        if (schedulerJob.getJobName().equals(jobName)
                            && (schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                            || schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))) {
                            reportError.set(false);
                        }
                    });
                    if(reportError.get()) {
                        this.reportError(contextTemplate.getName(), String.format("Job[%s] appears in job " +
                            "plan but there is no job template defined for it!\n", jobName), "");
                    }
                });
            });
        }

        if(!this.errorReport.toString().isEmpty() && !this.errors.isEmpty()) {
            throw new InvalidContextTemplateException(errorReport.toString(), this.errors);
        }
    }

    private void assertAllJobIdentifiersInJobPlanMapToJobs(ContextTemplate contextTemplate, List<SchedulerJob> jobsFromJobPlan, List<SchedulerJob> jobTemplates) {
        Map<String, SchedulerJob> schedulerJobMap = jobTemplates.stream()
            .collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (first, second) -> first));

        jobsFromJobPlan.forEach(schedulerJob -> {
            if(schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                || schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                // Context start and terminal jobs do not have job templates associated
                // with them and can be ignored for the purpose of validation.
                return;
            }
            List<String> residingContextList;
            if(schedulerJob.getJobName() == null) {
                residingContextList = schedulerJob.getChildContextNames();
            }
            else {
                residingContextList = ContextHelper.getContextsWhereJobFilterMatchResides
                    (contextTemplate, schedulerJob.getJobName());
            }
            String residingContexts = "";

            if(residingContextList != null && !residingContextList.isEmpty()) {
                residingContexts = String.join(", ", residingContextList);
            }

            if(!schedulerJobMap.containsKey(schedulerJob.getIdentifier())) {
                this.reportError(contextTemplate.getName(), String.format("Job [%s] defined in the job plan template with identifier[%s] " +
                    "does not have a job defined with the same identifier! This job resides within the following child contexts" +
                    " within the job plan [%s]. Please check the job definition artefact and confirm that the identifier in the artefact is correct.\n"
                    , schedulerJob.getJobName(), schedulerJob.getIdentifier(), residingContexts), schedulerJob.getJobName());
            }
        });
    }

    private void assertAllJobDependencyIdentifiersInJobPlanMapToJobs(ContextTemplate contextTemplate, List<String> jobsIdentifiersFromJobDependencies, List<SchedulerJob> jobTemplates) {
        Map<String, SchedulerJob> schedulerJobMap = jobTemplates.stream()
            .collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (first, second) -> first));

        Map<String, SchedulerJob> schedulerJobsFromContext = contextTemplate.getAllSchedulerJobs().stream()
            .collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (first, second) -> first));

        jobsIdentifiersFromJobDependencies.forEach(identifier -> {
            if(schedulerJobsFromContext.containsKey(identifier)) {
                SchedulerJob schedulerJobFromContext = schedulerJobsFromContext.get(identifier);
                if(schedulerJobFromContext.getAgentName().equals(JobConstants.CONTEXT_START_JOB) ||
                    schedulerJobFromContext.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                    // We can ignore context start and terminal jobs as they are not defined
                    // in separate job artefacts.
                    return;
                }
            }

            if(!schedulerJobMap.containsKey(identifier)) {
                this.reportError(contextTemplate.getName(), String.format("Job Dependency Identifier [%s] defined in the job plan template " +
                        "does not have a job artefact defined with the same identifier!\n"
                    , identifier), identifier);
            }
        });
    }

    private void reportError(String contextName, String error, String jobName) {
        this.errorReport.append(error);
        ContextError contextError = new ContextError(contextName, error, jobName);
        this.errors.add(contextError);
    }

    /**
     * Helper method validate all child contexts.
     *
     * @param contextTemplate
     */
    private void validateChildContext(ContextTemplate contextTemplate) {
        this.assertThatContextJobsPresentInContextForAllJobDependencies(contextTemplate);

        childContextName.add(contextTemplate.getName());
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
                if(jobDependency.getJobIdentifier() != null) jobs.add(jobDependency.getJobIdentifier());
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

            contextJobs.forEach(jobIdentifier -> {
                StringBuffer error = new StringBuffer();
                error.append("Context[").append(contextTemplate.getName())
                    .append("] The following job [").append(jobIdentifier).append("] appears in the scheduler jobs collection" +
                        ", but is not defined in any job dependencies.");
                this.errorReport.append(error).append("\n");
                this.errors.add(new ContextError(contextTemplate.getName(), error.toString()));
            });
        }

        Set<String> contextJobs2 = new HashSet<>();
        contextTemplate.getScheduledJobs().forEach(schedulerJob -> contextJobs2.add(schedulerJob.getIdentifier()));

        jobs.removeAll(contextJobs2);
        if(!jobs.isEmpty()) {
            this.inError = true;
            jobs.forEach(jobIdentifier -> {
                StringBuffer error = new StringBuffer();
                error.append("Context[").append(contextTemplate.getName())
                    .append("] The following job [").append(jobIdentifier).append("] appears in a job dependency" +
                        ", but is not defined in the scheduler job collection.");
                this.errorReport.append(error).append("\n");
                this.errors.add(new ContextError(contextTemplate.getName(), error.toString()));
            });
        }
    }

    /**
     * Check to make sure that all context names defined are unique
     */
    private void assertThatAllContextNamesAreUnique() {
        // Map to keep track of the contextNames
        Map<String, Integer> childContextNameMap = new HashMap<>();
        for (String s : childContextName) {
            if (childContextNameMap.containsKey(s)) {
                childContextNameMap.put(s, childContextNameMap.get(s).intValue() + 1);
            } else {
                childContextNameMap.put(s, 1);
            }
        }

        for (Map.Entry<String, Integer> entry : childContextNameMap.entrySet()) {
            if (entry.getValue() > 1) {
                inError = true;
                errorReport.append("The context name [" + entry.getKey() + "] has been repeated ["+ entry.getValue() +"] times within the template. " +
                    "Context Names needs to be unique.\n");
            }
        }
    }
    
    private void manageLogicalGrouping(LogicalGrouping logicalGrouping, Set<String> jobs) {
        if(logicalGrouping.getLogicalGrouping() != null) {
            manageLogicalGrouping(logicalGrouping.getLogicalGrouping(), jobs);
        }

        if(logicalGrouping.getAnd() != null) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getIdentifier() != null)jobs.add(and.getIdentifier());
                if(and.getLogicalGrouping() != null) {
                    manageLogicalGrouping(and.getLogicalGrouping(), jobs);
                }
            });
        }

        if(logicalGrouping.getOr() != null) {
            logicalGrouping.getOr().forEach(or -> {
                if(or.getIdentifier() != null)jobs.add(or.getIdentifier());
                if(or.getLogicalGrouping() != null) {
                    manageLogicalGrouping(or.getLogicalGrouping(), jobs);
                }
            });
        }

        if(logicalGrouping.getNot() != null) {
            logicalGrouping.getNot().forEach(not -> {
                if(not.getIdentifier() != null)jobs.add(not.getIdentifier());
                if(not.getLogicalGrouping() != null) {
                    manageLogicalGrouping(not.getLogicalGrouping(), jobs);
                }
            });
        }
    }
}
