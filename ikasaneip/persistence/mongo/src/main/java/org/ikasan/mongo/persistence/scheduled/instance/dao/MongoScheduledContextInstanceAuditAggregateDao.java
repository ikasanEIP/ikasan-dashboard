package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceAuditAggregateRepository;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;

/**
 * MongoDB implementation of ScheduledContextInstanceAuditAggregateDao.
 *
 * @author Ikasan Development Team
 */
public class MongoScheduledContextInstanceAuditAggregateDao implements ScheduledContextInstanceAuditAggregateDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextInstanceAuditAggregateDao.class);

    private final MongoScheduledContextInstanceAuditAggregateRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template
     */
    public MongoScheduledContextInstanceAuditAggregateDao(
            MongoScheduledContextInstanceAuditAggregateRepository repository,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
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

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Filter by context name (wildcard)
        if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            criteriaList.add(Criteria.where("contextName").regex(".*" + filter.getContextName() + ".*", "i"));
        }

        // Filter by context instance ID (wildcard)
        if (filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            criteriaList.add(Criteria.where("contextInstanceId").regex(".*" + filter.getContextInstanceId() + ".*", "i"));
        }

        // Filter by scheduled process event name (wildcard, case insensitive)
        if (filter.getScheduledProcessEventName() != null && !filter.getScheduledProcessEventName().isEmpty()) {
            criteriaList.add(Criteria.where("scheduledProcessEventName")
                    .regex(".*" + filter.getScheduledProcessEventName().toLowerCase() + ".*", "i"));
        }

        // Filter by status (wildcard)
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            criteriaList.add(Criteria.where("status").regex(".*" + filter.getStatus() + ".*", "i"));
        }

        // Filter by raised initiation event name (search in raisedEvents field)
        if (filter.getRaisedInitiationEventName() != null && !filter.getRaisedInitiationEventName().isEmpty()) {
            criteriaList.add(Criteria.where("raisedEvents")
                    .regex(".*" + filter.getRaisedInitiationEventName().toLowerCase() + ".*", "i"));
        }

        // Apply all criteria
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Apply sorting
        if (sortField != null && !sortField.isEmpty()) {
            Sort.Direction direction = "ASCENDING".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortField));
        } else {
            // Default sort by created date time descending
            query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        }

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoScheduledContextInstanceAuditAggregateRecordImpl.class);

        // Apply pagination
        if (limit > 0 && offset >= 0) {
            int page = offset / limit;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        } else if (offset > 0) {
            query.skip(offset);
        }

        // Execute query
        List<MongoScheduledContextInstanceAuditAggregateRecordImpl> results =
                mongoTemplate.find(query, MongoScheduledContextInstanceAuditAggregateRecordImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled context instance audit aggregate records (total: {})",
                results.size(), totalCount);

        return createSearchResults(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public Map<String, Map<String, Integer>> getRepeatingJobStatusCounts(List<String> contextInstanceIds) {
        logger.debug("Getting repeating job status counts for context instance IDs: {}", contextInstanceIds);

        Map<String, Map<String, Integer>> results = new HashMap<>();

        for (String contextInstanceId : contextInstanceIds) {
            Query query = new Query();
            query.addCriteria(Criteria.where("contextInstanceId").is(contextInstanceId));
            query.addCriteria(Criteria.where("isRepeatingJob").is(true));

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

        if (record instanceof MongoScheduledContextInstanceAuditAggregateRecordImpl) {
            return (MongoScheduledContextInstanceAuditAggregateRecordImpl) record;
        }

        MongoScheduledContextInstanceAuditAggregateRecordImpl entity =
                new MongoScheduledContextInstanceAuditAggregateRecordImpl();

        entity.setId(record.getId());
        entity.setContextName(record.getContextName());
        entity.setContextInstanceId(record.getContextInstanceId());
        entity.setScheduledProcessEventName(record.getScheduledProcessEventName());
        entity.setScheduledContextInstanceAuditAggregate(record.getScheduledContextInstanceAuditAggregate());
        entity.setStatus(record.getStatus());
        entity.setRepeatingJob(record.isRepeatingJob());
        entity.setJobType(record.getJobType());
        entity.setTimestamp(record.getTimestamp());

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

        return new SearchResults<ScheduledContextInstanceAuditAggregateRecord>() {
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
