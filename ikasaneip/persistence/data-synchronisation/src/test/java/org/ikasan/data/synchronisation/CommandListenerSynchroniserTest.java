package org.ikasan.data.synchronisation;

import org.bson.Document;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandListenerSynchroniserTest extends SyncTestSupport {

    private CommandListenerSynchroniser synchroniser;

    private CommandListenerSynchroniser start() throws InterruptedException {
        // Solr counts as loaded, so the first start doesn't reconcile and only captured writes reach Solr
        stateStore.saveFullLoad(new java.util.Date(), 0, 0);
        synchroniser = new CommandListenerSynchroniser(env.writeCaptureListener, pipeline, reconciler, stateStore,
            properties);
        synchroniser.start();
        await("listener active", env.writeCaptureListener::isActive);
        return synchroniser;
    }

    @After
    public void stopSynchroniser() {
        if (synchroniser != null) {
            synchroniser.stop();
        }
    }

    @Test
    public void inserts_replacements_and_deletes_by_id_reach_solr() throws Exception {
        start();

        samples.jobLockCache("env-1");
        await("insert", () -> solrIdsUnchecked("jobLockCache").size() == 1);

        samples.jobLockCache("env-1");
        samples.policy("policy-1");
        await("replacement and second type", () -> solrIdsUnchecked("securityPolicy").size() == 1);
        assertEquals(1, solrIds("jobLockCache").size());

        mongoTemplate.getCollection(SyncCollections.IKASAN).deleteOne(new Document("_id", "jobLockCacheIdentifier__env-1"));
        await("delete by id", () -> solrIdsUnchecked("jobLockCache").isEmpty());
    }

    @Test
    public void writes_before_activation_are_not_captured_but_reconcile_repairs_them() throws Exception {
        samples.wiretap("1");
        start();
        samples.wiretap("2");
        await("captured write", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE).equals(List.of("module-wiretap-2")));

        synchroniser.requestReconcile();
        await("reconciled", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE).size() == 2);
    }

    @Test
    public void deletes_by_query_are_not_synchronised_until_a_reconcile() throws Exception {
        start();
        samples.jobInstance("job-1");
        samples.jobInstance("job-2");
        await("inserted", () -> solrIdsUnchecked(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE).size() == 2);

        env.bean("schedulerJobInstanceDao", SchedulerJobInstanceDao.class).deleteSchedulerJobInstances("context-instance");
        samples.wiretap("1"); // a later write, so we know the delete has been through the queue
        await("later write", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE).size() == 1);
        assertEquals("a delete by query is not seen by the listener", 2,
            solrIds(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE).size());

        synchroniser.requestReconcile();
        await("orphans removed", () -> solrIdsUnchecked(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE).isEmpty());
    }

    @Test
    public void a_full_queue_drops_changes_without_blocking_mongo_and_asks_for_a_reconcile() throws Exception {
        properties.setQueueCapacity(1);
        synchroniser = new CommandListenerSynchroniser(env.writeCaptureListener, pipeline, reconciler, stateStore,
            properties);
        // not started: nothing takes changes off the queue
        ChangeEvent event = ChangeEvent.delete(SyncCollections.IKASAN, "a");
        long start = System.currentTimeMillis();
        synchroniser.enqueue(List.of(event, event, event));
        assertTrue("enqueue must not block", System.currentTimeMillis() - start < 1000);
        assertEquals(1, synchroniser.queued());
        assertEquals(2, synchroniser.droppedCount());
        assertTrue(synchroniser.reconcilePending());
    }

    @Test
    public void the_listener_is_inactive_once_stopped() throws Exception {
        start();
        synchroniser.stop();
        samples.wiretap("1");
        Thread.sleep(500);
        assertTrue(solrIds(EntityDao.WIRETAP_TYPE).isEmpty());
    }
}
