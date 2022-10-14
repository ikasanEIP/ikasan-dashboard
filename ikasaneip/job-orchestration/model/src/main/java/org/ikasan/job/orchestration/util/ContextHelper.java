package org.ikasan.job.orchestration.util;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextHelper {

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

    public static SchedulerJobInstance getSchedulerJobInstance(String jobIdentifier, String childContextName, ContextInstance contextInstance) {
        ContextInstance instance = ContextHelper.getChildContextInstance(childContextName, contextInstance);
        if (instance != null) {
            return instance.getScheduledJobsMap().get(jobIdentifier);
        }
        return null;
    }
}
