package org.ikasan.mongo.persistence.replay.repository;

import org.ikasan.mongo.persistence.replay.model.MongoReplayAuditEventImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for ReplayAuditEvent.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoReplayAuditEventRepository extends MongoRepository<MongoReplayAuditEventImpl, String> {

    void deleteByExpiryLessThan(long currentTime);
}
