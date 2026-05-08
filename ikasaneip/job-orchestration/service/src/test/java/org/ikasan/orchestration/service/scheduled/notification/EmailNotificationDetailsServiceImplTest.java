package org.ikasan.orchestration.service.scheduled.notification;

import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailNotificationDetailsServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailNotificationDetailsServiceImplTest {

    @Mock
    private EmailNotificationDetailsDao mockDao;

    @Mock
    private EmailNotificationDetailsRecord mockRecord;

    @Mock
    private EmailNotificationDetails mockEmailNotificationDetails;

    private EmailNotificationDetailsServiceImpl service;

    @Before
    public void setUp() {
        service = new EmailNotificationDetailsServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new EmailNotificationDetailsServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        EmailNotificationDetailsServiceImpl testService = new EmailNotificationDetailsServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testFindAll() {
        // Given
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 100L);
        when(mockDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals(100L, results.getQueryResponseTime());
        verify(mockDao).findAll(10, 0);
    }

    @Test
    public void testFindAllWithMultipleResults() {
        // Given
        EmailNotificationDetailsRecord record2 = mock(EmailNotificationDetailsRecord.class);
        EmailNotificationDetailsRecord record3 = mock(EmailNotificationDetailsRecord.class);
        List<EmailNotificationDetailsRecord> records = Arrays.asList(mockRecord, record2, record3);
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(records, 10L, 150L);
        when(mockDao.findAll(20, 5)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findAll(20, 5);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(10L, results.getTotalNumberOfResults());
        assertEquals(150L, results.getQueryResponseTime());
        verify(mockDao).findAll(20, 5);
    }

    @Test
    public void testFindAllEmptyResults() {
        // Given
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockDao).findAll(10, 0);
    }

    @Test
    public void testFindByContextName() {
        // Given
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 100L);
        when(mockDao.findByContextName("test-context", 10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results =
            service.findByContextName("test-context", 10, 0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        verify(mockDao).findByContextName("test-context", 10, 0);
    }

    @Test
    public void testFindByContextNameWithMultipleResults() {
        // Given
        EmailNotificationDetailsRecord record2 = mock(EmailNotificationDetailsRecord.class);
        EmailNotificationDetailsRecord record3 = mock(EmailNotificationDetailsRecord.class);
        List<EmailNotificationDetailsRecord> records = Arrays.asList(mockRecord, record2, record3);
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(records, 15L, 200L);
        when(mockDao.findByContextName("large-context", 20, 10)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results =
            service.findByContextName("large-context", 20, 10);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(15L, results.getTotalNumberOfResults());
        verify(mockDao).findByContextName("large-context", 20, 10);
    }

    @Test
    public void testFindByContextNameEmptyResults() {
        // Given
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 30L);
        when(mockDao.findByContextName("empty-context", 10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results =
            service.findByContextName("empty-context", 10, 0);

        // Then
        assertNotNull(results);
        assertTrue(results.getResultList().isEmpty());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockDao).findByContextName("empty-context", 10, 0);
    }

    @Test
    public void testFindByJobNameAndMonitorType() {
        // Given
        when(mockDao.findByJobNameAndMonitorType("test-job", "child-ctx", "SUCCESS"))
            .thenReturn(mockRecord);

        // When
        EmailNotificationDetailsRecord result =
            service.findByJobNameAndMonitorType("test-job", "child-ctx", "SUCCESS");

        // Then
        assertNotNull(result);
        assertEquals(mockRecord, result);
        verify(mockDao).findByJobNameAndMonitorType("test-job", "child-ctx", "SUCCESS");
    }

    @Test
    public void testFindByJobNameAndMonitorTypeReturnsNull() {
        // Given
        when(mockDao.findByJobNameAndMonitorType("non-existent", "child-ctx", "ERROR"))
            .thenReturn(null);

        // When
        EmailNotificationDetailsRecord result =
            service.findByJobNameAndMonitorType("non-existent", "child-ctx", "ERROR");

        // Then
        assertNull(result);
        verify(mockDao).findByJobNameAndMonitorType("non-existent", "child-ctx", "ERROR");
    }

    @Test
    public void testFindByJobNameAndMonitorTypeWithDifferentParameters() {
        // Given
        EmailNotificationDetailsRecord record1 = mock(EmailNotificationDetailsRecord.class);
        EmailNotificationDetailsRecord record2 = mock(EmailNotificationDetailsRecord.class);
        EmailNotificationDetailsRecord record3 = mock(EmailNotificationDetailsRecord.class);

        when(mockDao.findByJobNameAndMonitorType("job-1", "ctx-1", "SUCCESS")).thenReturn(record1);
        when(mockDao.findByJobNameAndMonitorType("job-2", "ctx-2", "ERROR")).thenReturn(record2);
        when(mockDao.findByJobNameAndMonitorType("job-3", "ctx-3", "WARNING")).thenReturn(record3);

        // When
        EmailNotificationDetailsRecord result1 = service.findByJobNameAndMonitorType("job-1", "ctx-1", "SUCCESS");
        EmailNotificationDetailsRecord result2 = service.findByJobNameAndMonitorType("job-2", "ctx-2", "ERROR");
        EmailNotificationDetailsRecord result3 = service.findByJobNameAndMonitorType("job-3", "ctx-3", "WARNING");

        // Then
        assertEquals(record1, result1);
        assertEquals(record2, result2);
        assertEquals(record3, result3);
        verify(mockDao).findByJobNameAndMonitorType("job-1", "ctx-1", "SUCCESS");
        verify(mockDao).findByJobNameAndMonitorType("job-2", "ctx-2", "ERROR");
        verify(mockDao).findByJobNameAndMonitorType("job-3", "ctx-3", "WARNING");
    }

    @Test
    public void testSaveSingleRecord() {
        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void testSaveSingleRecordMultipleTimes() {
        // Given
        EmailNotificationDetailsRecord record2 = mock(EmailNotificationDetailsRecord.class);
        EmailNotificationDetailsRecord record3 = mock(EmailNotificationDetailsRecord.class);

        // When
        service.save(mockRecord);
        service.save(record2);
        service.save(record3);

        // Then
        verify(mockDao).save(mockRecord);
        verify(mockDao).save(record2);
        verify(mockDao).save(record3);
    }

    @Test
    public void testSaveMultipleRecords() {
        // Given
        EmailNotificationDetailsRecord record2 = mock(EmailNotificationDetailsRecord.class);
        EmailNotificationDetailsRecord record3 = mock(EmailNotificationDetailsRecord.class);
        List<EmailNotificationDetailsRecord> records = Arrays.asList(mockRecord, record2, record3);

        // When
        service.save(records);

        // Then
        verify(mockDao).save(records);
    }

    @Test
    public void testSaveEmptyList() {
        // Given
        List<EmailNotificationDetailsRecord> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
    }

    @Test
    public void testSaveEmailNotificationDetails() {
        // Given
        EmailNotificationDetails details2 = mock(EmailNotificationDetails.class);
        EmailNotificationDetails details3 = mock(EmailNotificationDetails.class);
        List<EmailNotificationDetails> detailsList = Arrays.asList(
            mockEmailNotificationDetails, details2, details3
        );

        // When
        service.saveEmailNotificationDetails(detailsList);

        // Then
        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordsCaptor =
            ArgumentCaptor.forClass(List.class);
        verify(mockDao).save(recordsCaptor.capture());

        List<EmailNotificationDetailsRecord> capturedRecords = recordsCaptor.getValue();
        assertNotNull(capturedRecords);
        assertEquals(3, capturedRecords.size());

        // Verify each record has the correct details and timestamp
        assertEquals(EmailNotificationDetailsImpl.class, capturedRecords.get(0).getEmailNotificationDetails().getClass());
        assertEquals(EmailNotificationDetailsImpl.class, capturedRecords.get(1).getEmailNotificationDetails().getClass());
        assertEquals(EmailNotificationDetailsImpl.class, capturedRecords.get(2).getEmailNotificationDetails().getClass());

        for (EmailNotificationDetailsRecord record : capturedRecords) {
            assertTrue(record.getTimestamp() > 0);
        }
    }

    @Test
    public void testSaveEmailNotificationDetailsWithEmptyList() {
        // Given
        List<EmailNotificationDetails> emptyList = new ArrayList<>();

        // When
        service.saveEmailNotificationDetails(emptyList);

        // Then
        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordsCaptor =
            ArgumentCaptor.forClass(List.class);
        verify(mockDao).save(recordsCaptor.capture());

        List<EmailNotificationDetailsRecord> capturedRecords = recordsCaptor.getValue();
        assertNotNull(capturedRecords);
        assertTrue(capturedRecords.isEmpty());
    }

    @Test
    public void testSaveEmailNotificationDetailsTimestampsAreSet() {
        // Given
        long beforeTimestamp = System.currentTimeMillis();
        List<EmailNotificationDetails> detailsList = Arrays.asList(mockEmailNotificationDetails);

        // When
        service.saveEmailNotificationDetails(detailsList);

        // Then
        long afterTimestamp = System.currentTimeMillis();
        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordsCaptor =
            ArgumentCaptor.forClass(List.class);
        verify(mockDao).save(recordsCaptor.capture());

        List<EmailNotificationDetailsRecord> capturedRecords = recordsCaptor.getValue();
        assertEquals(1, capturedRecords.size());

        EmailNotificationDetailsRecord record = capturedRecords.get(0);
        assertTrue(record.getTimestamp() >= beforeTimestamp);
        assertTrue(record.getTimestamp() <= afterTimestamp);
    }

    @Test
    public void testDeleteByContextName() {
        // When
        service.deleteByContextName("test-context");

        // Then
        verify(mockDao).deleteByContextName("test-context");
    }

    @Test
    public void testDeleteByContextNameMultipleTimes() {
        // When
        service.deleteByContextName("context-1");
        service.deleteByContextName("context-2");
        service.deleteByContextName("context-3");

        // Then
        verify(mockDao).deleteByContextName("context-1");
        verify(mockDao).deleteByContextName("context-2");
        verify(mockDao).deleteByContextName("context-3");
        verify(mockDao, times(3)).deleteByContextName(anyString());
    }

    @Test
    public void testDeleteByJobNameAndMonitorType() {
        // When
        service.deleteByJobNameAndMonitorType("test-job", "child-ctx", "SUCCESS");

        // Then
        verify(mockDao).deleteByJobNameAndMonitorType("test-job", "child-ctx", "SUCCESS");
    }

    @Test
    public void testDeleteByJobNameAndMonitorTypeMultipleTimes() {
        // When
        service.deleteByJobNameAndMonitorType("job-1", "ctx-1", "SUCCESS");
        service.deleteByJobNameAndMonitorType("job-2", "ctx-2", "ERROR");
        service.deleteByJobNameAndMonitorType("job-3", "ctx-3", "WARNING");

        // Then
        verify(mockDao).deleteByJobNameAndMonitorType("job-1", "ctx-1", "SUCCESS");
        verify(mockDao).deleteByJobNameAndMonitorType("job-2", "ctx-2", "ERROR");
        verify(mockDao).deleteByJobNameAndMonitorType("job-3", "ctx-3", "WARNING");
        verify(mockDao, times(3)).deleteByJobNameAndMonitorType(anyString(), anyString(), anyString());
    }

    @Test
    public void testFindAllWithLargeLimit() {
        // Given
        List<EmailNotificationDetailsRecord> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(EmailNotificationDetailsRecord.class));
        }
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(largeList, 500L, 300L);
        when(mockDao.findAll(100, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results = service.findAll(100, 0);

        // Then
        assertNotNull(results);
        assertEquals(100, results.getResultList().size());
        assertEquals(500L, results.getTotalNumberOfResults());
        assertEquals(300L, results.getQueryResponseTime());
        verify(mockDao).findAll(100, 0);
    }

    @Test
    public void testFindByContextNameWithLargeOffset() {
        // Given
        SearchResults<EmailNotificationDetailsRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1000L, 80L);
        when(mockDao.findByContextName("test-context", 10, 990)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationDetailsRecord> results =
            service.findByContextName("test-context", 10, 990);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1000L, results.getTotalNumberOfResults());
        verify(mockDao).findByContextName("test-context", 10, 990);
    }

    @Test
    public void testSaveEmailNotificationDetailsWithLargeList() {
        // Given
        List<EmailNotificationDetails> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add(mock(EmailNotificationDetails.class));
        }

        // When
        service.saveEmailNotificationDetails(largeList);

        // Then
        ArgumentCaptor<List<EmailNotificationDetailsRecord>> recordsCaptor =
            ArgumentCaptor.forClass(List.class);
        verify(mockDao).save(recordsCaptor.capture());

        List<EmailNotificationDetailsRecord> capturedRecords = recordsCaptor.getValue();
        assertEquals(50, capturedRecords.size());
    }
}
