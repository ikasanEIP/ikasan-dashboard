package org.ikasan.orchestration.service.scheduled.instance;

import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchedulerJobInstanceServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class SchedulerJobInstanceServiceImplTest {

    @Mock
    private SchedulerJobInstanceDao mockJobInstanceDao;

    @Mock
    private ScheduledContextInstanceAuditAggregateDao mockAuditAggregateDao;

    @Mock
    private SchedulerJobDao mockJobDao;

    @Mock
    private ScheduledContextInstanceService mockContextInstanceService;

    @Mock
    private SchedulerJobInstanceRecord mockJobInstanceRecord;

    @Mock
    private SchedulerJobInstance mockJobInstance;

    @Mock
    private InternalEventDrivenJobInstance mockInternalEventJobInstance;

    @Mock
    private ScheduledContextInstanceRecord mockScheduledContextInstanceRecord;

    private Map<String, String> executionEnvironmentLabel;
    private SchedulerJobInstanceServiceImpl service;
    private SchedulerJobInstanceServiceImpl serviceWithLegacyCount;

    @Before
    public void setUp() {
        executionEnvironmentLabel = new HashMap<>();
        executionEnvironmentLabel.put("dev", "Development Environment");
        executionEnvironmentLabel.put("prod", "Production Environment");

        service = new SchedulerJobInstanceServiceImpl(
            mockJobInstanceDao,
            mockAuditAggregateDao,
            mockJobDao,
            mockContextInstanceService,
            executionEnvironmentLabel,
            false
        );

        serviceWithLegacyCount = new SchedulerJobInstanceServiceImpl(
            mockJobInstanceDao,
            mockAuditAggregateDao,
            mockJobDao,
            mockContextInstanceService,
            executionEnvironmentLabel,
            true
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullJobInstanceDao() {
        new SchedulerJobInstanceServiceImpl(
            null,
            mockAuditAggregateDao,
            mockJobDao,
            mockContextInstanceService,
            executionEnvironmentLabel,
            false
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullAuditAggregateDao() {
        new SchedulerJobInstanceServiceImpl(
            mockJobInstanceDao,
            null,
            mockJobDao,
            mockContextInstanceService,
            executionEnvironmentLabel,
            false
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullJobDao() {
        new SchedulerJobInstanceServiceImpl(
            mockJobInstanceDao,
            mockAuditAggregateDao,
            null,
            mockContextInstanceService,
            executionEnvironmentLabel,
            false
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullContextInstanceService() {
        new SchedulerJobInstanceServiceImpl(
            mockJobInstanceDao,
            mockAuditAggregateDao,
            mockJobDao,
            null,
            executionEnvironmentLabel,
            false
        );
    }

    @Test
    public void testFindById() {
        // Given
        when(mockJobInstanceDao.findById("job-instance-1")).thenReturn(mockJobInstanceRecord);

        // When
        SchedulerJobInstanceRecord result = service.findById("job-instance-1");

        // Then
        assertNotNull(result);
        assertEquals(mockJobInstanceRecord, result);
        verify(mockJobInstanceDao).findById("job-instance-1");
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockJobInstanceDao.findById("non-existent")).thenReturn(null);

        // When
        SchedulerJobInstanceRecord result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockJobInstanceDao).findById("non-existent");
    }

    @Test
    public void testFindByContextIdJobNameChildContextName() {
        // Given
        List<SchedulerJobInstanceRecord> records = Arrays.asList(mockJobInstanceRecord);
        SearchResults<SchedulerJobInstanceRecord> searchResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            any(SchedulerJobInstanceSearchFilter.class), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        SchedulerJobInstanceRecord result = service.findByContextIdJobNameChildContextName(
            "ctx-uuid-1", "test-job", "child-ctx");

        // Then
        assertNotNull(result);
        assertEquals(mockJobInstanceRecord, result);

        ArgumentCaptor<SchedulerJobInstanceSearchFilter> filterCaptor =
            ArgumentCaptor.forClass(SchedulerJobInstanceSearchFilter.class);
        verify(mockJobInstanceDao).getScheduledContextInstancesByFilter(
            filterCaptor.capture(), eq(1), eq(0), isNull(), isNull());

        SchedulerJobInstanceSearchFilter capturedFilter = filterCaptor.getValue();
        assertEquals("child-ctx", capturedFilter.getChildContextName());
        assertEquals("test-job", capturedFilter.getJobName());
        assertEquals("ctx-uuid-1", capturedFilter.getContextInstanceId());
    }

    @Test
    public void testFindByContextIdJobNameChildContextNameReturnsNull() {
        // Given
        SearchResults<SchedulerJobInstanceRecord> searchResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            any(SchedulerJobInstanceSearchFilter.class), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        SchedulerJobInstanceRecord result = service.findByContextIdJobNameChildContextName(
            "ctx-uuid-1", "test-job", "child-ctx");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveSingleRecord() {
        // When
        service.save(mockJobInstanceRecord);

        // Then
        verify(mockJobInstanceDao).save(mockJobInstanceRecord);
    }

    @Test
    public void testSaveMultipleRecords() {
        // Given
        List<SchedulerJobInstanceRecord> records = Arrays.asList(
            mockJobInstanceRecord,
            mock(SchedulerJobInstanceRecord.class),
            mock(SchedulerJobInstanceRecord.class)
        );

        // When
        service.save(records);

        // Then
        verify(mockJobInstanceDao).save(records);
    }

    @Test
    public void testUpdate() {
        // Given
        when(mockJobInstance.getContextInstanceId()).thenReturn("ctx-instance-1");
        when(mockJobInstance.getJobName()).thenReturn("test-job");
        when(mockJobInstance.getChildContextName()).thenReturn("child-ctx");
        when(mockJobInstance.getStatus()).thenReturn(InstanceStatus.COMPLETE);
        when(mockJobInstance.isHeld()).thenReturn(false);
        when(mockJobInstance.getScheduledProcessEvent()).thenReturn(null);

        SchedulerJobInstance persistedInstance = mock(SchedulerJobInstance.class);
        when(mockJobInstanceRecord.getSchedulerJobInstance()).thenReturn(persistedInstance);

        List<SchedulerJobInstanceRecord> records = Arrays.asList(mockJobInstanceRecord);
        SearchResults<SchedulerJobInstanceRecord> searchResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            any(SchedulerJobInstanceSearchFilter.class), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        service.update(mockJobInstance);

        // Then
        verify(mockJobInstanceRecord).setStatus("COMPLETE");
        verify(persistedInstance).setScheduledProcessEvent(null);
        verify(persistedInstance).setStatus(InstanceStatus.COMPLETE);
        verify(persistedInstance).setHeld(false);
        verify(mockJobInstanceRecord).setSchedulerJobInstance(persistedInstance);
        verify(mockJobInstanceRecord).setModifiedTimestamp(anyLong());
        verify(mockJobInstanceRecord).setModifiedBy("ContextMachine");
        verify(mockJobInstanceDao).save(mockJobInstanceRecord);
    }

    @Test
    public void testGetSchedulerJobInstancesByContextInstanceId() {
        // Given
        SearchResults<SchedulerJobInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockJobInstanceRecord), 1L, 100L);
        when(mockJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-instance-1", 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobInstanceRecord> results =
            service.getSchedulerJobInstancesByContextInstanceId(
                "ctx-instance-1", 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockJobInstanceDao).getSchedulerJobInstancesByContextInstanceId(
            "ctx-instance-1", 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetSchedulerJobInstancesByContextName() {
        // Given
        SearchResults<SchedulerJobInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockJobInstanceRecord), 1L, 100L);
        when(mockJobInstanceDao.getSchedulerJobInstancesByContextName(
            "test-context", 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobInstanceRecord> results =
            service.getSchedulerJobInstancesByContextName(
                "test-context", 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockJobInstanceDao).getSchedulerJobInstancesByContextName(
            "test-context", 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetScheduledContextInstancesByFilter() {
        // Given
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        SearchResults<SchedulerJobInstanceRecord> expectedResults =
            new SearchResultsImpl<>(Arrays.asList(mockJobInstanceRecord), 1L, 100L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            filter, 10, 0, "timestamp", "DESC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobInstanceRecord> results =
            service.getScheduledContextInstancesByFilter(filter, 10, 0, "timestamp", "DESC");

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        verify(mockJobInstanceDao).getScheduledContextInstancesByFilter(
            filter, 10, 0, "timestamp", "DESC");
    }

    @Test
    public void testGetCommandExecutionJobsForContextInstance() {
        // Given
        when(mockInternalEventJobInstance.getIdentifier()).thenReturn("job-identifier-1");
        when(mockJobInstanceRecord.getSchedulerJobInstance()).thenReturn(mockInternalEventJobInstance);

        List<SchedulerJobInstanceRecord> records = Arrays.asList(mockJobInstanceRecord);
        SearchResults<SchedulerJobInstanceRecord> searchResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            any(SchedulerJobInstanceSearchFilter.class), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        Map<String, InternalEventDrivenJobInstance> result =
            service.getCommandExecutionJobsForContextInstance("ctx-instance-1");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("job-identifier-1"));
        assertEquals(mockInternalEventJobInstance, result.get("job-identifier-1"));

        ArgumentCaptor<SchedulerJobInstanceSearchFilter> filterCaptor =
            ArgumentCaptor.forClass(SchedulerJobInstanceSearchFilter.class);
        verify(mockJobInstanceDao).getScheduledContextInstancesByFilter(
            filterCaptor.capture(), eq(-1), eq(-1), isNull(), isNull());

        SchedulerJobInstanceSearchFilter capturedFilter = filterCaptor.getValue();
        assertEquals("ctx-instance-1", capturedFilter.getContextInstanceId());
        assertEquals("internalEventDrivenJobInstance", capturedFilter.getJobType());
    }

    @Test
    public void testGetCommandExecutionJobsForContextInstanceChildContext() {
        // Given
        when(mockInternalEventJobInstance.getIdentifier()).thenReturn("job-identifier-1");
        when(mockInternalEventJobInstance.getChildContextName()).thenReturn("child-ctx-1");
        when(mockJobInstanceRecord.getSchedulerJobInstance()).thenReturn(mockInternalEventJobInstance);

        List<SchedulerJobInstanceRecord> records = Arrays.asList(mockJobInstanceRecord);
        SearchResults<SchedulerJobInstanceRecord> searchResults =
            new SearchResultsImpl<>(records, 1L, 100L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            any(SchedulerJobInstanceSearchFilter.class), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        Map<String, InternalEventDrivenJobInstance> result =
            service.getCommandExecutionJobsForContextInstanceChildContext("ctx-instance-1");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("job-identifier-1-child-ctx-1"));
        assertEquals(mockInternalEventJobInstance, result.get("job-identifier-1-child-ctx-1"));

        ArgumentCaptor<SchedulerJobInstanceSearchFilter> filterCaptor =
            ArgumentCaptor.forClass(SchedulerJobInstanceSearchFilter.class);
        verify(mockJobInstanceDao).getScheduledContextInstancesByFilter(
            filterCaptor.capture(), eq(-1), eq(-1), isNull(), isNull());

        SchedulerJobInstanceSearchFilter capturedFilter = filterCaptor.getValue();
        assertEquals("ctx-instance-1", capturedFilter.getContextInstanceId());
        assertEquals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE, capturedFilter.getJobType());
    }

    @Test
    public void testGetJobStatusCountForContextInstancesWithLegacyCount() {
        // Given
        List<String> contextInstanceIds = Arrays.asList("ctx-1", "ctx-2");
        ContextInstanceAggregateJobStatus status1 = mock(ContextInstanceAggregateJobStatus.class);
        when(status1.getContextInstanceId()).thenReturn("ctx-1");
        ContextInstanceAggregateJobStatus status2 = mock(ContextInstanceAggregateJobStatus.class);
        when(status2.getContextInstanceId()).thenReturn("ctx-2");

        List<ContextInstanceAggregateJobStatus> expectedStatuses = Arrays.asList(status1, status2);

        when(mockJobInstanceDao.getJobStatusCountForContextInstances(contextInstanceIds))
            .thenReturn(expectedStatuses);

        Map<String, Map<String, Integer>> repeatingJobStatuses = new HashMap<>();
        when(mockAuditAggregateDao.getRepeatingJobStatusCounts(contextInstanceIds))
            .thenReturn(repeatingJobStatuses);

        when(mockScheduledContextInstanceRecord.isContainsRepeatingJobs()).thenReturn(true);
        when(mockContextInstanceService.findById("ctx-1_scheduledContextInstance"))
            .thenReturn(mockScheduledContextInstanceRecord);
        when(mockContextInstanceService.findById("ctx-2_scheduledContextInstance"))
            .thenReturn(mockScheduledContextInstanceRecord);

        // When
        List<ContextInstanceAggregateJobStatus> results =
            serviceWithLegacyCount.getJobStatusCountForContextInstances(contextInstanceIds);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(mockJobInstanceDao).getJobStatusCountForContextInstances(contextInstanceIds);
        verify(status1).setContainsRepeatableJobs(true);
        verify(status2).setContainsRepeatableJobs(true);
    }

    @Test
    public void testGetJobStatusCountForContextInstancesWithNewCount() {
        // Given
        List<String> contextInstanceIds = Arrays.asList("ctx-1");
        ContextInstanceAggregateJobStatus status1 = mock(ContextInstanceAggregateJobStatus.class);
        when(status1.getContextInstanceId()).thenReturn("ctx-1");

        List<ContextInstanceAggregateJobStatus> expectedStatuses = Arrays.asList(status1);

        when(mockJobInstanceDao.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(contextInstanceIds))
            .thenReturn(expectedStatuses);

        Map<String, Map<String, Integer>> repeatingJobStatuses = new HashMap<>();
        when(mockAuditAggregateDao.getRepeatingJobStatusCounts(contextInstanceIds))
            .thenReturn(repeatingJobStatuses);

        when(mockScheduledContextInstanceRecord.isContainsRepeatingJobs()).thenReturn(false);
        when(mockContextInstanceService.findById("ctx-1_scheduledContextInstance"))
            .thenReturn(mockScheduledContextInstanceRecord);

        // When
        List<ContextInstanceAggregateJobStatus> results =
            service.getJobStatusCountForContextInstances(contextInstanceIds);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mockJobInstanceDao).getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(contextInstanceIds);
        verify(status1).setContainsRepeatableJobs(false);
    }

    @Test
    public void testGetJobStatusCountForContextInstancesConsiderNonTargetedDuplication() {
        // Given
        List<String> contextInstanceIds = Arrays.asList("ctx-1", "ctx-2", "ctx-3");
        ContextInstanceAggregateJobStatus status1 = mock(ContextInstanceAggregateJobStatus.class);
        ContextInstanceAggregateJobStatus status2 = mock(ContextInstanceAggregateJobStatus.class);
        ContextInstanceAggregateJobStatus status3 = mock(ContextInstanceAggregateJobStatus.class);

        List<ContextInstanceAggregateJobStatus> expectedStatuses = Arrays.asList(status1, status2, status3);

        when(mockJobInstanceDao.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(contextInstanceIds))
            .thenReturn(expectedStatuses);

        // When
        List<ContextInstanceAggregateJobStatus> results =
            service.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(contextInstanceIds);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        verify(mockJobInstanceDao).getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(contextInstanceIds);
    }

    @Test
    public void testDeleteSchedulerJobInstances() {
        // When
        service.deleteSchedulerJobInstances("ctx-instance-1");

        // Then
        verify(mockJobInstanceDao).deleteSchedulerJobInstances("ctx-instance-1");
    }

    @Test
    public void testGetCommandExecutionJobsForContextInstanceEmptyResults() {
        // Given
        SearchResults<SchedulerJobInstanceRecord> searchResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 50L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            any(SchedulerJobInstanceSearchFilter.class), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        Map<String, InternalEventDrivenJobInstance> result =
            service.getCommandExecutionJobsForContextInstance("ctx-instance-1");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSchedulerJobInstancesByContextInstanceIdWithMultipleResults() {
        // Given
        SchedulerJobInstanceRecord record2 = mock(SchedulerJobInstanceRecord.class);
        SchedulerJobInstanceRecord record3 = mock(SchedulerJobInstanceRecord.class);
        List<SchedulerJobInstanceRecord> records = Arrays.asList(mockJobInstanceRecord, record2, record3);
        SearchResults<SchedulerJobInstanceRecord> expectedResults =
            new SearchResultsImpl<>(records, 3L, 150L);
        when(mockJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-instance-1", 20, 5, "jobName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobInstanceRecord> results =
            service.getSchedulerJobInstancesByContextInstanceId(
                "ctx-instance-1", 20, 5, "jobName", "ASC");

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(3L, results.getTotalNumberOfResults());
        verify(mockJobInstanceDao).getSchedulerJobInstancesByContextInstanceId(
            "ctx-instance-1", 20, 5, "jobName", "ASC");
    }

    @Test
    public void testFindByContextIdJobNameChildContextNameWithMultipleResultsLogsWarning() {
        // Given
        SchedulerJobInstanceRecord record2 = mock(SchedulerJobInstanceRecord.class);
        List<SchedulerJobInstanceRecord> records = Arrays.asList(mockJobInstanceRecord, record2);
        SearchResults<SchedulerJobInstanceRecord> searchResults =
            new SearchResultsImpl<>(records, 2L, 100L);
        when(mockJobInstanceDao.getScheduledContextInstancesByFilter(
            any(SchedulerJobInstanceSearchFilter.class), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        SchedulerJobInstanceRecord result = service.findByContextIdJobNameChildContextName(
            "ctx-uuid-1", "test-job", "child-ctx");

        // Then - should return first result and log warning (warning verified by code inspection)
        assertNotNull(result);
        assertEquals(mockJobInstanceRecord, result);
    }

    @Test
    public void testGetJobStatusCountForContextInstancesWithNullScheduledContextInstance() {
        // Given
        List<String> contextInstanceIds = Arrays.asList("ctx-1");
        ContextInstanceAggregateJobStatus status1 = mock(ContextInstanceAggregateJobStatus.class);
        when(status1.getContextInstanceId()).thenReturn("ctx-1");

        List<ContextInstanceAggregateJobStatus> expectedStatuses = Arrays.asList(status1);

        when(mockJobInstanceDao.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(contextInstanceIds))
            .thenReturn(expectedStatuses);

        Map<String, Map<String, Integer>> repeatingJobStatuses = new HashMap<>();
        when(mockAuditAggregateDao.getRepeatingJobStatusCounts(contextInstanceIds))
            .thenReturn(repeatingJobStatuses);

        when(mockContextInstanceService.findById("ctx-1_scheduledContextInstance"))
            .thenReturn(null);

        // When
        List<ContextInstanceAggregateJobStatus> results =
            service.getJobStatusCountForContextInstances(contextInstanceIds);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(status1, never()).setContainsRepeatableJobs(anyBoolean());
    }
}
