package org.ikasan.relational.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateScheduledContextInstanceAuditDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateScheduledContextInstanceAuditDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();
    }

    @Autowired
    private ScheduledContextInstanceAuditDao auditDao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
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

    private HibernateScheduledContextInstanceRecord createAuditRecord(String contextName, String status) {
        HibernateScheduledContextInstanceRecord record = new HibernateScheduledContextInstanceRecord("audit-id-" + contextName);
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
        HibernateScheduledContextInstanceRecord record = createAuditRecord("audit-context-1", "COMPLETE");

        // When
        auditDao.save(record);

        // Then
        ScheduledContextInstanceRecord found = auditDao.findById(record.getId());
        assertNotNull(found);
        assertEquals("audit-context-1", found.getContextName());
        assertEquals("instance-audit-context-1", found.getContextInstanceId());
        assertEquals("COMPLETE", found.getStatus());
        assertNotNull(found.getContextInstance());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        ScheduledContextInstanceRecord result = auditDao.findById("non-existent-audit-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateScheduledContextInstanceRecord record = createAuditRecord("audit-context-2", "ERROR");

        // When
        auditDao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
    }

    @Test
    public void testSaveMultipleAuditRecords() {
        // Given
        HibernateScheduledContextInstanceRecord record1 = createAuditRecord("audit-context-3", "RUNNING");
        HibernateScheduledContextInstanceRecord record2 = createAuditRecord("audit-context-4", "COMPLETE");
        HibernateScheduledContextInstanceRecord record3 = createAuditRecord("audit-context-5", "ERROR");

        // When
        auditDao.save(record1);
        auditDao.save(record2);
        auditDao.save(record3);

        // Then
        assertNotNull(auditDao.findById(record1.getId()));
        assertNotNull(auditDao.findById(record2.getId()));
        assertNotNull(auditDao.findById(record3.getId()));
    }

    @Test
    public void testAuditRecordPreservesAllFields() {
        // Given
        HibernateScheduledContextInstanceRecord record = createAuditRecord("detailed-audit", "COMPLETE");
        record.setContainsRepeatingJobs(true);
        record.setStartTime(1000L);
        record.setEndTime(2000L);

        // When
        auditDao.save(record);

        // Then
        ScheduledContextInstanceRecord found = auditDao.findById(record.getId());
        assertEquals("detailed-audit", found.getContextName());
        assertEquals("COMPLETE", found.getStatus());
        assertTrue(found.isContainsRepeatingJobs());
        assertEquals(1000L, found.getStartTime());
        assertEquals(2000L, found.getEndTime());
        assertEquals("test-user", found.getModifiedBy());
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
        auditDao.save(invalidRecord);
    }
}
