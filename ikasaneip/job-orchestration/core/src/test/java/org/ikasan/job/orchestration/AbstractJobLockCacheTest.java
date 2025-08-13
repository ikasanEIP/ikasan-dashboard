package org.ikasan.job.orchestration;

import org.ikasan.job.orchestration.builder.context.JobLockBuilder;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.junit.After;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public abstract class AbstractJobLockCacheTest {

    @Mock
    protected JobLockCacheService jobLockCacheService;

    @After
    public void tearDown() {
        JobLockCacheImpl.instance().reset();
    }

    /**
     * Validates the list of SchedulerJob objects against a job lock name.
     *
     * @param schedulerJobs The list of SchedulerJob objects to validate
     * @param jobLockName The name of the job lock to validate against
     */
    protected void validate(List<SchedulerJob> schedulerJobs, String jobLockName) {
        Map<String, SchedulerJob> jobMap = this.listToMap(schedulerJobs);
        for (int i = 0; i < schedulerJobs.size(); i++) {
            SchedulerJob schedulerJob = jobMap.get(jobLockName + "-" + "JobName" + i);
            assertEquals(jobLockName + "-" + "JobName" + i, schedulerJob.getJobName());
            assertEquals("AgentName" + i, schedulerJob.getAgentName());
            assertEquals("Job" + i + " Description", schedulerJob.getJobDescription());
            assertEquals("AgentName" + i + "-" + jobLockName + "-" + "JobName" + i, schedulerJob.getIdentifier());
            assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());
        }
    }

    /**
     * Converts a list of SchedulerJob objects into a Map, where the key is the job name of each SchedulerJob.
     *
     * @param schedulerJobs The list of SchedulerJob objects to convert to a Map
     * @return A Map where the key is the job name of each SchedulerJob and the value is the SchedulerJob object itself
     */
    protected Map<String, SchedulerJob> listToMap(List<SchedulerJob> schedulerJobs) {
        return schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity()));
    }

    /**
     * Creates an exclusive JobLock with the specified parameters.
     *
     * @param jobLockName The name of the JobLock to create
     * @param count The number of jobs to add to the JobLock
     * @param jobLockCount The lock count for the JobLock
     * @return The exclusive JobLock instance created
     */
    protected JobLock makeExclusiveJobLock(String jobLockName, int count, int jobLockCount) {
        JobLock jobLock =  makeJobLock(jobLockName, count, jobLockCount, null);
        jobLock.setExclusiveJobLock(true);

        return jobLock;
    }

    /**
     * Creates a JobLock with the specified parameters.
     *
     * @param jobLockName The name of the JobLock to create
     * @param count The number of jobs to add to the JobLock
     * @param jobLockCount The lock count for the JobLock
     * @return The JobLock instance created
     */
    protected JobLock makeJobLock(String jobLockName, int count, int jobLockCount) {
        return makeJobLock(jobLockName, count, jobLockCount, null);
    }

    /**
     * Constructs a JobLock with the given parameters.
     *
     * @param jobLockName The name of the JobLock
     * @param count The number of jobs to add to the JobLock
     * @param jobLockCount The lock count for the JobLock
     * @param newOrNot Extra identifier for creating SchedulerJobLockParticipant
     * @return The constructed JobLock instance
     */
    protected JobLock makeJobLock(String jobLockName, int count, int jobLockCount, String newOrNot) {
        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName(jobLockName);
        jobLockBuilder.withLockCount(jobLockCount);
        for (int i = 0; i < count; i++) {
            SchedulerJobLockParticipant job = makeSchedulerJobLockParticipant(i, newOrNot, jobLockName, 1);
            job.setContextName(UUID.randomUUID().toString());
            jobLockBuilder.withJob("contextName"+i, job);
        }
        return jobLockBuilder.build().get(0);
    }

    /**
     * Creates a SchedulerJobLockParticipant with the given parameters.
     *
     * @param count The count used for creating the participant
     * @param newOrNot Extra identifier for creating the participant
     * @param jobLockName The name of the job lock associated with the participant
     * @param lockCount The lock count for the participant
     * @return A SchedulerJobLockParticipant instance with the specified parameters
     */
    protected SchedulerJobLockParticipant makeSchedulerJobLockParticipant(int count, String newOrNot, String jobLockName, int lockCount) {
        String extra = newOrNot == null ? "" : newOrNot;
        SchedulerJobLockParticipant job = new SchedulerJobLockParticipantImpl();
        job.setAgentName("AgentName" + count + extra);
        job.setJobName(jobLockName + "-" + "JobName" + count + extra);
        job.setIdentifier(job.getAgentName() + "-" + job.getJobName());
        job.setJobDescription("Job" + count + extra + " Description");
        job.setLockCount(lockCount);
        return job;
    }
}
