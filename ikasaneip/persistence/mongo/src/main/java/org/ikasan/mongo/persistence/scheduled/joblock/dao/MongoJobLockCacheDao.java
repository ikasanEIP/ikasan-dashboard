package org.ikasan.mongo.persistence.scheduled.joblock.dao;

import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheRepository;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

public class MongoJobLockCacheDao implements JobLockCacheDao {

    public static final String JOB_LOCK_CACHE_ID = "jobLockCacheIdentifier";
    private static final Logger logger = LoggerFactory.getLogger(MongoJobLockCacheDao.class);

    private final MongoJobLockCacheRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoJobLockCacheDao(MongoJobLockCacheRepository repository,
                                MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public JobLockCacheRecord get(String environment) {
        if(environment == null) {
            environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }

        String id = JOB_LOCK_CACHE_ID + "__" + environment;
        logger.debug("Getting JobLockCacheRecord with id: {}", id);

        Optional<MongoJobLockCacheRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }

    @Override
    public void save(JobLockCacheRecord record) {
        if (!(record instanceof MongoJobLockCacheRecordImpl)) {
            throw new IllegalArgumentException("Record must be an instance of MongoJobLockCacheRecordImpl");
        }

        MongoJobLockCacheRecordImpl mongoRecord = (MongoJobLockCacheRecordImpl) record;

        // Generate ID if not set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JOB_LOCK_CACHE_ID + "__" + mongoRecord.getEnvironment();
            mongoRecord.setId(id);
        }

        // Set timestamp if not already set
        if (mongoRecord.getTimestamp() == 0) {
            mongoRecord.setTimestamp(System.currentTimeMillis());
        }

        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        logger.debug("Saving JobLockCacheRecord with id: {}", mongoRecord.getId());
        repository.save(mongoRecord);
    }
}
