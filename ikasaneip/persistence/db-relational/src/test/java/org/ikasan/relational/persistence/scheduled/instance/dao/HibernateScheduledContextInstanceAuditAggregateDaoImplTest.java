package org.ikasan.relational.persistence.scheduled.instance.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregate;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateScheduledContextInstanceAuditAggregateDaoImpl using
 * Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateScheduledContextInstanceAuditAggregateDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    @BeforeClass
    public static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();

    }

    @Autowired
    private ScheduledContextInstanceAuditAggregateDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        // Clean up all test data by finding all records and deleting them via native query
        // Since there's no deleteById method, we'll rely on test isolation
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private HibernateScheduledContextInstanceAuditAggregateRecord createAuditAggregateRecord(
            String id, String contextName, String status) {
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            new HibernateScheduledContextInstanceAuditAggregateRecord(id);
        record.setContextName(contextName);
        record.setContextInstanceId("instance-" + id);
        record.setScheduledProcessEventName("event-" + id);
        record.setRaisedEvents("event1,event2,event3");
        record.setStatus(status);
        record.setRepeatingJob(false);
        record.setJobType("STANDARD");
        record.setTimestamp(System.currentTimeMillis());

        // Create a mock audit aggregate
        ScheduledContextInstanceAuditAggregate mockAggregate = new MockScheduledContextInstanceAuditAggregate();
        record.setScheduledContextInstanceAuditAggregate(mockAggregate);

        return record;
    }

    @Test
    public void testSaveAndFindAll() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record1 =
            createAuditAggregateRecord("test-1", "context-1", "COMPLETE");
        HibernateScheduledContextInstanceAuditAggregateRecord record2 =
            createAuditAggregateRecord("test-2", "context-2", "RUNNING");

        // When
        dao.save(record1);
        dao.save(record2);

        // Then
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findAll(-1, -1, null, null);

        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 2);
        assertTrue(results.getResultList().size() >= 2);
    }

    @Test
    public void testSaveWithTimestamp() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("test-timestamp", "context-ts", "COMPLETE");

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given - create multiple records
        for (int i = 1; i <= 5; i++) {
            HibernateScheduledContextInstanceAuditAggregateRecord record =
                createAuditAggregateRecord("limit-test-" + i, "context-" + i, "COMPLETE");
            dao.save(record);
        }

        // When
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findAll(2, 1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(2, results.getResultList().size());
        assertTrue(results.getTotalNumberOfResults() >= 5);
    }

    @Test
    public void testFindAllWithSortAscending() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record1 =
            createAuditAggregateRecord("sort-1", "zebra-context", "COMPLETE");
        dao.save(record1);

        HibernateScheduledContextInstanceAuditAggregateRecord record2 =
            createAuditAggregateRecord("sort-2", "alpha-context", "COMPLETE");
        dao.save(record2);

        // When
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findAll(-1, -1, "contextName", "ASCENDING");

        // Then
        assertNotNull(results);
        assertTrue(results.getResultList().size() >= 2);
        // Find our records in the results
        List<ScheduledContextInstanceAuditAggregateRecord> ourRecords = results.getResultList().stream()
            .filter(r -> r.getId().startsWith("sort-"))
            .toList();
        assertEquals(2, ourRecords.size());
        assertEquals("alpha-context", ourRecords.get(0).getContextName());
        assertEquals("zebra-context", ourRecords.get(1).getContextName());
    }

    @Test
    public void testFindAllWithSortDescending() throws InterruptedException {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record1 =
            createAuditAggregateRecord("desc-1", "context-desc-1", "COMPLETE");
        dao.save(record1);

        Thread.sleep(10);

        HibernateScheduledContextInstanceAuditAggregateRecord record2 =
            createAuditAggregateRecord("desc-2", "context-desc-2", "COMPLETE");
        dao.save(record2);

        // When
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findAll(-1, -1, "timestamp", "DESCENDING");

        // Then
        assertNotNull(results);
        assertTrue(results.getResultList().size() >= 2);
        // Most recent should be first
        List<ScheduledContextInstanceAuditAggregateRecord> ourRecords = results.getResultList().stream()
            .filter(r -> r.getId().startsWith("desc-"))
            .toList();
        assertEquals(2, ourRecords.size());
        assertTrue(ourRecords.get(0).getTimestamp() >= ourRecords.get(1).getTimestamp());
    }

    @Test
    public void testFindByFilterContextName() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("filter-1", "unique-filter-context", "COMPLETE");
        dao.save(record);

        // When
        ScheduledContextInstanceAuditAggregateSearchFilter filter =
            new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextName("unique-filter-context");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("unique-filter-context", results.getResultList().get(0).getContextName());
    }

    @Test
    public void testFindByFilterContextInstanceId() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("filter-2", "context-2", "COMPLETE");
        dao.save(record);

        // When
        ScheduledContextInstanceAuditAggregateSearchFilter filter =
            new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextInstanceId("instance-filter-2");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("instance-filter-2", results.getResultList().get(0).getContextInstanceId());
    }

    @Test
    public void testFindByFilterScheduledProcessEventName() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("filter-3", "context-3", "COMPLETE");
        dao.save(record);

        // When
        ScheduledContextInstanceAuditAggregateSearchFilter filter =
            new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setScheduledProcessEventName("event-filter-3");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("event-filter-3", results.getResultList().get(0).getScheduledProcessEventName());
    }

    @Test
    public void testFindByFilterRaisedInitiationEventName() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("filter-4", "context-4", "COMPLETE");
        record.setRaisedEvents("initiation-event-xyz,other-event");
        dao.save(record);

        // When
        ScheduledContextInstanceAuditAggregateSearchFilter filter =
            new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setRaisedInitiationEventName("initiation-event-xyz");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().get(0).getRaisedEvents().contains("initiation-event-xyz"));
    }

    @Test
    public void testFindByFilterStatus() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record1 =
            createAuditAggregateRecord("filter-status-1", "context-s1", "RUNNING");
        HibernateScheduledContextInstanceAuditAggregateRecord record2 =
            createAuditAggregateRecord("filter-status-2", "context-s2", "COMPLETE");
        dao.save(record1);
        dao.save(record2);

        // When
        ScheduledContextInstanceAuditAggregateSearchFilter filter =
            new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setStatus("RUNNING");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 1);
        // Find our record
        Optional<ScheduledContextInstanceAuditAggregateRecord> found = results.getResultList().stream()
            .filter(r -> r.getId().equals("filter-status-1"))
            .findFirst();
        assertTrue(found.isPresent());
        assertEquals("RUNNING", found.get().getStatus());
    }

    @Test
    public void testFindByFilterNullFilter() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("null-filter", "context-null", "COMPLETE");
        dao.save(record);

        // When - null filter should return all
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(null, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 1);
    }

    @Test
    public void testGetRepeatingJobStatusCounts() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record1 =
            createAuditAggregateRecord("repeat-1", "context-r1", "COMPLETE");
        record1.setContextInstanceId("instance-A");
        record1.setRepeatingJob(true);
        dao.save(record1);

        HibernateScheduledContextInstanceAuditAggregateRecord record2 =
            createAuditAggregateRecord("repeat-2", "context-r2", "COMPLETE");
        record2.setContextInstanceId("instance-A");
        record2.setRepeatingJob(true);
        dao.save(record2);

        HibernateScheduledContextInstanceAuditAggregateRecord record3 =
            createAuditAggregateRecord("repeat-3", "context-r3", "RUNNING");
        record3.setContextInstanceId("instance-A");
        record3.setRepeatingJob(true);
        dao.save(record3);

        HibernateScheduledContextInstanceAuditAggregateRecord record4 =
            createAuditAggregateRecord("repeat-4", "context-r4", "ERROR");
        record4.setContextInstanceId("instance-B");
        record4.setRepeatingJob(true);
        dao.save(record4);

        // Non-repeating job (should be excluded)
        HibernateScheduledContextInstanceAuditAggregateRecord record5 =
            createAuditAggregateRecord("repeat-5", "context-r5", "COMPLETE");
        record5.setContextInstanceId("instance-A");
        record5.setRepeatingJob(false);
        dao.save(record5);

        // When
        List<String> contextInstanceIds = Arrays.asList("instance-A", "instance-B");
        Map<String, Map<String, Integer>> counts = dao.getRepeatingJobStatusCounts(contextInstanceIds);

        // Then
        assertNotNull(counts);
        assertEquals(2, counts.size());

        Map<String, Integer> instanceACounts = counts.get("instance-A");
        assertNotNull(instanceACounts);
        assertEquals(Integer.valueOf(2), instanceACounts.get("COMPLETE"));
        assertEquals(Integer.valueOf(1), instanceACounts.get("RUNNING"));

        Map<String, Integer> instanceBCounts = counts.get("instance-B");
        assertNotNull(instanceBCounts);
        assertEquals(Integer.valueOf(1), instanceBCounts.get("ERROR"));
    }

    @Test
    public void testGetRepeatingJobStatusCountsEmptyList() {
        // When
        Map<String, Map<String, Integer>> counts = dao.getRepeatingJobStatusCounts(Collections.emptyList());

        // Then
        assertNotNull(counts);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testGetRepeatingJobStatusCountsNullList() {
        // When
        Map<String, Map<String, Integer>> counts = dao.getRepeatingJobStatusCounts(null);

        // Then
        assertNotNull(counts);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testRepeatingJobFlag() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("repeating-flag", "context-rf", "COMPLETE");
        record.setRepeatingJob(true);

        // When
        dao.save(record);

        // Then - verify by finding all and checking the flag
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findAll(-1, -1, null, null);
        Optional<ScheduledContextInstanceAuditAggregateRecord> found = results.getResultList().stream()
            .filter(r -> r.getId().equals("repeating-flag"))
            .findFirst();

        assertTrue(found.isPresent());
        assertTrue(found.get().isRepeatingJob());
    }

    @Test
    public void testJobTypeField() {
        // Given
        HibernateScheduledContextInstanceAuditAggregateRecord record =
            createAuditAggregateRecord("job-type", "context-jt", "COMPLETE");
        record.setJobType("SCHEDULED");

        // When
        dao.save(record);

        // Then - verify by finding all and checking the job type
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            dao.findAll(-1, -1, null, null);
        Optional<ScheduledContextInstanceAuditAggregateRecord> found = results.getResultList().stream()
            .filter(r -> r.getId().equals("job-type"))
            .findFirst();

        assertTrue(found.isPresent());
        assertEquals("SCHEDULED", found.get().getJobType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // Given - create an invalid record that doesn't extend Hibernate implementation
        ScheduledContextInstanceAuditAggregateRecord invalidRecord =
            new ScheduledContextInstanceAuditAggregateRecord() {
                @Override
                public String getId() { return "invalid"; }

                @Override
                public String getContextName() { return "invalid"; }

                @Override
                public void setContextName(String contextName) {}

                @Override
                public String getContextInstanceId() { return "invalid"; }

                @Override
                public void setContextInstanceId(String contextInstanceId) {}

                @Override
                public String getScheduledProcessEventName() { return null; }

                @Override
                public void setScheduledProcessEventName(String scheduledProcessEventName) {}

                @Override
                public String getRaisedEvents() { return null; }

                @Override
                public ScheduledContextInstanceAuditAggregate getScheduledContextInstanceAuditAggregate() {
                    return null;
                }

                @Override
                public void setScheduledContextInstanceAuditAggregate(
                    ScheduledContextInstanceAuditAggregate scheduledContextInstanceAudit) {}

                @Override
                public long getTimestamp() { return 0; }

                @Override
                public String getStatus() { return null; }

                @Override
                public void setStatus(String status) {}

                @Override
                public boolean isRepeatingJob() { return false; }

                @Override
                public void setRepeatingJob(boolean repeatingJob) {}

                @Override
                public String getJobType() { return null; }

                @Override
                public void setJobType(String jobType) {}
            };

        // When - should throw IllegalArgumentException
        dao.save(invalidRecord);
    }

    // Mock implementation for testing
    private static class MockScheduledContextInstanceAuditAggregate
            implements ScheduledContextInstanceAuditAggregate {
        @Override
        public org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent getProcessEvent() {
            return null;
        }

        @Override
        public void setProcessEvent(org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) {}

        @Override
        public List<org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent> getSchedulerJobInitiationEvents() {
            return Collections.emptyList();
        }

        @Override
        public void setSchedulerJobInitiationEvents(List<org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent> schedulerJobInitiationEvents) {}

        @Override
        public String getPreviousContextInstanceAuditId() { return null; }

        @Override
        public void setPreviousContextInstanceAuditId(String previousContextInstanceAuditId) {}

        @Override
        public String getUpdatedContextInstanceAuditId() { return null; }

        @Override
        public void setUpdatedContextInstanceAuditId(String updatedContextInstanceAuditId) {}
    }
}
