package org.ikasan.orchestration.service.context;

import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;

public class JobLockCacheInitialisationServiceImpl implements JobLockCacheInitialisationService {

    protected final JobLockCacheService jobLockCacheService;

    public JobLockCacheInitialisationServiceImpl(JobLockCacheService jobLockCacheService) {
        this.jobLockCacheService = jobLockCacheService;
        if(this.jobLockCacheService == null) {
            throw new IllegalArgumentException("jobLockCacheService cannot be null!");
        }
    }

    public void initialiseJobLockCache(Context context) {
        JobLockCacheRecord jobLockCacheRecord = jobLockCacheService.get();
        JobLockCacheImpl.instance().setJobLockCacheService(jobLockCacheService);

        if (jobLockCacheRecord != null) {
            JobLockCacheImpl.instance().setJobLockCacheRecord(jobLockCacheRecord);
        }

        JobLockCacheImpl.instance().removeJobsLocksForContext(context);
        JobLockCacheImpl.instance().addLocks(context.getAllNestedJobLocks());
    }

    @Override
    public void removeJobLocksFromCache(Context context) {
        JobLockCacheImpl.instance().removeJobsLocksForContext(context);
    }
}
