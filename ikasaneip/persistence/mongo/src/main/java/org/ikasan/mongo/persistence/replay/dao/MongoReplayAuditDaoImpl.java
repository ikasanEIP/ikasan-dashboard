package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.replay.model.MongoReplayAuditEventImpl;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayAuditEventRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ReplayAuditDao.
 *
 * @author Ikasan Development Team
 */
public class MongoReplayAuditDaoImpl implements BatchInsert<ReplayAuditEvent>, EntityDao<ReplayAuditEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoReplayAuditDaoImpl.class);

    private final MongoReplayAuditEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final int daysToKeep;
    private final JsonMapper mapper;

    public MongoReplayAuditDaoImpl(MongoReplayAuditEventRepository repository, MongoTemplate mongoTemplate
        , int daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
        this.mapper = JsonMapper.builder().build();
    }

    @Override
    public void insert(List<ReplayAuditEvent> entities) {
        long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
        long expiry = millisecondsInDay + System.currentTimeMillis();
        try {
            String json = mapper.writeValueAsString(entities);
            logger.debug("Batch insert replay audit events, count: {}, json: {}", entities.size(), json);

            MongoReplayAuditEventImpl entity = new MongoReplayAuditEventImpl();

            entity.setId("replay-audit-" + UUID.randomUUID());
            entity.setType(REPLAY_AUDIT);
            entity.setReplayAuditJson(json);
            entity.setTimestamp(System.currentTimeMillis());
            entity.setExpiry(expiry);

            repository.save(entity);
            logger.debug("Saved batch replay audit event with id: {}", entity.getId());
        } catch (Exception e) {
            throw new RuntimeException("An exception has occurred attempting to write replay audit event to MongoDB", e);
        }
    }

    @Override
    public void save(ReplayAuditEvent replayAuditEvent) {
        MongoReplayAuditEventImpl entity = convertToEntity(replayAuditEvent);
        repository.save(entity);
        logger.debug("Saved replay audit event with id: {}", entity.getId());
    }

    @Override
    public void save(List<ReplayAuditEvent> replayAuditEvents) {
        List<MongoReplayAuditEventImpl> entities = replayAuditEvents.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} replay audit events", replayAuditEvents.size());
    }

    public void deleteExpired() {
        long currentTime = System.currentTimeMillis();
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Deleted expired replay audit events before timestamp: {}", currentTime);
    }

    /**
     * Converts a ReplayAuditEvent to a MongoReplayAuditEventImpl entity.
     *
     * @param replayAuditEvent the ReplayAuditEvent to convert
     * @return the MongoReplayAuditEventImpl entity
     */
    private MongoReplayAuditEventImpl convertToEntity(ReplayAuditEvent replayAuditEvent) {
        MongoReplayAuditEventImpl entity = new MongoReplayAuditEventImpl();

        long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
        long expiry = millisecondsInDay + System.currentTimeMillis();

        String json = mapper.writeValueAsString(replayAuditEvent);

        logger.debug("Result json: " + json);

        entity.setId("replay-audit-" + UUID.randomUUID());
        entity.setType(REPLAY_AUDIT);
        entity.setReplayAuditJson(json);
        entity.setTimestamp(System.currentTimeMillis());
        entity.setExpiry(expiry);

        return entity;
    }
}
