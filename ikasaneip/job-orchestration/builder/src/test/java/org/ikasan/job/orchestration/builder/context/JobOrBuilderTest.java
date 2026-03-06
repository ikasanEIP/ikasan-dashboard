package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;
import org.junit.Assert;
import org.junit.Test;

public class JobOrBuilderTest {

    @Test
    public void test_build_with_agent_and_job_name() {
        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .build();

        Assert.assertNotNull(or);
        Assert.assertEquals("agent1-job1", or.getIdentifier());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(or);
        Assert.assertSame(grouping, or.getLogicalGrouping());
    }

    @Test
    public void test_build_with_null_agent_name() {
        Or or = new JobOrBuilder()
            .withAgentName(null)
            .withJobName("job1")
            .build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getIdentifier());
    }

    @Test
    public void test_build_with_null_job_name() {
        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName(null)
            .build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getIdentifier());
    }

    @Test
    public void test_build_with_both_names_null() {
        Or or = new JobOrBuilder()
            .withAgentName(null)
            .withJobName(null)
            .build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getIdentifier());
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getLogicalGrouping());
    }

    @Test
    public void test_build_minimal() {
        Or or = new JobOrBuilder().build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getIdentifier());
        Assert.assertNull(or.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        JobOrBuilder builder = new JobOrBuilder();

        JobOrBuilder result1 = builder.withAgentName("agent1");
        Assert.assertSame(builder, result1);

        JobOrBuilder result2 = builder.withJobName("job1");
        Assert.assertSame(builder, result2);

        JobOrBuilder result3 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result3);
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(or);
        Assert.assertEquals("agent1-job1", or.getIdentifier());
        Assert.assertSame(grouping, or.getLogicalGrouping());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        JobOrBuilder builder = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("job1");

        Or or1 = builder.build();
        Or or2 = builder.build();

        Assert.assertEquals(or1.getIdentifier(), or2.getIdentifier());
    }

    @Test
    public void test_overwrite_agent_name() {
        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withAgentName("agent2")
            .withJobName("job1")
            .build();

        Assert.assertEquals("agent2-job1", or.getIdentifier());
    }

    @Test
    public void test_overwrite_job_name() {
        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withJobName("job2")
            .build();

        Assert.assertEquals("agent1-job2", or.getIdentifier());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, or.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_agent_name() {
        Or or = new JobOrBuilder()
            .withAgentName("")
            .withJobName("job1")
            .build();

        Assert.assertEquals("-job1", or.getIdentifier());
    }

    @Test
    public void test_with_empty_job_name() {
        Or or = new JobOrBuilder()
            .withAgentName("agent1")
            .withJobName("")
            .build();

        Assert.assertEquals("agent1-", or.getIdentifier());
    }

    @Test
    public void test_with_special_characters_in_names() {
        Or or = new JobOrBuilder()
            .withAgentName("agent@#$")
            .withJobName("job!%^")
            .build();

        Assert.assertEquals("agent@#$-job!%^", or.getIdentifier());
    }
}
