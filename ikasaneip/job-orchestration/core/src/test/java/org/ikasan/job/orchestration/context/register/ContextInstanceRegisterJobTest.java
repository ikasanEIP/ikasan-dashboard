package org.ikasan.job.orchestration.context.register;

import static org.ikasan.job.orchestration.context.util.InternalEventDrivenJobTestSearchResults.AGENT_NAME;
import static org.ikasan.job.orchestration.context.util.TestUtils.AGENT_URL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.recovery.CustomerBackFillerMatcher;
import org.ikasan.job.orchestration.context.util.InternalEventDrivenJobTestSearchResults;
import org.ikasan.job.orchestration.context.util.TestUtils;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.serialiser.model.JobExecutionContextDefaultImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRegisterJobTest {

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private InternalEventDrivenJobService internalEventDrivenJobService;

    @Mock
    private JobLockCacheService jobLockCacheService;

    @Mock
    private ModuleMetaDataService moduleMetadataService;

    @Mock
    private ContextParametersInstanceService contextParametersInstanceService;

    @Mock
    private ContextParametersUpdateService<ContextInstance> contextParametersUpdateService;

    private ContextInstanceRegisterJob job;

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private String contextName;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);
        job = new ContextInstanceRegisterJob(contextName, "0 0 6 ? * * *", scheduledContextService, scheduledContextInstanceService,
            schedulerService, internalEventDrivenJobService, "/data/somequeue", jobLockCacheService, contextParametersInstanceService,
            moduleMetadataService, contextParametersUpdateService);

        TestUtils.resetContextMachineCache();
        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @After
    public void tearDown() {
        TestUtils.resetContextMachineCache();
    }

    @Test
    public void execute_null_ScheduledContextRecord_should_not_npe() throws Exception {
        when(scheduledContextService.findById(contextName)).thenReturn(null);

        JobExecutionContextDefaultImpl jobExecutionContext = new JobExecutionContextDefaultImpl();

        job.execute(jobExecutionContext);

        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @Test
    public void execute_with_agents_not_found_module_metadata_does_not_npe() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("data/context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"Context1\"", "\"name\": \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1);
        when(internalEventDrivenJobService.findByContext(contextName, -1, -1)).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.findById(AGENT_NAME + "1")).thenReturn(null);

        job.execute(new JobExecutionContextDefaultImpl());

        verify(scheduledContextService).findById(contextName);
        verify(internalEventDrivenJobService).findByContext(contextName, -1, -1);
        verify(moduleMetadataService).findById(AGENT_NAME + "1");

        assertNotNull(ContextMachineCache.instance().getByContextName(contextName));

        verifyNoMoreInteractions(scheduledContextService, internalEventDrivenJobService, jobLockCacheService, scheduledContextInstanceService,
            moduleMetadataService, schedulerService, contextParametersInstanceService, contextParametersUpdateService);
    }

    @Test
    public void execute_with_agents() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("data/context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"Context1\"", "\"name\": \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

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

        job.execute(new JobExecutionContextDefaultImpl());

        assertNotNull(ContextMachineCache.instance().getByContextName(contextName));

        verify(scheduledContextService).findById(contextName);
        verify(internalEventDrivenJobService).findByContext(contextName, -1, -1);
        verify(moduleMetadataService).findById(AGENT_NAME + "1");
        verify(moduleMetadataService).findById(AGENT_NAME + "2");
        verify(moduleMetadataService).findById(AGENT_NAME + "3");
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).getAllContextParameters(contextName);
        verify(contextParametersUpdateService).update(eq(AGENT_URL + "1"), argThat(new CustomerBackFillerMatcher(contextInstance, contextName)));
        verify(contextParametersUpdateService).update(eq(AGENT_URL + "2"), argThat(new CustomerBackFillerMatcher(contextInstance, contextName)));
        verify(contextParametersUpdateService).update(eq(AGENT_URL + "3"), argThat(new CustomerBackFillerMatcher(contextInstance, contextName)));

        verifyNoMoreInteractions(scheduledContextService, internalEventDrivenJobService, scheduledContextInstanceService, jobLockCacheService,
            moduleMetadataService, schedulerService, contextParametersInstanceService, contextParametersUpdateService);
    }

    @Test
    public void execute_no_agents() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("data/context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"Context1\"", "\"name\": \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(0);
        when(internalEventDrivenJobService.findByContext(contextName, -1, -1)).thenReturn(internalEventDrivenJobRecordSearchResults);

        job.execute(new JobExecutionContextDefaultImpl());

        verify(scheduledContextService).findById(contextName);
        verify(internalEventDrivenJobService).findByContext(contextName, -1, -1);

        assertNotNull(ContextMachineCache.instance().getByContextName(contextName));

        verifyNoMoreInteractions(scheduledContextService, moduleMetadataService, jobLockCacheService, scheduledContextInstanceService,
            internalEventDrivenJobService, schedulerService, contextParametersInstanceService, contextParametersUpdateService);
    }
}