package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceRepository;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao.SCHEDULED_CONTEXT_INSTANCE_TYPE;

/**
 * MongoDB DAO Test for ScheduledContextInstance operations.
 *
 * @author Ikasan Development Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoScheduledContextInstanceDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoScheduledContextInstanceRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoScheduledContextInstanceDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
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
    public void test_save_and_findById() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById("id1" + "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);

        Assert.assertNotNull(result);
        Assert.assertEquals("context1", result.getContextName());
        Assert.assertEquals(InstanceStatus.RUNNING.name(), result.getStatus());
    }

    @Test
    public void test_findById_returns_null_when_not_found() {
        ScheduledContextInstanceRecord result = dao.findById("nonexistent");

        Assert.assertNull(result);
    }

    @Test
    public void test_deleteById() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        dao.save(record);

        ScheduledContextInstanceRecord found = dao.findById("id1" + "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);
        Assert.assertNotNull(found);

        dao.deleteById("id1" + "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);

        ScheduledContextInstanceRecord notFound = dao.findById("id1" + "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);
        Assert.assertNull(notFound);
    }

    @Test
    public void test_getScheduledContextInstancesByStatus() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.ERROR));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
                List.of(InstanceStatus.COMPLETE)
        );

        Assert.assertEquals(2, results.getResultList().size());
        Assert.assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void test_getScheduledContextInstancesByStatus_with_limit_and_offset() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
                List.of(InstanceStatus.COMPLETE), 2, 0
        );

        Assert.assertEquals(2, results.getResultList().size());
        Assert.assertEquals(3, results.getTotalNumberOfResults());
    }

    @Test
    public void test_getScheduledContextInstancesByStatus_with_multiple_statuses() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.ERROR));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.RUNNING));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
                List.of(InstanceStatus.COMPLETE, InstanceStatus.ERROR)
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "context2", InstanceStatus.RUNNING));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
                "context1", 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName_with_sorting_asc() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
                "context1", 10, 0, "timestamp", "asc"
        );

        Assert.assertEquals(2, results.getResultList().size());
        // Verify ascending order
        Assert.assertTrue(results.getResultList().get(0).getTimestamp() <=
                results.getResultList().get(1).getTimestamp());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName_with_sorting_desc() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
                "context1", 10, 0, "timestamp", "desc"
        );

        Assert.assertEquals(2, results.getResultList().size());
        // Verify descending order
        Assert.assertTrue(results.getResultList().get(0).getTimestamp() >=
                results.getResultList().get(1).getTimestamp());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName_with_timestamp_range() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record1 = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record1.setTimestamp(now);
        dao.save(record1);

        ScheduledContextInstanceRecord record2 = createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE);
        record2.setTimestamp(now - 2 * 24 * 60 * 60 * 1000); // 2 days ago
        dao.save(record2);

        ScheduledContextInstanceRecord record3 = createContextInstanceRecord("id3", "context1", InstanceStatus.RUNNING);
        record3.setTimestamp(now + 24 * 60 * 60 * 1000); // 1 day in future
        dao.save(record3);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
                "context1",
                now - 1000,
                now + 1000,
                10,
                0,
                null,
                null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_context_instance_names() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.RUNNING));

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setContextInstanceNames(List.of("context1", "context2"));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_context_search_filter() {
        dao.save(createContextInstanceRecord("id1", "myContext1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("id2", "myContext2", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "otherContext", InstanceStatus.RUNNING));

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setContextSearchFilter("myContext");

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_context_instance_id() {
        dao.save(createContextInstanceRecord("testId123", "context1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setContextInstanceId("testId123");

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
        Assert.assertEquals("testId123", results.getResultList().get(0).getContextInstance().getId());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_status() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.ERROR));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.COMPLETE));

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setStatus(InstanceStatus.COMPLETE.name());

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_created_timestamp() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record1 = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record1.setTimestamp(now);
        dao.save(record1);

        ScheduledContextInstanceRecord record2 = createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE);
        record2.setTimestamp(now - 2 * 24 * 60 * 60 * 1000); // 2 days ago
        dao.save(record2);

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setCreatedTimestamp(now);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_modified_timestamp() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record.setTimestamp(now);
        record.setModifiedTimestamp(now);
        dao.save(record);

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setModifiedTimestamp(now);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_start_time_range() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record.getContextInstance().setStartTime(now);
        dao.save(record);

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setStartTimeStart(now - 1000);
        filter.setStartTimeEnd(now + 1000);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_end_time_range() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE);
        ContextInstance contextInstance = record.getContextInstance();
        contextInstance.setEndTime(now);
        record.setContextInstance(contextInstance);
        dao.save(record);

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setEndTimeStart(now - 1000);
        filter.setEndTimeEnd(now + 1000);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_default_sorting() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
        // Default is descending by timestamp, so most recent first
        Assert.assertTrue(results.getResultList().get(0).getTimestamp() >=
                results.getResultList().get(1).getTimestamp());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_custom_sorting() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, "timestamp", "ASCENDING"
        );

        Assert.assertEquals(2, results.getResultList().size());
        // Ascending order
        Assert.assertTrue(results.getResultList().get(0).getTimestamp() <=
                results.getResultList().get(1).getTimestamp());
    }

    @Test
    public void test_save_with_modifiedBy() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record.setModifiedBy("testUser");
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById("id1" + "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);
        Assert.assertNotNull(result);
        Assert.assertEquals("testUser", result.getModifiedBy());
    }

    @Test
    public void test_save_with_contains_repeating_jobs() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        ContextInstance contextInstance = record.getContextInstance();
        contextInstance.setContainsRepeatingJobs(true);
        record.setContextInstance(contextInstance);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById("id1"+ "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getContextInstance().isContainsRepeatingJobs());
    }

    @Test
    public void test_atStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 14, 30, 45);
        Date testDate = cal.getTime();

        long result = dao.atStartOfDay(testDate);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(result);

        Assert.assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, resultCal.get(Calendar.MINUTE));
        Assert.assertEquals(0, resultCal.get(Calendar.SECOND));
        Assert.assertEquals(0, resultCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void test_atEndOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 10, 20, 30);
        Date testDate = cal.getTime();

        long result = dao.atEndOfDay(testDate);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(result);

        Assert.assertEquals(23, resultCal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(59, resultCal.get(Calendar.MINUTE));
        Assert.assertEquals(59, resultCal.get(Calendar.SECOND));
        Assert.assertEquals(999, resultCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_multiple_filters() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record1 = createContextInstanceRecord("id1", "myContext1", InstanceStatus.COMPLETE);
        record1.setTimestamp(now);
        record1.getContextInstance().setStartTime(now);
        dao.save(record1);

        ScheduledContextInstanceRecord record2 = createContextInstanceRecord("id2", "otherContext", InstanceStatus.ERROR);
        record2.setTimestamp(now);
        dao.save(record2);

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();
        filter.setContextSearchFilter("myContext");
        filter.setStatus(InstanceStatus.COMPLETE.name());
        filter.setStartTimeStart(now - 1000);
        filter.setStartTimeEnd(now + 1000);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
        Assert.assertEquals("id1", results.getResultList().get(0).getContextInstance().getId());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_pagination() {
        for (int i = 0; i < 25; i++) {
            dao.save(createContextInstanceRecord("id" + i, "context" + i, InstanceStatus.RUNNING));
        }

        ContextInstanceSearchFilter filter = new TestContextInstanceSearchFilter();

        // Get first page
        SearchResults<ScheduledContextInstanceRecord> page1 = dao.getScheduledContextInstancesByFilter(
                filter, 10, 0, null, null
        );

        Assert.assertEquals(10, page1.getResultList().size());
        Assert.assertEquals(25, page1.getTotalNumberOfResults());

        // Get second page
        SearchResults<ScheduledContextInstanceRecord> page2 = dao.getScheduledContextInstancesByFilter(
                filter, 10, 10, null, null
        );

        Assert.assertEquals(10, page2.getResultList().size());
        Assert.assertEquals(25, page2.getTotalNumberOfResults());
    }

    @Test
    public void test_query_response_time_is_tracked() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
                List.of(InstanceStatus.RUNNING)
        );

        Assert.assertNotNull(results);
        Assert.assertTrue(results.getQueryResponseTime() >= 0);
    }

    // Helper methods
    private ScheduledContextInstanceRecord createContextInstanceRecord(String id, String contextName, InstanceStatus status) {
        MongoScheduledContextInstanceRecordImpl record = new MongoScheduledContextInstanceRecordImpl();
        record.setId(id);
        record.setContextName(contextName);
        record.setContextInstanceId(id);
        record.setStatus(status.name());
        record.setTimestamp(System.currentTimeMillis());
        record.setModifiedTimestamp(System.currentTimeMillis());

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setId(id);
        contextInstance.setName(contextName);
        contextInstance.setStartTime(System.currentTimeMillis());
        contextInstance.setEndTime(System.currentTimeMillis() + 1000);
        contextInstance.setContainsRepeatingJobs(false);

        record.setContextInstance(contextInstance);

        return record;
    }

    /**
     * Test implementation of ContextInstanceSearchFilter
     */
    private static class TestContextInstanceSearchFilter implements ContextInstanceSearchFilter {
        private List<String> contextInstanceNames;
        private String contextSearchFilter;
        private String contextInstanceId;
        private long createdTimestamp;
        private long modifiedTimestamp;
        private long startTime;
        private long endTime;
        private long startTimeStart;
        private long startTimeEnd;
        private long endTimeStart;
        private long endTimeEnd;
        private String status;

        @Override
        public List<String> getContextInstanceNames() {
            return contextInstanceNames;
        }

        @Override
        public void setContextInstanceNames(List<String> contextInstanceNames) {
            this.contextInstanceNames = contextInstanceNames;
        }

        @Override
        public String getContextSearchFilter() {
            return contextSearchFilter;
        }

        @Override
        public void setContextSearchFilter(String contextSearchFilter) {
            this.contextSearchFilter = contextSearchFilter;
        }

        @Override
        public String getContextInstanceId() {
            return contextInstanceId;
        }

        @Override
        public void setContextInstanceId(String contextInstanceId) {
            this.contextInstanceId = contextInstanceId;
        }

        @Override
        public long getCreatedTimestamp() {
            return createdTimestamp;
        }

        @Override
        public void setCreatedTimestamp(long createdTimestamp) {
            this.createdTimestamp = createdTimestamp;
        }

        @Override
        public long getModifiedTimestamp() {
            return modifiedTimestamp;
        }

        @Override
        public void setModifiedTimestamp(long modifiedTimestamp) {
            this.modifiedTimestamp = modifiedTimestamp;
        }

        @Override
        public long getStartTime() {
            return startTime;
        }

        @Override
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        @Override
        public long getEndTime() {
            return endTime;
        }

        @Override
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        @Override
        public long getStartTimeStart() {
            return startTimeStart;
        }

        @Override
        public void setStartTimeStart(long startTimeStart) {
            this.startTimeStart = startTimeStart;
        }

        @Override
        public long getStartTimeEnd() {
            return startTimeEnd;
        }

        @Override
        public void setStartTimeEnd(long startTimeEnd) {
            this.startTimeEnd = startTimeEnd;
        }

        @Override
        public long getEndTimeStart() {
            return endTimeStart;
        }

        @Override
        public void setEndTimeStart(long endTimeStart) {
            this.endTimeStart = endTimeStart;
        }

        @Override
        public long getEndTimeEnd() {
            return endTimeEnd;
        }

        @Override
        public void setEndTimeEnd(long endTimeEnd) {
            this.endTimeEnd = endTimeEnd;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public void setStatus(String status) {
            this.status = status;
        }
    }
}
