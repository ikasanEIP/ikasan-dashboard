package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoInternalEventDrivenJobRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
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

import static org.ikasan.spec.entity.EntityFields.ID;
import static org.ikasan.spec.entity.EntityFields.TYPE;
import static org.ikasan.spec.scheduled.job.model.JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE;

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
        MongoInternalEventDrivenJobRecordImpl mongoRecord = (MongoInternalEventDrivenJobRecordImpl) event;

        mongoRecord.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE);

        InternalEventDrivenJob job = event.getInternalEventDrivenJob();

        if(event.getId() != null && !event.getId().isEmpty()) {
            mongoRecord.setId(event.getId());
        }
        else {
            mongoRecord.setId(JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + event.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
        }
        mongoRecord.setInternalEventDrivenJob(job);
        mongoRecord.setDisplayName(job.getDisplayName());
        mongoRecord.setTargetResidingContextOnly(job.isTargetResidingContextOnly());
        mongoRecord.setParticipatesInLock(job.isParticipatesInLock());
        mongoRecord.setContextName(job.getContextName());
        mongoRecord.setAgentName(job.getAgentName());
        mongoRecord.setJobName(job.getJobName());
        mongoRecord.setTimestamp(mongoRecord.getTimestamp());
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        // only update modified by field if populated.
        if(event.getModifiedBy() != null &&
            !event.getModifiedBy().isEmpty()) {
            mongoRecord.setModifiedBy(event.getModifiedBy());
        }
        mongoRecord.setExpiry(-1);
        mongoRecord.setHeld(event.isHeld());
        mongoRecord.setSkipped(event.isSkipped());

        repository.save(mongoRecord);
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where(EntityFields.TYPE).is(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE);
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

        Criteria criteria = Criteria.where(EntityFields.TYPE).is(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE)
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
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE));

        return mongoTemplate.findOne(query, MongoInternalEventDrivenJobRecordImpl.class);
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
