package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Assert;
import org.junit.Test;

public class SchedulerJobBuilderTest {

    @Test
    public void test_build_with_agent_and_job_name() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .build();

        Assert.assertNotNull(job);
        Assert.assertEquals("agent1-job1", job.getIdentifier());
        Assert.assertEquals("agent1", job.getAgentName());
        Assert.assertEquals("job1", job.getJobName());
    }

    @Test
    public void test_build_with_all_properties() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withDisplayName("Display Name")
            .withContextName("context1")
            .withDescription("Test Description")
            .withStartupControlType("MANUAL")
            .withOrdinal(5)
            .build();

        Assert.assertNotNull(job);
        Assert.assertEquals("agent1-job1", job.getIdentifier());
        Assert.assertEquals("agent1", job.getAgentName());
        Assert.assertEquals("job1", job.getJobName());
        Assert.assertEquals("Display Name", job.getDisplayName());
        Assert.assertEquals("Test Description", job.getJobDescription());
        Assert.assertEquals("MANUAL", job.getStartupControlType());
        Assert.assertEquals(5, job.getOrdinal());
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_null_agent_name_throws_exception() {
        new SchedulerJobBuilder()
            .withJobName("job1")
            .build();
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_null_job_name_throws_exception() {
        new SchedulerJobBuilder()
            .withAgentName("agent1")
            .build();
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_both_names_null_throws_exception() {
        new SchedulerJobBuilder().build();
    }

    @Test
    public void test_exception_message() {
        try {
            new SchedulerJobBuilder().build();
            Assert.fail("Expected ContextBuilderException");
        } catch (ContextBuilderException e) {
            Assert.assertEquals("Both agent name and job name must no be null!", e.getMessage());
        }
    }

    @Test
    public void test_default_startup_control_type() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .build();

        Assert.assertEquals("AUTOMATIC", job.getStartupControlType());
    }

    @Test
    public void test_default_ordinal() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .build();

        Assert.assertEquals(-1, job.getOrdinal());
    }

    @Test
    public void test_add_single_child_context_id() {
        SchedulerJobBuilder builder = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .addChildContextId("child1");

        // Child context IDs are stored but not directly accessible on SchedulerJob
        // This just verifies the builder doesn't throw
        SchedulerJob job = builder.build();
        Assert.assertNotNull(job);
    }

    @Test
    public void test_add_multiple_child_context_ids() {
        SchedulerJobBuilder builder = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .addChildContextId("child1")
            .addChildContextId("child2")
            .addChildContextId("child3");

        SchedulerJob job = builder.build();
        Assert.assertNotNull(job);
    }

    @Test
    public void test_method_chaining() {
        SchedulerJobBuilder builder = new SchedulerJobBuilder();

        SchedulerJobBuilder result1 = builder.withAgentName("agent1");
        Assert.assertSame(builder, result1);

        SchedulerJobBuilder result2 = builder.withJobName("job1");
        Assert.assertSame(builder, result2);

        SchedulerJobBuilder result3 = builder.withContextName("context1");
        Assert.assertSame(builder, result3);

        SchedulerJobBuilder result4 = builder.withDescription("desc");
        Assert.assertSame(builder, result4);

        SchedulerJobBuilder result5 = builder.withStartupControlType("MANUAL");
        Assert.assertSame(builder, result5);

        SchedulerJobBuilder result6 = builder.withOrdinal(1);
        Assert.assertSame(builder, result6);

        SchedulerJobBuilder result7 = builder.withDisplayName("display");
        Assert.assertSame(builder, result7);

        SchedulerJobBuilder result8 = builder.addChildContextId("child");
        Assert.assertSame(builder, result8);
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        SchedulerJobBuilder builder = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1");

        SchedulerJob job1 = builder.build();
        SchedulerJob job2 = builder.build();

        Assert.assertEquals(job1.getIdentifier(), job2.getIdentifier());
    }

    @Test
    public void test_overwrite_agent_name() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withAgentName("agent2")
            .withJobName("job1")
            .build();

        Assert.assertEquals("agent2-job1", job.getIdentifier());
        Assert.assertEquals("agent2", job.getAgentName());
    }

    @Test
    public void test_overwrite_job_name() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withJobName("job2")
            .build();

        Assert.assertEquals("agent1-job2", job.getIdentifier());
        Assert.assertEquals("job2", job.getJobName());
    }

    @Test
    public void test_overwrite_startup_control_type() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withStartupControlType("MANUAL")
            .withStartupControlType("AUTOMATIC")
            .build();

        Assert.assertEquals("AUTOMATIC", job.getStartupControlType());
    }

    @Test
    public void test_with_empty_strings() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("")
            .withJobName("")
            .build();

        Assert.assertEquals("-", job.getIdentifier());
    }

    @Test
    public void test_with_null_optional_fields() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withDisplayName(null)
            .withDescription(null)
            .withContextName(null)
            .withStartupControlType(null)
            .build();

        Assert.assertNotNull(job);
        Assert.assertNull(job.getDisplayName());
        Assert.assertNull(job.getJobDescription());
        Assert.assertNull(job.getStartupControlType());
    }

    @Test
    public void test_with_special_characters() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent@#$")
            .withJobName("job!%^")
            .build();

        Assert.assertEquals("agent@#$-job!%^", job.getIdentifier());
    }

    @Test
    public void test_with_negative_ordinal() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withOrdinal(-5)
            .build();

        Assert.assertEquals(-5, job.getOrdinal());
    }

    @Test
    public void test_with_zero_ordinal() {
        SchedulerJob job = new SchedulerJobBuilder()
            .withAgentName("agent1")
            .withJobName("job1")
            .withOrdinal(0)
            .build();

        Assert.assertEquals(0, job.getOrdinal());
    }
}
