package org.ikasan.job.orchestration.context.util;

import java.util.*;

import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerOverrider extends Properties {
    private final static Logger LOG = LoggerFactory.getLogger(SchedulerOverrider.class);

    private final boolean useSkipJobs;
    private final Map<String, Map<String, Boolean>> jobsToSkip;
    private final boolean replaceContextParams;
    private final Map<String, Map<String, String>> paramsToReplace;

    public SchedulerOverrider(boolean useSkipJobs,
                              Map<String, Map<String, Boolean>> jobsToSkip,
                              boolean replaceContextParams,
                              Map<String, Map<String, String>> paramsToReplace) {

        this.useSkipJobs = useSkipJobs;
        this.jobsToSkip = jobsToSkip == null ? Collections.emptyMap() : jobsToSkip;
        this.replaceContextParams = replaceContextParams;
        this.paramsToReplace = paramsToReplace == null ? Collections.emptyMap() : paramsToReplace;

        String message = String.format("Creating TestOverride configuration with use jobsToSkip %b, " +
            "jobsToSkips %s, replaceContextParams %b, paramsToReplace %s", this.useSkipJobs, this.jobsToSkip, this.replaceContextParams, this.paramsToReplace);
        LOG.info(message);
    }

    public boolean isSkipped(String contextName, String jobName) {
        if (useSkipJobs && contextName != null && jobName != null) {
            Map<String, Boolean> jobsToSkipMap = jobsToSkip.get(contextName);
            if (jobsToSkipMap != null) {
                Boolean shouldSkip = jobsToSkipMap.get(jobName);
                return shouldSkip != null && shouldSkip;
            }
        }
        return false;
    }

    public String getReplacementForContextParamName(String contextName, String paramName) {
        if (replaceContextParams && contextName != null && paramName != null) {
            Map<String, String> paramMap = paramsToReplace.get(contextName);
            if (paramMap != null) {
                return paramMap.get(paramName);
            }
        }
        return null;
    }

    public List<ContextParameterInstance> getAllContextParameters(String contextName) {
        List<ContextParameterInstance> params = new ArrayList<>();
        if (contextName != null) {
            Map<String, String> paramMap = paramsToReplace.get(contextName);
            if (paramMap != null) {
                for (String name : paramMap.keySet()) {
                    ContextParameterInstanceImpl param = new ContextParameterInstanceImpl();
                    param.setName(name);
                    param.setValue(paramMap.get(name));
                    param.setType("java.lang.String");
                    params.add(param);
                }
            }
        }
        return params;
    }

}
