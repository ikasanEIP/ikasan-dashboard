package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextTerminalJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextTerminalJobRepository;
import org.ikasan.spec.scheduled.job.dao.ContextTerminalJobDao;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
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

import static org.ikasan.spec.entity.EntityFields.*;
import static org.ikasan.spec.scheduled.job.model.JobConstants.CONTEXT_TERMINAL_JOB;

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

        MongoContextTerminalJobRecordImpl mongoRecord = new MongoContextTerminalJobRecordImpl();
        mongoRecord.setType(JobConstants.CONTEXT_TERMINAL_JOB);

        ContextTerminalJob job = event.getContextTerminalJob();
        if(event.getId() != null && !event.getId().isEmpty()) {
            mongoRecord.setId(event.getId());
        }
        else {
            mongoRecord.setId(job.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
        }
        mongoRecord.setContextTerminalJob(job);
        mongoRecord.setContextName(event.getContextName());
        mongoRecord.setDisplayName(event.getDisplayName());
        mongoRecord.setAgentName(event.getAgentName());
        mongoRecord.setJobName(event.getJobName());
        mongoRecord.setTimestamp(event.getTimestamp());
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        // only update modified by field if populated.
        if(event.getModifiedBy() != null &&
            !event.getModifiedBy().isEmpty()) {
            mongoRecord.setModifiedBy(event.getModifiedBy());
        }
        mongoRecord.setExpiry(-1);

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

        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(CONTEXT_TERMINAL_JOB));

        long totalCount = mongoTemplate.count(query, MongoContextTerminalJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoContextTerminalJobRecordImpl> results = mongoTemplate.find(query, MongoContextTerminalJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<ContextTerminalJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria().andOperator(
            Criteria.where(COMPONENT_NAME).is(contextId),
            Criteria.where(TYPE).is(CONTEXT_TERMINAL_JOB)
        );
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
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(CONTEXT_TERMINAL_JOB));

        return mongoTemplate.findOne(query, MongoContextTerminalJobRecordImpl.class);
    }
}
