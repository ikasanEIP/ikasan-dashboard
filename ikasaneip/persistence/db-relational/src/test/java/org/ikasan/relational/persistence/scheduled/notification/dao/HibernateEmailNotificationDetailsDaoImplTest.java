package org.ikasan.relational.persistence.scheduled.notification.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationDetails;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for HibernateEmailNotificationDetailsDaoImpl using PostgreSQL test container.
 *
 * These tests verify:
 * - Save/update operations with JSONB persistence
 * - FindAll operations with pagination
 * - FindByContextName operations
 * - FindByJobNameAndMonitorType lookups
 * - Delete operations (by context name and composite key)
 * - Complex email notification details data persistence
 * - ID generation and uniqueness
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateEmailNotificationDetailsDaoImplTest {

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
    private EmailNotificationDetailsDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        this.dao.findAll(Integer.MAX_VALUE, 0).getResultList()
            .forEach(emailNotificationDetailsRecord
                -> this.dao.deleteByContextName(emailNotificationDetailsRecord.getContextName()));
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // ========== Helper Methods ==========

    private HibernateEmailNotificationDetailsRecord createRecord(String jobName, String contextName, String childContextName, String monitorType) {
        HibernateEmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();

        HibernateEmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName(jobName);
        details.setContextName(contextName);
        details.setChildContextName(childContextName);
        details.setMonitorType(monitorType);
        details.setEmailSendTo(Arrays.asList("admin@example.com"));
        details.setEmailSubject("Test Subject");
        details.setEmailBody("Test Body");
        details.setHtml(true);

        record.setEmailNotificationDetails(details);
        record.setModifiedBy("test-user");

        return record;
    }

    private HibernateEmailNotificationDetails createComplexDetails(String jobName, String contextName, String childContextName, String monitorType) {
        HibernateEmailNotificationDetails details = new HibernateEmailNotificationDetails();

        details.setJobName(jobName);
        details.setContextName(contextName);
        details.setChildContextName(childContextName);
        details.setMonitorType(monitorType);

        // Recipients
        details.setEmailSendTo(Arrays.asList("admin@example.com", "team@example.com"));
        details.setEmailSendCc(Arrays.asList("manager@example.com"));
        details.setEmailSendBcc(Arrays.asList("audit@example.com"));

        // Templates and content
        details.setEmailSubjectTemplate("Error in ${jobName}: ${monitorType}");
        details.setEmailBodyTemplate("<h1>Error Details</h1><p>${message}</p>");
        details.setEmailSubject("Error in TestJob: ERROR");
        details.setEmailBody("<h1>Error Details</h1><p>Test error message</p>");

        // Template parameters
        Map<String, String> params = new HashMap<>();
        params.put("jobName", jobName);
        params.put("monitorType", monitorType);
        params.put("message", "Test error message");
        details.setEmailNotificationTemplateParameters(params);

        details.setAttachment("error-report.pdf");
        details.setHtml(true);

        return details;
    }

    // ========== Constructor and Validation Tests ==========

    @Test
    public void test_dao_autowired_successfully() {
        assertNotNull("DAO should be autowired", dao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_null_record_throwsException() {
        dao.save((HibernateEmailNotificationDetailsRecord)null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_nonHibernateRecord_throwsException() {
        EmailNotificationDetailsRecord mockRecord = new EmailNotificationDetailsRecord() {
            @Override
            public String getId() { return "test"; }
            @Override
            public void setId(String id) {}
            @Override
            public String getJobName() { return "test"; }
            @Override
            public void setJobName(String jobName) {}
            @Override
            public String getContextName() { return "test"; }
            @Override
            public void setContextName(String contextName) {}
            @Override
            public String getMonitorType() { return "ERROR"; }
            @Override
            public void setMonitorType(String monitorType) {}
            @Override
            public EmailNotificationDetails getEmailNotificationDetails() { return null; }
            @Override
            public void setEmailNotificationDetails(EmailNotificationDetails details) {}
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
        HibernateEmailNotificationDetailsRecord record = createRecord("TestJob", "TestContext", "ChildContext", "ERROR");

        // When
        dao.save(record);

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("TestJob", "ChildContext", "ERROR");
        assertNotNull(retrieved);
        assertEquals("TestJob", retrieved.getJobName());
        assertEquals("TestContext", retrieved.getContextName());
        assertEquals("ERROR", retrieved.getMonitorType());
        assertNotNull(retrieved.getEmailNotificationDetails());
        assertEquals("admin@example.com", retrieved.getEmailNotificationDetails().getEmailSendTo().get(0));
        assertEquals("test-user", retrieved.getModifiedBy());
        assertTrue(retrieved.getTimestamp() > 0);
        assertTrue(retrieved.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_generates_id_automatically() {
        // Given
        HibernateEmailNotificationDetailsRecord record = createRecord("Job1", "Context1", "Child1", "WARNING");

        // When
        dao.save(record);

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("Job1", "Child1", "WARNING");
        assertNotNull(retrieved.getId());
        assertEquals("Job1_Child1_WARNING", retrieved.getId());
    }

    @Test
    public void test_save_updates_existing_record() throws InterruptedException {
        // Given
        HibernateEmailNotificationDetailsRecord record = createRecord("UpdateJob", "UpdateContext", "UpdateChild", "INFO");
        dao.save(record);
        long originalTimestamp = record.getTimestamp();

        Thread.sleep(10);

        // When - update the same record
        HibernateEmailNotificationDetails updatedDetails = new HibernateEmailNotificationDetails();
        updatedDetails.setJobName("UpdateJob");
        updatedDetails.setContextName("UpdateContext");
        updatedDetails.setChildContextName("UpdateChild");
        updatedDetails.setMonitorType("INFO");
        updatedDetails.setEmailSendTo(Arrays.asList("new-admin@example.com"));
        updatedDetails.setEmailSubject("Updated Subject");
        updatedDetails.setHtml(false);

        record.setEmailNotificationDetails(updatedDetails);
        record.setModifiedBy("updated-user");
        dao.save(record);

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("UpdateJob", "UpdateChild", "INFO");
        assertEquals("new-admin@example.com", retrieved.getEmailNotificationDetails().getEmailSendTo().get(0));
        assertEquals("Updated Subject", retrieved.getEmailNotificationDetails().getEmailSubject());
        assertFalse(retrieved.getEmailNotificationDetails().isHtml());
        assertEquals("updated-user", retrieved.getModifiedBy());
        assertTrue(retrieved.getModifiedTimestamp() > originalTimestamp);
    }

    @Test
    public void test_save_sets_timestamps_automatically() {
        // Given
        long beforeSave = System.currentTimeMillis();
        HibernateEmailNotificationDetailsRecord record = createRecord("TimestampJob", "TimestampContext", "TimestampChild", "ERROR");

        // When
        dao.save(record);
        long afterSave = System.currentTimeMillis();

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("TimestampJob", "TimestampChild", "ERROR");
        assertTrue("Timestamp should be set", retrieved.getTimestamp() > 0);
        assertTrue("Timestamp should be within test execution time",
            retrieved.getTimestamp() >= beforeSave && retrieved.getTimestamp() <= afterSave);
        assertTrue("Modified timestamp should be set", retrieved.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_syncs_indexed_fields_from_details() {
        // Given
        HibernateEmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();

        HibernateEmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName("SyncJob");
        details.setContextName("SyncContext");
        details.setChildContextName("SyncChild");
        details.setMonitorType("WARNING");
        details.setEmailSendTo(Arrays.asList("test@example.com"));

        record.setEmailNotificationDetails(details);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("SyncJob", "SyncChild", "WARNING");
        assertEquals("SyncJob", retrieved.getJobName());
        assertEquals("SyncContext", retrieved.getContextName());
        assertEquals("WARNING", retrieved.getMonitorType());
    }

    // ========== Complex Data Tests ==========

    @Test
    public void test_save_complex_notification_details() {
        // Given
        HibernateEmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();
        HibernateEmailNotificationDetails details = createComplexDetails("ComplexJob", "ComplexContext", "ComplexChild", "ERROR");
        record.setEmailNotificationDetails(details);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("ComplexJob", "ComplexChild", "ERROR");
        EmailNotificationDetails retrievedDetails = retrieved.getEmailNotificationDetails();

        // Verify recipients
        assertEquals(2, retrievedDetails.getEmailSendTo().size());
        assertTrue(retrievedDetails.getEmailSendTo().contains("admin@example.com"));
        assertEquals(1, retrievedDetails.getEmailSendCc().size());
        assertEquals(1, retrievedDetails.getEmailSendBcc().size());

        // Verify templates and content
        assertEquals("Error in ${jobName}: ${monitorType}", retrievedDetails.getEmailSubjectTemplate());
        assertEquals("<h1>Error Details</h1><p>${message}</p>", retrievedDetails.getEmailBodyTemplate());
        assertEquals("Error in TestJob: ERROR", retrievedDetails.getEmailSubject());
        assertEquals("<h1>Error Details</h1><p>Test error message</p>", retrievedDetails.getEmailBody());

        // Verify template parameters
        assertEquals(3, retrievedDetails.getEmailNotificationTemplateParameters().size());
        assertEquals("ComplexJob", retrievedDetails.getEmailNotificationTemplateParameters().get("jobName"));
        assertEquals("ERROR", retrievedDetails.getEmailNotificationTemplateParameters().get("monitorType"));

        // Verify other fields
        assertEquals("error-report.pdf", retrievedDetails.getAttachment());
        assertTrue(retrievedDetails.isHtml());
    }

    @Test
    public void test_save_with_empty_collections() {
        // Given
        HibernateEmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();

        HibernateEmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName("EmptyJob");
        details.setContextName("EmptyContext");
        details.setChildContextName("EmptyChild");
        details.setMonitorType("INFO");
        details.setEmailSendTo(new ArrayList<>());
        details.setHtml(false);

        record.setEmailNotificationDetails(details);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("EmptyJob", "EmptyChild", "INFO");
        EmailNotificationDetails retrievedDetails = retrieved.getEmailNotificationDetails();

        assertNull(retrievedDetails.getEmailSendTo());
    }

    @Test
    public void test_save_with_null_optional_fields() {
        // Given
        HibernateEmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();

        HibernateEmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName("NullJob");
        details.setContextName("NullContext");
        details.setChildContextName("NullChild");
        details.setMonitorType("DEBUG");
        // Leave optional fields as null

        record.setEmailNotificationDetails(details);
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("NullJob", "NullChild", "DEBUG");
        EmailNotificationDetails retrievedDetails = retrieved.getEmailNotificationDetails();

        assertEquals("NullJob", retrievedDetails.getJobName());
        assertNull(retrievedDetails.getEmailSendTo());
        assertNull(retrievedDetails.getEmailSubject());
        assertNull(retrievedDetails.getAttachment());
    }

    // ========== FindAll Tests ==========

    @Test
    public void test_findAll_returns_empty_when_no_records() {
        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(100, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_returns_all_records() {
        // Given
        dao.save(createRecord("Job1", "Context1", "Child1", "ERROR"));
        dao.save(createRecord("Job2", "Context2", "Child2", "WARNING"));
        dao.save(createRecord("Job3", "Context3", "Child3", "INFO"));

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(100, 0);

        // Then
        assertEquals(3, results.getResultList().size());
        assertEquals(3L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_respects_limit() {
        // Given - create 5 records
        for (int i = 0; i < 5; i++) {
            dao.save(createRecord("Job" + i, "Context" + i, "Child" + i, "ERROR"));
        }

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(3, 0);

        // Then
        assertEquals("Should return only 3 records", 3, results.getResultList().size());
        assertEquals("Total count should be 5", 5L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_respects_offset() {
        // Given - create 5 records
        for (int i = 0; i < 5; i++) {
            dao.save(createRecord("Job" + i, "Context" + i, "Child" + i, "WARNING"));
        }

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(100, 2);

        // Then
        assertEquals("Should skip first 2 records", 3, results.getResultList().size());
        assertEquals(5L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_orders_by_id() {
        // Given
        dao.save(createRecord("ZebraJob", "ZContext", "ZChild", "ERROR"));
        dao.save(createRecord("AlphaJob", "AContext", "AChild", "WARNING"));
        dao.save(createRecord("DeltaJob", "DContext", "DChild", "INFO"));

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(100, 0);

        // Then
        assertEquals(3, results.getResultList().size());
        // IDs are: AlphaJob_AChild_WARNING, DeltaJob_DChild_INFO, ZebraJob_ZChild_ERROR
        assertTrue(results.getResultList().get(0).getId().startsWith("AlphaJob"));
        assertTrue(results.getResultList().get(1).getId().startsWith("DeltaJob"));
        assertTrue(results.getResultList().get(2).getId().startsWith("ZebraJob"));
    }

    @Test
    public void test_findAll_with_zero_limit_returns_all() {
        // Given
        dao.save(createRecord("Job1", "Context1", "Child1", "ERROR"));
        dao.save(createRecord("Job2", "Context2", "Child2", "WARNING"));

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(0, 0);

        // Then
        assertEquals(2, results.getResultList().size());
    }

    // ========== FindByContextName Tests ==========

    @Test
    public void test_findByContextName_returns_matching_records() {
        // Given
        dao.save(createRecord("Job1", "SharedContext", "Child1", "ERROR"));
        dao.save(createRecord("Job2", "SharedContext", "Child2", "WARNING"));
        dao.save(createRecord("Job3", "DifferentContext", "Child3", "INFO"));

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findByContextName("SharedContext", 10, 0);

        // Then
        assertEquals(2, results.getResultList().size());
        assertEquals(2L, results.getTotalNumberOfResults());
        results.getResultList().forEach(record ->
            assertEquals("SharedContext", record.getContextName())
        );
    }

    @Test
    public void test_findByContextName_returns_empty_when_not_found() {
        // Given
        dao.save(createRecord("Job1", "ExistingContext", "Child1", "ERROR"));

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findByContextName("NonExistentContext", 10, 0);

        // Then
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByContextName_respects_pagination() {
        // Given - create 5 records for same context
        for (int i = 0; i < 5; i++) {
            dao.save(createRecord("Job" + i, "PaginatedContext", "Child" + i, "ERROR"));
        }

        // When
        SearchResults<EmailNotificationDetailsRecord> results = dao.findByContextName("PaginatedContext", 2, 1);

        // Then
        assertEquals(2, results.getResultList().size());
        assertEquals(5L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByContextName_is_case_sensitive() {
        // Given
        dao.save(createRecord("Job1", "TestContext", "Child1", "ERROR"));

        // When
        SearchResults<EmailNotificationDetailsRecord> upperResults = dao.findByContextName("TestContext", 10, 0);
        SearchResults<EmailNotificationDetailsRecord> lowerResults = dao.findByContextName("testcontext", 10, 0);

        // Then
        assertEquals(1, upperResults.getResultList().size());
        assertEquals(0, lowerResults.getResultList().size());
    }

    // ========== FindByJobNameAndMonitorType Tests ==========

    @Test
    public void test_findByJobNameAndMonitorType_returns_matching_record() {
        // Given
        dao.save(createRecord("FindJob", "FindContext", "FindChild", "ERROR"));
        dao.save(createRecord("FindJob", "FindContext", "FindChild", "WARNING"));

        // When
        EmailNotificationDetailsRecord errorResult = dao.findByJobNameAndMonitorType("FindJob", "FindChild", "ERROR");
        EmailNotificationDetailsRecord warningResult = dao.findByJobNameAndMonitorType("FindJob", "FindChild", "WARNING");

        // Then
        assertNotNull(errorResult);
        assertEquals("ERROR", errorResult.getMonitorType());

        assertNotNull(warningResult);
        assertEquals("WARNING", warningResult.getMonitorType());
    }

    @Test
    public void test_findByJobNameAndMonitorType_returns_null_when_not_found() {
        // Given
        dao.save(createRecord("ExistingJob", "ExistingContext", "ExistingChild", "ERROR"));

        // When
        EmailNotificationDetailsRecord result = dao.findByJobNameAndMonitorType("NonExistentJob", "NonExistentChild", "ERROR");

        // Then
        assertNull(result);
    }

    @Test
    public void test_findByJobNameAndMonitorType_is_case_sensitive() {
        // Given
        dao.save(createRecord("CaseSensitiveJob", "CaseContext", "CaseChild", "ERROR"));

        // When
        EmailNotificationDetailsRecord upperResult = dao.findByJobNameAndMonitorType("CaseSensitiveJob", "CaseChild", "ERROR");
        EmailNotificationDetailsRecord lowerResult = dao.findByJobNameAndMonitorType("casesensitivejob", "casechild", "error");

        // Then
        assertNotNull(upperResult);
        assertNull(lowerResult);
    }

    // ========== Delete Tests ==========

    @Test
    public void test_deleteByContextName_removes_matching_records() {
        // Given
        dao.save(createRecord("Job1", "DeleteContext", "Child1", "ERROR"));
        dao.save(createRecord("Job2", "DeleteContext", "Child2", "WARNING"));
        dao.save(createRecord("Job3", "KeepContext", "Child3", "INFO"));

        SearchResults<EmailNotificationDetailsRecord> beforeDelete = dao.findAll(100, 0);
        assertEquals(3, beforeDelete.getResultList().size());

        // When
        dao.deleteByContextName("DeleteContext");

        // Then
        SearchResults<EmailNotificationDetailsRecord> afterDelete = dao.findAll(100, 0);
        assertEquals(1, afterDelete.getResultList().size());
        assertEquals("KeepContext", afterDelete.getResultList().get(0).getContextName());
    }

    @Test
    public void test_deleteByContextName_handles_non_existent() {
        // Given
        dao.save(createRecord("Job1", "ExistingContext", "Child1", "ERROR"));

        // When - delete non-existent (should not throw exception)
        dao.deleteByContextName("NonExistentContext");

        // Then
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(100, 0);
        assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_deleteByJobNameAndMonitorType_removes_specific_record() {
        // Given
        dao.save(createRecord("DeleteJob", "DeleteContext", "DeleteChild", "ERROR"));
        dao.save(createRecord("DeleteJob", "DeleteContext", "DeleteChild", "WARNING"));
        dao.save(createRecord("KeepJob", "DeleteContext", "KeepChild", "ERROR"));

        // When
        dao.deleteByJobNameAndMonitorType("DeleteJob", "DeleteChild", "ERROR");

        // Then
        EmailNotificationDetailsRecord deleted = dao.findByJobNameAndMonitorType("DeleteJob", "DeleteChild", "ERROR");
        assertNull(deleted);

        EmailNotificationDetailsRecord warning = dao.findByJobNameAndMonitorType("DeleteJob", "DeleteChild", "WARNING");
        assertNotNull(warning);

        EmailNotificationDetailsRecord kept = dao.findByJobNameAndMonitorType("KeepJob", "KeepChild", "ERROR");
        assertNotNull(kept);
    }

    @Test
    public void test_deleteByJobNameAndMonitorType_handles_non_existent() {
        // Given
        dao.save(createRecord("ExistingJob", "ExistingContext", "ExistingChild", "ERROR"));

        // When - delete non-existent (should not throw exception)
        dao.deleteByJobNameAndMonitorType("NonExistentJob", "NonExistentChild", "ERROR");

        // Then
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(100, 0);
        assertEquals(1, results.getResultList().size());
    }

    // ========== Edge Cases ==========

    @Test
    public void test_multiple_monitor_types_for_same_job() {
        // Given - create records for same job but different monitor types
        dao.save(createRecord("MultiTypeJob", "MultiContext", "MultiChild", "ERROR"));
        dao.save(createRecord("MultiTypeJob", "MultiContext", "MultiChild", "WARNING"));
        dao.save(createRecord("MultiTypeJob", "MultiContext", "MultiChild", "INFO"));

        // Then
        SearchResults<EmailNotificationDetailsRecord> results = dao.findByContextName("MultiContext", 100, 0);
        assertEquals(3, results.getResultList().size());

        EmailNotificationDetailsRecord error = dao.findByJobNameAndMonitorType("MultiTypeJob", "MultiChild", "ERROR");
        EmailNotificationDetailsRecord warning = dao.findByJobNameAndMonitorType("MultiTypeJob", "MultiChild", "WARNING");
        EmailNotificationDetailsRecord info = dao.findByJobNameAndMonitorType("MultiTypeJob", "MultiChild", "INFO");

        assertNotNull(error);
        assertNotNull(warning);
        assertNotNull(info);
    }

    @Test
    public void test_id_uniqueness_prevents_duplicates() {
        // Given
        HibernateEmailNotificationDetailsRecord record1 = createRecord("UniqueJob", "UniqueContext", "UniqueChild", "ERROR");
        record1.setModifiedBy("first-save");
        dao.save(record1);

        // When - save again with same composite key
        HibernateEmailNotificationDetailsRecord record2 = createRecord("UniqueJob", "UniqueContext", "UniqueChild", "ERROR");
        record2.setModifiedBy("second-save");
        dao.save(record2);

        // Then - should update, not create duplicate
        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(100, 0);
        long errorCount = results.getResultList().stream()
            .filter(r -> "UniqueJob_UniqueChild_ERROR".equals(r.getId()))
            .count();
        assertEquals(1, errorCount);

        EmailNotificationDetailsRecord retrieved = dao.findByJobNameAndMonitorType("UniqueJob", "UniqueChild", "ERROR");
        assertEquals("second-save", retrieved.getModifiedBy());
    }
}
