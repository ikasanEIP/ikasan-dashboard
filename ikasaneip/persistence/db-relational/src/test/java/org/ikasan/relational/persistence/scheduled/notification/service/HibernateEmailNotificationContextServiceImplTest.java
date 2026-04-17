package org.ikasan.relational.persistence.scheduled.notification.service;

import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateEmailNotificationContextDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationContextRecord;
import org.ikasan.relational.persistence.scheduled.notification.service.HibernateEmailNotificationContextServiceImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HibernateEmailNotificationContextServiceImplTest {

    @Mock
    private HibernateEmailNotificationContextDaoImpl dao;

    private HibernateEmailNotificationContextServiceImpl service;

    // ========== Constructor Tests ==========

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_with_null_dao_should_throw_exception() {
        new HibernateEmailNotificationContextServiceImpl(null);
    }

    @Test
    public void test_constructor_with_valid_dao_should_create_instance() {
        // When
        service = new HibernateEmailNotificationContextServiceImpl(dao);

        // Then
        assertNotNull(service);
    }

    // ========== FindAll Tests ==========

    @Test
    public void test_findAll_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        int limit = 10;
        int offset = 5;
        SearchResults<EmailNotificationContextRecord> expectedResults = createSearchResults(10, 50);
        when(dao.findAll(limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao, times(1)).findAll(limit, offset);
    }

    @Test
    public void test_findAll_with_zero_limit_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        int limit = 0;
        int offset = 0;
        SearchResults<EmailNotificationContextRecord> expectedResults = createSearchResults(0, 0);
        when(dao.findAll(limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao, times(1)).findAll(limit, offset);
    }

    @Test
    public void test_findAll_with_large_offset_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        int limit = 100;
        int offset = 1000;
        SearchResults<EmailNotificationContextRecord> expectedResults = createSearchResults(0, 2000);
        when(dao.findAll(limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao, times(1)).findAll(limit, offset);
    }

    // ========== FindByContextName Tests ==========

    @Test
    public void test_findByContextName_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        String contextName = "testContext";
        int limit = 20;
        int offset = 10;
        SearchResults<EmailNotificationContextRecord> expectedResults = createSearchResults(5, 20);
        when(dao.findByContextName(contextName, limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findByContextName(contextName, limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao, times(1)).findByContextName(contextName, limit, offset);
    }

    @Test
    public void test_findByContextName_with_empty_context_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        String contextName = "";
        int limit = 10;
        int offset = 0;
        SearchResults<EmailNotificationContextRecord> expectedResults = createSearchResults(0, 0);
        when(dao.findByContextName(contextName, limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findByContextName(contextName, limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao, times(1)).findByContextName(contextName, limit, offset);
    }

    @Test
    public void test_findByContextName_with_special_characters_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        String contextName = "test-context_123!@#";
        int limit = 10;
        int offset = 0;
        SearchResults<EmailNotificationContextRecord> expectedResults = createSearchResults(1, 1);
        when(dao.findByContextName(contextName, limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findByContextName(contextName, limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao, times(1)).findByContextName(contextName, limit, offset);
    }

    // ========== Save Tests ==========

    @Test
    public void test_save_with_record_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContextRecord record = createContextRecord("testContext");

        // When
        service.save(record);

        // Then
        verify(dao, times(1)).save(record);
    }

    @Test
    public void test_save_with_null_record_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);

        // When
        service.save(null);

        // Then
        verify(dao, times(1)).save(null);
    }

    @Test
    public void test_save_with_record_containing_all_fields_should_delegate_to_dao() {
        // Given
        Map<String, String> bodyTemplate = new HashMap<>();
        bodyTemplate.put("ERROR", "someBodyLocation.txt");

        Map<String, String> subjectTemplate = new HashMap<>();
        subjectTemplate.put("ERROR", "someSubjectLocation.txt");

        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();
        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName("fullContext");
        context.setEmailSendTo(Arrays.asList("to1@test.com", "to2@test.com"));
        context.setEmailSendCc(Arrays.asList("cc@test.com"));
        context.setEmailSendBcc(Arrays.asList("bcc@test.com"));
        context.setMonitorTypes(Arrays.asList("TYPE1", "TYPE2"));
        context.setEmailSubjectNotificationTemplate(bodyTemplate);
        context.setEmailBodyNotificationTemplate(subjectTemplate);
        context.setAttachment("/path/to/file");
        context.setHtml(true);

        record.setEmailNotificationContext(context);
        record.setTimestamp(System.currentTimeMillis());

        // When
        service.save(record);

        // Then
        verify(dao, times(1)).save(record);
    }

    // ========== SaveEmailNotificationContext Tests ==========

    @Test
    public void test_saveEmailNotificationContext_should_create_record_and_save() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName("testContext");
        context.setEmailSendTo(Arrays.asList("test@example.com"));

        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor
            = ArgumentCaptor.forClass(EmailNotificationContextRecord.class);

        // When
        service.saveEmailNotificationContext(context);

        // Then
        verify(dao, times(1)).save(recordCaptor.capture());
        EmailNotificationContextRecord capturedRecord = recordCaptor.getValue();

        assertNotNull(capturedRecord);
        assertSame(context, capturedRecord.getEmailNotificationContext());
        assertTrue(capturedRecord.getTimestamp() > 0);
    }

    @Test
    public void test_saveEmailNotificationContext_with_complex_context_should_create_record_and_save() {
        // Given
        Map<String, String> bodyTemplate = new HashMap<>();
        bodyTemplate.put("ERROR", "someBodyLocation.txt");

        Map<String, String> subjectTemplate = new HashMap<>();
        subjectTemplate.put("ERROR", "someSubjectLocation.txt");

        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName("complexContext");
        context.setEmailSendTo(Arrays.asList("user1@example.com", "user2@example.com"));
        context.setEmailSendCc(Arrays.asList("cc@example.com"));
        context.setEmailSendBcc(Arrays.asList("bcc@example.com"));
        context.setMonitorTypes(Arrays.asList("ON_SUCCESS", "ON_FAILURE"));
        context.setEmailSubjectNotificationTemplate(subjectTemplate);
        context.setEmailBodyNotificationTemplate(bodyTemplate);
        context.setHtml(true);

        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor = ArgumentCaptor.forClass(EmailNotificationContextRecord.class);

        // When
        service.saveEmailNotificationContext(context);

        // Then
        verify(dao, times(1)).save(recordCaptor.capture());
        EmailNotificationContextRecord capturedRecord = recordCaptor.getValue();

        assertNotNull(capturedRecord);
        assertEquals(context, capturedRecord.getEmailNotificationContext());
        assertTrue(capturedRecord.getTimestamp() > 0);
    }

    @Test
    public void test_saveEmailNotificationContext_with_minimal_context_should_create_record_and_save() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName("minimalContext");

        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor = ArgumentCaptor.forClass(EmailNotificationContextRecord.class);

        // When
        service.saveEmailNotificationContext(context);

        // Then
        verify(dao, times(1)).save(recordCaptor.capture());
        EmailNotificationContextRecord capturedRecord = recordCaptor.getValue();

        assertNotNull(capturedRecord);
        assertEquals("minimalContext", capturedRecord.getEmailNotificationContext().getContextName());
        assertTrue(capturedRecord.getTimestamp() > 0);
    }

    @Test
    public void test_saveEmailNotificationContext_should_set_timestamp_automatically() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName("timestampTest");

        long beforeSave = System.currentTimeMillis();
        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor = ArgumentCaptor.forClass(EmailNotificationContextRecord.class);

        // When
        service.saveEmailNotificationContext(context);

        long afterSave = System.currentTimeMillis();

        // Then
        verify(dao, times(1)).save(recordCaptor.capture());
        EmailNotificationContextRecord capturedRecord = recordCaptor.getValue();

        assertTrue(capturedRecord.getTimestamp() >= beforeSave);
        assertTrue(capturedRecord.getTimestamp() <= afterSave);
    }

    @Test
    public void test_saveEmailNotificationContext_multiple_consecutive_calls_should_create_multiple_records() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContext context1 = new EmailNotificationContextImpl();
        context1.setContextName("context1");

        EmailNotificationContext context2 = new EmailNotificationContextImpl();
        context2.setContextName("context2");

        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor = ArgumentCaptor.forClass(EmailNotificationContextRecord.class);

        // When
        service.saveEmailNotificationContext(context1);
        service.saveEmailNotificationContext(context2);

        // Then
        verify(dao, times(2)).save(recordCaptor.capture());
        List<EmailNotificationContextRecord> capturedRecords = recordCaptor.getAllValues();

        assertEquals(2, capturedRecords.size());
        assertEquals("context1", capturedRecords.get(0).getEmailNotificationContext().getContextName());
        assertEquals("context2", capturedRecords.get(1).getEmailNotificationContext().getContextName());
    }

    // ========== Delete Tests ==========

    @Test
    public void test_deleteByContextName_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        String contextName = "contextToDelete";

        // When
        service.deleteByContextName(contextName);

        // Then
        verify(dao, times(1)).deleteByContextName(contextName);
    }

    @Test
    public void test_deleteByContextName_with_empty_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        String contextName = "";

        // When
        service.deleteByContextName(contextName);

        // Then
        verify(dao, times(1)).deleteByContextName(contextName);
    }

    @Test
    public void test_deleteByContextName_with_null_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);

        // When
        service.deleteByContextName(null);

        // Then
        verify(dao, times(1)).deleteByContextName(null);
    }

    @Test
    public void test_deleteByContextName_with_special_characters_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        String contextName = "context!@#$%^&*()";

        // When
        service.deleteByContextName(contextName);

        // Then
        verify(dao, times(1)).deleteByContextName(contextName);
    }

    // ========== Integration Scenario Tests ==========

    @Test
    public void test_multiple_operations_should_invoke_dao_correctly() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        String contextName = "multiOpContext";
        SearchResults<EmailNotificationContextRecord> mockResults = createSearchResults(1, 1);
        when(dao.findByContextName(contextName, 10, 0)).thenReturn(mockResults);

        // When
        service.findByContextName(contextName, 10, 0);

        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName(contextName);
        service.saveEmailNotificationContext(context);

        service.deleteByContextName(contextName);

        // Then
        verify(dao, times(1)).findByContextName(contextName, 10, 0);
        verify(dao, times(1)).save(any(EmailNotificationContextRecord.class));
        verify(dao, times(1)).deleteByContextName(contextName);
    }

    @Test
    public void test_save_then_find_workflow() {
        // Given
        service = new HibernateEmailNotificationContextServiceImpl(dao);
        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName("workflowContext");

        SearchResults<EmailNotificationContextRecord> mockResults = createSearchResults(1, 1);
        when(dao.findByContextName("workflowContext", 10, 0)).thenReturn(mockResults);

        // When
        service.saveEmailNotificationContext(context);
        SearchResults<EmailNotificationContextRecord> results = service.findByContextName("workflowContext", 10, 0);

        // Then
        verify(dao, times(1)).save(any(EmailNotificationContextRecord.class));
        verify(dao, times(1)).findByContextName("workflowContext", 10, 0);
        assertNotNull(results);
    }

    // ========== Helper Methods ==========

    private EmailNotificationContextRecord createContextRecord(String contextName) {
        EmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();
        EmailNotificationContext context = new EmailNotificationContextImpl();
        context.setContextName(contextName);
        record.setEmailNotificationContext(context);
        record.setTimestamp(System.currentTimeMillis());
        return record;
    }

    private SearchResults<EmailNotificationContextRecord> createSearchResults(int resultCount, long totalCount) {
        List<EmailNotificationContextRecord> results = new ArrayList<>();
        for (int i = 0; i < resultCount; i++) {
            results.add(createContextRecord("context-" + i));
        }
        return new SearchResultsImpl<>(results, totalCount, 0L);
    }
}
