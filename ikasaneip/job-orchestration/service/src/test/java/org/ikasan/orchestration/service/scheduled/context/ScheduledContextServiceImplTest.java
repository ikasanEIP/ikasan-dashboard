package org.ikasan.orchestration.service.scheduled.context;

import org.ikasan.job.orchestration.model.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
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
 * Unit tests for ScheduledContextServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledContextServiceImplTest {

    @Mock
    private ScheduledContextDao mockDao;

    @Mock
    private ScheduledContextViewDao mockViewDao;

    @Mock
    private ScheduledContextRecord mockRecord;

    @Mock
    private ScheduledContextViewRecord mockViewRecord;

    @Mock
    private ScheduledContextSearchFilter mockFilter;

    @Mock
    private ContextTemplate mockContextTemplate;

    private ScheduledContextServiceImpl service;

    @Before
    public void setUp() {
        service = new ScheduledContextServiceImpl(mockDao, mockViewDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ScheduledContextServiceImpl(null, mockViewDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullViewDao() {
        new ScheduledContextServiceImpl(mockDao, null);
    }

    @Test
    public void testFindAll() {
        // Given
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findAll()).thenReturn(expectedResults);

        // When
        SearchResults<? extends ScheduledContextRecord> results = service.findAll();

        // Then
        assertNotNull(results);
        verify(mockDao).findAll();
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findAll(10, 0)).thenReturn(expectedResults);

        // When
        SearchResults<? extends ScheduledContextRecord> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        verify(mockDao).findAll(10, 0);
    }

    @Test
    public void testFindByFilter() {
        // Given
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecord> results =
            service.findByFilter(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        verify(mockDao).findByFilter(mockFilter, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testFindByFilterLite() {
        // Given
        when(mockRecord.getId()).thenReturn("ctx-1");
        when(mockRecord.getContextName()).thenReturn("test-context");
        when(mockRecord.getContext()).thenReturn(mockContextTemplate);
        when(mockContextTemplate.getDescription()).thenReturn("Test description");
        when(mockRecord.getTimestamp()).thenReturn(123456L);
        when(mockRecord.getModifiedTimestamp()).thenReturn(123456L);
        when(mockRecord.getModifiedBy()).thenReturn("user1");
        when(mockRecord.isDisabled()).thenReturn(false);
        when(mockRecord.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(false);

        List<ScheduledContextRecord> records = Arrays.asList(mockRecord);
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals(100L, results.getQueryResponseTime());

        ScheduledContextRecordLite lite = results.getResultList().get(0);
        assertEquals("ctx-1", lite.getId());
        assertEquals("test-context", lite.getContextName());
        assertEquals("Test description", lite.getDescription());
        assertEquals(123456L, lite.getTimestamp());
        assertEquals("user1", lite.getModifiedBy());
        assertEquals(123456L, lite.getModifiedTimestamp());
        assertFalse(lite.isDisabled());
        assertFalse(lite.isQuartzScheduleDrivenJobsDisabledForContext());

        verify(mockDao).findByFilter(mockFilter, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("ctx-1")).thenReturn(mockRecord);

        // When
        ScheduledContextRecord result = service.findById("ctx-1");

        // Then
        assertNotNull(result);
        verify(mockDao).findById("ctx-1");
    }

    @Test
    public void testFindByName() {
        // Given
        when(mockDao.findByName("test-context")).thenReturn(mockRecord);

        // When
        ScheduledContextRecord result = service.findByName("test-context");

        // Then
        assertNotNull(result);
        verify(mockDao).findByName("test-context");
    }

    @Test
    public void testSave() {
        // When
        service.save(mockRecord);

        // Then
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void testGetContextView() {
        // Given
        when(mockViewDao.getContextView("parent-ctx", "child-ctx")).thenReturn(mockViewRecord);

        // When
        ScheduledContextViewRecord result = service.getContextView("parent-ctx", "child-ctx");

        // Then
        assertNotNull(result);
        verify(mockViewDao).getContextView("parent-ctx", "child-ctx");
    }

    @Test
    public void testSaveContextView() {
        // When
        service.saveContextView(mockViewRecord);

        // Then
        verify(mockViewDao).save(mockViewRecord);
    }

    @Test
    public void testDeleteContext() {
        // When
        service.deleteContext("test-context");

        // Then
        verify(mockDao).deleteContext("test-context");
    }

    @Test
    public void testCloneContext() {
        // When
        ScheduledContextRecord result = service.cloneContext("original-ctx", "cloned-ctx");

        // Then
        assertNull(result); // Implementation returns null
        verifyNoInteractions(mockDao, mockViewDao);
    }

    @Test
    public void testEnableScheduledJobs() {
        // Given
        when(mockContextTemplate.getName()).thenReturn("test-context");
        when(mockDao.findByName("test-context")).thenReturn(mockRecord);

        // When
        service.enableScheduledJobs(mockContextTemplate, "user1");

        // Then
        verify(mockContextTemplate).setQuartzScheduleDrivenJobsDisabledForContext(false);
        verify(mockDao).findByName("test-context");
        verify(mockRecord).setContext(mockContextTemplate);
        verify(mockRecord).setModifiedBy("user1");
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void testDisableScheduledJobs() {
        // Given
        when(mockContextTemplate.getName()).thenReturn("test-context");
        when(mockDao.findByName("test-context")).thenReturn(mockRecord);

        // When
        service.disableScheduledJobs(mockContextTemplate, "user1");

        // Then
        verify(mockContextTemplate).setQuartzScheduleDrivenJobsDisabledForContext(true);
        verify(mockDao).findByName("test-context");
        verify(mockRecord).setContext(mockContextTemplate);
        verify(mockRecord).setModifiedBy("user1");
        verify(mockDao).save(mockRecord);
    }

    @Test
    public void testFindByFilterLiteWithMultipleRecords() {
        // Given
        ScheduledContextRecord mockRecord2 = mock(ScheduledContextRecord.class);
        ContextTemplate mockContextTemplate2 = mock(ContextTemplate.class);

        when(mockRecord.getId()).thenReturn("ctx-1");
        when(mockRecord.getContextName()).thenReturn("context-1");
        when(mockRecord.getContext()).thenReturn(mockContextTemplate);
        when(mockContextTemplate.getDescription()).thenReturn("First context");
        when(mockRecord.getModifiedTimestamp()).thenReturn(111111L);
        when(mockRecord.getModifiedBy()).thenReturn("user1");
        when(mockRecord.isDisabled()).thenReturn(false);
        when(mockRecord.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(false);

        when(mockRecord2.getId()).thenReturn("ctx-2");
        when(mockRecord2.getContextName()).thenReturn("context-2");
        when(mockRecord2.getContext()).thenReturn(mockContextTemplate2);
        when(mockContextTemplate2.getDescription()).thenReturn("Second context");
        when(mockRecord2.getModifiedTimestamp()).thenReturn(222222L);
        when(mockRecord2.getModifiedBy()).thenReturn("user2");
        when(mockRecord2.isDisabled()).thenReturn(true);
        when(mockRecord2.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(true);

        List<ScheduledContextRecord> records = Arrays.asList(mockRecord, mockRecord2);
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 2L, 150L);
        when(mockDao.findByFilter(mockFilter, 20, 5, "contextName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 20, 5, "contextName", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(2, results.getResultList().size());
        assertEquals(2L, results.getTotalNumberOfResults());
        assertEquals(150L, results.getQueryResponseTime());

        ScheduledContextRecordLite lite1 = results.getResultList().get(0);
        assertEquals("ctx-1", lite1.getId());
        assertEquals("context-1", lite1.getContextName());
        assertEquals("First context", lite1.getDescription());
        assertEquals("user1", lite1.getModifiedBy());
        assertFalse(lite1.isDisabled());

        ScheduledContextRecordLite lite2 = results.getResultList().get(1);
        assertEquals("ctx-2", lite2.getId());
        assertEquals("context-2", lite2.getContextName());
        assertEquals("Second context", lite2.getDescription());
        assertEquals("user2", lite2.getModifiedBy());
        assertTrue(lite2.isDisabled());
        assertTrue(lite2.isQuartzScheduleDrivenJobsDisabledForContext());

        verify(mockDao).findByFilter(mockFilter, 20, 5, "contextName", "ASC");
    }

    @Test
    public void testFindByFilterLiteWithEmptyResults() {
        // Given
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
        assertEquals(50L, results.getQueryResponseTime());

        verify(mockDao).findByFilter(mockFilter, 10, 0, "timestamp", "DESC");
    }

    /**
     * Test that findByFilterLite correctly uses getTimestamp() instead of getModifiedTimestamp()
     * for the timestamp field in ScheduledContextRecordLite.
     * This is a regression test for IKASAN-2803.
     */
    @Test
    public void testFindByFilterLiteUsesCorrectTimestampField() {
        // Given - two different timestamp values to verify correct one is used
        long contextTimestamp = 999999L;
        long modifiedTimestamp = 123456L;

        when(mockRecord.getId()).thenReturn("ctx-1");
        when(mockRecord.getContextName()).thenReturn("test-context");
        when(mockRecord.getContext()).thenReturn(mockContextTemplate);
        when(mockContextTemplate.getDescription()).thenReturn("Test description");
        when(mockRecord.getTimestamp()).thenReturn(contextTimestamp);
        when(mockRecord.getModifiedTimestamp()).thenReturn(modifiedTimestamp);
        when(mockRecord.getModifiedBy()).thenReturn("user1");
        when(mockRecord.isDisabled()).thenReturn(false);
        when(mockRecord.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(false);

        List<ScheduledContextRecord> records = Arrays.asList(mockRecord);
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());

        ScheduledContextRecordLite lite = results.getResultList().get(0);
        // Verify that timestamp uses getTimestamp(), not getModifiedTimestamp()
        assertEquals("timestamp field should use getTimestamp()", contextTimestamp, lite.getTimestamp());
        assertEquals("modifiedTimestamp field should use getModifiedTimestamp()", modifiedTimestamp, lite.getModifiedTimestamp());

        verify(mockRecord).getTimestamp();
        verify(mockRecord).getModifiedTimestamp();
    }

    /**
     * Test that findByFilterLite correctly maps all fields when timestamp and modifiedTimestamp
     * have the same value (edge case).
     */
    @Test
    public void testFindByFilterLiteWhenTimestampsAreEqual() {
        // Given
        long sameTimestamp = 555555L;

        when(mockRecord.getId()).thenReturn("ctx-1");
        when(mockRecord.getContextName()).thenReturn("test-context");
        when(mockRecord.getContext()).thenReturn(mockContextTemplate);
        when(mockContextTemplate.getDescription()).thenReturn("Test description");
        when(mockRecord.getTimestamp()).thenReturn(sameTimestamp);
        when(mockRecord.getModifiedTimestamp()).thenReturn(sameTimestamp);
        when(mockRecord.getModifiedBy()).thenReturn("user1");
        when(mockRecord.isDisabled()).thenReturn(false);
        when(mockRecord.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(false);

        List<ScheduledContextRecord> records = Arrays.asList(mockRecord);
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());

        ScheduledContextRecordLite lite = results.getResultList().get(0);
        assertEquals(sameTimestamp, lite.getTimestamp());
        assertEquals(sameTimestamp, lite.getModifiedTimestamp());

        verify(mockRecord).getTimestamp();
        verify(mockRecord).getModifiedTimestamp();
    }

    /**
     * Test that findByFilterLite handles zero timestamp (e.g., uninitialized) gracefully.
     */
    @Test
    public void testFindByFilterLiteWithZeroTimestamp() {
        // Given
        when(mockRecord.getId()).thenReturn("ctx-1");
        when(mockRecord.getContextName()).thenReturn("test-context");
        when(mockRecord.getContext()).thenReturn(mockContextTemplate);
        when(mockContextTemplate.getDescription()).thenReturn("Test description");
        when(mockRecord.getTimestamp()).thenReturn(0L);
        when(mockRecord.getModifiedTimestamp()).thenReturn(123456L);
        when(mockRecord.getModifiedBy()).thenReturn("user1");
        when(mockRecord.isDisabled()).thenReturn(false);
        when(mockRecord.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(false);

        List<ScheduledContextRecord> records = Arrays.asList(mockRecord);
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());

        ScheduledContextRecordLite lite = results.getResultList().get(0);
        assertEquals("timestamp should be 0 when source is 0", 0L, lite.getTimestamp());
        assertEquals(123456L, lite.getModifiedTimestamp());

        verify(mockRecord).getTimestamp();
    }

    /**
     * Test that findByFilterLite correctly uses timestamp field for multiple records with varying values.
     */
    @Test
    public void testFindByFilterLiteWithMultipleRecordsAndDifferentTimestamps() {
        // Given
        ScheduledContextRecord mockRecord2 = mock(ScheduledContextRecord.class);
        ContextTemplate mockContextTemplate2 = mock(ContextTemplate.class);

        // First record with older timestamp, newer modified timestamp
        when(mockRecord.getId()).thenReturn("ctx-1");
        when(mockRecord.getContextName()).thenReturn("context-1");
        when(mockRecord.getContext()).thenReturn(mockContextTemplate);
        when(mockContextTemplate.getDescription()).thenReturn("First context");
        when(mockRecord.getTimestamp()).thenReturn(100000L);
        when(mockRecord.getModifiedTimestamp()).thenReturn(999999L);
        when(mockRecord.getModifiedBy()).thenReturn("user1");
        when(mockRecord.isDisabled()).thenReturn(false);
        when(mockRecord.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(false);

        // Second record with newer timestamp, older modified timestamp
        when(mockRecord2.getId()).thenReturn("ctx-2");
        when(mockRecord2.getContextName()).thenReturn("context-2");
        when(mockRecord2.getContext()).thenReturn(mockContextTemplate2);
        when(mockContextTemplate2.getDescription()).thenReturn("Second context");
        when(mockRecord2.getTimestamp()).thenReturn(999999L);
        when(mockRecord2.getModifiedTimestamp()).thenReturn(100000L);
        when(mockRecord2.getModifiedBy()).thenReturn("user2");
        when(mockRecord2.isDisabled()).thenReturn(true);
        when(mockRecord2.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(true);

        List<ScheduledContextRecord> records = Arrays.asList(mockRecord, mockRecord2);
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 2L, 150L);
        when(mockDao.findByFilter(mockFilter, 20, 5, "timestamp", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 20, 5, "timestamp", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(2, results.getResultList().size());

        ScheduledContextRecordLite lite1 = results.getResultList().get(0);
        assertEquals("ctx-1", lite1.getId());
        assertEquals(100000L, lite1.getTimestamp());
        assertEquals(999999L, lite1.getModifiedTimestamp());

        ScheduledContextRecordLite lite2 = results.getResultList().get(1);
        assertEquals("ctx-2", lite2.getId());
        assertEquals(999999L, lite2.getTimestamp());
        assertEquals(100000L, lite2.getModifiedTimestamp());

        verify(mockRecord).getTimestamp();
        verify(mockRecord2).getTimestamp();
    }

    /**
     * Test that findByFilterLite correctly maps timestamp field for disabled contexts.
     */
    @Test
    public void testFindByFilterLiteUsesCorrectTimestampFieldForDisabledContext() {
        // Given
        long contextTimestamp = 777777L;
        long modifiedTimestamp = 888888L;

        when(mockRecord.getId()).thenReturn("ctx-1");
        when(mockRecord.getContextName()).thenReturn("disabled-context");
        when(mockRecord.getContext()).thenReturn(mockContextTemplate);
        when(mockContextTemplate.getDescription()).thenReturn("Disabled context");
        when(mockRecord.getTimestamp()).thenReturn(contextTimestamp);
        when(mockRecord.getModifiedTimestamp()).thenReturn(modifiedTimestamp);
        when(mockRecord.getModifiedBy()).thenReturn("admin");
        when(mockRecord.isDisabled()).thenReturn(true);
        when(mockRecord.isQuartzScheduleDrivenJobsDisabledForContext()).thenReturn(true);

        List<ScheduledContextRecord> records = Arrays.asList(mockRecord);
        SearchResults<ScheduledContextRecord> expectedResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockDao.findByFilter(mockFilter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<ScheduledContextRecordLite> results =
            service.findByFilterLite(mockFilter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());

        ScheduledContextRecordLite lite = results.getResultList().get(0);
        assertTrue("context should be disabled", lite.isDisabled());
        assertTrue("scheduled jobs should be disabled", lite.isQuartzScheduleDrivenJobsDisabledForContext());
        // Verify correct timestamp is used even for disabled contexts
        assertEquals("timestamp field should use getTimestamp()", contextTimestamp, lite.getTimestamp());
        assertEquals("modifiedTimestamp field should use getModifiedTimestamp()", modifiedTimestamp, lite.getModifiedTimestamp());

        verify(mockRecord).getTimestamp();
        verify(mockRecord).getModifiedTimestamp();
    }
}
