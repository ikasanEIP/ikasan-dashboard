package org.ikasan.job.orchestration.util;

import org.ikasan.job.orchestration.model.status.ContextJobInstanceDetailsStatusImpl;
import org.ikasan.job.orchestration.model.context.ContextTransition;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.status.ContextJobInstanceStatusImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.ExternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
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

public class ContextHelper {

    static Logger logger = LoggerFactory.getLogger(ContextHelper.class);

    private static String AGENT_NAME_REPLACEMENT = "[[agent.name]]";
    private static String CONTEXT_NAME_REPLACEMENT = "[[context.name]]";
    private static String ENV_NAME_REPLACEMENT = "[[env.name]]";
    
    private static boolean USE_UNDERSCORE_SEPARATED_CONTEXT_NAME_CONVENTION = true;

    public static final String GLOBAL_EVENT = "GLOBAL_EVENT";

    /**
     * Helper method to add replacement tokens to a scheduler job.
     *
     * @param schedulerJob
     */
    public static void addSchedulerJobReplacementTokens(SchedulerJob schedulerJob) {
        if(!schedulerJob.getAgentName().equals(GLOBAL_EVENT)) {
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
    }

    /**
     * Recursively work through the ContextTemplate to add all relevant replacement tokens.
     *
     * @param contextTemplate
     */
    private static void _addContextTemplateReplacementTokens(ContextTemplate contextTemplate) {
        if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(schedulerJob -> {
                contextTemplate.getJobDependencies().forEach(jobDependency -> {
                    replaceJobIdentifierJobDependency(schedulerJob, jobDependency);
                });

                if(!schedulerJob.getAgentName().equals(GLOBAL_EVENT)) {
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
                        if(!job.getAgentName().equals(GLOBAL_EVENT)) {
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
    private static String getContextName(String contextName) {
        if(contextName.equals(GLOBAL_EVENT)) {
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
            if(!schedulerJob.getIdentifier().startsWith(GLOBAL_EVENT)) {
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
        if(and.getIdentifier().startsWith(GLOBAL_EVENT)) return;
        and.setIdentifier(AGENT_NAME_REPLACEMENT+"-"+schedulerJob.getJobName());
    }

    /**
     * Replace token in logical or.
     *
     * @param schedulerJob
     * @param or
     */
    private static void replaceJobIdentifierOr(SchedulerJob schedulerJob, Or or) {
        if(or.getIdentifier().startsWith(GLOBAL_EVENT)) return;
        or.setIdentifier(AGENT_NAME_REPLACEMENT+"-"+schedulerJob.getJobName());
    }

    /**
     * Replace token in logical not.
     *
     * @param schedulerJob
     * @param not
     */
    private static void replaceJobIdentifierNot(SchedulerJob schedulerJob, Not not) {
        if(not.getIdentifier().startsWith(GLOBAL_EVENT)) return;
        not.setIdentifier(AGENT_NAME_REPLACEMENT+"-"+schedulerJob.getJobName());
    }

    /**
     *
     * @param internalJobs
     * @return
     */
    public static List<ContextParameterInstance> getUniqueContextParameterInstancesFromJobInstances(Map<String, InternalEventDrivenJobInstance> internalJobs) {
        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();

        internalJobs.entrySet().forEach(entry ->
            contextParameterInstances.addAll(entry.getValue().getContextParameters().stream()
            .map(contextParameter -> {
                ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
                contextParameterInstance.setName(contextParameter.getName());
                contextParameterInstance.setValue(contextParameter.getDefaultValue());
                contextParameterInstance.setDefaultValue(contextParameter.getDefaultValue());

                return contextParameterInstance;
            })
            .collect(Collectors.toList())));

        return contextParameterInstances.stream()
            .filter(distinctByKey(contextParameterInstance -> contextParameterInstance.getName()) )
            .collect( Collectors.toList() );
    }

    /**
     *
     * @param internalJobs
     * @return
     */
    public static List<ContextParameterInstance> getUniqueContextParameterInstancesFromJobs(Map<String, InternalEventDrivenJob> internalJobs) {
        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();

        internalJobs.entrySet().forEach(entry ->
            contextParameterInstances.addAll(entry.getValue().getContextParameters().stream()
                .map(contextParameter -> {
                    ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
                    contextParameterInstance.setName(contextParameter.getName());
                    contextParameterInstance.setValue(contextParameter.getDefaultValue());
                    contextParameterInstance.setDefaultValue(contextParameter.getDefaultValue());

                    return contextParameterInstance;
                })
                .collect(Collectors.toList())));

        return contextParameterInstances.stream()
            .filter(distinctByKey(contextParameterInstance -> contextParameterInstance.getName()) )
            .collect( Collectors.toList() );
    }

    /**
     * The purpose of this method is to determine if a job has dependencies that transition
     * outside the child context provided.
     *
     * @param parentContext the parent context.
     * @param schedulerJobs the map of scheduler jobs in the child context.
     * @param child the child context we will detremine if jobs transition from.
     * @return
     */
    public static List<ContextTransition> determineIfJobsTransitionToOtherContexts(Context parentContext, Map<String, SchedulerJob> schedulerJobs
        , Context child, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        List<ContextTransition> contextTransitions = new ArrayList<>();

        // Get any jobs that represent the last jobs in the context that could potentially transition
        // to other contexts
        Map<String, SchedulerJob> jobMap = ContextHelper.getJobsOutsideLogicalGrouping(child);

        // Now iterate over all jobs in the context
        schedulerJobs.entrySet().forEach(entry -> {
            // Trace from each job through the child context provided and any subsequent contexts that the job extends into
            LinkedList<List<SchedulerJob>> linkedJobs = ContextHelper.traceJobThroughContext(parentContext, entry.getValue().getJobName(), child.getName());

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
                            && !internalEventDrivenJobMap.get(precedingJob.getIdentifier()).isTargetResidingContextOnly()) {
                            ContextTransition contextTransition = new ContextTransition();
                            contextTransition.setProceedingJob(precedingJob);
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

    public static AggregateContextInstanceStatus getAggregateContextInstanceStatus(ContextInstance contextInstance) {
        AggregateContextInstanceStatus aggregateContextInstanceStatus = new AggregateContextInstanceStatus();
        getAggregateContextInstanceStatus(contextInstance, aggregateContextInstanceStatus, null, null);

        return aggregateContextInstanceStatus;
    }

    public static AggregateContextInstanceStatus getAggregateContextInstanceStatus(ContextInstance contextInstance, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap,
                                                                                   ContextInstance parent) {
        AggregateContextInstanceStatus aggregateContextInstanceStatus = new AggregateContextInstanceStatus();
        getAggregateContextInstanceStatus(contextInstance, aggregateContextInstanceStatus, internalEventDrivenJobMap, parent);

        return aggregateContextInstanceStatus;
    }

    private static void getAggregateContextInstanceStatus(ContextInstance contextInstance, AggregateContextInstanceStatus aggregateContextInstanceStatus
        , Map<String, InternalEventDrivenJob> internalEventDrivenJobMap, ContextInstance parent) {
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

                if(schedulerJobInstance.getStatus().equals(InstanceStatus.DISABLED)) {
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
                getAggregateContextInstanceStatus(child, aggregateContextInstanceStatus, internalEventDrivenJobMap, parent);
            });
        }
    }

    public static AggregateContextInstanceStatus getAggregateContextInstanceStatus(ContextInstance parent, ContextInstance contextInstance, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        AggregateContextInstanceStatus aggregateContextInstanceStatus = new AggregateContextInstanceStatus();
        getAggregateContextInstanceStatus(parent, contextInstance, aggregateContextInstanceStatus, internalEventDrivenJobMap);

        return aggregateContextInstanceStatus;
    }

    private static void getAggregateContextInstanceStatus(ContextInstance parent, ContextInstance contextInstance
        , AggregateContextInstanceStatus aggregateContextInstanceStatus, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        if(contextInstance.getScheduledJobs() != null) {
            contextInstance.getScheduledJobs().forEach(schedulerJobInstance -> {
                if(!ContextHelper.determineIfJobsTransitionFromOtherContexts(parent,
                    schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName()
                    , internalEventDrivenJobMap).isEmpty()) {
                    return;
                }
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
            = new HashMap<>(context.getScheduledJobsMap());

        if(context.getJobDependencies() != null) {
            ((List<JobDependency>)context.getJobDependencies()).forEach(jobDependency -> {
                if(jobDependency.getLogicalGrouping() != null) {
                    removeJobsInLogicalConstructs(jobDependency.getLogicalGrouping(),
                        jobsOutsideLogicConstructs);
                }
            });
        }

        return jobsOutsideLogicConstructs;
    }

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

    public static List<ContextTransition> determineIfJobsTransitionFromOtherContexts(Context context
        , String jobName, String childContextName, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        Map<String, SchedulerJob> jobs = new HashMap<>();
        internalEventDrivenJobMap.entrySet().forEach(entry -> {
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
     * @param internalEventDrivenJobMap
     * @return
     */
    public static List<ContextTransition> determineIfSchedulerJobsTransitionFromOtherContexts(Context context
        , String jobName, String childContextName, Map<String, SchedulerJob> internalEventDrivenJobMap) {
        List<String> residingContexts = getContextsWhereJobResides(context, jobName);

        Map<String, ContextTransition> finalResults = new HashMap<>();

        for (String name : residingContexts) {
            Context child = ContextHelper.getChildContext(name, context);

            if (child == null) continue;

            Optional<SchedulerJob> schedulerJob = child.getScheduledJobs().stream()
                .filter(job -> jobName.equals(((SchedulerJob)job).getJobName()))
                .findFirst();

            if (schedulerJob.isEmpty()) continue;

            if(internalEventDrivenJobMap.containsKey(schedulerJob.get().getIdentifier())
                && internalEventDrivenJobMap.get(schedulerJob.get().getIdentifier()) instanceof InternalEventDrivenJob) {
                InternalEventDrivenJob instance = (InternalEventDrivenJob) internalEventDrivenJobMap.get(schedulerJob.get().getIdentifier());

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
                    contextTransition.setProceedingJob(schedulerJob.get());
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

    private static void getUpstreamDependencies(LogicalGrouping logicalGrouping, List<SchedulerJob> schedulerJobInstances,
                                                Map<String, SchedulerJob> schedulerJobInstanceMap) {
        if(logicalGrouping != null) {
            assessAnd(logicalGrouping, schedulerJobInstances, schedulerJobInstanceMap);
            assessOr(logicalGrouping, schedulerJobInstances, schedulerJobInstanceMap);
            assessNot(logicalGrouping, schedulerJobInstances, schedulerJobInstanceMap);
        }
    }

    public static String getIdentifier(String identifier) {
        if(identifier.contains("_in")) {
            identifier = identifier.substring(0, identifier.indexOf("_in"));
        }
        else if(identifier.contains("_out")) {
            identifier = identifier.substring(0, identifier.indexOf("_out"));
        }

        return identifier;
    }

    private static <T> Predicate<T> distinctByKey(
        Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

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

    public static LinkedList<List<SchedulerJob>> traceJobThroughContext(Context context, String jobName, String childContextName) {
        LinkedList<List<SchedulerJob>> results = new LinkedList<>();
        List<String> processedContexts = new ArrayList<>();
        _traceJobThroughContext(results, context, jobName, childContextName, processedContexts);

        return results;
    }

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

    private static void getNextJob(Context child, String jobIdentifier, LogicalGrouping logicalGrouping, SchedulerJob schedulerJobInstance, List<SchedulerJob> jobIdentifiers) {
        if(logicalGrouping == null) {
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

    public static ContextTemplate replaceChildContextTemplate(ContextTemplate contextTemplate, ContextTemplate updated) {
        if(contextTemplate.getName().equals(updated.getName())) {
            contextTemplate.setJobDependencies(updated.getJobDependencies());
            contextTemplate.setScheduledJobs(updated.getScheduledJobs());
            return contextTemplate;
        }

        if(contextTemplate.getContexts() != null) {
            for (int i=0; i<contextTemplate.getContexts().size(); i++) {

                if(contextTemplate.getContexts().get(i).getName().equals(updated.getName())) {
                    contextTemplate.getContexts().get(i).setJobDependencies(updated.getJobDependencies());
                    contextTemplate.getContexts().get(i).setScheduledJobs(updated.getScheduledJobs());
                }
                else {
                    replaceChildContextTemplate(contextTemplate.getContexts().get(i), updated);
                }
            }
        }

        return contextTemplate;
    }

    public static void holdAllJobs(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap) {
        _holdAllJobs(context, internalEventDrivenJobInstanceMap);
    }

    private static void _holdAllJobs(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap) {
        if(context.getScheduledJobs() != null) {
            context.getScheduledJobs().forEach(job -> {
                if(internalEventDrivenJobInstanceMap.containsKey(job.getIdentifier() + "-" + job.getChildContextName())
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
            context.getContexts().forEach(c -> _holdAllJobs(c, internalEventDrivenJobInstanceMap));
        }
    }

    public static void setJobStatusAll(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap, InstanceStatus instanceStatus) {
        _setJobStatusAll(context, internalEventDrivenJobInstanceMap, instanceStatus);
    }

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

    public static void releaseAllJobs(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap) {
        _releaseAllJobs(context, internalEventDrivenJobInstanceMap);
    }

    private static void _releaseAllJobs(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap) {
        if(context.getScheduledJobs() != null) {
            context.getScheduledJobs().stream()
                .filter(job -> internalEventDrivenJobInstanceMap.containsKey(job.getIdentifier()+job.getChildContextName())
                    && job.getStatus().equals(InstanceStatus.ON_HOLD))
                .forEach(job -> {
                    Map<String, Boolean> heldMap = new HashMap<>();
                    job.setHeldContexts(heldMap);
                    job.setHeld(false);
                    job.setStatus(InstanceStatus.RELEASED);
            });
        }

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> _holdAllJobs((ContextInstance) c, internalEventDrivenJobInstanceMap));
        }
    }

    public static void enrichJobs(ContextInstance context) {
        _enrichJobs(context, context);
    }

    private static void enrichJobs(ContextInstance context, Context child) {
        _enrichJobs(context, child);
    }

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

    public static void enrichJobs(ContextInstance context, Map<String, SchedulerJob> schedulerJobMap) {
        _enrichJobs(context, context, schedulerJobMap);
    }

    private static void enrichJobs(ContextInstance context, Context child, Map<String, SchedulerJob> schedulerJobMap) {
        _enrichJobs(context, child, schedulerJobMap);
    }

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


    private static List<String> getContextsWhereJobResides(Context context, String jobName) {
        List<String> results = new ArrayList<>();
        getContextsWhereJobResides(results, context, jobName);
        return results;
    }

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

    public static List<String> getContextsWhereJobFilterMatchResides(Context context, String jobNameFilter) {
        List<String> results = new ArrayList<>();
        getContextsWhereJobFilterMatchResides(results, context, jobNameFilter);
        return results.stream().distinct().collect(Collectors.toList());
    }

    private static void getContextsWhereJobFilterMatchResides(List<String> results, Context context, String jobNameFilter) {
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                if(((SchedulerJob)job).getJobName().toLowerCase().contains(jobNameFilter.toLowerCase())) {
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


    private static void getAllContexts(Context context, Map<String, Context> contextMap) {
        contextMap.put(context.getName(), context);

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                getAllContexts((Context) c, contextMap);
            });
        }
    }

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

    public static List<String> getAllAgents(Context context) {
        HashSet<String> agentSet = new HashSet<>();

        populateAgentSet(context, agentSet);

        return new ArrayList<>(agentSet);
    }

    private static void getAllAgents(Context context, HashSet<String> agentSet) {
        populateAgentSet(context, agentSet);
    }

    private static void populateAgentSet(Context context, HashSet<String> agentSet) {
        if(context.getScheduledJobs()!= null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                if(!agentSet.contains(((SchedulerJob)job).getAgentName())){
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

    public void setAgentNameReplacement(String agentNameReplacement) {
        AGENT_NAME_REPLACEMENT = agentNameReplacement;
    }

    public void setContextNameReplacement(String contextNameReplacement) {
        CONTEXT_NAME_REPLACEMENT = contextNameReplacement;
    }

    public void setEnvNameReplacement(String envNameReplacement) {
        ENV_NAME_REPLACEMENT = envNameReplacement;
    }

    public void setUseUnderscoreSeparatedContextNameConvention(boolean useUnderscoreSeparatedContextNameConvention) {
        USE_UNDERSCORE_SEPARATED_CONTEXT_NAME_CONVENTION = useUnderscoreSeparatedContextNameConvention;
    }
}
