package org.ikasan.scheduled.instance.model;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.ConcurrentHashMap;

public class SolrContextInstanceImplTest {

    @Test
    public void test_held_jobs_initialised_on_construction() {
        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();

        Assert.assertNotNull(contextInstance.getHeldJobs());
        Assert.assertTrue(contextInstance.getHeldJobs().isEmpty());
        Assert.assertTrue(contextInstance.getHeldJobs() instanceof ConcurrentHashMap);
    }

    @Test
    public void test_held_jobs_can_be_added_to_without_being_set() {
        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        SchedulerJobInitiationEvent event = Mockito.mock(SchedulerJobInitiationEvent.class);

        contextInstance.getHeldJobs().put("agentName-jobName_childContext", event);

        Assert.assertSame(event, contextInstance.getHeldJobs().get("agentName-jobName_childContext"));
    }
}
