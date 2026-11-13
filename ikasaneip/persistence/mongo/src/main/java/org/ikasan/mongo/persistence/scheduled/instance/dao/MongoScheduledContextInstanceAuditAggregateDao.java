package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.mongo.persistence.general.dao.AtlasSearchFilter;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceAuditAggregateRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB implementation of ScheduledContextInstanceAuditAggregateDao.
 *
 * @author Ikasan Development Team
 */
public class MongoScheduledContextInstanceAuditAggregateDao implements ScheduledContextInstanceAuditAggregateDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextInstanceAuditAggregateDao.class);

    private final MongoScheduledContextInstanceAuditAggregateRepository repository;
    private final MongoTemplate mongoTemplate;
    private final long daysToKeep;
    private final AtlasSearchQueryHelper atlasSearchQueryHelper;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template
     * @param daysToKeep the number of days to keep records
     */
    public MongoScheduledContextInstanceAuditAggregateDao(
            MongoScheduledContextInstanceAuditAggregateRepository repository,
            MongoTemplate mongoTemplate, long daysToKeep) {
        this(repository, mongoTemplate, daysToKeep, null);
    }

    /**
     * Constructor with AtlasSearchQueryHelper
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template
     * @param daysToKeep the number of days to keep records
     * @param atlasSearchQueryHelper the Atlas Search query helper
     */
    public MongoScheduledContextInstanceAuditAggregateDao(
            MongoScheduledContextInstanceAuditAggregateRepository repository,
            MongoTemplate mongoTemplate, long daysToKeep,
            AtlasSearchQueryHelper atlasSearchQueryHelper) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
        this.atlasSearchQueryHelper = atlasSearchQueryHelper != null
            ? atlasSearchQueryHelper : new AtlasSearchQueryHelper(mongoTemplate, null, false);
    }

    @Override
    public void save(ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord) {
        MongoScheduledContextInstanceAuditAggregateRecordImpl entity = convertToEntity(scheduledContextInstanceAuditAggregateRecord);

        // Generate ID if not set
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }

        // Set timestamp if not set
        if (entity.getTimestamp() == 0) {
            entity.setTimestamp(System.currentTimeMillis());
        }

        repository.save(entity);
        logger.debug("Saved scheduled context instance audit aggregate record with id: {}", entity.getId());
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAll(
            int limit, int offset, String sortField, String sortDirection) {
        return this.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                new ScheduledContextInstanceAuditAggregateSearchFilter() {
                    @Override public String getContextName() { return null; }
                    @Override public void setContextName(String contextName) {}
                    @Override public String getContextInstanceId() { return null; }
                    @Override public void setContextInstanceId(String contextInstanceId) {}
                    @Override public String getScheduledProcessEventName() { return null; }
                    @Override public void setScheduledProcessEventName(String scheduledProcessEventName) {}
                    @Override public String getStatus() { return null; }
                    @Override public void setStatus(String status) {}
                    @Override public String getRaisedInitiationEventName() { return null; }
                    @Override public void setRaisedInitiationEventName(String raisedInitiationEventName) {}
                },
                limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findScheduledContextInstanceAuditAggregateRecordsByFilter(
            ScheduledContextInstanceAuditAggregateSearchFilter filter, int limit, int offset,
            String sortField, String sortDirection) {

        logger.debug("Finding scheduled context instance audit aggregate records by filter: {}, limit: {}, offset: {}",
                filter, limit, offset);

        long queryStartTime = System.currentTimeMillis();

        // Build sort
        Sort sort;
        if (sortField != null && !sortField.isEmpty()) {
            Sort.Direction direction = "ASCENDING".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            sort = Sort.by(direction, sortField);
        } else {
            // Default sort by created date time descending
            sort = Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME);
        }

        // All fields are case-insensitive substring matches (AND'ed) evaluated inside the Atlas $search stage
        AtlasSearchFilter searchFilter = AtlasSearchFilter.builder()
                .equals(EntityFields.TYPE, SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE)
                .contains(EntityFields.MODULE_NAME, filter.getContextName())
                .contains(EntityFields.FLOW_NAME, filter.getContextInstanceId())
                .contains(EntityFields.COMPONENT_NAME, filter.getScheduledProcessEventName())
                .contains(EntityFields.STATUS, filter.getStatus())
                .contains(EntityFields.EVENT, filter.getRaisedInitiationEventName())
                .build();

        AtlasSearchQueryHelper.SearchQueryResult<MongoScheduledContextInstanceAuditAggregateRecordImpl> result =
                atlasSearchQueryHelper.search(searchFilter, sort, Math.max(offset, 0), limit,
                        MongoScheduledContextInstanceAuditAggregateRecordImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled context instance audit aggregate records (total: {})",
                result.getResults().size(), result.getTotalCount());

        return createSearchResults(new ArrayList<>(result.getResults()), result.getTotalCount(), queryTime);
    }

    @Override
    public Map<String, Map<String, Integer>> getRepeatingJobStatusCounts(List<String> contextInstanceIds) {
        logger.debug("Getting repeating job status counts for context instance IDs: {}", contextInstanceIds);

        Map<String, Map<String, Integer>> results = new HashMap<>();

        for (String contextInstanceId : contextInstanceIds) {
            Query query = new Query();
            query.addCriteria(Criteria.where(EntityFields.FLOW_NAME).is(contextInstanceId));
            query.addCriteria(Criteria.where(EntityFields.IS_REPEATING_JOB).is(true));
            query.addCriteria(Criteria.where(EntityFields.TYPE).is(SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE));

            List<MongoScheduledContextInstanceAuditAggregateRecordImpl> records =
                    mongoTemplate.find(query, MongoScheduledContextInstanceAuditAggregateRecordImpl.class);

            // Count by status
            Map<String, Integer> statusCounts = new HashMap<>();
            for (MongoScheduledContextInstanceAuditAggregateRecordImpl record : records) {
                String status = record.getStatus();
                if (status != null) {
                    statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
                }
            }

            results.put(contextInstanceId, statusCounts);
        }

        logger.debug("Repeating job status counts: {}", results);
        return results;
    }

    /**
     * Convert to MongoDB entity
     *
     * @param record the record to convert
     * @return the entity ready for persistence
     */
    private MongoScheduledContextInstanceAuditAggregateRecordImpl convertToEntity(
            ScheduledContextInstanceAuditAggregateRecord record) {
        MongoScheduledContextInstanceAuditAggregateRecordImpl entity =
                new MongoScheduledContextInstanceAuditAggregateRecordImpl();

        if(record.getId() != null) {
            entity.setId(record.getId());
        }
        else {
            entity.setId(SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_ID + "_" + UUID.randomUUID());
        }
        entity.setScheduledContextInstanceAuditAggregate(record.getScheduledContextInstanceAuditAggregate());
        entity.setType(SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE);
        entity.setContextInstanceId(record.getContextInstanceId());
        entity.setContextName(record.getContextName());
        if(record.getScheduledProcessEventName() != null) {
            entity.setScheduledProcessEventName(record.getScheduledProcessEventName());
        }
        entity.setTimestamp(System.currentTimeMillis());

        if(record.getScheduledContextInstanceAuditAggregate().getSchedulerJobInitiationEvents() != null &&
            !record.getScheduledContextInstanceAuditAggregate().getSchedulerJobInitiationEvents().isEmpty()) {
            StringBuffer eventsBuffer = new StringBuffer();
            record.getScheduledContextInstanceAuditAggregate()
                .getSchedulerJobInitiationEvents().forEach(event -> eventsBuffer.append(event.getJobName()).append(" "));

            entity.setRaisedEvents(eventsBuffer.toString().toLowerCase());
        }

        entity.setRepeatingJob(record.isRepeatingJob());
        entity.setJobType(record.getJobType());
        entity.setStatus(record.getStatus());
        entity.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());

        return entity;
    }

    /**
     * Create SearchResults wrapper
     *
     * @param results the result list
     * @param totalCount the total count
     * @param queryTime the query time
     * @return SearchResults instance
     */
    private SearchResults<ScheduledContextInstanceAuditAggregateRecord> createSearchResults(
            List<ScheduledContextInstanceAuditAggregateRecord> results, long totalCount, long queryTime) {

        return new SearchResults<>() {
            @Override
            public List<ScheduledContextInstanceAuditAggregateRecord> getResultList() {
                return results;
            }

            @Override
            public long getTotalNumberOfResults() {
                return totalCount;
            }

            @Override
            public long getQueryResponseTime() {
                return queryTime;
            }
        };
    }
}
