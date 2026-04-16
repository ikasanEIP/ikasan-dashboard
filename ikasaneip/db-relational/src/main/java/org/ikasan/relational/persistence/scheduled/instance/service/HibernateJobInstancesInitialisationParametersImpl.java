package org.ikasan.relational.persistence.scheduled.instance.service;

import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;

public class HibernateJobInstancesInitialisationParametersImpl implements SchedulerJobInstancesInitialisationParameters {

    private boolean initialiseWithJobsOnHold;

    /**
     * Constructs an instance of HibernateJobInstancesInitialisationParametersImpl.
     *
     * @param initialiseWithJobsOnHold indicates whether the job instances should be
     *                                  initialized with jobs on hold.
     */
    public HibernateJobInstancesInitialisationParametersImpl(boolean initialiseWithJobsOnHold) {
        this.initialiseWithJobsOnHold = initialiseWithJobsOnHold;
    }

    @Override
    public boolean isInitialiseWithJobsOnHold() {
        return initialiseWithJobsOnHold;
    }
}
