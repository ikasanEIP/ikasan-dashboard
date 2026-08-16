package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.replay.model.MongoReplayEvent;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayEventRepository;
import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.replay.ReplayEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of EsbEntityDao for ReplayEvent.
 *
 * @author Ikasan Development Team
 */
public class MongoReplayDao implements EsbEntityDao<ReplayEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoReplayDao.class);

    private final MongoReplayEventRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoReplayDao(MongoReplayEventRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(ReplayEvent replayEvent) {
        MongoReplayEvent entity = convertToEntity(replayEvent);
        repository.save(entity);
        logger.debug("Saved replay event for module: {}, flow: {}, eventId: {}",
                replayEvent.getModuleName(), replayEvent.getFlowName(), replayEvent.getEventId());
    }

    @Override
    public void save(List<ReplayEvent> replayEvents) {
        List<MongoReplayEvent> entities = replayEvents.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} replay events", replayEvents.size());
    }

    public List<ReplayEvent> find(List<String> moduleNames, List<String> flowNames, String searchString,
                                  long fromTimestamp, long untilTimestamp, int offset, int limit) {
        Query query = new Query();
        List<Criteria> criteriaList = new java.util.ArrayList<>();

        if (moduleNames != null && !moduleNames.isEmpty()) {
            criteriaList.add(Criteria.where("module_name").in(moduleNames));
        }

        if (flowNames != null && !flowNames.isEmpty()) {
            criteriaList.add(Criteria.where("flow_name").in(flowNames));
        }

        if (fromTimestamp > 0 || untilTimestamp > 0) {
            Criteria timestampCriteria = Criteria.where("timestamp");
            if (fromTimestamp > 0) {
                timestampCriteria.gte(fromTimestamp);
            }
            if (untilTimestamp > 0) {
                timestampCriteria.lte(untilTimestamp);
            }
            criteriaList.add(timestampCriteria);
        }

        if (searchString != null && !searchString.trim().isEmpty()) {
            String searchPattern = ".*" + searchString + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("event_as_string").regex(searchPattern, "i"),
                    Criteria.where("event_id").regex(searchPattern, "i"),
                    Criteria.where("related_event_identifier").regex(searchPattern, "i")
            );
            criteriaList.add(searchCriteria);
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        query.skip(offset).limit(limit);

        List<MongoReplayEvent> entities = mongoTemplate.find(query, MongoReplayEvent.class);
        return entities.stream()
                .map(entity -> (ReplayEvent) entity)
                .collect(Collectors.toList());
    }

    public void delete(String id) {
        repository.deleteById(id);
        logger.debug("Deleted replay event with id: {}", id);
    }

    public void deleteExpired() {
        long currentTime = System.currentTimeMillis();
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Deleted expired replay events before timestamp: {}", currentTime);
    }

    /**
     * Converts a ReplayEvent to a MongoReplayEvent entity.
     *
     * @param replayEvent the ReplayEvent to convert
     * @return the MongoReplayEvent entity
     */
    private MongoReplayEvent convertToEntity(ReplayEvent replayEvent) {
        if (replayEvent instanceof MongoReplayEvent) {
            MongoReplayEvent mongoEvent = (MongoReplayEvent) replayEvent;
            if (mongoEvent.getIdAsString() == null) {
                String id = replayEvent.getModuleName() + "-replay-" + UUID.randomUUID();
                mongoEvent.setId(id);
            }
            if (mongoEvent.getCreatedTimestamp() == 0) {
                mongoEvent.setCreatedTimestamp(System.currentTimeMillis());
            }
            return mongoEvent;
        }

        MongoReplayEvent entity = new MongoReplayEvent();
        String id = replayEvent.getModuleName() + "-replay-" + UUID.randomUUID();
        entity.setId(id);
        entity.setModuleName(replayEvent.getModuleName());
        entity.setFlowName(replayEvent.getFlowName());
        entity.setEventId(replayEvent.getEventId());
        entity.setEvent(replayEvent.getEvent());
        entity.setEventAsString(replayEvent.getEventAsString());
        entity.setTimestamp(replayEvent.getTimestamp());
        entity.setExpiry(replayEvent.getExpiry());
        entity.setCreatedTimestamp(System.currentTimeMillis());

        return entity;
    }
}
