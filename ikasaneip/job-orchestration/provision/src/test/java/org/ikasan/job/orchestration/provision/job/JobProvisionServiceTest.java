package org.ikasan.job.orchestration.provision.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.configuration.metadata.model.SolrConfigurationMetaData;
import org.ikasan.configuration.metadata.model.SolrConfigurationParameterMetaData;
import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.builder.context.ContextParameterBuilder;
import org.ikasan.job.orchestration.builder.job.FileEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.InternalEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.QuartzScheduleDrivenJobBuilder;
import org.ikasan.module.metadata.model.SolrModuleMetaDataImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
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
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
// todo fix this test
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
    private MetaDataService metaDataRestService;
    @Mock
    private ModuleMetaData agent;
    @Mock
    Environment environment;

    private ObjectMapper objectMapper = new ObjectMapper();



    @Test
    public void test() throws IOException {
        when(moduleMetaDataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(this.getModuleMetaData(super.loadDataFile("/data/scheduler-agent1-module-metadata.json")),
                this.getModuleMetaData(super.loadDataFile("/data/scheduler-agent1-module-metadata.json")),
                this.getModuleMetaData(super.loadDataFile("/data/scheduler-agent1-module-metadata.json")))
                , 3, 3));

        when(configurationRestService.getModuleConfiguration(anyString())).thenReturn(this.getConfigurarationMetaData());

        when(this.moduleControlRestService.changeModuleActivationState(anyString(), anyString(), anyString())).thenReturn(true);

        when(this.metaDataRestService.getModuleMetadata(anyString(), anyString())).thenReturn(Optional.of(agent));

        when(this.configurationRestService.getConfiguredResourceConfiguration(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(this.getConfigurarationMetaData());

        JobProvisionServiceImpl jobProvisionService = new JobProvisionServiceImpl(schedulerJobService,
            moduleMetaDataService, null);

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

        jobProvisionService.provisionJobs(schedulerJobs);
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
            .withContextId(contextId)
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
            .withContextId(contextId)
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
            .withContextId(contextId)
            .withAgentName(agentName);

        return fileEventDrivenJobBuilder.build();
    }

    private List<ContextParameter> getContextParameters() {
        ArrayList<ContextParameter> contextParameters = new ArrayList<>();

        IntStream.range(0, 10).forEach(i -> {
            ContextParameterBuilder contextParameterBuilder = new ContextParameterBuilder();
            contextParameterBuilder.withName("param-name-"+i);
            contextParameterBuilder.withType("java.lang.String");

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
