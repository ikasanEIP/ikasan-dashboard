package org.ikasan.orchestration.service.scheduled.joblock;

import org.ikasan.job.orchestration.model.cache.JobLockCacheAuditRecordImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
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
 * Unit tests for JobLockCacheServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class JobLockCacheServiceImplTest {

    @Mock
    private JobLockCacheDao mockCacheDao;

    @Mock
    private JobLockCacheAuditDao mockAuditDao;

    @Mock
    private JobLockCacheRecord mockCacheRecord;

    private JobLockCacheData testJobLockCache;

    @Mock
    private JobLockCacheAuditRecord mockAuditRecord;

    private JobLockCacheServiceImpl serviceWithAuditEnabled;
    private JobLockCacheServiceImpl serviceWithAuditDisabled;

    @Before
    public void setUp() {
        testJobLockCache = new JobLockCacheDataImpl();
        serviceWithAuditEnabled = new JobLockCacheServiceImpl(mockCacheDao, mockAuditDao, true);
        serviceWithAuditDisabled = new JobLockCacheServiceImpl(mockCacheDao, mockAuditDao, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullCacheDao() {
        new JobLockCacheServiceImpl(null, mockAuditDao, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullAuditDao() {
        new JobLockCacheServiceImpl(mockCacheDao, null, true);
    }

    @Test
    public void testConstructorWithValidParameters() {
        // When
        JobLockCacheServiceImpl service = new JobLockCacheServiceImpl(mockCacheDao, mockAuditDao, true);

        // Then
        assertNotNull(service);
    }

    @Test
    public void testSaveWithAuditEnabled() {
        // Given
        when(mockCacheRecord.getJobLockCache()).thenReturn(testJobLockCache);

        // When
        serviceWithAuditEnabled.save(mockCacheRecord);

        // Then
        verify(mockCacheDao).save(mockCacheRecord);

        ArgumentCaptor<JobLockCacheAuditRecord> auditCaptor =
            ArgumentCaptor.forClass(JobLockCacheAuditRecord.class);
        verify(mockAuditDao).save(auditCaptor.capture());

        JobLockCacheAuditRecord capturedAudit = auditCaptor.getValue();
        assertNotNull(capturedAudit);
        assertNotNull(capturedAudit.getJobLockCache());
        assertEquals(JobLockCacheDataImpl.class, capturedAudit.getJobLockCache().getClass());
    }

    @Test
    public void testSaveWithAuditDisabled() {
        // When
        serviceWithAuditDisabled.save(mockCacheRecord);

        // Then
        verify(mockCacheDao).save(mockCacheRecord);
        verifyNoInteractions(mockAuditDao);
    }

    @Test
    public void testSaveMultipleTimesWithAuditEnabled() {
        // Given
        JobLockCacheRecord record2 = mock(JobLockCacheRecord.class);
        JobLockCacheRecord record3 = mock(JobLockCacheRecord.class);
        JobLockCacheData cache2 = new JobLockCacheDataImpl();
        JobLockCacheData cache3 = new JobLockCacheDataImpl();

        when(mockCacheRecord.getJobLockCache()).thenReturn(testJobLockCache);
        when(record2.getJobLockCache()).thenReturn(cache2);
        when(record3.getJobLockCache()).thenReturn(cache3);

        // When
        serviceWithAuditEnabled.save(mockCacheRecord);
        serviceWithAuditEnabled.save(record2);
        serviceWithAuditEnabled.save(record3);

        // Then
        verify(mockCacheDao).save(mockCacheRecord);
        verify(mockCacheDao).save(record2);
        verify(mockCacheDao).save(record3);
        verify(mockAuditDao, times(3)).save(any(JobLockCacheAuditRecord.class));
    }

    @Test
    public void testSaveMultipleTimesWithAuditDisabled() {
        // Given
        JobLockCacheRecord record2 = mock(JobLockCacheRecord.class);
        JobLockCacheRecord record3 = mock(JobLockCacheRecord.class);

        // When
        serviceWithAuditDisabled.save(mockCacheRecord);
        serviceWithAuditDisabled.save(record2);
        serviceWithAuditDisabled.save(record3);

        // Then
        verify(mockCacheDao).save(mockCacheRecord);
        verify(mockCacheDao).save(record2);
        verify(mockCacheDao).save(record3);
        verifyNoInteractions(mockAuditDao);
    }

    @Test
    public void testGet() {
        // Given
        when(mockCacheDao.get("dev")).thenReturn(mockCacheRecord);

        // When
        JobLockCacheRecord result = serviceWithAuditEnabled.get("dev");

        // Then
        assertNotNull(result);
        assertEquals(mockCacheRecord, result);
        verify(mockCacheDao).get("dev");
    }

    @Test
    public void testGetReturnsNull() {
        // Given
        when(mockCacheDao.get("non-existent")).thenReturn(null);

        // When
        JobLockCacheRecord result = serviceWithAuditDisabled.get("non-existent");

        // Then
        assertNull(result);
        verify(mockCacheDao).get("non-existent");
    }

    @Test
    public void testGetMultipleEnvironments() {
        // Given
        JobLockCacheRecord devRecord = mock(JobLockCacheRecord.class);
        JobLockCacheRecord prodRecord = mock(JobLockCacheRecord.class);
        JobLockCacheRecord testRecord = mock(JobLockCacheRecord.class);

        when(mockCacheDao.get("dev")).thenReturn(devRecord);
        when(mockCacheDao.get("prod")).thenReturn(prodRecord);
        when(mockCacheDao.get("test")).thenReturn(testRecord);

        // When
        JobLockCacheRecord devResult = serviceWithAuditEnabled.get("dev");
        JobLockCacheRecord prodResult = serviceWithAuditEnabled.get("prod");
        JobLockCacheRecord testResult = serviceWithAuditEnabled.get("test");

        // Then
        assertEquals(devRecord, devResult);
        assertEquals(prodRecord, prodResult);
        assertEquals(testRecord, testResult);
        verify(mockCacheDao).get("dev");
        verify(mockCacheDao).get("prod");
        verify(mockCacheDao).get("test");
    }

    @Test
    public void testFindAll() {
        // Given
        SearchResults<JobLockCacheAuditRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockAuditRecord), 1L, 100L);
        when(mockAuditDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = serviceWithAuditEnabled.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals(100L, results.getQueryResponseTime());
        verify(mockAuditDao).findAll(10, 0);
    }

    @Test
    public void testFindAllWithMultipleResults() {
        // Given
        JobLockCacheAuditRecord audit2 = mock(JobLockCacheAuditRecord.class);
        JobLockCacheAuditRecord audit3 = mock(JobLockCacheAuditRecord.class);
        List<JobLockCacheAuditRecord> audits = Arrays.asList(mockAuditRecord, audit2, audit3);
        SearchResults<JobLockCacheAuditRecord> expectedResults =
            new SearchResultsImpl<>(audits, 10L, 150L);
        when(mockAuditDao.findAll(20, 5)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = serviceWithAuditDisabled.findAll(20, 5);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(10L, results.getTotalNumberOfResults());
        assertEquals(150L, results.getQueryResponseTime());
        verify(mockAuditDao).findAll(20, 5);
    }

    @Test
    public void testFindAllEmptyResults() {
        // Given
        SearchResults<JobLockCacheAuditRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockAuditDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = serviceWithAuditEnabled.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
        verify(mockAuditDao).findAll(10, 0);
    }

    @Test
    public void testFindAllWithLargeLimit() {
        // Given
        List<JobLockCacheAuditRecord> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(JobLockCacheAuditRecord.class));
        }
        SearchResults<JobLockCacheAuditRecord> expectedResults =
            new SearchResultsImpl<>(largeList, 500L, 300L);
        when(mockAuditDao.findAll(100, 0)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = serviceWithAuditDisabled.findAll(100, 0);

        // Then
        assertNotNull(results);
        assertEquals(100, results.getResultList().size());
        assertEquals(500L, results.getTotalNumberOfResults());
        assertEquals(300L, results.getQueryResponseTime());
        verify(mockAuditDao).findAll(100, 0);
    }

    @Test
    public void testFindAllWithZeroLimitAndOffset() {
        // Given
        SearchResults<JobLockCacheAuditRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 20L);
        when(mockAuditDao.findAll(0, 0)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = serviceWithAuditEnabled.findAll(0, 0);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        verify(mockAuditDao).findAll(0, 0);
    }

    @Test
    public void testSaveWithNullJobLockCacheWhenAuditEnabled() {
        // Given
        when(mockCacheRecord.getJobLockCache()).thenReturn(null);

        // When
        serviceWithAuditEnabled.save(mockCacheRecord);

        // Then
        verify(mockCacheDao).save(mockCacheRecord);

        ArgumentCaptor<JobLockCacheAuditRecord> auditCaptor =
            ArgumentCaptor.forClass(JobLockCacheAuditRecord.class);
        verify(mockAuditDao).save(auditCaptor.capture());

        JobLockCacheAuditRecord capturedAudit = auditCaptor.getValue();
        assertNotNull(capturedAudit);
        assertNull(capturedAudit.getJobLockCache());
    }

    @Test
    public void testGetSameEnvironmentMultipleTimes() {
        // Given
        when(mockCacheDao.get("prod")).thenReturn(mockCacheRecord);

        // When
        JobLockCacheRecord result1 = serviceWithAuditEnabled.get("prod");
        JobLockCacheRecord result2 = serviceWithAuditEnabled.get("prod");
        JobLockCacheRecord result3 = serviceWithAuditEnabled.get("prod");

        // Then
        assertEquals(mockCacheRecord, result1);
        assertEquals(mockCacheRecord, result2);
        assertEquals(mockCacheRecord, result3);
        verify(mockCacheDao, times(3)).get("prod");
    }

    @Test
    public void testAuditFlagBehavior() {
        // Given
        when(mockCacheRecord.getJobLockCache()).thenReturn(testJobLockCache);

        // When - save with audit enabled
        serviceWithAuditEnabled.save(mockCacheRecord);

        // Then
        verify(mockCacheDao).save(mockCacheRecord);
        verify(mockAuditDao).save(any(JobLockCacheAuditRecord.class));

        reset(mockCacheDao, mockAuditDao);

        // When - save with audit disabled
        serviceWithAuditDisabled.save(mockCacheRecord);

        // Then
        verify(mockCacheDao).save(mockCacheRecord);
        verifyNoInteractions(mockAuditDao);
    }
}
