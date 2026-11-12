package org.ikasan.mongo.persistence.systemevent.dao;

import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventRecordImpl;
import org.ikasan.mongo.persistence.systemevent.repository.MongoSystemEventRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchDao;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.ikasan.spec.entity.EntityFields.EXPIRY;
import static org.ikasan.spec.entity.EntityFields.TYPE;

/**
 * MongoDB implementation of SystemEventSearchDao and EntityDao.
 *
 * @author Ikasan Development Team
 */
public class MongoSystemEventDaoImpl implements SystemEventSearchDao, EntityDao<SystemEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoSystemEventDaoImpl.class);

    /**
     * Fields to search in for full-text search.
     */
    private static final List<String> SEARCH_FIELDS = List.of(
        EntityFields.ACTOR,
        EntityFields.SYSTEM_EVENT_SUBJECT,
        EntityFields.SYSTEM_EVENT_ACTION,
        EntityFields.PAYLOAD_CONTENT
    );

    private final MongoSystemEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final AtlasSearchQueryHelper atlasSearchQueryHelper;
    private final JsonMapper objectMapper;
    private final int daysToKeep;

    /**
     * Constructor for MongoSystemEventDaoImpl.
     *
     * @param repository the MongoDB repository for system events, used for database operations.
     * @param mongoTemplate the MongoTemplate for executing MongoDB queries and updates.
     * @param daysToKeep the number of days for which system events should be retained in the database.
     */
    public MongoSystemEventDaoImpl(MongoSystemEventRepository repository, MongoTemplate mongoTemplate, int daysToKeep) {
        this(repository, mongoTemplate, daysToKeep, new AtlasSearchQueryHelper(mongoTemplate));
    }

    /**
     * Constructor for MongoSystemEventDaoImpl with custom AtlasSearchQueryHelper.
     *
     * @param repository the MongoDB repository for system events, used for database operations.
     * @param mongoTemplate the MongoTemplate for executing MongoDB queries and updates.
     * @param daysToKeep the number of days for which system events should be retained in the database.
     * @param atlasSearchQueryHelper the AtlasSearchQueryHelper for search queries.
     */
    public MongoSystemEventDaoImpl(MongoSystemEventRepository repository, MongoTemplate mongoTemplate, int daysToKeep,
                                   AtlasSearchQueryHelper atlasSearchQueryHelper) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.atlasSearchQueryHelper = atlasSearchQueryHelper;
        this.daysToKeep = daysToKeep;
        this.objectMapper = JsonMapper.builder().build();
    }

    @Override
    public SystemEvent findById(String id) {
        logger.debug("Finding system event by id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where(TYPE).is(SYSTEM_EVENT_TYPE));

        MongoSystemEventRecordImpl result = mongoTemplate.findOne(query, MongoSystemEventRecordImpl.class);

        if (result != null) {
            logger.debug("Found system event: {}", result);
        } else {
            logger.debug("No system event found with id: {}", id);
        }

        return result;
    }

    @Override
    public SearchResults<SystemEvent> findByFilter(SystemEventSearchFilter filter, int limit, int offset,
                                                     String sortColumn, String sortOrder) {
        logger.debug("Finding system events by filter: {}, limit: {}, offset: {}, sortColumn: {}, sortOrder: {}",
                filter, limit, offset, sortColumn, sortOrder);

        // Determine if we have a search term that requires Atlas Search
        String searchTerm = filter.getSearchTerm();
        boolean hasSearchTerm = searchTerm != null && !searchTerm.trim().isEmpty();

        // Build match criteria for filtering (exact matching for filter fields)
        Criteria matchCriteria = buildMatchCriteria(filter);

        // Build sort
        Sort sort;
        if (sortColumn != null && !sortColumn.isEmpty()) {
            Sort.Direction direction = "ASCENDING".equals(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, sortColumn);
        } else {
            // Default sort by timestamp descending
            sort = Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME);
        }

        // If searchTerm is provided, use Atlas Search
        if (hasSearchTerm) {
            AtlasSearchQueryHelper.SearchQueryResult<MongoSystemEventRecordImpl> result =
                atlasSearchQueryHelper.searchWithWildcard(searchTerm, SEARCH_FIELDS, matchCriteria,
                    sort, offset, limit, MongoSystemEventRecordImpl.class);

            List<SystemEvent> systemEvents = new ArrayList<>(result.getResults());
            long totalCount = result.getTotalCount();

            logger.debug("Found {} system events (total: {})", systemEvents.size(), totalCount);

            return new SearchResults<SystemEvent>() {
                @Override
                public List<SystemEvent> getResultList() {
                    return systemEvents;
                }

                @Override
                public long getTotalNumberOfResults() {
                    return totalCount;
                }

                @Override
                public long getQueryResponseTime() {
                    return 0; // Query response time not tracked for MongoDB
                }
            };
        }

        // Otherwise use standard query
        Query query = new Query();
        if (matchCriteria != null) {
            query.addCriteria(matchCriteria);
        }
        query.with(sort);

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoSystemEventRecordImpl.class);

        // Apply pagination
        if(limit > 0 && offset > -1) {
            Pageable pageable = PageRequest.of(offset / limit, limit);
            query.with(pageable);
        }

        // Execute query
        List<MongoSystemEventRecordImpl> results = mongoTemplate.find(query, MongoSystemEventRecordImpl.class);

        logger.debug("Found {} system events (total: {})", results.size(), totalCount);

        // Convert to SearchResults
        List<SystemEvent> systemEvents = new ArrayList<>(results);

        return new SearchResults<SystemEvent>() {
            @Override
            public List<SystemEvent> getResultList() {
                return systemEvents;
            }

            @Override
            public long getTotalNumberOfResults() {
                return totalCount;
            }

            @Override
            public long getQueryResponseTime() {
                return 0; // Query response time not tracked for MongoDB
            }
        };
    }

    /**
     * Build match criteria for filtering.
     */
    private Criteria buildMatchCriteria(SystemEventSearchFilter filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Always add type filter
        criteriaList.add(Criteria.where(TYPE).is(SYSTEM_EVENT_TYPE));

        // Filter by actor (wildcard search using regex)
        if (filter.getActor() != null && !filter.getActor().isEmpty()) {
            String regex = ".*" + escapeRegex(filter.getActor()) + ".*";
            criteriaList.add(Criteria.where(EntityFields.ACTOR).regex(regex, "i"));
        }

        // Filter by subject (wildcard search using regex)
        if (filter.getSubject() != null && !filter.getSubject().isEmpty()) {
            String regex = ".*" + escapeRegex(filter.getSubject()) + ".*";
            criteriaList.add(Criteria.where(EntityFields.SYSTEM_EVENT_SUBJECT).regex(regex, "i"));
        }

        // Filter by action (wildcard search using regex)
        if (filter.getAction() != null && !filter.getAction().isEmpty()) {
            String regex = ".*" + escapeRegex(filter.getAction()) + ".*";
            criteriaList.add(Criteria.where(EntityFields.SYSTEM_EVENT_ACTION).regex(regex, "i"));
        }

        // Filter by date range
        if (filter.getEndTime() > 0) {
            Date startDate = new Date(filter.getStartTime());
            Date endDate = new Date(filter.getEndTime());
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(startDate).lte(endDate));
        }

        if (criteriaList.isEmpty()) {
            return null;
        } else if (criteriaList.size() == 1) {
            return criteriaList.get(0);
        } else {
            return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        }
    }

    /**
     * Escape special regex characters in a string.
     */
    private String escapeRegex(String input) {
        return input.replaceAll("([\\\\\\[\\](){}.*+?^$|])", "\\\\$1");
    }

    @Override
    public void save(SystemEvent systemEvent) {
        logger.debug("Saving system event: {}", systemEvent);

        MongoSystemEventRecordImpl mongoEvent = convertToMongoEntity(systemEvent);
        repository.save(mongoEvent);

        logger.debug("Saved system event with id: {}", mongoEvent.getMongoId());
    }

    @Override
    public void save(List<SystemEvent> systemEvents) {
        logger.debug("Saving {} system events", systemEvents.size());

        List<MongoSystemEventRecordImpl> mongoEvents = new ArrayList<>();
        for (SystemEvent systemEvent : systemEvents) {
            mongoEvents.add(convertToMongoEntity(systemEvent));
        }

        repository.saveAll(mongoEvents);
        logger.debug("Saved {} system events", mongoEvents.size());
    }

    /**
     * Delete expired system events
     */
    public void deleteExpired() {
        Date currentTime = new Date();
        Query query = new Query();
        query.addCriteria(Criteria.where(EXPIRY).lt(currentTime));
        query.addCriteria(Criteria.where(TYPE).is(SYSTEM_EVENT_TYPE));

        mongoTemplate.remove(query, MongoSystemEventRecordImpl.class);
        logger.debug("Deleted expired system events before: {}", currentTime);
    }

    /**
     * Convert SystemEvent to MongoSystemEventRecordImpl
     *
     * @param systemEvent the system event
     * @return the MongoDB entity
     */
    private MongoSystemEventRecordImpl convertToMongoEntity(SystemEvent systemEvent) {
        MongoSystemEventRecordImpl mongoEvent = new MongoSystemEventRecordImpl();
        mongoEvent.setId(buildMongoId(systemEvent));
        mongoEvent.setType(SYSTEM_EVENT_TYPE);
        try {
            mongoEvent.setPayload(objectMapper.writeValueAsString(systemEvent));
        }
        catch (JacksonException e) {
            throw new RuntimeException(String.format("Cannot convert system event to string! [%s]", systemEvent));
        }
        mongoEvent.setModuleName(systemEvent.getModuleName());
        mongoEvent.setActor(systemEvent.getActor());
        mongoEvent.setAction(systemEvent.getAction());
        mongoEvent.setSubject(systemEvent.getSubject());
        mongoEvent.setTimestamp(systemEvent.getTimestamp());
        mongoEvent.setExpiry(systemEvent.getExpiry());
        mongoEvent.setSystemEventId(systemEvent.getId());
        mongoEvent.setExpiry(new Date(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis()));

        return mongoEvent;
    }

    /**
     * Build MongoDB document id from SystemEvent
     * Format: moduleName-systemEvent-id OR systemEvent-subject-id
     *
     * @param systemEvent the system event
     * @return the MongoDB id
     */
    private String buildMongoId(SystemEvent systemEvent) {
        if (systemEvent.getModuleName() != null && !systemEvent.getModuleName().isEmpty()) {
            return systemEvent.getModuleName() + "-" + SYSTEM_EVENT_TYPE + "-" + systemEvent.getId();
        } else {
            return SYSTEM_EVENT_TYPE + "-" + systemEvent.getSubject() + "-" + systemEvent.getId();
        }
    }
}
