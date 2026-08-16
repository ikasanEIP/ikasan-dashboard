package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoBridgingJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoBridgingJobRepository;
import org.ikasan.spec.scheduled.job.dao.BridgingJobDao;
import org.ikasan.spec.scheduled.job.model.BridgingJobRecord;
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
 * MongoDB implementation of BridgingJobDao.
 */
public class MongoBridgingJobDao implements BridgingJobDao<BridgingJobRecord> {

    private final MongoBridgingJobRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for custom queries
     */
    public MongoBridgingJobDao(MongoBridgingJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(BridgingJobRecord event) {
        if (!(event instanceof MongoBridgingJobRecordImpl)) {
            throw new IllegalArgumentException("Event must be an instance of MongoBridgingJobRecordImpl");
        }

        MongoBridgingJobRecordImpl mongoRecord = (MongoBridgingJobRecordImpl) event;

        // Generate ID if not set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JobConstants.BRIDGING_JOB + "_"
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
    public void save(List<BridgingJobRecord> events) {
        for (BridgingJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<BridgingJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        long totalCount = repository.count();

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Query query = new Query();
        query.with(pageable);

        List<MongoBridgingJobRecordImpl> results = mongoTemplate.find(query, MongoBridgingJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<BridgingJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("contextName").is(contextId);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoBridgingJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoBridgingJobRecordImpl> results = mongoTemplate.find(query, MongoBridgingJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public BridgingJobRecord findById(String id) {
        Optional<MongoBridgingJobRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }
}
