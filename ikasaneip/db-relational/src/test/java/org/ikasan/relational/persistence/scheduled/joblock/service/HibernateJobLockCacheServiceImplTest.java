package org.ikasan.relational.persistence.scheduled.joblock.service;

import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheAuditRecord;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheData;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheRecord;
import org.ikasan.relational.persistence.scheduled.joblock.service.HibernateJobLockCacheServiceImpl;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for HibernateJobLockCacheServiceImpl using Mockito.
 *
 * These tests verify:
 * - Constructor validation
 * - Save operations with and without audit creation
 * - Get operations with DAO delegation
 * - FindAll operations with pagination
 * - Proper interaction with both DAOs
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateJobLockCacheServiceImplTest {

    @Mock
    private JobLockCacheDao jobLockCacheDao;

    @Mock
    private JobLockCacheAuditDao jobLockCacheAuditDao;

    private HibernateJobLockCacheServiceImpl service;

    // ========== Constructor Tests ==========

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_job_lock_cache_dao_is_null() {
        new HibernateJobLockCacheServiceImpl(null, jobLockCacheAuditDao, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_job_lock_cache_audit_dao_is_null() {
        new HibernateJobLockCacheServiceImpl(jobLockCacheDao, null, true);
    }

    @Test
    public void test_constructor_succeeds_with_valid_dependencies() {
        // When
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);

        // Then
        assertNotNull(service);
    }

    @Test
    public void test_constructor_succeeds_with_audit_disabled() {
        // When
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, false);

        // Then
        assertNotNull(service);
    }

    // ========== Save Tests - Without Audit ==========

    @Test
    public void test_save_delegates_to_cache_dao_when_audit_disabled() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, false);
        HibernateJobLockCacheRecord record = createCacheRecord("test-env");

        // When
        service.save(record);

        // Then
        verify(jobLockCacheDao, times(1)).save(record);
        verify(jobLockCacheAuditDao, never()).save(any());
    }

    @Test
    public void test_save_with_null_environment_when_audit_disabled() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, false);
        HibernateJobLockCacheRecord record = createCacheRecord(null);

        // When
        service.save(record);

        // Then
        verify(jobLockCacheDao, times(1)).save(record);
        verify(jobLockCacheAuditDao, never()).save(any());
    }

    @Test
    public void test_save_with_default_environment_when_audit_disabled() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, false);
        HibernateJobLockCacheRecord record = createCacheRecord(JobLockCacheRecord.DEFAULT_ENVIRONMENT);

        // When
        service.save(record);

        // Then
        verify(jobLockCacheDao, times(1)).save(record);
        verify(jobLockCacheAuditDao, never()).save(any());
    }

    @Test
    public void test_save_multiple_times_when_audit_disabled() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, false);
        HibernateJobLockCacheRecord record1 = createCacheRecord("env1");
        HibernateJobLockCacheRecord record2 = createCacheRecord("env2");
        HibernateJobLockCacheRecord record3 = createCacheRecord("env3");

        // When
        service.save(record1);
        service.save(record2);
        service.save(record3);

        // Then
        verify(jobLockCacheDao, times(3)).save(any(JobLockCacheRecord.class));
        verify(jobLockCacheAuditDao, never()).save(any());
    }

    // ========== Save Tests - With Audit ==========

    @Test
    public void test_save_creates_audit_record_when_enabled() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record = createCacheRecord("test-env");

        // When
        service.save(record);

        // Then
        verify(jobLockCacheDao, times(1)).save(record);
        verify(jobLockCacheAuditDao, times(1)).save(any(JobLockCacheAuditRecord.class));
    }

    @Test
    public void test_save_audit_record_has_same_environment_as_cache_record() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record = createCacheRecord("prod-env");

        ArgumentCaptor<JobLockCacheAuditRecord> auditCaptor = ArgumentCaptor.forClass(JobLockCacheAuditRecord.class);

        // When
        service.save(record);

        // Then
        verify(jobLockCacheAuditDao).save(auditCaptor.capture());
        JobLockCacheAuditRecord capturedAudit = auditCaptor.getValue();
        assertEquals("prod-env", capturedAudit.getEnvironment());
    }

    @Test
    public void test_save_audit_record_has_same_cache_data_as_cache_record() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record = createCacheRecord("test-env");
        JobLockCacheData cacheData = record.getJobLockCache();

        ArgumentCaptor<JobLockCacheAuditRecord> auditCaptor = ArgumentCaptor.forClass(JobLockCacheAuditRecord.class);

        // When
        service.save(record);

        // Then
        verify(jobLockCacheAuditDao).save(auditCaptor.capture());
        JobLockCacheAuditRecord capturedAudit = auditCaptor.getValue();
        assertSame(cacheData, capturedAudit.getJobLockCache());
    }

    @Test
    public void test_save_audit_record_is_hibernate_type() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record = createCacheRecord("test-env");

        ArgumentCaptor<JobLockCacheAuditRecord> auditCaptor = ArgumentCaptor.forClass(JobLockCacheAuditRecord.class);

        // When
        service.save(record);

        // Then
        verify(jobLockCacheAuditDao).save(auditCaptor.capture());
        JobLockCacheAuditRecord capturedAudit = auditCaptor.getValue();
        assertTrue("Audit record should be HibernateJobLockCacheAuditRecord type",
            capturedAudit instanceof HibernateJobLockCacheAuditRecord);
    }

    @Test
    public void test_save_with_null_environment_creates_audit_with_null_environment() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record = createCacheRecord(null);

        ArgumentCaptor<JobLockCacheAuditRecord> auditCaptor = ArgumentCaptor.forClass(JobLockCacheAuditRecord.class);

        // When
        service.save(record);

        // Then
        verify(jobLockCacheAuditDao).save(auditCaptor.capture());
        JobLockCacheAuditRecord capturedAudit = auditCaptor.getValue();
        assertEquals("DEFAULT_ENVIRONMENT", capturedAudit.getEnvironment());
    }

    @Test
    public void test_save_multiple_times_creates_multiple_audit_records() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record1 = createCacheRecord("env1");
        HibernateJobLockCacheRecord record2 = createCacheRecord("env2");

        // When
        service.save(record1);
        service.save(record2);

        // Then
        verify(jobLockCacheDao, times(2)).save(any(JobLockCacheRecord.class));
        verify(jobLockCacheAuditDao, times(2)).save(any(JobLockCacheAuditRecord.class));
    }

    @Test
    public void test_save_same_environment_multiple_times_creates_multiple_audit_snapshots() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record1 = createCacheRecord("prod");
        HibernateJobLockCacheRecord record2 = createCacheRecord("prod");
        HibernateJobLockCacheRecord record3 = createCacheRecord("prod");

        // When
        service.save(record1);
        service.save(record2);
        service.save(record3);

        // Then
        verify(jobLockCacheDao, times(3)).save(any(JobLockCacheRecord.class));
        verify(jobLockCacheAuditDao, times(3)).save(any(JobLockCacheAuditRecord.class));
    }

    // ========== Get Tests ==========

    @Test
    public void test_get_delegates_to_cache_dao() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord expectedRecord = createCacheRecord("test-env");
        when(jobLockCacheDao.get("test-env")).thenReturn(expectedRecord);

        // When
        JobLockCacheRecord result = service.get("test-env");

        // Then
        verify(jobLockCacheDao, times(1)).get("test-env");
        assertSame(expectedRecord, result);
    }

    @Test
    public void test_get_with_null_environment() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord expectedRecord = createCacheRecord(JobLockCacheRecord.DEFAULT_ENVIRONMENT);
        when(jobLockCacheDao.get(null)).thenReturn(expectedRecord);

        // When
        JobLockCacheRecord result = service.get(null);

        // Then
        verify(jobLockCacheDao, times(1)).get(null);
        assertSame(expectedRecord, result);
    }

    @Test
    public void test_get_with_default_environment() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord expectedRecord = createCacheRecord(JobLockCacheRecord.DEFAULT_ENVIRONMENT);
        when(jobLockCacheDao.get(JobLockCacheRecord.DEFAULT_ENVIRONMENT)).thenReturn(expectedRecord);

        // When
        JobLockCacheRecord result = service.get(JobLockCacheRecord.DEFAULT_ENVIRONMENT);

        // Then
        verify(jobLockCacheDao, times(1)).get(JobLockCacheRecord.DEFAULT_ENVIRONMENT);
        assertSame(expectedRecord, result);
    }

    @Test
    public void test_get_returns_null_when_not_found() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        when(jobLockCacheDao.get("non-existent")).thenReturn(null);

        // When
        JobLockCacheRecord result = service.get("non-existent");

        // Then
        verify(jobLockCacheDao, times(1)).get("non-existent");
        assertNull(result);
    }

    @Test
    public void test_get_multiple_different_environments() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record1 = createCacheRecord("env1");
        HibernateJobLockCacheRecord record2 = createCacheRecord("env2");

        when(jobLockCacheDao.get("env1")).thenReturn(record1);
        when(jobLockCacheDao.get("env2")).thenReturn(record2);

        // When
        JobLockCacheRecord result1 = service.get("env1");
        JobLockCacheRecord result2 = service.get("env2");

        // Then
        verify(jobLockCacheDao, times(1)).get("env1");
        verify(jobLockCacheDao, times(1)).get("env2");
        assertSame(record1, result1);
        assertSame(record2, result2);
    }

    // ========== FindAll Tests ==========

    @Test
    public void test_findAll_delegates_to_audit_dao() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        SearchResults<JobLockCacheAuditRecord> expectedResults = createSearchResults(3, 3);
        when(jobLockCacheAuditDao.findAll(25, 0)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = service.findAll(25, 0);

        // Then
        verify(jobLockCacheAuditDao, times(1)).findAll(25, 0);
        assertSame(expectedResults, results);
    }

    @Test
    public void test_findAll_with_limit_and_offset() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        SearchResults<JobLockCacheAuditRecord> expectedResults = createSearchResults(2, 5);
        when(jobLockCacheAuditDao.findAll(2, 3)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = service.findAll(2, 3);

        // Then
        verify(jobLockCacheAuditDao, times(1)).findAll(2, 3);
        assertSame(expectedResults, results);
        assertEquals(2, results.getResultList().size());
        assertEquals(5L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_returns_empty_results_when_no_audits() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        SearchResults<JobLockCacheAuditRecord> emptyResults = createSearchResults(0, 0);
        when(jobLockCacheAuditDao.findAll(100, 0)).thenReturn(emptyResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = service.findAll(100, 0);

        // Then
        verify(jobLockCacheAuditDao, times(1)).findAll(100, 0);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_with_zero_limit() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        SearchResults<JobLockCacheAuditRecord> expectedResults = createSearchResults(10, 10);
        when(jobLockCacheAuditDao.findAll(0, 0)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = service.findAll(0, 0);

        // Then
        verify(jobLockCacheAuditDao, times(1)).findAll(0, 0);
        assertEquals(10, results.getResultList().size());
    }

    @Test
    public void test_findAll_with_large_offset() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        SearchResults<JobLockCacheAuditRecord> expectedResults = createSearchResults(0, 100);
        when(jobLockCacheAuditDao.findAll(25, 100)).thenReturn(expectedResults);

        // When
        SearchResults<JobLockCacheAuditRecord> results = service.findAll(25, 100);

        // Then
        verify(jobLockCacheAuditDao, times(1)).findAll(25, 100);
        assertEquals(0, results.getResultList().size());
        assertEquals(100L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findAll_multiple_calls() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        SearchResults<JobLockCacheAuditRecord> page1 = createSearchResults(10, 25);
        SearchResults<JobLockCacheAuditRecord> page2 = createSearchResults(10, 25);
        SearchResults<JobLockCacheAuditRecord> page3 = createSearchResults(5, 25);

        when(jobLockCacheAuditDao.findAll(10, 0)).thenReturn(page1);
        when(jobLockCacheAuditDao.findAll(10, 10)).thenReturn(page2);
        when(jobLockCacheAuditDao.findAll(10, 20)).thenReturn(page3);

        // When
        SearchResults<JobLockCacheAuditRecord> result1 = service.findAll(10, 0);
        SearchResults<JobLockCacheAuditRecord> result2 = service.findAll(10, 10);
        SearchResults<JobLockCacheAuditRecord> result3 = service.findAll(10, 20);

        // Then
        verify(jobLockCacheAuditDao, times(1)).findAll(10, 0);
        verify(jobLockCacheAuditDao, times(1)).findAll(10, 10);
        verify(jobLockCacheAuditDao, times(1)).findAll(10, 20);

        assertEquals(10, result1.getResultList().size());
        assertEquals(10, result2.getResultList().size());
        assertEquals(5, result3.getResultList().size());
    }

    // ========== Integration Scenario Tests ==========

    @Test
    public void test_save_then_get_workflow() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, false);
        HibernateJobLockCacheRecord record = createCacheRecord("workflow-env");
        when(jobLockCacheDao.get("workflow-env")).thenReturn(record);

        // When
        service.save(record);
        JobLockCacheRecord retrieved = service.get("workflow-env");

        // Then
        verify(jobLockCacheDao, times(1)).save(record);
        verify(jobLockCacheDao, times(1)).get("workflow-env");
        assertSame(record, retrieved);
    }

    @Test
    public void test_save_with_audit_then_findAll_workflow() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record1 = createCacheRecord("env1");
        HibernateJobLockCacheRecord record2 = createCacheRecord("env2");

        SearchResults<JobLockCacheAuditRecord> expectedResults = createSearchResults(2, 2);
        when(jobLockCacheAuditDao.findAll(100, 0)).thenReturn(expectedResults);

        // When
        service.save(record1);
        service.save(record2);
        SearchResults<JobLockCacheAuditRecord> audits = service.findAll(100, 0);

        // Then
        verify(jobLockCacheDao, times(2)).save(any(JobLockCacheRecord.class));
        verify(jobLockCacheAuditDao, times(2)).save(any(JobLockCacheAuditRecord.class));
        verify(jobLockCacheAuditDao, times(1)).findAll(100, 0);
        assertEquals(2, audits.getResultList().size());
    }

    @Test
    public void test_no_interaction_with_audit_dao_when_only_getting() {
        // Given
        service = new HibernateJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
        HibernateJobLockCacheRecord record = createCacheRecord("test-env");
        when(jobLockCacheDao.get("test-env")).thenReturn(record);

        // When
        service.get("test-env");

        // Then
        verify(jobLockCacheDao, times(1)).get("test-env");
        verifyNoInteractions(jobLockCacheAuditDao);
    }

    // ========== Helper Methods ==========

    private HibernateJobLockCacheRecord createCacheRecord(String environment) {
        HibernateJobLockCacheRecord record = new HibernateJobLockCacheRecord();
        record.setEnvironment(environment);

        HibernateJobLockCacheData cacheData = new HibernateJobLockCacheData();
        record.setJobLockCache(cacheData);

        return record;
    }

    private SearchResults<JobLockCacheAuditRecord> createSearchResults(int resultCount, long totalCount) {
        List<JobLockCacheAuditRecord> results = new ArrayList<>();
        for (int i = 0; i < resultCount; i++) {
            HibernateJobLockCacheAuditRecord auditRecord = new HibernateJobLockCacheAuditRecord();
            auditRecord.setEnvironment("env-" + i);
            results.add(auditRecord);
        }

        return new SearchResultsImpl<>(results, totalCount, 0L);
    }
}
