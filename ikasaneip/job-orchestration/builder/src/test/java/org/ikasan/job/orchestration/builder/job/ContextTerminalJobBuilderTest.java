package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.junit.Assert;
import org.junit.Test;

public class ContextTerminalJobBuilderTest {

    @Test
    public void test_build_with_job_name() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job1");

        ContextTerminalJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
    }

    @Test
    public void test_build_with_all_properties() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job1");
        builder.withDescription("Test Description");
        builder.withContextName("context1");
        builder.withDisplayName("Display Name");

        ContextTerminalJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
        Assert.assertEquals("Test Description", job.getJobDescription());
        Assert.assertEquals("context1", job.getContextName());
        Assert.assertEquals("Display Name", job.getDisplayName());
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_null_job_name_throws_exception() {
        new ContextTerminalJobBuilder().build();
    }

    @Test
    public void test_exception_message() {
        try {
            new ContextTerminalJobBuilder().build();
            Assert.fail("Expected ContextBuilderException");
        } catch (ContextBuilderException e) {
            Assert.assertEquals("Job name must no be null!", e.getMessage());
        }
    }

    @Test
    public void test_startup_control_type_is_null() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job1");

        ContextTerminalJob job = builder.build();
        Assert.assertNull(job.getStartupControlType());
    }

    @Test
    public void test_method_chaining() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job1");
        builder.withDescription("desc");
        builder.withContextName("context1");
        builder.withDisplayName("display");

        ContextTerminalJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
        Assert.assertEquals("desc", job.getJobDescription());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job1");

        ContextTerminalJob job1 = builder.build();
        ContextTerminalJob job2 = builder.build();

        Assert.assertEquals(job1.getJobName(), job2.getJobName());
    }

    @Test
    public void test_overwrite_job_name() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job1");
        builder.withJobName("job2");

        ContextTerminalJob job = builder.build();
        Assert.assertEquals("job2", job.getJobName());
    }

    @Test
    public void test_with_null_optional_fields() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job1");
        builder.withDescription(null);
        builder.withContextName(null);
        builder.withDisplayName(null);

        ContextTerminalJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertNull(job.getJobDescription());
        Assert.assertNull(job.getContextName());
        Assert.assertNull(job.getDisplayName());
    }

    @Test
    public void test_with_empty_job_name() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("");

        ContextTerminalJob job = builder.build();
        Assert.assertEquals("", job.getJobName());
    }

    @Test
    public void test_with_special_characters() {
        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName("job@#$%^");
        builder.withDescription("desc!@#");

        ContextTerminalJob job = builder.build();
        Assert.assertEquals("job@#$%^", job.getJobName());
        Assert.assertEquals("desc!@#", job.getJobDescription());
    }

    @Test
    public void test_with_long_values() {
        String longName = "a".repeat(1000);
        String longDesc = "b".repeat(1000);

        ContextTerminalJobBuilder builder = new ContextTerminalJobBuilder();
        builder.withJobName(longName);
        builder.withDescription(longDesc);

        ContextTerminalJob job = builder.build();
        Assert.assertEquals(longName, job.getJobName());
        Assert.assertEquals(longDesc, job.getJobDescription());
    }
}
