package org.ikasan.orchestration.service.scheduled.job;

import org.ikasan.job.orchestration.model.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InternalEventDrivenJobRecordServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class InternalEventDrivenJobRecordServiceImplTest {

    @Mock
    private InternalEventDrivenJobDao mockDao;

    @Mock
    private InternalEventDrivenJobRecord mockJobRecord;

    private InternalEventDrivenJobRecordServiceImpl service;

    @Before
    public void setUp() {
        service = new InternalEventDrivenJobRecordServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new InternalEventDrivenJobRecordServiceImpl(null);
    }

    @Test
    public void testFindAll() {
        // Given
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockJobRecord), 1L, 100L);
        when(mockDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals(100L, results.getQueryResponseTime());
        verify(mockDao).findAll(10, 0);
    }

    @Test
    public void testFindAllWithDifferentPagination() {
        // Given
        InternalEventDrivenJobRecord record2 = mock(InternalEventDrivenJobRecord.class);
        InternalEventDrivenJobRecord record3 = mock(InternalEventDrivenJobRecord.class);
        List<InternalEventDrivenJobRecord> records = Arrays.asList(mockJobRecord, record2, record3);
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records, 10L, 150L);
        when(mockDao.findAll(20, 5)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results = service.findAll(20, 5);

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
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockDao).findAll(10, 0);
    }

    @Test
    public void testFindByContext() {
        // Given
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockJobRecord), 1L, 100L);
        when(mockDao.findByContext("test-context", 10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results =
            service.findByContext("test-context", 10, 0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals(100L, results.getQueryResponseTime());
        verify(mockDao).findByContext("test-context", 10, 0);
    }

    @Test
    public void testFindByContextWithMultipleResults() {
        // Given
        InternalEventDrivenJobRecord record2 = mock(InternalEventDrivenJobRecord.class);
        InternalEventDrivenJobRecord record3 = mock(InternalEventDrivenJobRecord.class);
        InternalEventDrivenJobRecord record4 = mock(InternalEventDrivenJobRecord.class);
        List<InternalEventDrivenJobRecord> records = Arrays.asList(mockJobRecord, record2, record3, record4);
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records, 15L, 200L);
        when(mockDao.findByContext("large-context", 20, 10)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results =
            service.findByContext("large-context", 20, 10);

        // Then
        assertNotNull(results);
        assertEquals(4, results.getResultList().size());
        assertEquals(15L, results.getTotalNumberOfResults());
        assertEquals(200L, results.getQueryResponseTime());
        verify(mockDao).findByContext("large-context", 20, 10);
    }

    @Test
    public void testFindByContextEmptyResults() {
        // Given
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 30L);
        when(mockDao.findByContext("empty-context", 10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results =
            service.findByContext("empty-context", 10, 0);

        // Then
        assertNotNull(results);
        assertTrue(results.getResultList().isEmpty());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockDao).findByContext("empty-context", 10, 0);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("job-1")).thenReturn(mockJobRecord);

        // When
        InternalEventDrivenJobRecord result = service.findById("job-1");

        // Then
        assertNotNull(result);
        assertEquals(mockJobRecord, result);
        verify(mockDao).findById("job-1");
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockDao.findById("non-existent")).thenReturn(null);

        // When
        InternalEventDrivenJobRecord result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockDao).findById("non-existent");
    }

    @Test
    public void testFindByIdWithDifferentIds() {
        // Given
        InternalEventDrivenJobRecord record1 = mock(InternalEventDrivenJobRecord.class);
        InternalEventDrivenJobRecord record2 = mock(InternalEventDrivenJobRecord.class);
        InternalEventDrivenJobRecord record3 = mock(InternalEventDrivenJobRecord.class);

        when(mockDao.findById("job-1")).thenReturn(record1);
        when(mockDao.findById("job-2")).thenReturn(record2);
        when(mockDao.findById("job-3")).thenReturn(record3);

        // When
        InternalEventDrivenJobRecord result1 = service.findById("job-1");
        InternalEventDrivenJobRecord result2 = service.findById("job-2");
        InternalEventDrivenJobRecord result3 = service.findById("job-3");

        // Then
        assertEquals(record1, result1);
        assertEquals(record2, result2);
        assertEquals(record3, result3);
        verify(mockDao).findById("job-1");
        verify(mockDao).findById("job-2");
        verify(mockDao).findById("job-3");
    }

    @Test
    public void testSave() {
        // When
        service.save(mockJobRecord);

        // Then
        verify(mockDao).save(mockJobRecord);
    }

    @Test
    public void testSaveMultipleTimes() {
        // Given
        InternalEventDrivenJobRecord record2 = mock(InternalEventDrivenJobRecord.class);
        InternalEventDrivenJobRecord record3 = mock(InternalEventDrivenJobRecord.class);

        // When
        service.save(mockJobRecord);
        service.save(record2);
        service.save(record3);

        // Then
        verify(mockDao).save(mockJobRecord);
        verify(mockDao).save(record2);
        verify(mockDao).save(record3);
        verify(mockDao, times(3)).save(any(InternalEventDrivenJobRecord.class));
    }

    @Test
    public void testFindByContextWithZeroLimit() {
        // Given
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 25L);
        when(mockDao.findByContext("test-context", 0, 0)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results =
            service.findByContext("test-context", 0, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        verify(mockDao).findByContext("test-context", 0, 0);
    }

    @Test
    public void testFindAllWithZeroLimitAndOffset() {
        // Given
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 20L);
        when(mockDao.findAll(0, 0)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results = service.findAll(0, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        verify(mockDao).findAll(0, 0);
    }

    @Test
    public void testFindByContextWithLargeOffset() {
        // Given
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockJobRecord), 1000L, 80L);
        when(mockDao.findByContext("test-context", 10, 990)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results =
            service.findByContext("test-context", 10, 990);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1000L, results.getTotalNumberOfResults());
        verify(mockDao).findByContext("test-context", 10, 990);
    }

    @Test
    public void testFindAllWithLargeLimit() {
        // Given
        List<InternalEventDrivenJobRecord> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(InternalEventDrivenJobRecord.class));
        }
        SearchResults<InternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(largeList, 500L, 300L);
        when(mockDao.findAll(100, 0)).thenReturn(expectedResults);

        // When
        SearchResults<InternalEventDrivenJobRecord> results = service.findAll(100, 0);

        // Then
        assertNotNull(results);
        assertEquals(100, results.getResultList().size());
        assertEquals(500L, results.getTotalNumberOfResults());
        assertEquals(300L, results.getQueryResponseTime());
        verify(mockDao).findAll(100, 0);
    }
}
