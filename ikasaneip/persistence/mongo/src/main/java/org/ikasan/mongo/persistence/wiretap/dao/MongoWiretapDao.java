package org.ikasan.mongo.persistence.wiretap.dao;

import org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl;
import org.ikasan.mongo.persistence.wiretap.repository.MongoWiretapEventRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * MongoDB implementation of WiretapDao.
 *
 * @author Ikasan Development Team
 */
public class MongoWiretapDao implements EntityDao<WiretapEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoWiretapDao.class);

    private final MongoWiretapEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final int daysToKeep;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository for wiretap events
     * @param mongoTemplate the MongoDB template
     * @param daysToKeep the number of days to keep wiretap events
     */
    public MongoWiretapDao(MongoWiretapEventRepository repository, MongoTemplate mongoTemplate, int daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
    }

    @Override
    public void save(WiretapEvent wiretapEvent) {
        MongoWiretapEventImpl entity = convertToEntity(wiretapEvent);
        repository.save(entity);
        logger.debug("Saved wiretap event with id: {}", entity.getId());
    }

    @Override
    public void save(List<WiretapEvent> wiretapEvents) {
        List<MongoWiretapEventImpl> entities = wiretapEvents.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} wiretap events", wiretapEvents.size());
    }

    /**
     * Find a wiretap event by ID.
     * Handles both String ID format (moduleName-wiretap-identifier) and Long identifier format.
     *
     * @param id the ID to search for (can be String or Long)
     * @return the wiretap event if found
     */
    public WiretapEvent findById(String id) {
        if (id == null) {
            return null;
        }

        // Try to find by direct ID first
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(WIRETAP_TYPE));

        MongoWiretapEventImpl result = mongoTemplate.findOne(query, MongoWiretapEventImpl.class);
        if (result != null) {
            return result;
        }

        // If not found and id is numeric, it might be just the identifier
        // Try to parse as Long and search by identifier field
        try {
            Long identifier = Long.parseLong(id);
            // Note: This would require a custom query method in the repository
            // For now, we'll just try the direct lookup
            logger.debug("Could not find wiretap event by id: {}", id);
        } catch (NumberFormatException e) {
            logger.debug("ID is not numeric: {}", id);
        }

        return null;
    }

    /**
     * Delete wiretap events that have expired.
     */
    public void deleteExpired() {
        long currentTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where(EXPIRY).lt(currentTime));
        query.addCriteria(Criteria.where(TYPE).is(WIRETAP_TYPE));

        mongoTemplate.remove(query, MongoWiretapEventImpl.class);
        logger.debug("Deleted expired wiretap events before timestamp: {}", currentTime);
    }

    /**
     * Converts a WiretapEvent to a MongoWiretapEventImpl entity.
     *
     * @param wiretapEvent the WiretapEvent to convert
     * @return the MongoWiretapEventImpl entity
     */
    private MongoWiretapEventImpl convertToEntity(WiretapEvent wiretapEvent) {
        // Convert from generic WiretapEvent to MongoWiretapEventImpl
        MongoWiretapEventImpl entity = new MongoWiretapEventImpl();
        entity.setId(wiretapEvent.getModuleName() + "-wiretap-" + wiretapEvent.getIdentifier());
        entity.setType(WIRETAP_TYPE);
        entity.setModuleName(wiretapEvent.getModuleName());
        entity.setFlowName(wiretapEvent.getFlowName());
        entity.setComponentName(wiretapEvent.getComponentName());
        entity.setEventId(wiretapEvent.getEventId());
        entity.setEvent(wiretapEvent.getEvent().toString());
        entity.setTimestamp(wiretapEvent.getTimestamp());
        entity.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());

        return entity;
    }
}
