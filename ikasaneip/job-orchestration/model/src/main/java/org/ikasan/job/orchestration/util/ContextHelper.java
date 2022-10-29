package org.ikasan.job.orchestration.util;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextHelper {

    public static LinkedList<List<SchedulerJobInstance>> traceJobThroughContextInstance(ContextInstance context, String jobName, String childContextName) {
        LinkedList<List<SchedulerJobInstance>> results = new LinkedList<>();
        _traceJobThroughContextInstance(results, context, jobName, childContextName);

        return results;
    }
    public static void _traceJobThroughContextInstance(LinkedList<List<SchedulerJobInstance>> results, ContextInstance context, String jobName, String childContextName) {
        ContextInstance child = ContextHelper.getChildContextInstance(childContextName, context);

        Optional<SchedulerJobInstance> schedulerJobInstance = child.getScheduledJobs().stream()
            .filter(job -> job.getJobName().equals(jobName))
            .findFirst();

        List<SchedulerJobInstance> jobs = new ArrayList<>();

        if(schedulerJobInstance.isPresent()) {
            schedulerJobInstance.ifPresent(job -> {
                child.getJobDependencies().forEach(jobDependency -> {
                    getNextJob(child, jobDependency.getJobIdentifier()
                        , jobDependency.getLogicalGrouping(), schedulerJobInstance.get(), jobs);
                });
            });
        }

        if(!jobs.isEmpty()) {
            results.add(jobs);
            jobs.forEach(job -> {
                getContextsWhereJobResides(context, job.getJobName())
                            .forEach(filtered -> {
                                System.out.println(filtered);
                                _traceJobThroughContextInstance(results, context, job.getJobName(), filtered);
                            });
            });
        }
    }

    private static void getNextJob(ContextInstance child, String jobIdentifier, LogicalGrouping logicalGrouping, SchedulerJobInstance schedulerJobInstance, List<SchedulerJobInstance> jobIdentifiers) {
        if(logicalGrouping.getLogicalGrouping() != null) {
            getNextJob(child, jobIdentifier, logicalGrouping.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
        }

        if(logicalGrouping.getAnd() != null) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getLogicalGrouping() != null) {
                    getNextJob(child, jobIdentifier, and.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
                }
                else if(schedulerJobInstance.getIdentifier().equals(and.getIdentifier())) {
                    jobIdentifiers.add(child.getScheduledJobsMap().get(jobIdentifier));
                }
            });
        }

        if(logicalGrouping.getOr() != null) {
            logicalGrouping.getOr().forEach(or -> {
                if(or.getLogicalGrouping() != null) {
                    getNextJob(child, jobIdentifier, or.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
                }
                else if(schedulerJobInstance.getIdentifier().equals(or.getIdentifier())) {
                    jobIdentifiers.add(child.getScheduledJobsMap().get(jobIdentifier));
                }
            });
        }

        if(logicalGrouping.getNot() != null) {
            logicalGrouping.getNot().forEach(not -> {
                if(not.getLogicalGrouping() != null) {
                    getNextJob(child, jobIdentifier, not.getLogicalGrouping(), schedulerJobInstance, jobIdentifiers);
                }
                else if(schedulerJobInstance.getIdentifier().equals(not.getIdentifier())) {
                    jobIdentifiers.add(child.getScheduledJobsMap().get(jobIdentifier));
                }
            });
        }
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
