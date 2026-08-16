package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.replay.model.MongoReplayAuditEvent;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayAuditEventRepository;
import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayAudit;
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
public class MongoReplayAuditDao implements BatchInsert<ReplayAuditEvent>, EsbEntityDao<ReplayAuditEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoReplayAuditDao.class);

    private final MongoReplayAuditEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final int daysToKeep;
    private final JsonMapper mapper;

    public MongoReplayAuditDao(MongoReplayAuditEventRepository repository, MongoTemplate mongoTemplate, int daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
        this.mapper = JsonMapper.builder().build();
    }

    @Override
    public void insert(List<ReplayAuditEvent> entities) {
        long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
        long expiry = millisecondsInDay + System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();

        try {
            String json = mapper.writeValueAsString(entities);
            logger.debug("Batch insert replay audit events, count: {}, json: {}", entities.size(), json);

            MongoReplayAuditEvent batchEvent = new MongoReplayAuditEvent();
            batchEvent.setId("replay-audit-" + currentTime);
            batchEvent.setReplayAuditJson(json);
            batchEvent.setSuccess(true); // Batch operation success
            batchEvent.setResultMessage("Batch insert of " + entities.size() + " events");
            batchEvent.setTimestamp(currentTime);
            batchEvent.setExpiry(expiry);
            batchEvent.setCreatedTimestamp(currentTime);

            repository.save(batchEvent);
            logger.debug("Saved batch replay audit event with id: {}", batchEvent.getId());
        } catch (Exception e) {
            throw new RuntimeException("An exception has occurred attempting to write replay audit event to MongoDB", e);
        }
    }

    @Override
    public void save(ReplayAuditEvent replayAuditEvent) {
        MongoReplayAuditEvent entity = convertToEntity(replayAuditEvent);
        repository.save(entity);
        logger.debug("Saved replay audit event with id: {}", entity.getId());
    }

    @Override
    public void save(List<ReplayAuditEvent> replayAuditEvents) {
        List<MongoReplayAuditEvent> entities = replayAuditEvents.stream()
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
     * Converts a ReplayAuditEvent to a MongoReplayAuditEvent entity.
     *
     * @param replayAuditEvent the ReplayAuditEvent to convert
     * @return the MongoReplayAuditEvent entity
     */
    private MongoReplayAuditEvent convertToEntity(ReplayAuditEvent replayAuditEvent) {
        if (replayAuditEvent instanceof MongoReplayAuditEvent) {
            MongoReplayAuditEvent mongoEvent = (MongoReplayAuditEvent) replayAuditEvent;
            if (mongoEvent.getId() == null) {
                mongoEvent.setId("replay-audit-" + UUID.randomUUID());
            }
            if (mongoEvent.getCreatedTimestamp() == 0) {
                mongoEvent.setCreatedTimestamp(System.currentTimeMillis());
            }
            if (mongoEvent.getExpiry() == 0) {
                long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
                mongoEvent.setExpiry(millisecondsInDay + System.currentTimeMillis());
            }

            // Serialize ReplayAudit if present
            if (mongoEvent.getReplayAudit() != null && mongoEvent.getReplayAuditJson() == null) {
                try {
                    mongoEvent.setReplayAuditJson(mapper.writeValueAsString(mongoEvent.getReplayAudit()));
                } catch (Exception e) {
                    logger.error("Failed to serialize ReplayAudit to JSON", e);
                }
            }

            return mongoEvent;
        }

        MongoReplayAuditEvent entity = new MongoReplayAuditEvent();
        entity.setId("replay-audit-" + UUID.randomUUID());
        entity.setSuccess(replayAuditEvent.isSuccess());
        entity.setResultMessage(replayAuditEvent.getResultMessage());
        entity.setTimestamp(replayAuditEvent.getTimestamp());

        long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
        entity.setExpiry(millisecondsInDay + System.currentTimeMillis());
        entity.setCreatedTimestamp(System.currentTimeMillis());

        // Serialize ReplayAudit
        ReplayAudit replayAudit = replayAuditEvent.getReplayAudit();
        if (replayAudit != null) {
            try {
                entity.setReplayAuditJson(mapper.writeValueAsString(replayAudit));
            } catch (Exception e) {
                logger.error("Failed to serialize ReplayAudit to JSON", e);
                throw new RuntimeException("An exception has occurred convert a replay audit event to a MongoDB document", e);
            }
        }

        return entity;
    }
}
