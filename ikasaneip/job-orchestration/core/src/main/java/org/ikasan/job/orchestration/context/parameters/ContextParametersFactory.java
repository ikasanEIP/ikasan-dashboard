package org.ikasan.job.orchestration.context.parameters;

import org.ikasan.job.orchestration.context.util.SchedulerContextParametersPropertiesProvider;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;

import java.util.List;

public class ContextParametersFactory {

    private final SchedulerContextParametersPropertiesProvider schedulerContextParametersPropertiesProvider;

    public ContextParametersFactory(SchedulerContextParametersPropertiesProvider schedulerContextParametersPropertiesProvider) {
        if(schedulerContextParametersPropertiesProvider == null) {
            throw new IllegalArgumentException("schedulerContextParametersProvider cannot be null!");
        }
        this.schedulerContextParametersPropertiesProvider = schedulerContextParametersPropertiesProvider;
    }

    public void populateContextParameters() {
        // at the moment no point as on startup properties are read
    }

    public List<ContextParameterInstance> getAllContextParameters(String contextName) {
        return schedulerContextParametersPropertiesProvider.getAllContextParameters(contextName);
    }

    public String getContextParameter(String contextName, String parameterValue) {
        return schedulerContextParametersPropertiesProvider.getContextParameter(contextName, parameterValue);
    }
}
