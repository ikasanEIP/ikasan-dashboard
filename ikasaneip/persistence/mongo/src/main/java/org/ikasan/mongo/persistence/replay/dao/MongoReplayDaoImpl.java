package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.general.dao.AtlasSearchFilter;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
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

    /**
     * Fields to search in for full-text search.
     */
    private static final List<String> SEARCH_FIELDS = List.of(
        EntityFields.PAYLOAD_CONTENT,
        EntityFields.EVENT
    );

    private final MongoReplayEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final AtlasSearchQueryHelper atlasSearchQueryHelper;

    public MongoReplayDaoImpl(MongoReplayEventRepository repository, MongoTemplate mongoTemplate) {
        this(repository, mongoTemplate, new AtlasSearchQueryHelper(mongoTemplate));
    }

    public MongoReplayDaoImpl(MongoReplayEventRepository repository, MongoTemplate mongoTemplate,
                              AtlasSearchQueryHelper atlasSearchQueryHelper) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.atlasSearchQueryHelper = atlasSearchQueryHelper;
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

        AtlasSearchFilter searchFilter = AtlasSearchFilter.builder()
            .equals(EntityFields.TYPE, REPLAY)
            .in(EntityFields.MODULE_NAME, moduleNames)
            .in(EntityFields.FLOW_NAME, flowNames)
            .rangeMillis(EntityFields.CREATED_DATE_TIME, fromTimestamp, untilTimestamp)
            .containsAny(SEARCH_FIELDS, searchString)
            .build();

        return atlasSearchQueryHelper.search(searchFilter, null, offset, limit, false, MongoReplayEventImpl.class)
            .getResults().stream()
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
        entity.setHarvestReceivedTimestamp(System.currentTimeMillis());

        return entity;
    }
}
