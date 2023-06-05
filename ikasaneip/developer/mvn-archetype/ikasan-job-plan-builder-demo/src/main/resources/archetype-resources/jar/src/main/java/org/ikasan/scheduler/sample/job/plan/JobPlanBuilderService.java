package org.ikasan.scheduler.sample.job.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.job.orchestration.builder.context.ContextParameterBuilder;
import org.ikasan.job.orchestration.builder.context.ContextTemplateBuilder;
import org.ikasan.job.orchestration.builder.job.FileEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.GlobalEventJobBuilder;
import org.ikasan.job.orchestration.builder.job.InternalEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.QuartzScheduleDrivenJobBuilder;
import org.ikasan.job.orchestration.model.context.ContextBundleImpl;
import org.ikasan.orchestration.service.context.util.ContextExportZipUtils;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobPlanBuilderService {

    private Logger logger = LoggerFactory.getLogger(JobPlanBuilderService.class);

    private Map<String, FileEventDrivenJob> fileEventDrivenJobs;
    private Map<String, InternalEventDrivenJob> internalEventDrivenJobs;
    private Map<String, QuartzScheduleDrivenJob> quartzScheduleDrivenJobs;
    private Map<String, GlobalEventJob> globalEventJobs;

    private String zipOutputDir;

    /**
     * Constructor
     *
     * @param zipOutputDir
     */
    public JobPlanBuilderService(String zipOutputDir) {
        this.zipOutputDir = zipOutputDir;
        if (this.zipOutputDir == null) {
            throw new IllegalArgumentException("zipOutputDir cannot be null!");
        }
    }

    /**
     * Method to build the context bundle.
     *
     * @return
     * @throws JsonProcessingException
     */
    public ContextBundle buildContext() throws JsonProcessingException {
        logger.info("Starting to build context bundle");
        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();

        contextTemplateBuilder.withName("MyFirstJobPlan")
                .withDescription("This is a sample context template used to demonstrate the builder " +
                        "classes associated with the Ikasan Enterprise Scheduler.")
                .withTimeWindowStartCronExpression("0 0 1 ? * * *")
                .withContextTtlMilliseconds(82800000L);

        QuartzScheduleDrivenJob quartzScheduleDrivenJob = this.buildQuartzScheduleDrivenJob("7amScheduledEvent",
                "scheduler-agent", "This event fires at 7am Monday through Friday."
                , List.of("MyFirstJobPlan"), "MyFirstJobPlan", "* * 7 ? * * *");


        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName(quartzScheduleDrivenJob.getJobName())
                .withAgentName(quartzScheduleDrivenJob.getAgentName())
                .withDescription(quartzScheduleDrivenJob.getJobDescription())
                .build());

        InternalEventDrivenJob sampleCommandExecutionJob1 = this.buildCommandExecutionJob("SampleCommandExecutionJob1", "scheduler-agent",
                "This is a simple script to demonstrate how command execution jobs work.", List.of("ScriptProcessingContext", "MyFirstJobPlan"),
                "MyFirstJobPlan", "echo \"Running Job :- SampleCommandExecutionJob1\"\nls -la\nsleep 10\necho $sample_param",
                List.of(this.buildContextParameter("sample_param", "sample default value")));

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName(sampleCommandExecutionJob1.getJobName())
                .withAgentName(sampleCommandExecutionJob1.getAgentName())
                .withDescription(sampleCommandExecutionJob1.getJobDescription())
                .build());

                // Add the job dependencies
        contextTemplateBuilder.addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(quartzScheduleDrivenJob.getJobName())
                        .withAgentName(quartzScheduleDrivenJob.getAgentName())
                        .build())
                .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(sampleCommandExecutionJob1.getJobName())
                        .withAgentName(sampleCommandExecutionJob1.getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(quartzScheduleDrivenJob.getAgentName())
                                        .withJobName(quartzScheduleDrivenJob.getJobName())
                                        .build())
                                .build())
                        .build())
                .withTreeViewExpandLevel(1);

        contextTemplateBuilder.addContext(this.buildScriptProcessingContext())
                .addContext(this.buildGlobalEventContext());

        ContextTemplate contextTemplate = contextTemplateBuilder.build();

        ArrayList<SchedulerJob> allJobs = new ArrayList<>();
        allJobs.addAll(this.globalEventJobs.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList()));
        allJobs.addAll(this.internalEventDrivenJobs.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList()));
        allJobs.addAll(this.quartzScheduleDrivenJobs.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList()));
        allJobs.addAll(this.fileEventDrivenJobs.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList()));

        try(OutputStream outputStream = new FileOutputStream(this.zipOutputDir+contextTemplate.getName()+".zip")) {
            ContextExportZipUtils.createZipFile(contextTemplate, ".", allJobs,false)
                    .writeTo(outputStream);
            logger.info("Wrote: "+this.zipOutputDir+contextTemplate.getName()+".zip");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, allJobs, new ArrayList<>(),
                new ArrayList<>(), null);

        logger.info("Context bundle created!");
        return contextBundle;
    }

    /**
     * Method to build the ScriptProcessingContext
     * @return
     * @throws JsonProcessingException
     */
    private ContextTemplate buildScriptProcessingContext() throws JsonProcessingException {
        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();

        contextTemplateBuilder.withName("ScriptProcessingContext")
                .withDescription("This context demonstrates executing some command execution jobs along with a file job");


        InternalEventDrivenJob sampleCommandExecutionJob2 = this.buildCommandExecutionJob("SampleCommandExecutionJob2", "scheduler-agent",
                "This is a simple script to demonstrate how command execution jobs work.", List.of("ScriptProcessingContext"),
                "MyFirstJobPlan", "echo \"Running Job :- SampleCommandExecutionJob1\"\nls -la\nsleep 10\necho $sample_param",
                List.of(this.buildContextParameter("sample_param", "sample default value")));

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                        .withJobName(sampleCommandExecutionJob2.getJobName())
                        .withAgentName(sampleCommandExecutionJob2.getAgentName())
                        .withDescription(sampleCommandExecutionJob2.getJobDescription())
                        .build());

        InternalEventDrivenJob sampleCommandExecutionJob3 = this.buildCommandExecutionJob("SampleCommandExecutionJob3", "scheduler-agent",
                "This is a simple script to demonstrate how command execution jobs work.", List.of("ScriptProcessingContext"),
                "MyFirstJobPlan", "echo \"Running Job :- SampleCommandExecutionJob1\"\nls -la\nsleep 10\necho $sample_param",
                List.of(this.buildContextParameter("sample_param", "sample default value")));

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                        .withJobName(sampleCommandExecutionJob3.getJobName())
                        .withAgentName(sampleCommandExecutionJob3.getAgentName())
                        .withDescription(sampleCommandExecutionJob3.getJobDescription())
                        .build());

        InternalEventDrivenJob sampleCommandExecutionJob4 = this.buildCommandExecutionJob("SampleCommandExecutionJob4", "scheduler-agent",
                "This is a simple script to demonstrate how command execution jobs work.", List.of("ScriptProcessingContext"),
                "MyFirstJobPlan", "echo \"Running Job :- SampleCommandExecutionJob1\"\nls -la\nsleep 10\necho $sample_param",
                List.of(this.buildContextParameter("sample_param", "sample default value")));

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                        .withJobName(sampleCommandExecutionJob4.getJobName())
                        .withAgentName(sampleCommandExecutionJob4.getAgentName())
                        .withDescription(sampleCommandExecutionJob4.getJobDescription())
                        .build());

        InternalEventDrivenJob sampleCommandExecutionJob5 = this.buildCommandExecutionJob("SampleCommandExecutionJob4", "scheduler-agent",
                "This is a simple script to demonstrate how command execution jobs work.", List.of("ScriptProcessingContext"),
                "MyFirstJobPlan", "echo \"Running Job :- SampleCommandExecutionJob1\"\nls -la\nsleep 10\necho $sample_param",
                List.of(this.buildContextParameter("sample_param", "sample default value")));

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                        .withJobName(sampleCommandExecutionJob5.getJobName())
                        .withAgentName(sampleCommandExecutionJob5.getAgentName())
                        .withDescription(sampleCommandExecutionJob5.getJobDescription())
                        .build());

        InternalEventDrivenJob sampleCommandExecutionJob6 = this.buildCommandExecutionJob("SampleCommandExecutionJob6", "scheduler-agent",
                "This is a simple script to demonstrate how command execution jobs work.", List.of("ScriptProcessingContext", "MyGlobalEventContext"),
                "MyFirstJobPlan", "echo \"Running Job :- SampleCommandExecutionJob1\"\nls -la\nsleep 10\necho $sample_param",
                List.of(this.buildContextParameter("sample_param", "sample default value")));

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                        .withJobName(sampleCommandExecutionJob6.getJobName())
                        .withAgentName(sampleCommandExecutionJob6.getAgentName())
                        .withDescription(sampleCommandExecutionJob6.getJobDescription())
                        .build());
        
        QuartzScheduleDrivenJob fileWatcherJob1 = this.buildFileEventDrivenJob("FileWatcherJob1", "scheduler-agent", "This is a file watcher job looking for a file on the file system.",
                List.of("ScriptProcessingContext"), "MyFirstJobPlan", "0 0/1 * * * ?", "the file path", List.of("file.1.txt"));

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName(fileWatcherJob1.getJobName())
                .withAgentName(fileWatcherJob1.getAgentName())
                .withDescription(fileWatcherJob1.getJobDescription())
                .build());

        InternalEventDrivenJob sampleCommandExecutionJob1 = this.internalEventDrivenJobs.get("SampleCommandExecutionJob1");

        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName(sampleCommandExecutionJob1.getJobName())
                .withAgentName(sampleCommandExecutionJob1.getAgentName())
                .withDescription(sampleCommandExecutionJob1.getJobDescription())
                .build());

                // Add the job dependencies
        contextTemplateBuilder.addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(sampleCommandExecutionJob2.getJobName())
                        .withAgentName(sampleCommandExecutionJob2.getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(sampleCommandExecutionJob1.getAgentName())
                                        .withJobName(sampleCommandExecutionJob1.getJobName())
                                        .build())
                                .build())
                        .build())
                .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(sampleCommandExecutionJob3.getJobName())
                        .withAgentName(sampleCommandExecutionJob3.getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(sampleCommandExecutionJob2.getAgentName())
                                        .withJobName(sampleCommandExecutionJob2.getJobName())
                                        .build())
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(fileWatcherJob1.getAgentName())
                                        .withJobName(fileWatcherJob1.getJobName())
                                        .build())
                                .build())
                        .build())
                .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(sampleCommandExecutionJob4.getJobName())
                        .withAgentName(sampleCommandExecutionJob4.getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(sampleCommandExecutionJob3.getAgentName())
                                        .withJobName(sampleCommandExecutionJob3.getJobName())
                                        .build())
                                .build())
                        .build())
                .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(sampleCommandExecutionJob5.getJobName())
                        .withAgentName(sampleCommandExecutionJob5.getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(sampleCommandExecutionJob3.getAgentName())
                                        .withJobName(sampleCommandExecutionJob3.getJobName())
                                        .build())
                                .build())
                        .build())
                .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(sampleCommandExecutionJob6.getJobName())
                        .withAgentName(sampleCommandExecutionJob6.getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(sampleCommandExecutionJob5.getAgentName())
                                        .withJobName(sampleCommandExecutionJob5.getJobName())
                                        .build())
                                .build())
                        .build())
                .withTreeViewExpandLevel(1)
                .build();

        return contextTemplateBuilder.build();
    }

    /**
     * Mehtod to build MyGlobalEventContext
     *
     * @return
     */
    private ContextTemplate buildGlobalEventContext() {
        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();

        contextTemplateBuilder.withName("MyGlobalEventContext")
                .withDescription("This is a sample context template that executes a Global Event Job after all upstream jobs are complete.")
                .withTimeWindowStartCronExpression("* * 1 ? * * *")
                .withContextTtlMilliseconds(82800000L);

        GlobalEventJob globalEventJob1 = this.buildGlobalEventJob("GlobalEventJob1", "This is a sample global job!",
                List.of("MyGlobalEventContext"), "MyFirstJobPlan");

        InternalEventDrivenJob sampleCommandExecutionJob6 = this.internalEventDrivenJobs.get("SampleCommandExecutionJob6");

        // add the scheduler jobs that will be orchestrated
        contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                        .withJobName(globalEventJob1.getJobName())
                        .withAgentName(globalEventJob1.getAgentName())
                        .withDescription(globalEventJob1.getJobDescription())
                        .build())
                .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                        .withJobName(sampleCommandExecutionJob6.getJobName())
                        .withAgentName(sampleCommandExecutionJob6.getAgentName())
                        .withDescription(sampleCommandExecutionJob6.getJobDescription())
                        .build())
                .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(globalEventJob1.getJobName())
                        .withAgentName(globalEventJob1.getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                                .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                        .withAgentName(sampleCommandExecutionJob6.getAgentName())
                                        .withJobName(sampleCommandExecutionJob6.getJobName())
                                        .build())
                                .build())
                        .build())
                .withTreeViewExpandLevel(1)
                .build();

        return contextTemplateBuilder.build();
    }

    /**
     * Method to build a file watcher job.
     *
     * @param jobName
     * @param agentName
     * @param jobDescription
     * @param childContextIds
     * @param contextName
     * @param cronExpression
     * @param filePath
     * @param filenames
     * @return
     */
    private FileEventDrivenJob buildFileEventDrivenJob(String jobName, String agentName, String jobDescription
            , List<String> childContextIds, String contextName, String cronExpression, String filePath, List<String> filenames) {
        FileEventDrivenJobBuilder fileEventDrivenJobBuilder = new FileEventDrivenJobBuilder();

        ArrayList finalFileNames = new ArrayList();
        finalFileNames.addAll(filenames);

        fileEventDrivenJobBuilder.withFilePath(filePath)
                .withFilenames(finalFileNames)
                .withDirectoryDepth(1)
                .withMinFileAgeSeconds(30)
                .withCronExpression(cronExpression)
                .withAgentName(agentName)
                .withContextName(contextName)
                .withDescription(jobDescription)
                .withJobName(jobName);

        childContextIds.forEach(id -> fileEventDrivenJobBuilder.addChildContextId(id));

        FileEventDrivenJob fileEventDrivenJob = fileEventDrivenJobBuilder.build();

        if(this.fileEventDrivenJobs == null) {
            this.fileEventDrivenJobs = new HashMap<>();
        }

        this.fileEventDrivenJobs.put(fileEventDrivenJob.getJobName(), fileEventDrivenJob);

        return fileEventDrivenJob;
    }

    /**
     * Method to build command execution jobs,
     *
     * @param jobName
     * @param agentName
     * @param jobDescription
     * @param childContextIds
     * @param contextName
     * @param commandLine
     * @param contextParameters
     * @return
     */
    private InternalEventDrivenJob buildCommandExecutionJob(String jobName, String agentName, String jobDescription
            , List<String> childContextIds, String contextName, String commandLine, List<ContextParameter> contextParameters) {
        InternalEventDrivenJobBuilder internalEventDrivenJobBuilder = new InternalEventDrivenJobBuilder();
        internalEventDrivenJobBuilder
                .withCommandLine(commandLine)
                .withMaxExecutionTime(1000000L)
                .withMinExecutionTime(10L)
                .withContextName(contextName)
                .withAgentName(agentName)
                .withDescription(jobDescription)
                .withJobName(jobName);

        childContextIds.forEach(id -> internalEventDrivenJobBuilder.addChildContextId(id));
        contextParameters.forEach(contextParameter -> internalEventDrivenJobBuilder.addContextParameter(contextParameter));

        InternalEventDrivenJob internalEventDrivenJob = internalEventDrivenJobBuilder.build();

        if(this.internalEventDrivenJobs == null) {
            this.internalEventDrivenJobs = new HashMap<>();
        }

        this.internalEventDrivenJobs.put(internalEventDrivenJob.getJobName(), internalEventDrivenJob);

        return internalEventDrivenJob;
    }

    /**
     * Method to build quartz schedule jobs.
     *
     * @param jobName
     * @param agentName
     * @param jobDescription
     * @param childContextIds
     * @param contextName
     * @param cronExpression
     * @return
     */
    private QuartzScheduleDrivenJob buildQuartzScheduleDrivenJob(String jobName, String agentName, String jobDescription
            , List<String> childContextIds, String contextName, String cronExpression) {
        QuartzScheduleDrivenJobBuilder quartzScheduleDrivenJobBuilder = new QuartzScheduleDrivenJobBuilder();

        quartzScheduleDrivenJobBuilder.withCronExpression(cronExpression)
                .withAgentName(agentName)
                .withContextName(contextName)
                .withDescription(jobDescription)
                .withJobName(jobName);

        childContextIds.forEach(id -> quartzScheduleDrivenJobBuilder.addChildContextId(id));

        QuartzScheduleDrivenJob quartzScheduleDrivenJob = quartzScheduleDrivenJobBuilder.build();

        if(this.quartzScheduleDrivenJobs == null) {
            this.quartzScheduleDrivenJobs = new HashMap<>();
        }

        this.quartzScheduleDrivenJobs.put(quartzScheduleDrivenJob.getJobName(), quartzScheduleDrivenJob);

        return quartzScheduleDrivenJob;
    }

    /**
     * Method to build global event jobs.
     *
     * @param jobName
     * @param jobDescription
     * @param childContextIds
     * @param contextName
     * @return
     */
    private GlobalEventJob buildGlobalEventJob(String jobName, String jobDescription
            , List<String> childContextIds, String contextName) {
        GlobalEventJobBuilder globalEventJobBuilder = new GlobalEventJobBuilder();

        globalEventJobBuilder
                .withAgentName(JobConstants.GLOBAL_EVENT)
                .withContextName(contextName)
                .withDescription(jobDescription)
                .withJobName(jobName);

        childContextIds.forEach(id -> globalEventJobBuilder.addChildContextId(id));

        GlobalEventJob globalEventJob = globalEventJobBuilder.build();

        if(this.globalEventJobs == null) {
            this.globalEventJobs = new HashMap<>();
        }

        this.globalEventJobs.put(globalEventJob.getJobName(), globalEventJob);

        return globalEventJob;
    }

    /**
     * Method to build context parameters.
     *
     * @param name
     * @param defaultValue
     * @return
     */
    private ContextParameter buildContextParameter(String name, String defaultValue) {
        return new ContextParameterBuilder()
                .withName(name)
                .withDefaultValue(defaultValue)
                .build();
    }
}
