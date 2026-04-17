package org.ikasan.scheduled.instance.service;

import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;

public class SolrSchedulerJobInstancesInitialisationParametersImpl implements SchedulerJobInstancesInitialisationParameters {

    private boolean initialiseWithJobsOnHold;

    public SolrSchedulerJobInstancesInitialisationParametersImpl(boolean initialiseWithJobsOnHold) {
        this.initialiseWithJobsOnHold = initialiseWithJobsOnHold;
    }

    @Override
    public boolean isInitialiseWithJobsOnHold() {
        return initialiseWithJobsOnHold;
    }
}
