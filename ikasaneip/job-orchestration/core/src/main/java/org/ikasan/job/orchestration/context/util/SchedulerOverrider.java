package org.ikasan.job.orchestration.context.util;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerOverrider {
    private final static Logger LOG = LoggerFactory.getLogger(SchedulerOverrider.class);

    private final boolean useSkipJobs;
    private final Map<String, Boolean> jobsToSkip;
    private final boolean replaceContextParams;
    private final Map<String, String> paramsToReplace;

    public SchedulerOverrider(boolean useSkipJobs,
                              Map<String, Boolean> jobsToSkip,
                              boolean replaceContextParams,
                              Map<String, String> paramsToReplace) {

        this.useSkipJobs = useSkipJobs;
        this.jobsToSkip = jobsToSkip == null ? Collections.emptyMap() : jobsToSkip;
        this.replaceContextParams = replaceContextParams;
        this.paramsToReplace = paramsToReplace == null ? Collections.emptyMap() : paramsToReplace;

        String message = String.format("Creating TestOverride configuration with use jobsToSkip %b, " +
            "jobsToSkips %s, replaceContextParams %b, paramsToReplace %s", this.useSkipJobs, this.jobsToSkip, this.replaceContextParams, this.paramsToReplace);
        LOG.info(message);
    }

    public boolean isSkipped(String jobName) {
        if (useSkipJobs) {
            Boolean shouldSkip = jobsToSkip.get(jobName);
            return shouldSkip != null && shouldSkip;
        }
        return false;
    }

    public String getReplacementForContextParamName(String paramName) {
        if (replaceContextParams) {
            return paramsToReplace.get(paramName);
        }
        return null;
    }
}
