package org.ikasan.job.orchestration.context.parameters;

import java.util.List;

import org.ikasan.job.orchestration.context.util.SchedulerOverrider;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;

public class ContextParametersFactory {

    // TODO this needs to be replaced with something correct i.e. cloud properties or some other service
    private final SchedulerOverrider schedulerOverrider;

    public ContextParametersFactory(SchedulerOverrider schedulerOverrider) {
        if(schedulerOverrider == null) {
            throw new IllegalArgumentException("schedulerOverrider cannot be null!");
        }
        this.schedulerOverrider = schedulerOverrider;
    }

    public void populateContextParameters() {
        // at the moment no point as on startup properties are re read
    }

    public List<ContextParameterInstance> getAllContextParameters(String contextName) {
        return schedulerOverrider.getAllContextParameters(contextName);
    }

    public String getContextParameter(String contextName, String parameterValue) {
        return schedulerOverrider.getReplacementForContextParamName(contextName, parameterValue);
    }

    public boolean isSkipped(String contextName, String jobName) {
        return schedulerOverrider.isSkipped(contextName, jobName);
    }
}
