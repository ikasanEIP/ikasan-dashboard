package org.ikasan.scheduled.job.service;

import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class SolrInternalEventDrivenJobRecordServiceImplTest {

    private InternalEventDrivenJobDao internalEventDrivenJobRecordDao;
    private SolrInternalEventDrivenJobRecordServiceImpl service;

    @Before
    public void setUp() {
        internalEventDrivenJobRecordDao = mock(InternalEventDrivenJobDao.class);
        service = new SolrInternalEventDrivenJobRecordServiceImpl(internalEventDrivenJobRecordDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_internalEventDrivenJobRecordDao_throwsException() {
        new SolrInternalEventDrivenJobRecordServiceImpl(null);
    }

    @Test
    public void test_findAll_delegatesToDao() {
        SearchResults<InternalEventDrivenJobRecord> expectedResults = mock(SearchResults.class);
        when(internalEventDrivenJobRecordDao.findAll(0, 10)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findAll(0, 10);

        Assert.assertEquals(expectedResults, actualResults);
        verify(internalEventDrivenJobRecordDao, times(1)).findAll(0, 10);
    }

    @Test
    public void test_findByContext_delegatesToDao() {
        String contextId = "testContext";
        SearchResults<InternalEventDrivenJobRecord> expectedResults = mock(SearchResults.class);
        when(internalEventDrivenJobRecordDao.findByContext(contextId, 0, 10)).thenReturn(expectedResults);

        SearchResults<InternalEventDrivenJobRecord> actualResults = service.findByContext(contextId, 0, 10);

        Assert.assertEquals(expectedResults, actualResults);
        verify(internalEventDrivenJobRecordDao, times(1)).findByContext(contextId, 0, 10);
    }

    @Test
    public void test_findById_delegatesToDao() {
        String id = "testId";
        InternalEventDrivenJobRecord expectedRecord = mock(InternalEventDrivenJobRecord.class);
        when(internalEventDrivenJobRecordDao.findById(id)).thenReturn(expectedRecord);

        InternalEventDrivenJobRecord actualRecord = service.findById(id);

        Assert.assertEquals(expectedRecord, actualRecord);
        verify(internalEventDrivenJobRecordDao, times(1)).findById(id);
    }

    @Test
    public void test_save_delegatesToDao() {
        InternalEventDrivenJobRecord recordToSave = mock(InternalEventDrivenJobRecord.class);

        service.save(recordToSave);

        verify(internalEventDrivenJobRecordDao, times(1)).save(recordToSave);
    }
}
