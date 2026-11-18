package org.ikasan.data.synchronisation;

import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.ikasan.mongo.persistence.general.dao.MongoGeneralDaoImpl;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChangeStreamSynchroniserTest extends SyncTestSupport {

    private ChangeStreamSynchroniser synchroniser;

    private ChangeStreamSynchroniser newSynchroniser() {
        return new ChangeStreamSynchroniser(mongoTemplate, pipeline, reconciler, stateStore, properties);
    }

    @After
    public void stopSynchroniser() {
        if (synchroniser != null) {
            synchroniser.stop();
        }
    }

    @Test
    public void first_start_loads_existing_documents_then_follows_new_writes() throws Exception {
        samples.wiretap("1");
        samples.policy("policy-1");

        synchroniser = newSynchroniser();
        synchroniser.start();

        await("existing documents loaded", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE).size() == 1
            && solrIdsUnchecked("securityPolicy").size() == 1);
        await("full load recorded", () -> stateStore.lastFullLoad().isPresent());

        samples.wiretap("2");
        await("new insert synchronised", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE).size() == 2);
    }

    @Test
    public void inserts_replacements_and_deletes_reach_solr() throws Exception {
        synchroniser = newSynchroniser();
        synchroniser.start();
        await("stream following", () -> stateStore.resumeToken().isPresent());

        samples.jobLockCache("env-1");
        await("insert", () -> solrIdsUnchecked("jobLockCache").size() == 1);

        // replace: the lock cache is saved again with new data
        samples.jobLockCache("env-1");
        await("replace keeps one document", () -> solrIdsUnchecked("jobLockCache").size() == 1);

        mongoTemplate.getCollection(SyncCollections.IKASAN).deleteOne(
            new org.bson.Document("_id", "jobLockCacheIdentifier__env-1"));
        await("delete", () -> solrIdsUnchecked("jobLockCache").isEmpty());
    }

    @Test
    public void deletes_by_query_reach_solr() throws Exception {
        samples.jobInstance("job-1");
        samples.jobInstance("job-2");
        samples.wiretap("1");
        synchroniser = newSynchroniser();
        synchroniser.start();
        await("loaded", () -> solrIdsUnchecked(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE).size() == 2);

        // deletes by context instance id: a query, not by _id
        env.bean("schedulerJobInstanceDao", SchedulerJobInstanceDao.class).deleteSchedulerJobInstances("context-instance");
        await("deleted by query", () -> solrIdsUnchecked(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE).isEmpty());

        // housekeeping across all types deletes expired documents by query
        mongoTemplate.getCollection(SyncCollections.IKASAN).updateMany(new org.bson.Document("type", "wiretap"),
            new org.bson.Document("$set", new org.bson.Document("expiry", 1L)));
        env.context.getBean(MongoGeneralDaoImpl.class).removeExpired();
        await("housekeeping delete", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE).isEmpty());
    }

    @Test
    public void resumes_after_a_restart_without_missing_changes() throws Exception {
        synchroniser = newSynchroniser();
        synchroniser.start();
        samples.wiretap("1");
        await("first write", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE).size() == 1);
        synchroniser.stop();

        // written while the synchronisation is stopped
        samples.wiretap("2");
        samples.wiretap("3");
        mongoTemplate.getCollection(SyncCollections.IKASAN).deleteOne(new org.bson.Document("_id", "module-wiretap-1"));

        synchroniser = newSynchroniser();
        synchroniser.start();
        await("caught up", () -> solrIdsUnchecked(EntityDao.WIRETAP_TYPE)
            .equals(List.of("module-wiretap-2", "module-wiretap-3")));
    }

    @Test
    public void lost_history_and_fatal_errors_mean_the_stream_cannot_resume() {
        // Mongo can't be made to lose change stream history in a test, so the classification is tested directly
        assertTrue(ChangeStreamSynchroniser.cannotResume(commandError(ChangeStreamSynchroniser.HISTORY_LOST)));
        assertTrue(ChangeStreamSynchroniser.cannotResume(commandError(ChangeStreamSynchroniser.FATAL_ERROR)));
        assertFalse(ChangeStreamSynchroniser.cannotResume(commandError(11600))); // InterruptedAtShutdown
    }

    private static MongoCommandException commandError(int code) {
        return new MongoCommandException(new BsonDocument("ok", new BsonInt32(0))
            .append("code", new BsonInt32(code))
            .append("errmsg", new BsonString("error " + code)), new ServerAddress());
    }
}
