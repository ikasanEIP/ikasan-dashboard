package org.ikasan.relational.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateScheduledContextInstanceDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateScheduledContextInstanceDaoImplTest {

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
    private ScheduledContextInstanceDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        // Clean up all test data
        SearchResults<ScheduledContextInstanceRecord> all = dao.getScheduledContextInstancesByStatus(null, -1, -1);
        all.getResultList().forEach(record -> {
            dao.deleteById(record.getId());
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private ContextInstance createContextInstance(String name) {
        ContextInstanceImpl instance = new ContextInstanceImpl();
        instance.setName(name);
        instance.setId("instance-" + name);
        instance.setCreatedDateTime(System.currentTimeMillis());
        instance.setUpdatedDateTime(System.currentTimeMillis());
        return instance;
    }

    private HibernateScheduledContextInstanceRecord createScheduledContextInstanceRecord(String contextName, String status) {
        HibernateScheduledContextInstanceRecord record = new HibernateScheduledContextInstanceRecord("id-" + contextName);
        record.setContextName(contextName);
        record.setContextInstanceId("instance-" + contextName);
        record.setContextInstance(createContextInstance(contextName));
        record.setStatus(status);
        record.setModifiedBy("test-user");
        record.setStartTime(System.currentTimeMillis());
        record.setEndTime(System.currentTimeMillis() + 10000);
        return record;
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        HibernateScheduledContextInstanceRecord record = createScheduledContextInstanceRecord("test-context-1", "RUNNING");

        // When
        dao.save(record);

        // Then
        ScheduledContextInstanceRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-context-1", found.getContextName());
        assertEquals("instance-test-context-1", found.getContextInstanceId());
        assertEquals("RUNNING", found.getStatus());
        assertNotNull(found.getContextInstance());
        assertEquals("test-context-1", found.getContextInstance().getName());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        ScheduledContextInstanceRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateScheduledContextInstanceRecord record = createScheduledContextInstanceRecord("test-context-2", "COMPLETE");

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
        assertEquals(record.getContextInstanceId(), "instance-test-context-2");
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        HibernateScheduledContextInstanceRecord record = createScheduledContextInstanceRecord("test-context-3", "RUNNING");

        // Save initial
        dao.save(record);

        long initialTimestamp = record.getTimestamp();
        long initialModifiedTimestamp = record.getModifiedTimestamp();

        Thread.sleep(10); // Ensure time difference

        // Update
        record.setStatus("COMPLETE");
        record.setEndTime(System.currentTimeMillis());

        dao.save(record);

        // Then
        assertEquals(initialTimestamp, record.getTimestamp()); // Original timestamp preserved
        assertTrue(record.getModifiedTimestamp() > initialModifiedTimestamp); // Modified timestamp updated

        ScheduledContextInstanceRecord found = dao.findById(record.getId());
        assertEquals("COMPLETE", found.getStatus());
    }

    @Test
    public void testDeleteById() {
        // Given
        HibernateScheduledContextInstanceRecord record = createScheduledContextInstanceRecord("context-to-delete", "RUNNING");
        dao.save(record);

        // Verify it exists
        assertNotNull(dao.findById(record.getId()));

        // When
        dao.deleteById(record.getId());

        // Then
        assertNull(dao.findById(record.getId()));
    }

    @Test
    public void testDeleteByIdNotFound() {
        // When - should not throw exception
        dao.deleteById("non-existent-id");

        // Then - no exception thrown
    }

    @Test
    public void testGetScheduledContextInstancesByStatus() {
        // Given
        dao.save(createScheduledContextInstanceRecord("context-1", "RUNNING"));
        dao.save(createScheduledContextInstanceRecord("context-2", "RUNNING"));
        dao.save(createScheduledContextInstanceRecord("context-3", "COMPLETE"));
        dao.save(createScheduledContextInstanceRecord("context-4", "ERROR"));

        // When
        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
            Arrays.asList(InstanceStatus.RUNNING)
        );

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testGetScheduledContextInstancesByStatusWithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createScheduledContextInstanceRecord("context-" + i, "RUNNING"));
        }

        // When
        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
            Arrays.asList(InstanceStatus.RUNNING), 2, 1
        );

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testGetScheduledContextInstancesByContextName() {
        // Given
        String contextName = "unique-context";
        dao.save(createScheduledContextInstanceRecord(contextName, "RUNNING"));
        dao.save(createScheduledContextInstanceRecord("other-context", "RUNNING"));

        // When
        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
            contextName, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals(contextName, results.getResultList().get(0).getContextName());
    }

    @Test
    public void testGetScheduledContextInstancesByContextNameWithTimeWindow() {
        // Given
        String contextName = "time-window-context";
        long now = System.currentTimeMillis();
        HibernateScheduledContextInstanceRecord record = createScheduledContextInstanceRecord(contextName, "RUNNING");
        record.setTimestamp(now);
        dao.save(record);

        // When - query within time window
        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
            contextName, now - 1000, now + 1000, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());

        // When - query outside time window
        SearchResults<ScheduledContextInstanceRecord> resultsOutside = dao.getScheduledContextInstancesByContextName(
            contextName, now + 2000, now + 3000, -1, -1, null, null
        );

        // Then
        assertEquals(0L, resultsOutside.getTotalNumberOfResults());
    }

    @Test
    public void testGetScheduledContextInstancesByFilter() {
        // Given
        dao.save(createScheduledContextInstanceRecord("production-context", "RUNNING"));
        dao.save(createScheduledContextInstanceRecord("staging-context", "COMPLETE"));
        dao.save(createScheduledContextInstanceRecord("development-context", "ERROR"));

        // When
        ContextInstanceSearchFilter filter = new ContextInstanceSearchFilter() {
            @Override
            public String getContextSearchFilter() {
                return "prod";
            }

            @Override
            public void setContextSearchFilter(String contextSearchFilter) {}

            @Override
            public List<String> getContextInstanceNames() {
                return null;
            }

            @Override
            public void setContextInstanceNames(List<String> contextInstanceNames) {}

            @Override
            public String getContextInstanceId() {
                return null;
            }

            @Override
            public void setContextInstanceId(String contextInstanceId) {}

            @Override
            public long getCreatedTimestamp() {
                return 0;
            }

            @Override
            public void setCreatedTimestamp(long createdTimestamp) {}

            @Override
            public long getModifiedTimestamp() {
                return 0;
            }

            @Override
            public void setModifiedTimestamp(long modifiedTimestamp) {}

            @Override
            public long getStartTime() {
                return 0;
            }

            @Override
            public void setStartTime(long timestamp) {}

            @Override
            public long getStartTimeStart() {
                return 0;
            }

            @Override
            public void setStartTimeStart(long timestamp) {}

            @Override
            public long getStartTimeEnd() {
                return 0;
            }

            @Override
            public void setStartTimeEnd(long timestamp) {}

            @Override
            public long getEndTime() {
                return 0;
            }

            @Override
            public void setEndTime(long timestamp) {}

            @Override
            public long getEndTimeStart() {
                return 0;
            }

            @Override
            public void setEndTimeStart(long timestamp) {}

            @Override
            public long getEndTimeEnd() {
                return 0;
            }

            @Override
            public void setEndTimeEnd(long timestamp) {}

            @Override
            public String getStatus() {
                return null;
            }

            @Override
            public void setStatus(String status) {}
        };

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("production-context", results.getResultList().get(0).getContextName());
    }

    @Test
    public void testGetScheduledContextInstancesByFilterWithStatus() {
        // Given
        dao.save(createScheduledContextInstanceRecord("context-a", "RUNNING"));
        dao.save(createScheduledContextInstanceRecord("context-b", "COMPLETE"));

        // When
        ContextInstanceSearchFilter filter = new MockContextInstanceSearchFilter();
        filter.setStatus("RUNNING");

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("RUNNING", results.getResultList().get(0).getStatus());
    }

    @Test
    public void testGetScheduledContextInstancesByFilterWithSortAscending() {
        // Given
        dao.save(createScheduledContextInstanceRecord("zebra-context", "RUNNING"));
        dao.save(createScheduledContextInstanceRecord("alpha-context", "RUNNING"));
        dao.save(createScheduledContextInstanceRecord("beta-context", "RUNNING"));

        // When
        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            null, -1, -1, "contextName", "ASCENDING"
        );

        // Then
        assertNotNull(results);
        assertEquals(3L, results.getTotalNumberOfResults());
        assertEquals("alpha-context", results.getResultList().get(0).getContextName());
        assertEquals("beta-context", results.getResultList().get(1).getContextName());
        assertEquals("zebra-context", results.getResultList().get(2).getContextName());
    }

    @Test
    public void testGetScheduledContextInstancesByFilterWithSortDescending() throws InterruptedException {
        // Given
        HibernateScheduledContextInstanceRecord record1 = createScheduledContextInstanceRecord("context-1", "RUNNING");
        dao.save(record1);
        Thread.sleep(10);

        HibernateScheduledContextInstanceRecord record2 = createScheduledContextInstanceRecord("context-2", "RUNNING");
        dao.save(record2);
        Thread.sleep(10);

        HibernateScheduledContextInstanceRecord record3 = createScheduledContextInstanceRecord("context-3", "RUNNING");
        dao.save(record3);

        // When
        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            null, -1, -1, "timestamp", "DESCENDING"
        );

        // Then
        assertNotNull(results);
        assertEquals(3L, results.getTotalNumberOfResults());
        // Most recent first
        assertEquals("context-3", results.getResultList().get(0).getContextName());
    }

    @Test
    public void testContainsRepeatingJobsFlag() {
        // Given
        HibernateScheduledContextInstanceRecord record = createScheduledContextInstanceRecord("repeating-context", "RUNNING");
        record.setContainsRepeatingJobs(true);

        // When
        dao.save(record);

        // Then
        ScheduledContextInstanceRecord found = dao.findById(record.getId());
        assertTrue(found.isContainsRepeatingJobs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // Given
        ScheduledContextInstanceRecord invalidRecord = new ScheduledContextInstanceRecord() {
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
            public ContextInstance getContextInstance() { return null; }

            @Override
            public void setContextInstance(ContextInstance context) {}

            @Override
            public String getStatus() { return "RUNNING"; }

            @Override
            public void setStatus(String status) {}

            @Override
            public long getTimestamp() { return 0; }

            @Override
            public void setTimestamp(long timestamp) {}

            @Override
            public long getModifiedTimestamp() { return 0; }

            @Override
            public void setModifiedTimestamp(long timestamp) {}

            @Override
            public long getStartTime() { return 0; }

            @Override
            public void setStartTime(long startTime) {}

            @Override
            public long getEndTime() { return 0; }

            @Override
            public void setEndTime(long endTime) {}

            @Override
            public boolean isContainsRepeatingJobs() { return false; }

            @Override
            public void setContainsRepeatingJobs(boolean containsRepeatingJobs) {}

            @Override
            public String getModifiedBy() { return null; }

            @Override
            public void setModifiedBy(String modifiedBy) {}
        };

        // When - should throw IllegalArgumentException
        dao.save(invalidRecord);
    }

    // Helper class for mock filter
    private static class MockContextInstanceSearchFilter implements ContextInstanceSearchFilter {
        private String contextSearchFilter;
        private String status;
        private String contextInstanceId;

        @Override
        public String getContextSearchFilter() { return contextSearchFilter; }

        @Override
        public void setContextSearchFilter(String contextSearchFilter) { this.contextSearchFilter = contextSearchFilter; }

        @Override
        public List<String> getContextInstanceNames() { return null; }

        @Override
        public void setContextInstanceNames(List<String> contextInstanceNames) {}

        @Override
        public String getContextInstanceId() { return contextInstanceId; }

        @Override
        public void setContextInstanceId(String contextInstanceId) { this.contextInstanceId = contextInstanceId; }

        @Override
        public long getCreatedTimestamp() { return 0; }

        @Override
        public void setCreatedTimestamp(long createdTimestamp) {}

        @Override
        public long getModifiedTimestamp() { return 0; }

        @Override
        public void setModifiedTimestamp(long modifiedTimestamp) {}

        @Override
        public long getStartTime() { return 0; }

        @Override
        public void setStartTime(long timestamp) {}

        @Override
        public long getStartTimeStart() { return 0; }

        @Override
        public void setStartTimeStart(long timestamp) {}

        @Override
        public long getStartTimeEnd() { return 0; }

        @Override
        public void setStartTimeEnd(long timestamp) {}

        @Override
        public long getEndTime() { return 0; }

        @Override
        public void setEndTime(long timestamp) {}

        @Override
        public long getEndTimeStart() { return 0; }

        @Override
        public void setEndTimeStart(long timestamp) {}

        @Override
        public long getEndTimeEnd() { return 0; }

        @Override
        public void setEndTimeEnd(long timestamp) {}

        @Override
        public String getStatus() { return status; }

        @Override
        public void setStatus(String status) { this.status = status; }
    }
}
