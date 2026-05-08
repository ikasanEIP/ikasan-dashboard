package org.ikasan.orchestration.service.scheduled.instance;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceAuditRecordImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
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
 * Unit tests for ScheduledContextInstanceServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledContextInstanceServiceImplTest {

    @Mock
    private ScheduledContextInstanceDao mockInstanceDao;

    @Mock
    private ScheduledContextInstanceAuditDao mockAuditDao;

    @Mock
    private ScheduledContextInstanceAuditAggregateDao mockAuditAggregateDao;

    @Mock
    private ScheduledContextInstanceRecord mockInstanceRecord;

    @Mock
    private ScheduledContextInstanceAuditAggregateRecord mockAuditAggregateRecord;

    @Mock
    private ScheduledContextInstanceAuditAggregate mockAuditAggregate;

    @Mock
    private ContextInstance mockPreviousContextInstance;

    @Mock
    private ContextInstance mockUpdatedContextInstance;

    @Mock
    private ContextInstanceSearchFilter mockFilter;

    @Mock
    private ScheduledContextInstanceAuditAggregateSearchFilter mockAuditFilter;

    private ScheduledContextInstanceServiceImpl service;
    private ScheduledContextInstanceServiceImpl serviceWithAuditEnabled;
    private ScheduledContextInstanceServiceImpl serviceWithDeltaEnabled;

    @Before
    public void setUp() {
        service = new ScheduledContextInstanceServiceImpl(
            mockInstanceDao, mockAuditDao, mockAuditAggregateDao, false, false
        );
        serviceWithAuditEnabled = new ScheduledContextInstanceServiceImpl(
            mockInstanceDao, mockAuditDao, mockAuditAggregateDao, true, false
        );
        serviceWithDeltaEnabled = new ScheduledContextInstanceServiceImpl(
            mockInstanceDao, mockAuditDao, mockAuditAggregateDao, true, true
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullInstanceDao() {
        new ScheduledContextInstanceServiceImpl(null, mockAuditDao, mockAuditAggregateDao, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullAuditDao() {
        new ScheduledContextInstanceServiceImpl(mockInstanceDao, null, mockAuditAggregateDao, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullAuditAggregateDao() {
        new ScheduledContextInstanceServiceImpl(mockInstanceDao, mockAuditDao, null, false, false);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockInstanceDao.findById("ctx-instance-1")).thenReturn(mockInstanceRecord);

        // When
        ScheduledContextInstanceRecord result = service.findById("ctx-instance-1");

        // Then
        assertNotNull(result);
        assertEquals(mockInstanceRecord, result);
        verify(mockInstanceDao).findById("ctx-instance-1");
    }

    @Test
    public void testDeleteById() {
        // When
        service.deleteById("ctx-instance-1");

        // Then
        verify(mockInstanceDao).deleteById("ctx-instance-1");
    }

    @Test
    public void testSave() {
        // When
        service.save(mockInstanceRecord);

        // Then
        verify(mockInstanceDao).save(mockInstanceRecord);
    }

    @Test
    public void testGetScheduledContextInstancesByStatus() {
        // Given
        List<InstanceStatus> statuses = Arrays.asList(InstanceStatus.RUNNING, InstanceStatus.COMPLETE);
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockInstanceRecord), 1L, 100L);
        when(mockInstanceDao.getScheduledContextInstancesByStatus(statuses)).thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results = service.getScheduledContextInstancesByStatus(statuses);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockInstanceDao).getScheduledContextInstancesByStatus(statuses);
    }

    @Test
    public void testGetScheduledContextInstancesByStatusWithLimitAndOffset() {
        // Given
        List<InstanceStatus> statuses = Arrays.asList(InstanceStatus.RUNNING);
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockInstanceRecord), 1L, 100L);
        when(mockInstanceDao.getScheduledContextInstancesByStatus(statuses, 10, 0))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByStatus(statuses, 10, 0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockInstanceDao).getScheduledContextInstancesByStatus(statuses, 10, 0);
    }

    @Test
    public void testFindAuditRecordById() {
        // Given
        when(mockAuditDao.findById("audit-1")).thenReturn(mockInstanceRecord);

        // When
        ScheduledContextInstanceRecord result = service.findAuditRecordById("audit-1");

        // Then
        assertNotNull(result);
        assertEquals(mockInstanceRecord, result);
        verify(mockAuditDao).findById("audit-1");
    }

    @Test
    public void testSaveAuditWithAuditDisabled() {
        // When
        service.saveAudit(mockAuditAggregateRecord, mockPreviousContextInstance, mockUpdatedContextInstance);

        // Then
        verifyNoInteractions(mockAuditDao);
        verifyNoInteractions(mockAuditAggregateDao);
        verifyNoInteractions(mockAuditAggregateRecord);
    }

    @Test
    public void testSaveAuditWithAuditEnabledButDeltaDisabled() {
        // Given
        when(mockAuditAggregateRecord.getScheduledContextInstanceAuditAggregate())
            .thenReturn(mockAuditAggregate);

        // When
        serviceWithAuditEnabled.saveAudit(mockAuditAggregateRecord, mockPreviousContextInstance, mockUpdatedContextInstance);

        // Then
        verify(mockAuditAggregateRecord).getScheduledContextInstanceAuditAggregate();
        verify(mockAuditAggregateRecord).setScheduledContextInstanceAuditAggregate(mockAuditAggregate);
        verify(mockAuditAggregateDao).save(mockAuditAggregateRecord);
        verifyNoInteractions(mockAuditDao);
    }

    @Test
    public void testSaveAuditWithAuditAndDeltaEnabled() {
        // Given
        when(mockAuditAggregateRecord.getScheduledContextInstanceAuditAggregate())
            .thenReturn(mockAuditAggregate);
        ContextInstance previous = new ContextInstanceImpl();
        previous.setName("test-context");
        previous.setId("prev-ctx-instance-1");
        ContextInstance updated = new ContextInstanceImpl();
        updated.setName("test-context");
        updated.setId("updated-ctx-instance-1");

        // When
        serviceWithDeltaEnabled.saveAudit(mockAuditAggregateRecord, previous, updated);

        // Then
        ArgumentCaptor<SolrScheduledContextInstanceAuditRecordImpl> auditCaptor =
            ArgumentCaptor.forClass(SolrScheduledContextInstanceAuditRecordImpl.class);
        verify(mockAuditDao, times(2)).save(auditCaptor.capture());

        List<SolrScheduledContextInstanceAuditRecordImpl> savedAuditRecords = auditCaptor.getAllValues();
        assertEquals(2, savedAuditRecords.size());

        // Verify previous context instance audit record
        SolrScheduledContextInstanceAuditRecordImpl previousRecord = savedAuditRecords.get(0);
        assertEquals("test-context", previousRecord.getContextName());
        assertEquals("prev-ctx-instance-1", previousRecord.getContextInstanceId());
        assertEquals(previous.getId(), previousRecord.getContextInstance().getId());

        // Verify updated context instance audit record
        SolrScheduledContextInstanceAuditRecordImpl updatedRecord = savedAuditRecords.get(1);
        assertEquals("test-context", updatedRecord.getContextName());
        assertEquals("updated-ctx-instance-1", updatedRecord.getContextInstanceId());
        assertEquals(updated.getId(), updatedRecord.getContextInstance().getId());

        // Verify audit aggregate updated with IDs
        verify(mockAuditAggregate).setPreviousContextInstanceAuditId(previousRecord.getId());
        verify(mockAuditAggregate).setUpdatedContextInstanceAuditId(updatedRecord.getId());
        verify(mockAuditAggregateRecord).setScheduledContextInstanceAuditAggregate(mockAuditAggregate);
        verify(mockAuditAggregateDao).save(mockAuditAggregateRecord);
    }

    @Test
    public void testFindAllAuditRecords() {
        // Given
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockAuditAggregateRecord), 1L, 100L);
        when(mockAuditAggregateDao.findAll(10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            service.findAllAuditRecords(10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockAuditAggregateDao).findAll(10, 0, "timestamp", "DESC");
    }

    @Test
    public void testFindAllAuditRecordsByFilter() {
        // Given
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockAuditAggregateRecord), 1L, 100L);
        when(mockAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
            mockAuditFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            service.findAllAuditRecordsByFilter(mockAuditFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockAuditAggregateDao).findScheduledContextInstanceAuditAggregateRecordsByFilter(
            mockAuditFilter, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetScheduledContextInstancesByContextName() {
        // Given
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockInstanceRecord), 1L, 100L);
        when(mockInstanceDao.getScheduledContextInstancesByContextName(
            "test-context", 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByContextName("test-context", 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockInstanceDao).getScheduledContextInstancesByContextName(
            "test-context", 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetScheduledContextInstancesByContextNameWithTimestamps() {
        // Given
        long startTimestamp = 1000000L;
        long endTimestamp = 2000000L;
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockInstanceRecord), 1L, 100L);
        when(mockInstanceDao.getScheduledContextInstancesByContextName(
            "test-context", startTimestamp, endTimestamp, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByContextName(
                "test-context", startTimestamp, endTimestamp, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockInstanceDao).getScheduledContextInstancesByContextName(
            "test-context", startTimestamp, endTimestamp, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetScheduledContextInstancesByFilter() {
        // Given
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockInstanceRecord), 1L, 100L);
        when(mockInstanceDao.getScheduledContextInstancesByFilter(
            mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByFilter(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockInstanceDao).getScheduledContextInstancesByFilter(
            mockFilter, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetScheduledContextInstancesByStatusWithMultipleStatuses() {
        // Given
        List<InstanceStatus> statuses = Arrays.asList(
            InstanceStatus.RUNNING,
            InstanceStatus.COMPLETE,
            InstanceStatus.ERROR
        );
        ScheduledContextInstanceRecord record2 = mock(ScheduledContextInstanceRecord.class);
        ScheduledContextInstanceRecord record3 = mock(ScheduledContextInstanceRecord.class);
        List<ScheduledContextInstanceRecord> records = Arrays.asList(mockInstanceRecord, record2, record3);
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(records, 3L, 150L);
        when(mockInstanceDao.getScheduledContextInstancesByStatus(statuses, 20, 5))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByStatus(statuses, 20, 5);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(3L, results.getTotalNumberOfResults());
        verify(mockInstanceDao).getScheduledContextInstancesByStatus(statuses, 20, 5);
    }

    @Test
    public void testSaveAuditWithDeltaEnabledVerifyAuditIds() {
        // Given
        when(mockAuditAggregateRecord.getScheduledContextInstanceAuditAggregate())
            .thenReturn(mockAuditAggregate);
        ContextInstance previous = new ContextInstanceImpl();
        previous.setName("test-context");
        previous.setId("prev-ctx-instance-1");
        ContextInstance updated = new ContextInstanceImpl();
        updated.setName("test-context");
        updated.setId("updated-ctx-instance-1");

        // When
        serviceWithDeltaEnabled.saveAudit(mockAuditAggregateRecord, previous, updated);

        // Then
        ArgumentCaptor<SolrScheduledContextInstanceAuditRecordImpl> captor =
            ArgumentCaptor.forClass(SolrScheduledContextInstanceAuditRecordImpl.class);
        verify(mockAuditDao, times(2)).save(captor.capture());

        List<SolrScheduledContextInstanceAuditRecordImpl> savedRecords = captor.getAllValues();
        String previousAuditId = savedRecords.get(0).getId();
        String updatedAuditId = savedRecords.get(1).getId();

        assertNotNull(previousAuditId);
        assertNotNull(updatedAuditId);

        verify(mockAuditAggregate).setPreviousContextInstanceAuditId(previousAuditId);
        verify(mockAuditAggregate).setUpdatedContextInstanceAuditId(updatedAuditId);
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockInstanceDao.findById("non-existent")).thenReturn(null);

        // When
        ScheduledContextInstanceRecord result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockInstanceDao).findById("non-existent");
    }

    @Test
    public void testGetScheduledContextInstancesByStatusEmptyResults() {
        // Given
        List<InstanceStatus> statuses = Arrays.asList(InstanceStatus.SKIPPED);
        SearchResults<ScheduledContextInstanceRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockInstanceDao.getScheduledContextInstancesByStatus(statuses, 10, 0))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextInstanceRecord> results =
            service.getScheduledContextInstancesByStatus(statuses, 10, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockInstanceDao).getScheduledContextInstancesByStatus(statuses, 10, 0);
    }
}
