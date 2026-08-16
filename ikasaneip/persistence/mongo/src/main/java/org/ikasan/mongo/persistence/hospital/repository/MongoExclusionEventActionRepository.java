package org.ikasan.mongo.persistence.hospital.repository;

import org.ikasan.mongo.persistence.hospital.model.MongoExclusionEventAction;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@DependsOn("mongoTemplate")
public interface MongoExclusionEventActionRepository extends MongoRepository<MongoExclusionEventAction, String> {

    Optional<MongoExclusionEventAction> findByErrorUri(String errorUri);

    List<MongoExclusionEventAction> findByModuleName(String moduleName);

    List<MongoExclusionEventAction> findByModuleNameAndFlowName(String moduleName, String flowName);

    List<MongoExclusionEventAction> findByExpiryLessThan(long currentTime);

    void deleteByExpiryLessThan(long currentTime);
}
