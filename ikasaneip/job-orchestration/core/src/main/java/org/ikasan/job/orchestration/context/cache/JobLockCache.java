package org.ikasan.job.orchestration.context.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.ikasan.spec.scheduled.job.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public final class JobLockCache {

    private final ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName;
    private final ConcurrentHashMap<String, JobLockHolder> jobLocksByIdentifier;

    private JobLockCache() {
        jobLocksByLockName = new ConcurrentHashMap<>();
        jobLocksByIdentifier = new ConcurrentHashMap<>();
    }

    private static final class JobLockMachineHolder {
        public final static JobLockCache INSTANCE = new JobLockCache();
    }

    public static JobLockCache instance() {
        return JobLockMachineHolder.INSTANCE;
    }

    public synchronized void addLock(JobLock jobLock) {
        if (jobLock != null) {
            // we need jobLocksByLockName to create the global lock holder added later in jobLocksByIdentifier
            JobLockHolder jobLockHolder = jobLocksByLockName.get(jobLock.getName());
            if (jobLockHolder == null) {
                jobLockHolder = new JobLockHolder();
                jobLockHolder.setLockName(jobLock.getName());
                jobLockHolder.setLockCount(jobLock.getLockCount());
                jobLockHolder.addSchedulerJobs(jobLock.getJobs());
            } else {
                jobLockHolder.addSchedulerJobs(jobLock.getJobs());
            }
            jobLocksByLockName.put(jobLock.getName(), jobLockHolder);

            List<SchedulerJob> jobs = jobLocksByLockName.get(jobLock.getName()).getSchedulerJobs();
            for (SchedulerJob schedulerJob : jobs) {
                jobLocksByIdentifier.put(schedulerJob.getIdentifier(), jobLockHolder);
            }
        }
    }

    public synchronized void addLocks(List<JobLock> jobLocks) {
        if (jobLocks != null) {
            jobLocks.forEach(this::addLock);
        }
    }

    public synchronized boolean lock(String jobIdentifier) {
        if (jobIdentifier != null) {
            JobLockHolder jobLockHolder = jobLocksByIdentifier.get(jobIdentifier);
            if (jobLockHolder != null && !locked(jobIdentifier)) {
                jobLockHolder.addLockHolder(jobIdentifier);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean release(String jobIdentifier) {
        if (jobIdentifier != null) {
            JobLockHolder jobLockHolder = jobLocksByIdentifier.get(jobIdentifier);
            if (jobLockHolder != null) {
                return jobLockHolder.removeLockHolder(jobIdentifier);
            }
        }
        return false;
    }

    public synchronized boolean locked(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh != null && workingCountIsGreaterThanOrEqualToLockCount(jlh);
    }

    public synchronized boolean hasLock(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh != null && jlh.getLockHolders().contains(jobIdentifier);
    }

    public synchronized boolean existsByIdentifier(String jobIdentifier) {
        return jobIdentifier != null && jobLocksByIdentifier.get(jobIdentifier) != null;
    }

    public synchronized boolean existsByJobLockName(String jobLockName) {
        return jobLockName != null && jobLocksByLockName.get(jobLockName) != null;
    }

    public synchronized List<SchedulerJob> getJobsForIdentifier(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh == null ? Collections.emptyList() : jlh.getSchedulerJobs();
    }

    public synchronized void reset() {
        jobLocksByLockName.clear();
        jobLocksByIdentifier.clear();
    }

    public synchronized boolean resetLock(String lockName) {
        if (existsByJobLockName(lockName)) {
            jobLocksByLockName.get(lockName).getLockHolders().clear();
            return true;
        }
        return false;
    }

    private JobLockHolder getJobLockHolderForJobIdentifier(String jobIdentifier) {
        JobLockHolder jlh = null;
        if (jobIdentifier != null) {
            jlh = jobLocksByIdentifier.get(jobIdentifier);
        }
        return jlh;
    }

    private boolean workingCountIsGreaterThanOrEqualToLockCount(JobLockHolder jlh) {
        return jlh.getLockHolders().size() >= jlh.getLockCount();
    }

    protected static class JobLockHolder {
        private String lockName;
        private long lockCount = 1;
        private List<SchedulerJob> schedulerJobs = new ArrayList<>();
        private Set<String> lockHolders = new HashSet<>();

        public String getLockName() {
            return lockName;
        }

        public void setLockName(String lockName) {
            this.lockName = lockName;
        }

        public long getLockCount() {
            return lockCount;
        }

        public void setLockCount(long lockCount) {
            this.lockCount = lockCount;
        }

        public List<SchedulerJob> getSchedulerJobs() {
            return schedulerJobs;
        }

        public void addSchedulerJobs(List<SchedulerJob> jobs) {
            this.schedulerJobs.addAll(jobs);
        }

        public Set<String> getLockHolders() {
            return lockHolders;
        }

        public void addLockHolder(String jobIdentifier) {
            lockHolders.add(jobIdentifier);
        }

        public boolean removeLockHolder(String jobIdentifier) {
            return lockHolders.remove(jobIdentifier);
        }
    }
}
