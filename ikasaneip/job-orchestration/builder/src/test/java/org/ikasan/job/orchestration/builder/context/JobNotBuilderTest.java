package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;
import org.junit.Assert;
import org.junit.Test;

public class JobNotBuilderTest {

    @Test
    public void test_build_with_agent_and_job_name() {
        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .build();

        Assert.assertNotNull(not);
        Assert.assertEquals("agent1-job1", not.getIdentifier());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(not);
        Assert.assertSame(grouping, not.getLogicalGrouping());
    }

    @Test
    public void test_build_with_null_agent_name() {
        Not not = new JobNotBuilder()
            .withAgentName(null)
            .withJobName("job1")
            .build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getIdentifier());
    }

    @Test
    public void test_build_with_null_job_name() {
        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName(null)
            .build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getIdentifier());
    }

    @Test
    public void test_build_with_both_names_null() {
        Not not = new JobNotBuilder()
            .withAgentName(null)
            .withJobName(null)
            .build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getIdentifier());
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getLogicalGrouping());
    }

    @Test
    public void test_build_minimal() {
        Not not = new JobNotBuilder().build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getIdentifier());
        Assert.assertNull(not.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        JobNotBuilder builder = new JobNotBuilder();

        JobNotBuilder result1 = builder.withAgentName("agent1");
        Assert.assertSame(builder, result1);

        JobNotBuilder result2 = builder.withJobName("job1");
        Assert.assertSame(builder, result2);

        JobNotBuilder result3 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result3);
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(not);
        Assert.assertEquals("agent1-job1", not.getIdentifier());
        Assert.assertSame(grouping, not.getLogicalGrouping());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        JobNotBuilder builder = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("job1");

        Not not1 = builder.build();
        Not not2 = builder.build();

        Assert.assertEquals(not1.getIdentifier(), not2.getIdentifier());
    }

    @Test
    public void test_overwrite_agent_name() {
        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withAgentName("agent2")
            .withJobName("job1")
            .build();

        Assert.assertEquals("agent2-job1", not.getIdentifier());
    }

    @Test
    public void test_overwrite_job_name() {
        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withJobName("job2")
            .build();

        Assert.assertEquals("agent1-job2", not.getIdentifier());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, not.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_agent_name() {
        Not not = new JobNotBuilder()
            .withAgentName("")
            .withJobName("job1")
            .build();

        Assert.assertEquals("-job1", not.getIdentifier());
    }

    @Test
    public void test_with_empty_job_name() {
        Not not = new JobNotBuilder()
            .withAgentName("agent1")
            .withJobName("")
            .build();

        Assert.assertEquals("agent1-", not.getIdentifier());
    }

    @Test
    public void test_with_special_characters_in_names() {
        Not not = new JobNotBuilder()
            .withAgentName("agent@#$")
            .withJobName("job!%^")
            .build();

        Assert.assertEquals("agent@#$-job!%^", not.getIdentifier());
    }
}
