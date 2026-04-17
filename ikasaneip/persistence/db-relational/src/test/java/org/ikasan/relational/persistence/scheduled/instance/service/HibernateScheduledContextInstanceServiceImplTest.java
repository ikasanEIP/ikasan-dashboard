package org.ikasan.relational.persistence.scheduled.instance.service;

import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.instance.service.HibernateScheduledContextInstanceServiceImpl;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
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
 * Unit tests for HibernateScheduledContextInstanceServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateScheduledContextInstanceServiceImplTest {

    @Mock
    private ScheduledContextInstanceDao mockDao;

    @Mock
    private ScheduledContextInstanceAuditDao mockAuditDao;

    @Mock
    private ScheduledContextInstanceAuditAggregateDao mockAuditAggregateDao;

    @Mock
    private ScheduledContextInstanceRecord mockRecord;

    @Mock
    private ContextInstance mockContextInstance;

    @Mock
    private ScheduledContextInstanceAuditAggregateRecord mockAuditAggregateRecord;

    private HibernateScheduledContextInstanceServiceImpl service;

    @Before
    public void setUp() {
        service = new HibernateScheduledContextInstanceServiceImpl(mockDao, mockAuditDao, mockAuditAggregateDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new HibernateScheduledContextInstanceServiceImpl(null, mockAuditDao, mockAuditAggregateDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullAuditDao() {
        new HibernateScheduledContextInstanceServiceImpl(mockDao, null, mockAuditAggregateDao);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("test-id")).thenReturn(mockRecord);

        // When
        ScheduledContextInstanceRecord result = service.findById("test-id");

        // Then
        assertNotNull(result);
        verify(mockDao).findById("test-id");
    }

    @Test
    public void testDeleteById() {
        // When
        service.deleteById("test-id");

        // Then
        verify(mockDao).deleteById("test-id");
    }

    @Test
    public void testSave() {
        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void testGetScheduledContextInstancesByStatus() {
        // Given
        List<InstanceStatus> statuses = Arrays.asList(InstanceStatus.RUNNING, InstanceStatus.COMPLETE);
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.getScheduledContextInstancesByStatus(statuses)).thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByStatus(statuses);

        // Then
        assertNotNull(results);
        verify(mockDao).getScheduledContextInstancesByStatus(statuses);
    }

    @Test
    public void testSaveAudit() {
        // Given
        when(mockAuditAggregateRecord.getContextInstanceId()).thenReturn("context-instance-id");

        // When
        service.saveAudit(mockAuditAggregateRecord, mockContextInstance, mockContextInstance);

        // Then
        verify(mockAuditAggregateDao).save(mockAuditAggregateRecord);
    }

    @Test
    public void testSaveAuditWithNullAggregateDao() {
        // Given - service with null aggregate DAO
        HibernateScheduledContextInstanceServiceImpl serviceWithNullAggregateDao =
            new HibernateScheduledContextInstanceServiceImpl(mockDao, mockAuditDao, null);
        when(mockAuditAggregateRecord.getContextInstanceId()).thenReturn("context-instance-id");

        // When
        serviceWithNullAggregateDao.saveAudit(mockAuditAggregateRecord, mockContextInstance, mockContextInstance);

        // Then - should not throw, just log warning
        verifyNoInteractions(mockAuditAggregateDao);
    }

    @Test
    public void testFindAuditRecordById() {
        // Given
        when(mockAuditDao.findById("audit-id")).thenReturn(mockRecord);

        // When
        ScheduledContextInstanceRecord result = service.findAuditRecordById("audit-id");

        // Then
        assertNotNull(result);
        verify(mockAuditDao).findById("audit-id");
    }

    @Test
    public void testFindAllAuditRecords() {
        // Given
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockAuditAggregateDao.findAll(10, 0, "timestamp", "DESC")).thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            service.findAllAuditRecords(10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockAuditAggregateDao).findAll(10, 0, "timestamp", "DESC");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFindAllAuditRecordsWithNullAggregateDao() {
        // Given - service with null aggregate DAO
        HibernateScheduledContextInstanceServiceImpl serviceWithNullAggregateDao =
            new HibernateScheduledContextInstanceServiceImpl(mockDao, mockAuditDao, null);

        // When
        serviceWithNullAggregateDao.findAllAuditRecords(10, 0, "timestamp", "DESC");

        // Then - should throw UnsupportedOperationException
    }

    @Test
    public void testGetScheduledContextInstancesByStatusWithLimitOffset() {
        // Given
        List<InstanceStatus> statuses = Arrays.asList(InstanceStatus.RUNNING);
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.getScheduledContextInstancesByStatus(statuses, 10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByStatus(statuses, 10, 0);

        // Then
        assertNotNull(results);
        verify(mockDao).getScheduledContextInstancesByStatus(statuses, 10, 0);
    }

    @Test
    public void testGetScheduledContextInstancesByContextName() {
        // Given
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.getScheduledContextInstancesByContextName("test-context", 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByContextName("test-context", 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).getScheduledContextInstancesByContextName("test-context", 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetScheduledContextInstancesByContextNameWithTimeWindow() {
        // Given
        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.getScheduledContextInstancesByContextName(
            "test-context", startTime, endTime, 10, 0, "timestamp", "DESC")).thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByContextName("test-context", startTime, endTime,
                10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).getScheduledContextInstancesByContextName(
            "test-context", startTime, endTime, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetScheduledContextInstancesByFilter() {
        // Given
        ContextInstanceSearchFilter filter = mock(ContextInstanceSearchFilter.class);
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.getScheduledContextInstancesByFilter(filter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByFilter(filter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).getScheduledContextInstancesByFilter(filter, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testFindAllAuditRecordsByFilter() {
        // Given
        ScheduledContextInstanceAuditAggregateSearchFilter filter =
            mock(ScheduledContextInstanceAuditAggregateSearchFilter.class);
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
            filter, 10, 0, "timestamp", "DESC")).thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            service.findAllAuditRecordsByFilter(filter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockAuditAggregateDao).findScheduledContextInstanceAuditAggregateRecordsByFilter(
            filter, 10, 0, "timestamp", "DESC");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFindAllAuditRecordsByFilterWithNullAggregateDao() {
        // Given - service with null aggregate DAO
        HibernateScheduledContextInstanceServiceImpl serviceWithNullAggregateDao =
            new HibernateScheduledContextInstanceServiceImpl(mockDao, mockAuditDao, null);
        ScheduledContextInstanceAuditAggregateSearchFilter filter =
            mock(ScheduledContextInstanceAuditAggregateSearchFilter.class);

        // When
        serviceWithNullAggregateDao.findAllAuditRecordsByFilter(filter, 10, 0, "timestamp", "DESC");

        // Then - should throw UnsupportedOperationException
    }
}
