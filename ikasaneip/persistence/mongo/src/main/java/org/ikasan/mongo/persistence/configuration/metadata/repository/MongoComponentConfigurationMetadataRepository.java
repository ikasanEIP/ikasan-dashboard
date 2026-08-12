package org.ikasan.mongo.persistence.configuration.metadata.repository;

import org.ikasan.mongo.persistence.configuration.metadata.model.MongoComponentConfigurationMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for MongoComponentConfigurationMetadata.
 */
@Repository
public interface MongoComponentConfigurationMetadataRepository extends MongoRepository<MongoComponentConfigurationMetadata, String> {
}
