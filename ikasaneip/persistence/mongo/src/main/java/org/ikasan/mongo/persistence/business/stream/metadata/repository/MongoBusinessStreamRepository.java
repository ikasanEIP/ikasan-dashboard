package org.ikasan.mongo.persistence.business.stream.metadata.repository;

import org.ikasan.mongo.persistence.business.stream.metadata.model.MongoBusinessStream;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for MongoBusinessStream.
 */
@Repository
public interface MongoBusinessStreamRepository extends MongoRepository<MongoBusinessStream, String> {
}
