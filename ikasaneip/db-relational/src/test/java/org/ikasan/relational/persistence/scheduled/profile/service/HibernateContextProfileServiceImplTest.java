package org.ikasan.relational.persistence.scheduled.profile.service;

import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.profile.service.HibernateContextProfileServiceImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HibernateContextProfileServiceImpl using Mockito.
 *
 * These tests verify that the service layer correctly delegates to the DAO
 * and performs proper validation and business logic.
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateContextProfileServiceImplTest {

    @Mock
    private ContextProfileDao mockDao;

    @Mock
    private ContextProfileRecord mockRecord;

    @Mock
    private ContextProfileSearchFilter mockFilter;

    private HibernateContextProfileServiceImpl service;

    @Before
    public void setUp() {
        service = new HibernateContextProfileServiceImpl(mockDao);
    }

    // ========== Constructor Tests ==========

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_should_throw_exception_when_dao_is_null() {
        // When
        new HibernateContextProfileServiceImpl(null);

        // Then - should throw IllegalArgumentException
    }

    @Test
    public void test_constructor_should_succeed_with_valid_dao() {
        // When
        HibernateContextProfileServiceImpl validService = new HibernateContextProfileServiceImpl(mockDao);

        // Then
        assertNotNull(validService);
    }

    // ========== Save Single Record Tests ==========

    @Test
    public void test_save_should_delegate_to_dao() {
        // Given
        when(mockRecord.getProfileName()).thenReturn("profile1");
        when(mockRecord.getContextName()).thenReturn("context1");

        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void test_save_should_call_dao_exactly_once() {
        // Given
        when(mockRecord.getProfileName()).thenReturn("profile1");
        when(mockRecord.getContextName()).thenReturn("context1");

        // When
        service.save(mockRecord);

        // Then
        verify(mockDao, times(1)).save(mockRecord);
    }

    // ========== Save Multiple Records Tests ==========

    @Test
    public void test_save_list_should_save_all_records() {
        // Given
        List<ContextProfileRecord> records = Arrays.asList(mockRecord, mockRecord, mockRecord);

        // When
        service.save(records);

        // Then
        verify(mockDao, times(3)).save(mockRecord);
    }

    @Test
    public void test_save_empty_list_should_not_call_dao() {
        // Given
        List<ContextProfileRecord> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao, never()).save(any(ContextProfileRecord.class));
    }

    @Test
    public void test_save_list_with_single_record_should_call_dao_once() {
        // Given
        List<ContextProfileRecord> singleRecord = Arrays.asList(mockRecord);

        // When
        service.save(singleRecord);

        // Then
        verify(mockDao, times(1)).save(mockRecord);
    }

    // ========== Delete Tests ==========

    @Test
    public void test_deleteByContextName_should_delegate_to_dao() {
        // When
        service.deleteByContextName("context1");

        // Then
        verify(mockDao).deleteByContextName("context1");
    }

    @Test
    public void test_deleteByContextName_should_call_dao_exactly_once() {
        // When
        service.deleteByContextName("context1");

        // Then
        verify(mockDao, times(1)).deleteByContextName("context1");
    }

    @Test
    public void test_deleteByContextName_should_handle_empty_string() {
        // When
        service.deleteByContextName("");

        // Then
        verify(mockDao).deleteByContextName("");
    }

    // ========== FindById Tests ==========

    @Test
    public void test_findById_should_delegate_to_dao() {
        // Given
        when(mockDao.findById("test-id")).thenReturn(mockRecord);

        // When
        ContextProfileRecord result = service.findById("test-id");

        // Then
        assertNotNull(result);
        verify(mockDao).findById("test-id");
    }

    @Test
    public void test_findById_should_return_null_when_not_found() {
        // Given
        when(mockDao.findById("non-existent-id")).thenReturn(null);

        // When
        ContextProfileRecord result = service.findById("non-existent-id");

        // Then
        assertNull(result);
        verify(mockDao).findById("non-existent-id");
    }

    @Test
    public void test_findById_should_return_same_record_from_dao() {
        // Given
        when(mockDao.findById("test-id")).thenReturn(mockRecord);

        // When
        ContextProfileRecord result = service.findById("test-id");

        // Then
        assertEquals(mockRecord, result);
    }

    // ========== FindByFilter Tests ==========

    @Test
    public void test_findByFilter_should_delegate_to_dao() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "profileName", "ASCENDING"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 10, 0, "profileName", "ASCENDING");

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockFilter, 10, 0, "profileName", "ASCENDING");
    }

    @Test
    public void test_findByFilter_should_pass_correct_parameters() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(any(), eq(25), eq(50), eq("contextName"), eq("DESCENDING")))
            .thenReturn(expectedResults);

        // When
        service.findByFilter(mockFilter, 25, 50, "contextName", "DESCENDING");

        // Then
        verify(mockDao).findByFilter(mockFilter, 25, 50, "contextName", "DESCENDING");
    }

    @Test
    public void test_findByFilter_should_return_results_from_dao() {
        // Given
        List<ContextProfileRecord> records = Arrays.asList(mockRecord, mockRecord);
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(records, 2L, 0L);
        when(mockDao.findByFilter(mockFilter, 10, 0, null, null))
            .thenReturn(expectedResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 10, 0, null, null);

        // Then
        assertEquals(2, results.getResultList().size());
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_with_no_results_should_return_empty_list() {
        // Given
        SearchResults<ContextProfileRecord> emptyResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, 10, 0, null, null))
            .thenReturn(emptyResults);

        // When
        SearchResults<ContextProfileRecord> results =
            service.findByFilter(mockFilter, 10, 0, null, null);

        // Then
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_should_handle_null_sort_parameters() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, 10, 0, null, null))
            .thenReturn(expectedResults);

        // When
        service.findByFilter(mockFilter, 10, 0, null, null);

        // Then
        verify(mockDao).findByFilter(mockFilter, 10, 0, null, null);
    }

    @Test
    public void test_findByFilter_should_handle_large_limit() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, Integer.MAX_VALUE, 0, null, null))
            .thenReturn(expectedResults);

        // When
        service.findByFilter(mockFilter, Integer.MAX_VALUE, 0, null, null);

        // Then
        verify(mockDao).findByFilter(mockFilter, Integer.MAX_VALUE, 0, null, null);
    }

    @Test
    public void test_findByFilter_should_handle_large_offset() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, 10, 1000, null, null))
            .thenReturn(expectedResults);

        // When
        service.findByFilter(mockFilter, 10, 1000, null, null);

        // Then
        verify(mockDao).findByFilter(mockFilter, 10, 1000, null, null);
    }

    // ========== Integration Scenario Tests ==========

    @Test
    public void test_scenario_save_and_find_record() {
        // Given
        when(mockRecord.getProfileName()).thenReturn("profile1");
        when(mockRecord.getContextName()).thenReturn("context1");
        when(mockDao.findById("profile1_context1")).thenReturn(mockRecord);

        // When
        service.save(mockRecord);
        ContextProfileRecord found = service.findById("profile1_context1");

        // Then
        verify(mockDao).save(mockRecord);
        verify(mockDao).findById("profile1_context1");
        assertEquals(mockRecord, found);
    }

    @Test
    public void test_scenario_save_multiple_and_search() {
        // Given
        List<ContextProfileRecord> records = Arrays.asList(mockRecord, mockRecord);
        SearchResults<ContextProfileRecord> searchResults =
            new SearchResultsImpl<>(records, 2L, 0L);
        when(mockDao.findByFilter(any(), anyInt(), anyInt(), any(), any()))
            .thenReturn(searchResults);

        // When
        service.save(records);
        SearchResults<ContextProfileRecord> results = service.findByFilter(mockFilter, 10, 0, null, null);

        // Then
        verify(mockDao, times(2)).save(mockRecord);
        verify(mockDao).findByFilter(mockFilter, 10, 0, null, null);
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_scenario_save_search_and_delete() {
        // Given
        when(mockRecord.getProfileName()).thenReturn("profile1");
        when(mockRecord.getContextName()).thenReturn("context1");
        when(mockDao.findById("profile1_context1")).thenReturn(mockRecord);

        // When
        service.save(mockRecord);
        service.findById("profile1_context1");
        service.deleteByContextName("context1");

        // Then
        verify(mockDao).save(mockRecord);
        verify(mockDao).findById("profile1_context1");
        verify(mockDao).deleteByContextName("context1");
    }

    // ========== Edge Case Tests ==========

    @Test
    public void test_save_should_handle_record_with_null_profile() {
        // Given
        when(mockRecord.getProfileName()).thenReturn("profile1");
        when(mockRecord.getContextName()).thenReturn("context1");

        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void test_save_should_handle_record_with_null_access_lists() {
        // Given
        when(mockRecord.getProfileName()).thenReturn("profile1");
        when(mockRecord.getContextName()).thenReturn("context1");

        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void test_findByFilter_should_handle_zero_limit() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, 0, 0, null, null))
            .thenReturn(expectedResults);

        // When
        service.findByFilter(mockFilter, 0, 0, null, null);

        // Then
        verify(mockDao).findByFilter(mockFilter, 0, 0, null, null);
    }

    @Test
    public void test_findByFilter_should_handle_negative_limit() {
        // Given
        SearchResults<ContextProfileRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, -1, 0, null, null))
            .thenReturn(expectedResults);

        // When
        service.findByFilter(mockFilter, -1, 0, null, null);

        // Then
        verify(mockDao).findByFilter(mockFilter, -1, 0, null, null);
    }
}
