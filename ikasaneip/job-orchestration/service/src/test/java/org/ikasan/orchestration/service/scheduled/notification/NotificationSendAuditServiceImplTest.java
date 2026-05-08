package org.ikasan.orchestration.service.scheduled.notification;

import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationSendAuditServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationSendAuditServiceImplTest {

    @Mock
    private NotificationSendAuditDao<NotificationSendAuditRecord> mockDao;

    @Mock
    private NotificationSendAuditRecord mockAuditRecord;

    private NotificationSendAuditServiceImpl service;

    @Before
    public void setUp() {
        service = new NotificationSendAuditServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new NotificationSendAuditServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        NotificationSendAuditServiceImpl testService = new NotificationSendAuditServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testFind() {
        // Given
        when(mockDao.find("ctx-instance-1", "test-context", "test-job", "SUCCESS", "EMAIL"))
            .thenReturn(mockAuditRecord);

        // When
        NotificationSendAuditRecord result = service.find(
            "ctx-instance-1", "test-context", "test-job", "SUCCESS", "EMAIL"
        );

        // Then
        assertNotNull(result);
        assertEquals(mockAuditRecord, result);
        verify(mockDao).find("ctx-instance-1", "test-context", "test-job", "SUCCESS", "EMAIL");
    }

    @Test
    public void testFindReturnsNull() {
        // Given
        when(mockDao.find("non-existent", "context", "job", "ERROR", "SMS"))
            .thenReturn(null);

        // When
        NotificationSendAuditRecord result = service.find(
            "non-existent", "context", "job", "ERROR", "SMS"
        );

        // Then
        assertNull(result);
        verify(mockDao).find("non-existent", "context", "job", "ERROR", "SMS");
    }

    @Test
    public void testFindWithDifferentParameters() {
        // Given
        NotificationSendAuditRecord record1 = mock(NotificationSendAuditRecord.class);
        NotificationSendAuditRecord record2 = mock(NotificationSendAuditRecord.class);
        NotificationSendAuditRecord record3 = mock(NotificationSendAuditRecord.class);

        when(mockDao.find("ctx-1", "context-1", "job-1", "SUCCESS", "EMAIL")).thenReturn(record1);
        when(mockDao.find("ctx-2", "context-2", "job-2", "ERROR", "SMS")).thenReturn(record2);
        when(mockDao.find("ctx-3", "context-3", "job-3", "WARNING", "SLACK")).thenReturn(record3);

        // When
        NotificationSendAuditRecord result1 = service.find("ctx-1", "context-1", "job-1", "SUCCESS", "EMAIL");
        NotificationSendAuditRecord result2 = service.find("ctx-2", "context-2", "job-2", "ERROR", "SMS");
        NotificationSendAuditRecord result3 = service.find("ctx-3", "context-3", "job-3", "WARNING", "SLACK");

        // Then
        assertEquals(record1, result1);
        assertEquals(record2, result2);
        assertEquals(record3, result3);
        verify(mockDao).find("ctx-1", "context-1", "job-1", "SUCCESS", "EMAIL");
        verify(mockDao).find("ctx-2", "context-2", "job-2", "ERROR", "SMS");
        verify(mockDao).find("ctx-3", "context-3", "job-3", "WARNING", "SLACK");
    }

    @Test
    public void testFindWithNullContextInstanceId() {
        // Given
        when(mockDao.find(null, "test-context", "test-job", "SUCCESS", "EMAIL"))
            .thenReturn(mockAuditRecord);

        // When
        NotificationSendAuditRecord result = service.find(
            null, "test-context", "test-job", "SUCCESS", "EMAIL"
        );

        // Then
        assertNotNull(result);
        assertEquals(mockAuditRecord, result);
        verify(mockDao).find(null, "test-context", "test-job", "SUCCESS", "EMAIL");
    }

    @Test
    public void testFindWithAllNullParameters() {
        // Given
        when(mockDao.find(null, null, null, null, null))
            .thenReturn(null);

        // When
        NotificationSendAuditRecord result = service.find(null, null, null, null, null);

        // Then
        assertNull(result);
        verify(mockDao).find(null, null, null, null, null);
    }

    @Test
    public void testFindMultipleTimes() {
        // Given
        when(mockDao.find("ctx-1", "context", "job", "SUCCESS", "EMAIL"))
            .thenReturn(mockAuditRecord);

        // When
        NotificationSendAuditRecord result1 = service.find("ctx-1", "context", "job", "SUCCESS", "EMAIL");
        NotificationSendAuditRecord result2 = service.find("ctx-1", "context", "job", "SUCCESS", "EMAIL");
        NotificationSendAuditRecord result3 = service.find("ctx-1", "context", "job", "SUCCESS", "EMAIL");

        // Then
        assertEquals(mockAuditRecord, result1);
        assertEquals(mockAuditRecord, result2);
        assertEquals(mockAuditRecord, result3);
        verify(mockDao, times(3)).find("ctx-1", "context", "job", "SUCCESS", "EMAIL");
    }

    @Test
    public void testSave() {
        // When
        service.save(mockAuditRecord);

        // Then
        verify(mockDao).save(mockAuditRecord);
    }

    @Test
    public void testSaveMultipleTimes() {
        // Given
        NotificationSendAuditRecord record2 = mock(NotificationSendAuditRecord.class);
        NotificationSendAuditRecord record3 = mock(NotificationSendAuditRecord.class);

        // When
        service.save(mockAuditRecord);
        service.save(record2);
        service.save(record3);

        // Then
        verify(mockDao).save(mockAuditRecord);
        verify(mockDao).save(record2);
        verify(mockDao).save(record3);
        verify(mockDao, times(3)).save(any(NotificationSendAuditRecord.class));
    }

    @Test
    public void testSaveAndFindInteraction() {
        // Given
        when(mockDao.find("ctx-1", "context-1", "job-1", "SUCCESS", "EMAIL"))
            .thenReturn(mockAuditRecord);

        // When - save first
        service.save(mockAuditRecord);

        // Then - verify save was called
        verify(mockDao).save(mockAuditRecord);

        // When - find the saved record
        NotificationSendAuditRecord result = service.find("ctx-1", "context-1", "job-1", "SUCCESS", "EMAIL");

        // Then - verify find was called and returned the record
        assertEquals(mockAuditRecord, result);
        verify(mockDao).find("ctx-1", "context-1", "job-1", "SUCCESS", "EMAIL");
    }

    @Test
    public void testFindWithDifferentMonitorTypes() {
        // Given
        NotificationSendAuditRecord successRecord = mock(NotificationSendAuditRecord.class);
        NotificationSendAuditRecord errorRecord = mock(NotificationSendAuditRecord.class);
        NotificationSendAuditRecord warningRecord = mock(NotificationSendAuditRecord.class);

        when(mockDao.find("ctx-1", "context", "job", "SUCCESS", "EMAIL")).thenReturn(successRecord);
        when(mockDao.find("ctx-1", "context", "job", "ERROR", "EMAIL")).thenReturn(errorRecord);
        when(mockDao.find("ctx-1", "context", "job", "WARNING", "EMAIL")).thenReturn(warningRecord);

        // When
        NotificationSendAuditRecord result1 = service.find("ctx-1", "context", "job", "SUCCESS", "EMAIL");
        NotificationSendAuditRecord result2 = service.find("ctx-1", "context", "job", "ERROR", "EMAIL");
        NotificationSendAuditRecord result3 = service.find("ctx-1", "context", "job", "WARNING", "EMAIL");

        // Then
        assertEquals(successRecord, result1);
        assertEquals(errorRecord, result2);
        assertEquals(warningRecord, result3);
    }

    @Test
    public void testFindWithDifferentNotifierTypes() {
        // Given
        NotificationSendAuditRecord emailRecord = mock(NotificationSendAuditRecord.class);
        NotificationSendAuditRecord smsRecord = mock(NotificationSendAuditRecord.class);
        NotificationSendAuditRecord slackRecord = mock(NotificationSendAuditRecord.class);

        when(mockDao.find("ctx-1", "context", "job", "SUCCESS", "EMAIL")).thenReturn(emailRecord);
        when(mockDao.find("ctx-1", "context", "job", "SUCCESS", "SMS")).thenReturn(smsRecord);
        when(mockDao.find("ctx-1", "context", "job", "SUCCESS", "SLACK")).thenReturn(slackRecord);

        // When
        NotificationSendAuditRecord result1 = service.find("ctx-1", "context", "job", "SUCCESS", "EMAIL");
        NotificationSendAuditRecord result2 = service.find("ctx-1", "context", "job", "SUCCESS", "SMS");
        NotificationSendAuditRecord result3 = service.find("ctx-1", "context", "job", "SUCCESS", "SLACK");

        // Then
        assertEquals(emailRecord, result1);
        assertEquals(smsRecord, result2);
        assertEquals(slackRecord, result3);
    }

    @Test
    public void testFindWithEmptyStrings() {
        // Given
        when(mockDao.find("", "", "", "", ""))
            .thenReturn(mockAuditRecord);

        // When
        NotificationSendAuditRecord result = service.find("", "", "", "", "");

        // Then
        assertEquals(mockAuditRecord, result);
        verify(mockDao).find("", "", "", "", "");
    }

    @Test
    public void testSaveNullRecord() {
        // When
        service.save(null);

        // Then
        verify(mockDao).save(null);
    }
}
