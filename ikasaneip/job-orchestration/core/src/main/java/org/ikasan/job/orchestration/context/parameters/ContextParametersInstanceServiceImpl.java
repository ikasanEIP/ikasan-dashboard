package org.ikasan.job.orchestration.context.parameters;

import java.util.List;

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

}
