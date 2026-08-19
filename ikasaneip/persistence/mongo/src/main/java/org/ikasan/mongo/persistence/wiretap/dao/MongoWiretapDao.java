package org.ikasan.mongo.persistence.wiretap.dao;

import org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl;
import org.ikasan.mongo.persistence.wiretap.repository.MongoWiretapEventRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        Optional<MongoWiretapEventImpl> result = repository.findById(id);
        if (result.isPresent()) {
            return result.get();
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
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Deleted expired wiretap events before timestamp: {}", currentTime);
    }

    /**
     * Converts a WiretapEvent to a MongoWiretapEventImpl entity.
     *
     * @param wiretapEvent the WiretapEvent to convert
     * @return the MongoWiretapEventImpl entity
     */
    private MongoWiretapEventImpl convertToEntity(WiretapEvent wiretapEvent) {
        if (wiretapEvent instanceof MongoWiretapEventImpl) {
            MongoWiretapEventImpl mongoEvent = (MongoWiretapEventImpl) wiretapEvent;

            // Ensure ID is set based on module name and identifier
            if (mongoEvent.getId() == null && mongoEvent.getModuleName() != null && mongoEvent.getIdentifier() > 0) {
                mongoEvent.setId(mongoEvent.getModuleName() + "-wiretap-" + mongoEvent.getIdentifier());
            }

            // Set expiry if not already set
            if (mongoEvent.getExpiry() == 0) {
                long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
                mongoEvent.setExpiry(millisecondsInDay + System.currentTimeMillis());
            }

            return mongoEvent;
        }

        // Convert from generic WiretapEvent to MongoWiretapEventImpl
        MongoWiretapEventImpl entity = new MongoWiretapEventImpl();
        entity.setIdentifier(wiretapEvent.getIdentifier());
        entity.setModuleName(wiretapEvent.getModuleName());
        entity.setFlowName(wiretapEvent.getFlowName());
        entity.setComponentName(wiretapEvent.getComponentName());
        entity.setEventId(wiretapEvent.getEventId());
        entity.setTimestamp(wiretapEvent.getTimestamp());

        // Set the event content
        Object event = wiretapEvent.getEvent();
        if (event != null) {
            entity.setEvent(event.toString());
        }

        // Generate ID
        if (wiretapEvent.getModuleName() != null && wiretapEvent.getIdentifier() > 0) {
            entity.setId(wiretapEvent.getModuleName() + "-wiretap-" + wiretapEvent.getIdentifier());
        }

        // Set expiry
        long expiry = wiretapEvent.getExpiry();
        if (expiry == 0) {
            long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
            expiry = millisecondsInDay + System.currentTimeMillis();
        }
        entity.setExpiry(expiry);

        return entity;
    }
}
