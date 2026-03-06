package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JobLockBuilderTest {

    @Test
    public void test_build_with_lock_name_only() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());
        Assert.assertEquals("testLock", jobLocks.get(0).getName());
        Assert.assertEquals(1, jobLocks.get(0).getLockCount());
        Assert.assertFalse(jobLocks.get(0).isExclusiveJobLock());
    }

    @Test
    public void test_build_with_lock_count() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withLockCount(5)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());
        Assert.assertEquals(5, jobLocks.get(0).getLockCount());
    }

    @Test
    public void test_build_with_exclusive_job_lock() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withExclusiveJobLock(true)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());
        Assert.assertTrue(jobLocks.get(0).isExclusiveJobLock());
    }

    @Test
    public void test_build_with_single_job() {
        SchedulerJobLockParticipant job = new SchedulerJobLockParticipantImpl();
        job.setJobName("job1");

        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withJob("context1", job)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());
        Assert.assertNotNull(jobLocks.get(0).getJobs());
        Assert.assertTrue(jobLocks.get(0).getJobs().containsKey("context1"));
        Assert.assertEquals(1, jobLocks.get(0).getJobs().get("context1").size());
    }

    @Test
    public void test_build_with_multiple_jobs_same_context() {
        SchedulerJobLockParticipant job1 = new SchedulerJobLockParticipantImpl();
        job1.setJobName("job1");

        SchedulerJobLockParticipant job2 = new SchedulerJobLockParticipantImpl();
        job2.setJobName("job2");

        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withJob("context1", job1)
            .withJob("context1", job2)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());
        Assert.assertTrue(jobLocks.get(0).getJobs().containsKey("context1"));
        Assert.assertEquals(2, jobLocks.get(0).getJobs().get("context1").size());
    }

    @Test
    public void test_build_with_jobs_in_different_contexts() {
        SchedulerJobLockParticipant job1 = new SchedulerJobLockParticipantImpl();
        job1.setJobName("job1");

        SchedulerJobLockParticipant job2 = new SchedulerJobLockParticipantImpl();
        job2.setJobName("job2");

        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withJob("context1", job1)
            .withJob("context2", job2)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());
        Assert.assertEquals(2, jobLocks.get(0).getJobs().size());
        Assert.assertTrue(jobLocks.get(0).getJobs().containsKey("context1"));
        Assert.assertTrue(jobLocks.get(0).getJobs().containsKey("context2"));
    }

    @Test
    public void test_build_complete_configuration() {
        SchedulerJobLockParticipant job1 = new SchedulerJobLockParticipantImpl();
        job1.setJobName("job1");

        SchedulerJobLockParticipant job2 = new SchedulerJobLockParticipantImpl();
        job2.setJobName("job2");

        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withLockCount(3)
            .withExclusiveJobLock(true)
            .withJob("context1", job1)
            .withJob("context2", job2)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());

        JobLock jobLock = jobLocks.get(0);
        Assert.assertEquals("testLock", jobLock.getName());
        Assert.assertEquals(3, jobLock.getLockCount());
        Assert.assertTrue(jobLock.isExclusiveJobLock());
        Assert.assertEquals(2, jobLock.getJobs().size());
    }

    @Test
    public void test_builder_method_chaining() {
        JobLockBuilder builder = new JobLockBuilder();

        JobLockBuilder result1 = builder.withLockName("testLock");
        Assert.assertSame(builder, result1);

        JobLockBuilder result2 = builder.withLockCount(5);
        Assert.assertSame(builder, result2);

        JobLockBuilder result3 = builder.withExclusiveJobLock(true);
        Assert.assertSame(builder, result3);
    }

    @Test
    public void test_build_with_null_lock_name() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName(null)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(1, jobLocks.size());
        Assert.assertNull(jobLocks.get(0).getName());
    }

    @Test
    public void test_build_with_zero_lock_count() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withLockCount(0)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(0, jobLocks.get(0).getLockCount());
    }

    @Test
    public void test_build_with_negative_lock_count() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withLockCount(-1)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertEquals(-1, jobLocks.get(0).getLockCount());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        SchedulerJobLockParticipant job = new SchedulerJobLockParticipantImpl();
        job.setJobName("job1");

        JobLockBuilder builder = new JobLockBuilder()
            .withLockName("testLock")
            .withJob("context1", job);

        List<JobLock> locks1 = builder.build();
        List<JobLock> locks2 = builder.build();

        // Both should have the same configuration
        Assert.assertEquals(locks1.get(0).getName(), locks2.get(0).getName());
        Assert.assertEquals(locks1.get(0).getLockCount(), locks2.get(0).getLockCount());
    }

    @Test
    public void test_build_with_null_context_name() {
        SchedulerJobLockParticipant job = new SchedulerJobLockParticipantImpl();
        job.setJobName("job1");

        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("testLock")
            .withJob(null, job)
            .build();

        Assert.assertNotNull(jobLocks);
        Assert.assertTrue(jobLocks.get(0).getJobs().containsKey(null));
    }

    @Test
    public void test_overwrite_lock_name() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockName("firstLock")
            .withLockName("secondLock")
            .build();

        Assert.assertEquals("secondLock", jobLocks.get(0).getName());
    }

    @Test
    public void test_overwrite_lock_count() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withLockCount(5)
            .withLockCount(10)
            .build();

        Assert.assertEquals(10, jobLocks.get(0).getLockCount());
    }

    @Test
    public void test_overwrite_exclusive_lock() {
        List<JobLock> jobLocks = new JobLockBuilder()
            .withExclusiveJobLock(true)
            .withExclusiveJobLock(false)
            .build();

        Assert.assertFalse(jobLocks.get(0).isExclusiveJobLock());
    }
}
