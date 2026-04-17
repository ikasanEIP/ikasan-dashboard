package org.ikasan.scheduled.joblock.service;

import org.ikasan.scheduled.joblock.model.SolrJobLockCacheAuditRecordImpl;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;

public class SolrJobLockCacheServiceImpl implements JobLockCacheService {

    private final JobLockCacheDao jobLockCacheDao;
    private final JobLockCacheAuditDao jobLockCacheAuditDao;
    private final boolean saveJobLockCacheAudits;

    public SolrJobLockCacheServiceImpl(JobLockCacheDao jobLockCacheDao, JobLockCacheAuditDao jobLockCacheAuditDao, boolean saveJobLockCacheAudits) {
        if (jobLockCacheDao == null) {
            throw new IllegalArgumentException("solrJobLockCacheDao can not be null!");
        }
        if (jobLockCacheAuditDao == null) {
            throw new IllegalArgumentException("solrJobLockCacheAuditDao can not be null!");
        }
        this.jobLockCacheDao = jobLockCacheDao;
        this.jobLockCacheAuditDao = jobLockCacheAuditDao;
        this.saveJobLockCacheAudits = saveJobLockCacheAudits;
    }

    @Override
    public void save(JobLockCacheRecord record) {
        this.jobLockCacheDao.save(record);

        if (this.saveJobLockCacheAudits) {
            JobLockCacheAuditRecord audit = new SolrJobLockCacheAuditRecordImpl();
            audit.setJobLockCache(record.getJobLockCache());
            this.jobLockCacheAuditDao.save(audit);
        }
    }

    @Override
    public JobLockCacheRecord get(String environment) {
        return this.jobLockCacheDao.get(environment);
    }

    @Override
    public SearchResults<JobLockCacheAuditRecord> findAll(int limit, int offset) {
        return this.jobLockCacheAuditDao.findAll(limit, offset);
    }
}
