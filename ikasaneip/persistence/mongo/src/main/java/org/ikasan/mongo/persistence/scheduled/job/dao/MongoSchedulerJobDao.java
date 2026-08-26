package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoSchedulerJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoSchedulerJobRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ikasan.spec.metadata.dao.ModuleMetadataDao.MODULE_METADATA;

public class MongoSchedulerJobDao implements SchedulerJobDao<SchedulerJobRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(MongoSchedulerJobDao.class);

    private final MongoSchedulerJobRepository repository;
    private final MongoTemplate mongoTemplate;

    // All job types except those explicitly excluded
    private static final List<String> ALL_JOB_TYPES = Arrays.asList(
        JobConstants.FILE_EVENT_DRIVEN_JOB,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB,
        JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB,
        JobConstants.GLOBAL_EVENT_JOB,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE
    );

    // All job types including context lifecycle jobs
    private static final List<String> ALL_JOB_TYPES_WITH_LIFECYCLE = Arrays.asList(
        JobConstants.FILE_EVENT_DRIVEN_JOB,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB,
        JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB,
        JobConstants.GLOBAL_EVENT_JOB,
        JobConstants.CONTEXT_START_JOB,
        JobConstants.CONTEXT_TERMINAL_JOB,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE
    );

    public MongoSchedulerJobDao(MongoSchedulerJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SearchResults<SchedulerJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where(EntityFields.TYPE).in(ALL_JOB_TYPES);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoSchedulerJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoSchedulerJobRecordImpl> results = mongoTemplate.find(query, MongoSchedulerJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<SchedulerJobRecord> findByContext(String contextName, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        // Build criteria: All job types with lifecycle, and either matching contextName OR GLOBAL_EVENT
        Criteria criteria = Criteria.where("type").in(ALL_JOB_TYPES_WITH_LIFECYCLE)
            .andOperator(
                new Criteria().orOperator(
                    Criteria.where(EntityFields.COMPONENT_NAME).is(contextName),
                    Criteria.where(EntityFields.COMPONENT_NAME).is(JobConstants.GLOBAL_EVENT)
                )
            );

        Query query = new Query(criteria);
        long totalCount = mongoTemplate.count(query, MongoSchedulerJobRecordImpl.class);

        if (limit > -1 && offset > -1) {
            Pageable pageable = PageRequest.of(offset / limit, limit);
            query.with(pageable);
        }

        List<MongoSchedulerJobRecordImpl> results = mongoTemplate.find(query, MongoSchedulerJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<SchedulerJobRecord> findByAgent(String agentName, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where(EntityFields.TYPE).in(ALL_JOB_TYPES_WITH_LIFECYCLE)
            .and(EntityFields.MODULE_NAME).is(agentName);

        Query query = new Query(criteria);
        long totalCount = mongoTemplate.count(query, MongoSchedulerJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoSchedulerJobRecordImpl> results = mongoTemplate.find(query, MongoSchedulerJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<SchedulerJobRecord> findByFilter(SchedulerJobSearchFilter filter, int limit, int offset,
                                                           String sortColumn, String sortDirection) {
        long startTime = System.currentTimeMillis();

        List<Criteria> criteriaList = new ArrayList<>();

        // Handle job type filtering
        if (filter.getJobTypeFilter() != null && !filter.getJobTypeFilter().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.TYPE).is(filter.getJobTypeFilter()));
        } else if (filter.getJobTypes() != null && !filter.getJobTypes().isEmpty()) {
            criteriaList.add(Criteria.where( EntityFields.TYPE).in(filter.getJobTypes()));
        } else {
            // Default to all job types with lifecycle
            criteriaList.add(Criteria.where(EntityFields.TYPE).in(ALL_JOB_TYPES_WITH_LIFECYCLE));
        }

        // Job name filter
        if (filter.getJobNameFilter() != null && !filter.getJobNameFilter().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).regex(".*" + filter.getJobNameFilter() + ".*", "i"));
        }

        // Display name filter
        if (filter.getDisplayNameFilter() != null && !filter.getDisplayNameFilter().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.DISPLAY_NAME).regex(".*" + filter.getDisplayNameFilter() + ".*", "i"));
        }

        // NOT job name filter
        if (filter.getNotJobNameInFilter() != null && !filter.getNotJobNameInFilter().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).nin(filter.getNotJobNameInFilter()));
        }

        // Context name filters
        if (filter.getContextNames() != null && !filter.getContextNames().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.COMPONENT_NAME).in(filter.getContextNames()));
        } else if (filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty()) {
            criteriaList.add(
                new Criteria().orOperator(
                    Criteria.where(EntityFields.COMPONENT_NAME).is(filter.getContextSearchFilter()),
                    Criteria.where(EntityFields.COMPONENT_NAME).is(JobConstants.GLOBAL_EVENT)
                )
            );
        }

        // Boolean filters
        if (filter.isHeld()) {
            criteriaList.add(Criteria.where(EntityFields.HELD).is(true));
        }

        if (filter.isSkipped()) {
            criteriaList.add(Criteria.where(EntityFields.SKIPPED).is(true));
        }

        if (filter.isTargetResidingContextOnly() != null) {
            criteriaList.add(Criteria.where(EntityFields.TARGET_RESIDING_CONTEXT_ONLY).is(filter.isTargetResidingContextOnly()));
        }

        if (filter.isParticipatesInLock() != null) {
            criteriaList.add(Criteria.where(EntityFields.PARTICIPATES_IN_LOCK).is(filter.isParticipatesInLock()));
        }

        // Combine all criteria
        Criteria finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(finalCriteria);

        // Count total results
        long totalCount = mongoTemplate.count(query, MongoSchedulerJobRecordImpl.class);

        // Apply sorting
        Sort sort;
        if (sortColumn != null && !sortColumn.isEmpty() && sortDirection != null && !sortDirection.isEmpty()) {
            sort = sortDirection.equals("ASCENDING")
                ? Sort.by(Sort.Direction.ASC, sortColumn)
                : Sort.by(Sort.Direction.DESC, sortColumn);
        } else {
            // Default sort by jobName descending
            sort = Sort.by(Sort.Direction.DESC, EntityFields.FLOW_NAME);
        }

        if(limit > -1 && offset > -1) {
            // Apply pagination
            Pageable pageable = PageRequest.of(offset / limit, limit, sort);
            query.with(pageable);
        }

        List<MongoSchedulerJobRecordImpl> results = mongoTemplate.find(query, MongoSchedulerJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SchedulerJobRecord findById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public SchedulerJobRecord findByContextIdAndJobName(String contextId, String jobName) {
        Criteria criteria = Criteria.where(EntityFields.TYPE).in(ALL_JOB_TYPES_WITH_LIFECYCLE)
            .and(EntityFields.FLOW_NAME).is(jobName)
            .andOperator(
                new Criteria().orOperator(
                    Criteria.where(EntityFields.COMPONENT_NAME).is(contextId),
                    Criteria.where(EntityFields.COMPONENT_NAME).is(JobConstants.GLOBAL_EVENT)
                )
            );

        Query query = new Query(criteria);
        List<MongoSchedulerJobRecordImpl> results = mongoTemplate.find(query, MongoSchedulerJobRecordImpl.class);

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void delete(SchedulerJobRecord record) {
        repository.deleteById(record.getId());
    }

    @Override
    public void deleteByAgentName(String agentName) {
        repository.deleteByAgentName(agentName);
    }

    @Override
    public void deleteByContextName(String contextName) {
        repository.deleteByContextName(contextName);
    }

    @Override
    public void save(SchedulerJobRecord event) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    @Override
    public void save(List<SchedulerJobRecord> events) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    @Override
    public List<String> getAllAgentNames() {
        // Query for documents with type="moduleMetaData" and module_metadata_json containing "type":"SCHEDULER_AGENT"
        // This is equivalent to Solr query: type:"moduleMetaData" AND payload:"*\"type\":\"SCHEDULER_AGENT\"*"
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where(EntityFields.TYPE).is(MODULE_METADATA)
                .and(EntityFields.PAYLOAD_CONTENT).regex(".*\"type\":\"SCHEDULER_AGENT\".*")),
            Aggregation.group().addToSet(EntityFields.ID).as("agentNames"),
            Aggregation.project().and("agentNames").as("agentNames")
        );

        AggregationResults<AgentNameResult> results = mongoTemplate.aggregate(
            aggregation,
            MongoConstants.IKASAN_COLLECTION_NAME,
            AgentNameResult.class
        );

        List<String> agentNames = new ArrayList<>();
        if (results.getUniqueMappedResult() != null) {
            agentNames.addAll(results.getUniqueMappedResult().getAgentNames());
        }

        return agentNames;
    }

    // Helper class for aggregation results
    private static class AgentNameResult {
        private List<String> agentNames;

        public List<String> getAgentNames() {
            return agentNames;
        }

        public void setAgentNames(List<String> agentNames) {
            this.agentNames = agentNames;
        }
    }
}
