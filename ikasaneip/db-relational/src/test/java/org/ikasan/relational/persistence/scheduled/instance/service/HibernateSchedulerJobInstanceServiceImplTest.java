package org.ikasan.relational.persistence.scheduled.instance.service;

import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.instance.service.HibernateSchedulerJobInstanceServiceImpl;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
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
 * Unit tests for HibernateSchedulerJobInstanceServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateSchedulerJobInstanceServiceImplTest {

    @Mock
    private SchedulerJobInstanceDao mockDao;

    @Mock
    private SchedulerJobInstanceRecord mockRecord;

    @Mock
    private SchedulerJobInstance mockJobInstance;

    @Mock
    private SchedulerJobInstanceSearchFilter mockFilter;

    @Mock
    private ContextInstance mockContextInstance;

    private HibernateSchedulerJobInstanceServiceImpl service;

    @Before
    public void setUp() {
        service = new HibernateSchedulerJobInstanceServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new HibernateSchedulerJobInstanceServiceImpl(null);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("test-id")).thenReturn(mockRecord);

        // When
        SchedulerJobInstanceRecord result = service.findById("test-id");

        // Then
        assertNotNull(result);
        verify(mockDao).findById("test-id");
    }

    @Test
    public void testSave() {
        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void testSaveList() {
        // Given
        List<SchedulerJobInstanceRecord> records = Arrays.asList(mockRecord, mockRecord);

        // When
        service.save(records);

        // Then
        verify(mockDao, times(2)).save(mockRecord);
    }

    @Test
    public void testGetSchedulerJobInstancesByContextInstanceId() {
        // Given
        SearchResults<SchedulerJobInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.getSchedulerJobInstancesByContextInstanceId("context-1", 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobInstanceRecord> results =
            service.getSchedulerJobInstancesByContextInstanceId("context-1", 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).getSchedulerJobInstancesByContextInstanceId("context-1", 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetSchedulerJobInstancesByContextName() {
        // Given
        SearchResults<SchedulerJobInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.getSchedulerJobInstancesByContextName("test-context", 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobInstanceRecord> results =
            service.getSchedulerJobInstancesByContextName("test-context", 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).getSchedulerJobInstancesByContextName("test-context", 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testDeleteSchedulerJobInstances() {
        // When
        service.deleteSchedulerJobInstances("context-1");

        // Then
        verify(mockDao).deleteSchedulerJobInstances("context-1");
    }
}
