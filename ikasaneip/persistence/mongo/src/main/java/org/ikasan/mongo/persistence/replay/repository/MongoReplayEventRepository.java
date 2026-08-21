package org.ikasan.mongo.persistence.replay.repository;

import org.ikasan.mongo.persistence.replay.model.MongoReplayEventImpl;
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
public interface MongoReplayEventRepository extends MongoRepository<MongoReplayEventImpl, String> {

    List<MongoReplayEventImpl> findByModuleName(String moduleName);

    List<MongoReplayEventImpl> findByModuleNameAndFlowName(String moduleName, String flowName);

    void deleteByExpiryLessThan(long currentTime);
}
