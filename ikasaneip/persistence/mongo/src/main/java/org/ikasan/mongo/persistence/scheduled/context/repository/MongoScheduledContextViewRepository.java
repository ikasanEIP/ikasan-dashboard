package org.ikasan.mongo.persistence.scheduled.context.repository;

import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextViewRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for MongoScheduledContextViewRecordImpl.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoScheduledContextViewRepository extends MongoRepository<MongoScheduledContextViewRecordImpl, String> {
}
