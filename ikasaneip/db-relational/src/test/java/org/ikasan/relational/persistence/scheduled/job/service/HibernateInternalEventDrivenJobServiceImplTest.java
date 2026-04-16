package org.ikasan.relational.persistence.scheduled.job.service;

import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.dao.HibernateInternalEventDrivenJobDaoImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateInternalEventDrivenJobRecord;
import org.ikasan.relational.persistence.scheduled.job.service.HibernateInternalEventDrivenJobServiceImpl;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for HibernateInternalEventDrivenJobServiceImpl using Mockito.
 *
 * Tests all service methods including CRUD operations, delegation to DAO,
 * validation, and error handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateInternalEventDrivenJobServiceImplTest {

    @Mock
    private HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobDao;

    private HibernateInternalEventDrivenJobServiceImpl service;

    @Before
    public void setUp() {
        service = new HibernateInternalEventDrivenJobServiceImpl(internalEventDrivenJobDao);
    }

    // Constructor Tests

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_internalEventDrivenJobDao_throwsException() {
        new HibernateInternalEventDrivenJobServiceImpl(null);
    }

    @Test
    public void test_constructor_valid_dao_createsService() {
        HibernateInternalEventDrivenJobServiceImpl newService =
            new HibernateInternalEventDrivenJobServiceImpl(internalEventDrivenJobDao);

        Assert.assertNotNull(newService);
    }

    // FindAll Tests

    @Test
    public void test_findAll_delegatesToDao() {
        List<HibernateInternalEventDrivenJobRecord> records = createSampleRecords(3);
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records, 3L, 0L);

        when(internalEventDrivenJobDao.findAll(10, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findAll(10, 0);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(3, actualResults.getResultList().size());
        Assert.assertEquals(3L, actualResults.getTotalNumberOfResults());
        verify(internalEventDrivenJobDao, times(1)).findAll(10, 0);
    }

    @Test
    public void test_findAll_withLimit_returnsLimitedResults() {
        List<HibernateInternalEventDrivenJobRecord> records = createSampleRecords(5);
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records.subList(0, 3), 10L, 0L);

        when(internalEventDrivenJobDao.findAll(3, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findAll(3, 0);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(3, actualResults.getResultList().size());
        Assert.assertEquals(10L, actualResults.getTotalNumberOfResults());
        verify(internalEventDrivenJobDao, times(1)).findAll(3, 0);
    }

    @Test
    public void test_findAll_withOffset_returnsOffsetResults() {
        List<HibernateInternalEventDrivenJobRecord> records = createSampleRecords(3);
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records, 10L, 0L);

        when(internalEventDrivenJobDao.findAll(10, 5)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findAll(10, 5);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(3, actualResults.getResultList().size());
        verify(internalEventDrivenJobDao, times(1)).findAll(10, 5);
    }

    @Test
    public void test_findAll_emptyResults_returnsEmptyList() {
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);

        when(internalEventDrivenJobDao.findAll(10, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findAll(10, 0);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(0, actualResults.getResultList().size());
        Assert.assertEquals(0L, actualResults.getTotalNumberOfResults());
        verify(internalEventDrivenJobDao, times(1)).findAll(10, 0);
    }

    @Test
    public void test_findAll_negativeLimit_delegatesToDao() {
        List<HibernateInternalEventDrivenJobRecord> records = createSampleRecords(2);
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records, 2L, 0L);

        when(internalEventDrivenJobDao.findAll(-1, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findAll(-1, 0);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(2, actualResults.getResultList().size());
        verify(internalEventDrivenJobDao, times(1)).findAll(-1, 0);
    }

    // FindByContext Tests

    @Test
    public void test_findByContext_delegatesToDao() {
        String contextId = "testContext";
        List<HibernateInternalEventDrivenJobRecord> records = createSampleRecords(2);
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records, 2L, 0L);

        when(internalEventDrivenJobDao.findByContext(contextId, 10, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findByContext(contextId, 10, 0);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(2, actualResults.getResultList().size());
        Assert.assertEquals(2L, actualResults.getTotalNumberOfResults());
        verify(internalEventDrivenJobDao, times(1)).findByContext(contextId, 10, 0);
    }

    @Test
    public void test_findByContext_withLimitAndOffset_delegatesToDao() {
        String contextId = "contextWithPagination";
        List<HibernateInternalEventDrivenJobRecord> records = createSampleRecords(5);
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(records.subList(2, 4), 10L, 0L);

        when(internalEventDrivenJobDao.findByContext(contextId, 2, 2)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findByContext(contextId, 2, 2);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(2, actualResults.getResultList().size());
        verify(internalEventDrivenJobDao, times(1)).findByContext(contextId, 2, 2);
    }

    @Test
    public void test_findByContext_emptyContext_delegatesToDao() {
        String contextId = "";
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);

        when(internalEventDrivenJobDao.findByContext(contextId, 10, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findByContext(contextId, 10, 0);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(0, actualResults.getResultList().size());
        verify(internalEventDrivenJobDao, times(1)).findByContext(contextId, 10, 0);
    }

    @Test
    public void test_findByContext_nullContext_delegatesToDao() {
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);

        when(internalEventDrivenJobDao.findByContext(null, 10, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findByContext(null, 10, 0);

        Assert.assertNotNull(actualResults);
        verify(internalEventDrivenJobDao, times(1)).findByContext(null, 10, 0);
    }

    @Test
    public void test_findByContext_nonExistentContext_returnsEmpty() {
        String contextId = "nonExistentContext";
        SearchResults<HibernateInternalEventDrivenJobRecord> expectedResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);

        when(internalEventDrivenJobDao.findByContext(contextId, 10, 0)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findByContext(contextId, 10, 0);

        Assert.assertNotNull(actualResults);
        Assert.assertEquals(0, actualResults.getResultList().size());
        verify(internalEventDrivenJobDao, times(1)).findByContext(contextId, 10, 0);
    }

    // FindById Tests

    @Test
    public void test_findById_delegatesToDao() {
        String id = "testId";
        HibernateInternalEventDrivenJobRecord expectedRecord = createSampleRecord("agent1", "job1", "context1");

        when(internalEventDrivenJobDao.findById(id)).thenReturn(expectedRecord);

        InternalEventDrivenJobRecord actualRecord = service.findById(id);

        Assert.assertNotNull(actualRecord);
        Assert.assertEquals(expectedRecord, actualRecord);
        verify(internalEventDrivenJobDao, times(1)).findById(id);
    }

    @Test
    public void test_findById_nonExistentId_returnsNull() {
        String id = "nonExistentId";

        when(internalEventDrivenJobDao.findById(id)).thenReturn(null);

        InternalEventDrivenJobRecord actualRecord = service.findById(id);

        Assert.assertNull(actualRecord);
        verify(internalEventDrivenJobDao, times(1)).findById(id);
    }

    @Test
    public void test_findById_emptyId_delegatesToDao() {
        String id = "";

        when(internalEventDrivenJobDao.findById(id)).thenReturn(null);

        InternalEventDrivenJobRecord actualRecord = service.findById(id);

        Assert.assertNull(actualRecord);
        verify(internalEventDrivenJobDao, times(1)).findById(id);
    }

    @Test
    public void test_findById_nullId_delegatesToDao() {
        when(internalEventDrivenJobDao.findById(null)).thenReturn(null);

        InternalEventDrivenJobRecord actualRecord = service.findById(null);

        Assert.assertNull(actualRecord);
        verify(internalEventDrivenJobDao, times(1)).findById(null);
    }

    // Save Tests

    @Test
    public void test_save_validRecord_delegatesToDao() {
        HibernateInternalEventDrivenJobRecord recordToSave = createSampleRecord("agent1", "job1", "context1");

        service.save(recordToSave);

        verify(internalEventDrivenJobDao, times(1)).save(recordToSave);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_nullRecord_throwsException() {
        service.save(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_nonHibernateRecord_throwsException() {
        InternalEventDrivenJobRecord mockRecord = mock(InternalEventDrivenJobRecord.class);

        service.save(mockRecord);
    }

    @Test
    public void test_save_recordWithJob_delegatesToDao() {
        HibernateInternalEventDrivenJobRecord recordToSave = createSampleRecord("agent1", "job1", "context1");
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setAgentName("agent1");
        job.setJobName("job1");
        job.setContextName("context1");
        job.setCommandLine("ls -al");
        recordToSave.setInternalEventDrivenJob(job);

        service.save(recordToSave);

        verify(internalEventDrivenJobDao, times(1)).save(recordToSave);
    }

    @Test
    public void test_save_recordWithHeldContexts_delegatesToDao() {
        HibernateInternalEventDrivenJobRecord recordToSave = createSampleRecord("agent1", "job1", "context1");
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setAgentName("agent1");
        job.setJobName("job1");
        job.setContextName("context1");
        job.setHeldContexts(new HashMap<>());
        job.getHeldContexts().put("child1", true);
        recordToSave.setInternalEventDrivenJob(job);
        recordToSave.setHeld(true);

        service.save(recordToSave);

        verify(internalEventDrivenJobDao, times(1)).save(recordToSave);
    }

    @Test
    public void test_save_recordWithSkippedContexts_delegatesToDao() {
        HibernateInternalEventDrivenJobRecord recordToSave = createSampleRecord("agent1", "job1", "context1");
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setAgentName("agent1");
        job.setJobName("job1");
        job.setContextName("context1");
        job.setSkippedContexts(new HashMap<>());
        job.getSkippedContexts().put("child1", true);
        recordToSave.setInternalEventDrivenJob(job);
        recordToSave.setSkipped(true);

        service.save(recordToSave);

        verify(internalEventDrivenJobDao, times(1)).save(recordToSave);
    }

    @Test
    public void test_save_multipleRecords_eachDelegatesToDao() {
        HibernateInternalEventDrivenJobRecord record1 = createSampleRecord("agent1", "job1", "context1");
        HibernateInternalEventDrivenJobRecord record2 = createSampleRecord("agent2", "job2", "context2");
        HibernateInternalEventDrivenJobRecord record3 = createSampleRecord("agent3", "job3", "context3");

        service.save(record1);
        service.save(record2);
        service.save(record3);

        verify(internalEventDrivenJobDao, times(1)).save(record1);
        verify(internalEventDrivenJobDao, times(1)).save(record2);
        verify(internalEventDrivenJobDao, times(1)).save(record3);
        verify(internalEventDrivenJobDao, times(3)).save(any(HibernateInternalEventDrivenJobRecord.class));
    }

    @Test
    public void test_save_recordWithModifiedTimestamp_delegatesToDao() {
        HibernateInternalEventDrivenJobRecord recordToSave = createSampleRecord("agent1", "job1", "context1");
        recordToSave.setModifiedTimestamp(System.currentTimeMillis());
        recordToSave.setModifiedBy("tester");

        service.save(recordToSave);

        verify(internalEventDrivenJobDao, times(1)).save(recordToSave);
    }

    @Test
    public void test_save_recordWithAllFields_delegatesToDao() {
        HibernateInternalEventDrivenJobRecord recordToSave = createSampleRecord("agent1", "job1", "context1");
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setAgentName("agent1");
        job.setJobName("job1");
        job.setContextName("context1");
        job.setCommandLine("echo 'test'");
        job.setChildContextNames(Arrays.asList("child1", "child2"));
        job.setTargetResidingContextOnly(true);
        job.setParticipatesInLock(true);
        job.setSkippedContexts(new HashMap<>());
        job.setHeldContexts(new HashMap<>());

        recordToSave.setInternalEventDrivenJob(job);
        recordToSave.setTimestamp(System.currentTimeMillis());
        recordToSave.setModifiedTimestamp(System.currentTimeMillis());
        recordToSave.setModifiedBy("tester");
        recordToSave.setHeld(false);
        recordToSave.setSkipped(false);
        recordToSave.setTargetResidingContextOnly(true);
        recordToSave.setParticipatesInLock(true);
        recordToSave.setDisplayName("Test Job Display Name");

        service.save(recordToSave);

        verify(internalEventDrivenJobDao, times(1)).save(recordToSave);
    }

    // Integration-style Tests

    @Test
    public void test_findAll_thenSave_thenFindAll_sequence() {
        // First findAll - empty
        SearchResults<HibernateInternalEventDrivenJobRecord> emptyResults =
            new SearchResultsImpl<>(new ArrayList<>(), 0L, 0L);
        when(internalEventDrivenJobDao.findAll(10, 0)).thenReturn(emptyResults);

        SearchResults<InternalEventDrivenJobRecord> initialResults = service.findAll(10, 0);
        Assert.assertEquals(0, initialResults.getResultList().size());

        // Save a record
        HibernateInternalEventDrivenJobRecord record = createSampleRecord("agent1", "job1", "context1");
        service.save(record);

        // Second findAll - with record
        List<HibernateInternalEventDrivenJobRecord> records = Arrays.asList(record);
        SearchResults<HibernateInternalEventDrivenJobRecord> resultsWithRecord =
            new SearchResultsImpl<>(records, 1L, 0L);
        when(internalEventDrivenJobDao.findAll(10, 0)).thenReturn(resultsWithRecord);

        SearchResults<InternalEventDrivenJobRecord> finalResults = service.findAll(10, 0);
        Assert.assertEquals(1, finalResults.getResultList().size());

        verify(internalEventDrivenJobDao, times(2)).findAll(10, 0);
        verify(internalEventDrivenJobDao, times(1)).save(record);
    }

    @Test
    public void test_save_thenFindById_sequence() {
        String id = "testId";
        HibernateInternalEventDrivenJobRecord record = createSampleRecord("agent1", "job1", "context1");
        record.setId(id);

        service.save(record);

        when(internalEventDrivenJobDao.findById(id)).thenReturn(record);

        InternalEventDrivenJobRecord foundRecord = service.findById(id);

        Assert.assertNotNull(foundRecord);
        Assert.assertEquals(id, foundRecord.getId());
        verify(internalEventDrivenJobDao, times(1)).save(record);
        verify(internalEventDrivenJobDao, times(1)).findById(id);
    }

    @Test
    public void test_save_thenFindByContext_sequence() {
        String contextId = "context1";
        HibernateInternalEventDrivenJobRecord record = createSampleRecord("agent1", "job1", contextId);

        service.save(record);

        List<HibernateInternalEventDrivenJobRecord> records = Arrays.asList(record);
        SearchResults<HibernateInternalEventDrivenJobRecord> searchResults =
            new SearchResultsImpl<>(records, 1L, 0L);
        when(internalEventDrivenJobDao.findByContext(contextId, 10, 0)).thenReturn(searchResults);

        SearchResults<InternalEventDrivenJobRecord> results = service.findByContext(contextId, 10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getResultList().size());
        verify(internalEventDrivenJobDao, times(1)).save(record);
        verify(internalEventDrivenJobDao, times(1)).findByContext(contextId, 10, 0);
    }

    // Helper Methods

    /**
     * Create a list of sample records for testing
     */
    private List<HibernateInternalEventDrivenJobRecord> createSampleRecords(int count) {
        List<HibernateInternalEventDrivenJobRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(createSampleRecord("agent" + i, "job" + i, "context" + i));
        }
        return records;
    }

    /**
     * Create a single sample record for testing
     */
    private HibernateInternalEventDrivenJobRecord createSampleRecord(String agentName, String jobName, String contextName) {
        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setId(jobName + "_" + contextName);
        record.setTimestamp(System.currentTimeMillis());
        record.setModifiedTimestamp(System.currentTimeMillis());
        return record;
    }
}
