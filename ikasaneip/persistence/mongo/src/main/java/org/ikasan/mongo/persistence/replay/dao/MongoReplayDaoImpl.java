package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.replay.model.MongoReplayEventImpl;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayEventRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityFields;
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
 * MongoDB implementation of EntityDao for ReplayEvent.
 *
 * @author Ikasan Development Team
 */
public class MongoReplayDaoImpl implements EntityDao<ReplayEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoReplayDaoImpl.class);

    private final MongoReplayEventRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoReplayDaoImpl(MongoReplayEventRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(ReplayEvent replayEvent) {
        MongoReplayEventImpl entity = convertToEntity(replayEvent);
        repository.save(entity);
        logger.debug("Saved replay event for module: {}, flow: {}, eventId: {}",
                replayEvent.getModuleName(), replayEvent.getFlowName(), replayEvent.getEventId());
    }

    @Override
    public void save(List<ReplayEvent> replayEvents) {
        List<MongoReplayEventImpl> entities = replayEvents.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} replay events", replayEvents.size());
    }

    public List<ReplayEvent> find(List<String> moduleNames, List<String> flowNames, String searchString,
                                  long fromTimestamp, long untilTimestamp, int offset, int limit) {
        Query query = new Query();
        List<Criteria> criteriaList = new java.util.ArrayList<>();

        criteriaList.add(Criteria.where(EntityFields.TYPE).is(REPLAY));

        if (moduleNames != null && !moduleNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).in(moduleNames));
        }

        if (flowNames != null && !flowNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).in(flowNames));
        }

        if (fromTimestamp > 0 || untilTimestamp > 0) {
            Criteria timestampCriteria = Criteria.where(EntityFields.CREATED_DATE_TIME);
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
                    Criteria.where(EntityFields.PAYLOAD_CONTENT).regex(searchPattern, "i"),
                    Criteria.where(EntityFields.EVENT).regex(searchPattern, "i")
            );
            criteriaList.add(searchCriteria);
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        query.skip(offset).limit(limit);

        List<MongoReplayEventImpl> entities = mongoTemplate.find(query, MongoReplayEventImpl.class);
        return entities.stream()
                .map(entity -> (ReplayEvent) entity)
                .collect(Collectors.toList());
    }

    public void delete(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(REPLAY));

        mongoTemplate.remove(query, MongoReplayEventImpl.class);
        logger.debug("Deleted replay event with id: {}", id);
    }

    public void deleteExpired() {
        long currentTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.EXPIRY).lt(currentTime));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(REPLAY));

        mongoTemplate.remove(query, MongoReplayEventImpl.class);
        logger.debug("Deleted expired replay events before timestamp: {}", currentTime);
    }

    /**
     * Converts a ReplayEvent to a MongoReplayEventImpl entity.
     *
     * @param replayEvent the ReplayEvent to convert
     * @return the MongoReplayEventImpl entity
     */
    private MongoReplayEventImpl convertToEntity(ReplayEvent replayEvent) {
        MongoReplayEventImpl entity = new MongoReplayEventImpl();
        String id = replayEvent.getModuleName() + "-replay-" + UUID.randomUUID();
        entity.setId(id);
        entity.setType(REPLAY);
        entity.setModuleName(replayEvent.getModuleName());
        entity.setFlowName(replayEvent.getFlowName());
        entity.setEventId(replayEvent.getEventId());

        if(replayEvent.getEventAsString() != null && !replayEvent.getEventAsString().isEmpty())
        {
            entity.setEventAsString(replayEvent.getEventAsString());
        }
        else
        {
            entity.setEventAsString(new String(replayEvent.getEvent()));
        }
        entity.setEvent(replayEvent.getEvent());
        entity.setTimestamp(replayEvent.getTimestamp());
        entity.setExpiry(replayEvent.getExpiry());

        return entity;
    }
}
