package org.ikasan.relational.persistence.scheduled.notification.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateNotificationSendAudit;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
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
 * Integration tests for HibernateNotificationSendAuditDaoImpl using PostgreSQL test container.
 *
 * These tests verify:
 * - Save/update operations with JSONB persistence
 * - Find operations by composite key
 * - ID generation and uniqueness
 * - Notification send status tracking
 * - Timestamp management
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateNotificationSendAuditDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();
    }

    @Autowired
    private NotificationSendAuditDao<NotificationSendAuditRecord> dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // ========== Helper Methods ==========

    private HibernateNotificationSendAuditRecord createRecord(String contextInstanceId, String contextName,
                                                              String jobName, String monitorType,
                                                              String notifierType, boolean isSent) {
        HibernateNotificationSendAuditRecord record = new HibernateNotificationSendAuditRecord();

        HibernateNotificationSendAudit audit = new HibernateNotificationSendAudit();
        audit.setContextInstanceId(contextInstanceId);
        audit.setContextName(contextName);
        audit.setJobName(jobName);
        audit.setMonitorType(monitorType);
        audit.setNotifierType(notifierType);
        audit.setNotificationSend(isSent);

        record.setNotificationSendAudit(audit);
        record.setModifiedBy("test-user");

        return record;
    }

    // ========== Constructor and Validation Tests ==========

    @Test
    public void test_dao_autowired_successfully() {
        assertNotNull("DAO should be autowired", dao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_null_record_throwsException() {
        dao.save(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_nonHibernateRecord_throwsException() {
        NotificationSendAuditRecord mockRecord = new NotificationSendAuditRecord() {
            @Override
            public String getId() { return "test"; }
            @Override
            public void setId(String id) {}
            @Override
            public NotificationSendAudit getNotificationSendAudit() { return null; }
            @Override
            public void setNotificationSendAudit(NotificationSendAudit audit) {}
            @Override
            public long getTimestamp() { return 0; }
            @Override
            public void setTimestamp(long timestamp) {}
            @Override
            public long getModifiedTimestamp() { return 0; }
            @Override
            public void setModifiedTimestamp(long modifiedTimestamp) {}
            @Override
            public String getModifiedBy() { return null; }
            @Override
            public void setModifiedBy(String modifiedBy) {}
        };

        dao.save(mockRecord);
    }

    // ========== Save Tests ==========

    @Test
    public void test_save_and_retrieve_simple_record() {
        // Given
        HibernateNotificationSendAuditRecord record = createRecord(
            "instance-123", "TestContext", "TestJob", "ERROR", "EMAIL", true
        );

        // When
        dao.save(record);

        // Then
        NotificationSendAuditRecord retrieved = dao.find("instance-123", "TestContext", "TestJob", "ERROR", "EMAIL");
        assertNotNull(retrieved);
        assertNotNull(retrieved.getNotificationSendAudit());

        NotificationSendAudit audit = retrieved.getNotificationSendAudit();
        assertEquals("instance-123", audit.getContextInstanceId());
        assertEquals("TestContext", audit.getContextName());
        assertEquals("TestJob", audit.getJobName());
        assertEquals("ERROR", audit.getMonitorType());
        assertEquals("EMAIL", audit.getNotifierType());
        assertTrue(audit.isNotificationSend());
        assertEquals("test-user", retrieved.getModifiedBy());
        assertTrue(retrieved.getTimestamp() > 0);
        assertTrue(retrieved.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_generates_id_automatically() {
        // Given
        HibernateNotificationSendAuditRecord record = createRecord(
            "inst-1", "Context1", "Job1", "WARNING", "SMS", false
        );

        // When
        dao.save(record);

        // Then
        NotificationSendAuditRecord retrieved = dao.find("inst-1", "Context1", "Job1", "WARNING", "SMS");
        assertNotNull(retrieved.getId());
        assertEquals("inst-1_Context1_Job1_WARNING_SMS", retrieved.getId());
    }

    @Test
    public void test_save_updates_existing_record() throws InterruptedException {
        // Given
        HibernateNotificationSendAuditRecord record = createRecord(
            "inst-update", "UpdateContext", "UpdateJob", "INFO", "EMAIL", false
        );
        dao.save(record);
        long originalTimestamp = record.getTimestamp();

        Thread.sleep(10);

        // When - update the same record
        HibernateNotificationSendAudit updatedAudit = new HibernateNotificationSendAudit();
        updatedAudit.setContextInstanceId("inst-update");
        updatedAudit.setContextName("UpdateContext");
        updatedAudit.setJobName("UpdateJob");
        updatedAudit.setMonitorType("INFO");
        updatedAudit.setNotifierType("EMAIL");
        updatedAudit.setNotificationSend(true);  // Changed to true

        record.setNotificationSendAudit(updatedAudit);
        record.setModifiedBy("updated-user");
        dao.save(record);

        // Then
        NotificationSendAuditRecord retrieved = dao.find("inst-update", "UpdateContext", "UpdateJob", "INFO", "EMAIL");
        assertTrue(retrieved.getNotificationSendAudit().isNotificationSend());
        assertEquals("updated-user", retrieved.getModifiedBy());
        assertTrue(retrieved.getModifiedTimestamp() > originalTimestamp);
    }

    @Test
    public void test_save_sets_timestamps_automatically() {
        // Given
        HibernateNotificationSendAuditRecord record = createRecord(
            "inst-time", "TimeContext", "TimeJob", "DEBUG", "SLACK", true
        );
        long beforeSave = System.currentTimeMillis();

        // When
        dao.save(record);
        long afterSave = System.currentTimeMillis();

        // Then
        NotificationSendAuditRecord retrieved = dao.find("inst-time", "TimeContext", "TimeJob", "DEBUG", "SLACK");
        assertTrue("Timestamp should be set", retrieved.getTimestamp() > 0);
        assertTrue("Timestamp should be within test execution time",
            retrieved.getTimestamp() >= beforeSave && retrieved.getTimestamp() <= afterSave);
        assertTrue("Modified timestamp should be set", retrieved.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_notification_sent_true() {
        // Given
        HibernateNotificationSendAuditRecord record = createRecord(
            "inst-sent", "SentContext", "SentJob", "ERROR", "EMAIL", true
        );

        // When
        dao.save(record);

        // Then
        NotificationSendAuditRecord retrieved = dao.find("inst-sent", "SentContext", "SentJob", "ERROR", "EMAIL");
        assertTrue("Notification should be marked as sent", retrieved.getNotificationSendAudit().isNotificationSend());
    }

    @Test
    public void test_save_notification_sent_false() {
        // Given
        HibernateNotificationSendAuditRecord record = createRecord(
            "inst-not-sent", "NotSentContext", "NotSentJob", "ERROR", "EMAIL", false
        );

        // When
        dao.save(record);

        // Then
        NotificationSendAuditRecord retrieved = dao.find("inst-not-sent", "NotSentContext", "NotSentJob", "ERROR", "EMAIL");
        assertFalse("Notification should be marked as not sent", retrieved.getNotificationSendAudit().isNotificationSend());
    }

    // ========== Find Tests ==========

    @Test
    public void test_find_returns_matching_record() {
        // Given
        dao.save(createRecord("inst-1", "Context1", "Job1", "ERROR", "EMAIL", true));
        dao.save(createRecord("inst-2", "Context2", "Job2", "WARNING", "SMS", false));

        // When
        NotificationSendAuditRecord result1 = dao.find("inst-1", "Context1", "Job1", "ERROR", "EMAIL");
        NotificationSendAuditRecord result2 = dao.find("inst-2", "Context2", "Job2", "WARNING", "SMS");

        // Then
        assertNotNull(result1);
        assertEquals("inst-1", result1.getNotificationSendAudit().getContextInstanceId());
        assertTrue(result1.getNotificationSendAudit().isNotificationSend());

        assertNotNull(result2);
        assertEquals("inst-2", result2.getNotificationSendAudit().getContextInstanceId());
        assertFalse(result2.getNotificationSendAudit().isNotificationSend());
    }

    @Test
    public void test_find_returns_null_when_not_found() {
        // Given
        dao.save(createRecord("inst-existing", "ExistingContext", "ExistingJob", "ERROR", "EMAIL", true));

        // When
        NotificationSendAuditRecord result = dao.find("inst-nonexistent", "NonContext", "NonJob", "ERROR", "EMAIL");

        // Then
        assertNull(result);
    }

    @Test
    public void test_find_is_case_sensitive() {
        // Given
        dao.save(createRecord("inst-case", "CaseContext", "CaseJob", "ERROR", "EMAIL", true));

        // When
        NotificationSendAuditRecord upperResult = dao.find("inst-case", "CaseContext", "CaseJob", "ERROR", "EMAIL");
        NotificationSendAuditRecord lowerResult = dao.find("inst-case", "casecontext", "casejob", "error", "email");

        // Then
        assertNotNull(upperResult);
        assertNull(lowerResult);
    }

    @Test
    public void test_find_with_different_composite_key_parts() {
        // Given - same context instance but different jobs/monitors/notifiers
        dao.save(createRecord("inst-1", "Context", "Job1", "ERROR", "EMAIL", true));
        dao.save(createRecord("inst-1", "Context", "Job2", "ERROR", "EMAIL", false));
        dao.save(createRecord("inst-1", "Context", "Job1", "WARNING", "EMAIL", true));
        dao.save(createRecord("inst-1", "Context", "Job1", "ERROR", "SMS", false));

        // When/Then
        NotificationSendAuditRecord result1 = dao.find("inst-1", "Context", "Job1", "ERROR", "EMAIL");
        assertTrue(result1.getNotificationSendAudit().isNotificationSend());

        NotificationSendAuditRecord result2 = dao.find("inst-1", "Context", "Job2", "ERROR", "EMAIL");
        assertFalse(result2.getNotificationSendAudit().isNotificationSend());

        NotificationSendAuditRecord result3 = dao.find("inst-1", "Context", "Job1", "WARNING", "EMAIL");
        assertTrue(result3.getNotificationSendAudit().isNotificationSend());

        NotificationSendAuditRecord result4 = dao.find("inst-1", "Context", "Job1", "ERROR", "SMS");
        assertFalse(result4.getNotificationSendAudit().isNotificationSend());
    }

    // ========== Edge Cases and Complex Scenarios ==========

    @Test
    public void test_multiple_notifier_types_for_same_context_instance() {
        // Given - same context instance/job/monitor but different notifier types
        dao.save(createRecord("inst-multi", "MultiContext", "MultiJob", "ERROR", "EMAIL", true));
        dao.save(createRecord("inst-multi", "MultiContext", "MultiJob", "ERROR", "SMS", false));
        dao.save(createRecord("inst-multi", "MultiContext", "MultiJob", "ERROR", "SLACK", true));

        // Then - all should be independently retrievable
        NotificationSendAuditRecord email = dao.find("inst-multi", "MultiContext", "MultiJob", "ERROR", "EMAIL");
        NotificationSendAuditRecord sms = dao.find("inst-multi", "MultiContext", "MultiJob", "ERROR", "SMS");
        NotificationSendAuditRecord slack = dao.find("inst-multi", "MultiContext", "MultiJob", "ERROR", "SLACK");

        assertNotNull(email);
        assertTrue(email.getNotificationSendAudit().isNotificationSend());

        assertNotNull(sms);
        assertFalse(sms.getNotificationSendAudit().isNotificationSend());

        assertNotNull(slack);
        assertTrue(slack.getNotificationSendAudit().isNotificationSend());
    }

    @Test
    public void test_multiple_monitor_types_for_same_context_instance() {
        // Given - same context instance/job but different monitor types
        dao.save(createRecord("inst-mon", "MonContext", "MonJob", "ERROR", "EMAIL", true));
        dao.save(createRecord("inst-mon", "MonContext", "MonJob", "WARNING", "EMAIL", false));
        dao.save(createRecord("inst-mon", "MonContext", "MonJob", "INFO", "EMAIL", true));

        // Then - all should be independently retrievable
        NotificationSendAuditRecord error = dao.find("inst-mon", "MonContext", "MonJob", "ERROR", "EMAIL");
        NotificationSendAuditRecord warning = dao.find("inst-mon", "MonContext", "MonJob", "WARNING", "EMAIL");
        NotificationSendAuditRecord info = dao.find("inst-mon", "MonContext", "MonJob", "INFO", "EMAIL");

        assertNotNull(error);
        assertTrue(error.getNotificationSendAudit().isNotificationSend());

        assertNotNull(warning);
        assertFalse(warning.getNotificationSendAudit().isNotificationSend());

        assertNotNull(info);
        assertTrue(info.getNotificationSendAudit().isNotificationSend());
    }

    @Test
    public void test_id_uniqueness_prevents_duplicates() {
        // Given
        HibernateNotificationSendAuditRecord record1 = createRecord(
            "inst-unique", "UniqueContext", "UniqueJob", "ERROR", "EMAIL", false
        );
        record1.setModifiedBy("first-save");
        dao.save(record1);

        // When - save again with same composite key but different status
        HibernateNotificationSendAuditRecord record2 = createRecord(
            "inst-unique", "UniqueContext", "UniqueJob", "ERROR", "EMAIL", true
        );
        record2.setModifiedBy("second-save");
        dao.save(record2);

        // Then - should update, not create duplicate
        NotificationSendAuditRecord retrieved = dao.find("inst-unique", "UniqueContext", "UniqueJob", "ERROR", "EMAIL");
        assertNotNull(retrieved);
        assertTrue("Should have updated status to true", retrieved.getNotificationSendAudit().isNotificationSend());
        assertEquals("second-save", retrieved.getModifiedBy());
    }

    @Test
    public void test_context_instance_with_special_characters() {
        // Given - IDs with special characters (but valid for composite key)
        dao.save(createRecord("inst-abc-123", "Context_Name", "Job-Name", "ERROR", "EMAIL", true));

        // When
        NotificationSendAuditRecord retrieved = dao.find("inst-abc-123", "Context_Name", "Job-Name", "ERROR", "EMAIL");

        // Then
        assertNotNull(retrieved);
        assertEquals("inst-abc-123", retrieved.getNotificationSendAudit().getContextInstanceId());
        assertEquals("Context_Name", retrieved.getNotificationSendAudit().getContextName());
        assertEquals("Job-Name", retrieved.getNotificationSendAudit().getJobName());
    }

    @Test
    public void test_long_composite_id_generation() {
        // Given - create record with long values
        String longContextId = "very-long-context-instance-id-12345678901234567890";
        String longContext = "VeryLongContextNameForTesting";
        String longJob = "VeryLongJobNameForTestingPurposes";
        String monitorType = "ERROR";
        String notifierType = "EMAIL";

        dao.save(createRecord(longContextId, longContext, longJob, monitorType, notifierType, true));

        // When
        NotificationSendAuditRecord retrieved = dao.find(longContextId, longContext, longJob, monitorType, notifierType);

        // Then
        assertNotNull(retrieved);
        String expectedId = longContextId + "_" + longContext + "_" + longJob + "_" + monitorType + "_" + notifierType;
        assertEquals(expectedId, retrieved.getId());
    }

    @Test
    public void test_tracking_notification_send_status_changes() throws InterruptedException {
        // Given - create initial record with notification not sent
        HibernateNotificationSendAuditRecord record = createRecord(
            "inst-track", "TrackContext", "TrackJob", "ERROR", "EMAIL", false
        );
        dao.save(record);

        NotificationSendAuditRecord initial = dao.find("inst-track", "TrackContext", "TrackJob", "ERROR", "EMAIL");
        assertFalse("Initially notification not sent", initial.getNotificationSendAudit().isNotificationSend());
        long initialModTime = initial.getModifiedTimestamp();

        Thread.sleep(10);

        // When - update to mark notification as sent
        HibernateNotificationSendAudit updatedAudit = new HibernateNotificationSendAudit();
        updatedAudit.setContextInstanceId("inst-track");
        updatedAudit.setContextName("TrackContext");
        updatedAudit.setJobName("TrackJob");
        updatedAudit.setMonitorType("ERROR");
        updatedAudit.setNotifierType("EMAIL");
        updatedAudit.setNotificationSend(true);

        record.setNotificationSendAudit(updatedAudit);
        dao.save(record);

        // Then
        NotificationSendAuditRecord updated = dao.find("inst-track", "TrackContext", "TrackJob", "ERROR", "EMAIL");
        assertTrue("Notification should now be marked as sent", updated.getNotificationSendAudit().isNotificationSend());
        assertTrue("Modified timestamp should be updated", updated.getModifiedTimestamp() > initialModTime);
    }
}
