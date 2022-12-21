package org.ikasan.job.orchestration.util;

import org.ikasan.job.orchestration.model.context.ContextTransition;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ContextHelper {

    static Logger logger = LoggerFactory.getLogger(ContextHelper.class);

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

        finalResults = finalResults.stream().filter(job -> !((SchedulerJobInstance)job).getChildContextName().equals(childContextName) &&
                !theChildContext.getScheduledJobs().stream()
                    .filter(j -> job.getJobName().equals(((SchedulerJob)j).getJobName()))
                    .findFirst()
                    .isPresent())
            .filter(distinctByKey(j -> j.getJobName()))
            .collect(Collectors.toList());

        return finalResults.stream().map(job -> (SchedulerJobInstance)job).collect(Collectors.toList());
    }

    public static List<ContextTransition> determineIfJobsTransitionFromOtherContexts(Context context
        , String jobName, String childContextName, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        List<String> residingContexts = getContextsWhereJobResides(context, jobName);

        Map<String, ContextTransition> finalResults = new HashMap<>();

        for (String name : residingContexts) {
            Context child = ContextHelper.getChildContext(name, context);

            if (child == null) continue;

            Optional<SchedulerJob> schedulerJob = child.getScheduledJobs().stream()
                .filter(job -> jobName.equals(((SchedulerJob)job).getJobName()))
                .findFirst();

            if (schedulerJob.isEmpty()) continue;

            if(internalEventDrivenJobMap.containsKey(schedulerJob.get().getIdentifier())) {
                InternalEventDrivenJob instance = internalEventDrivenJobMap.get(schedulerJob.get().getIdentifier());

                if (instance.isTargetResidingContextOnly()) continue;
            }

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
                ((List<JobDependency>)child.getJobDependencies()).forEach(jobDependency -> {
                    getNextJob(child, jobDependency.getJobIdentifier()
                        , jobDependency.getLogicalGrouping(), schedulerJobInstance.get(), jobs);
                });
            });
        }

        if(!jobs.isEmpty()) {
            results.add(jobs);
            jobs.forEach(job -> getContextsWhereJobResides(context, job.getJobName()).forEach(filtered
                -> {
                if(!filtered.equals(childContextName) && !filtered.isEmpty()) {
                    _traceJobThroughContext(results, context, job.getJobName(), filtered, processedContexts);
                }
            }));
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
        if(contextTemplate.getContexts() != null) {
            for (int i=0; i<contextTemplate.getContexts().size(); i++) {

                if(contextTemplate.getContexts().get(i).getName().equals(updated.getName())) {
                    contextTemplate.getContexts().set(i, updated);
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
                if(internalEventDrivenJobInstanceMap.containsKey(job.getIdentifier() + "-" + job.getChildContextName())) {
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
            context.getContexts().forEach(c -> _holdAllJobs((ContextInstance) c, internalEventDrivenJobInstanceMap));
        }
    }

    public static void releaseAllJobs(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap) {
        _releaseAllJobs(context, internalEventDrivenJobInstanceMap);
    }

    private static void _releaseAllJobs(ContextInstance context, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap) {
        if(context.getScheduledJobs() != null) {
            context.getScheduledJobs().stream().filter(job -> internalEventDrivenJobInstanceMap.containsKey(job.getIdentifier()+job.getChildContextName()))
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
        return results;
    }

    private static void getContextsWhereJobFilterMatchResides(List<String> results, Context context, String jobNameFilter) {
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                if(((SchedulerJob)job).getJobName().toLowerCase().contains(jobNameFilter.toLowerCase())) {
                    results.add(context.getName());
                }
            });
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
}
