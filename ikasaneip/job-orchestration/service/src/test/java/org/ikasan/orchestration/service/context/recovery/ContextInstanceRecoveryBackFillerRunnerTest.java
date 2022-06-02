package org.ikasan.orchestration.service.context.recovery;

import static org.ikasan.orchestration.service.utils.InternalEventDrivenJobTestSearchResults.AGENT_NAME;
import static org.ikasan.orchestration.service.utils.TestUtils.AGENT_URL;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.utils.CustomerBackFillerMatcher;
import org.ikasan.orchestration.service.utils.InternalEventDrivenJobTestSearchResults;
import org.ikasan.orchestration.service.utils.TestUtils;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRecoveryBackFillerRunnerTest {
    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private ModuleMetaDataService moduleMetadataService;

    @Mock
    private InternalEventDrivenJobService internalEventDrivenJobService;

    @Mock
    private ContextParametersInstanceService contextParametersInstanceService;

    @Mock
    private ContextParametersUpdateService<ContextInstance> contextParametersUpdateService;

    @Mock
    private JobLockCacheService jobLockCacheService;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;

    private ContextInstanceRecoveryBackFillerRunner backFiller;

    private String contextName;

    private ScheduledContextRecord record;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    @Before
    public void setUp() {
        TestUtils.resetContextMachineCache();

        contextName = RandomStringUtils.randomAlphabetic(22);
        record = new ScheduledContextRecordImpl();
        record.setContextName(contextName);
        ContextTemplateImpl context = new ContextTemplateImpl();
        context.setName(contextName);
        record.setContext(context);

        backFiller = new ContextInstanceRecoveryBackFillerRunner("bigQueue/Dir",
            scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            record,
            JobLockCacheImpl.instance(),
            schedulerJobInstanceService
        );

        assertEquals(0, ContextMachineCache.instance().contextNames().size());
    }

    @After
    public void tearDown() {
        TestUtils.resetContextMachineCache();
    }

    @Test
    public void should_create_instance_and_populate_params_and_save_instance_with_agents() throws Exception {
        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        when(internalEventDrivenJobService.findByContext(contextName, -1, -1)).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.findById(AGENT_NAME + "1")).thenReturn(TestUtils.createModuleMetaData("1"));
        when(moduleMetadataService.findById(AGENT_NAME + "2")).thenReturn(TestUtils.createModuleMetaData("2"));
        when(moduleMetadataService.findById(AGENT_NAME + "3")).thenReturn(TestUtils.createModuleMetaData("3"));

        List<ContextParameterInstance> params = TestUtils.createParams();
        when(contextParametersInstanceService.getAllContextParameters(contextName)).thenReturn(params);

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        backFiller.run();

        verify(internalEventDrivenJobService).findByContext(contextName, -1, -1);
        verify(moduleMetadataService).findById(AGENT_NAME + "1");
        verify(moduleMetadataService).findById(AGENT_NAME + "2");
        verify(moduleMetadataService).findById(AGENT_NAME + "3");
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).getAllContextParameters(contextName);
        verify(contextParametersUpdateService).update(eq(AGENT_URL + "1"), argThat(new CustomerBackFillerMatcher(contextInstance, contextName)));
        verify(contextParametersUpdateService).update(eq(AGENT_URL + "2"), argThat(new CustomerBackFillerMatcher(contextInstance, contextName)));
        verify(contextParametersUpdateService).update(eq(AGENT_URL + "3"), argThat(new CustomerBackFillerMatcher(contextInstance, contextName)));
        verify(schedulerJobInstanceService).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            schedulerJobInstanceService
        );

        assertNotNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @Test
    public void should_create_instance_and_populate_params_and_save_instance_with_no_agents() throws Exception {
        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(0);
        when(internalEventDrivenJobService.findByContext(contextName, -1, -1)).thenReturn(internalEventDrivenJobRecordSearchResults);

        backFiller.run();

        verify(schedulerJobInstanceService).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));

        verify(internalEventDrivenJobService).findByContext(contextName, -1, -1);
        verify(scheduledContextInstanceService).save(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            schedulerJobInstanceService
        );

        assertNotNull(ContextMachineCache.instance().getByContextName(contextName));
    }
}