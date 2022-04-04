package org.ikasan.scheduled.joblockcache.service;

import org.ikasan.scheduled.joblockcache.dao.SolrJobLockCacheAuditDaoImpl;
import org.ikasan.scheduled.joblockcache.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.joblockcache.model.SolrJobLockCacheAuditRecordImpl;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;

public class SolrJobLockCacheServiceImpl implements JobLockCacheService {

    private SolrJobLockCacheDaoImpl solrJobLockCacheDao;
    private SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao;

    public SolrJobLockCacheServiceImpl(SolrJobLockCacheDaoImpl solrJobLockCacheDao, SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao) {
        if (solrJobLockCacheDao == null) {
            throw new IllegalArgumentException("solrJobLockCacheDao can not be null!");
        }
        if (solrJobLockCacheAuditDao == null) {
            throw new IllegalArgumentException("solrJobLockCacheAuditDao can not be null!");
        }
        this.solrJobLockCacheDao = solrJobLockCacheDao;
        this.solrJobLockCacheAuditDao = solrJobLockCacheAuditDao;
    }

    @Override
    public void save(JobLockCacheRecord record) {
        solrJobLockCacheDao.save(record);

        JobLockCacheAuditRecord audit = new SolrJobLockCacheAuditRecordImpl();
        audit.setJobLockCache(record.getJobLockCache());
        audit.setTimestamp(record.getTimestamp());
        solrJobLockCacheAuditDao.save(audit);
    }

    @Override
    public JobLockCacheRecord get() {
        return solrJobLockCacheDao.get();
    }

    @Override
    public SearchResults<JobLockCacheAuditRecord> findAll(int limit, int offset) {
        return solrJobLockCacheAuditDao.findAll(limit, offset);
    }
}
