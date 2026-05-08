package org.ikasan.orchestration.service.scheduled.instance;

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
}
