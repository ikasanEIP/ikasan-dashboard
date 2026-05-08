package org.ikasan.esb.service.systemevent;

import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchDao;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SystemEventSearchServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class SystemEventSearchServiceImplTest {

    @Mock
    private SystemEventSearchDao mockDao;

    @Mock
    private SystemEvent mockSystemEvent;

    @Mock
    private SystemEventSearchFilter mockSearchFilter;

    @Mock
    private SearchResults<SystemEvent> mockSearchResults;

    private SystemEventSearchServiceImpl service;

    @Before
    public void setUp() {
        service = new SystemEventSearchServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new SystemEventSearchServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        SystemEventSearchServiceImpl testService = new SystemEventSearchServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("event-1")).thenReturn(mockSystemEvent);

        // When
        SystemEvent result = service.findById("event-1");

        // Then
        assertNotNull(result);
        assertEquals(mockSystemEvent, result);
        verify(mockDao).findById("event-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockDao.findById("non-existent")).thenReturn(null);

        // When
        SystemEvent result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockDao).findById("non-existent");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdWithDifferentIds() {
        // Given
        SystemEvent event1 = mock(SystemEvent.class);
        SystemEvent event2 = mock(SystemEvent.class);
        SystemEvent event3 = mock(SystemEvent.class);

        when(mockDao.findById("event-1")).thenReturn(event1);
        when(mockDao.findById("event-2")).thenReturn(event2);
        when(mockDao.findById("event-3")).thenReturn(event3);

        // When
        SystemEvent result1 = service.findById("event-1");
        SystemEvent result2 = service.findById("event-2");
        SystemEvent result3 = service.findById("event-3");

        // Then
        assertEquals(event1, result1);
        assertEquals(event2, result2);
        assertEquals(event3, result3);
        verify(mockDao).findById("event-1");
        verify(mockDao).findById("event-2");
        verify(mockDao).findById("event-3");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdMultipleCalls() {
        // Given
        when(mockDao.findById("event-1")).thenReturn(mockSystemEvent);

        // When
        service.findById("event-1");
        service.findById("event-1");
        service.findById("event-1");

        // Then
        verify(mockDao, times(3)).findById("event-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilter() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(mockSearchResults);

        // When
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(mockSearchResults, results);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithDifferentLimits() {
        // Given
        SearchResults<SystemEvent> results1 = mock(SearchResults.class);
        SearchResults<SystemEvent> results2 = mock(SearchResults.class);
        SearchResults<SystemEvent> results3 = mock(SearchResults.class);

        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC")).thenReturn(results1);
        when(mockDao.findByFilter(mockSearchFilter, 20, 0, "timestamp", "DESC")).thenReturn(results2);
        when(mockDao.findByFilter(mockSearchFilter, 50, 0, "timestamp", "DESC")).thenReturn(results3);

        // When
        SearchResults<SystemEvent> result1 = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        SearchResults<SystemEvent> result2 = service.findByFilter(mockSearchFilter, 20, 0, "timestamp", "DESC");
        SearchResults<SystemEvent> result3 = service.findByFilter(mockSearchFilter, 50, 0, "timestamp", "DESC");

        // Then
        assertEquals(results1, result1);
        assertEquals(results2, result2);
        assertEquals(results3, result3);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        verify(mockDao).findByFilter(mockSearchFilter, 20, 0, "timestamp", "DESC");
        verify(mockDao).findByFilter(mockSearchFilter, 50, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithDifferentOffsets() {
        // Given
        SearchResults<SystemEvent> results1 = mock(SearchResults.class);
        SearchResults<SystemEvent> results2 = mock(SearchResults.class);
        SearchResults<SystemEvent> results3 = mock(SearchResults.class);

        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC")).thenReturn(results1);
        when(mockDao.findByFilter(mockSearchFilter, 10, 10, "timestamp", "DESC")).thenReturn(results2);
        when(mockDao.findByFilter(mockSearchFilter, 10, 20, "timestamp", "DESC")).thenReturn(results3);

        // When
        SearchResults<SystemEvent> result1 = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        SearchResults<SystemEvent> result2 = service.findByFilter(mockSearchFilter, 10, 10, "timestamp", "DESC");
        SearchResults<SystemEvent> result3 = service.findByFilter(mockSearchFilter, 10, 20, "timestamp", "DESC");

        // Then
        assertEquals(results1, result1);
        assertEquals(results2, result2);
        assertEquals(results3, result3);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        verify(mockDao).findByFilter(mockSearchFilter, 10, 10, "timestamp", "DESC");
        verify(mockDao).findByFilter(mockSearchFilter, 10, 20, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithDifferentSortColumns() {
        // Given
        SearchResults<SystemEvent> results1 = mock(SearchResults.class);
        SearchResults<SystemEvent> results2 = mock(SearchResults.class);
        SearchResults<SystemEvent> results3 = mock(SearchResults.class);

        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC")).thenReturn(results1);
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "subject", "ASC")).thenReturn(results2);
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "action", "DESC")).thenReturn(results3);

        // When
        SearchResults<SystemEvent> result1 = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        SearchResults<SystemEvent> result2 = service.findByFilter(mockSearchFilter, 10, 0, "subject", "ASC");
        SearchResults<SystemEvent> result3 = service.findByFilter(mockSearchFilter, 10, 0, "action", "DESC");

        // Then
        assertEquals(results1, result1);
        assertEquals(results2, result2);
        assertEquals(results3, result3);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "subject", "ASC");
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "action", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithDifferentSortOrders() {
        // Given
        SearchResults<SystemEvent> resultsAsc = mock(SearchResults.class);
        SearchResults<SystemEvent> resultsDesc = mock(SearchResults.class);

        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "ASC")).thenReturn(resultsAsc);
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC")).thenReturn(resultsDesc);

        // When
        SearchResults<SystemEvent> resultAsc = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "ASC");
        SearchResults<SystemEvent> resultDesc = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertEquals(resultsAsc, resultAsc);
        assertEquals(resultsDesc, resultDesc);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", "ASC");
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithNullSortColumn() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, null, "DESC"))
            .thenReturn(mockSearchResults);

        // When
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 10, 0, null, "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, null, "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithNullSortOrder() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", null))
            .thenReturn(mockSearchResults);

        // When
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", null);

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", null);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithDifferentFilters() {
        // Given
        SystemEventSearchFilter filter1 = mock(SystemEventSearchFilter.class);
        SystemEventSearchFilter filter2 = mock(SystemEventSearchFilter.class);
        SystemEventSearchFilter filter3 = mock(SystemEventSearchFilter.class);

        SearchResults<SystemEvent> results1 = mock(SearchResults.class);
        SearchResults<SystemEvent> results2 = mock(SearchResults.class);
        SearchResults<SystemEvent> results3 = mock(SearchResults.class);

        when(mockDao.findByFilter(filter1, 10, 0, "timestamp", "DESC")).thenReturn(results1);
        when(mockDao.findByFilter(filter2, 10, 0, "timestamp", "DESC")).thenReturn(results2);
        when(mockDao.findByFilter(filter3, 10, 0, "timestamp", "DESC")).thenReturn(results3);

        // When
        SearchResults<SystemEvent> result1 = service.findByFilter(filter1, 10, 0, "timestamp", "DESC");
        SearchResults<SystemEvent> result2 = service.findByFilter(filter2, 10, 0, "timestamp", "DESC");
        SearchResults<SystemEvent> result3 = service.findByFilter(filter3, 10, 0, "timestamp", "DESC");

        // Then
        assertEquals(results1, result1);
        assertEquals(results2, result2);
        assertEquals(results3, result3);
        verify(mockDao).findByFilter(filter1, 10, 0, "timestamp", "DESC");
        verify(mockDao).findByFilter(filter2, 10, 0, "timestamp", "DESC");
        verify(mockDao).findByFilter(filter3, 10, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithLargeLimit() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 1000, 0, "timestamp", "DESC"))
            .thenReturn(mockSearchResults);

        // When
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 1000, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockSearchFilter, 1000, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithLargeOffset() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 10, 10000, "timestamp", "DESC"))
            .thenReturn(mockSearchResults);

        // When
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 10, 10000, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockSearchFilter, 10, 10000, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterMultipleCalls() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(mockSearchResults);

        // When
        service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");

        // Then
        verify(mockDao, times(2)).findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithZeroLimit() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 0, 0, "timestamp", "DESC"))
            .thenReturn(mockSearchResults);

        // When
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 0, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockSearchFilter, 0, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByFilterWithAllParametersCombinations() {
        // Given
        when(mockDao.findByFilter(mockSearchFilter, 25, 50, "subject", "ASC"))
            .thenReturn(mockSearchResults);

        // When
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 25, 50, "subject", "ASC");

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockSearchFilter, 25, 50, "subject", "ASC");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testMixedFindByIdAndFindByFilter() {
        // Given
        when(mockDao.findById("event-1")).thenReturn(mockSystemEvent);
        when(mockDao.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(mockSearchResults);

        // When
        SystemEvent event = service.findById("event-1");
        SearchResults<SystemEvent> results = service.findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(event);
        assertNotNull(results);
        verify(mockDao).findById("event-1");
        verify(mockDao).findByFilter(mockSearchFilter, 10, 0, "timestamp", "DESC");
        verifyNoMoreInteractions(mockDao);
    }
}
