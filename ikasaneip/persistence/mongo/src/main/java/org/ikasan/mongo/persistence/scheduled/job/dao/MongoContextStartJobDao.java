package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextStartJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextStartJobRepository;
import org.ikasan.spec.scheduled.job.dao.ContextStartJobDao;
import org.ikasan.spec.scheduled.job.model.ContextStartJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of ContextStartJobDao.
 */
public class MongoContextStartJobDao implements ContextStartJobDao<ContextStartJobRecord> {

    private final MongoContextStartJobRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for custom queries
     */
    public MongoContextStartJobDao(MongoContextStartJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(ContextStartJobRecord event) {
        if (!(event instanceof MongoContextStartJobRecordImpl)) {
            throw new IllegalArgumentException("Event must be an instance of MongoContextStartJobRecordImpl");
        }

        MongoContextStartJobRecordImpl mongoRecord = (MongoContextStartJobRecordImpl) event;

        // Generate ID if not set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JobConstants.CONTEXT_START_JOB + "_"
                + mongoRecord.getAgentName() + "_"
                + mongoRecord.getJobName() + "_"
                + mongoRecord.getContextName();
            mongoRecord.setId(id);
        }

        // Set modified timestamp
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<ContextStartJobRecord> events) {
        for (ContextStartJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<ContextStartJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        long totalCount = repository.count();

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Query query = new Query();
        query.with(pageable);

        List<MongoContextStartJobRecordImpl> results = mongoTemplate.find(query, MongoContextStartJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<ContextStartJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("contextName").is(contextId);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoContextStartJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoContextStartJobRecordImpl> results = mongoTemplate.find(query, MongoContextStartJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public ContextStartJobRecord findById(String id) {
        Optional<MongoContextStartJobRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }
}
