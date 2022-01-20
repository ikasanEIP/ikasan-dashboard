package org.ikasan.job.orchestration.context;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.recovery.ContextInstanceRecoveryManager;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.search.SearchResults;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRecoveryManagerTest {

    @Mock
    ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    ScheduledContextService scheduledContextService;

    @Mock
    InternalEventDrivenJobService internalEventDrivenJobService;

    @Test
    public void test() {
        // Firstly mock the service behaviour that determines if any contexts are in the data store
        // that need to be recovered.
        SearchResults<ScheduledContextInstanceRecord> searchResults = mock(SearchResults.class);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(anyList())).thenReturn(searchResults);
        ArrayList<ScheduledContextInstanceRecord> scheduledContextInstanceRecords = mock(ArrayList.class);
        when(searchResults.getResultList()).thenReturn(scheduledContextInstanceRecords);

        // Now mock the behaviour to iterate over the results.
        Iterator mockIterator = mock(Iterator.class);
        ScheduledContextInstanceRecord scheduledContextInstanceRecord = mock(ScheduledContextInstanceRecord.class);
        when(scheduledContextInstanceRecords.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.next()).thenReturn(scheduledContextInstanceRecord);
        ContextInstance contextInstance = mock(ContextInstance.class);
        when(scheduledContextInstanceRecord.getContextInstance()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("instance-name");
        when(contextInstance.getId()).thenReturn("instance-id");
        when(scheduledContextInstanceRecord.getContextName()).thenReturn("contextName");

        // Mock the behaviour to get the actual context record.
        ScheduledContextRecord scheduledContextRecord = mock(ScheduledContextRecord.class);
        when(scheduledContextService.findByName("contextName")).thenReturn(scheduledContextRecord);

        // Mock the behaviour to get all the associated jobs.
        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults = mock(SearchResults.class);
        when(internalEventDrivenJobService.findByContext(anyString(), anyInt(), anyInt())).thenReturn(internalEventDrivenJobRecordSearchResults);

        // Mock the behaviour to put the jobs into a map keyed on their identifier.
        InternalEventDrivenJobRecord internalEventDrivenJobRecord = mock(InternalEventDrivenJobRecord.class);
        ArrayList<InternalEventDrivenJobRecord> internalEventDrivenJobRecords = mock(ArrayList.class);
        when(internalEventDrivenJobRecordSearchResults.getResultList()).thenReturn(internalEventDrivenJobRecords);
        when(internalEventDrivenJobRecords.stream()).thenReturn(Stream.of(internalEventDrivenJobRecord));
        InternalEventDrivenJob internalEventDrivenJob = mock(InternalEventDrivenJob.class);
        when(internalEventDrivenJobRecord.getInternalEventDrivenJob()).thenReturn(internalEventDrivenJob);
        when(internalEventDrivenJob.getIdentifier()).thenReturn("identifier");

        ContextInstanceRecoveryManager contextInstanceRecoveryManager = new ContextInstanceRecoveryManager(scheduledContextInstanceService,
            scheduledContextService, internalEventDrivenJobService, "queueDir");

        contextInstanceRecoveryManager.recoverContextInstances();

        // Assert expectations.
        Assert.assertEquals(true, ContextMachineCache.instance().containsContextName("instance-name"));
        Assert.assertEquals(true, ContextMachineCache.instance().containsInstanceIdentifier("instance-id"));
        Assert.assertNotNull(ContextMachineCache.instance().getByContextName("instance-name"));
        Assert.assertNotNull(ContextMachineCache.instance().getByContextInstanceId("instance-id"));
        Assert.assertEquals(1, ContextMachineCache.instance().contextInstanceIdentifiers().size());
        Assert.assertEquals("instance-id", ContextMachineCache.instance().contextInstanceIdentifiers().iterator().next());
        Assert.assertEquals(1, ContextMachineCache.instance().contextNames().size());
        Assert.assertEquals("instance-name", ContextMachineCache.instance().contextNames().iterator().next());
    }
}
