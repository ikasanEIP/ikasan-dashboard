package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.junit.Assert;
import org.junit.Test;

public class JobDependencyBuilderTest {

    @Test
    public void test_build_with_agent_and_job_name() {
        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .build();

        Assert.assertNotNull(jobDependency);
        Assert.assertEquals("agent1-job1", jobDependency.getJobIdentifier());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(jobDependency);
        Assert.assertSame(grouping, jobDependency.getLogicalGrouping());
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_null_agent_name_throws_exception() {
        new JobDependencyBuilder()
            .withAgentName(null)
            .withJobName("job1")
            .build();
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_null_job_name_throws_exception() {
        new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName(null)
            .build();
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_both_names_null_throws_exception() {
        new JobDependencyBuilder()
            .withAgentName(null)
            .withJobName(null)
            .build();
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(jobDependency);
        Assert.assertNull(jobDependency.getLogicalGrouping());
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_minimal_throws_exception() {
        new JobDependencyBuilder().build();
    }

    @Test
    public void test_exception_message_for_null_names() {
        try {
            new JobDependencyBuilder()
                .withAgentName(null)
                .withJobName(null)
                .build();
            Assert.fail("Expected ContextBuilderException");
        } catch (ContextBuilderException e) {
            Assert.assertEquals("Both agent name and job name must not be null!", e.getMessage());
        }
    }

    @Test
    public void test_method_chaining() {
        JobDependencyBuilder builder = new JobDependencyBuilder();

        JobDependencyBuilder result1 = builder.withAgentName("agent1");
        Assert.assertSame(builder, result1);

        JobDependencyBuilder result2 = builder.withJobName("job1");
        Assert.assertSame(builder, result2);

        JobDependencyBuilder result3 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result3);
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(jobDependency);
        Assert.assertEquals("agent1-job1", jobDependency.getJobIdentifier());
        Assert.assertSame(grouping, jobDependency.getLogicalGrouping());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        JobDependencyBuilder builder = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("job1");

        JobDependency dependency1 = builder.build();
        JobDependency dependency2 = builder.build();

        Assert.assertEquals(dependency1.getJobIdentifier(), dependency2.getJobIdentifier());
    }

    @Test
    public void test_overwrite_agent_name() {
        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withAgentName("agent2")
            .withJobName("job1")
            .build();

        Assert.assertEquals("agent2-job1", jobDependency.getJobIdentifier());
    }

    @Test
    public void test_overwrite_job_name() {
        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withJobName("job2")
            .build();

        Assert.assertEquals("agent1-job2", jobDependency.getJobIdentifier());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, jobDependency.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_agent_name() {
        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("")
            .withJobName("job1")
            .build();

        Assert.assertEquals("-job1", jobDependency.getJobIdentifier());
    }

    @Test
    public void test_with_empty_job_name() {
        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent1")
            .withJobName("")
            .build();

        Assert.assertEquals("agent1-", jobDependency.getJobIdentifier());
    }

    @Test
    public void test_with_special_characters_in_names() {
        JobDependency jobDependency = new JobDependencyBuilder()
            .withAgentName("agent@#$")
            .withJobName("job!%^")
            .build();

        Assert.assertEquals("agent@#$-job!%^", jobDependency.getJobIdentifier());
    }
}
