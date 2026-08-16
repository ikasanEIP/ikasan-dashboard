package org.ikasan.mongo.persistence.scheduled.joblock.repository;

import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@DependsOn("mongoTemplate")
public interface MongoJobLockCacheRepository extends MongoRepository<MongoJobLockCacheRecordImpl, String> {
}
