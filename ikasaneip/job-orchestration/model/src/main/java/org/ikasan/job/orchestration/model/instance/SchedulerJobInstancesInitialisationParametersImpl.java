package org.ikasan.job.orchestration.model.instance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;

public class SchedulerJobInstancesInitialisationParametersImpl implements SchedulerJobInstancesInitialisationParameters {
    private boolean initialiseWithJobsOnHold;

    public SchedulerJobInstancesInitialisationParametersImpl(boolean initialiseWithJobsOnHold) {
        this.initialiseWithJobsOnHold = initialiseWithJobsOnHold;
    }

    @Override
    public boolean isInitialiseWithJobsOnHold() {
        return initialiseWithJobsOnHold;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
