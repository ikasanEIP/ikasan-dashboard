package org.ikasan.mongo.persistence.module.metadata.repository;

import org.ikasan.mongo.persistence.module.metadata.model.MongoModuleMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for MongoModuleMetadata.
 */
@Repository
public interface MongoModuleMetadataRepository extends MongoRepository<MongoModuleMetadata, String> {
}
