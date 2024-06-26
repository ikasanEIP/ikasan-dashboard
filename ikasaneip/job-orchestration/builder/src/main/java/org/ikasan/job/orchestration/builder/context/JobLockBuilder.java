package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobLockBuilder {

    private String lockName;
    private Map<String, List<SchedulerJobLockParticipant>> schedulerJobs;
    private int lockCount = 1;

    private boolean exclusiveJobLock;

    /**
     * A builder class for creating JobLock objects.
     */
    public JobLockBuilder() {
    }

    /**
     * Sets the lock name for the job lock builder.
     * @param lockName the lock name to be set
     * @return the JobLockBuilder instance
     */
    public JobLockBuilder withLockName(String lockName) {
        this.lockName = lockName;
        return this;
    }

    /**
     * Adds a job to the builder with the given context name.
     * If the schedulerJobs map is null, it will be initialized.
     * If the context name is not present in the map, a new entry will be created.
     * The job will then be added to the list of jobs for the context.
     *
     * @param contextName The name of the context for the job
     * @param job The job to add
     * @return The JobLockBuilder instance with the added job
     */
    public JobLockBuilder withJob(String contextName, SchedulerJobLockParticipant job) {
        if(this.schedulerJobs == null) {
            schedulerJobs = new HashMap<>();
        }

        if(!schedulerJobs.containsKey(contextName)) {
            schedulerJobs.put(contextName, new ArrayList<>());
        }

        schedulerJobs.get(contextName).add(job);
        return this;
    }

    /**
     * Sets the lock count for the JobLockBuilder.
     *
     * @param lockCount the lock count to be set
     * @return the JobLockBuilder object for method chaining
     */
    public JobLockBuilder withLockCount(Integer lockCount) {
        this.lockCount = lockCount;
        return this;
    }

    /**
     * Sets whether the job lock should be exclusive or not.
     *
     * @param exclusiveJobLock true to make the job lock exclusive, false otherwise
     * @return the JobLockBuilder object
     */
    public JobLockBuilder withExclusiveJobLock(boolean exclusiveJobLock) {
        this.exclusiveJobLock = exclusiveJobLock;
        return this;
    }

    /**
     * Builds a list of JobLock instances with the specified parameters.
     *
     * @return a list of JobLock instances
     */
    public List<JobLock> build() {
        JobLock jobLock = new JobLockImpl();
        jobLock.setName(this.lockName);
        jobLock.setJobs(this.schedulerJobs);
        jobLock.setLockCount(this.lockCount);
        jobLock.setExclusiveJobLock(this.exclusiveJobLock);
        return List.of(jobLock);
    }
}
