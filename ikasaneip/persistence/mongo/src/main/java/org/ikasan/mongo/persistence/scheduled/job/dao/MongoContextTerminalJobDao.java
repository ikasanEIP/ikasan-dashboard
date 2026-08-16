package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextTerminalJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextTerminalJobRepository;
import org.ikasan.spec.scheduled.job.dao.ContextTerminalJobDao;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJobRecord;
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
 * MongoDB implementation of ContextTerminalJobDao.
 */
public class MongoContextTerminalJobDao implements ContextTerminalJobDao<ContextTerminalJobRecord> {

    private final MongoContextTerminalJobRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for custom queries
     */
    public MongoContextTerminalJobDao(MongoContextTerminalJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(ContextTerminalJobRecord event) {
        if (!(event instanceof MongoContextTerminalJobRecordImpl)) {
            throw new IllegalArgumentException("Event must be an instance of MongoContextTerminalJobRecordImpl");
        }

        MongoContextTerminalJobRecordImpl mongoRecord = (MongoContextTerminalJobRecordImpl) event;

        // Generate ID if not set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JobConstants.CONTEXT_TERMINAL_JOB + "_"
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
    public void save(List<ContextTerminalJobRecord> events) {
        for (ContextTerminalJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<ContextTerminalJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        long totalCount = repository.count();

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Query query = new Query();
        query.with(pageable);

        List<MongoContextTerminalJobRecordImpl> results = mongoTemplate.find(query, MongoContextTerminalJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<ContextTerminalJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("contextName").is(contextId);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoContextTerminalJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoContextTerminalJobRecordImpl> results = mongoTemplate.find(query, MongoContextTerminalJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public ContextTerminalJobRecord findById(String id) {
        Optional<MongoContextTerminalJobRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }
}
