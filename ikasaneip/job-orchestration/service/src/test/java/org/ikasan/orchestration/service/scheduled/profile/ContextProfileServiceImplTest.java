package org.ikasan.orchestration.service.scheduled.profile;

import org.ikasan.job.orchestration.model.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
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
 * Unit tests for ContextProfileServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ContextProfileServiceImplTest {

    @Mock
    private ContextProfileDao mockDao;

    @Mock
    private ContextProfileRecord mockRecord;

    @Mock
    private ContextProfileSearchFilter mockFilter;

    private ContextProfileServiceImpl service;

    @Before
    public void setUp() {
        service = new ContextProfileServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ContextProfileServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        ContextProfileServiceImpl testService = new ContextProfileServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
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
        ContextProfileRecord record2 = mock(ContextProfileRecord.class);
        ContextProfileRecord record3 = mock(ContextProfileRecord.class);

        // When
        service.save(mockRecord);
        service.save(record2);
        service.save(record3);

        // Then
        verify(mockDao).save(mockRecord);
        verify(mockDao).save(record2);
        verify(mockDao).save(record3);
        verify(mockDao, times(3)).save(any(ContextProfileRecord.class));
    }

    @Test
    public void testSaveMultipleRecords() {
        // Given
        ContextProfileRecord record2 = mock(ContextProfileRecord.class);
        ContextProfileRecord record3 = mock(ContextProfileRecord.class);
        List<ContextProfileRecord> records = Arrays.asList(mockRecord, record2, record3);

        // When
        service.save(records);

        // Then
        verify(mockDao).save(records);
    }

    @Test
    public void testSaveEmptyList() {
        // Given
        List<ContextProfileRecord> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
    }

    @Test
    public void testSaveLargeList() {
        // Given
        List<ContextProfileRecord> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ContextProfileRecord.class));
        }

        // When
        service.save(largeList);

        // Then
        verify(mockDao).save(largeList);
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
    public void testDeleteByContextNameWithNullParameter() {
        // When
        service.deleteByContextName(null);

        // Then
        verify(mockDao).deleteByContextName(null);
    }

    @Test
    public void testDeleteByContextNameWithEmptyString() {
        // When
        service.deleteByContextName("");

        // Then
        verify(mockDao).deleteByContextName("");
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("profile-1")).thenReturn(mockRecord);

        // When
        ContextProfileRecord result = service.findById("profile-1");

        // Then
        assertNotNull(result);
        assertEquals(mockRecord, result);
        verify(mockDao).findById("profile-1");
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockDao.findById("non-existent")).thenReturn(null);

        // When
        ContextProfileRecord result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockDao).findById("non-existent");
    }

    @Test
    public void testFindByIdWithDifferentIds() {
        // Given
        ContextProfileRecord record1 = mock(ContextProfileRecord.class);
        ContextProfileRecord record2 = mock(ContextProfileRecord.class);
        ContextProfileRecord record3 = mock(ContextProfileRecord.class);

        when(mockDao.findById("profile-1")).thenReturn(record1);
        when(mockDao.findById("profile-2")).thenReturn(record2);
        when(mockDao.findById("profile-3")).thenReturn(record3);

        // When
        ContextProfileRecord result1 = service.findById("profile-1");
        ContextProfileRecord result2 = service.findById("profile-2");
        ContextProfileRecord result3 = service.findById("profile-3");

        // Then
        assertEquals(record1, result1);
        assertEquals(record2, result2);
        assertEquals(record3, result3);
        verify(mockDao).findById("profile-1");
        verify(mockDao).findById("profile-2");
        verify(mockDao).findById("profile-3");
    }

    @Test
    public void testFindByIdWithNullParameter() {
        // Given
        when(mockDao.findById(null)).thenReturn(null);

        // When
        ContextProfileRecord result = service.findById(null);

        // Then
        assertNull(result);
        verify(mockDao).findById(null);
    }

    @Test
    public void testFindByFilter() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 100L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "profileName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 10, 0, "profileName", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals(100L, results.getQueryResponseTime());
        verify(mockDao).findByFilter(mockFilter, 10, 0, "profileName", "ASC");
    }

    @Test
    public void testFindByFilterWithMultipleResults() {
        // Given
        ContextProfileRecord record2 = mock(ContextProfileRecord.class);
        ContextProfileRecord record3 = mock(ContextProfileRecord.class);
        List<ContextProfileRecord> records = Arrays.asList(mockRecord, record2, record3);
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(records, 10L, 150L);
        when(mockDao.findByFilter(mockFilter, 20, 5, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 20, 5, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(10L, results.getTotalNumberOfResults());
        assertEquals(150L, results.getQueryResponseTime());
        verify(mockDao).findByFilter(mockFilter, 20, 5, "timestamp", "DESC");
    }

    @Test
    public void testFindByFilterEmptyResults() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "profileName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 10, 0, "profileName", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockDao).findByFilter(mockFilter, 10, 0, "profileName", "ASC");
    }

    @Test
    public void testFindByFilterWithNullSortParameters() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 80L);
        when(mockDao.findByFilter(mockFilter, 10, 0, null, null))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 10, 0, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockDao).findByFilter(mockFilter, 10, 0, null, null);
    }

    @Test
    public void testFindByFilterWithLargeOffset() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockRecord), 1000L, 120L);
        when(mockDao.findByFilter(mockFilter, 10, 990, "profileName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 10, 990, "profileName", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1000L, results.getTotalNumberOfResults());
        verify(mockDao).findByFilter(mockFilter, 10, 990, "profileName", "ASC");
    }

    @Test
    public void testFindByFilterWithDifferentSortColumns() {
        // Given
        SearchResults<ContextProfileRecord> results1 = new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 50L);
        SearchResults<ContextProfileRecord> results2 = new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 60L);
        SearchResults<ContextProfileRecord> results3 = new SearchResultsImpl<>(Arrays.asList(mockRecord), 1L, 70L);

        when(mockDao.findByFilter(mockFilter, 10, 0, "profileName", "ASC")).thenReturn(results1);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC")).thenReturn(results2);
        when(mockDao.findByFilter(mockFilter, 10, 0, "contextName", "ASC")).thenReturn(results3);

        // When
        SearchResults<ContextProfileRecord> result1 = service.findByFilter(mockFilter, 10, 0, "profileName", "ASC");
        SearchResults<ContextProfileRecord> result2 = service.findByFilter(mockFilter, 10, 0, "timestamp", "DESC");
        SearchResults<ContextProfileRecord> result3 = service.findByFilter(mockFilter, 10, 0, "contextName", "ASC");

        // Then
        assertEquals(50L, result1.getQueryResponseTime());
        assertEquals(60L, result2.getQueryResponseTime());
        assertEquals(70L, result3.getQueryResponseTime());
        verify(mockDao).findByFilter(mockFilter, 10, 0, "profileName", "ASC");
        verify(mockDao).findByFilter(mockFilter, 10, 0, "timestamp", "DESC");
        verify(mockDao).findByFilter(mockFilter, 10, 0, "contextName", "ASC");
    }

    @Test
    public void testFindByFilterWithZeroLimitAndOffset() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 20L);
        when(mockDao.findByFilter(mockFilter, 0, 0, "profileName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 0, 0, "profileName", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        verify(mockDao).findByFilter(mockFilter, 0, 0, "profileName", "ASC");
    }

    @Test
    public void testFindByFilterWithLargeLimit() {
        // Given
        List<ContextProfileRecord> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ContextProfileRecord.class));
        }
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(largeList, 500L, 300L);
        when(mockDao.findByFilter(mockFilter, 100, 0, "profileName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 100, 0, "profileName", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(100, results.getResultList().size());
        assertEquals(500L, results.getTotalNumberOfResults());
        assertEquals(300L, results.getQueryResponseTime());
        verify(mockDao).findByFilter(mockFilter, 100, 0, "profileName", "ASC");
    }

    @Test
    public void testSaveAndFindInteraction() {
        // Given
        when(mockDao.findById("profile-1")).thenReturn(mockRecord);

        // When - save first
        service.save(mockRecord);

        // Then - verify save was called
        verify(mockDao).save(mockRecord);

        // When - find the saved record
        ContextProfileRecord result = service.findById("profile-1");

        // Then - verify find was called and returned the record
        assertEquals(mockRecord, result);
        verify(mockDao).findById("profile-1");
    }

    @Test
    public void testSaveDeleteAndFindWorkflow() {
        // Given
        when(mockDao.findById("profile-1")).thenReturn(null);

        // When - save, delete, then find
        service.save(mockRecord);
        service.deleteByContextName("test-context");
        ContextProfileRecord result = service.findById("profile-1");

        // Then
        verify(mockDao).save(mockRecord);
        verify(mockDao).deleteByContextName("test-context");
        assertNull(result);
    }
}
