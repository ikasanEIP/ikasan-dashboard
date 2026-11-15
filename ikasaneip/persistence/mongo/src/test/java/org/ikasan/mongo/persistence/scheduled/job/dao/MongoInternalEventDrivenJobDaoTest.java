package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoInternalEventDrivenJobRepository;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MongoInternalEventDrivenJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoInternalEventDrivenJobRepository repository;
    private MongoInternalEventDrivenJobDao dao;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        // Use Spring Data MongoDB repository factory
        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoInternalEventDrivenJobRepository.class);

        dao = new MongoInternalEventDrivenJobDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoInternalEventDrivenJobRecordImpl.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_find_success() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");

        dao.save(record);

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(record.getId(), found.getId());
        Assert.assertEquals("agent1", found.getAgentName());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertEquals("context1", found.getContextName());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setId("custom_id");

        dao.save(record);

        InternalEventDrivenJobRecord found = dao.findById("custom_id");
        Assert.assertNotNull(found);
        Assert.assertEquals("custom_id", found.getId());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        InternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");

        long beforeSave = System.currentTimeMillis();
        Thread.sleep(10);
        dao.save(record);

        record = dao.findById(record.getId());

        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<InternalEventDrivenJobRecord> records = new ArrayList<>();
        records.add(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));
        records.add(createInternalEventDrivenJobRecord("agent2", "job2", "context2"));

        dao.save(records);

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_all() {
        dao.save(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent2", "job2", "context2"));

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createInternalEventDrivenJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        results = dao.findAll(2, 2);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context() {
        dao.save(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent2", "job2", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent3", "job3", "context2"));

        SearchResults<InternalEventDrivenJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createInternalEventDrivenJobRecord("agent" + i, "job" + i, "context1"));
        }

        SearchResults<InternalEventDrivenJobRecord> results = dao.findByContext("context1", 2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_and_find_by_context_follow_paging_convention() {
        for (int i = 0; i < 25; i++) {
            dao.save(createInternalEventDrivenJobRecord("agent" + i, "job" + i, "context1"));
        }
        for (int i = 0; i < 4; i++) {
            dao.save(createInternalEventDrivenJobRecord("other" + i, "job" + i, "context2"));
        }

        PagingConvention.assertFollowed(29, (offset, limit) -> PagingConvention.page(dao.findAll(limit, offset)));
        PagingConvention.assertFollowed(25,
            (offset, limit) -> PagingConvention.page(dao.findByContext("context1", limit, offset)));
    }

    @Test
    public void test_find_all_limit_zero_returns_only_the_total() {
        for (int i = 0; i < 3; i++) {
            dao.save(createInternalEventDrivenJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(0, 0);

        Assert.assertEquals(3, results.getTotalNumberOfResults());
        Assert.assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void test_find_by_context_limit_zero_returns_only_the_total() {
        dao.save(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent2", "job2", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent3", "job3", "context2"));

        SearchResults<InternalEventDrivenJobRecord> results = dao.findByContext("context1", 0, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void test_find_all_limit_minus_one_returns_every_record() {
        for (int i = 0; i < 3; i++) {
            dao.save(createInternalEventDrivenJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(-1, -1);

        Assert.assertEquals(3, results.getTotalNumberOfResults());
        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_save_persists_disabled() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setDisabled(true);

        dao.save(record);

        Assert.assertTrue(dao.findById(record.getId()).isDisabled());
    }

    @Test
    public void test_save_clears_disabled() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setDisabled(true);
        dao.save(record);

        record.setDisabled(false);
        dao.save(record);

        Assert.assertFalse(dao.findById(record.getId()).isDisabled());
    }

    @Test
    public void test_save_defaults_flags_to_false() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");

        dao.save(record);

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertFalse(found.isDisabled());
        Assert.assertFalse(found.isHeld());
        Assert.assertFalse(found.isSkipped());
    }

    @Test
    public void test_save_persists_disabled_held_and_skipped_independently() {
        MongoInternalEventDrivenJobRecordImpl disabledOnly = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        disabledOnly.setDisabled(true);
        MongoInternalEventDrivenJobRecordImpl heldOnly = createInternalEventDrivenJobRecord("agent2", "job2", "context1");
        heldOnly.setHeld(true);
        MongoInternalEventDrivenJobRecordImpl skippedOnly = createInternalEventDrivenJobRecord("agent3", "job3", "context1");
        skippedOnly.setSkipped(true);
        MongoInternalEventDrivenJobRecordImpl all = createInternalEventDrivenJobRecord("agent4", "job4", "context1");
        all.setDisabled(true);
        all.setHeld(true);
        all.setSkipped(true);

        dao.save(List.of(disabledOnly, heldOnly, skippedOnly, all));

        assertFlags(disabledOnly.getId(), true, false, false);
        assertFlags(heldOnly.getId(), false, true, false);
        assertFlags(skippedOnly.getId(), false, false, true);
        assertFlags(all.getId(), true, true, true);
    }

    @Test
    public void test_save_without_id_persists_flags() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setId(null);
        record.setDisabled(true);
        record.setHeld(true);
        record.setSkipped(true);

        dao.save(record);

        // the DAO generates the id from the agent, job and context names
        assertFlags(JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_agent1_job1_context1", true, true, true);
    }

    @Test
    public void test_skip_hold_enable_and_release_keep_disabled() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        withChildContexts(record, "child1");
        record.setDisabled(true);
        dao.save(record);

        dao.skip(dao.findById(record.getId()), List.of("child1"), "testUser");
        assertFlags(record.getId(), true, false, true);

        dao.hold(dao.findById(record.getId()), List.of("child1"), "testUser");
        assertFlags(record.getId(), true, true, true);

        dao.enable(dao.findById(record.getId()), "testUser");
        assertFlags(record.getId(), true, true, false);

        dao.release(dao.findById(record.getId()), "testUser");
        assertFlags(record.getId(), true, false, false);
    }

    @Test
    public void test_hold_all_release_all_and_enable_all_keep_disabled() {
        MongoInternalEventDrivenJobRecordImpl disabled = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        withChildContexts(disabled, "child1");
        disabled.setDisabled(true);
        MongoInternalEventDrivenJobRecordImpl enabled = createInternalEventDrivenJobRecord("agent2", "job2", "context1");
        withChildContexts(enabled, "child2");
        dao.save(List.of(disabled, enabled));

        dao.holdAll(List.of(dao.findById(disabled.getId()), dao.findById(enabled.getId())), "testUser");
        assertFlags(disabled.getId(), true, true, false);
        assertFlags(enabled.getId(), false, true, false);

        dao.releaseAll(List.of(dao.findById(disabled.getId()), dao.findById(enabled.getId())), "testUser");
        assertFlags(disabled.getId(), true, false, false);
        assertFlags(enabled.getId(), false, false, false);

        dao.enableAll(List.of(dao.findById(disabled.getId()), dao.findById(enabled.getId())), "testUser");
        assertFlags(disabled.getId(), true, false, false);
        assertFlags(enabled.getId(), false, false, false);
    }

    @Test
    public void test_hold_all_clears_skipped() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        withChildContexts(record, "child1");
        record.setSkipped(true);
        dao.save(record);

        dao.holdAll(List.of(dao.findById(record.getId())), "testUser");

        assertFlags(record.getId(), false, true, false);
        Assert.assertEquals(Boolean.TRUE,
            dao.findById(record.getId()).getInternalEventDrivenJob().getHeldContexts().get("child1"));
    }

    @Test
    public void test_find_by_context_no_results() {
        dao.save(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));

        SearchResults<InternalEventDrivenJobRecord> results = dao.findByContext("nonexistent", 10, 0);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_modified_by_field() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setModifiedBy("testUser");

        dao.save(record);

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    @Test
    public void test_skip_contexts() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        InternalEventDrivenJob job = record.getInternalEventDrivenJob();
        job.setTargetResidingContextOnly(true);
        record.setInternalEventDrivenJob(job);
        dao.save(record);

        List<String> childContexts = Arrays.asList("childContext1", "childContext2");
        InternalEventDrivenJobRecord savedRecord = dao.findById(record.getId());
        dao.skip(savedRecord, childContexts, "testUser");

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertTrue(found.isSkipped());
        Assert.assertEquals("testUser", found.getModifiedBy());
        Assert.assertTrue(found.getInternalEventDrivenJob().getSkippedContexts().containsKey("childContext1"));
        Assert.assertTrue(found.getInternalEventDrivenJob().getSkippedContexts().containsKey("childContext2"));
    }

    @Test
    public void test_hold_contexts() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        InternalEventDrivenJob job = record.getInternalEventDrivenJob();
        job.setTargetResidingContextOnly(true);
        record.setInternalEventDrivenJob(job);
        dao.save(record);

        List<String> childContexts = Arrays.asList("childContext1", "childContext2");
        InternalEventDrivenJobRecord savedRecord = dao.findById(record.getId());
        dao.hold(savedRecord, childContexts, "testUser");

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertTrue(found.isHeld());
        Assert.assertEquals("testUser", found.getModifiedBy());
        Assert.assertTrue(found.getInternalEventDrivenJob().getHeldContexts().containsKey("childContext1"));
        Assert.assertTrue(found.getInternalEventDrivenJob().getHeldContexts().containsKey("childContext2"));
    }

    @Test
    public void test_enable() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setSkipped(true);
        dao.save(record);

        dao.enable(record, "testUser");

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertFalse(found.isSkipped());
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    @Test
    public void test_release() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setHeld(true);
        dao.save(record);

        dao.release(record, "testUser");

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertFalse(found.isHeld());
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    @Test
    public void test_release_all() {
        MongoInternalEventDrivenJobRecordImpl record1 = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        MongoInternalEventDrivenJobRecordImpl record2 = createInternalEventDrivenJobRecord("agent2", "job2", "context2");
        record1.setHeld(true);
        record2.setHeld(true);
        dao.save(record1);
        dao.save(record2);

        List<InternalEventDrivenJobRecord> records = Arrays.asList(record1, record2);
        dao.releaseAll(records, "testUser");

        InternalEventDrivenJobRecord found1 = dao.findById(record1.getId());
        InternalEventDrivenJobRecord found2 = dao.findById(record2.getId());
        Assert.assertFalse(found1.isHeld());
        Assert.assertFalse(found2.isHeld());
        Assert.assertEquals("testUser", found1.getModifiedBy());
        Assert.assertEquals("testUser", found2.getModifiedBy());
    }

    @Test
    public void test_hold_all() {
        MongoInternalEventDrivenJobRecordImpl record1 = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        MongoInternalEventDrivenJobRecordImpl record2 = createInternalEventDrivenJobRecord("agent2", "job2", "context2");
        withChildContexts(record1, "child1", "child2");
        withChildContexts(record2, "child3", "child4");
        dao.save(record1);
        dao.save(record2);

        List<InternalEventDrivenJobRecord> records = Arrays.asList(record1, record2);
        dao.holdAll(records, "testUser");

        InternalEventDrivenJobRecord found1 = dao.findById(record1.getId());
        InternalEventDrivenJobRecord found2 = dao.findById(record2.getId());
        Assert.assertTrue(found1.isHeld());
        Assert.assertTrue(found2.isHeld());
        Assert.assertEquals(Set.of("child1", "child2"), found1.getInternalEventDrivenJob().getHeldContexts().keySet());
        Assert.assertEquals(Set.of("child3", "child4"), found2.getInternalEventDrivenJob().getHeldContexts().keySet());
        Assert.assertEquals("testUser", found1.getModifiedBy());
        Assert.assertEquals("testUser", found2.getModifiedBy());
    }

    @Test
    public void test_enable_all() {
        MongoInternalEventDrivenJobRecordImpl record1 = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        MongoInternalEventDrivenJobRecordImpl record2 = createInternalEventDrivenJobRecord("agent2", "job2", "context2");
        record1.setSkipped(true);
        record2.setSkipped(true);
        dao.save(record1);
        dao.save(record2);

        List<InternalEventDrivenJobRecord> records = Arrays.asList(record1, record2);
        dao.enableAll(records, "testUser");

        InternalEventDrivenJobRecord found1 = dao.findById(record1.getId());
        InternalEventDrivenJobRecord found2 = dao.findById(record2.getId());
        Assert.assertFalse(found1.isSkipped());
        Assert.assertFalse(found2.isSkipped());
        Assert.assertEquals("testUser", found1.getModifiedBy());
        Assert.assertEquals("testUser", found2.getModifiedBy());
    }

    // Helper methods

    /** The job is stored as JSON, so {@code getInternalEventDrivenJob()} returns a copy: set it back. */
    private static void withChildContexts(MongoInternalEventDrivenJobRecordImpl record, String... names) {
        InternalEventDrivenJob job = record.getInternalEventDrivenJob();
        job.setChildContextNames(List.of(names));
        record.setInternalEventDrivenJob(job);
    }

    private void assertFlags(String id, boolean disabled, boolean held, boolean skipped) {
        InternalEventDrivenJobRecord found = dao.findById(id);
        Assert.assertNotNull("no record with id " + id, found);
        Assert.assertEquals(id + ": disabled", disabled, found.isDisabled());
        Assert.assertEquals(id + ": held", held, found.isHeld());
        Assert.assertEquals(id + ": skipped", skipped, found.isSkipped());
    }

    private MongoInternalEventDrivenJobRecordImpl createInternalEventDrivenJobRecord(String agentName, String jobName, String contextName) {
        InternalEventDrivenJobImpl internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName(agentName);
        internalEventDrivenJob.setJobName(jobName);
        internalEventDrivenJob.setContextName(contextName);
        internalEventDrivenJob.setDisplayName("Display " + jobName);
        internalEventDrivenJob.setTargetResidingContextOnly(false);
        internalEventDrivenJob.setParticipatesInLock(false);

        MongoInternalEventDrivenJobRecordImpl record = new MongoInternalEventDrivenJobRecordImpl();
        record.setId(JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + internalEventDrivenJob.getAgentName() + "_"
            + internalEventDrivenJob.getJobName() + "_" + internalEventDrivenJob.getContextName());
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName("Display " + jobName);
        record.setInternalEventDrivenJob(internalEventDrivenJob);
        record.setTimestamp(System.currentTimeMillis());
        record.setTargetResidingContextOnly(false);
        record.setParticipatesInLock(false);

        return record;
    }
}
