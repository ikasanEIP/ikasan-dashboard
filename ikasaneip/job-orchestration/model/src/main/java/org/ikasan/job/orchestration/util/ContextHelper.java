package org.ikasan.job.orchestration.util;

import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.job.orchestration.model.context.ContextTransition;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextStartJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextTerminalJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.BridgingJobImpl;
import org.ikasan.job.orchestration.model.job.ContextStartJobImpl;
import org.ikasan.job.orchestration.model.job.ContextTerminalJobImpl;
import org.ikasan.job.orchestration.model.job.LocalEventJobImpl;
import org.ikasan.job.orchestration.model.status.ContextJobInstanceDetailsStatusImpl;
import org.ikasan.job.orchestration.model.status.ContextJobInstanceStatusImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceDetailsStatus;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class that provides methods for manipulating and retrieving information from the job plan context.
 */
public class ContextHelper {

    static Logger logger = LoggerFactory.getLogger(ContextHelper.class);

    private static String AGENT_NAME_REPLACEMENT = "[[agent.name]]";
    private static String CONTEXT_NAME_REPLACEMENT = "[[context.name]]";
    public static String ENV_NAME_REPLACEMENT = "[[env.name]]";
    
    private static boolean USE_UNDERSCORE_SEPARATED_CONTEXT_NAME_CONVENTION = true;

