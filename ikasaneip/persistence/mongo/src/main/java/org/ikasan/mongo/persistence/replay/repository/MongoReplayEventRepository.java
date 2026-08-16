package org.ikasan.mongo.persistence.replay.repository;

import org.ikasan.mongo.persistence.replay.model.MongoReplayEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for ReplayEvent.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoReplayEventRepository extends MongoRepository<MongoReplayEvent, String> {

    List<MongoReplayEvent> findByModuleName(String moduleName);

    List<MongoReplayEvent> findByModuleNameAndFlowName(String moduleName, String flowName);

    void deleteByExpiryLessThan(long currentTime);
}
