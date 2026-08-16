package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceAuditAggregateRepository;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB DAO Test for ScheduledContextInstanceAuditAggregate operations.
 *
 * @author Ikasan Development Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoScheduledContextInstanceAuditAggregateDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoScheduledContextInstanceAuditAggregateRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoScheduledContextInstanceAuditAggregateDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoScheduledContextInstanceAuditAggregateRepository repository,
                       MongoTemplate mongoTemplate) {
        this.dao = new MongoScheduledContextInstanceAuditAggregateDao(repository, mongoTemplate);
    }

    @After
    public void teardown() {
        repository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_get_repeating_job_status_count_success() {
        List<ScheduledContextInstanceAuditAggregateRecord> auditAggregateRecords
                = this.createScheduledContextInstanceAuditAggregateRecords("id1", "contextName",
                20, InstanceStatus.COMPLETE);
        auditAggregateRecords.forEach(record -> dao.save(record));

        auditAggregateRecords
                = this.createScheduledContextInstanceAuditAggregateRecords("id1", "contextName",
                3, InstanceStatus.ERROR);
        auditAggregateRecords.forEach(record -> dao.save(record));

        auditAggregateRecords
                = this.createScheduledContextInstanceAuditAggregateRecords("id2", "contextName",
                27, InstanceStatus.COMPLETE);
        auditAggregateRecords.forEach(record -> dao.save(record));

        auditAggregateRecords
                = this.createScheduledContextInstanceAuditAggregateRecords("id2", "contextName",
                5, InstanceStatus.ERROR);
        auditAggregateRecords.forEach(record -> dao.save(record));

        Map<String, Map<String, Integer>> statusCounts = this.dao
                .getRepeatingJobStatusCounts(List.of("id1", "id2"));

        Assert.assertTrue(!statusCounts.isEmpty());
        Assert.assertEquals(20, statusCounts.get("id1").get("COMPLETE").intValue());
        Assert.assertEquals(3, statusCounts.get("id1").get("ERROR").intValue());
        Assert.assertEquals(27, statusCounts.get("id2").get("COMPLETE").intValue());
        Assert.assertEquals(5, statusCounts.get("id2").get("ERROR").intValue());
    }

    @Test
    public void test_findAll() {
        List<ScheduledContextInstanceAuditAggregateRecord> records = createScheduledContextInstanceAuditAggregateRecords(
                "id1", "contextName", 5, InstanceStatus.COMPLETE
        );
        records.forEach(record -> dao.save(record));

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findAll(10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(5, results.getResultList().size());
    }

    @Test
    public void test_findAll_with_limit() {
        List<ScheduledContextInstanceAuditAggregateRecord> records = createScheduledContextInstanceAuditAggregateRecords(
                "id1", "contextName", 10, InstanceStatus.COMPLETE
        );
        records.forEach(record -> dao.save(record));

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findAll(5, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(5, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_context_name() {
        createScheduledContextInstanceAuditAggregateRecords("id1", "myContext", 3, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));
        createScheduledContextInstanceAuditAggregateRecords("id2", "otherContext", 2, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextName("myContext");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                        filter, 10, 0, null, null
                );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_context_instance_id() {
        createScheduledContextInstanceAuditAggregateRecords("instanceId123", "context1", 4, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));
        createScheduledContextInstanceAuditAggregateRecords("instanceId456", "context2", 2, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextInstanceId("instanceId123");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                        filter, 10, 0, null, null
                );

        Assert.assertEquals(4, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_status() {
        createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));
        createScheduledContextInstanceAuditAggregateRecords("id2", "context2", 2, InstanceStatus.ERROR)
                .forEach(record -> dao.save(record));

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setStatus(InstanceStatus.ERROR.name());

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                        filter, 10, 0, null, null
                );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_scheduled_process_event_name() {
        List<ScheduledContextInstanceAuditAggregateRecord> records1 =
                createScheduledContextInstanceAuditAggregateRecordsWithEvent("id1", "context1", 3, InstanceStatus.COMPLETE, "event1");
        records1.forEach(record -> dao.save(record));

        List<ScheduledContextInstanceAuditAggregateRecord> records2 =
                createScheduledContextInstanceAuditAggregateRecordsWithEvent("id2", "context2", 2, InstanceStatus.COMPLETE, "event2");
        records2.forEach(record -> dao.save(record));

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setScheduledProcessEventName("event1");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                        filter, 10, 0, null, null
                );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_sorting_ascending() {
        createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                        filter, 10, 0, "timestamp", "ASCENDING"
                );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_sorting_descending() {
        createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                        filter, 10, 0, "timestamp", "DESC"
                );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_getRepeatingJobStatusCounts_with_multiple_context_instances() {
        createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 10, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));
        createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 5, InstanceStatus.ERROR)
                .forEach(record -> dao.save(record));
        createScheduledContextInstanceAuditAggregateRecords("id2", "context2", 15, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));
        createScheduledContextInstanceAuditAggregateRecords("id2", "context2", 3, InstanceStatus.RUNNING)
                .forEach(record -> dao.save(record));

        Map<String, Map<String, Integer>> statusCounts =
                this.dao.getRepeatingJobStatusCounts(List.of("id1", "id2"));

        Assert.assertEquals(2, statusCounts.size());
        Assert.assertEquals(10, statusCounts.get("id1").get("COMPLETE").intValue());
        Assert.assertEquals(5, statusCounts.get("id1").get("ERROR").intValue());
        Assert.assertEquals(15, statusCounts.get("id2").get("COMPLETE").intValue());
        Assert.assertEquals(3, statusCounts.get("id2").get("RUNNING").intValue());
    }

    @Test
    public void test_getRepeatingJobStatusCounts_with_empty_list() {
        Map<String, Map<String, Integer>> statusCounts =
                this.dao.getRepeatingJobStatusCounts(List.of());

        Assert.assertTrue(statusCounts.isEmpty());
    }

    @Test
    public void test_getRepeatingJobStatusCounts_with_nonexistent_context_instance() {
        createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 5, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));

        Map<String, Map<String, Integer>> statusCounts =
                this.dao.getRepeatingJobStatusCounts(List.of("nonexistent"));

        Assert.assertTrue(statusCounts.get("nonexistent").isEmpty());
    }

    @Test
    public void test_save_non_repeating_jobs_excluded_from_repeating_counts() {
        List<ScheduledContextInstanceAuditAggregateRecord> repeatingRecords =
                createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 5, InstanceStatus.COMPLETE);
        repeatingRecords.forEach(record -> dao.save(record));

        List<ScheduledContextInstanceAuditAggregateRecord> nonRepeatingRecords =
                createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE);
        nonRepeatingRecords.forEach(r -> {
            r.setRepeatingJob(false);
            dao.save(r);
        });

        Map<String, Map<String, Integer>> statusCounts =
                this.dao.getRepeatingJobStatusCounts(List.of("id1"));

        Assert.assertNotNull(statusCounts.get("id1"));
        Assert.assertEquals(5, statusCounts.get("id1").get("COMPLETE").intValue());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_multiple_filters() {
        List<ScheduledContextInstanceAuditAggregateRecord> records =
                createScheduledContextInstanceAuditAggregateRecordsWithEvent("id1", "myContext", 3, InstanceStatus.COMPLETE, "myEvent");
        records.forEach(record -> dao.save(record));

        createScheduledContextInstanceAuditAggregateRecordsWithEvent("id2", "otherContext", 2, InstanceStatus.ERROR, "otherEvent")
                .forEach(record -> dao.save(record));

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextName("myContext");
        filter.setContextInstanceId("id1");
        filter.setStatus(InstanceStatus.COMPLETE.name());
        filter.setScheduledProcessEventName("myEvent");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
                this.dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                        filter, 10, 0, null, null
                );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_pagination() {
        for (int i = 0; i < 25; i++) {
            createScheduledContextInstanceAuditAggregateRecords("id" + i, "context", 1, InstanceStatus.COMPLETE)
                    .forEach(record -> dao.save(record));
        }

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();

        // Get first page
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> page1 =
                dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, 10, 0, null, null);

        Assert.assertEquals(10, page1.getResultList().size());
        Assert.assertEquals(25, page1.getTotalNumberOfResults());

        // Get second page
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> page2 =
                dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, 10, 10, null, null);

        Assert.assertEquals(10, page2.getResultList().size());
        Assert.assertEquals(25, page2.getTotalNumberOfResults());
    }

    @Test
    public void test_query_response_time_is_tracked() {
        createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 5, InstanceStatus.COMPLETE)
                .forEach(record -> dao.save(record));

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results = dao.findAll(10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertTrue(results.getQueryResponseTime() >= 0);
    }

    // Helper methods
    private List<ScheduledContextInstanceAuditAggregateRecord> createScheduledContextInstanceAuditAggregateRecords(
            String contextInstanceId, String contextName, int numberToCreate, InstanceStatus instanceStatus) {
        List<ScheduledContextInstanceAuditAggregateRecord> records = new ArrayList<>();

        for (int i = 0; i < numberToCreate; i++) {
            MongoScheduledContextInstanceAuditAggregateRecordImpl scheduledContextInstanceAuditAggregateRecord
                    = new MongoScheduledContextInstanceAuditAggregateRecordImpl();
            scheduledContextInstanceAuditAggregateRecord.setRepeatingJob(true);
            scheduledContextInstanceAuditAggregateRecord.setJobType("");
            scheduledContextInstanceAuditAggregateRecord.setContextInstanceId(contextInstanceId);
            scheduledContextInstanceAuditAggregateRecord.setStatus(instanceStatus.name());
            scheduledContextInstanceAuditAggregateRecord.setContextName(contextName);
            scheduledContextInstanceAuditAggregateRecord
                    .setScheduledContextInstanceAuditAggregate(new ScheduledContextInstanceAuditAggregateImpl());
            scheduledContextInstanceAuditAggregateRecord.setScheduledProcessEventName("eventName");

            records.add(scheduledContextInstanceAuditAggregateRecord);
        }

        return records;
    }

    private List<ScheduledContextInstanceAuditAggregateRecord> createScheduledContextInstanceAuditAggregateRecordsWithEvent(
            String contextInstanceId, String contextName, int numberToCreate, InstanceStatus instanceStatus, String eventName) {
        List<ScheduledContextInstanceAuditAggregateRecord> records = new ArrayList<>();

        for (int i = 0; i < numberToCreate; i++) {
            MongoScheduledContextInstanceAuditAggregateRecordImpl scheduledContextInstanceAuditAggregateRecord
                    = new MongoScheduledContextInstanceAuditAggregateRecordImpl();
            scheduledContextInstanceAuditAggregateRecord.setRepeatingJob(true);
            scheduledContextInstanceAuditAggregateRecord.setJobType("");
            scheduledContextInstanceAuditAggregateRecord.setContextInstanceId(contextInstanceId);
            scheduledContextInstanceAuditAggregateRecord.setStatus(instanceStatus.name());
            scheduledContextInstanceAuditAggregateRecord.setContextName(contextName);
            scheduledContextInstanceAuditAggregateRecord
                    .setScheduledContextInstanceAuditAggregate(new ScheduledContextInstanceAuditAggregateImpl());
            scheduledContextInstanceAuditAggregateRecord.setScheduledProcessEventName(eventName);

            records.add(scheduledContextInstanceAuditAggregateRecord);
        }

        return records;
    }
}
