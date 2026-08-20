package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoSchedulerJobInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoSchedulerJobInstanceRecordRepository;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of SchedulerJobInstanceDao.
 *
 * This DAO leverages:
 * - Spring Data MongoDB for repository operations
 * - MongoTemplate for complex queries and aggregations
 * - MongoDB indexing for performance on contextInstanceId, contextName, status, etc.
 */
public class MongoSchedulerJobInstanceDaoImpl implements SchedulerJobInstanceDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoSchedulerJobInstanceDaoImpl.class);

    private final MongoSchedulerJobInstanceRecordRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoSchedulerJobInstanceDaoImpl(MongoSchedulerJobInstanceRecordRepository repository,
                                            MongoTemplate mongoTemplate) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        if (mongoTemplate == null) {
            throw new IllegalArgumentException("mongoTemplate cannot be null");
        }
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SchedulerJobInstanceRecord findById(String id) {
        logger.debug("Finding SchedulerJobInstanceRecord by id: {}", id);
        return repository.findById(id).orElse(null);
    }

    @Override
    public void save(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        logger.debug("Saving SchedulerJobInstanceRecord: {}", schedulerJobInstanceRecord.getId());

        if (!(schedulerJobInstanceRecord instanceof MongoSchedulerJobInstanceRecordImpl)) {
            throw new IllegalArgumentException("schedulerJobInstanceRecord must be an instance of MongoSchedulerJobInstanceRecordImpl");
        }

        MongoSchedulerJobInstanceRecordImpl mongoRecord = (MongoSchedulerJobInstanceRecordImpl) schedulerJobInstanceRecord;
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords) {
        List<MongoSchedulerJobInstanceRecordImpl> records = schedulerJobInstanceRecords.stream()
                .map(record -> {
                    MongoSchedulerJobInstanceRecordImpl mongoRecord = (MongoSchedulerJobInstanceRecordImpl) record;
                    mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
                    return mongoRecord;
                })
                .collect(Collectors.toList());

        repository.saveAll(records);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String contextInstanceId,
                                                                                                  int limit,
                                                                                                  int offset,
                                                                                                  String sortField,
                                                                                                  String sortDirection) {
        logger.debug("Finding job instances by contextInstanceId: {}, limit={}, offset={}, sortField={}, sortDirection={}",
            contextInstanceId, limit, offset, sortField, sortDirection);

        Query query = new Query(Criteria.where("contextInstanceId").is(contextInstanceId));

        return executeQueryWithPagination(query, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String contextName,
                                                                                            int limit,
                                                                                            int offset,
                                                                                            String sortField,
                                                                                            String sortDirection) {
        logger.debug("Finding job instances by contextName: {}, limit={}, offset={}, sortField={}, sortDirection={}",
            contextName, limit, offset, sortField, sortDirection);

        Query query = new Query(Criteria.where("contextName").is(contextName));

        return executeQueryWithPagination(query, limit, offset, sortField, sortDirection);
    }

    @Override
    public boolean doesJobPlanInstanceContainRepeatingJobs(String contextInstanceId) {
        logger.debug("Checking if job plan instance contains repeating jobs: {}", contextInstanceId);

        Query query = new Query(Criteria.where("contextInstanceId").is(contextInstanceId));

        List<MongoSchedulerJobInstanceRecordImpl> jobInstances = mongoTemplate.find(query, MongoSchedulerJobInstanceRecordImpl.class);

        // Check if any job instance is repeating by examining the job instance types
        for (MongoSchedulerJobInstanceRecordImpl record : jobInstances) {
            SchedulerJobInstance instance = record.getSchedulerJobInstance();
            if(instance instanceof InternalEventDrivenJobInstance) {
                if(((InternalEventDrivenJobInstance)instance).isJobRepeatable()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter filter,
                                                                                           int limit,
                                                                                           int offset,
                                                                                           String sortField,
                                                                                           String sortDirection) {
        logger.debug("Finding job instances by filter: {}, limit={}, offset={}, sortField={}, sortDirection={}",
            filter, limit, offset, sortField, sortDirection);

        Query query = buildQueryFromFilter(filter);

        return executeQueryWithPagination(query, limit, offset, sortField, sortDirection);
    }

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstances(List<String> contextInstanceIds) {
        logger.debug("Getting job status count for context instances: {}", contextInstanceIds);

        if (contextInstanceIds == null || contextInstanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Build aggregation pipeline
        MatchOperation matchStage = Aggregation.match(Criteria.where("context_instance_id").in(contextInstanceIds));

        GroupOperation groupStage = Aggregation.group("context_instance_id", "status")
            .count().as("statusCount")
            .first("context_name").as("contextName");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, MongoConstants.IKASAN_COLLECTION_NAME, Map.class);

        // Process results into ContextInstanceAggregateJobStatus objects
        Map<String, ContextInstanceAggregateJobStatusImpl> aggregateMap = new HashMap<>();

        for (Map<String, Object> result : results.getMappedResults()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> idMap = (Map<String, Object>) result.get("_id");
            String contextInstanceId = (String) idMap.get("context_instance_id");
            String status = (String) idMap.get("status");
            Integer count = (Integer) result.get("statusCount");
            String contextName = (String) result.get("contextName");

            ContextInstanceAggregateJobStatusImpl aggregate = aggregateMap.computeIfAbsent(
                contextInstanceId,
                id -> new ContextInstanceAggregateJobStatusImpl(id, contextName)
            );

            try {
                InstanceStatus instanceStatus = InstanceStatus.valueOf(status);
                aggregate.addStatusCount(instanceStatus, count);
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown status: {}", status);
            }
        }

        // Check for repeating jobs
        for (String contextInstanceId : contextInstanceIds) {
            if (aggregateMap.containsKey(contextInstanceId)) {
                boolean containsRepeating = doesJobPlanInstanceContainRepeatingJobs(contextInstanceId);
                aggregateMap.get(contextInstanceId).setContainsRepeatableJobs(containsRepeating);
            }
        }

        return new ArrayList<>(aggregateMap.values());
    }

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(List<String> contextInstanceIds) {
        logger.debug("Getting job status count for context instances (considering non-targeted duplication): {}", contextInstanceIds);

        if (contextInstanceIds == null || contextInstanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // For this implementation, we filter out jobs where targetResidingContextOnly = false
        // to avoid counting duplicated jobs that appear in multiple child contexts

        MatchOperation matchStage = Aggregation.match(
            Criteria.where("context_instance_id").in(contextInstanceIds)
                .and("target_residing_context_only").is(true)
        );

        GroupOperation groupStage = Aggregation.group("context_instance_id", "status")
            .count().as("statusCount")
            .first("context_name").as("contextName");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, MongoConstants.IKASAN_COLLECTION_NAME, Map.class);

        // Process results into ContextInstanceAggregateJobStatus objects
        Map<String, ContextInstanceAggregateJobStatusImpl> aggregateMap = new HashMap<>();

        for (Map<String, Object> result : results.getMappedResults()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> idMap = (Map<String, Object>) result.get("_id");
            String contextInstanceId = (String) idMap.get("contextInstanceId");
            String status = (String) idMap.get("status");
            Integer count = (Integer) result.get("statusCount");
            String contextName = (String) result.get("contextName");

            ContextInstanceAggregateJobStatusImpl aggregate = aggregateMap.computeIfAbsent(
                contextInstanceId,
                id -> new ContextInstanceAggregateJobStatusImpl(id, contextName)
            );

            try {
                InstanceStatus instanceStatus = InstanceStatus.valueOf(status);
                aggregate.addStatusCount(instanceStatus, count);
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown status: {}", status);
            }
        }

        // Check for repeating jobs
        for (String contextInstanceId : contextInstanceIds) {
            if (aggregateMap.containsKey(contextInstanceId)) {
                boolean containsRepeating = doesJobPlanInstanceContainRepeatingJobs(contextInstanceId);
                aggregateMap.get(contextInstanceId).setContainsRepeatableJobs(containsRepeating);
            }
        }

        return new ArrayList<>(aggregateMap.values());
    }

    @Override
    public void deleteSchedulerJobInstances(String contextInstanceId) {
        logger.debug("Deleting all job instances for contextInstanceId: {}", contextInstanceId);
        repository.deleteByContextInstanceId(contextInstanceId);
    }

    /**
     * Build MongoDB query from filter.
     */
    private Query buildQueryFromFilter(SchedulerJobInstanceSearchFilter filter) {
        Query query = new Query();

        if (filter == null) {
            return query;
        }

        if (filter.getJobName() != null && !filter.getJobName().isEmpty()) {
            query.addCriteria(Criteria.where("jobName").regex(filter.getJobName(), "i"));
        }

        if (filter.getDisplayNameFilter() != null && !filter.getDisplayNameFilter().isEmpty()) {
            query.addCriteria(Criteria.where("displayName").regex(filter.getDisplayNameFilter(), "i"));
        }

        if (filter.getJobType() != null && !filter.getJobType().isEmpty()) {
            query.addCriteria(Criteria.where("type").is(filter.getJobType()));
        }

        if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            query.addCriteria(Criteria.where("contextName").is(filter.getContextName()));
        }

        if (filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            query.addCriteria(Criteria.where("contextInstanceId").is(filter.getContextInstanceId()));
        }

        if (filter.getChildContextName() != null && !filter.getChildContextName().isEmpty()) {
            query.addCriteria(Criteria.where("childContextName").is(filter.getChildContextName()));
        }

        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            query.addCriteria(Criteria.where("status").is(filter.getStatus()));
        }

        if (filter.isTargetResidingContextOnly() != null) {
            query.addCriteria(Criteria.where("targetResidingContextOnly").is(filter.isTargetResidingContextOnly()));
        }

        if (filter.isParticipatesInLock() != null) {
            query.addCriteria(Criteria.where("participatesInLock").is(filter.isParticipatesInLock()));
        }

        // Time window filters
        if (filter.getStartTimeWindowStart() > 0 && filter.getStartTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where("startTime")
                .gte(filter.getStartTimeWindowStart())
                .lte(filter.getStartTimeWindowEnd()));
        } else if (filter.getStartTimeWindowStart() > 0) {
            query.addCriteria(Criteria.where("startTime").gte(filter.getStartTimeWindowStart()));
        } else if (filter.getStartTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where("startTime").lte(filter.getStartTimeWindowEnd()));
        }

        if (filter.getEndTimeWindowStart() > 0 && filter.getEndTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where("endTime")
                .gte(filter.getEndTimeWindowStart())
                .lte(filter.getEndTimeWindowEnd()));
        } else if (filter.getEndTimeWindowStart() > 0) {
            query.addCriteria(Criteria.where("endTime").gte(filter.getEndTimeWindowStart()));
        } else if (filter.getEndTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where("endTime").lte(filter.getEndTimeWindowEnd()));
        }

        // Exclude start and terminal jobs if requested
        if (!filter.includeStartAndTerminalJobsInSearchResults()) {
            query.addCriteria(Criteria.where("type").nin("ContextStartJob", "ContextTerminalJob"));
        }

        return query;
    }

    /**
     * Execute query with pagination and sorting.
     */
    private SearchResults<SchedulerJobInstanceRecord> executeQueryWithPagination(Query query,
                                                                                  int limit,
                                                                                  int offset,
                                                                                  String sortField,
                                                                                  String sortDirection) {
        long startTime = System.currentTimeMillis();

        // Count total matching records
        long totalCount = mongoTemplate.count(query, MongoSchedulerJobInstanceRecordImpl.class);

        // Apply sorting
        Sort sort = buildSort(sortField, sortDirection);
        query.with(sort);

        // Apply pagination if limit is specified
        if (limit > 0) {
            int page = offset > 0 ? offset / limit : 0;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        }

        // Execute query
        List<MongoSchedulerJobInstanceRecordImpl> results = mongoTemplate.find(query, MongoSchedulerJobInstanceRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;

        logger.debug("Found {} job instances out of {} total in {}ms", results.size(), totalCount, queryTime);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    /**
     * Build Sort object from field name and direction.
     */
    private Sort buildSort(String sortField, String sortDirection) {
        if (sortField == null || sortField.isEmpty()) {
            sortField = "modifiedTimestamp";
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if ("ASC".equalsIgnoreCase(sortDirection) || "asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        return Sort.by(direction, sortField);
    }

    /**
     * Implementation of ContextInstanceAggregateJobStatus for aggregation results.
     */
    private static class ContextInstanceAggregateJobStatusImpl implements ContextInstanceAggregateJobStatus {
        private final String contextInstanceId;
        private final String contextInstanceName;
        private final Map<InstanceStatus, Integer> statusCounts = new HashMap<>();
        private final Map<String, Integer> repeatingJobsStatusCounts = new HashMap<>();
        private boolean containsRepeatableJobs;

        public ContextInstanceAggregateJobStatusImpl(String contextInstanceId, String contextInstanceName) {
            this.contextInstanceId = contextInstanceId;
            this.contextInstanceName = contextInstanceName;
        }

        public void addStatusCount(InstanceStatus status, int count) {
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + count);
        }

        @Override
        public String getContextInstanceId() {
            return contextInstanceId;
        }

        @Override
        public String getContextInstanceName() {
            return contextInstanceName;
        }

        @Override
        public int getStatusCount(InstanceStatus instanceStatus) {
            return statusCounts.getOrDefault(instanceStatus, 0);
        }

        @Override
        public boolean containsRepeatableJobs() {
            return containsRepeatableJobs;
        }

        @Override
        public void setContainsRepeatableJobs(boolean containsRepeatableJobs) {
            this.containsRepeatableJobs = containsRepeatableJobs;
        }

        @Override
        public int repeatingJobInstanceStatusCount(InstanceStatus instanceStatus) {
            String statusKey = instanceStatus.name();
            return repeatingJobsStatusCounts.getOrDefault(statusKey, 0);
        }

        @Override
        public void setRepeatingJobsStatusCounts(Map<String, Integer> repeatingJobsStatusCounts) {
            this.repeatingJobsStatusCounts.clear();
            if (repeatingJobsStatusCounts != null) {
                this.repeatingJobsStatusCounts.putAll(repeatingJobsStatusCounts);
            }
        }
    }
}
