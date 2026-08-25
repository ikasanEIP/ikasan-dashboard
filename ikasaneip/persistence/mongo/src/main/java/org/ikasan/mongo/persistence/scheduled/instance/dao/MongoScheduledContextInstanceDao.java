package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * MongoDB implementation of ScheduledContextInstanceDao.
 *
 * @author Ikasan Development Team
 */
public class MongoScheduledContextInstanceDao implements ScheduledContextInstanceDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextInstanceDao.class);

    private final MongoScheduledContextInstanceRepository repository;
    private final MongoTemplate mongoTemplate;
    private final long daysToKeep;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template
     */
    public MongoScheduledContextInstanceDao(MongoScheduledContextInstanceRepository repository,
                                            MongoTemplate mongoTemplate, long daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
    }

    @Override
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        MongoScheduledContextInstanceRecordImpl entity = convertToEntity(scheduledContextInstanceRecord);
        repository.save(entity);
        logger.debug("Saved scheduled context instance record with id: {}", entity.getId());
    }

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        logger.debug("Finding scheduled context instance record by id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(SCHEDULED_CONTEXT_INSTANCE_TYPE));

        return mongoTemplate.findOne(query, MongoScheduledContextInstanceRecordImpl.class);
    }

    @Override
    public void deleteById(String id) {
        logger.debug("Deleting scheduled context instance record by id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(SCHEDULED_CONTEXT_INSTANCE_TYPE));

        mongoTemplate.remove(query, MongoScheduledContextInstanceRecordImpl.class);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(
            List<InstanceStatus> instanceStatuses) {
        return this.getScheduledContextInstancesByStatus(instanceStatuses, -1, -1);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(
            List<InstanceStatus> instanceStatuses, int limit, int offset) {

        logger.debug("Getting scheduled context instances by status: {}, limit: {}, offset: {}",
                instanceStatuses, limit, offset);

        long queryStartTime = System.currentTimeMillis();

        List<String> statusStrings = instanceStatuses.stream()
                .map(Enum::toString)
                .collect(Collectors.toList());

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.STATUS).in(statusStrings));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(SCHEDULED_CONTEXT_INSTANCE_TYPE));

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoScheduledContextInstanceRecordImpl.class);

        // Apply pagination if specified
        if (limit > 0 && offset >= 0) {
            int page = offset / limit;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        } else if (offset > 0) {
            query.skip(offset);
        }

        // Execute query
        List<MongoScheduledContextInstanceRecordImpl> results =
                mongoTemplate.find(query, MongoScheduledContextInstanceRecordImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled context instances (total: {})", results.size(), totalCount);

        return createSearchResults(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(
            String contextName, int limit, int offset, String sortField, String sortDirection) {

        logger.debug("Getting scheduled context instances by contextName: {}, limit: {}, offset: {}, sortField: {}, sortDirection: {}",
                contextName, limit, offset, sortField, sortDirection);

        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(contextName));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(SCHEDULED_CONTEXT_INSTANCE_TYPE));

        // Apply sorting
        if (sortField != null && !sortField.isEmpty()) {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortField));
        }

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoScheduledContextInstanceRecordImpl.class);

        // Apply pagination if specified
        if (limit > 0 && offset >= 0) {
            int page = offset / limit;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        } else if (offset > 0) {
            query.skip(offset);
        }

        // Execute query
        List<MongoScheduledContextInstanceRecordImpl> results =
                mongoTemplate.find(query, MongoScheduledContextInstanceRecordImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled context instances (total: {})", results.size(), totalCount);

        return createSearchResults(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(
            String contextName, long startTimestamp, long endTimestamp,
            int limit, int offset, String sortField, String sortDirection) {

        logger.debug("Getting scheduled context instances by contextName: {}, startTimestamp: {}, endTimestamp: {}, limit: {}, offset: {}",
                contextName, startTimestamp, endTimestamp, limit, offset);

        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where(EntityFields.TYPE).is(SCHEDULED_CONTEXT_INSTANCE_TYPE));
        criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).is(contextName));

        if (startTimestamp > 0 || endTimestamp > 0) {
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(startTimestamp).lte(endTimestamp));
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));

        // Apply sorting
        if (sortField != null && !sortField.isEmpty()) {
            Sort.Direction direction = "ASCENDING".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortField));
        }

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoScheduledContextInstanceRecordImpl.class);

        // Apply pagination if specified
        if (limit > 0 && offset >= 0) {
            int page = offset / limit;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        } else if (offset > 0) {
            query.skip(offset);
        }

        // Execute query
        List<MongoScheduledContextInstanceRecordImpl> results =
                mongoTemplate.find(query, MongoScheduledContextInstanceRecordImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled context instances (total: {})", results.size(), totalCount);

        return createSearchResults(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByFilter(
            ContextInstanceSearchFilter filter, int limit, int offset,
            String sortField, String sortDirection) {

        logger.debug("Getting scheduled context instances by filter: {}, limit: {}, offset: {}",
                filter, limit, offset);

        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Always add type filter
        criteriaList.add(Criteria.where(EntityFields.TYPE).is(SCHEDULED_CONTEXT_INSTANCE_TYPE));

        // Filter by context instance names
        if (filter.getContextInstanceNames() != null && !filter.getContextInstanceNames().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).in(filter.getContextInstanceNames()));
        }

        // Filter by context search term (wildcard)
        if (filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.MODULE_NAME)
                    .regex(".*" + filter.getContextSearchFilter() + ".*", "i"));
        }

        // Filter by context instance ID (wildcard)
        if (filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.COMPONENT_NAME)
                    .regex(".*" + filter.getContextInstanceId() + ".*", "i"));
        }

        // Filter by created timestamp (day range)
        if (filter.getCreatedTimestamp() > 0) {
            long startOfDay = atStartOfDay(new Date(filter.getCreatedTimestamp()));
            long endOfDay = atEndOfDay(new Date(filter.getCreatedTimestamp()));
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(startOfDay).lte(endOfDay));
        }

        // Filter by start time range
        if (filter.getStartTimeStart() > 0 && filter.getStartTimeEnd() > 0) {
            criteriaList.add(Criteria.where(EntityFields.START_TIME)
                    .gte(filter.getStartTimeStart())
                    .lte(filter.getStartTimeEnd()));
        }

        // Filter by end time range
        if (filter.getEndTimeStart() > 0 && filter.getEndTimeEnd() > 0) {
            criteriaList.add(Criteria.where(EntityFields.END_TIME)
                    .gte(filter.getEndTimeStart())
                    .lte(filter.getEndTimeEnd()));
        }

        // Filter by status
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.STATUS).is(filter.getStatus()));
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
            query.with(Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME));
        }

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoScheduledContextInstanceRecordImpl.class);

        // Apply pagination if specified
        if (limit > 0 && offset >= 0) {
            int page = offset / limit;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        } else if (offset > 0) {
            query.skip(offset);
        }

        // Execute query
        List<MongoScheduledContextInstanceRecordImpl> results =
                mongoTemplate.find(query, MongoScheduledContextInstanceRecordImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled context instances (total: {})", results.size(), totalCount);

        return createSearchResults(new ArrayList<>(results), totalCount, queryTime);
    }

    /**
     * Convert ScheduledContextInstanceRecord to MongoDB entity
     *
     * @param scheduledContextInstanceRecord the record to convert
     * @return the entity ready for persistence
     */
    private MongoScheduledContextInstanceRecordImpl convertToEntity(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        MongoScheduledContextInstanceRecordImpl entity = new MongoScheduledContextInstanceRecordImpl();

        entity.setId(scheduledContextInstanceRecord.getContextInstance().getId() + "_" + SCHEDULED_CONTEXT_INSTANCE_TYPE);
        entity.setType(SCHEDULED_CONTEXT_INSTANCE_TYPE);
        entity.setContextInstance(scheduledContextInstanceRecord.getContextInstance());
        entity.setStatus(scheduledContextInstanceRecord.getStatus());
        entity.setContextName(scheduledContextInstanceRecord.getContextName());
        entity.setContextInstanceId(scheduledContextInstanceRecord.getContextInstance().getId());
        entity.setTimestamp(scheduledContextInstanceRecord.getTimestamp());
        entity.setModifiedTimestamp(System.currentTimeMillis());
        // only update modified by field if populated.
        if(scheduledContextInstanceRecord.getModifiedBy() != null &&
            !scheduledContextInstanceRecord.getModifiedBy().isEmpty()) {
            entity.setModifiedBy(scheduledContextInstanceRecord.getModifiedBy());
        }
        entity.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());
        entity.setStartTime(scheduledContextInstanceRecord.getContextInstance().getStartTime());
        entity.setEndTime(scheduledContextInstanceRecord.getContextInstance().getEndTime());
        entity.setContainsRepeatingJobs(scheduledContextInstanceRecord.getContextInstance().isContainsRepeatingJobs());

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
    private SearchResults<ScheduledContextInstanceRecord> createSearchResults(
            List<ScheduledContextInstanceRecord> results, long totalCount, long queryTime) {

        return new SearchResults<ScheduledContextInstanceRecord>() {
            @Override
            public List<ScheduledContextInstanceRecord> getResultList() {
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

    /**
     * Get timestamp at end of day
     *
     * @param date the date
     * @return timestamp at end of day
     */
    public long atEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime().getTime();
    }

    /**
     * Get timestamp at start of day
     *
     * @param date the date
     * @return timestamp at start of day
     */
    public long atStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }
}
