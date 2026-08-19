package org.ikasan.mongo.persistence.systemevent.dao;

import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventImpl;
import org.ikasan.mongo.persistence.systemevent.repository.MongoSystemEventRepository;
import org.ikasan.spec.entity.EntityDao;
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
import java.util.regex.Pattern;

/**
 * MongoDB implementation of SystemEventSearchDao and EntityDao.
 *
 * @author Ikasan Development Team
 */
public class MongoSystemEventDao implements SystemEventSearchDao, EntityDao<SystemEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoSystemEventDao.class);

    private static final String SYSTEM_EVENT = "systemEvent";

    private final MongoSystemEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final JsonMapper objectMapper;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template
     */
    public MongoSystemEventDao(MongoSystemEventRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = JsonMapper.builder().build();
    }

    @Override
    public SystemEvent findById(String id) {
        logger.debug("Finding system event by id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        MongoSystemEventImpl result = mongoTemplate.findOne(query, MongoSystemEventImpl.class);

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

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Filter by actor (wildcard search)
        if (filter.getActor() != null && !filter.getActor().isEmpty()) {
            String regex = ".*" + Pattern.quote(filter.getActor()) + ".*";
            criteriaList.add(Criteria.where("actor").regex(regex, "i"));
        }

        // Filter by subject (wildcard search)
        if (filter.getSubject() != null && !filter.getSubject().isEmpty()) {
            String regex = ".*" + Pattern.quote(filter.getSubject()) + ".*";
            criteriaList.add(Criteria.where("subject").regex(regex, "i"));
        }

        // Filter by action (wildcard search)
        if (filter.getAction() != null && !filter.getAction().isEmpty()) {
            String regex = ".*" + Pattern.quote(filter.getAction()) + ".*";
            criteriaList.add(Criteria.where("action").regex(regex, "i"));
        }

        // Filter by search term in payload
        if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
            String[] terms = filter.getSearchTerm().split(" ");
            for (String term : terms) {
                if (!term.isEmpty()) {
                    String regex = ".*" + Pattern.quote(term) + ".*";
                    criteriaList.add(Criteria.where("payload").regex(regex, "i"));
                }
            }
        }

        // Filter by date range
        if (filter.getEndTime() > 0) {
            Date startDate = new Date(filter.getStartTime());
            Date endDate = new Date(filter.getEndTime());
            criteriaList.add(Criteria.where("timestamp").gte(startDate).lte(endDate));
        }

        // Apply all criteria
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoSystemEventImpl.class);

        // Apply sorting
        if (sortColumn != null && !sortColumn.isEmpty()) {
            Sort.Direction direction = "ASCENDING".equals(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortColumn));
        } else {
            // Default sort by timestamp descending
            query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        }

        // Apply pagination
        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        // Execute query
        List<MongoSystemEventImpl> results = mongoTemplate.find(query, MongoSystemEventImpl.class);

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

    @Override
    public void save(SystemEvent systemEvent) {
        logger.debug("Saving system event: {}", systemEvent);

        MongoSystemEventImpl mongoEvent = convertToMongoEntity(systemEvent);
        repository.save(mongoEvent);

        logger.debug("Saved system event with id: {}", mongoEvent.getMongoId());
    }

    @Override
    public void save(List<SystemEvent> systemEvents) {
        logger.debug("Saving {} system events", systemEvents.size());

        List<MongoSystemEventImpl> mongoEvents = new ArrayList<>();
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
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Deleted expired system events before: {}", currentTime);
    }

    /**
     * Convert SystemEvent to MongoSystemEventImpl
     *
     * @param systemEvent the system event
     * @return the MongoDB entity
     */
    private MongoSystemEventImpl convertToMongoEntity(SystemEvent systemEvent) {
        if (systemEvent instanceof MongoSystemEventImpl) {
            MongoSystemEventImpl mongoEvent = (MongoSystemEventImpl) systemEvent;

            // Ensure MongoDB id is set
            if (mongoEvent.getMongoId() == null) {
                mongoEvent.setId(buildMongoId(systemEvent));
            }

            // Serialize the full SystemEvent to JSON for the payload field
            if (mongoEvent.getPayload() == null) {
                try {
                    mongoEvent.setPayload(objectMapper.writeValueAsString(systemEvent));
                } catch (JacksonException e) {
                    throw new RuntimeException("Cannot convert system event to JSON string!", e);
                }
            }

            return mongoEvent;
        }

        // Convert from other SystemEvent implementations
        MongoSystemEventImpl mongoEvent = new MongoSystemEventImpl();
        mongoEvent.setId(buildMongoId(systemEvent));
        mongoEvent.setModuleName(systemEvent.getModuleName());
        mongoEvent.setActor(systemEvent.getActor());
        mongoEvent.setAction(systemEvent.getAction());
        mongoEvent.setSubject(systemEvent.getSubject());
        mongoEvent.setTimestamp(systemEvent.getTimestamp());
        mongoEvent.setExpiry(systemEvent.getExpiry());
        mongoEvent.setSystemEventId(systemEvent.getId());

        // Serialize the full SystemEvent to JSON
        try {
            mongoEvent.setPayload(objectMapper.writeValueAsString(systemEvent));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert system event to JSON string!", e);
        }

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
            return systemEvent.getModuleName() + "-" + SYSTEM_EVENT + "-" + systemEvent.getId();
        } else {
            return SYSTEM_EVENT + "-" + systemEvent.getSubject() + "-" + systemEvent.getId();
        }
    }
}
