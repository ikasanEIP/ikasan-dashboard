package org.ikasan.job.orchestration.context.parameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;

public class ContextParametersInstanceServiceImpl implements ContextParametersInstanceService {

    private final ContextParametersFactory contextParametersFactory;

    public ContextParametersInstanceServiceImpl(ContextParametersFactory contextParametersFactory) {
        if(contextParametersFactory == null) {
            throw new IllegalArgumentException("contextParametersFactory cannot be null!");
        }
        this.contextParametersFactory = contextParametersFactory;
    }

    @Override
    public void populateContextParameters() {
        contextParametersFactory.populateContextParameters();
    }

    @Override
    public String getContextParameterValue(String contextName, String parameterValue) {
        return contextParametersFactory.getContextParameter(contextName, parameterValue);
    }

    @Override
    public boolean isSkipped(String contextName, String jobName) {
        return contextParametersFactory.isSkipped(contextName, jobName);
    }

    @Override
    public List<ContextParameterInstance> getAllContextParameters(String contextName) {
        return contextParametersFactory.getAllContextParameters(contextName);
    }

    @Override
    public void populateContextParametersOnContextInstance(ContextInstance contextInstance) {
        List<ContextParameterInstance> propertyBackedContextParameterInstances
            = this.getAllContextParameters(contextInstance.getName());

        List<ContextParameterInstance> defaultContextParameterInstances
            = contextInstance.getContextParameters();

        /**
         * Now create a map of all the property backed context parameters.
         */
        Map<String, ContextParameterInstance> finalContextParameters = new HashMap<>();
        propertyBackedContextParameterInstances.forEach(contextParameterInstance
            -> finalContextParameters.put(contextParameterInstance.getName(), contextParameterInstance));

        /**
         * Then iterate over the default parameter instances and add any to the final
         * list that were not populated from properties.
         */
        defaultContextParameterInstances.forEach(contextParameterInstance -> {
            if(!finalContextParameters.containsKey(contextParameterInstance.getName())) {
                contextParameterInstance.setValue(contextParameterInstance.getDefaultValue());
                finalContextParameters.put(contextParameterInstance.getName(), contextParameterInstance);
            }
        });

        /**
         * Now set the aggregated context parameters onto the context instance.
         */
        contextInstance.setContextParameters(finalContextParameters.values().stream().collect(Collectors.toList()));
    }
}
