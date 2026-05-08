package org.ikasan.orchestration.service.scheduled.job;

import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.job.dao.*;
import org.ikasan.spec.scheduled.job.model.*;
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
 * Unit tests for SchedulerJobServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class SchedulerJobServiceImplTest {

    @Mock
    private FileEventDrivenJobDao mockFileEventDrivenJobDao;

    @Mock
    private InternalEventDrivenJobDao mockInternalEventDrivenJobDao;

    @Mock
    private QuartzScheduleDrivenJobDao mockQuartzScheduleDrivenJobDao;

    @Mock
    private GlobalEventJobDao mockGlobalEventJobDao;

    @Mock
    private ContextStartJobDao mockContextStartJobDao;

    @Mock
    private ContextTerminalJobDao mockContextTerminalJobDao;

    @Mock
    private SchedulerJobDao mockSchedulerJobDao;

    @Mock
    private InternalEventDrivenJobDao mockInternalEventDrivenJobTemplateDao;

    @Mock
    private SchedulerJobRecord mockSchedulerJobRecord;

    @Mock
    private InternalEventDrivenJobRecord mockInternalEventJobRecord;

    @Mock
    private FileEventDrivenJobRecord mockFileEventJobRecord;

    @Mock
    private QuartzScheduleDrivenJobRecord mockQuartzJobRecord;

    @Mock
    private GlobalEventJobRecord mockGlobalEventJobRecord;

    @Mock
    private ContextStartJobRecord mockContextStartJobRecord;

    @Mock
    private ContextTerminalJobRecord mockContextTerminalJobRecord;

    private SchedulerJobServiceImpl service;

    @Before
    public void setUp() {
        service = new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            mockInternalEventDrivenJobDao,
            mockQuartzScheduleDrivenJobDao,
            mockGlobalEventJobDao,
            mockContextStartJobDao,
            mockContextTerminalJobDao,
            mockSchedulerJobDao,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullFileEventDrivenJobDao() {
        new SchedulerJobServiceImpl(
            null,
            mockInternalEventDrivenJobDao,
            mockQuartzScheduleDrivenJobDao,
            mockGlobalEventJobDao,
            mockContextStartJobDao,
            mockContextTerminalJobDao,
            mockSchedulerJobDao,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullInternalEventDrivenJobDao() {
        new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            null,
            mockQuartzScheduleDrivenJobDao,
            mockGlobalEventJobDao,
            mockContextStartJobDao,
            mockContextTerminalJobDao,
            mockSchedulerJobDao,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullQuartzScheduleDrivenJobDao() {
        new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            mockInternalEventDrivenJobDao,
            null,
            mockGlobalEventJobDao,
            mockContextStartJobDao,
            mockContextTerminalJobDao,
            mockSchedulerJobDao,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullGlobalEventJobDao() {
        new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            mockInternalEventDrivenJobDao,
            mockQuartzScheduleDrivenJobDao,
            null,
            mockContextStartJobDao,
            mockContextTerminalJobDao,
            mockSchedulerJobDao,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullContextStartJobDao() {
        new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            mockInternalEventDrivenJobDao,
            mockQuartzScheduleDrivenJobDao,
            mockGlobalEventJobDao,
            null,
            mockContextTerminalJobDao,
            mockSchedulerJobDao,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullContextTerminalJobDao() {
        new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            mockInternalEventDrivenJobDao,
            mockQuartzScheduleDrivenJobDao,
            mockGlobalEventJobDao,
            mockContextStartJobDao,
            null,
            mockSchedulerJobDao,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullSchedulerJobDao() {
        new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            mockInternalEventDrivenJobDao,
            mockQuartzScheduleDrivenJobDao,
            mockGlobalEventJobDao,
            mockContextStartJobDao,
            mockContextTerminalJobDao,
            null,
            mockInternalEventDrivenJobTemplateDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullInternalEventDrivenJobTemplateDao() {
        new SchedulerJobServiceImpl(
            mockFileEventDrivenJobDao,
            mockInternalEventDrivenJobDao,
            mockQuartzScheduleDrivenJobDao,
            mockGlobalEventJobDao,
            mockContextStartJobDao,
            mockContextTerminalJobDao,
            mockSchedulerJobDao,
            null
        );
    }

    @Test
    public void testFindById() {
        // Given
        when(mockSchedulerJobDao.findById("job-1")).thenReturn(mockSchedulerJobRecord);

        // When
        SchedulerJobRecord result = service.findById("job-1");

        // Then
        assertNotNull(result);
        assertEquals(mockSchedulerJobRecord, result);
        verify(mockSchedulerJobDao).findById("job-1");
    }

    @Test
    public void testFindByAgent() {
        // Given
        SearchResults expectedResults = new SearchResultsImpl<>(
            Arrays.asList(mockSchedulerJobRecord), 1L, 100L);
        when(mockSchedulerJobDao.findByAgent("test-agent", 10, 0))
            .thenReturn(expectedResults);

        // When
        SearchResults result = service.findByAgent("test-agent", 10, 0);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getResultList().size());
        verify(mockSchedulerJobDao).findByAgent("test-agent", 10, 0);
    }

    @Test
    public void testFindByContextNameAndJobName() {
        // Given
        when(mockSchedulerJobDao.findByContextIdAndJobName("ctx-1", "job-1"))
            .thenReturn(mockSchedulerJobRecord);

        // When
        SchedulerJobRecord result = service.findByContextNameAndJobName("ctx-1", "job-1");

        // Then
        assertNotNull(result);
        assertEquals(mockSchedulerJobRecord, result);
        verify(mockSchedulerJobDao).findByContextIdAndJobName("ctx-1", "job-1");
    }

    @Test
    public void testFindByContext() {
        // Given
        SearchResults<SchedulerJobRecord> expectedResults = new SearchResultsImpl<>(
            Arrays.asList(mockSchedulerJobRecord), 1L, 100L);
        when(mockSchedulerJobDao.findByContext("ctx-1", 10, 0))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobRecord> result = service.findByContext("ctx-1", 10, 0);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getResultList().size());
        verify(mockSchedulerJobDao).findByContext("ctx-1", 10, 0);
    }

    @Test
    public void testFindByFilter() {
        // Given
        SchedulerJobSearchFilter filter = mock(SchedulerJobSearchFilter.class);
        SearchResults<SchedulerJobRecord> expectedResults = new SearchResultsImpl<>(
            Arrays.asList(mockSchedulerJobRecord), 1L, 100L);
        when(mockSchedulerJobDao.findByFilter(filter, 10, 0, "jobName", "ASC"))
            .thenReturn(expectedResults);

        // When
        SearchResults<SchedulerJobRecord> result = service.findByFilter(filter, 10, 0, "jobName", "ASC");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getResultList().size());
        verify(mockSchedulerJobDao).findByFilter(filter, 10, 0, "jobName", "ASC");
    }

    @Test
    public void testDelete() {
        // When
        service.delete(mockSchedulerJobRecord);

        // Then
        verify(mockSchedulerJobDao).delete(mockSchedulerJobRecord);
    }

    @Test
    public void testDeleteByAgentName() {
        // When
        service.deleteByAgentName("test-agent");

        // Then
        verify(mockSchedulerJobDao).deleteByAgentName("test-agent");
    }

    @Test
    public void testDeleteByContextName() {
        // When
        service.deleteByContextName("test-context");

        // Then
        verify(mockSchedulerJobDao).deleteByContextName("test-context");
    }

    @Test
    public void testSaveFileEventDrivenJobRecord() {
        // When
        service.saveFileEventDrivenJobRecord(mockFileEventJobRecord);

        // Then
        verify(mockFileEventDrivenJobDao).save(mockFileEventJobRecord);
    }

    @Test
    public void testSaveInternalEventDrivenJobRecord() {
        // When
        service.saveInternalEventDrivenJobRecord(mockInternalEventJobRecord);

        // Then
        verify(mockInternalEventDrivenJobDao).save(mockInternalEventJobRecord);
    }

    @Test
    public void testSaveQuartzScheduledJobRecord() {
        // When
        service.saveQuartzScheduledJobRecord(mockQuartzJobRecord);

        // Then
        verify(mockQuartzScheduleDrivenJobDao).save(mockQuartzJobRecord);
    }

    @Test
    public void testSaveGlobalEventJobRecord() {
        // When
        service.saveGlobalEventJobRecord(mockGlobalEventJobRecord);

        // Then
        verify(mockGlobalEventJobDao).save(mockGlobalEventJobRecord);
    }

    @Test
    public void testSaveContextStartJobRecord() {
        // When
        service.saveContextStartJobRecord(mockContextStartJobRecord);

        // Then
        verify(mockContextStartJobDao).save(mockContextStartJobRecord);
    }

    @Test
    public void testSaveContextTerminalJobRecord() {
        // When
        service.saveContextTerminalJobRecord(mockContextTerminalJobRecord);

        // Then
        verify(mockContextTerminalJobDao).save(mockContextTerminalJobRecord);
    }

    @Test
    public void testSaveInternalEventDrivenJobTemplateRecord() {
        // When
        service.saveInternalEventDrivenJobTemplateRecord(mockInternalEventJobRecord, "user1");

        // Then
        verify(mockInternalEventDrivenJobTemplateDao).save(mockInternalEventJobRecord);
    }

    @Test
    public void testSaveFileEventDrivenJobRecords() {
        // Given
        List<FileEventDrivenJobRecord> records = Arrays.asList(
            mockFileEventJobRecord, mock(FileEventDrivenJobRecord.class)
        );

        // When
        service.saveFileEventDrivenJobRecords(records);

        // Then
        verify(mockFileEventDrivenJobDao).save(records);
    }

    @Test
    public void testSaveInternalEventDrivenJobRecords() {
        // Given
        List<InternalEventDrivenJobRecord> records = Arrays.asList(
            mockInternalEventJobRecord, mock(InternalEventDrivenJobRecord.class)
        );

        // When
        service.saveInternalEventDrivenJobRecords(records);

        // Then
        verify(mockInternalEventDrivenJobDao).save(records);
    }

    @Test
    public void testSaveQuartzScheduledJobRecords() {
        // Given
        List<QuartzScheduleDrivenJobRecord> records = Arrays.asList(
            mockQuartzJobRecord, mock(QuartzScheduleDrivenJobRecord.class)
        );

        // When
        service.saveQuartzScheduledJobRecords(records);

        // Then
        verify(mockQuartzScheduleDrivenJobDao).save(records);
    }

    @Test
    public void testSaveGlobalEventJobRecords() {
        // Given
        List<GlobalEventJobRecord> records = Arrays.asList(
            mockGlobalEventJobRecord, mock(GlobalEventJobRecord.class)
        );

        // When
        service.saveGlobalEventJobRecords(records);

        // Then
        verify(mockGlobalEventJobDao).save(records);
    }

    @Test
    public void testSaveContextStartJobRecords() {
        // Given
        List<ContextStartJobRecord> records = Arrays.asList(
            mockContextStartJobRecord, mock(ContextStartJobRecord.class)
        );

        // When
        service.saveContextStartJobRecords(records);

        // Then
        verify(mockContextStartJobDao).save(records);
    }

    @Test
    public void testSaveContextTerminalJobRecords() {
        // Given
        List<ContextTerminalJobRecord> records = Arrays.asList(
            mockContextTerminalJobRecord, mock(ContextTerminalJobRecord.class)
        );

        // When
        service.saveContextTerminalJobRecord(records);

        // Then
        verify(mockContextTerminalJobDao).save(records);
    }

    @Test
    public void testSaveInternalEventDrivenJobTemplateRecords() {
        // Given
        List<InternalEventDrivenJobRecord> records = Arrays.asList(
            mockInternalEventJobRecord, mock(InternalEventDrivenJobRecord.class)
        );

        // When
        service.saveInternalEventDrivenJobTemplateRecords(records);

        // Then
        verify(mockInternalEventDrivenJobTemplateDao).save(records);
    }

    @Test
    public void testSaveInternalEventDrivenJobs() {
        // Given
        InternalEventDrivenJob job1 = new InternalEventDrivenJobImpl();
        job1.setAgentName("agent1");
        job1.setJobName("job1");
        job1.setContextName("ctx1");
        job1.setHeldContexts(new HashMap<>());
        job1.setSkippedContexts(new HashMap<>());

        List<InternalEventDrivenJob> jobs = Arrays.asList(job1);

        // When
        service.saveInternalEventDrivenJobs(jobs, "user1");

        // Then
        ArgumentCaptor<List<InternalEventDrivenJobRecord>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(mockInternalEventDrivenJobDao).save(captor.capture());
        assertEquals(1, captor.getValue().size());
        verifyNoMoreInteractions(mockInternalEventDrivenJobDao);
    }

    @Test
    public void testSaveQuartzScheduledJobs() {
        // Given
        QuartzScheduleDrivenJob quartzJob = new QuartzScheduleDrivenJobImpl();
        quartzJob.setAgentName("agent1");
        quartzJob.setJobName("quartz-job");
        quartzJob.setContextName("ctx1");

        List<QuartzScheduleDrivenJob> jobs = Arrays.asList(quartzJob);

        // When
        service.saveQuartzScheduledJobs(jobs, "user1");

        // Then
        ArgumentCaptor<List<QuartzScheduleDrivenJobRecord>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(mockQuartzScheduleDrivenJobDao).save(captor.capture());
        assertEquals(1, captor.getValue().size());

        verifyNoMoreInteractions(mockQuartzScheduleDrivenJobDao);
    }

    @Test
    public void testSaveFileEventDrivenJobs() {
        // Given
        FileEventDrivenJob fileJob = new FileEventDrivenJobImpl();
        fileJob.setAgentName("agent1");
        fileJob.setJobName("file-job");
        fileJob.setContextName("ctx1");

        List<FileEventDrivenJob> jobs = Arrays.asList(fileJob);

        // When
        service.saveFileEventDrivenJobs(jobs, "user1");

        // Then
        ArgumentCaptor<List<FileEventDrivenJobRecord>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(mockFileEventDrivenJobDao).save(captor.capture());
        assertEquals(1, captor.getValue().size());
        verifyNoMoreInteractions(mockFileEventDrivenJobDao);
    }

    @Test
    public void testSaveGlobalEventJobs() {
        // Given
        GlobalEventJob job1 = new GlobalEventJobImpl();
        job1.setAgentName("agent1");
        job1.setJobName("job1");
        job1.setContextName("ctx1");

        List<GlobalEventJob> jobs = Arrays.asList(job1);

        // When
        service.saveGlobalEventJobs(jobs, "user1");

        // Then
        ArgumentCaptor<List<GlobalEventJobRecord>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(mockGlobalEventJobDao).save(captor.capture());
        assertEquals(1, captor.getValue().size());
        verifyNoMoreInteractions(mockGlobalEventJobDao);
    }

    @Test
    public void testSaveContextStartJobs() {
        // Given
        ContextStartJob job1 = new ContextStartJobImpl();
        job1.setAgentName("agent1");
        job1.setJobName("job1");
        job1.setContextName("ctx1");

        List<ContextStartJob> jobs = Arrays.asList(job1);

        // When
        service.saveContextStartJobs(jobs, "user1");

        // Then
        ArgumentCaptor<List<ContextStartJobRecord>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(mockContextStartJobDao).save(captor.capture());
        assertEquals(1, captor.getValue().size());
        verifyNoMoreInteractions(mockContextStartJobDao);
    }

    @Test
    public void testSaveContextTerminalJobs() {
        // Given
        ContextTerminalJob job1 = new ContextTerminalJobImpl();
        job1.setAgentName("agent1");
        job1.setJobName("job1");
        job1.setContextName("ctx1");

        List<ContextTerminalJob> jobs = Arrays.asList(job1);

        // When
        service.saveContextTerminalJobs(jobs, "user1");

        // Then
        ArgumentCaptor<List<ContextTerminalJobRecord>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(mockContextTerminalJobDao).save(captor.capture());
        assertEquals(1, captor.getValue().size());
        verifyNoMoreInteractions(mockContextTerminalJobDao);
    }

    @Test
    public void testSaveWithNullOrEmptyList() {
        // When - null list
        service.save(null, "user1");

        // Then
        verifyNoInteractions(mockInternalEventDrivenJobDao);
        verifyNoInteractions(mockFileEventDrivenJobDao);

        // When - empty list
        service.save(new ArrayList<>(), "user1");

        // Then
        verifyNoInteractions(mockQuartzScheduleDrivenJobDao);
        verifyNoInteractions(mockGlobalEventJobDao);
    }

    @Test
    public void testSaveWithMixedJobTypes() {
        // Given
        InternalEventDrivenJob internalJob = new InternalEventDrivenJobImpl();
        internalJob.setAgentName("agent1");
        internalJob.setJobName("job1");
        internalJob.setContextName("ctx1");
        internalJob.setHeldContexts(new HashMap<>());
        internalJob.setSkippedContexts(new HashMap<>());

        FileEventDrivenJob fileJob = new FileEventDrivenJobImpl();
        fileJob.setAgentName("agent1");
        fileJob.setJobName("file-job");
        fileJob.setContextName("ctx1");

        QuartzScheduleDrivenJob quartzJob = new QuartzScheduleDrivenJobImpl();
        quartzJob.setAgentName("agent1");
        quartzJob.setJobName("quartz-job");
        quartzJob.setContextName("ctx1");

        List<SchedulerJob> jobs = Arrays.asList(internalJob, fileJob, quartzJob);

        // When
        service.save(jobs, "user1");

        // Then
        verify(mockInternalEventDrivenJobDao).save(anyList());
        verify(mockFileEventDrivenJobDao).save(anyList());
        verify(mockQuartzScheduleDrivenJobDao).save(anyList());

        verifyNoMoreInteractions(mockInternalEventDrivenJobDao,
            mockFileEventDrivenJobDao, mockQuartzScheduleDrivenJobDao);
    }

    @Test
    public void testGetCommandExecutionJobsForContext() {
        // Given
        InternalEventDrivenJob job1 = mock(InternalEventDrivenJobImpl.class);
        when(job1.getIdentifier()).thenReturn("identifier-1");

        SchedulerJobRecord record1 = mock(SchedulerJobRecord.class);
        when(record1.getJob()).thenReturn(job1);

        SearchResults<SchedulerJobRecord> searchResults = new SearchResultsImpl<>(
            Arrays.asList(record1), 1L, 100L);
        when(mockSchedulerJobDao.findByFilter(any(SchedulerJobSearchFilter.class),
            eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        Map<String, InternalEventDrivenJob> result = service.getCommandExecutionJobsForContext("ctx-1");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("identifier-1"));
        assertEquals(job1, result.get("identifier-1"));

        ArgumentCaptor<SchedulerJobSearchFilter> filterCaptor =
            ArgumentCaptor.forClass(SchedulerJobSearchFilter.class);
        verify(mockSchedulerJobDao).findByFilter(filterCaptor.capture(), eq(-1), eq(-1), isNull(), isNull());

        SchedulerJobSearchFilter capturedFilter = filterCaptor.getValue();
        assertNotNull(capturedFilter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSkipWithNonInternalEventDrivenJob() {
        // Given
        QuartzScheduleDrivenJob quartzJob = mock(QuartzScheduleDrivenJobImpl.class);
        when(mockSchedulerJobRecord.getJob()).thenReturn(quartzJob);

        // When
        service.skip(mockSchedulerJobRecord, Arrays.asList("ctx-1"), "user1");

        // Then - expect exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHoldWithNonInternalEventDrivenJob() {
        // Given
        QuartzScheduleDrivenJob quartzJob = mock(QuartzScheduleDrivenJobImpl.class);
        when(mockSchedulerJobRecord.getJob()).thenReturn(quartzJob);

        // When
        service.hold(mockSchedulerJobRecord, Arrays.asList("ctx-1"), "user1");

        // Then - expect exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReleaseWithNonInternalEventDrivenJob() {
        // Given
        QuartzScheduleDrivenJob quartzJob = mock(QuartzScheduleDrivenJobImpl.class);
        when(mockSchedulerJobRecord.getJob()).thenReturn(quartzJob);

        // When
        service.release(mockSchedulerJobRecord, "user1");

        // Then - expect exception
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockSchedulerJobDao.findById("non-existent")).thenReturn(null);

        // When
        SchedulerJobRecord result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockSchedulerJobDao).findById("non-existent");
    }

    @Test
    public void testGetCommandExecutionJobsForContextEmptyResults() {
        // Given
        SearchResults<SchedulerJobRecord> searchResults = new SearchResultsImpl<>(
            new ArrayList<>(), 0L, 50L);
        when(mockSchedulerJobDao.findByFilter(any(SchedulerJobSearchFilter.class),
            eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(searchResults);

        // When
        Map<String, InternalEventDrivenJob> result = service.getCommandExecutionJobsForContext("ctx-1");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
