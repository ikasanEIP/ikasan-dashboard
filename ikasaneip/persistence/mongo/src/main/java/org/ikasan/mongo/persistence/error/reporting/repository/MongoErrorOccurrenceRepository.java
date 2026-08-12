package org.ikasan.mongo.persistence.error.reporting.repository;

import org.ikasan.mongo.persistence.error.reporting.model.MongoErrorOccurrence;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MongoErrorOccurrenceRepository extends MongoRepository<MongoErrorOccurrence, String> {

    Optional<MongoErrorOccurrence> findByErrorUri(String errorUri);

    List<MongoErrorOccurrence> findByModuleName(String moduleName);

    List<MongoErrorOccurrence> findByModuleNameAndFlowName(String moduleName, String flowName);

    List<MongoErrorOccurrence> findByExpiryLessThan(long currentTime);

    void deleteByExpiryLessThan(long currentTime);
}
