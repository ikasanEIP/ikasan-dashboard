package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.HashMap;
import java.util.Map;

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

}
