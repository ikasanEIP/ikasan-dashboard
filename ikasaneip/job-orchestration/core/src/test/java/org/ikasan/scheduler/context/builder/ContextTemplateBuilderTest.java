package org.ikasan.scheduler.context.builder;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

public class ContextTemplateBuilderTest extends AbstractTest {

    @Test
    public void test_builder_success() throws IOException, JSONException {
        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();

        ContextTemplate contextTemplate1 = contextTemplateBuilder.withName("Context Template Name")
            .withDescription("Context Template Description")
            .withTimeWindowStartCronExpression("* * 6 ? * * *")
            .withTimeWindowEndCronExpression("* * 15 ? * * *")

            // add some context parameters
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param1").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param2").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param3").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param4").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param5").withType("java.lang.String").build())

            // add the scheduler jobs that will be orchestrated
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job1")
                .withAgentName("AgentName")
                .withDescription("Job1 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job2")
                .withAgentName("AgentName")
                .withDescription("Job2 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job3")
                .withAgentName("AgentName")
                .withDescription("Job3 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job4")
                .withAgentName("AgentName")
                .withDescription("Job4 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job5")
                .withAgentName("AgentName")
                .withDescription("Job5 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job6")
                .withAgentName("AgentName")
                .withDescription("Job6 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job7")
                .withAgentName("AgentName")
                .withDescription("Job7 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job8")
                .withAgentName("AgentName")
                .withDescription("Job8 Description")
                .build())

            // Add the job dependencies
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job2")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job3")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job5")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job1")
                .withAgentName("AgentName")
                .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                    .addAnd(contextTemplateBuilder.getJobAndBuilder()
                        .withAgentName("AgentName")
                        .withJobName("Job2")
                        .build())
                    .addAnd(contextTemplateBuilder.getJobAndBuilder()
                        .withAgentName("AgentName")
                        .withJobName("Job3")
                        .build()).build()).build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job8")
                .withAgentName("AgentName")
                .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                    .addOr(contextTemplateBuilder.getJobOrBuilder()
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job4")
                                .build())
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job5")
                                .build()).build()).build())
                    .addOr(contextTemplateBuilder.getJobOrBuilder()
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job6")
                                .build())
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job7")
                                .build()).build()).build()).build())
                .build())
            .build();

        ContextService contextService = new ContextService();

        JSONAssert.assertEquals(super.loadDataFile("/data/context-builder-result.json"),
            contextService.getContextString(contextTemplate1), false);
    }

    @Test
    public void test_builder_success_with_context_dependencies() throws IOException, JSONException {
        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();

        ContextTemplate contextTemplate1 = contextTemplateBuilder.withName("Context Template 1")

            // add the scheduler jobs that will be orchestrated
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job1")
                .withAgentName("AgentName")
                .withDescription("Job1 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job2")
                .withAgentName("AgentName")
                .withDescription("Job2 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job3")
                .withAgentName("AgentName")
                .withDescription("Job3 Description")
                .build())

            // Add the job dependencies
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job2")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job3")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job1")
                .withAgentName("AgentName")
                .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                    .addAnd(contextTemplateBuilder.getJobAndBuilder()
                        .withAgentName("AgentName")
                        .withJobName("Job2")
                        .build())
                    .addAnd(contextTemplateBuilder.getJobAndBuilder()
                        .withAgentName("AgentName")
                        .withJobName("Job3")
                        .build()).build()).build())
            .build();

        contextTemplateBuilder = new ContextTemplateBuilder();

        ContextTemplate contextTemplate2 = contextTemplateBuilder.withName("Context Template 2")

            // add the scheduler jobs that will be orchestrated
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job4")
                .withAgentName("AgentName")
                .withDescription("Job4 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job5")
                .withAgentName("AgentName")
                .withDescription("Job5 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job6")
                .withAgentName("AgentName")
                .withDescription("Job6 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job7")
                .withAgentName("AgentName")
                .withDescription("Job7 Description")
                .build())
            .addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName("Job8")
                .withAgentName("AgentName")
                .withDescription("Job8 Description")
                .build())

            // Add the job dependencies
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job4")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job5")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job6")
                .withAgentName("AgentName")
                .build())
            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job7")
                .withAgentName("AgentName")
                .build())

            .addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                .withJobName("Job8")
                .withAgentName("AgentName")
                .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                    .addOr(contextTemplateBuilder.getJobOrBuilder()
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job4")
                                .build())
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job5")
                                .build()).build()).build())
                    .addOr(contextTemplateBuilder.getJobOrBuilder()
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job6")
                                .build())
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName("AgentName")
                                .withJobName("Job7")
                                .build()).build()).build()).build())
                .build())
            .build();

        contextTemplateBuilder = new ContextTemplateBuilder();

        ContextTemplate parentContext = contextTemplateBuilder.withName("Parent Context")
            .withDescription("Context Template Description")
            .withTimeWindowStartCronExpression("* * 6 ? * * *")
            .withTimeWindowEndCronExpression("* * 15 ? * * *")

            // add some context parameters
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param1").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param2").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param3").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param4").withType("java.lang.String").build())
            .addContextParameter(contextTemplateBuilder.getContextParameterBuilder().withName("param5").withType("java.lang.String").build())

            .addContext(contextTemplate1)
            .addContext(contextTemplate2)

            .addContextDependency(contextTemplateBuilder.getContextDependencyBuilder()
                .withContextDependencyName("parent context dependency")
                .withContextIdentifier(contextTemplate1.getName())
                .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                    .addAnd(contextTemplateBuilder.getContextAndBuilder()
                        .withIdentifier(contextTemplate2.getName())
                        .build())
                    .build())
                .build())
            .build();

        ContextService contextService = new ContextService();

        JSONAssert.assertEquals(super.loadDataFile("/data/context-builder-nested-context-result.json"),
            contextService.getContextString(parentContext), false);
    }
}
