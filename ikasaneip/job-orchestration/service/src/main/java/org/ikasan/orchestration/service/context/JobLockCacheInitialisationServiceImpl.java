package org.ikasan.orchestration.service.context;

import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobLockCacheInitialisationServiceImpl implements JobLockCacheInitialisationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLockCacheInitialisationServiceImpl.class);

    protected final JobLockCacheService jobLockCacheService;

    /**
     * Initializes the job lock cache with the provided job lock cache service.
     *
     * @param jobLockCacheService the job lock cache service
     * @throws IllegalArgumentException if jobLockCacheService is null
     */
    public JobLockCacheInitialisationServiceImpl(JobLockCacheService jobLockCacheService) {
        this.jobLockCacheService = jobLockCacheService;
        if(this.jobLockCacheService == null) {
            throw new IllegalArgumentException("jobLockCacheService cannot be null!");
        }
        JobLockCacheImpl.instance().setJobLockCacheService(jobLockCacheService);
    }

    @Override
    public void initialiseJobLockCache(Context context, boolean isRefresh) {
        LOGGER.info(String.format("Initialising job lock cache for context[%s], refresh[%s]", context.getName(), isRefresh));
        JobLockCacheRecord jobLockCacheRecord = jobLockCacheService.get(context.getEnvironmentGroup());

        if (jobLockCacheRecord != null) {
            JobLockCacheImpl.instance().setJobLockCacheRecord(jobLockCacheRecord, context.getEnvironmentGroup());
        }

        if(isRefresh) {
            JobLockCacheImpl.instance().removeJobsLocksForContext(context);
            JobLockCacheImpl.instance().addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());
        }
        LOGGER.info(String.format("Successfully initialised job lock cache for context[%s]", context.getName()));
    }

    @Override
    public void removeJobLocksFromCache(Context context) {
        LOGGER.info(String.format("Removing job locks from cache for context[%s]", context.getName()));
        JobLockCacheImpl.instance().removeJobsLocksForContext(context);
        LOGGER.info(String.format("Successfully removed job locks from cache for context[%s]", context.getName()));
    }
}
