package org.ikasan.job.orchestration.context.parameters;

import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public List<ContextParameterInstance> getAllContextParameters(String contextName) {
        return contextParametersFactory.getAllContextParameters(contextName);
    }

    @Override
    public void populateContextParametersOnContextInstance(ContextInstance contextInstance,
                                                           Map<String, InternalEventDrivenJobInstance> internalJobs) {
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

        List<ContextParameterInstance> parameterInstancesFromJobs
            = ContextHelper.getUniqueContextParameterInstancesFromJobInstances(internalJobs);

        /**
         * Now iterate over any context parameter instances that may be on jobs that have not been included.
         */
        parameterInstancesFromJobs.forEach(contextParameterInstance -> {
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

    @Override
    public List<ContextParameterInstance> getContextParameterInstancesForContext(ContextTemplate contextTemplate
        , Map<String, InternalEventDrivenJob> internalJobs) {
        List<ContextParameterInstance> propertyBackedContextParameterInstances
            = this.getAllContextParameters(contextTemplate.getName());

        List<ContextParameter> defaultContextParameters
            = contextTemplate.getContextParameters();

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
        defaultContextParameters.forEach(contextParameter -> {
            if(!finalContextParameters.containsKey(contextParameter.getName())) {
                ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
                contextParameterInstance.setValue(contextParameter.getDefaultValue());
                contextParameterInstance.setName(contextParameter.getName());
                contextParameterInstance.setDefaultValue(contextParameter.getDefaultValue());

                finalContextParameters.put(contextParameter.getName(), contextParameterInstance);
            }
        });

        List<ContextParameterInstance> parameterInstancesFromJobs
            = ContextHelper.getUniqueContextParameterInstancesFromJobs(internalJobs);

        /**
         * Now iterate over any context parameter instances that may be on jobs that have not been included.
         */
        parameterInstancesFromJobs.forEach(contextParameterInstance -> {
            if(!finalContextParameters.containsKey(contextParameterInstance.getName())) {
                contextParameterInstance.setValue(contextParameterInstance.getDefaultValue());
                finalContextParameters.put(contextParameterInstance.getName(), contextParameterInstance);
            }
        });

        /**
         * Now set the aggregated context parameters onto the context instance.
         */
        return finalContextParameters.values().stream().collect(Collectors.toList());
    }
}
