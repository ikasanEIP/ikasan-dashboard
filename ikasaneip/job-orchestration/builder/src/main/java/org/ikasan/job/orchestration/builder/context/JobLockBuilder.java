package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobLockBuilder {
    public static final String CONTEXT_SEPARATOR = ":CHILD:";

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
     * Adds a job to the builder using the context name derived from the provided job object.
     *
     * This method serves as a convenience wrapper for the overloaded {@code withJob} method,
     * automatically setting the parent and child context names based on the {@code job} parameter.
     *
     * @param job the job to be added, which provides its context name
     * @return the current instance of {@code JobLockBuilder} for method chaining
     */
    public JobLockBuilder withJob(SchedulerJobLockParticipant job) {
        return withJob(job.getContextName(), job.getChildContextName(), job);
    }

    /**
     * Adds a job to the schedulerJobs map for a given context name.
     * If the map does not already include the context name, it initializes a new list
     * and then adds the specified job to the list.
     *
     * @param parentContextName the name of the parent context, used for referential purposes
     * @param contextName the name of the context where the job will be added
     * @param job the job to be added to the specified context
     * @return the current instance of {@code JobLockBuilder} for method chaining
     */
    public JobLockBuilder withJob(String parentContextName, String contextName, SchedulerJobLockParticipant job) {
        if(this.schedulerJobs == null) {
            schedulerJobs = new HashMap<>();
        }

        if(!schedulerJobs.containsKey(parentContextName + CONTEXT_SEPARATOR + contextName)) {
            schedulerJobs.put(parentContextName + CONTEXT_SEPARATOR + contextName, new ArrayList<>());
        }

        schedulerJobs.get(parentContextName + CONTEXT_SEPARATOR + contextName).add(job);
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
