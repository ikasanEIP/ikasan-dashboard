package org.ikasan.orchestration.service.status;

import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;

public class JobLockCacheServiceTestImpl implements JobLockCacheService {
    @Override
    public void save(JobLockCacheRecord jobLockCacheLockHolderRecord) {

    }

    @Override
    public JobLockCacheRecord get() {
        return null;
    }

    @Override
    public SearchResults<JobLockCacheAuditRecord> findAll(int limit, int offset) {
        return null;
    }
}
