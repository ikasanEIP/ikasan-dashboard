package org.ikasan.mongo.persistence.scheduled.profile.repository;

import org.ikasan.mongo.persistence.scheduled.profile.model.MongoContextProfileRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for MongoContextProfileRecordImpl.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoContextProfileRepository extends MongoRepository<MongoContextProfileRecordImpl, String> {
}
