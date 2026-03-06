package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.spec.scheduled.job.model.LocalEventJob;
import org.junit.Assert;
import org.junit.Test;

public class LocalEventJobBuilderTest {

    @Test
    public void test_build_with_job_name() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("job1");

        LocalEventJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
    }

    @Test
    public void test_build_with_all_properties() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("job1");
        builder.withDescription("Test Description");
        builder.withContextName("context1");
        builder.withDisplayName("Display Name");

        LocalEventJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
        Assert.assertEquals("Test Description", job.getJobDescription());
        Assert.assertEquals("context1", job.getContextName());
        Assert.assertEquals("Display Name", job.getDisplayName());
    }

    @Test(expected = ContextBuilderException.class)
    public void test_build_with_null_job_name_throws_exception() {
        new LocalEventJobBuilder().build();
    }

    @Test
    public void test_exception_message() {
        try {
            new LocalEventJobBuilder().build();
            Assert.fail("Expected ContextBuilderException");
        } catch (ContextBuilderException e) {
            Assert.assertEquals("Job name must no be null!", e.getMessage());
        }
    }

    @Test
    public void test_method_chaining() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("job1");
        builder.withDescription("desc");
        builder.withContextName("context1");
        builder.withDisplayName("display");

        LocalEventJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertEquals("job1", job.getJobName());
        Assert.assertEquals("desc", job.getJobDescription());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("job1");

        LocalEventJob job1 = builder.build();
        LocalEventJob job2 = builder.build();

        Assert.assertEquals(job1.getJobName(), job2.getJobName());
    }

    @Test
    public void test_overwrite_job_name() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("job1");
        builder.withJobName("job2");

        LocalEventJob job = builder.build();
        Assert.assertEquals("job2", job.getJobName());
    }

    @Test
    public void test_with_null_optional_fields() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("job1");
        builder.withDescription(null);
        builder.withContextName(null);
        builder.withDisplayName(null);

        LocalEventJob job = builder.build();
        Assert.assertNotNull(job);
        Assert.assertNull(job.getJobDescription());
        Assert.assertNull(job.getContextName());
        Assert.assertNull(job.getDisplayName());
    }

    @Test
    public void test_with_empty_job_name() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("");

        LocalEventJob job = builder.build();
        Assert.assertEquals("", job.getJobName());
    }

    @Test
    public void test_with_special_characters() {
        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName("job@#$%^");
        builder.withDescription("desc!@#");

        LocalEventJob job = builder.build();
        Assert.assertEquals("job@#$%^", job.getJobName());
        Assert.assertEquals("desc!@#", job.getJobDescription());
    }

    @Test
    public void test_with_long_values() {
        String longName = "a".repeat(1000);
        String longDesc = "b".repeat(1000);

        LocalEventJobBuilder builder = new LocalEventJobBuilder();
        builder.withJobName(longName);
        builder.withDescription(longDesc);

        LocalEventJob job = builder.build();
        Assert.assertEquals(longName, job.getJobName());
        Assert.assertEquals(longDesc, job.getJobDescription());
    }
}
