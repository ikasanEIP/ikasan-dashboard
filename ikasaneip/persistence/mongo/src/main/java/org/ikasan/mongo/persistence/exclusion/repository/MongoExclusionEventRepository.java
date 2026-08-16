package org.ikasan.mongo.persistence.exclusion.repository;

import org.ikasan.mongo.persistence.exclusion.model.MongoExclusionEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@DependsOn("mongoTemplate")
public interface MongoExclusionEventRepository extends MongoRepository<MongoExclusionEvent, String> {

    Optional<MongoExclusionEvent> findByErrorUri(String errorUri);

    List<MongoExclusionEvent> findByModuleName(String moduleName);

    List<MongoExclusionEvent> findByModuleNameAndFlowName(String moduleName, String flowName);

    List<MongoExclusionEvent> findByExpiryLessThan(long currentTime);

    void deleteByExpiryLessThan(long currentTime);
}
