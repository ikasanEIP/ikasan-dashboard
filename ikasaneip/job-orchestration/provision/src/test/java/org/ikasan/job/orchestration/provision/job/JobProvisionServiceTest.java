package org.ikasan.job.orchestration.provision.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.configuration.metadata.model.SolrConfigurationMetaData;
import org.ikasan.configuration.metadata.model.SolrConfigurationParameterMetaData;
import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.builder.context.ContextParameterBuilder;
import org.ikasan.job.orchestration.builder.job.FileEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.GlobalEventJobBuilder;
import org.ikasan.job.orchestration.builder.job.InternalEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.QuartzScheduleDrivenJobBuilder;
import org.ikasan.module.metadata.model.SolrModuleMetaDataImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobRecordImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JobProvisionServiceTest extends AbstractTest {
    @Mock
    private SchedulerJobService schedulerJobService;
    @Mock
    private ConfigurationService configurationRestService;
    @Mock
    private ModuleControlService moduleControlRestService;
    @Mock
    private ModuleMetaDataService moduleMetaDataService;
    @Mock
    private JobProvisionModuleService jobProvisionModuleRestService;
    @Mock
    private ModuleMetaData agent;

    private ObjectMapper objectMapper = new ObjectMapper();


    @Test
    public void test_provision_jobs_success() throws IOException {
        when(moduleMetaDataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(this.getModuleMetaData(super.loadDataFile("/data/scheduler-agent1-module-metadata.json")),
                this.getModuleMetaData(super.loadDataFile("/data/scheduler-agent1-module-metadata.json")),
                this.getModuleMetaData(super.loadDataFile("/data/scheduler-agent1-module-metadata.json")))
                , 3, 3));

        JobProvisionServiceImpl jobProvisionService = new JobProvisionServiceImpl(schedulerJobService,
            moduleMetaDataService, jobProvisionModuleRestService);

        jobProvisionService.provisionJobs(this.createSchedulerJobs(), "system");

        verify(schedulerJobService, times(3)).deleteByContextName(anyString());
        verify(schedulerJobService, times(3)).saveQuartzScheduledJobs(anyList(), anyString());
        verify(schedulerJobService, times(3)).saveInternalEventDrivenJobs(anyList(), anyString());
        verify(schedulerJobService, times(3)).saveFileEventDrivenJobs(anyList(), anyString());
        verify(schedulerJobService, times(3)).saveGlobalEventJobs(anyList(), anyString());
        verify(jobProvisionModuleRestService, times(3)).provisionJobs(anyString(), any());
        verify(moduleMetaDataService, times(1)).find(anyList(), any(), anyInt(), anyInt());


        verifyNoMoreInteractions(configurationRestService
            , moduleControlRestService
            , schedulerJobService
            , jobProvisionModuleRestService
            , moduleMetaDataService);
    }

    @Test
    public void test_remove_jobs_for_context_success() throws IOException {
        JobProvisionServiceImpl jobProvisionService = new JobProvisionServiceImpl(schedulerJobService,
            moduleMetaDataService, jobProvisionModuleRestService);

        List<SchedulerJob> schedulerJobs = this.createSchedulerJobs();
        List<SchedulerJobRecord> schedulerJobRecords = new ArrayList<>();
        schedulerJobs.forEach(job -> {
            TestSchedulerJobRecord schedulerJobRecord = new TestSchedulerJobRecord(job);
            schedulerJobRecords.add(schedulerJobRecord);
        });

        SearchResults<SchedulerJobRecord> searchResults = new SearchResultsImpl<>(schedulerJobRecords, schedulerJobRecords.size(), 100L);

        when(schedulerJobService.findByContext(anyString(), anyInt(), anyInt())).thenReturn(searchResults);

        ModuleMetadataSearchResults moduleMetadataSearchResults = new ModuleMetadataSearchResults(List.of(agent)
            , 1, 100L);

        when(moduleMetaDataService.find(anyList(), any(), anyInt(), anyInt())).thenReturn(moduleMetadataSearchResults);
        when(agent.getUrl()).thenReturn("url");

        jobProvisionService.removeJobs("contextName");

        verify(schedulerJobService).findByContext(anyString(), anyInt(), anyInt());
        verify(moduleMetaDataService).find(anyList(), any(), anyInt(), anyInt());
        verify(this.jobProvisionModuleRestService).removeJobsForContext("url", "contextName");

        verifyNoMoreInteractions(configurationRestService
            , moduleControlRestService
            , schedulerJobService
            , jobProvisionModuleRestService
            , moduleMetaDataService);
    }

    @Test
    public void test_remove_jobs_for_context_success_no_unique_jobs() throws IOException {
        JobProvisionServiceImpl jobProvisionService = new JobProvisionServiceImpl(schedulerJobService,
            moduleMetaDataService, jobProvisionModuleRestService);

        List<SchedulerJob> schedulerJobs = this.createSchedulerJobs();
        List<SchedulerJobRecord> schedulerJobRecords = new ArrayList<>();
        schedulerJobs.forEach(job -> {
            TestSchedulerJobRecord schedulerJobRecord = new TestSchedulerJobRecord(job);
            schedulerJobRecords.add(schedulerJobRecord);
        });

        SearchResults<SchedulerJobRecord> searchResults = new SearchResultsImpl<>(schedulerJobRecords, schedulerJobRecords.size(), 100L);

        when(schedulerJobService.findByContext(anyString(), anyInt(), anyInt())).thenReturn(searchResults);

        ModuleMetadataSearchResults moduleMetadataSearchResults = new ModuleMetadataSearchResults(List.of()
            , 1, 100L);

        when(moduleMetaDataService.find(anyList(), any(), anyInt(), anyInt())).thenReturn(moduleMetadataSearchResults);

        jobProvisionService.removeJobs("contextName");

        verify(schedulerJobService).findByContext(anyString(), anyInt(), anyInt());
        verify(moduleMetaDataService).find(anyList(), any(), anyInt(), anyInt());

        verifyNoMoreInteractions(configurationRestService
            , moduleControlRestService
            , schedulerJobService
            , jobProvisionModuleRestService
            , moduleMetaDataService);
    }

    private List<SchedulerJob> createSchedulerJobs() {
        List<SchedulerJob> schedulerJobs = new ArrayList<>();

        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent1", "contextId", "description", "jobName1", getContextParameters(), List.of("1")));
        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent1", "contextId", "description", "jobName2", getContextParameters(), List.of("1")));
        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent1", "contextId", "description", "jobName3", getContextParameters(), List.of("1")));

        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent2", "contextId", "description", "jobName1", getContextParameters(), List.of("1")));
        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent2", "contextId", "description", "jobName2", getContextParameters(), List.of("1")));
        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent2", "contextId", "description", "jobName3", getContextParameters(), List.of("1")));

        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent3", "contextId", "description", "jobName1", getContextParameters(), List.of("1")));
        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent3", "contextId", "description", "jobName2", getContextParameters(), List.of("1")));
        schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "agent3", "contextId", "description", "jobName3", getContextParameters(), List.of("1")));

        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent1", "contextId", "description"
            , "quartz-jobName1", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent1", "contextId", "description"
            , "quartz-jobName2", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent1", "contextId", "description"
            , "quartz-jobName3", "jobGroup", "cronExpression", "timezone"));

        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent2", "contextId", "description"
            , "quartz-jobName1", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent2", "contextId", "description"
            , "quartz-jobName2", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent2", "contextId", "description"
            , "quartz-jobName3", "jobGroup", "cronExpression", "timezone"));

        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent3", "contextId", "description"
            , "quartz-jobName1", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent3", "contextId", "description"
            , "quartz-jobName2", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createQuartzScheduleDrivenJob("agent3", "contextId", "description"
            , "quartz-jobName3", "jobGroup", "cronExpression", "timezone"));

        schedulerJobs.add(this.createFileEventDrivenJob("agent1", "contextId", "description"
            , "file-jobName1", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createFileEventDrivenJob("agent1", "contextId", "description"
            , "file-jobName2", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createFileEventDrivenJob("agent1", "contextId", "description"
            , "file-jobName3", "jobGroup", "cronExpression", "timezone"));

        schedulerJobs.add(this.createFileEventDrivenJob("agent2", "contextId", "description"
            , "file-jobName1", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createFileEventDrivenJob("agent2", "contextId", "description"
            , "file-jobName2", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createFileEventDrivenJob("agent2", "contextId", "description"
            , "file-jobName3", "jobGroup", "cronExpression", "timezone"));

        schedulerJobs.add(this.createFileEventDrivenJob("agent3", "contextId", "description"
            , "file-jobName1", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createFileEventDrivenJob("agent3", "contextId", "description"
            , "file-jobName2", "jobGroup", "cronExpression", "timezone"));
        schedulerJobs.add(this.createFileEventDrivenJob("agent3", "contextId", "description"
            , "file-jobName3", "jobGroup", "cronExpression", "timezone"));

        schedulerJobs.add(this.createGlobalEventJob("agent1", "contextId", "description"
            , "global-jobName1"));
        schedulerJobs.add(this.createGlobalEventJob("agent1", "contextId", "description"
            , "global-jobName2"));
        schedulerJobs.add(this.createGlobalEventJob("agent1", "contextId", "description"
            , "global-jobName3"));

        schedulerJobs.add(this.createGlobalEventJob("agent2", "contextId", "description"
            , "global-jobName1"));
        schedulerJobs.add(this.createGlobalEventJob("agent2", "contextId", "description"
            , "global-jobName2"));
        schedulerJobs.add(this.createGlobalEventJob("agent2", "contextId", "description"
            , "global-jobName3"));

        schedulerJobs.add(this.createGlobalEventJob("agent3", "contextId", "description"
            , "global-jobName1"));
        schedulerJobs.add(this.createGlobalEventJob("agent3", "contextId", "description"
            , "global-jobName2"));
        schedulerJobs.add(this.createGlobalEventJob("agent3", "contextId", "description"
            , "global-jobName3"));

        return schedulerJobs;
    }

    private InternalEventDrivenJob createInternalEventDrivenJob(String commandLine, String workingDirectory
        , String agentName, String contextId, String description, String jobName, List<ContextParameter> contextParameters
        , List<String> successfulReturnCodes) {
        InternalEventDrivenJobBuilder fileEventDrivenJobBuilder = new InternalEventDrivenJobBuilder();
        fileEventDrivenJobBuilder
            .withCommandLine(commandLine)
            .withMaxExecutionTime(100000L)
            .withMinExecutionTime(1000L)
            .withWorkingDirectory(workingDirectory)
            .addSuccessfulReturnCode("0")
            .withAgentName(agentName)
            .withContextName(contextId)
            .withDescription(description)
            .withJobName(jobName);

        contextParameters.forEach(contextParameter
            -> fileEventDrivenJobBuilder.addContextParameter(contextParameter));
        successfulReturnCodes.forEach(successfulReturnCode
            -> fileEventDrivenJobBuilder.addSuccessfulReturnCode(successfulReturnCode));

        return fileEventDrivenJobBuilder.build();
    }

    private QuartzScheduleDrivenJob createQuartzScheduleDrivenJob(String agentName, String contextId, String description, String jobName, String jobGroup
        , String cronExpression, String timezone) {
        QuartzScheduleDrivenJobBuilder quartzScheduleDrivenJobBuilder = new QuartzScheduleDrivenJobBuilder();

        quartzScheduleDrivenJobBuilder.withCronExpression(cronExpression)
            .withTimeZone(timezone)
            .withJobGroup(jobGroup)
            .withDescription(description)
            .withJobName(jobName)
            .withContextName(contextId)
            .withAgentName(agentName);

        return quartzScheduleDrivenJobBuilder.build();
    }

    private FileEventDrivenJob createFileEventDrivenJob(String agentName, String contextId, String description, String jobName, String jobGroup
        , String cronExpression, String timezone) {
        FileEventDrivenJobBuilder fileEventDrivenJobBuilder = new FileEventDrivenJobBuilder();

        fileEventDrivenJobBuilder
            .withFilePath("./some-file.txt")
            .withCronExpression(cronExpression)
            .withTimeZone(timezone)
            .withJobGroup(jobGroup)
            .withDescription(description)
            .withJobName(jobName)
            .withContextName(contextId)
            .withAgentName(agentName);

        return fileEventDrivenJobBuilder.build();
    }

    private GlobalEventJob createGlobalEventJob(String agentName, String contextId, String description, String jobName) {
        GlobalEventJobBuilder globalEventJobBuilder = new GlobalEventJobBuilder();

        globalEventJobBuilder
            .withDescription(description)
            .withJobName(jobName)
            .withContextName(contextId)
            .withAgentName(agentName);

        return globalEventJobBuilder.build();
    }

    private List<ContextParameter> getContextParameters() {
        ArrayList<ContextParameter> contextParameters = new ArrayList<>();

        IntStream.range(0, 10).forEach(i -> {
            ContextParameterBuilder contextParameterBuilder = new ContextParameterBuilder();
            contextParameterBuilder.withName("param-name-"+i);
            contextParameterBuilder.withDefaultValue("defaultValue");

            contextParameters.add(contextParameterBuilder.build());
        });

        return contextParameters;
    }

    private ModuleMetaData getModuleMetaData(String moduleMetaData) throws JsonProcessingException {
        return this.objectMapper.readValue(moduleMetaData, SolrModuleMetaDataImpl.class);
    }

    private SolrConfigurationMetaData getConfigurarationMetaData() {
        SolrConfigurationMetaData solrConfigurationMetaData = new SolrConfigurationMetaData();
        solrConfigurationMetaData.setParameters(this.getConfigurationParameterMetaDataList());

        return solrConfigurationMetaData;
    }

    private List<SolrConfigurationParameterMetaData> getConfigurationParameterMetaDataList() {
        List<SolrConfigurationParameterMetaData> solrConfigurationParameterMetaDataList = new ArrayList<>();

        SolrConfigurationParameterMetaData solrConfigurationParameterMetaData = new SolrConfigurationParameterMetaData();
        solrConfigurationParameterMetaData.setName("flowDefinitions");
        solrConfigurationParameterMetaData.setValue(new HashMap<String, String>());
        solrConfigurationParameterMetaData.setImplementingClass(Map.class.getName());
        solrConfigurationParameterMetaData.setDescription("description");

        solrConfigurationParameterMetaDataList.add(solrConfigurationParameterMetaData);

        solrConfigurationParameterMetaData = new SolrConfigurationParameterMetaData();
        solrConfigurationParameterMetaData.setName("flowDefinitionProfiles");
        solrConfigurationParameterMetaData.setValue(new HashMap<String, String>());
        solrConfigurationParameterMetaData.setImplementingClass(Map.class.getName());
        solrConfigurationParameterMetaData.setDescription("description");

        solrConfigurationParameterMetaDataList.add(solrConfigurationParameterMetaData);

        return solrConfigurationParameterMetaDataList;
    }
}