    /**
     * Helper method to add replacement tokens to a scheduler job.
     *
     * @param schedulerJob
     */
    public static void addSchedulerJobReplacementTokens(SchedulerJob schedulerJob) {
        if(!schedulerJob.getAgentName().equals(JobConstants.GLOBAL_EVENT) &&
            !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB) &&
            !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
            !schedulerJob.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB) &&
            !schedulerJob.getAgentName().equals(JobConstants.BRIDGING_JOB)) {
            schedulerJob.setAgentName(AGENT_NAME_REPLACEMENT);
        }
        schedulerJob.setContextName(getContextName(schedulerJob.getContextName()));
        if(!(schedulerJob instanceof GlobalEventJob)) {
            schedulerJob.setIdentifier(AGENT_NAME_REPLACEMENT + "-" + schedulerJob.getJobName());
        }
    }

    /**
     * Add all replacement tokens to a ContextTemplate
     *
     * @param contextTemplate
     */
    public static void addContextTemplateReplacementTokens(ContextTemplate contextTemplate) {
        _addContextTemplateReplacementTokens(contextTemplate);
        contextTemplate.setName(getContextName(contextTemplate.getName()));
        contextTemplate.setEnvironmentGroup(ENV_NAME_REPLACEMENT);
    }

    /**
     * Recursively work through the ContextTemplate to add all relevant replacement tokens.
     *
     * @param contextTemplate
     */
    private static void _addContextTemplateReplacementTokens(ContextTemplate contextTemplate) {
        if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            Optional<SchedulerJob> jobOptional = contextTemplate.getScheduledJobs().stream()
                .filter(schedulerJob ->
                    !schedulerJob.getAgentName().equals(JobConstants.GLOBAL_EVENT) &&
                        !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB) &&
                        !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
                        !schedulerJob.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB) &&
                        !schedulerJob.getAgentName().equals(JobConstants.BRIDGING_JOB))
                .findFirst();
            if(contextTemplate.getUserGeneratedLayout() != null && jobOptional.isPresent()) {
                contextTemplate.setUserGeneratedLayout(contextTemplate.getUserGeneratedLayout()
                    .replaceAll(jobOptional.get().getAgentName(), AGENT_NAME_REPLACEMENT));
            }
            contextTemplate.getScheduledJobs().forEach(schedulerJob -> {
                if(contextTemplate.getJobDependencies() != null) {
                    contextTemplate.getJobDependencies().forEach(jobDependency -> {
                        replaceJobIdentifierJobDependency(schedulerJob, jobDependency);
                    });
                }

                if(!schedulerJob.getAgentName().equals(JobConstants.GLOBAL_EVENT) &&
                    !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB) &&
                    !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
                    !schedulerJob.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB) &&
                    !schedulerJob.getAgentName().equals(JobConstants.BRIDGING_JOB)) {
                    schedulerJob.setAgentName(AGENT_NAME_REPLACEMENT);
                    schedulerJob.setIdentifier(AGENT_NAME_REPLACEMENT+"-"+schedulerJob.getJobName());
                }

            });
        }

        if(contextTemplate.getJobLocks() != null) {
            contextTemplate.getJobLocks().forEach(jobLock -> {
                jobLock.getJobs().entrySet().forEach(entry -> {
                    entry.getValue().forEach(job -> {
                        if(job.getContextName() != null) {
                            job.setContextName(getContextName(job.getContextName()));
                        }
                        if(!job.getAgentName().equals(JobConstants.GLOBAL_EVENT) &&
                            !job.getAgentName().equals(JobConstants.CONTEXT_START_JOB) &&
                            !job.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
                            !job.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB) &&
                            !job.getAgentName().equals(JobConstants.BRIDGING_JOB)) {
                            job.setAgentName(AGENT_NAME_REPLACEMENT);
                            job.setIdentifier(AGENT_NAME_REPLACEMENT + "-" + job.getJobName());
                        }
                    });
                });
            });
        }

        if(contextTemplate.getContexts() != null) {
            contextTemplate.getContexts().forEach(child -> _addContextTemplateReplacementTokens(child));
        }
    }

    /**
     * Helper method to set the context name.
     *
     * @param contextName
     * @return
     */
    public static String getContextName(String contextName) {
        if(contextName.equals(JobConstants.GLOBAL_EVENT)) {
            return contextName;
        }
        else if(!USE_UNDERSCORE_SEPARATED_CONTEXT_NAME_CONVENTION) {
            return CONTEXT_NAME_REPLACEMENT;
        }
        else if(!contextName.contains("_")) {
            return contextName + "_" + ENV_NAME_REPLACEMENT;
        }
        else {
            return contextName.substring(0, contextName.lastIndexOf("_")) + "_" + ENV_NAME_REPLACEMENT;
        }
    }

    /**
     * Replace tokens in job dependencies.
     *
     * @param schedulerJob
     * @param jobDependency
     */
    private static void replaceJobIdentifierJobDependency(SchedulerJob schedulerJob, JobDependency jobDependency) {
        if(jobDependency.getJobIdentifier().equals(schedulerJob.getIdentifier())) {
            if(!schedulerJob.getIdentifier().startsWith(JobConstants.GLOBAL_EVENT) &&
                !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB) &&
                !schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
                !schedulerJob.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB) &&
                !schedulerJob.getAgentName().equals(JobConstants.BRIDGING_JOB)) {
                jobDependency.setJobIdentifier(AGENT_NAME_REPLACEMENT + "-" + schedulerJob.getJobName());
            }
        }

        if(jobDependency.getLogicalGrouping() != null) {
            replaceJobIdentifierLogicalGrouping(schedulerJob, jobDependency.getLogicalGrouping());
        }
    }

    /**
     * Replace tokens in logical groupings.
     *
     * @param schedulerJob
     * @param logicalGrouping
     */
    private static void replaceJobIdentifierLogicalGrouping(SchedulerJob schedulerJob, LogicalGrouping logicalGrouping) {
        if(logicalGrouping.getAnd() != null) {
            logicalGrouping.getAnd().forEach(and -> {
                if(schedulerJob.getIdentifier().equals(and.getIdentifier())) {
                    replaceJobIdentifierAnd(schedulerJob, and);
                    if (and.getLogicalGrouping() != null) {
                        replaceJobIdentifierLogicalGrouping(schedulerJob, and.getLogicalGrouping());
                    }
                }
            });
        }

        if(logicalGrouping.getOr() != null) {
            logicalGrouping.getOr().forEach(or -> {
                if(schedulerJob.getIdentifier().equals(or.getIdentifier())) {
                    replaceJobIdentifierOr(schedulerJob, or);
                    if (or.getLogicalGrouping() != null) {
                        replaceJobIdentifierLogicalGrouping(schedulerJob, or.getLogicalGrouping());
                    }
                }
            });
        }

        if(logicalGrouping.getNot() != null) {
            logicalGrouping.getNot().forEach(not -> {
                if(schedulerJob.getIdentifier().equals(not.getIdentifier())) {
                    replaceJobIdentifierNot(schedulerJob, not);
                    if (not.getLogicalGrouping() != null) {
                        replaceJobIdentifierLogicalGrouping(schedulerJob, not.getLogicalGrouping());
                    }
                }
            });
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            replaceJobIdentifierLogicalGrouping(schedulerJob, logicalGrouping.getLogicalGrouping());
        }
    }

    /**
     * Replace tokens in logical and.
     *
     * @param schedulerJob
     * @param and
     */
    private static void replaceJobIdentifierAnd(SchedulerJob schedulerJob, And and) {
        if(and.getIdentifier().startsWith(JobConstants.GLOBAL_EVENT) ||
            and.getIdentifier().startsWith(JobConstants.CONTEXT_START_JOB) ||
            and.getIdentifier().startsWith(JobConstants.CONTEXT_TERMINAL_JOB) ||
            and.getIdentifier().startsWith(JobConstants.LOCAL_EVENT_JOB) ||
            and.getIdentifier().startsWith(JobConstants.BRIDGING_JOB)) return;
        and.setIdentifier(AGENT_NAME_REPLACEMENT+"-"+schedulerJob.getJobName());
    }

    /**
     * Replace token in logical or.
     *
     * @param schedulerJob
     * @param or
     */
    private static void replaceJobIdentifierOr(SchedulerJob schedulerJob, Or or) {
        if(or.getIdentifier().startsWith(JobConstants.GLOBAL_EVENT) ||
            or.getIdentifier().startsWith(JobConstants.CONTEXT_START_JOB) ||
            or.getIdentifier().startsWith(JobConstants.CONTEXT_TERMINAL_JOB) ||
            or.getIdentifier().startsWith(JobConstants.LOCAL_EVENT_JOB) ||
            or.getIdentifier().startsWith(JobConstants.BRIDGING_JOB)) return;
        or.setIdentifier(AGENT_NAME_REPLACEMENT+"-"+schedulerJob.getJobName());
    }

    /**
     * Replace token in logical not.
     *
     * @param schedulerJob
     * @param not
     */
    private static void replaceJobIdentifierNot(SchedulerJob schedulerJob, Not not) {
        if(not.getIdentifier().startsWith(JobConstants.GLOBAL_EVENT) ||
            not.getIdentifier().startsWith(JobConstants.CONTEXT_START_JOB) ||
            not.getIdentifier().startsWith(JobConstants.CONTEXT_TERMINAL_JOB) ||
            not.getIdentifier().startsWith(JobConstants.LOCAL_EVENT_JOB) ||
            not.getIdentifier().startsWith(JobConstants.BRIDGING_JOB)) return;
        not.setIdentifier(AGENT_NAME_REPLACEMENT+"-"+schedulerJob.getJobName());
    }

    /**
     * Retrieves a ContextStartJob from a given ContextTemplate.
     *
     * @param context the ContextTemplate from which to retrieve the ContextStartJob
     * @return an Optional containing the ContextStartJob if found, otherwise an empty Optional
     */
    public static Optional<ContextStartJob> getContextStartJobFromContext(ContextTemplate context) {
        return context.getScheduledJobs().stream()
            .distinct()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .map(schedulerJob -> {
                ContextStartJob contextStartJob = new ContextStartJobImpl();
                contextStartJob.setContextName(schedulerJob.getContextName());
                contextStartJob.setJobName(schedulerJob.getJobName());
                contextStartJob.setAgentName(schedulerJob.getAgentName());
                contextStartJob.setChildContextNames(context.getAllContextNamesWhereJobResides(schedulerJob.getIdentifier()));
                contextStartJob.setOrdinal(Integer.MIN_VALUE);

                return contextStartJob;
            })
            .findFirst();
    }

    /**
     * Retrieves a ContextTerminalJob from a given ContextTemplate.
     *
     * @param context the ContextTemplate from which to retrieve the ContextTerminalJob
     * @return an Optional containing the ContextTerminalJob if found, otherwise an empty Optional
     */
    public static Optional<ContextTerminalJob> getContextTerminalJobFromContext(ContextTemplate context) {
        return context.getScheduledJobs().stream()
            .distinct()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) && schedulerJob.getJobName().contains(context.getName()))
            .map(schedulerJob -> {
                ContextTerminalJob contextStartJob = new ContextTerminalJobImpl();
                contextStartJob.setContextName(schedulerJob.getContextName());
                contextStartJob.setJobName(schedulerJob.getJobName());
                contextStartJob.setAgentName(schedulerJob.getAgentName());
                contextStartJob.setChildContextNames(context.getAllContextNamesWhereJobResides(schedulerJob.getIdentifier()));
                contextStartJob.setOrdinal(Integer.MIN_VALUE);

                return contextStartJob;
            })
            .findFirst();
    }

    /**
     * Retrieves a list of ContextStartJob objects from the given ContextTemplate object.
     *
     * @param context The ContextTemplate object to retrieve the ContextStartJobs from.
     * @return A list of ContextStartJob objects.
     */
    public static List<ContextStartJob> getContextStartJobsFromContext(ContextTemplate context) {
        return context.getAllSchedulerJobs().stream()
            .distinct()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .map(schedulerJob -> {
                ContextStartJob contextStartJob = new ContextStartJobImpl();
                contextStartJob.setContextName(schedulerJob.getContextName());
                contextStartJob.setJobName(schedulerJob.getJobName());
                contextStartJob.setAgentName(schedulerJob.getAgentName());
                contextStartJob.setChildContextNames(context.getAllContextNamesWhereJobResides(schedulerJob.getIdentifier()));
                contextStartJob.setOrdinal(Integer.MIN_VALUE);

                return contextStartJob;
            })
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of BridgingJob objects from the given ContextTemplate.
     *
     * @param context the ContextTemplate from which to retrieve BridgingJob objects
     * @return a list of BridgingJob objects filtered from all scheduler jobs in the context
     */
    public static List<BridgingJob> getBridgingJobsFromContext(ContextTemplate context) {
        return context.getAllSchedulerJobs().stream()
            .distinct()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(JobConstants.BRIDGING_JOB))
            .map(schedulerJob -> {
                BridgingJob bridgingJob = new BridgingJobImpl();
                bridgingJob.setContextName(schedulerJob.getContextName());
                bridgingJob.setJobName(schedulerJob.getJobName());
                bridgingJob.setAgentName(schedulerJob.getAgentName());
                bridgingJob.setChildContextNames(context.getAllContextNamesWhereJobResides(schedulerJob.getIdentifier()));
                bridgingJob.setOrdinal(Integer.MIN_VALUE);

                return bridgingJob;
            })
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of LocalEventJobs from the given ContextTemplate object based on certain criteria.
     *
     * @param context The ContextTemplate object from which to retrieve LocalEventJobs
     * @return A list of LocalEventJob objects satisfying the specified criteria
     */
    public static List<LocalEventJob> getLocalEventJobsFromContext(ContextTemplate context) {
        List<LocalEventJob> jobs = context.getAllSchedulerJobs().stream()
            .distinct()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB))
            .map(schedulerJob -> {
                LocalEventJob localEventJob = new LocalEventJobImpl();
                localEventJob.setContextName(schedulerJob.getContextName());
                localEventJob.setJobName(schedulerJob.getJobName());
                localEventJob.setChildContextNames(context.getAllContextNamesWhereJobResides(schedulerJob.getIdentifier()));
                localEventJob.setOrdinal(schedulerJob.getOrdinal());

                return localEventJob;
            })
            .collect(Collectors.toList());

        return jobs;
    }

    /**
     * Retrieves a map of {@link ContextStartJob} objects from the given {@link ContextTemplate} object.
     *
     * @param context The context template object from which to retrieve the context start jobs.
     * @return A map of {@link ContextStartJob} objects, where the keys are the identifier of the jobs
     * and the values are the objects themselves.
     */
    public static Map<String, ContextStartJob> getContextStartJobsMapFromContext(ContextTemplate context) {
        return getContextStartJobsFromContext(context).stream()
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));
    }

    /**
     * Retrieves all context terminal jobs from the given context template.
     *
     * @param context The context template from which to retrieve context terminal jobs.
     * @return A list of context terminal jobs extracted from the context template.
     */
    public static List<ContextTerminalJob> getContextTerminalJobsFromContext(ContextTemplate context) {
        return context.getAllSchedulerJobs().stream()
            .distinct()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
            .map(schedulerJob -> {
                ContextTerminalJob contextTerminalJob = new ContextTerminalJobImpl();
                contextTerminalJob.setContextName(schedulerJob.getContextName());
                contextTerminalJob.setJobName(schedulerJob.getJobName());
                contextTerminalJob.setAgentName(schedulerJob.getAgentName());
                contextTerminalJob.setChildContextNames(context.getAllContextNamesWhereJobResides(schedulerJob.getIdentifier()));
                contextTerminalJob.setOrdinal(Integer.MAX_VALUE);

                return contextTerminalJob;
            })
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a map of context terminal jobs from the given ContextTemplate.
     *
     * @param context The ContextTemplate from which to retrieve the context terminal jobs.
     * @return A map of context terminal jobs, where the keys are identifiers and the values
     * are ContextTerminalJob objects.
     */
    public static Map<String, ContextTerminalJob> getContextTerminalJobsMapFromContext(ContextTemplate context) {
        return getContextTerminalJobsFromContext(context).stream()
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));
    }

    /**
     * Retrieves a list of ContextStartJobInstances from a given ContextTemplate and ContextInstance.
     *
     * @param contextTemplate The ContextTemplate from which the ContextStartJobInstances are retrieved.
     * @param contextInstance The ContextInstance for which the ContextStartJobInstances are retrieved.
     * @return List of ContextStartJobInstances that are associated with the given ContextTemplate and ContextInstance.
     */
    public static List<ContextStartJobInstance> getContextStartJobInstancesFromContextForInstance(ContextTemplate contextTemplate
        , ContextInstance contextInstance) {
        List<ContextStartJob> contextStartJobs = getContextStartJobsFromContext(contextTemplate);

        Map<String, ContextStartJobInstance> stringContextStartJobInstanceMap =  contextStartJobs.stream()
            .map(contextTerminalJob -> {
                ContextStartJobInstance contextStartJobInstance = new ContextStartJobInstanceImpl();
                contextStartJobInstance.setContextName(contextTerminalJob.getContextName());
                contextStartJobInstance.setJobName(contextTerminalJob.getJobName());

                return contextStartJobInstance;
            })
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));

        List<ContextStartJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
            ContextStartJobInstance instance = stringContextStartJobInstanceMap.get(schedulerJobInstance.getIdentifier());

            if(instance != null) {
                ContextStartJobInstance contextualisedInstance = SerializationUtils.clone(instance);
                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                contextualisedInstance.setContextInstanceId(contextInstance.getId());

                contextualisedSchedulerJobInstances.add(contextualisedInstance);
            }
        });

        return contextualisedSchedulerJobInstances;
    }


    /**
     * Retrieves a map of context start job instances from the given context template and context instance.
     *
     * @param contextTemplate The context template to retrieve the job instances from.
     * @param contextInstance The context instance to retrieve the job instances for.
     * @return A map of context start job instances, where the key is a combination of the job's identifier and child context name, and the value is the job instance itself.
     */
    public static Map<String, ContextStartJobInstance> getContextStartJobInstancesMapFromContextForInstance(ContextTemplate contextTemplate
        , ContextInstance contextInstance) {
        return getContextStartJobInstancesFromContextForInstance(contextTemplate, contextInstance).stream()
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
    }

    /**
     * Retrieves a list of ContextTerminalJobInstances from a ContextTemplate and ContextInstance.
     *
     * @param contextTemplate The ContextTemplate containing the ContextTerminalJobs.
     * @param contextInstance The ContextInstance to retrieve SchedulerJobInstances from.
     * @return A list of ContextTerminalJobInstances that are associated with the given ContextTemplate and ContextInstance.
     */
    public static List<ContextTerminalJobInstance> getContextTerminalJobInstancesFromContextForInstance(ContextTemplate contextTemplate
        , ContextInstance contextInstance) {
        List<ContextTerminalJob> contextTerminalJobs = getContextTerminalJobsFromContext(contextTemplate);

        Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap =  contextTerminalJobs.stream()
            .map(contextTerminalJob -> {
                ContextTerminalJobInstance contextTerminalJobInstance = new ContextTerminalJobInstanceImpl();
                contextTerminalJobInstance.setContextName(contextTerminalJob.getContextName());
                contextTerminalJobInstance.setJobName(contextTerminalJob.getJobName());

                return contextTerminalJobInstance;
            })
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));

        List<ContextTerminalJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
            ContextTerminalJobInstance instance = contextTerminalJobInstanceMap.get(schedulerJobInstance.getIdentifier());

            if(instance != null) {
                ContextTerminalJobInstance contextualisedInstance = SerializationUtils.clone(instance);
                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                contextualisedInstance.setContextInstanceId(contextInstance.getId());

                contextualisedSchedulerJobInstances.add(contextualisedInstance);
            }
        });

        return contextualisedSchedulerJobInstances;
    }

    /**
     * Returns an optional {@link SchedulerJob} object representing the first job found in the given context
     * whose agent name matches the constant {@code JobConstants.CONTEXT_TERMINAL_JOB}.
     *
     * @param context the context object from which to retrieve the jobs
     * @return an optional containing the first {@link SchedulerJob} object found, or an empty optional if no such job exists
     */
    public static Optional<SchedulerJob> getContextStartJobFromContext(Context context) {
        return context.getScheduledJobs().stream()
            .filter(job -> ((SchedulerJob)job).getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .findFirst();
    }

    /**
     * Returns a list of terminal jobs from the given context.
     *
     * @param context the context from which to retrieve the terminal jobs
     * @return a list of SchedulerJob objects representing the terminal jobs
     */
    public static List<SchedulerJob> getContextTerminalJobsFromContext(Context context) {
        List<SchedulerJob> terminalJobs = new ArrayList<>();
        getContextTerminalJobFromContext(context, terminalJobs);

        return terminalJobs;
    }

    /**
     * Retrieves the terminal job from the given context.
     *
     * @param context the context from which to retrieve the terminal job
     * @return an Optional containing the terminal job, or an empty Optional if no terminal job is found
     */
    public static void getContextTerminalJobFromContext(Context context, List<SchedulerJob> terminalJobs) {
        if(context.getScheduledJobs() == null) return;
        Optional<SchedulerJob> terminalJob = context.getScheduledJobs().stream()
            .filter(job -> ((SchedulerJob)job).getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)
                && ((SchedulerJob)job).getJobName().toLowerCase().replaceAll("_", "").replaceAll(" ", "")
                .contains(context.getName().toLowerCase().replaceAll("_", "").replaceAll(" ", "")))
            .findFirst();

        if(!terminalJob.isPresent()) {
            terminalJob = context.getScheduledJobs().stream()
                .filter(job -> ((SchedulerJob) job).getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
                .findFirst();
        }

        if(terminalJob.isPresent()) terminalJobs.add(terminalJob.get());

        context.getContexts().forEach(c -> getContextTerminalJobFromContext((Context) c, terminalJobs));
    }

    /**
     * Returns a list of contexts that transition from the given target context.
     *
     * @param target The target context from which the transitions are searched.
     * @param sources The list of contexts to search for transitions from the target context.
     * @return A list of contexts that transition from the target context. If the target context does not have a start job,
     *         null is returned.
     */
    public static List<Context> transitionsFromContext(Context target, List<Context> sources) {
        List<Context> sourceContexts = new ArrayList<>();
        sources.forEach(source -> {
            List<SchedulerJob> terminalJobs = ContextHelper.getContextTerminalJobsFromContext(source);
            AtomicBoolean found = new AtomicBoolean(false);
            if(target.getJobDependencies() != null && !target.getJobDependencies().isEmpty()) {
                target.getJobDependencies()
                    .forEach(jd -> {
                        terminalJobs.forEach(schedulerJob -> {
                            if (((JobDependency) jd).getLogicalGrouping() != null &&
                                ((JobDependency) jd).getLogicalGrouping().getAnd() != null &&
                                ((JobDependency) jd).getLogicalGrouping().getAnd().size() == 1
                                && ((JobDependency) jd).getLogicalGrouping().getAnd().get(0)
                                .getIdentifier().equals(schedulerJob.getIdentifier())) {
                                sourceContexts.add(source);
                            }
                            else {
                                referencesTerminalJob(found, target, terminalJobs);
                                if(found.get()) {
                                    sourceContexts.add(source);
                                }
                            }
                        });
                    });
            }
            else {
                referencesTerminalJob(found, target, terminalJobs);
                if(found.get()) {
                    sourceContexts.add(source);
                }
            }
        });

        return sourceContexts;
    }

    /**
     * Checks if the given terminalJobs are referenced in the provided target Context or its dependencies.
     *
     * @param result AtomicBoolean flag to hold the result of the check
     * @param target The target Context to check for job dependencies
     * @param terminalJobs List of terminal jobs to search for in the target Context and dependencies
     */
    private static void referencesTerminalJob(AtomicBoolean result, Context target, List<SchedulerJob> terminalJobs) {
        if(target.getJobDependencies() != null && !target.getJobDependencies().isEmpty()) {
            target.getJobDependencies().forEach(jd ->
                terminalJobs.forEach(schedulerJob -> {
                    if(((JobDependency) jd).getLogicalGrouping() != null &&
                        ((JobDependency) jd).getLogicalGrouping().getAnd() != null) {
                        ((JobDependency) jd).getLogicalGrouping().getAnd().forEach(and -> {
                            if (and.getIdentifier().equals(schedulerJob.getIdentifier())) {
                                result.set(true);
                            }
                        });
                    }
                })
            );
        }

        if(!result.get()) {
            target.getContexts()
                .forEach(c -> referencesTerminalJob(result, (Context) c, terminalJobs));
        }
    }


    /**
     * Retrieves the map of ContextTerminalJobInstances from the given ContextTemplate and ContextInstance.
     *
     * @param contextTemplate The context template to retrieve the instances from.
     * @param contextInstance The specific instance to retrieve the job instances for.
     * @return A map of ContextTerminalJobInstances where the key is a combination of identifier and child context name,
     * and the value is the corresponding ContextTerminalJobInstance.
     */
    public static Map<String, ContextTerminalJobInstance> getContextTerminalJobInstancesMapFromContextForInstance(ContextTemplate contextTemplate
        , ContextInstance contextInstance) {
        return getContextTerminalJobInstancesFromContextForInstance(contextTemplate, contextInstance).stream()
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
    }

    /**
     * Populates the child context names on the given list of scheduler jobs using the provided context template.
     *
     * @param contextTemplate The context template used to retrieve the child context names.
     * @param schedulerJobs The list of scheduler jobs to populate with child context names.
     */
    public static void populateChildContextNamesOnSchedulerJobs(ContextTemplate contextTemplate, List<SchedulerJob> schedulerJobs) {
        schedulerJobs.forEach(schedulerJob -> {
            schedulerJob.setChildContextNames(contextTemplate.getAllContextNamesWhereJobResides(schedulerJob.getIdentifier()));
        });
    }

    /**
     * Retrieves unique context parameter instances from a collection of job instances.
     *
     * @param internalJobs The map of internal event-driven job instances where the key is the job name and the
     *                     value is the job instance.
     * @return A list of unique context parameter instances.
     */
    public static List<ContextParameterInstance> getUniqueContextParameterInstancesFromJobInstances(Map<String, InternalEventDrivenJobInstance> internalJobs) {
        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();

        internalJobs.entrySet().forEach(entry -> {
            if(entry.getValue().getContextParameters() != null) {
                contextParameterInstances.addAll(entry.getValue().getContextParameters().stream()
                .map(contextParameter -> {
                    ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
                    contextParameterInstance.setName(contextParameter.getName());
                    contextParameterInstance.setValue(contextParameter.getDefaultValue());
                    contextParameterInstance.setDefaultValue(contextParameter.getDefaultValue());

                    return contextParameterInstance;
                })
                .collect(Collectors.toList()));
            }
        });

        return contextParameterInstances.stream()
            .filter(distinctByKey(contextParameterInstance -> contextParameterInstance.getName()) )
            .collect( Collectors.toList() );
    }


    /**
     * Retrieves a list of unique context parameter instances from a map of internal jobs.
     *
     * @param internalJobs A map of internal jobs, where the key is the job name and the value is an InternalEventDrivenJob object.
     * @return A list of ContextParameterInstance objects, containing unique context parameter instances from all jobs.
     */
    public static List<ContextParameterInstance> getUniqueContextParameterInstancesFromJobs(Map<String, InternalEventDrivenJob> internalJobs) {
        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();

        internalJobs.entrySet().forEach(entry -> {
            if(entry.getValue().getContextParameters() != null) {
                contextParameterInstances.addAll(entry.getValue().getContextParameters().stream()
                    .map(contextParameter -> {
                        ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
                        contextParameterInstance.setName(contextParameter.getName());
                        contextParameterInstance.setValue(contextParameter.getDefaultValue());
                        contextParameterInstance.setDefaultValue(contextParameter.getDefaultValue());

                        return contextParameterInstance;
                    })
                    .collect(Collectors.toList()));
            }
        });

        return contextParameterInstances.stream()
            .filter(distinctByKey(contextParameterInstance -> contextParameterInstance.getName()))
            .collect( Collectors.toList() );
    }

    /**
     * The purpose of this method is to determine if a job has dependencies that transition
     * outside the child context provided.
     *
     * @param parentContext the parent context.
     * @param schedulerJobs the map of scheduler jobs in the child context.
     * @param child the child context we will determine if jobs transition from.
     * @return
     */
    public static List<ContextTransition> determineIfJobsTransitionToOtherContexts(Context parentContext, Map<String, SchedulerJob> schedulerJobs
        , Context child, Map<String, SchedulerJob> internalEventDrivenJobMap) {
        List<ContextTransition> contextTransitions = new ArrayList<>();

        // Get any jobs that represent the last jobs in the context that could potentially transition
        // to other contexts
        Map<String, SchedulerJob> jobMap = ContextHelper.getJobsOutsideLogicalGrouping(child);

        // Now iterate over all jobs in the context
        schedulerJobs.entrySet().forEach(entry -> {
            LinkedList<List<SchedulerJob>> linkedJobs;
            // Trace from each job through the child context provided and any subsequent contexts that the job extends into
            if(entry.getValue().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                linkedJobs = new LinkedList<>();
            }
            else {
                linkedJobs = ContextHelper.traceJobThroughContext(parentContext, entry.getValue().getJobName(), child.getName());
            }

            List<SchedulerJob> precedingJobs = new ArrayList<>();

            // Now iterate of the job trace. The goal is to determine where
            // we cross the boundary of the child context.
            linkedJobs.forEach(jobs -> jobs.forEach(job -> {
                // Keep track of the previous job in the child context.
                if(jobMap.containsKey(job.getIdentifier())) {
                    precedingJobs.add(job);
                }

                // At this point we have detected that we have crossed the boundary into another context.
                if(precedingJobs.size() > 0 && !schedulerJobs.containsKey(job.getIdentifier())) {
                    precedingJobs.forEach(precedingJob -> {
                        // We do not add jobs that target their residing context only as these jobs
                        // by their nature cannot transition ot another context.
                        if(internalEventDrivenJobMap.containsKey(precedingJob.getIdentifier())
                            && ((internalEventDrivenJobMap.get(precedingJob.getIdentifier()) instanceof  InternalEventDrivenJob
                                && !((InternalEventDrivenJob) internalEventDrivenJobMap.get(precedingJob.getIdentifier())).isTargetResidingContextOnly())
                            || !(internalEventDrivenJobMap.get(precedingJob.getIdentifier()) instanceof  InternalEventDrivenJob))) {
                            ContextTransition contextTransition = new ContextTransition();
                            contextTransition.setPrecedingJob(precedingJob);
                            contextTransition.setSubsequentJob(job);
                            contextTransitions.add(contextTransition);
                        }
                    });
                    precedingJobs.clear();
                }
            }));
        });

        // Now that we have detected some context transitions, we need to enrich the transition object
        // with the details of which context that the jobs in outside contexts reside within.
        contextTransitions.stream().distinct().forEach(contextTransition -> {
            List<String> residingContexts = ContextHelper.getContextsWhereJobFilterMatchResides(parentContext, contextTransition.getPrecedingJob().getJobName());
            // We don't want the child context to be included.
            residingContexts.remove(child.getName());
            contextTransition.setContexts(residingContexts);
        });

        contextTransitions.stream().distinct().forEach(contextTransition -> logger.debug(contextTransition.toString()));

        // Now return a distinct list of transitions.
        return contextTransitions.stream().filter(contextTransition
            -> !contextTransition.getContexts().isEmpty()).distinct().collect(Collectors.toList());
    }

    /**
     * Retrieves the status of an aggregate context instance.
     *
     * @param contextInstance The context instance for which to retrieve the status.
     * @return The aggregate context instance status.
     */
    public static AggregateContextInstanceStatus getAggregateContextInstanceStatus(ContextInstance contextInstance) {
        AggregateContextInstanceStatus aggregateContextInstanceStatus = new AggregateContextInstanceStatus();
        getAggregateContextInstanceStatus(contextInstance, aggregateContextInstanceStatus, null, null, null);

        return aggregateContextInstanceStatus;
    }

    /**
     * Returns the AggregateContextInstanceStatus of a given ContextInstance.
     *
     * @param contextInstance The ContextInstance for which the AggregateContextInstanceStatus is to be retrieved.
     * @param internalEventDrivenJobMap A map containing the InternalEventDrivenJobs associated with the contextInstance.
     * @param quartzSchedulerJobMap A map containing the QuartzScheduleDrivenJobInstances associated with the contextInstance.
     * @param parent The parent ContextInstance of the given contextInstance.
     * @return The AggregateContextInstanceStatus of the given ContextInstance.
     */
    public static AggregateContextInstanceStatus getAggregateContextInstanceStatus(ContextInstance contextInstance, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap,
                                                                                   Map<String, QuartzScheduleDrivenJobInstance> quartzSchedulerJobMap, ContextInstance parent) {
        AggregateContextInstanceStatus aggregateContextInstanceStatus = new AggregateContextInstanceStatus();
        getAggregateContextInstanceStatus(contextInstance, aggregateContextInstanceStatus, internalEventDrivenJobMap, quartzSchedulerJobMap, parent);

        return aggregateContextInstanceStatus;
    }

    /**
     * Calculates the aggregate status of a given context instance and updates the aggregateContextInstanceStatus object.
     * Updates the aggregateContextInstanceStatus object based on the status of each scheduled job within the context instance.
     * Recursively traverses child context instances.
     *
     * @param contextInstance               The current context instance
     * @param aggregateContextInstanceStatus The aggregate context instance status object to be updated
     * @param internalEventDrivenJobMap     The map of internal event-driven jobs
     * @param quartzSchedulerJobMap         The map of quartz schedule-driven job instances
     * @param parent                        The parent context instance
     */
    private static void getAggregateContextInstanceStatus(ContextInstance contextInstance, AggregateContextInstanceStatus aggregateContextInstanceStatus
        , Map<String, InternalEventDrivenJob> internalEventDrivenJobMap, Map<String, QuartzScheduleDrivenJobInstance> quartzSchedulerJobMap, ContextInstance parent) {
        if(contextInstance.getScheduledJobs() != null) {
            contextInstance.getScheduledJobs().forEach(schedulerJobInstance -> {
                int externalJobs = 0;

                if(internalEventDrivenJobMap != null && parent != null) {
                    externalJobs = ContextHelper.getPrecedingJobsFromOutsideContext(parent, schedulerJobInstance.getJobName()
                        , schedulerJobInstance.getChildContextName(), internalEventDrivenJobMap).size();
                }

                // we don't consider jobs that come from a preceding context
                if(externalJobs > 0) {
                    return;
                }

                if(schedulerJobInstance.getStatus().equals(InstanceStatus.DISABLED) ||
                    (quartzSchedulerJobMap != null
                        && quartzSchedulerJobMap.containsKey(schedulerJobInstance.getJobName())
                        && parent.isQuartzScheduleDrivenJobsDisabledForContext())) {
                    aggregateContextInstanceStatus.setDisabledJobs();
                }
                else if(schedulerJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
                    aggregateContextInstanceStatus.setHeldJobs();
                }
                else if(schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED)
                    || schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)
                    || schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED_RUNNING)) {
                    aggregateContextInstanceStatus.setSkippedJobs();
                }
            });
        }

        if(contextInstance.getContexts() != null) {
            contextInstance.getContexts().forEach(child -> {
                getAggregateContextInstanceStatus(child, aggregateContextInstanceStatus
                    , internalEventDrivenJobMap, quartzSchedulerJobMap, parent);
            });
        }
    }

    /**
     * Retrieves the aggregate status of a context instance.
     *
     * @param parent the parent context instance
     * @param contextInstance the context instance to retrieve the status for
     * @param internalEventDrivenJobMap the map of internal event-driven jobs
     * @return the aggregate context instance status
     */
    public static AggregateContextInstanceStatus getAggregateContextInstanceStatus(ContextInstance parent, ContextInstance contextInstance, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        AggregateContextInstanceStatus aggregateContextInstanceStatus = new AggregateContextInstanceStatus();
        getAggregateContextInstanceStatus(parent, contextInstance, aggregateContextInstanceStatus, internalEventDrivenJobMap);

        return aggregateContextInstanceStatus;
    }

    /**
     * This method calculates the aggregate instance status for a given context instance and its child instances.
     *
     * @param parent the parent context instance
     * @param contextInstance the current context instance
     * @param aggregateContextInstanceStatus the object to store the aggregate instance status
     * @param internalEventDrivenJobMap a map of internal event driven jobs
     */
    private static void getAggregateContextInstanceStatus(ContextInstance parent, ContextInstance contextInstance
        , AggregateContextInstanceStatus aggregateContextInstanceStatus, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        if(contextInstance.getScheduledJobs() != null) {
            contextInstance.getScheduledJobs().forEach(schedulerJobInstance -> {
                if(schedulerJobInstance.getStatus().equals(InstanceStatus.DISABLED)) {
                    aggregateContextInstanceStatus.setDisabledJobs();
                }
                else if(schedulerJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
                    aggregateContextInstanceStatus.setHeldJobs();
                }
                else if(schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED)) {
                    aggregateContextInstanceStatus.setSkippedJobs();
                }
            });
        }

        if(contextInstance.getContexts() != null) {
            contextInstance.getContexts().forEach(child -> {
                getAggregateContextInstanceStatus(parent, child, aggregateContextInstanceStatus, internalEventDrivenJobMap);
            });
        }
    }
    /**
     * Looks at the context instance and get the status for all the jobs
     * It will return 1 record per job that sits across multiple context if targetResidingContextOnly = false
     * If the job has targetResidingContextOnly set to true, it will return a single record for it
     * @param contextInstance - instance
     * @param internalEventDrivenJobs - internal jobs
     * @return List of ContextJobInstanceStatusImpl
     */
    public static ContextJobInstanceStatus getContextJobInstanceStatus(ContextInstance contextInstance, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs) {
        ContextJobInstanceStatus contextJobInstanceStatus = new ContextJobInstanceStatusImpl();
        contextJobInstanceStatus.setContextName(contextInstance.getName());
        contextJobInstanceStatus.setContextInstanceId(contextInstance.getId());
        contextJobInstanceStatus.setInstanceStatus(contextInstance.getStatus());
        contextJobInstanceStatus.setJobDetails(new ArrayList<>());
        getContextJobInstanceStatus(contextInstance, contextJobInstanceStatus, internalEventDrivenJobs);
        return contextJobInstanceStatus;
    }

    /**
     * Helper method to get the status
     * @param contextInstance - instance
     * @param contextJobInstanceStatus - ContextJobInstanceStatusImpl object to store all the information of the status
     * @param internalEventDrivenJobs - internal jobs
     */
    private static void getContextJobInstanceStatus(ContextInstance contextInstance, ContextJobInstanceStatus contextJobInstanceStatus,
                                                    Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs) {
        if (contextInstance.getContexts() == null || contextInstance.getContexts().isEmpty()) {
            if(contextInstance.getScheduledJobs() != null) {
                // For each
                contextInstance.getScheduledJobs().forEach(schedulerJobInstance -> {
                    ContextJobInstanceDetailsStatus contextJobInstanceDetailsStatus = new ContextJobInstanceDetailsStatusImpl();
                    contextJobInstanceDetailsStatus.setTargetResidingContextOnly(false);
                    contextJobInstanceDetailsStatus.setErrorAcknowledged(schedulerJobInstance.isErrorAcknowledged() != null && schedulerJobInstance.isErrorAcknowledged());

                    // check if the job is targetResiding. If so create new ContextJobInstanceDetailsStatus, else we check if we already have it in our contextJobInstanceDetailsStatusList
                    if (internalEventDrivenJobs != null && internalEventDrivenJobs.containsKey(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName())) {
                        InternalEventDrivenJobInstance internalEventDrivenJobInstance = internalEventDrivenJobs.get(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName());
                        if(internalEventDrivenJobInstance.isTargetResidingContextOnly()) {
                            contextJobInstanceDetailsStatus.setTargetResidingContextOnly(true);
                        }

                        // If you are in internal job, take the completionTime and set it to the End time
                        if (schedulerJobInstance.getScheduledProcessEvent() != null) {
                            contextJobInstanceDetailsStatus.setEndTime(schedulerJobInstance.getScheduledProcessEvent().getCompletionTime());
                        }
                    } else {
                        // If you are anything else, i.e. file, schedule or globalEvent, then sent the Fire Time as the End time - completionTime is not updated when actioned.
                        if (schedulerJobInstance.getScheduledProcessEvent() != null) {
                            contextJobInstanceDetailsStatus.setEndTime(schedulerJobInstance.getScheduledProcessEvent().getFireTime());
                        }
                    }

                    // if targetResiding is false, check if we have a record in our list and add the childContextName
                    AtomicBoolean hasUpdated = new AtomicBoolean(false);
                    contextJobInstanceStatus.getJobDetails().stream()
                        .filter(record -> record.checkExist(schedulerJobInstance.getJobName()))
                        .forEach(record -> {
                            record.getChildContextName().add(schedulerJobInstance.getChildContextName());
                            hasUpdated.set(true);
                        });

                    // If we haven't updated or if targetResiding = true then create an entry for the list
                    if (!hasUpdated.get()) {
                        contextJobInstanceDetailsStatus.getChildContextName().add(schedulerJobInstance.getChildContextName());
                        contextJobInstanceDetailsStatus.setJobName(schedulerJobInstance.getJobName());
                        contextJobInstanceDetailsStatus.setInstanceStatus(schedulerJobInstance.getStatus());
                        if (schedulerJobInstance.getScheduledProcessEvent() != null) {
                            contextJobInstanceDetailsStatus.setStartTime(schedulerJobInstance.getScheduledProcessEvent().getFireTime());
                        }
                        contextJobInstanceStatus.getJobDetails().add(contextJobInstanceDetailsStatus);
                    }
                });
            }
        } else {
            // Recursive call to get status of its nested context
            contextInstance.getContexts().forEach(contextInstanceChild -> {
                getContextJobInstanceStatus(contextInstanceChild, contextJobInstanceStatus, internalEventDrivenJobs);
            });
        }
    }

    /**
     * This helper method returns a map of jobs within a context that are not
     * present within any logical constructs within the context.
     *
     * @param context
     * @return
     */
    public static Map<String, SchedulerJob> getJobsOutsideLogicalGrouping(Context context) {
        Map<String, SchedulerJob> jobsOutsideLogicConstructs
            = (Map<String, SchedulerJob>) context.getScheduledJobs().stream()
                .collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (job1, job2) -> job1));

        if(context.getJobDependencies() != null) {
            ((List<JobDependency>)context.getJobDependencies()).forEach(jobDependency -> {
                if(jobDependency.getLogicalGrouping() != null) {
                    removeJobsInLogicalConstructs(jobDependency.getLogicalGrouping(),
                        jobsOutsideLogicConstructs);
                }
            });
        }
        // We retain the terminal jobs if one exists as by its nature it can
        // transition to other contexts.
        List<SchedulerJob> terminal = ((List<SchedulerJob>)context.getScheduledJobs()).stream()
            .filter(job -> job.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)).collect(Collectors.toList());
        if(!terminal.isEmpty()) {
            terminal.forEach(terminalJob -> jobsOutsideLogicConstructs.put(terminalJob.getIdentifier(), terminalJob));
        }

        return jobsOutsideLogicConstructs;
    }

    /**
     * Removes jobs within logical constructs recursively.
     *
     * @param logicalGrouping the logical grouping to remove jobs from
     * @param schedulerJobMap the map containing the scheduler jobs
     */
    private static void removeJobsInLogicalConstructs(LogicalGrouping logicalGrouping,
                                                Map<String, SchedulerJob> schedulerJobMap) {
        if(logicalGrouping != null) {
            removeJobsInAndConstructs(logicalGrouping, schedulerJobMap);
            removeJobsInOrConstructs(logicalGrouping, schedulerJobMap);
            removeJobsInNotConstructs(logicalGrouping, schedulerJobMap);

            if(logicalGrouping.getLogicalGrouping() != null) {
                removeJobsInLogicalConstructs(logicalGrouping.getLogicalGrouping(), schedulerJobMap);
            }
        }
    }

    /**
     * Recursively remove jobs within in constructs.
     *
     * @param logicalGrouping
     * @param schedulerJobMap
     */
    private static void removeJobsInAndConstructs(LogicalGrouping logicalGrouping,
                                     Map<String, SchedulerJob> schedulerJobMap) {
        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            logicalGrouping.getAnd().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    removeJobsInLogicalConstructs(operator.getLogicalGrouping(),
                        schedulerJobMap);
                }
                else {
                    schedulerJobMap.remove(operator.getIdentifier());
                }
            });
        }
    }

    /**
     * Recursively remove jobs within not constructs.
     *
     * @param logicalGrouping
     * @param schedulerJobMap
     */
    private static void removeJobsInNotConstructs(LogicalGrouping logicalGrouping,
                                     Map<String, SchedulerJob> schedulerJobMap) {
        if(logicalGrouping.getNot() != null && !logicalGrouping.getNot().isEmpty()) {
            logicalGrouping.getNot().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    removeJobsInLogicalConstructs(operator.getLogicalGrouping(),
                        schedulerJobMap);
                }
                else {
                    schedulerJobMap.remove(operator.getIdentifier());
                }
            });
        }
    }

    /**
     * Recursively remove jobs within or constructs.
     *
     * @param logicalGrouping
     * @param schedulerJobInstanceMap
     */
    private static void removeJobsInOrConstructs(LogicalGrouping logicalGrouping,
                                    Map<String, SchedulerJob> schedulerJobInstanceMap) {
        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            logicalGrouping.getOr().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    removeJobsInLogicalConstructs(operator.getLogicalGrouping(),
                        schedulerJobInstanceMap);
                }
                else {
                    schedulerJobInstanceMap.remove(operator.getIdentifier());
                }
            });
        }
    }

    /**
     * Helper method to return a list of jobs that act as a catalyst for the provided job within
     * the provided context.
     *
     * @param context
     * @param jobName
     * @param childContextName
     * @param internalEventDrivenJobMap
     * @return
     */
    public static List<SchedulerJobInstance> getPrecedingJobsFromOutsideContext(Context context
        , String jobName, String childContextName, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        Context theChildContext = getChildContext(childContextName, context);
        List<String> residingContexts = getContextsWhereJobResides(context, jobName);

        List<SchedulerJob> finalResults = new ArrayList<>();

        for (String name : residingContexts) {
            Context child = ContextHelper.getChildContext(name, context);

            if (child == null) continue;

            Optional<SchedulerJobInstance> schedulerJobInstance = child.getScheduledJobs().stream()
                .filter(job -> jobName.equals(((SchedulerJob)job).getJobName()))
                .findFirst();

            if (schedulerJobInstance.isEmpty()) continue;

            if(internalEventDrivenJobMap
                .containsKey(schedulerJobInstance.get().getIdentifier() + "-" +schedulerJobInstance.get().getChildContextName())) {
                InternalEventDrivenJob instance = internalEventDrivenJobMap
                    .get(schedulerJobInstance.get().getIdentifier() + "-" +schedulerJobInstance.get().getChildContextName());

                if (instance.isTargetResidingContextOnly()) continue;
            }

            if (child.getJobDependencies() == null) continue;

            Optional<JobDependency> jobDependency = child.getJobDependencies().stream().filter(dependency
                    -> schedulerJobInstance.get().getIdentifier().equals(((JobDependency)dependency).getJobIdentifier()))
                .findFirst();

            if (jobDependency.isEmpty()) continue;

            List<SchedulerJob> results = new ArrayList<>();

            getUpstreamDependencies(jobDependency.get().getLogicalGrouping(), results, child.getScheduledJobsMap());

            // Filter to only return jobs that appear in multiple contexts.
            if(results.size() > 0) {
                finalResults.addAll(results.stream()
                    .collect(Collectors.toList()));
            }
        }

        finalResults = finalResults.stream().flatMap(s -> Stream.ofNullable(s))
            .filter(job -> !((SchedulerJobInstance)job).getChildContextName().equals(childContextName) &&
                theChildContext != null &&
                !theChildContext.getScheduledJobs().stream()
                    .flatMap(s -> Stream.ofNullable(s))
                    .filter(j -> job!= null && job.getJobName().equals(((SchedulerJob)j).getJobName()))
                    .findFirst()
                    .isPresent())
            .filter(distinctByKey(j -> j.getJobName()))
            .collect(Collectors.toList());

        return finalResults.stream().map(job -> (SchedulerJobInstance)job).collect(Collectors.toList());
    }

    /**
     * Retrieves a list of preceding job instances for a given job name within a specified context.
     *
     * This method evaluates the dependencies of the specified job by analyzing the contexts where
     * the job resides, as well as the upstream dependencies linked through those contexts.
     *
     * @param context the base context in which the jobs and their dependencies are evaluated
     * @param jobName the name of the job for which preceding jobs are to be determined
     * @param internalEventDrivenJobMap a mapping of internal event-driven jobs used to resolve dependencies
     * @return a list of {@code SchedulerJobInstance} objects representing jobs that precede the specified job
     */
    public static List<SchedulerJobInstance> getPrecedingJobs(Context context
        , String jobName, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        List<String> residingContexts = getContextsWhereJobResides(context, jobName);

        List<SchedulerJob> finalResults = new ArrayList<>();

        for (String name : residingContexts) {
            Context child = ContextHelper.getChildContext(name, context);

            if (child == null) continue;

            Optional<SchedulerJobInstance> schedulerJobInstance = child.getScheduledJobs().stream()
                .filter(job -> jobName.equals(((SchedulerJob)job).getJobName()))
                .findFirst();

            if (schedulerJobInstance.isEmpty()) continue;

            if (child.getJobDependencies() == null) continue;

            Optional<JobDependency> jobDependency = child.getJobDependencies().stream().filter(dependency
                    -> schedulerJobInstance.get().getIdentifier().equals(((JobDependency)dependency).getJobIdentifier()))
                .findFirst();

            if (jobDependency.isEmpty()) continue;

            List<SchedulerJob> results = new ArrayList<>();

            getUpstreamDependencies(jobDependency.get().getLogicalGrouping(), results, child.getScheduledJobsMap());

            // Filter to only return jobs that appear in multiple contexts.
            if(results.size() > 0) {
                finalResults.addAll(results.stream()
                    .collect(Collectors.toList()));
            }
        }

        finalResults = finalResults.stream().flatMap(s -> Stream.ofNullable(s))
            .filter(distinctByKey(j -> j.getJobName()))
            .collect(Collectors.toList());

        return finalResults.stream().map(job -> (SchedulerJobInstance)job).collect(Collectors.toList());
    }

    /**
     * Determines if Jobs transition from other contexts.
     *
     * @param context                    the parent context
     * @param jobName                    the name of the job
     * @param childContextName           the name of the child context
     * @param schedulerJobMap            a map of scheduler jobs
     * @return a list of context transitions where jobs transition from other contexts
     */
    public static List<ContextTransition> determineIfJobsTransitionFromOtherContexts(Context context
        , String jobName, String childContextName, Map<String, SchedulerJob> schedulerJobMap) {
        Map<String, SchedulerJob> jobs = new HashMap<>();
        schedulerJobMap.entrySet().forEach(entry -> {
            jobs.put(entry.getKey(), entry.getValue());
        });
        return determineIfSchedulerJobsTransitionFromOtherContexts(context, jobName, childContextName, jobs);
    }

    /**
     * Similar to the method above, a helper method to determine if any jobs transition from outside
     * the given context for the given job. A list of ContextTransition objects are returned which
     * contains information about all contexts that transition here.
     *
     * @param context
     * @param jobName
     * @param childContextName
     * @param schedulerJobMap
     * @return
     */
    public static List<ContextTransition> determineIfSchedulerJobsTransitionFromOtherContexts(Context context
        , String jobName, String childContextName, Map<String, SchedulerJob> schedulerJobMap) {
        List<String> residingContexts = getContextsWhereJobResides(context, jobName);

        Map<String, ContextTransition> finalResults = new HashMap<>();

        for (String name : residingContexts) {
            Context child = ContextHelper.getChildContext(name, context);

            if (child == null) continue;

            Optional<SchedulerJob> schedulerJob = child.getScheduledJobs().stream()
                .filter(job -> jobName.equals(((SchedulerJob)job).getJobName()))
                .findFirst();

            if (schedulerJob.isEmpty()) continue;

            if(schedulerJobMap.containsKey(schedulerJob.get().getIdentifier() + "-" + name)
                && schedulerJobMap.get(schedulerJob.get().getIdentifier() + "-" + name) instanceof InternalEventDrivenJob) {
                InternalEventDrivenJob instance = (InternalEventDrivenJob)
                    schedulerJobMap.get(schedulerJob.get().getIdentifier() + "-" + name);

                if (instance.isTargetResidingContextOnly()) continue;
            }

            if (child.getJobDependencies() == null) continue;

            Optional<JobDependency> jobDependency = child.getJobDependencies().stream().filter(dependency
                    -> schedulerJob.get().getIdentifier().equals(((JobDependency)dependency).getJobIdentifier()))
                .findFirst();

            if (jobDependency.isEmpty()) continue;

            List<SchedulerJob> results = new ArrayList<>();
            results.add(schedulerJob.get());


            // Filter to only return jobs that appear in multiple contexts.
            if(results.size() > 0 && !child.getName().equals(childContextName)) {
                if(!finalResults.containsKey(schedulerJob.get().getIdentifier())) {
                    ContextTransition contextTransition = new ContextTransition();
                    contextTransition.setPrecedingJob(schedulerJob.get());
                    contextTransition.setSubsequentJob(schedulerJob.get());
                    contextTransition.setContexts(new ArrayList<>());
                    finalResults.put(schedulerJob.get().getIdentifier(), contextTransition);
                }

                if(!finalResults.get(schedulerJob.get().getIdentifier()).getContexts().contains(child.getName())) {
                    finalResults.get(schedulerJob.get().getIdentifier()).getContexts().add(child.getName());
                }
            }
        }

        return finalResults.values().stream().collect(Collectors.toList());
    }

    /**
     * Retrieves the upstream dependencies for a given logical grouping of scheduler jobs.
     *
     * @param logicalGrouping         the logical grouping of scheduler jobs
     * @param schedulerJobInstances   the list of scheduler job instances
     * @param schedulerJobInstanceMap the map of scheduler job instances
     */
    private static void getUpstreamDependencies(LogicalGrouping logicalGrouping, List<SchedulerJob> schedulerJobInstances,
                                                Map<String, SchedulerJob> schedulerJobInstanceMap) {
        if(logicalGrouping != null) {
            assessAnd(logicalGrouping, schedulerJobInstances, schedulerJobInstanceMap);
            assessOr(logicalGrouping, schedulerJobInstances, schedulerJobInstanceMap);
            assessNot(logicalGrouping, schedulerJobInstances, schedulerJobInstanceMap);
        }
    }

    /**
     * Removes the "_in" or "_out" suffix from the given identifier.
     *
     * @param identifier the identifier to be processed
     * @return the modified identifier without the "_in" or "_out" suffix
     */
    public static String getIdentifier(String identifier) {
        if(identifier.contains("_in")) {
            identifier = identifier.substring(0, identifier.indexOf("_in"));
        }
        else if(identifier.contains("_out")) {
            identifier = identifier.substring(0, identifier.indexOf("_out"));
        }

        return identifier;
    }

    /**
     * Creates a Predicate that filters elements based on their uniqueness with respect to a key extracted from the elements.
     *
     * @param <T> the type of elements in the input
     * @param keyExtractor the function to extract the key from the elements
     * @return a Predicate that filters elements based on uniqueness with respect to the extracted key
     */
    private static <T> Predicate<T> distinctByKey(
        Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * Assess the logical grouping with an 'AND' operator.
     *
     * @param logicalGrouping        the logical grouping to assess
     * @param schedulerJobInstances  the list of scheduler job instances
     * @param schedulerJobInstanceMap  the map of scheduler job instances
     *
     * @return true if the logical grouping has an 'AND' operator, false otherwise
     */
    private static boolean assessAnd(LogicalGrouping logicalGrouping, List<SchedulerJob> schedulerJobInstances,
                                Map<String, SchedulerJob> schedulerJobInstanceMap) {
        AtomicBoolean and = new AtomicBoolean(false);

        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            and.set(true);
            logicalGrouping.getAnd().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    getUpstreamDependencies(operator.getLogicalGrouping(), schedulerJobInstances,
                        schedulerJobInstanceMap);
                }
                else {
                    schedulerJobInstances.add(schedulerJobInstanceMap.get(operator.getIdentifier()));
                }
            });
        }

        return and.get();
    }

    /**
     * Assess the logical OR condition for a given LogicalGrouping.
     *
     * @param logicalGrouping        The LogicalGrouping object to be assessed.
     * @param schedulerJobInstances  The list of SchedulerJob instances.
     * @param schedulerJobInstanceMap The map of SchedulerJob instances.
     * @return True if any of the operators in the logicalGrouping are satisfied by the schedulerJobInstances, false otherwise.
     */
    private static boolean assessOr(LogicalGrouping logicalGrouping, List<SchedulerJob> schedulerJobInstances,
                               Map<String, SchedulerJob> schedulerJobInstanceMap) {
        AtomicBoolean or = new AtomicBoolean(false);
        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            logicalGrouping.getOr().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    getUpstreamDependencies(operator.getLogicalGrouping(), schedulerJobInstances,
                        schedulerJobInstanceMap);
                }
                else {
                    schedulerJobInstances.add(schedulerJobInstanceMap.get(operator.getIdentifier()));
                }
            });
        }

        return or.get();
    }

    /**
     * This method assesses the logical grouping by considering the negation (NOT) operator.
     * It checks if any elements in the not list of the logical grouping satisfy the given criteria.
     *
     * @param logicalGrouping         the logical grouping to be assessed
     * @param schedulerJobInstances   the list of scheduler job instances
     * @param schedulerJobInstanceMap the map of scheduler job instances
     * @return true if any elements in the not list satisfy the criteria, false otherwise
     */
    private static boolean assessNot(LogicalGrouping logicalGrouping, List<SchedulerJob> schedulerJobInstances,
                                Map<String, SchedulerJob> schedulerJobInstanceMap) {
        AtomicBoolean not = new AtomicBoolean(false);
        if(logicalGrouping.getNot() != null && !logicalGrouping.getNot().isEmpty()) {
            logicalGrouping.getNot().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    getUpstreamDependencies(operator.getLogicalGrouping(), schedulerJobInstances,
                        schedulerJobInstanceMap);
                }
                else {
                    schedulerJobInstances.add(schedulerJobInstanceMap.get(operator.getIdentifier()));
                }
            });
        }

        return not.get();
    }

    /**
     * Traces a job through the specified context and its child contexts, returning a linked list of lists
     * of SchedulerJob objects that match the given job name and child context name.
     *
     * @param context The starting context from which to trace the job.
     * @param jobName The name of the job to trace.
     * @param childContextName The name of the child context to trace.
     * @return A linked list of lists of SchedulerJob objects that match the given job name and child context name.
     */
    public static LinkedList<List<SchedulerJob>> traceJobThroughContext(Context context, String jobName, String childContextName) {
        LinkedList<List<SchedulerJob>> results = new LinkedList<>();
        List<String> processedContexts = new ArrayList<>();
        _traceJobThroughContext(results, context, jobName, childContextName, processedContexts);

        return results;
    }

    /**
     * Traces a job through a given context and its child contexts.
     *
     * @param results           The list containing the traced jobs.
     * @param context           The current context.
     * @param jobName           The name of the job to trace.
     * @param childContextName  The name of the child context.
     * @param processedContexts The list of processed contexts to avoid circular dependencies.
     */
    private static void _traceJobThroughContext(LinkedList<List<SchedulerJob>> results, Context context, String jobName, String childContextName, List<String> processedContexts) {
        logger.debug(String.format("_traceJobThroughContext - contextName[%s], jobName[%s], childContextName[%s]", context.getName(),
            jobName, childContextName));
        Context child = ContextHelper.getChildContext(childContextName, context);

        // protect against circular dependencies that cause stack overflows
        if(processedContexts.contains(child.getName())) {
            return;
        }
        else {
            processedContexts.add(child.getName());
        }

        Optional<SchedulerJob> schedulerJobInstance = ((List<SchedulerJob>)child.getScheduledJobs()).stream()
            .filter(job -> job.getJobName().equals(jobName))
            .findFirst();

        List<SchedulerJob> jobs = new ArrayList<>();

        if(schedulerJobInstance.isPresent()) {
            schedulerJobInstance.ifPresent(job -> {
                if(child.getJobDependencies() != null) {
                    ((List<JobDependency>) child.getJobDependencies()).forEach(jobDependency -> {
                        getNextJob(child, jobDependency.getJobIdentifier()
                            , jobDependency.getLogicalGrouping(), schedulerJobInstance.get(), jobs);
                    });
                }
            });
        }

        if(!jobs.isEmpty()) {
            results.add(jobs);
            jobs.forEach(job -> {
                if(job != null) {
                    List<String> contexts = getContextsWhereJobResides(context, job.getJobName());
                    if (contexts != null) {
                        contexts.forEach(filtered -> {
                            if (!filtered.equals(childContextName) && !filtered.isEmpty()) {
                                _traceJobThroughContext(results, context, job.getJobName(), filtered, processedContexts);
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * Retrieves the next job from the given logical grouping based on job identifier.
     * If the logicalGrouping is null, it adds the SchedulerJob with the specified job identifier to the jobIdentifiers list.
     * If the logicalGrouping has logical grouping, it recursively calls getNextJob with the logicalGrouping.
     * If the logicalGrouping has 'and' conditions, it checks each condition and adds the SchedulerJob with the specified job identifier to the jobIdentifiers list if it matches.
     * If the logicalGrouping has 'or' conditions, it checks each condition and adds the SchedulerJob with the specified job identifier to the jobIdentifiers list if it matches.
     * If the logicalGrouping has 'not' conditions, it checks each condition and adds the SchedulerJob with the specified job identifier to the jobIdentifiers list if it matches.
     *
     * @param child the context object
     * @param jobIdentifier the job identifier to search for
     * @param logicalGrouping the logical grouping to retrieve the job from
     * @param schedulerJobInstance the scheduler job instance
     * @param jobIdentifiers the list to store the matched jobs
     */
    private static void getNextJob(Context child, String jobIdentifier, LogicalGrouping logicalGrouping, SchedulerJob schedulerJobInstance, List<SchedulerJob> jobIdentifiers) {
        if(logicalGrouping == null) {
            jobIdentifiers.add(((Map<String, SchedulerJob>)child.getScheduledJobsMap()).get(jobIdentifier));
            return;
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            getNextJob(child, jobIdentifier, logicalGrouping.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
        }

        if(logicalGrouping.getAnd() != null) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getLogicalGrouping() != null) {
                    getNextJob(child, jobIdentifier, and.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
                }
                else if(schedulerJobInstance.getIdentifier().equals(and.getIdentifier())) {
                    jobIdentifiers.add(((Map<String, SchedulerJob>)child.getScheduledJobsMap()).get(jobIdentifier));
                }
            });
        }

        if(logicalGrouping.getOr() != null) {
            logicalGrouping.getOr().forEach(or -> {
                if(or.getLogicalGrouping() != null) {
                    getNextJob(child, jobIdentifier, or.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
                }
                else if(schedulerJobInstance.getIdentifier().equals(or.getIdentifier())) {
                    jobIdentifiers.add(((Map<String, SchedulerJob>)child.getScheduledJobsMap()).get(jobIdentifier));
                }
            });
        }

        if(logicalGrouping.getNot() != null) {
            logicalGrouping.getNot().forEach(not -> {
                if(not.getLogicalGrouping() != null) {
                    getNextJob(child, jobIdentifier, not.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
                }
                else if(schedulerJobInstance.getIdentifier().equals(not.getIdentifier())) {
                    jobIdentifiers.add(((Map<String, SchedulerJob>)child.getScheduledJobsMap()).get(jobIdentifier));
                }
            });
        }
    }

    /**
     * Retrieves the child context with the specified name from the given parent context.
     * If the parent context matches the specified child context name, it is returned.
     * Otherwise, it recursively searches through the child contexts of the parent context to find the matching child context.
     *
     * @param childContextName the name of the child context to retrieve
     * @param context the parent context to search within
     * @return the child context with the specified name, or null if it does not exist
     */
    public static Context getChildContext(String childContextName, Context context) {
        if(context.getName().equals(childContextName)) {
            return context;
        }

        if(context.getContexts() != null) {
            for (Context c : (List<Context>)context.getContexts()) {
                Context result = getChildContext(childContextName, c);

                if(result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Retrieves the child ContextInstance with the specified name from the given parent ContextInstance.
     *
     * @param childContextName The name of the child ContextInstance to retrieve.
     * @param contextInstance The parent ContextInstance from which to retrieve the child ContextInstance.
     * @return The child ContextInstance with the specified name, or null if not found.
     */
    public static ContextInstance getChildContextInstance(String childContextName, ContextInstance contextInstance) {
        if(contextInstance.getName().equals(childContextName)) {
            return contextInstance;
        }

        if(contextInstance.getContexts() != null) {
            for (ContextInstance instance: contextInstance.getContexts()) {
                ContextInstance result = getChildContextInstance(childContextName, instance);

                if(result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Retrieves a child context template based on its name from the given parent context template.
     *
     * @param childContextName    The name of the child context template to retrieve.
     * @param contextTemplate     The parent context template from which to retrieve the child context template.
     * @return The child context template if found, or null if no such child context template exists.
     */
    public static ContextTemplate getChildContextTemplate(String childContextName, ContextTemplate contextTemplate) {
        if(contextTemplate.getName().equals(childContextName)) {
            return contextTemplate;
        }

        if(contextTemplate.getContexts() != null) {
            for (ContextTemplate template: contextTemplate.getContexts()) {
                ContextTemplate result = getChildContextTemplate(childContextName, template);

                if(result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Finds the parent context template that contains a child context with the specified name.
     *
     * @param childContextName the name of the child context to search for
     * @param contextTemplate the root context template to search within
     * @return the parent context template that contains the child context, or null if not found
     */
    public static ContextTemplate getParentContextTemplate(String childContextName, ContextTemplate contextTemplate) {
        AtomicBoolean containsContext = new AtomicBoolean(false);

        contextTemplate.getContexts().forEach(c -> {
            if(c.getName().equals(childContextName)) {
                containsContext.set(true);
            }
        });

        if(containsContext.get()) {
            return contextTemplate;
        }

        if(contextTemplate.getContexts() != null) {
            for (ContextTemplate template: contextTemplate.getContexts()) {
                ContextTemplate result = getParentContextTemplate(childContextName, template);

                if(result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Removes the child context template with the specified name from the given context template.
     *
     * @param childContextName the name of the child context template to be removed
     * @param contextTemplate the context template from which to remove the child context template
     */
    public static void removeChildContextTemplate(String childContextName, ContextTemplate contextTemplate) {
        if(contextTemplate.getContexts() != null) {
            if(contextTemplate.getContextsMap().containsKey(childContextName)) {
                contextTemplate.getContexts().remove(contextTemplate.getContextsMap().get(childContextName));
                contextTemplate.getContextsMap().remove(childContextName);
            }
            else {
                contextTemplate.getContexts().forEach(template -> removeChildContextTemplate(childContextName, template));
            }
        }
    }

    /**
     * Replaces a child context template within a parent context template with an updated version.
     *
     * @param contextTemplate The parent context template containing the child context template.
     * @param updated The updated version of the child context template.
     * @return The parent context template with the updated child context template. It may or may not be modified.
     */
    public static void replaceChildContextTemplate(ContextTemplate contextTemplate, ContextTemplate updated) {
        if(contextTemplate.getName().equals(updated.getName())) {
            contextTemplate.setJobDependencies(updated.getJobDependencies());
            contextTemplate.setScheduledJobs(updated.getScheduledJobs());
            contextTemplate.setUserGeneratedLayout(updated.getUserGeneratedLayout());
            return;
        }

        if(contextTemplate.getContexts() != null) {
            contextTemplate.getContexts().forEach(template -> replaceChildContextTemplate(template, updated));
        }
    }

    /**
     * Holds all jobs in the provided context by setting their state to "hold".
     *
     * @param context The context instance where the jobs are held.
     * @param schedulerJobInstanceMap A map of internal event-driven job instances to be held.
     */
    public static void holdAllJobs(ContextInstance context, Map<String, SchedulerJobInstance> schedulerJobInstanceMap) {
        _holdAllJobs(context, schedulerJobInstanceMap);
    }

    /**
     * Holds all jobs that are either in the WAITING or RELEASED status, and sets their status to ON_HOLD.
     * Additionally, it sets the child contexts of these jobs as held, and updates their status to ON_HOLD.
     *
     * @param context The ContextInstance object to hold the jobs.
     * @param schedulerJobInstanceMap A map containing the instances of InternalEventDrivenJobInstance objects, with their identifiers as keys.
     */
    private static void _holdAllJobs(ContextInstance context, Map<String, SchedulerJobInstance> schedulerJobInstanceMap) {
        if(context.getScheduledJobs() != null) {
            context.getScheduledJobs().forEach(job -> {
                if(schedulerJobInstanceMap.containsKey(job.getIdentifier() + "-" + job.getChildContextName())
                    && (job.getStatus().equals(InstanceStatus.WAITING) || job.getStatus().equals(InstanceStatus.RELEASED))) {
                    if (job.getChildContextNames() != null) {
                        Map<String, Boolean> heldMap = new HashMap<>();
                        job.getChildContextNames()
                            .forEach(name -> heldMap.put(name, Boolean.TRUE));
                        job.setHeldContexts(heldMap);
                    }

                    job.setHeld(true);
                    job.setStatus(InstanceStatus.ON_HOLD);
                }
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> _holdAllJobs(c, schedulerJobInstanceMap));
        }
    }

    /**
     * Sets the status of all job instances in the given internal event-driven job instance map.
     *
     * @param context The context instance.
     * @param internalEventDrivenJobInstanceMap The map of internal event-driven job instances.
     * @param instanceStatus The status to set for all job instances.
     */
    public static void setJobStatusAll(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap, InstanceStatus instanceStatus) {
        _setJobStatusAll(context, internalEventDrivenJobInstanceMap, instanceStatus);
    }

    /**
     * Sets the job status for all the jobs in the given context and its child contexts.
     *
     * @param context The context instance.
     * @param internalEventDrivenJobInstanceMap The map of internal event driven job instances.
     * @param instanceStatus The status to be set for the jobs.
     */
    private static void _setJobStatusAll(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap, InstanceStatus instanceStatus) {
        if(context.getScheduledJobs() != null) {
            context.getScheduledJobs().forEach(job -> {
                if(internalEventDrivenJobInstanceMap.containsKey(job.getIdentifier() + "-" + job.getChildContextName())) {
                    job.setStatus(instanceStatus);
                }
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> _setJobStatusAll(c, internalEventDrivenJobInstanceMap, instanceStatus));
        }
    }

    /**
     * Releases all the jobs associated with the given context instance.
     *
     * @param context The context instance for which the jobs need to be released.
     * @param schedulerJobInstanceMap The map containing the internal event driven job instances associated with the context.
     */
    public static void releaseAllJobs(ContextInstance context, Map<String, SchedulerJobInstance> schedulerJobInstanceMap) {
        _releaseAllJobs(context, schedulerJobInstanceMap);
    }

    /**
     * Releases all the jobs in the given context and its child contexts that are on hold.
     *
     * @param context                      the context instance containing the jobs
     * @param schedulerJobInstanceMap the map of internal event-driven job instances
     */
    private static void _releaseAllJobs(ContextInstance context, Map<String, SchedulerJobInstance> schedulerJobInstanceMap) {
        if(context.getScheduledJobs() != null) {
            context.getScheduledJobs().stream()
                .filter(job -> schedulerJobInstanceMap.containsKey(job.getIdentifier()+job.getChildContextName())
                    && job.getStatus().equals(InstanceStatus.ON_HOLD))
                .forEach(job -> {
                    Map<String, Boolean> heldMap = new HashMap<>();
                    job.setHeldContexts(heldMap);
                    job.setHeld(false);
                    job.setStatus(InstanceStatus.RELEASED);
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> _holdAllJobs((ContextInstance) c, schedulerJobInstanceMap));
        }
    }

    /**
     * Enriches the jobs in the given context instance.
     * The enrichment is done recursively starting from the given context instance.
     *
     * @param context The context instance to enrich the jobs.
     */
    public static void enrichJobs(ContextInstance context) {
        _enrichJobs(context, context);
    }

    /**
     * Enriches the jobs in the given context by calling the private helper method.
     *
     * @param context the main context instance
     * @param child   the child context instance
     */
    private static void enrichJobs(ContextInstance context, Context child) {
        _enrichJobs(context, child);
    }

    /**
     * Enriches the jobs in the given child context with the context name and child context name.
     *
     * @param context The parent context instance.
     * @param child The child context instance.
     */
    private static void _enrichJobs(ContextInstance context, Context child) {
        if(child.getScheduledJobs() != null) {
            child.getScheduledJobs().forEach(job -> {
                ((SchedulerJobInstance)job).setContextName(context.getName());
                ((SchedulerJobInstance)job).setChildContextName(child.getName());
            });
        }

        if(child.getContexts() != null) {
            child.getContexts().forEach(c -> enrichJobs(context, (Context) c));
        }
    }

    /**
     * Enriches the jobs in the given scheduler job map by populating additional information using the given context.
     *
     * @param context           the context instance used to enrich the jobs
     * @param schedulerJobMap   the map of scheduler jobs to be enriched
     */
    public static void enrichJobs(ContextInstance context, Map<String, SchedulerJob> schedulerJobMap) {
        _enrichJobs(context, context, schedulerJobMap);
    }

    /**
     * Enriches the jobs in the given context and child with additional information from the schedulerJobMap.
     *
     * @param context          the parent context instance
     * @param child            the child context instance
     * @param schedulerJobMap  a map of scheduler jobs, keyed by a unique identifier
     */
    private static void enrichJobs(ContextInstance context, Context child, Map<String, SchedulerJob> schedulerJobMap) {
        _enrichJobs(context, child, schedulerJobMap);
    }

    /**
     * Enriches the scheduled jobs in the child context with additional information.
     *
     * @param context The parent context instance
     * @param child The child context instance
     * @param schedulerJobMap A map of job names to SchedulerJob instances
     */
    private static void _enrichJobs(ContextInstance context, Context child, Map<String, SchedulerJob> schedulerJobMap) {
        if(child.getScheduledJobs() != null) {
            child.getScheduledJobs().forEach(job -> {
                ((SchedulerJobInstance)job).setContextName(context.getName());
                ((SchedulerJobInstance)job).setChildContextName(child.getName());
                if(schedulerJobMap.containsKey(((SchedulerJobInstance)job).getJobName())) {
                    ((SchedulerJobInstance) job).setDisplayName
                        (schedulerJobMap.get((((SchedulerJobInstance) job).getJobName())).getDisplayName());
                }
            });
        }

        if(child.getContexts() != null) {
            child.getContexts().forEach(c -> enrichJobs(context, (Context) c, schedulerJobMap));
        }
    }


    /**
     * Returns a list of contexts where the specified job resides.
     *
     * @param context the main context to search for the job
     * @param jobName the name of the job to search for
     * @return a list of contexts where the job resides
     */
    private static List<String> getContextsWhereJobResides(Context context, String jobName) {
        List<String> results = new ArrayList<>();
        getContextsWhereJobResides(results, context, jobName);
        return results;
    }

    /**
     * This method is used to find the contexts where a specified job resides.
     * It recursively searches through the given context and its child contexts to find the job.
     *
     * @param results  [IN/OUT] A list to store the names of the contexts where the job is found
     * @param context  [IN] The context to search within
     * @param jobName  [IN] The name of the job to be searched
     */
    private static void getContextsWhereJobResides(List<String> results, Context context, String jobName) {
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                if(((SchedulerJob)job).getJobName().equals(jobName)) {
                    results.add(context.getName());
                }
            });
        }

        if(context.getContexts() != null && !context.getContexts().isEmpty()) {
            context.getContexts().forEach(child -> getContextsWhereJobResides(results, (Context) child, jobName));
        }
    }

    /**
     * Retrieves a list of contexts from the given context where the job name matches the provided filter.
     *
     * @param context the context object from where to start searching for job name matches
     * @param jobNameFilter the filter to match against job names
     * @return a list of unique contexts where the job name matches the filter
     */
    public static List<String> getContextsWhereJobFilterMatchResides(Context context, String jobNameFilter) {
        List<String> results = new ArrayList<>();
        getContextsWhereJobFilterMatchResides(results, context, jobNameFilter);
        return results.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Retrieves the contexts where a job filter match resides.
     *
     * @param results         the list to store the matching context names
     * @param context         the current context to search for job filter match
     * @param jobNameFilter   the job name filter to match with job names or display names within the context
     */
    private static void getContextsWhereJobFilterMatchResides(List<String> results, Context context, String jobNameFilter) {
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                if(((SchedulerJob)job).getJobName() != null && ((SchedulerJob)job).getJobName().toLowerCase().contains(jobNameFilter.toLowerCase())) {
                    results.add(context.getName());
                }
                else if(((SchedulerJob)job).getDisplayName() != null
                    && ((SchedulerJob)job).getDisplayName().toLowerCase().contains(jobNameFilter.toLowerCase())) {
                    results.add(context.getName());
                }
            });
        }

        if(context.getName().toLowerCase().contains(jobNameFilter.toLowerCase()) && !results.contains(context.getName())) {
            results.add(context.getName());
        }

        if(context.getContexts() != null && !context.getContexts().isEmpty()) {
            context.getContexts().forEach(child -> getContextsWhereJobFilterMatchResides(results, (Context) child, jobNameFilter));
        }
    }


    public static List<String> getContextsWhereJobNameMatchResides(Context context, String jobName) {
        List<String> results = new ArrayList<>();
        getContextsWhereJobNameMatchResides(results, context, jobName);
        return results.stream().distinct().collect(Collectors.toList());
    }


    private static void getContextsWhereJobNameMatchResides(List<String> results, Context context, String jobName) {
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                if(((SchedulerJob)job).getJobName() != null && ((SchedulerJob)job).getJobName().equals(jobName)) {
                    results.add(context.getName());
                }
                else if(((SchedulerJob)job).getDisplayName() != null
                    && ((SchedulerJob)job).getDisplayName().equals(jobName)) {
                    results.add(context.getName());
                }
            });
        }


        if(context.getContexts() != null && !context.getContexts().isEmpty()) {
            context.getContexts().forEach(child -> getContextsWhereJobNameMatchResides(results, (Context) child, jobName));
        }
    }

    /**
     * Retrieves all contexts within the given context and its child contexts recursively.
     *
     * @param context the starting context to retrieve all contexts from
     * @return a Map containing all the contexts, where the key is the name of the context and the value is the context object itself
     */
    public static Map<String, Context> getAllContexts(Context context) {
        Map<String, Context> contextMap = new HashMap<>();
        contextMap.put(context.getName(), context);

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllContexts((Context) c, contextMap);
            });
        }

        return contextMap;
    }


    /**
     * Recursively retrieves all contexts from the given context and adds them to the context map.
     *
     * @param context The starting context.
     * @param contextMap The map to store the retrieved contexts.
     */
    private static void getAllContexts(Context context, Map<String, Context> contextMap) {
        contextMap.put(context.getName(), context);

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllContexts((Context) c, contextMap);
            });
        }
    }

    /**
     * Returns a map of all jobs in the given context and its child contexts.
     *
     * @param context The context instance to retrieve jobs from. It must not be null.
     * @return A map of all jobs in the context and its child contexts. The key of the map is the concatenation
     *         of the job key and child context name, and the value is the corresponding SchedulerJobInstance object.
     */
    public static Map<String, SchedulerJobInstance> getAllJobs(ContextInstance context) {
        Map<String, SchedulerJobInstance> contextMap = new HashMap<>();

        if(context.getScheduledJobsMap() != null
            && !context.getScheduledJobsMap().isEmpty()) {
            context.getScheduledJobsMap().entrySet().forEach(entry -> {
                contextMap.put(entry.getKey()+entry.getValue().getChildContextName()
                    , entry.getValue());
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllJobs(c, contextMap);
            });
        }

        return contextMap;
    }


    /**
     * Recursively retrieves all jobs from the given context and adds them to a map.
     *
     * @param context     the context instance to retrieve jobs from
     * @param contextMap  the map to store the retrieved jobs
     */
    private static void getAllJobs(ContextInstance context, Map<String, SchedulerJobInstance> contextMap) {
        if(context.getScheduledJobsMap() != null
            && !context.getScheduledJobsMap().isEmpty()) {
            context.getScheduledJobsMap().entrySet().forEach(entry -> {
                contextMap.put(entry.getKey()+entry.getValue().getChildContextName()
                    , entry.getValue());
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllJobs(c, contextMap);
            });
        }
    }

    /**
     * Retrieves all the SchedulerJob objects from the given ContextTemplate and its child ContextTemplates recursively.
     *
     * @param context The ContextTemplate from which to retrieve the SchedulerJob objects.
     * @return A List of SchedulerJob objects found in the given ContextTemplate and its child ContextTemplates.
     */
    public static List<SchedulerJob> getAllJobs(ContextTemplate context) {
        List<SchedulerJob> contextMap = new ArrayList<>();

        if(context.getScheduledJobsMap() != null
            && !context.getScheduledJobsMap().isEmpty()) {
            context.getScheduledJobsMap().entrySet().forEach(entry -> {
                contextMap.add(entry.getValue());
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllJobs(c, contextMap);
            });
        }

        return contextMap;
    }


    /**
     * Retrieves all jobs from the provided context and its child contexts recursively
     *
     * @param context The context to retrieve jobs from
     * @param contextMap The list to store the retrieved jobs
     */
    private static void getAllJobs(ContextTemplate context, List<SchedulerJob> contextMap) {
        if(context.getScheduledJobsMap() != null
            && !context.getScheduledJobsMap().isEmpty()) {
            context.getScheduledJobsMap().entrySet().forEach(entry -> {
                contextMap.add(entry.getValue());
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllJobs(c, contextMap);
            });
        }
    }


    /**
     * Retrieves the instance of a SchedulerJob for a given job name, child context name, and context instance.
     *
     * @param jobName The name of the job.
     * @param childContextName The name of the child context.
     * @param contextInstance The context instance.
     * @return The SchedulerJobInstance if found, or null if no matching job instance is found.
     */
    public static SchedulerJobInstance getSchedulerJobInstance(String jobName, String childContextName, ContextInstance contextInstance) {
        ContextInstance instance = ContextHelper.getChildContextInstance(childContextName, contextInstance);
        if (instance != null) {
            Optional<SchedulerJobInstance> jobInstance = instance.getScheduledJobs().stream()
                .filter(job -> jobName.equals(job.getJobName())).findFirst();

            if(jobInstance.isPresent()) {
                return jobInstance.get();
            }
        }
        return null;
    }

    /**
     * Returns a list of all agents.
     *
     * @param context the context used to retrieve the agents
     * @return a list of agents
     */
    public static List<String> getAllAgents(Context context) {
        HashSet<String> agentSet = new HashSet<>();

        populateAgentSet(context, agentSet);

        return new ArrayList<>(agentSet);
    }

    /**
     * This method is used to get all agents in the provided context and add them to the given HashSet.
     *
     * @param context The context in which the method is called.
     * @param agentSet The HashSet to which the agents will be added.
     */
    private static void getAllAgents(Context context, HashSet<String> agentSet) {
        populateAgentSet(context, agentSet);
    }

    /**
     * Populates the agentSet with unique agent names from the given context.
     *
     * @param context   The context object from which to retrieve agent names.
     * @param agentSet  The set to populate with unique agent names.
     */
    private static void populateAgentSet(Context context, HashSet<String> agentSet) {
        if(context.getScheduledJobs()!= null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                if(!agentSet.contains(((SchedulerJob)job).getAgentName()) &&
                    !((SchedulerJob)job).getAgentName().equals(JobConstants.GLOBAL_EVENT) &&
                    !((SchedulerJob)job).getAgentName().equals(JobConstants.LOCAL_EVENT_JOB) &&
                    !((SchedulerJob)job).getAgentName().equals(JobConstants.CONTEXT_START_JOB) &&
                    !((SchedulerJob)job).getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)){
                    agentSet.add(((SchedulerJob)job).getAgentName());
                }
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllAgents((Context) c, agentSet);
            });
        }
    }
    /**
     * Retrieves all job dependency identifiers from the given context.
     *
     * @param context the context containing the job dependencies
     * @return a list of unique job dependency identifiers
     */
    public static List<String> getAllJobDependencyIdentifiers(Context context) {
        List<String> results = new ArrayList<>();
        _getAllJobDependencyIdentifiers(context, results);
        return results.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Recursively retrieves all job dependency identifiers within the given context.
     * This method populates the provided results list with the retrieved identifiers.
     *
     * @param context The context to search for job dependencies.
     * @param results The list to store the retrieved job dependency identifiers.
     */
    private static void  _getAllJobDependencyIdentifiers(Context context, List<String> results) {
        if(context.getJobDependencies() != null && !context.getJobDependencies().isEmpty()) {
            context.getJobDependencies().forEach(jobDependency -> {
                getAllJobsInJobDependencies((JobDependency) jobDependency, results);
            });
        }


        if(context.getContexts() != null) {
             context.getContexts().forEach(child -> _getAllJobDependencyIdentifiers((Context)child, results));
        }
    }
    /**
     * Recursively retrieves all job identifiers in a job dependency tree.
     *
     * @param jobDependency the root job dependency
     * @param results       the list of job identifiers to store the results
     */
    private static void getAllJobsInJobDependencies(JobDependency jobDependency, List<String> results) {
        results.add(jobDependency.getJobIdentifier());

        if(jobDependency.getLogicalGrouping() != null) {
            getAllJobsInJobDependencies(jobDependency.getLogicalGrouping(), results);
        }
    }

    /**
     * This method recursively collects all the job identifiers in the given logical grouping
     * and its subgroups, and stores them in the provided results list.
     *
     * @param logicalGrouping the logical grouping to collect job identifiers from
     * @param results         the list to store the collected job identifiers in
     */
    private static void getAllJobsInJobDependencies(LogicalGrouping logicalGrouping, List<String> results) {
        if(logicalGrouping.getAnd() != null) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getIdentifier() != null) results.add(and.getIdentifier());
                if (and.getLogicalGrouping() != null) {
                    getAllJobsInJobDependencies(and.getLogicalGrouping(), results);
                }
            });
        }

        if(logicalGrouping.getOr() != null) {
            logicalGrouping.getOr().forEach(or -> {
                if(or.getIdentifier() != null) results.add(or.getIdentifier());
                if (or.getLogicalGrouping() != null) {
                    getAllJobsInJobDependencies(or.getLogicalGrouping(), results);
                }
            });
        }

        if(logicalGrouping.getNot() != null) {
            logicalGrouping.getNot().forEach(not -> {
                if(not.getIdentifier() != null) results.add(not.getIdentifier());
                if (not.getLogicalGrouping() != null) {
                    getAllJobsInJobDependencies(not.getLogicalGrouping(), results);
                }
            });
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            getAllJobsInJobDependencies(logicalGrouping.getLogicalGrouping(), results);
        }
    }

    /**
     * Sets the replacement for the agent name.
     *
     * @param agentNameReplacement the replacement string for the agent name
     */
    public void setAgentNameReplacement(String agentNameReplacement) {
        AGENT_NAME_REPLACEMENT = agentNameReplacement;
    }

    /**
     * Sets the replacement for the context name.
     *
     * @param contextNameReplacement the replacement for the context name
     */
    public void setContextNameReplacement(String contextNameReplacement) {
        CONTEXT_NAME_REPLACEMENT = contextNameReplacement;
    }

    /**
     * Sets the replacement value for environment variable names.
     *
     * @param envNameReplacement the replacement value for environment variable names
     */
    public void setEnvNameReplacement(String envNameReplacement) {
        ENV_NAME_REPLACEMENT = envNameReplacement;
    }

    /**
     * Sets whether to use the underscore separated context name convention.
     *
     * @param useUnderscoreSeparatedContextNameConvention a boolean value indicating whether to use the convention
     */
    public void setUseUnderscoreSeparatedContextNameConvention(boolean useUnderscoreSeparatedContextNameConvention) {
        USE_UNDERSCORE_SEPARATED_CONTEXT_NAME_CONVENTION = useUnderscoreSeparatedContextNameConvention;
    }
}
