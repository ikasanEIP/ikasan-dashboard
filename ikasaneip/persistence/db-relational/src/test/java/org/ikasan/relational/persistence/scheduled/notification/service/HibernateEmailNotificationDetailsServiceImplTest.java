package org.ikasan.relational.persistence.scheduled.notification.service;

import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateEmailNotificationDetailsDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationDetails;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationDetailsRecord;
import org.ikasan.relational.persistence.scheduled.notification.service.HibernateEmailNotificationDetailsServiceImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
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
public class HibernateEmailNotificationDetailsServiceImplTest {

    @Mock
    private HibernateEmailNotificationDetailsDaoImpl dao;

    private HibernateEmailNotificationDetailsServiceImpl service;

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_with_null_dao_should_throw_exception() {
        new HibernateEmailNotificationDetailsServiceImpl(null);
    }

    @Test
    public void test_constructor_with_valid_dao_should_create_instance() {
        // When
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);

        // Then
        assertNotNull(service);
    }

    @Test
    public void test_find_all_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        int limit = 10;
        int offset = 5;
        SearchResults<EmailNotificationDetailsRecord> expectedResults = mock(SearchResults.class);
        when(dao.findAll(limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findAll(limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao).findAll(limit, offset);
    }

    @Test
    public void test_find_all_with_zero_limit_and_offset_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        int limit = 0;
        int offset = 0;
        SearchResults<EmailNotificationDetailsRecord> expectedResults = mock(SearchResults.class);
        when(dao.findAll(limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findAll(limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao).findAll(limit, offset);
    }

    @Test
    public void test_find_by_context_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String contextName = "testContext";
        int limit = 20;
        int offset = 10;
        SearchResults<EmailNotificationDetailsRecord> expectedResults = mock(SearchResults.class);
        when(dao.findByContextName(contextName, limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findByContextName(contextName, limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao).findByContextName(contextName, limit, offset);
    }

    @Test
    public void test_find_by_context_name_with_empty_context_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String contextName = "";
        int limit = 10;
        int offset = 0;
        SearchResults<EmailNotificationDetailsRecord> expectedResults = mock(SearchResults.class);
        when(dao.findByContextName(contextName, limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findByContextName(contextName, limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao).findByContextName(contextName, limit, offset);
    }

    @Test
    public void test_find_by_job_name_and_monitor_type_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String jobName = "testJob";
        String childContextName = "childContext";
        String monitorType = "ON_SUCCESS";
        EmailNotificationDetailsRecord expectedRecord = new HibernateEmailNotificationDetailsRecord();
        when(dao.findByJobNameAndMonitorType(jobName, childContextName, monitorType)).thenReturn(expectedRecord);

        // When
        EmailNotificationDetailsRecord result = service.findByJobNameAndMonitorType(jobName, childContextName, monitorType);

        // Then
        assertSame(expectedRecord, result);
        verify(dao).findByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }

    @Test
    public void test_find_by_job_name_and_monitor_type_with_null_result_should_return_null() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String jobName = "nonExistentJob";
        String childContextName = "childContext";
        String monitorType = "ON_FAILURE";
        when(dao.findByJobNameAndMonitorType(jobName, childContextName, monitorType)).thenReturn(null);

        // When
        EmailNotificationDetailsRecord result = service.findByJobNameAndMonitorType(jobName, childContextName, monitorType);

        // Then
        assertNull(result);
        verify(dao).findByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }

    @Test
    public void test_save_with_single_record_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        EmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();
        EmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName("testJob");
        details.setContextName("testContext");
        details.setMonitorType("ON_SUCCESS");
        record.setEmailNotificationDetails(details);

        // When
        service.save(record);

        // Then
        verify(dao).save(record);
    }

    @Test
    public void test_save_with_null_record_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);

        // When
        service.save((EmailNotificationDetailsRecord) null);

        // Then
        verify(dao).save((EmailNotificationDetailsRecord) null);
    }

    @Test
    public void test_save_with_record_list_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        List<EmailNotificationDetailsRecord> records = Arrays.asList(
            createRecord("job1", "context1", "child1", "ON_SUCCESS"),
            createRecord("job2", "context2", "child2", "ON_FAILURE")
        );

        // When
        service.save(records);

        // Then
        verify(dao).save(records);
    }

    @Test
    public void test_save_with_empty_record_list_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        List<EmailNotificationDetailsRecord> records = Collections.emptyList();

        // When
        service.save(records);

        // Then
        verify(dao).save(records);
    }

    @Test
    public void test_save_email_notification_details_with_single_detail_should_create_record_and_save() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        EmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName("testJob");
        details.setContextName("testContext");
        details.setChildContextName("childContext");
        details.setMonitorType("ON_SUCCESS");
        details.setEmailSendTo(Arrays.asList("test@example.com"));

        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        service.saveEmailNotificationDetails(Collections.singletonList(details));

        // Then
        verify(dao).save(recordListCaptor.capture());
        List<EmailNotificationDetailsRecord> capturedRecords = recordListCaptor.getValue();

        assertEquals(1, capturedRecords.size());
        assertSame(details, capturedRecords.get(0).getEmailNotificationDetails());
        assertTrue(capturedRecords.get(0).getTimestamp() > 0);
    }

    @Test
    public void test_save_email_notification_details_with_multiple_details_should_create_records_and_save() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        EmailNotificationDetails details1 = createDetails("job1", "context1", "child1", "ON_SUCCESS");
        EmailNotificationDetails details2 = createDetails("job2", "context2", "child2", "ON_FAILURE");
        EmailNotificationDetails details3 = createDetails("job3", "context3", "child3", "ON_COMPLETION");

        List<EmailNotificationDetails> detailsList = Arrays.asList(details1, details2, details3);

        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        service.saveEmailNotificationDetails(detailsList);

        // Then
        verify(dao).save(recordListCaptor.capture());
        List<EmailNotificationDetailsRecord> capturedRecords = recordListCaptor.getValue();

        assertEquals(3, capturedRecords.size());
        assertEquals(details1, capturedRecords.get(0).getEmailNotificationDetails());
        assertEquals(details2, capturedRecords.get(1).getEmailNotificationDetails());
        assertEquals(details3, capturedRecords.get(2).getEmailNotificationDetails());
        assertTrue(capturedRecords.get(0).getTimestamp() > 0);
        assertTrue(capturedRecords.get(1).getTimestamp() > 0);
        assertTrue(capturedRecords.get(2).getTimestamp() > 0);
    }

    @Test
    public void test_save_email_notification_details_with_empty_list_should_save_empty_list() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        List<EmailNotificationDetails> emptyList = Collections.emptyList();

        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        service.saveEmailNotificationDetails(emptyList);

        // Then
        verify(dao).save(recordListCaptor.capture());
        List<EmailNotificationDetailsRecord> capturedRecords = recordListCaptor.getValue();

        assertEquals(0, capturedRecords.size());
    }

    @Test
    public void test_save_email_notification_details_with_complex_details_should_create_record_with_all_fields() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        EmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName("complexJob");
        details.setContextName("complexContext");
        details.setChildContextName("complexChild");
        details.setMonitorType("ON_SUCCESS");
        details.setEmailSendTo(Arrays.asList("user1@example.com", "user2@example.com"));
        details.setEmailSendCc(Arrays.asList("cc@example.com"));
        details.setEmailSendBcc(Arrays.asList("bcc@example.com"));
        details.setEmailSubject("Test Subject");
        details.setEmailBody("Test Body");
        details.setEmailSubjectTemplate("Subject: {{name}}");
        details.setEmailBodyTemplate("Body: {{message}}");
        details.setAttachment("/path/to/file");
        details.setHtml(true);

        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");
        details.setEmailNotificationTemplateParameters(params);

        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        service.saveEmailNotificationDetails(Collections.singletonList(details));

        // Then
        verify(dao).save(recordListCaptor.capture());
        List<EmailNotificationDetailsRecord> capturedRecords = recordListCaptor.getValue();

        assertEquals(1, capturedRecords.size());
        assertEquals(details, capturedRecords.get(0).getEmailNotificationDetails());
    }

    @Test
    public void test_save_email_notification_details_should_set_timestamp_automatically() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        EmailNotificationDetails details = createDetails("job", "context", "child", "ON_SUCCESS");
        long beforeSave = System.currentTimeMillis();

        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        service.saveEmailNotificationDetails(Collections.singletonList(details));

        long afterSave = System.currentTimeMillis();

        // Then
        verify(dao).save(recordListCaptor.capture());
        List<EmailNotificationDetailsRecord> capturedRecords = recordListCaptor.getValue();

        assertTrue(capturedRecords.get(0).getTimestamp() >= beforeSave);
        assertTrue(capturedRecords.get(0).getTimestamp() <= afterSave);
    }

    @Test
    public void test_delete_by_context_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String contextName = "contextToDelete";

        // When
        service.deleteByContextName(contextName);

        // Then
        verify(dao).deleteByContextName(contextName);
    }

    @Test
    public void test_delete_by_context_name_with_empty_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String contextName = "";

        // When
        service.deleteByContextName(contextName);

        // Then
        verify(dao).deleteByContextName(contextName);
    }

    @Test
    public void test_delete_by_context_name_with_null_name_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);

        // When
        service.deleteByContextName(null);

        // Then
        verify(dao).deleteByContextName(null);
    }

    @Test
    public void test_delete_by_job_name_and_monitor_type_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String jobName = "jobToDelete";
        String childContextName = "childToDelete";
        String monitorType = "ON_FAILURE";

        // When
        service.deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);

        // Then
        verify(dao).deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }

    @Test
    public void test_delete_by_job_name_and_monitor_type_with_empty_values_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);

        // When
        service.deleteByJobNameAndMonitorType("", "", "");

        // Then
        verify(dao).deleteByJobNameAndMonitorType("", "", "");
    }

    @Test
    public void test_delete_by_job_name_and_monitor_type_with_null_values_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);

        // When
        service.deleteByJobNameAndMonitorType(null, null, null);

        // Then
        verify(dao).deleteByJobNameAndMonitorType(null, null, null);
    }

    @Test
    public void test_multiple_operations_should_invoke_dao_correctly() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String contextName = "multiOpContext";
        String jobName = "multiOpJob";
        String childContextName = "multiOpChild";
        String monitorType = "ON_SUCCESS";

        SearchResults<EmailNotificationDetailsRecord> mockResults = mock(SearchResults.class);
        when(dao.findByContextName(contextName, 10, 0)).thenReturn(mockResults);

        EmailNotificationDetailsRecord mockRecord = new HibernateEmailNotificationDetailsRecord();
        when(dao.findByJobNameAndMonitorType(jobName, childContextName, monitorType)).thenReturn(mockRecord);

        // When
        service.findByContextName(contextName, 10, 0);
        service.findByJobNameAndMonitorType(jobName, childContextName, monitorType);

        EmailNotificationDetails details = createDetails(jobName, contextName, childContextName, monitorType);
        service.saveEmailNotificationDetails(Collections.singletonList(details));

        service.deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);
        service.deleteByContextName(contextName);

        // Then
        verify(dao).findByContextName(contextName, 10, 0);
        verify(dao).findByJobNameAndMonitorType(jobName, childContextName, monitorType);
        verify(dao).save(anyList());
        verify(dao).deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);
        verify(dao).deleteByContextName(contextName);
    }

    @Test
    public void test_find_all_with_large_offset_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        int limit = 100;
        int offset = 1000;
        SearchResults<EmailNotificationDetailsRecord> expectedResults = mock(SearchResults.class);
        when(dao.findAll(limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findAll(limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao).findAll(limit, offset);
    }

    @Test
    public void test_save_with_multiple_consecutive_single_record_calls_should_invoke_dao_multiple_times() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        EmailNotificationDetailsRecord record1 = createRecord("job1", "context1", "child1", "ON_SUCCESS");
        EmailNotificationDetailsRecord record2 = createRecord("job2", "context2", "child2", "ON_FAILURE");

        // When
        service.save(record1);
        service.save(record2);

        // Then
        verify(dao, times(2)).save(any(EmailNotificationDetailsRecord.class));
    }

    @Test
    public void test_save_email_notification_details_with_duplicate_details_should_create_multiple_records() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        EmailNotificationDetails details = createDetails("job", "context", "child", "ON_SUCCESS");
        List<EmailNotificationDetails> detailsList = Arrays.asList(details, details, details);

        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        service.saveEmailNotificationDetails(detailsList);

        // Then
        verify(dao).save(recordListCaptor.capture());
        List<EmailNotificationDetailsRecord> capturedRecords = recordListCaptor.getValue();

        assertEquals(3, capturedRecords.size());
    }

    @Test
    public void test_find_by_context_name_with_special_characters_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String contextName = "test-context_123!@#";
        int limit = 10;
        int offset = 0;
        SearchResults<EmailNotificationDetailsRecord> expectedResults = mock(SearchResults.class);
        when(dao.findByContextName(contextName, limit, offset)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findByContextName(contextName, limit, offset);

        // Then
        assertSame(expectedResults, results);
        verify(dao).findByContextName(contextName, limit, offset);
    }

    @Test
    public void test_find_by_job_name_and_monitor_type_with_special_characters_should_delegate_to_dao() {
        // Given
        service = new HibernateEmailNotificationDetailsServiceImpl(dao);
        String jobName = "job!@#";
        String childContextName = "child$%^";
        String monitorType = "TYPE_&*()";
        EmailNotificationDetailsRecord expectedRecord = new HibernateEmailNotificationDetailsRecord();
        when(dao.findByJobNameAndMonitorType(jobName, childContextName, monitorType)).thenReturn(expectedRecord);

        // When
        EmailNotificationDetailsRecord result = service.findByJobNameAndMonitorType(jobName, childContextName, monitorType);

        // Then
        assertSame(expectedRecord, result);
        verify(dao).findByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }

    // Helper methods
    private EmailNotificationDetailsRecord createRecord(String jobName, String contextName, String childContextName, String monitorType) {
        EmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();
        EmailNotificationDetails details = createDetails(jobName, contextName, childContextName, monitorType);
        record.setEmailNotificationDetails(details);
        record.setTimestamp(System.currentTimeMillis());
        return record;
    }

    private EmailNotificationDetails createDetails(String jobName, String contextName, String childContextName, String monitorType) {
        EmailNotificationDetails details = new HibernateEmailNotificationDetails();
        details.setJobName(jobName);
        details.setContextName(contextName);
        details.setChildContextName(childContextName);
        details.setMonitorType(monitorType);
        return details;
    }
}
