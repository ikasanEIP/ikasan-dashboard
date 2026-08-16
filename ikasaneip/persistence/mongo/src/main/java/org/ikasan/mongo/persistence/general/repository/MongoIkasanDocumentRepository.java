package org.ikasan.mongo.persistence.general.repository;

import org.ikasan.mongo.persistence.general.model.MongoIkasanDocument;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for MongoIkasanDocument.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoIkasanDocumentRepository extends MongoRepository<MongoIkasanDocument, String> {
}
