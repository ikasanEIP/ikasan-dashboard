package org.ikasan.scheduled.instance.service;

import org.junit.Assert;
import org.junit.Test;

public class SolrSchedulerJobInstancesInitialisationParametersImplTest {

    @Test
    public void test_initialise_with_jobs_on_hold_true() {
        SolrSchedulerJobInstancesInitialisationParametersImpl parameters =
            new SolrSchedulerJobInstancesInitialisationParametersImpl(true);

        Assert.assertTrue(parameters.isInitialiseWithJobsOnHold());
    }

    @Test
    public void test_initialise_with_jobs_on_hold_false() {
        SolrSchedulerJobInstancesInitialisationParametersImpl parameters =
            new SolrSchedulerJobInstancesInitialisationParametersImpl(false);

        Assert.assertFalse(parameters.isInitialiseWithJobsOnHold());
    }

    @Test
    public void test_multiple_instances_with_different_values() {
        SolrSchedulerJobInstancesInitialisationParametersImpl parametersTrue =
            new SolrSchedulerJobInstancesInitialisationParametersImpl(true);

        SolrSchedulerJobInstancesInitialisationParametersImpl parametersFalse =
            new SolrSchedulerJobInstancesInitialisationParametersImpl(false);

        Assert.assertTrue(parametersTrue.isInitialiseWithJobsOnHold());
        Assert.assertFalse(parametersFalse.isInitialiseWithJobsOnHold());

        // Verify they are independent instances
        Assert.assertNotEquals(parametersTrue.isInitialiseWithJobsOnHold(),
            parametersFalse.isInitialiseWithJobsOnHold());
    }

    @Test
    public void test_consistent_return_value() {
        SolrSchedulerJobInstancesInitialisationParametersImpl parameters =
            new SolrSchedulerJobInstancesInitialisationParametersImpl(true);

        // Call multiple times to ensure value doesn't change
        Assert.assertTrue(parameters.isInitialiseWithJobsOnHold());
        Assert.assertTrue(parameters.isInitialiseWithJobsOnHold());
        Assert.assertTrue(parameters.isInitialiseWithJobsOnHold());
    }
}
