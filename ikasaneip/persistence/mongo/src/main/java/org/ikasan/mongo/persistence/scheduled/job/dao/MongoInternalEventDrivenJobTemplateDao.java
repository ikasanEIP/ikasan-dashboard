package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoInternalEventDrivenJobRepository;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MongoInternalEventDrivenJobTemplateDao extends MongoInternalEventDrivenJobDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoInternalEventDrivenJobTemplateDao.class);

    private final MongoInternalEventDrivenJobRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoInternalEventDrivenJobTemplateDao(MongoInternalEventDrivenJobRepository repository,
                                                   MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate);
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(InternalEventDrivenJobRecord event) {
        if (!(event instanceof MongoInternalEventDrivenJobRecordImpl)) {
            throw new IllegalArgumentException("Event must be an instance of MongoInternalEventDrivenJobRecordImpl");
        }

        MongoInternalEventDrivenJobRecordImpl mongoRecord = (MongoInternalEventDrivenJobRecordImpl) event;

        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE + "_"
                + mongoRecord.getAgentName() + "_"
                + mongoRecord.getJobName() + "_"
                + mongoRecord.getInternalEventDrivenJob().getContextName();
            mongoRecord.setId(id);
        }

        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        repository.save(mongoRecord);
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("id").regex("^" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE + "_");
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoInternalEventDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoInternalEventDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoInternalEventDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        logger.debug("findAll query took {}ms, found {} results", queryTime, totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("id").regex("^" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE + "_")
            .and("contextName").is(contextId);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoInternalEventDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoInternalEventDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoInternalEventDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        logger.debug("findByContext query took {}ms, found {} results", queryTime, totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public InternalEventDrivenJobRecord findById(String id) {
        Optional<MongoInternalEventDrivenJobRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }

    @Override
    public void skip(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job templates");
    }

    @Override
    public void hold(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job templates");
    }

    @Override
    public void enable(InternalEventDrivenJobRecord jobRecord, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job templates");
    }

    @Override
    public void release(InternalEventDrivenJobRecord jobRecord, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job templates");
    }

    @Override
    public void releaseAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job templates");
    }

    @Override
    public void holdAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job templates");
    }

    @Override
    public void enableAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job templates");
    }
}
