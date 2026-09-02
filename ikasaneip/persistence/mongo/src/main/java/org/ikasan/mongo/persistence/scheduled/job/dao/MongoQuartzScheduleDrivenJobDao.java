package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoQuartzScheduleDrivenJobRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
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
import static org.ikasan.spec.scheduled.job.model.JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB;

public class MongoQuartzScheduleDrivenJobDao implements QuartzScheduleDrivenJobDao<QuartzScheduleDrivenJobRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(MongoQuartzScheduleDrivenJobDao.class);

    private final MongoQuartzScheduleDrivenJobRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoQuartzScheduleDrivenJobDao(MongoQuartzScheduleDrivenJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(QuartzScheduleDrivenJobRecord event) {
        MongoQuartzScheduleDrivenJobRecordImpl mongoRecord = new MongoQuartzScheduleDrivenJobRecordImpl();

        if(event.getId() != null && !event.getId().isEmpty()) {
            mongoRecord.setId(event.getId());
        }
        else {
            mongoRecord.setId(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB + "_" + event.getAgentName() + "_" + event.getJobName()
                + "_" + event.getQuartzScheduleDrivenJob().getContextName());
        }
        mongoRecord.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);
        mongoRecord.setQuartzScheduleDrivenJob(event.getQuartzScheduleDrivenJob());
        mongoRecord.setAgentName(event.getAgentName());
        mongoRecord.setJobName(event.getJobName());
        mongoRecord.setContextName(event.getQuartzScheduleDrivenJob().getContextName());
        mongoRecord.setDisplayName(event.getQuartzScheduleDrivenJob().getDisplayName());
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
    public void save(List<QuartzScheduleDrivenJobRecord> events) {
        events.forEach(this::save);
    }

    @Override
    public SearchResults<QuartzScheduleDrivenJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(QUARTZ_SCHEDULE_DRIVEN_JOB));

        long totalCount = mongoTemplate.count(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoQuartzScheduleDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<QuartzScheduleDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria().andOperator(
            Criteria.where(EntityFields.COMPONENT_NAME).is(contextId),
            Criteria.where(TYPE).is(QUARTZ_SCHEDULE_DRIVEN_JOB)
        );
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoQuartzScheduleDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public QuartzScheduleDrivenJobRecord findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(QUARTZ_SCHEDULE_DRIVEN_JOB));

        return mongoTemplate.findOne(query, MongoQuartzScheduleDrivenJobRecordImpl.class);
    }
}
