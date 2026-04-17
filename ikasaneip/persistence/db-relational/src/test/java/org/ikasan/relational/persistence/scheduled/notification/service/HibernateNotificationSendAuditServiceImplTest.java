package org.ikasan.relational.persistence.scheduled.notification.service;

import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateNotificationSendAuditDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateNotificationSendAudit;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateNotificationSendAuditRecord;
import org.ikasan.relational.persistence.scheduled.notification.service.HibernateNotificationSendAuditServiceImpl;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HibernateNotificationSendAuditServiceImplTest {

    @Mock
    private HibernateNotificationSendAuditDaoImpl dao;

    private HibernateNotificationSendAuditServiceImpl service;

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_with_null_dao_should_throw_exception() {
        new HibernateNotificationSendAuditServiceImpl(null);
    }

    @Test
    public void test_constructor_with_valid_dao_should_create_instance() {
        // When
        service = new HibernateNotificationSendAuditServiceImpl(dao);

        // Then
        assertNotNull(service);
    }

    @Test
    public void test_find_with_valid_parameters_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "instance-123";
        String contextName = "testContext";
        String jobName = "testJob";
        String monitorType = "ON_SUCCESS";
        String notifierType = "EMAIL";

        NotificationSendAuditRecord expectedRecord = new HibernateNotificationSendAuditRecord();
        when(dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType)).thenReturn(expectedRecord);

        // When
        NotificationSendAuditRecord result = service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        // Then
        assertSame(expectedRecord, result);
        verify(dao).find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    @Test
    public void test_find_with_non_existent_record_should_return_null() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "nonExistent";
        String contextName = "context";
        String jobName = "job";
        String monitorType = "ON_FAILURE";
        String notifierType = "SMS";

        when(dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType)).thenReturn(null);

        // When
        NotificationSendAuditRecord result = service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        // Then
        assertNull(result);
        verify(dao).find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    @Test
    public void test_find_with_empty_strings_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "";
        String contextName = "";
        String jobName = "";
        String monitorType = "";
        String notifierType = "";

        when(dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType)).thenReturn(null);

        // When
        NotificationSendAuditRecord result = service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        // Then
        assertNull(result);
        verify(dao).find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    @Test
    public void test_find_with_null_parameters_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);

        // When
        NotificationSendAuditRecord result = service.find(null, null, null, null, null);

        // Then
        verify(dao).find(null, null, null, null, null);
    }

    @Test
    public void test_find_with_special_characters_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "instance!@#123";
        String contextName = "context-name_test";
        String jobName = "job$%^";
        String monitorType = "TYPE&*()";
        String notifierType = "NOTIFIER_123";

        NotificationSendAuditRecord expectedRecord = new HibernateNotificationSendAuditRecord();
        when(dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType)).thenReturn(expectedRecord);

        // When
        NotificationSendAuditRecord result = service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        // Then
        assertSame(expectedRecord, result);
        verify(dao).find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    @Test
    public void test_save_with_valid_record_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        NotificationSendAuditRecord record = new HibernateNotificationSendAuditRecord();
        HibernateNotificationSendAudit audit = new HibernateNotificationSendAudit();
        audit.setContextInstanceId("instance-123");
        audit.setContextName("testContext");
        audit.setJobName("testJob");
        audit.setMonitorType("ON_SUCCESS");
        audit.setNotifierType("EMAIL");
        audit.setNotificationSend(true);
        record.setNotificationSendAudit(audit);

        // When
        service.save(record);

        // Then
        verify(dao).save(record);
    }

    @Test
    public void test_save_with_null_record_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);

        // When
        service.save(null);

        // Then
        verify(dao).save(null);
    }

    @Test
    public void test_save_with_notification_send_false_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        NotificationSendAuditRecord record = createRecord("instance-1", "context1", "job1", "ON_FAILURE", "EMAIL", false);

        // When
        service.save(record);

        // Then
        verify(dao).save(record);
    }

    @Test
    public void test_save_with_notification_send_true_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        NotificationSendAuditRecord record = createRecord("instance-2", "context2", "job2", "ON_SUCCESS", "SMS", true);

        // When
        service.save(record);

        // Then
        verify(dao).save(record);
    }

    @Test
    public void test_save_with_minimal_audit_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        NotificationSendAuditRecord record = new HibernateNotificationSendAuditRecord();
        HibernateNotificationSendAudit audit = new HibernateNotificationSendAudit();
        audit.setContextInstanceId("minimal-instance");
        audit.setContextName("minimal");
        audit.setJobName("minimal");
        audit.setMonitorType("minimal");
        audit.setNotifierType("minimal");
        record.setNotificationSendAudit(audit);

        // When
        service.save(record);

        // Then
        verify(dao).save(record);
    }

    @Test
    public void test_multiple_operations_should_invoke_dao_correctly() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "instance-multi";
        String contextName = "multiContext";
        String jobName = "multiJob";
        String monitorType = "ON_SUCCESS";
        String notifierType = "EMAIL";

        NotificationSendAuditRecord mockRecord = createRecord(contextInstanceId, contextName, jobName, monitorType, notifierType, true);
        when(dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType)).thenReturn(mockRecord);

        // When
        service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        NotificationSendAuditRecord newRecord = createRecord(contextInstanceId, contextName, jobName, monitorType, notifierType, false);
        service.save(newRecord);

        service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        // Then
        verify(dao, times(2)).find(contextInstanceId, contextName, jobName, monitorType, notifierType);
        verify(dao).save(newRecord);
    }

    @Test
    public void test_save_multiple_consecutive_calls_should_invoke_dao_multiple_times() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        NotificationSendAuditRecord record1 = createRecord("instance-1", "context1", "job1", "TYPE1", "NOTIFIER1", true);
        NotificationSendAuditRecord record2 = createRecord("instance-2", "context2", "job2", "TYPE2", "NOTIFIER2", false);
        NotificationSendAuditRecord record3 = createRecord("instance-3", "context3", "job3", "TYPE3", "NOTIFIER3", true);

        // When
        service.save(record1);
        service.save(record2);
        service.save(record3);

        // Then
        verify(dao, times(3)).save(any(NotificationSendAuditRecord.class));
        verify(dao).save(record1);
        verify(dao).save(record2);
        verify(dao).save(record3);
    }

    @Test
    public void test_find_multiple_different_records_should_return_different_results() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        NotificationSendAuditRecord record1 = createRecord("instance-1", "context1", "job1", "TYPE1", "NOTIFIER1", true);
        NotificationSendAuditRecord record2 = createRecord("instance-2", "context2", "job2", "TYPE2", "NOTIFIER2", false);

        when(dao.find("instance-1", "context1", "job1", "TYPE1", "NOTIFIER1")).thenReturn(record1);
        when(dao.find("instance-2", "context2", "job2", "TYPE2", "NOTIFIER2")).thenReturn(record2);

        // When
        NotificationSendAuditRecord result1 = service.find("instance-1", "context1", "job1", "TYPE1", "NOTIFIER1");
        NotificationSendAuditRecord result2 = service.find("instance-2", "context2", "job2", "TYPE2", "NOTIFIER2");

        // Then
        assertSame(record1, result1);
        assertSame(record2, result2);
        assertNotSame(result1, result2);
    }

    @Test
    public void test_find_with_long_strings_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "very-long-context-instance-id-" + "x".repeat(100);
        String contextName = "very-long-context-name-" + "y".repeat(100);
        String jobName = "very-long-job-name-" + "z".repeat(100);
        String monitorType = "LONG_MONITOR_TYPE";
        String notifierType = "LONG_NOTIFIER_TYPE";

        NotificationSendAuditRecord expectedRecord = new HibernateNotificationSendAuditRecord();
        when(dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType)).thenReturn(expectedRecord);

        // When
        NotificationSendAuditRecord result = service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        // Then
        assertSame(expectedRecord, result);
        verify(dao).find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    @Test
    public void test_save_with_long_strings_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "very-long-instance-" + "a".repeat(100);
        String contextName = "very-long-context-" + "b".repeat(100);
        String jobName = "very-long-job-" + "c".repeat(100);

        NotificationSendAuditRecord record = createRecord(contextInstanceId, contextName, jobName, "MONITOR", "NOTIFIER", true);

        // When
        service.save(record);

        // Then
        verify(dao).save(record);
    }

    @Test
    public void test_find_with_different_monitor_types_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String[] monitorTypes = {"ON_SUCCESS", "ON_FAILURE", "ON_COMPLETION", "ON_START", "ON_ERROR"};

        for (String monitorType : monitorTypes) {
            NotificationSendAuditRecord mockRecord = createRecord("instance", "context", "job", monitorType, "EMAIL", true);
            when(dao.find("instance", "context", "job", monitorType, "EMAIL")).thenReturn(mockRecord);
        }

        // When & Then
        for (String monitorType : monitorTypes) {
            NotificationSendAuditRecord result = service.find("instance", "context", "job", monitorType, "EMAIL");
            assertNotNull(result);
            verify(dao).find("instance", "context", "job", monitorType, "EMAIL");
        }
    }

    @Test
    public void test_find_with_different_notifier_types_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String[] notifierTypes = {"EMAIL", "SMS", "SLACK", "WEBHOOK", "PUSH"};

        for (String notifierType : notifierTypes) {
            NotificationSendAuditRecord mockRecord = createRecord("instance", "context", "job", "ON_SUCCESS", notifierType, true);
            when(dao.find("instance", "context", "job", "ON_SUCCESS", notifierType)).thenReturn(mockRecord);
        }

        // When & Then
        for (String notifierType : notifierTypes) {
            NotificationSendAuditRecord result = service.find("instance", "context", "job", "ON_SUCCESS", notifierType);
            assertNotNull(result);
            verify(dao).find("instance", "context", "job", "ON_SUCCESS", notifierType);
        }
    }

    @Test
    public void test_save_with_same_composite_key_different_status_should_delegate_to_dao() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "instance-same";
        String contextName = "context-same";
        String jobName = "job-same";
        String monitorType = "TYPE-same";
        String notifierType = "NOTIFIER-same";

        NotificationSendAuditRecord recordTrue = createRecord(contextInstanceId, contextName, jobName, monitorType, notifierType, true);
        NotificationSendAuditRecord recordFalse = createRecord(contextInstanceId, contextName, jobName, monitorType, notifierType, false);

        // When
        service.save(recordTrue);
        service.save(recordFalse);

        // Then
        verify(dao).save(recordTrue);
        verify(dao).save(recordFalse);
    }

    @Test
    public void test_find_consecutive_calls_with_same_parameters_should_invoke_dao_each_time() {
        // Given
        service = new HibernateNotificationSendAuditServiceImpl(dao);
        String contextInstanceId = "instance-repeat";
        String contextName = "context-repeat";
        String jobName = "job-repeat";
        String monitorType = "TYPE-repeat";
        String notifierType = "NOTIFIER-repeat";

        NotificationSendAuditRecord mockRecord = createRecord(contextInstanceId, contextName, jobName, monitorType, notifierType, true);
        when(dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType)).thenReturn(mockRecord);

        // When
        service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);
        service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);
        service.find(contextInstanceId, contextName, jobName, monitorType, notifierType);

        // Then
        verify(dao, times(3)).find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    // Helper method
    private NotificationSendAuditRecord createRecord(String contextInstanceId, String contextName, String jobName,
                                                     String monitorType, String notifierType, boolean isNotificationSend) {
        NotificationSendAuditRecord record = new HibernateNotificationSendAuditRecord();
        HibernateNotificationSendAudit audit = new HibernateNotificationSendAudit();
        audit.setContextInstanceId(contextInstanceId);
        audit.setContextName(contextName);
        audit.setJobName(jobName);
        audit.setMonitorType(monitorType);
        audit.setNotifierType(notifierType);
        audit.setNotificationSend(isNotificationSend);
        record.setNotificationSendAudit(audit);
        return record;
    }
}
