package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoInternalEventDrivenJobRepository;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MongoInternalEventDrivenJobDao implements InternalEventDrivenJobDao<InternalEventDrivenJobRecord> {

    private final MongoInternalEventDrivenJobRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoInternalEventDrivenJobDao(MongoInternalEventDrivenJobRepository repository,
                                          MongoTemplate mongoTemplate) {
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
            String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_"
                + mongoRecord.getAgentName() + "_"
                + mongoRecord.getJobName() + "_"
                + mongoRecord.getInternalEventDrivenJob().getContextName();
            mongoRecord.setId(id);
        }

        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        repository.save(mongoRecord);
    }

    @Override
    public void save(List<InternalEventDrivenJobRecord> events) {
        events.forEach(this::save);
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        long totalCount = mongoTemplate.count(query, MongoInternalEventDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoInternalEventDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoInternalEventDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("contextName").is(contextId);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoInternalEventDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoInternalEventDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoInternalEventDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public InternalEventDrivenJobRecord findById(String id) {
        Optional<MongoInternalEventDrivenJobRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }

    @Override
    public void skip(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setSkippedContexts(new HashMap<>());
        if(internalEventDrivenJob.isTargetResidingContextOnly()) {
            childContextNames.forEach(name ->
                internalEventDrivenJob.getSkippedContexts().put(name, true));
        }
        else {
            internalEventDrivenJob.getChildContextNames().forEach(name ->
                internalEventDrivenJob.getSkippedContexts().put(name, true));
        }

        jobRecord.setSkipped(true);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void hold(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setHeldContexts(new HashMap<>());
        if(internalEventDrivenJob.isTargetResidingContextOnly()) {
            childContextNames.forEach(name ->
                internalEventDrivenJob.getHeldContexts().put(name, true));
        }
        else {
            internalEventDrivenJob.getChildContextNames().forEach(name ->
                internalEventDrivenJob.getHeldContexts().put(name, true));
        }

        jobRecord.setHeld(true);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void enable(InternalEventDrivenJobRecord jobRecord, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setSkippedContexts(new HashMap<>());
        jobRecord.setSkipped(false);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void release(InternalEventDrivenJobRecord jobRecord, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setHeldContexts(new HashMap<>());
        jobRecord.setHeld(false);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void releaseAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        jobRecords.forEach(jobRecord -> {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setHeldContexts(new HashMap<>());
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setHeld(false);
            jobRecord.setModifiedBy(actor);
        });

        save(jobRecords);
    }

    @Override
    public void holdAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        jobRecords.forEach(jobRecord -> {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            if(internalEventDrivenJob.getChildContextNames() != null) {
                HashMap<String, Boolean> heldContexts = new HashMap<>();

                internalEventDrivenJob.getChildContextNames()
                    .forEach(name -> heldContexts.put(name, Boolean.TRUE));

                internalEventDrivenJob.setHeldContexts(heldContexts);
            }
            internalEventDrivenJob.setSkippedContexts(new HashMap<>());
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setSkipped(false);
            jobRecord.setHeld(true);
            jobRecord.setModifiedBy(actor);
        });

        save(jobRecords);
    }

    @Override
    public void enableAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        jobRecords.forEach(jobRecord -> {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setSkippedContexts(new HashMap<>());
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setSkipped(false);
            jobRecord.setModifiedBy(actor);
        });

        save(jobRecords);
    }
}
