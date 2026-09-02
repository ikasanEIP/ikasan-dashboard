package org.ikasan.orchestration.service.scheduled.notification;

import org.ikasan.job.orchestration.model.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
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
 * Unit tests for EmailNotificationContextServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailNotificationContextServiceImplTest {

    @Mock
    private EmailNotificationContextDao mockDao;

    @Mock
    private EmailNotificationContextRecord mockRecord;

    @Mock
    private EmailNotificationContext mockEmailNotificationContext;

    private EmailNotificationContextServiceImpl service;

    @Before
    public void setUp() {
        service = new EmailNotificationContextServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new EmailNotificationContextServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        EmailNotificationContextServiceImpl testService = new EmailNotificationContextServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testFindAll() {
        // Given
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 100L);
        when(mockDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(10, 0);

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
        EmailNotificationContextRecord record2 = mock(EmailNotificationContextRecord.class);
        EmailNotificationContextRecord record3 = mock(EmailNotificationContextRecord.class);
        List<EmailNotificationContextRecord> records = Arrays.asList(mockRecord, record2, record3);
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 10L, 150L);
        when(mockDao.findAll(20, 5)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(20, 5);

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
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockDao).findAll(10, 0);
    }

    @Test
    public void testFindAllWithZeroLimitAndOffset() {
        // Given
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 20L);
        when(mockDao.findAll(0, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(0, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        verify(mockDao).findAll(0, 0);
    }

    @Test
    public void testFindAllWithLargeLimit() {
        // Given
        List<EmailNotificationContextRecord> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(EmailNotificationContextRecord.class));
        }
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(largeList, 500L, 300L);
        when(mockDao.findAll(100, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(100, 0);

        // Then
        assertNotNull(results);
        assertEquals(100, results.getResultList().size());
        assertEquals(500L, results.getTotalNumberOfResults());
        assertEquals(300L, results.getQueryResponseTime());
        verify(mockDao).findAll(100, 0);
    }

    @Test
    public void testFindByContextName() {
        // Given
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 100L);
        when(mockDao.findByContextName("test-context", 10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results =
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
        EmailNotificationContextRecord record2 = mock(EmailNotificationContextRecord.class);
        EmailNotificationContextRecord record3 = mock(EmailNotificationContextRecord.class);
        List<EmailNotificationContextRecord> records = Arrays.asList(mockRecord, record2, record3);
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 15L, 200L);
        when(mockDao.findByContextName("large-context", 20, 10)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results =
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
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 30L);
        when(mockDao.findByContextName("empty-context", 10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results =
            service.findByContextName("empty-context", 10, 0);

        // Then
        assertNotNull(results);
        assertTrue(results.getResultList().isEmpty());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockDao).findByContextName("empty-context", 10, 0);
    }

    @Test
    public void testFindByContextNameWithDifferentContexts() {
        // Given
        EmailNotificationContextRecord record1 = mock(EmailNotificationContextRecord.class);
        EmailNotificationContextRecord record2 = mock(EmailNotificationContextRecord.class);
        EmailNotificationContextRecord record3 = mock(EmailNotificationContextRecord.class);

        SearchResults<EmailNotificationContextRecord> results1 =
            new SearchResultsImpl<>(Arrays.asList(record1), 1L, 50L);
        SearchResults<EmailNotificationContextRecord> results2 =
            new SearchResultsImpl<>(Arrays.asList(record2), 1L, 60L);
        SearchResults<EmailNotificationContextRecord> results3 =
            new SearchResultsImpl<>(Arrays.asList(record3), 1L, 70L);

        when(mockDao.findByContextName("context-1", 10, 0)).thenReturn(results1);
        when(mockDao.findByContextName("context-2", 10, 0)).thenReturn(results2);
        when(mockDao.findByContextName("context-3", 10, 0)).thenReturn(results3);

        // When
        SearchResults<EmailNotificationContextRecord> result1 = service.findByContextName("context-1", 10, 0);
        SearchResults<EmailNotificationContextRecord> result2 = service.findByContextName("context-2", 10, 0);
        SearchResults<EmailNotificationContextRecord> result3 = service.findByContextName("context-3", 10, 0);

        // Then
        assertEquals(record1, result1.getResultList().get(0));
        assertEquals(record2, result2.getResultList().get(0));
        assertEquals(record3, result3.getResultList().get(0));
        verify(mockDao).findByContextName("context-1", 10, 0);
        verify(mockDao).findByContextName("context-2", 10, 0);
        verify(mockDao).findByContextName("context-3", 10, 0);
    }

    @Test
    public void testSave() {
        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void testSaveMultipleTimes() {
        // Given
        EmailNotificationContextRecord record2 = mock(EmailNotificationContextRecord.class);
        EmailNotificationContextRecord record3 = mock(EmailNotificationContextRecord.class);

        // When
        service.save(mockRecord);
        service.save(record2);
        service.save(record3);

        // Then
        verify(mockDao).save(mockRecord);
        verify(mockDao).save(record2);
        verify(mockDao).save(record3);
        verify(mockDao, times(3)).save(any(EmailNotificationContextRecord.class));
    }

    @Test
    public void testSaveEmailNotificationContext() {
        // When
        service.saveEmailNotificationContext(mockEmailNotificationContext);

        // Then
        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor =
            ArgumentCaptor.forClass(EmailNotificationContextRecord.class);
        verify(mockDao).save(recordCaptor.capture());

        EmailNotificationContextRecord capturedRecord = recordCaptor.getValue();
        assertNotNull(capturedRecord);
        assertEquals(mockEmailNotificationContext, capturedRecord.getEmailNotificationContext());
        assertTrue(capturedRecord.getTimestamp() > 0);
    }

    @Test
    public void testSaveEmailNotificationContextMultipleTimes() {
        // Given
        EmailNotificationContext context2 = mock(EmailNotificationContext.class);
        EmailNotificationContext context3 = mock(EmailNotificationContext.class);

        // When
        service.saveEmailNotificationContext(mockEmailNotificationContext);
        service.saveEmailNotificationContext(context2);
        service.saveEmailNotificationContext(context3);

        // Then
        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor =
            ArgumentCaptor.forClass(EmailNotificationContextRecord.class);
        verify(mockDao, times(3)).save(recordCaptor.capture());

        List<EmailNotificationContextRecord> capturedRecords = recordCaptor.getAllValues();
        assertEquals(3, capturedRecords.size());
        assertEquals(mockEmailNotificationContext, capturedRecords.get(0).getEmailNotificationContext());
        assertEquals(context2, capturedRecords.get(1).getEmailNotificationContext());
        assertEquals(context3, capturedRecords.get(2).getEmailNotificationContext());
    }

    @Test
    public void testSaveEmailNotificationContextTimestampIsSet() {
        // Given
        long beforeTimestamp = System.currentTimeMillis();

        // When
        service.saveEmailNotificationContext(mockEmailNotificationContext);

        // Then
        long afterTimestamp = System.currentTimeMillis();
        ArgumentCaptor<EmailNotificationContextRecord> recordCaptor =
            ArgumentCaptor.forClass(EmailNotificationContextRecord.class);
        verify(mockDao).save(recordCaptor.capture());

        EmailNotificationContextRecord capturedRecord = recordCaptor.getValue();
        assertTrue(capturedRecord.getTimestamp() >= beforeTimestamp);
        assertTrue(capturedRecord.getTimestamp() <= afterTimestamp);
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
    public void testFindByContextNameWithLargeOffset() {
        // Given
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1000L, 80L);
        when(mockDao.findByContextName("test-context", 10, 990)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results =
            service.findByContextName("test-context", 10, 990);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1000L, results.getTotalNumberOfResults());
        verify(mockDao).findByContextName("test-context", 10, 990);
    }

    @Test
    public void testFindAllWithLargeOffset() {
        // Given
        SearchResults<EmailNotificationContextRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 2000L, 120L);
        when(mockDao.findAll(10, 1990)).thenReturn(expectedResults);

        // When
        SearchResults<EmailNotificationContextRecord> results = service.findAll(10, 1990);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(2000L, results.getTotalNumberOfResults());
        verify(mockDao).findAll(10, 1990);
    }
}
