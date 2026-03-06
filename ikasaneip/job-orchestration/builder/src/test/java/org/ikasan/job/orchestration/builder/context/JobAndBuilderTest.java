package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.junit.Assert;
import org.junit.Test;

public class JobAndBuilderTest {

    @Test
    public void test_build_with_agent_and_job_name() {
        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .build();

        Assert.assertNotNull(and);
        Assert.assertEquals("agent1-job1", and.getIdentifier());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(and);
        Assert.assertSame(grouping, and.getLogicalGrouping());
    }

    @Test
    public void test_build_with_null_agent_name() {
        And and = new JobAndBuilder()
            .withAgentName(null)
            .withJobName("job1")
            .build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getIdentifier());
    }

    @Test
    public void test_build_with_null_job_name() {
        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName(null)
            .build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getIdentifier());
    }

    @Test
    public void test_build_with_both_names_null() {
        And and = new JobAndBuilder()
            .withAgentName(null)
            .withJobName(null)
            .build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getIdentifier());
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getLogicalGrouping());
    }

    @Test
    public void test_build_minimal() {
        And and = new JobAndBuilder().build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getIdentifier());
        Assert.assertNull(and.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        JobAndBuilder builder = new JobAndBuilder();

        JobAndBuilder result1 = builder.withAgentName("agent1");
        Assert.assertSame(builder, result1);

        JobAndBuilder result2 = builder.withJobName("job1");
        Assert.assertSame(builder, result2);

        JobAndBuilder result3 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result3);
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(and);
        Assert.assertEquals("agent1-job1", and.getIdentifier());
        Assert.assertSame(grouping, and.getLogicalGrouping());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        JobAndBuilder builder = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("job1");

        And and1 = builder.build();
        And and2 = builder.build();

        Assert.assertEquals(and1.getIdentifier(), and2.getIdentifier());
    }

    @Test
    public void test_overwrite_agent_name() {
        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withAgentName("agent2")
            .withJobName("job1")
            .build();

        Assert.assertEquals("agent2-job1", and.getIdentifier());
    }

    @Test
    public void test_overwrite_job_name() {
        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withJobName("job2")
            .build();

        Assert.assertEquals("agent1-job2", and.getIdentifier());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, and.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_agent_name() {
        And and = new JobAndBuilder()
            .withAgentName("")
            .withJobName("job1")
            .build();

        Assert.assertEquals("-job1", and.getIdentifier());
    }

    @Test
    public void test_with_empty_job_name() {
        And and = new JobAndBuilder()
            .withAgentName("agent1")
            .withJobName("")
            .build();

        Assert.assertEquals("agent1-", and.getIdentifier());
    }

    @Test
    public void test_with_special_characters_in_names() {
        And and = new JobAndBuilder()
            .withAgentName("agent@#$")
            .withJobName("job!%^")
            .build();

        Assert.assertEquals("agent@#$-job!%^", and.getIdentifier());
    }
}
