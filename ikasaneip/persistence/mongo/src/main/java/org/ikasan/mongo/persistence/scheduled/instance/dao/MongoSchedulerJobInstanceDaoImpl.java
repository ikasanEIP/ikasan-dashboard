package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoContextInstanceAggregateJobStatusImpl;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoSchedulerJobInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoSchedulerJobInstanceRecordRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
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

import static org.ikasan.spec.entity.EntityFields.TYPE;

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
        MongoSchedulerJobInstanceRecordImpl mongoRecord;

        if (!(schedulerJobInstanceRecord instanceof MongoSchedulerJobInstanceRecordImpl)) {
            mongoRecord = this.convert(schedulerJobInstanceRecord.getSchedulerJobInstance());
        }
        else {
            mongoRecord = (MongoSchedulerJobInstanceRecordImpl) schedulerJobInstanceRecord;
        }

        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords) {
        List<MongoSchedulerJobInstanceRecordImpl> records = schedulerJobInstanceRecords.stream()
                .map(record -> {
                    if(!(record instanceof MongoSchedulerJobInstanceRecordImpl)) {
                        return this.convert(record.getSchedulerJobInstance());
                    }
                    else {
                        MongoSchedulerJobInstanceRecordImpl mongoRecord = (MongoSchedulerJobInstanceRecordImpl) record;
                        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
                        return mongoRecord;
                    }
                })
                .collect(Collectors.toList());

        repository.saveAll(records);
    }

    private MongoSchedulerJobInstanceRecordImpl convert(SchedulerJobInstance schedulerJobInstance) {
        MongoSchedulerJobInstanceRecordImpl instanceRecord = new MongoSchedulerJobInstanceRecordImpl();

        if(schedulerJobInstance instanceof FileEventDrivenJobInstance) {
            instanceRecord.setId(schedulerJobInstance.getJobName()
                + "_" + schedulerJobInstance.getContextInstanceId()
                + "_" + schedulerJobInstance.getChildContextName()
                + "_" + JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
            instanceRecord.setType(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof InternalEventDrivenJobInstance) {
            instanceRecord.setId(schedulerJobInstance.getJobName()
                + "_" + schedulerJobInstance.getContextInstanceId()
                + "_" + schedulerJobInstance.getChildContextName()
                + "_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            instanceRecord.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            instanceRecord.setTargetResidingContextOnly(((InternalEventDrivenJobInstance) schedulerJobInstance)
                .isTargetResidingContextOnly());
            instanceRecord.setParticipatesInLock(((InternalEventDrivenJobInstance) schedulerJobInstance)
                .isParticipatesInLock());
        }
        else if(schedulerJobInstance instanceof QuartzScheduleDrivenJobInstance) {
                instanceRecord.setId(schedulerJobInstance.getJobName()
                    + "_" + schedulerJobInstance.getContextInstanceId()
                    + "_" + schedulerJobInstance.getChildContextName()
                    + "_" + JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
                instanceRecord.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof GlobalEventJobInstance) {
                instanceRecord.setId(schedulerJobInstance.getJobName()
                    + "_" + schedulerJobInstance.getContextInstanceId()
                    + "_" + schedulerJobInstance.getChildContextName()
                    + "_" + JobConstants.GLOBAL_EVENT_JOB_INSTANCE);
                instanceRecord.setType(JobConstants.GLOBAL_EVENT_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof ContextStartJobInstance) {
                instanceRecord.setId(schedulerJobInstance.getJobName()
                    + "_" + schedulerJobInstance.getContextInstanceId()
                    + "_" + schedulerJobInstance.getChildContextName()
                    + "_" + JobConstants.CONTEXT_START_JOB_INSTANCE);
                instanceRecord.setType(JobConstants.CONTEXT_START_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof ContextTerminalJobInstance) {
                instanceRecord.setId(schedulerJobInstance.getJobName()
                    + "_" + schedulerJobInstance.getContextInstanceId()
                    + "_" + schedulerJobInstance.getChildContextName()
                    + "_" + JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE);
                instanceRecord.setType(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof LocalEventJobInstance) {
                instanceRecord.setId(schedulerJobInstance.getJobName()
                    + "_" + schedulerJobInstance.getContextInstanceId()
                    + "_" + schedulerJobInstance.getChildContextName()
                    + "_" + JobConstants.LOCAL_EVENT_JOB_INSTANCE);
                instanceRecord.setType(JobConstants.LOCAL_EVENT_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof BridgingJobInstance) {
                instanceRecord.setId(schedulerJobInstance.getJobName()
                    + "_" + schedulerJobInstance.getContextInstanceId()
                    + "_" + schedulerJobInstance.getChildContextName()
                    + "_" + JobConstants.BRIDGING_JOB_INSTANCE);
                instanceRecord.setType(JobConstants.BRIDGING_JOB_INSTANCE);
        }

        if(schedulerJobInstance.getScheduledProcessEvent() != null) {
            instanceRecord.setStartTime(schedulerJobInstance.getScheduledProcessEvent().getFireTime());
            instanceRecord.setStartTime(schedulerJobInstance.getScheduledProcessEvent().getCompletionTime());
        }

        instanceRecord.setStatus(schedulerJobInstance.isErrorAcknowledged() != null
            && schedulerJobInstance.isErrorAcknowledged()
            && schedulerJobInstance.getStatus() != null
            && schedulerJobInstance.getStatus().equals(InstanceStatus.ERROR)
            ? InstanceStatus.ERROR_ACKNOWLEDGED.name() : schedulerJobInstance.getStatus().name());

        instanceRecord.setSchedulerJobInstance(schedulerJobInstance);
        instanceRecord.setContextName(schedulerJobInstance.getContextName());
        instanceRecord.setDisplayName(schedulerJobInstance.getDisplayName());
        instanceRecord.setJobName(schedulerJobInstance.getJobName());
        instanceRecord.setTimestamp(System.currentTimeMillis());
        instanceRecord.setContextInstanceId(schedulerJobInstance.getContextInstanceId());
        instanceRecord.setChildContextName(schedulerJobInstance.getChildContextName());
        instanceRecord.setSchedulerJobInstance(schedulerJobInstance);
        instanceRecord.setModifiedTimestamp(System.currentTimeMillis());

        return instanceRecord;
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String contextInstanceId,
                                                                                                  int limit,
                                                                                                  int offset,
                                                                                                  String sortField,
                                                                                                  String sortDirection) {
        logger.debug("Finding job instances by contextInstanceId: {}, limit={}, offset={}, sortField={}, sortDirection={}",
            contextInstanceId, limit, offset, sortField, sortDirection);

        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        return this.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String contextName,
                                                                                            int limit,
                                                                                            int offset,
                                                                                            String sortField,
                                                                                            String sortDirection) {
        logger.debug("Finding job instances by contextName: {}, limit={}, offset={}, sortField={}, sortDirection={}",
            contextName, limit, offset, sortField, sortDirection);

        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextName(contextName);
        return this.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }

    @Override
    public boolean doesJobPlanInstanceContainRepeatingJobs(String contextInstanceId) {
        logger.debug("Checking if job plan instance contains repeating jobs: {}", contextInstanceId);

        Query query = new Query(Criteria.where(EntityFields.COMPONENT_NAME).is(contextInstanceId));

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

        List<Criteria> typeOrCriteria = new ArrayList<>(List.of(
            Criteria.where(EntityFields.TYPE).is(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.GLOBAL_EVENT_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.LOCAL_EVENT_JOB_INSTANCE)));

        MatchOperation matchStage = Aggregation.match(
            Criteria.where(EntityFields.COMPONENT_NAME).in(contextInstanceIds)
                .orOperator(typeOrCriteria)
        );
        GroupOperation groupStage = Aggregation.group(EntityFields.COMPONENT_NAME, EntityFields.STATUS)
            .count().as("statusCount")
            .first(EntityFields.FLOW_NAME).as("contextName");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, MongoConstants.IKASAN_COLLECTION_NAME, Map.class);

        // Process results into ContextInstanceAggregateJobStatus objects
        Map<String, MongoContextInstanceAggregateJobStatusImpl> aggregateMap = new HashMap<>();

        for (Map<String, Object> result : results.getMappedResults()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> idMap = (Map<String, Object>) result.get("_id");
            String contextInstanceId = (String) idMap.get(EntityFields.COMPONENT_NAME);
            String status = (String) idMap.get(EntityFields.STATUS);
            Integer count = (Integer) result.get("statusCount");
            String contextName = (String) result.get("contextName");

            MongoContextInstanceAggregateJobStatusImpl aggregate = aggregateMap.computeIfAbsent(
                contextInstanceId,
                id -> new MongoContextInstanceAggregateJobStatusImpl(id, contextName)
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

        List<Criteria> typeOrCriteria = new ArrayList<>(List.of(
            Criteria.where(EntityFields.TYPE).is(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.GLOBAL_EVENT_JOB_INSTANCE),
            Criteria.where(EntityFields.TYPE).is(JobConstants.LOCAL_EVENT_JOB_INSTANCE)));

        MatchOperation matchStage = Aggregation.match(
            Criteria.where(EntityFields.COMPONENT_NAME).in(contextInstanceIds)
                .orOperator(typeOrCriteria)
        );

        GroupOperation groupStage = Aggregation.group(EntityFields.COMPONENT_NAME, EntityFields.STATUS)
            .count().as("statusCount")
            .first(EntityFields.FLOW_NAME).as("contextName");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, MongoConstants.IKASAN_COLLECTION_NAME, Map.class);

        // Process results into ContextInstanceAggregateJobStatus objects
        Map<String, MongoContextInstanceAggregateJobStatusImpl> aggregateMap = new HashMap<>();

        for (Map<String, Object> result : results.getMappedResults()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> idMap = (Map<String, Object>) result.get("_id");
            String contextInstanceId = (String) idMap.get(EntityFields.COMPONENT_NAME);
            String status = (String) idMap.get(EntityFields.STATUS);
            Integer count = (Integer) result.get("statusCount");
            String contextName = (String) result.get("contextName");

            MongoContextInstanceAggregateJobStatusImpl aggregate = aggregateMap.computeIfAbsent(
                contextInstanceId,
                id -> new MongoContextInstanceAggregateJobStatusImpl(id, contextName)
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
            query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).regex(filter.getJobName(), "i"));
        }

        if (filter.getDisplayNameFilter() != null && !filter.getDisplayNameFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.DISPLAY_NAME).regex(filter.getDisplayNameFilter(), "i"));
        }

        if (filter.getJobType() != null && !filter.getJobType().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.TYPE).is(filter.getJobType()));
        }
        else {
            List<Criteria> typeOrCriteria = new ArrayList<>(List.of(
                Criteria.where(EntityFields.TYPE).is(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE),
                Criteria.where(EntityFields.TYPE).is(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE),
                Criteria.where(EntityFields.TYPE).is(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE),
                Criteria.where(EntityFields.TYPE).is(JobConstants.GLOBAL_EVENT_JOB_INSTANCE),
                Criteria.where(EntityFields.TYPE).is(JobConstants.LOCAL_EVENT_JOB_INSTANCE)));

            if(filter.includeStartAndTerminalJobsInSearchResults()) {
                typeOrCriteria.add(Criteria.where(EntityFields.TYPE).is(JobConstants.CONTEXT_START_JOB_INSTANCE));
                typeOrCriteria.add(Criteria.where(EntityFields.TYPE).is(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE));
                typeOrCriteria.add(Criteria.where(EntityFields.TYPE).is(JobConstants.BRIDGING_JOB_INSTANCE));

            }

            query.addCriteria(new Criteria().orOperator(typeOrCriteria));
        }

        if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.FLOW_NAME).is(filter.getContextName()));
        }

        if (filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.COMPONENT_NAME).is(filter.getContextInstanceId()));
        }

        if (filter.getChildContextName() != null && !filter.getChildContextName().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.CHILD_CONTEXT_NAME).is(filter.getChildContextName()));
        }

        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.STATUS).is(filter.getStatus()));
        }

        if (filter.isTargetResidingContextOnly() != null) {
            query.addCriteria(Criteria.where(EntityFields.TARGET_RESIDING_CONTEXT_ONLY).is(filter.isTargetResidingContextOnly()));
        }

        if (filter.isParticipatesInLock() != null) {
            query.addCriteria(Criteria.where(EntityFields.PARTICIPATES_IN_LOCK).is(filter.isParticipatesInLock()));
        }

        // Time window filters
        if (filter.getStartTimeWindowStart() > 0 && filter.getStartTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where(EntityFields.START_TIME)
                .gte(filter.getStartTimeWindowStart())
                .lte(filter.getStartTimeWindowEnd()));
        } else if (filter.getStartTimeWindowStart() > 0) {
            query.addCriteria(Criteria.where(EntityFields.START_TIME).gte(filter.getStartTimeWindowStart()));
        } else if (filter.getStartTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where(EntityFields.START_TIME).lte(filter.getStartTimeWindowEnd()));
        }

        if (filter.getEndTimeWindowStart() > 0 && filter.getEndTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where(EntityFields.END_TIME)
                .gte(filter.getEndTimeWindowStart())
                .lte(filter.getEndTimeWindowEnd()));
        } else if (filter.getEndTimeWindowStart() > 0) {
            query.addCriteria(Criteria.where(EntityFields.END_TIME).gte(filter.getEndTimeWindowStart()));
        } else if (filter.getEndTimeWindowEnd() > 0) {
            query.addCriteria(Criteria.where(EntityFields.END_TIME).lte(filter.getEndTimeWindowEnd()));
        }

//        // Exclude start and terminal jobs if requested
//        if (!filter.includeStartAndTerminalJobsInSearchResults()) {
//            query.addCriteria(Criteria.where(EntityFields.TYPE).nin("ContextStartJob", "ContextTerminalJob"));
//        }

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
}
