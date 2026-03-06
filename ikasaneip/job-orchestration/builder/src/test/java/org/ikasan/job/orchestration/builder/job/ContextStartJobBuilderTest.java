package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.junit.Assert;
import org.junit.Test;

public class ContextStartJobBuilderTest {

    @Test
    public void test_build_with_job_name() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job1");

        ContextStartJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
    }

    @Test
    public void test_build_with_all_properties() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job1");
        builder.withDescription("Test Description");
        builder.withContextName("context1");
        builder.withDisplayName("Display Name");

        ContextStartJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
        Assert.assertEquals("Test Description", job.getJobDescription());
        Assert.assertEquals("context1", job.getContextName());
        Assert.assertEquals("Display Name", job.getDisplayName());
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_null_job_name_throws_exception() {
        new ContextStartJobBuilder().build();
    }

    @Test
    public void test_exception_message() {
        try {
            new ContextStartJobBuilder().build();
            Assert.fail("Expected ContextBuilderException");
        } catch (ContextBuilderException e) {
            Assert.assertEquals("Job name must no be null!", e.getMessage());
        }
    }

    @Test
    public void test_startup_control_type_is_null() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job1");

        ContextStartJob job = builder.build();
        Assert.assertNull(job.getStartupControlType());
    }

    @Test
    public void test_method_chaining() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job1");
        builder.withDescription("desc");
        builder.withContextName("context1");
        builder.withDisplayName("display");

        ContextStartJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
        Assert.assertEquals("desc", job.getJobDescription());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job1");

        ContextStartJob job1 = builder.build();
        ContextStartJob job2 = builder.build();

        Assert.assertEquals(job1.getJobName(), job2.getJobName());
    }

    @Test
    public void test_overwrite_job_name() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job1");
        builder.withJobName("job2");

        ContextStartJob job = builder.build();
        Assert.assertEquals("job2", job.getJobName());
    }

    @Test
    public void test_with_null_optional_fields() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job1");
        builder.withDescription(null);
        builder.withContextName(null);
        builder.withDisplayName(null);

        ContextStartJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertNull(job.getJobDescription());
        Assert.assertNull(job.getContextName());
        Assert.assertNull(job.getDisplayName());
    }

    @Test
    public void test_with_empty_job_name() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("");

        ContextStartJob job = builder.build();
        Assert.assertEquals("", job.getJobName());
    }

    @Test
    public void test_with_special_characters() {
        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName("job@#$%^");
        builder.withDescription("desc!@#");

        ContextStartJob job = builder.build();
        Assert.assertEquals("job@#$%^", job.getJobName());
        Assert.assertEquals("desc!@#", job.getJobDescription());
    }

    @Test
    public void test_with_long_values() {
        String longName = "a".repeat(1000);
        String longDesc = "b".repeat(1000);

        ContextStartJobBuilder builder = new ContextStartJobBuilder();
        builder.withJobName(longName);
        builder.withDescription(longDesc);

        ContextStartJob job = builder.build();
        Assert.assertEquals(longName, job.getJobName());
        Assert.assertEquals(longDesc, job.getJobDescription());
    }
}
