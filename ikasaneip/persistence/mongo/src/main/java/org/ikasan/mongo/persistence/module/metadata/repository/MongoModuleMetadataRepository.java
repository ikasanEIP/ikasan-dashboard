package org.ikasan.mongo.persistence.module.metadata.repository;

import org.ikasan.mongo.persistence.module.metadata.model.MongoModuleMetadata;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for MongoModuleMetadata.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoModuleMetadataRepository extends MongoRepository<MongoModuleMetadata, String> {
}
