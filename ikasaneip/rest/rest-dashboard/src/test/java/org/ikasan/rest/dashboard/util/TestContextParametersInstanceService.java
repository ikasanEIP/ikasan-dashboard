package org.ikasan.rest.dashboard.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;

public class TestContextParametersInstanceService implements ContextParametersInstanceService {

    private Map<String, List<ContextParameterInstance>> params = new HashMap<>();

    @Override
    public void populateContextParameters() {
        // nothing to do
    }

    @Override
    public String getContextParameterValue(String contextName, String parameterValue) {
        return null;
    }

    @Override
    public List<ContextParameterInstance> getAllContextParameters(String contextName) {
        return params.get(contextName);
    }

    @Override
    public boolean isSkipped(String contextName, String jobName) {
        return false;
    }

    public void addParamsToContext(String contextName, List<ContextParameterInstance> contextParams) {
        params.put(contextName, contextParams);
    }
}
