package org.ikasan.mongo.persistence.scheduled.joblock.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheAuditRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheAuditRepository;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MongoJobLockCacheAuditDao implements JobLockCacheAuditDao {

    private static final String JOB_LOCK_AUDIT_CACHE_TYPE_ID = "jockLockCacheRecordAuditID";
    private static final Logger logger = LoggerFactory.getLogger(MongoJobLockCacheAuditDao.class);

    private final MongoJobLockCacheAuditRepository repository;
    private final MongoTemplate mongoTemplate;
    private final int daysToKeep;

    public MongoJobLockCacheAuditDao(MongoJobLockCacheAuditRepository repository,
                                     MongoTemplate mongoTemplate,
                                     int daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
    }

    @Override
    public SearchResults<JobLockCacheAuditRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        long totalCount = mongoTemplate.count(query, MongoJobLockCacheAuditRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoJobLockCacheAuditRecordImpl> results = mongoTemplate.find(query, MongoJobLockCacheAuditRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        logger.debug("findAll query took {}ms, found {} results", queryTime, totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public void save(JobLockCacheAuditRecord record) {
        MongoJobLockCacheAuditRecordImpl mongoRecord = new MongoJobLockCacheAuditRecordImpl();

        mongoRecord.setId(JOB_LOCK_AUDIT_CACHE_TYPE_ID + "_" + UUID.randomUUID());
        mongoRecord.setType(JOB_LOCK_AUDIT_CACHE_TYPE);
        mongoRecord.setJobLockCache(record.getJobLockCache());
        mongoRecord.setTimestamp(System.currentTimeMillis());
        mongoRecord.setExpiry(daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());


        logger.debug("Saving JobLockCacheAuditRecord with id: {}", mongoRecord.getId());
        repository.save(mongoRecord);
    }
}
