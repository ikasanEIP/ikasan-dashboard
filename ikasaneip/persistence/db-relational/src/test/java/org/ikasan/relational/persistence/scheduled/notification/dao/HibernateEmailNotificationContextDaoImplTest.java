package org.ikasan.relational.persistence.scheduled.notification.dao;

import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
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
 * Integration tests for HibernateEmailNotificationContextDaoImpl using PostgreSQL test container.
 *
 * These tests verify:
 * - Save/update operations with JSONB persistence
 * - FindAll operations with pagination
 * - FindByContextName operations
 * - Delete operations
 * - Complex email notification context data persistence
 * - Timestamp management
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateEmailNotificationContextDaoImplTest {

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
    private EmailNotificationContextDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        // Clean up all test data
        dao.findAll(Integer.MAX_VALUE, 0).getResultList()
            .forEach(emailNotificationContextRecord
                -> dao.deleteByContextName(emailNotificationContextRecord.getContextName()));
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // ========== Helper Methods ==========

    private HibernateEmailNotificationContextRecord createRecord(String contextName) {
        HibernateEmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();

        EmailNotificationContextImpl context = new EmailNotificationContextImpl();
        context.setContextName(contextName);
        context.setMonitorTypes(Arrays.asList("ERROR", "WARNING"));
        context.setEmailSendTo(Arrays.asList("admin@example.com"));
        context.setHtml(true);

        record.setEmailNotificationContext(context);
        record.setModifiedBy("test-user");

        return record;
    }

    private EmailNotificationContextImpl createComplexContext(String contextName) {
        EmailNotificationContextImpl context = new EmailNotificationContextImpl();
        context.setContextName(contextName);

        // Monitor types
        context.setMonitorTypes(Arrays.asList("ERROR", "WARNING", "INFO"));

        // Email recipients
        context.setEmailSendTo(Arrays.asList("admin@example.com", "team@example.com"));
        context.setEmailSendCc(Arrays.asList("manager@example.com"));
        context.setEmailSendBcc(Arrays.asList("audit@example.com"));

        // Monitor type specific recipients
        Map<String, List<String>> toByType = new HashMap<>();
        toByType.put("ERROR", Arrays.asList("oncall@example.com"));
        toByType.put("WARNING", Arrays.asList("dev@example.com"));
        context.setEmailSendToByMonitorType(toByType);

        Map<String, List<String>> ccByType = new HashMap<>();
        ccByType.put("ERROR", Arrays.asList("manager@example.com"));
        context.setEmailSendCcByMonitorType(ccByType);

        Map<String, List<String>> bccByType = new HashMap<>();
        bccByType.put("INFO", Arrays.asList("analytics@example.com"));
        context.setEmailSendBccByMonitorType(bccByType);

        // Templates
        Map<String, String> subjectTemplates = new HashMap<>();
        subjectTemplates.put("ERROR", "Critical Error in ${contextName}");
        subjectTemplates.put("WARNING", "Warning in ${contextName}");
        context.setEmailSubjectNotificationTemplate(subjectTemplates);

        Map<String, String> bodyTemplates = new HashMap<>();
        bodyTemplates.put("ERROR", "<h1>Error Details</h1><p>${message}</p>");
        bodyTemplates.put("WARNING", "<h2>Warning</h2><p>${message}</p>");
        context.setEmailBodyNotificationTemplate(bodyTemplates);

        context.setAttachment("report.pdf");
        context.setHtml(true);

        return context;
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
        EmailNotificationContextRecord mockRecord = new EmailNotificationContextRecord() {
            @Override
            public String getId() { return "test"; }
            @Override
            public void setId(String id) {}
            @Override
            public String getContextName() { return "test"; }
            @Override
            public void setContextName(String contextName) {}
            @Override
            public EmailNotificationContext getEmailNotificationContext() { return null; }
            @Override
            public void setEmailNotificationContext(EmailNotificationContext context) {}
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
        HibernateEmailNotificationContextRecord record = createRecord("test-context");

        // When
        dao.save(record);

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("test-context", 10, 0);
        assertEquals(1, results.getResultList().size());

        EmailNotificationContextRecord retrieved = results.getResultList().get(0);
        assertEquals("test-context", retrieved.getContextName());
        assertNotNull(retrieved.getEmailNotificationContext());
        assertEquals("test-context", retrieved.getEmailNotificationContext().getContextName());
        assertEquals("test-user", retrieved.getModifiedBy());
        assertTrue(retrieved.getTimestamp() > 0);
        assertTrue(retrieved.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_updates_existing_record() throws InterruptedException {
        // Given
        HibernateEmailNotificationContextRecord record = createRecord("update-context");
        dao.save(record);
        long originalTimestamp = record.getTimestamp();

        Thread.sleep(10);

        // When - update the same context
        EmailNotificationContextImpl updatedContext = new EmailNotificationContextImpl();
        updatedContext.setContextName("update-context");
        updatedContext.setMonitorTypes(Arrays.asList("ERROR"));
        updatedContext.setEmailSendTo(Arrays.asList("new-admin@example.com"));
        updatedContext.setHtml(false);

        record.setEmailNotificationContext(updatedContext);
        record.setModifiedBy("updated-user");
        dao.save(record);

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("update-context", 10, 0);
        assertEquals(1, results.getResultList().size());

        EmailNotificationContextRecord retrieved = results.getResultList().get(0);
        assertEquals("update-context", retrieved.getContextName());
        assertEquals(1, retrieved.getEmailNotificationContext().getMonitorTypes().size());
        assertEquals("ERROR", retrieved.getEmailNotificationContext().getMonitorTypes().get(0));
        assertEquals("new-admin@example.com", retrieved.getEmailNotificationContext().getEmailSendTo().get(0));
        assertFalse(retrieved.getEmailNotificationContext().isHtml());
        assertEquals("updated-user", retrieved.getModifiedBy());
        assertTrue(retrieved.getModifiedTimestamp() > originalTimestamp);
    }

    @Test
    public void test_save_sets_timestamps_automatically() {
        // Given
        HibernateEmailNotificationContextRecord record = createRecord("timestamp-context");
        long beforeSave = System.currentTimeMillis();

        // When
        dao.save(record);
        long afterSave = System.currentTimeMillis();

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("timestamp-context", 10, 0);
        EmailNotificationContextRecord retrieved = results.getResultList().get(0);

        assertTrue("Timestamp should be set", retrieved.getTimestamp() > 0);
        assertTrue("Timestamp should be within test execution time",
            retrieved.getTimestamp() >= beforeSave && retrieved.getTimestamp() <= afterSave);
        assertTrue("Modified timestamp should be set", retrieved.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_syncs_context_name_from_email_notification_context() {
        // Given
        HibernateEmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();

        EmailNotificationContextImpl context = new EmailNotificationContextImpl();
        context.setContextName("auto-sync-context");
        context.setMonitorTypes(Arrays.asList("ERROR"));

        record.setEmailNotificationContext(context);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("auto-sync-context", 10, 0);
        assertEquals(1, results.getResultList().size());
        assertEquals("auto-sync-context", results.getResultList().get(0).getContextName());
    }

    // ========== Complex Data Tests ==========

    @Test
    public void test_save_complex_notification_context() {
        // Given
        HibernateEmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();
        EmailNotificationContextImpl context = createComplexContext("complex-context");
        record.setEmailNotificationContext(context);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("complex-context", 10, 0);
        EmailNotificationContext retrieved = results.getResultList().get(0).getEmailNotificationContext();

        // Verify monitor types
        assertEquals(3, retrieved.getMonitorTypes().size());
        assertTrue(retrieved.getMonitorTypes().contains("ERROR"));
        assertTrue(retrieved.getMonitorTypes().contains("WARNING"));
        assertTrue(retrieved.getMonitorTypes().contains("INFO"));

        // Verify recipients
        assertEquals(2, retrieved.getEmailSendTo().size());
        assertTrue(retrieved.getEmailSendTo().contains("admin@example.com"));
        assertEquals(1, retrieved.getEmailSendCc().size());
        assertEquals(1, retrieved.getEmailSendBcc().size());

        // Verify monitor type specific recipients
        assertEquals(1, retrieved.getEmailSendToByMonitorType().get("ERROR").size());
        assertTrue(retrieved.getEmailSendToByMonitorType().get("ERROR").contains("oncall@example.com"));
        assertEquals(1, retrieved.getEmailSendCcByMonitorType().get("ERROR").size());
        assertEquals(1, retrieved.getEmailSendBccByMonitorType().get("INFO").size());

        // Verify templates
        assertEquals(2, retrieved.getEmailSubjectNotificationTemplate().size());
        assertTrue(retrieved.getEmailSubjectNotificationTemplate().get("ERROR").contains("Critical Error"));
        assertEquals(2, retrieved.getEmailBodyNotificationTemplate().size());
        assertTrue(retrieved.getEmailBodyNotificationTemplate().get("ERROR").contains("<h1>Error Details</h1>"));

        // Verify other fields
        assertEquals("report.pdf", retrieved.getAttachment());
        assertTrue(retrieved.isHtml());
    }

    @Test
    public void test_save_with_empty_collections() {
        // Given
        HibernateEmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();

        EmailNotificationContextImpl context = new EmailNotificationContextImpl();
        context.setContextName("empty-collections");
        context.setMonitorTypes(new ArrayList<>());
        context.setEmailSendTo(new ArrayList<>());
        context.setHtml(false);

        record.setEmailNotificationContext(context);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("empty-collections", 10, 0);
        EmailNotificationContext retrieved = results.getResultList().get(0).getEmailNotificationContext();

        assertNotNull(retrieved.getMonitorTypes());
        assertEquals(0, retrieved.getMonitorTypes().size());
        assertNotNull(retrieved.getEmailSendTo());
        assertEquals(0, retrieved.getEmailSendTo().size());
    }

    @Test
    public void test_save_with_null_optional_fields() {
        // Given
        HibernateEmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();

        EmailNotificationContextImpl context = new EmailNotificationContextImpl();
        context.setContextName("null-optionals");
        context.setMonitorTypes(Arrays.asList("ERROR"));
        // Leave optional fields as null

        record.setEmailNotificationContext(context);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("null-optionals", 10, 0);
        EmailNotificationContext retrieved = results.getResultList().get(0).getEmailNotificationContext();

        assertEquals("null-optionals", retrieved.getContextName());
        assertEquals(1, retrieved.getMonitorTypes().size());
        assertNull(retrieved.getEmailSendTo());
        assertNull(retrieved.getEmailSendCc());
        assertNull(retrieved.getAttachment());
    }

    // ========== FindAll Tests ==========

    @Test
    public void test_findAll_returns_empty_when_no_records() {
        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findAll(100, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_returns_all_records() {
        // Given
        dao.save(createRecord("context1"));
        dao.save(createRecord("context2"));
        dao.save(createRecord("context3"));

        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findAll(100, 0);

        // Then
        assertEquals(3, results.getResultList().size());
        assertEquals(3L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_respects_limit() {
        // Given - create 5 records
        for (int i = 0; i < 5; i++) {
            dao.save(createRecord("context-" + i));
        }

        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findAll(3, 0);

        // Then
        assertEquals("Should return only 3 records", 3, results.getResultList().size());
        assertEquals("Total count should be 5", 5L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_respects_offset() {
        // Given - create 5 records
        for (int i = 0; i < 5; i++) {
            dao.save(createRecord("context-" + i));
        }

        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findAll(100, 2);

        // Then
        assertEquals("Should skip first 2 records", 3, results.getResultList().size());
        assertEquals(5L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_orders_by_context_name() {
        // Given
        dao.save(createRecord("zebra"));
        dao.save(createRecord("alpha"));
        dao.save(createRecord("delta"));

        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findAll(100, 0);

        // Then
        assertEquals(3, results.getResultList().size());
        assertEquals("alpha", results.getResultList().get(0).getContextName());
        assertEquals("delta", results.getResultList().get(1).getContextName());
        assertEquals("zebra", results.getResultList().get(2).getContextName());
    }

    @Test
    public void test_findAll_with_zero_limit_returns_all() {
        // Given
        dao.save(createRecord("context1"));
        dao.save(createRecord("context2"));

        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findAll(0, 0);

        // Then
        assertEquals(2, results.getResultList().size());
    }

    // ========== FindByContextName Tests ==========

    @Test
    public void test_findByContextName_returns_matching_record() {
        // Given
        dao.save(createRecord("find-me"));
        dao.save(createRecord("not-me"));

        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("find-me", 10, 0);

        // Then
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("find-me", results.getResultList().get(0).getContextName());
    }

    @Test
    public void test_findByContextName_returns_empty_when_not_found() {
        // Given
        dao.save(createRecord("existing"));

        // When
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("non-existent", 10, 0);

        // Then
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByContextName_is_case_sensitive() {
        // Given
        dao.save(createRecord("TestContext"));

        // When
        SearchResults<EmailNotificationContextRecord> upperResults = dao.findByContextName("TestContext", 10, 0);
        SearchResults<EmailNotificationContextRecord> lowerResults = dao.findByContextName("testcontext", 10, 0);

        // Then
        assertEquals(1, upperResults.getResultList().size());
        assertEquals(0, lowerResults.getResultList().size());
    }

    @Test
    public void test_findByContextName_respects_pagination() {
        // Given
        dao.save(createRecord("paginated"));

        // When - with offset beyond available results
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("paginated", 10, 5);

        // Then
        assertEquals(0, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
    }

    // ========== Delete Tests ==========

    @Test
    public void test_deleteByContextName_removes_record() {
        // Given
        dao.save(createRecord("to-delete"));
        dao.save(createRecord("to-keep"));

        SearchResults<EmailNotificationContextRecord> beforeDelete = dao.findAll(100, 0);
        assertEquals(2, beforeDelete.getResultList().size());

        // When
        dao.deleteByContextName("to-delete");

        // Then
        SearchResults<EmailNotificationContextRecord> afterDelete = dao.findAll(100, 0);
        assertEquals(1, afterDelete.getResultList().size());
        assertEquals("to-keep", afterDelete.getResultList().get(0).getContextName());
    }

    @Test
    public void test_deleteByContextName_handles_non_existent() {
        // Given
        dao.save(createRecord("existing"));

        // When - delete non-existent (should not throw exception)
        dao.deleteByContextName("non-existent");

        // Then
        SearchResults<EmailNotificationContextRecord> results = dao.findAll(100, 0);
        assertEquals(1, results.getResultList().size());
    }

    // ========== Edge Cases ==========

    @Test
    public void test_save_multiple_different_contexts() {
        // Given - create records for different contexts
        HibernateEmailNotificationContextRecord record1 = new HibernateEmailNotificationContextRecord();
        record1.setEmailNotificationContext(createComplexContext("prod"));
        record1.setModifiedBy("user1");

        HibernateEmailNotificationContextRecord record2 = new HibernateEmailNotificationContextRecord();
        record2.setEmailNotificationContext(createComplexContext("dev"));
        record2.setModifiedBy("user2");

        HibernateEmailNotificationContextRecord record3 = new HibernateEmailNotificationContextRecord();
        record3.setEmailNotificationContext(createComplexContext("test"));
        record3.setModifiedBy("user3");

        // When
        dao.save(record1);
        dao.save(record2);
        dao.save(record3);

        // Then
        SearchResults<EmailNotificationContextRecord> allResults = dao.findAll(100, 0);
        assertEquals(3, allResults.getResultList().size());

        SearchResults<EmailNotificationContextRecord> prodResults = dao.findByContextName("prod", 10, 0);
        assertEquals(1, prodResults.getResultList().size());
        assertEquals("user1", prodResults.getResultList().get(0).getModifiedBy());
    }

    @Test
    public void test_context_name_uniqueness() {
        // Given
        HibernateEmailNotificationContextRecord record1 = createRecord("unique-context");
        record1.setModifiedBy("first-save");
        dao.save(record1);

        // When - save again with same context name
        HibernateEmailNotificationContextRecord record2 = createRecord("unique-context");
        record2.setModifiedBy("second-save");
        dao.save(record2);

        // Then - should update, not create duplicate
        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("unique-context", 10, 0);
        assertEquals(1, results.getResultList().size());
        assertEquals("second-save", results.getResultList().get(0).getModifiedBy());
    }
}
