package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchFilter;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
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

    /**
     * Every job instance document type. All entity types share the collection, so deletes must be scoped to these.
     */
    private static final List<String> ALL_JOB_INSTANCE_TYPES = List.of(
        JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE,
        JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE,
        JobConstants.GLOBAL_EVENT_JOB_INSTANCE,
        JobConstants.CONTEXT_START_JOB_INSTANCE,
        JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE,
        JobConstants.LOCAL_EVENT_JOB_INSTANCE,
        JobConstants.BRIDGING_JOB_INSTANCE
    );

    private final MongoSchedulerJobInstanceRecordRepository repository;
    private final MongoTemplate mongoTemplate;
    private final AtlasSearchQueryHelper atlasSearchQueryHelper;

    public MongoSchedulerJobInstanceDaoImpl(MongoSchedulerJobInstanceRecordRepository repository,
                                            MongoTemplate mongoTemplate) {
        this(repository, mongoTemplate, null);
    }

    public MongoSchedulerJobInstanceDaoImpl(MongoSchedulerJobInstanceRecordRepository repository,
                                            MongoTemplate mongoTemplate,
                                            AtlasSearchQueryHelper atlasSearchQueryHelper) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        if (mongoTemplate == null) {
            throw new IllegalArgumentException("mongoTemplate cannot be null");
        }
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.atlasSearchQueryHelper = atlasSearchQueryHelper != null
            ? atlasSearchQueryHelper : new AtlasSearchQueryHelper(mongoTemplate, null, false);
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

        long startTime = System.currentTimeMillis();

        AtlasSearchQueryHelper.SearchQueryResult<MongoSchedulerJobInstanceRecordImpl> result =
            atlasSearchQueryHelper.search(buildSearchFilter(filter), buildSort(sortField, sortDirection),
                Math.max(offset, 0), limit, MongoSchedulerJobInstanceRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;

        logger.debug("Found {} job instances out of {} total in {}ms",
            result.getResults().size(), result.getTotalCount(), queryTime);

        return new SearchResultsImpl<>(new ArrayList<>(result.getResults()), result.getTotalCount(), queryTime);
    }

    /**
     * Build the search filter. Job name and display name are case-insensitive substring matches (AND'ed); all other
     * fields are exact restrictions. Everything is evaluated inside the Atlas {@code $search} stage when available.
     */
    private AtlasSearchFilter buildSearchFilter(SchedulerJobInstanceSearchFilter filter) {
        AtlasSearchFilter.Builder builder = AtlasSearchFilter.builder();
        if (filter == null) {
            return builder.build();
        }

        if (filter.getJobType() != null && !filter.getJobType().isEmpty()) {
            builder.equals(EntityFields.TYPE, filter.getJobType());
        } else {
            List<String> types = new ArrayList<>(List.of(
                JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE,
                JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE,
                JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE,
                JobConstants.GLOBAL_EVENT_JOB_INSTANCE,
                JobConstants.LOCAL_EVENT_JOB_INSTANCE));
            if (filter.includeStartAndTerminalJobsInSearchResults()) {
                types.add(JobConstants.CONTEXT_START_JOB_INSTANCE);
                types.add(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE);
                types.add(JobConstants.BRIDGING_JOB_INSTANCE);
            }
            builder.in(EntityFields.TYPE, types);
        }

        return builder
            .contains(EntityFields.MODULE_NAME, filter.getJobName())
            .contains(EntityFields.DISPLAY_NAME, filter.getDisplayNameFilter())
            .equals(EntityFields.FLOW_NAME, filter.getContextName())
            .equals(EntityFields.COMPONENT_NAME, filter.getContextInstanceId())
            .equals(EntityFields.CHILD_CONTEXT_NAME, filter.getChildContextName())
            .equals(EntityFields.STATUS, filter.getStatus())
            .equals(EntityFields.TARGET_RESIDING_CONTEXT_ONLY, filter.isTargetResidingContextOnly())
            .equals(EntityFields.PARTICIPATES_IN_LOCK, filter.isParticipatesInLock())
            .rangeMillis(EntityFields.START_TIME, filter.getStartTimeWindowStart(), filter.getStartTimeWindowEnd())
            .rangeMillis(EntityFields.END_TIME, filter.getEndTimeWindowStart(), filter.getEndTimeWindowEnd())
            .build();
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
        mongoTemplate.remove(new Query(Criteria.where(EntityFields.TYPE).in(ALL_JOB_INSTANCE_TYPES)
            .and(EntityFields.COMPONENT_NAME).is(contextInstanceId)), MongoSchedulerJobInstanceRecordImpl.class);
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
