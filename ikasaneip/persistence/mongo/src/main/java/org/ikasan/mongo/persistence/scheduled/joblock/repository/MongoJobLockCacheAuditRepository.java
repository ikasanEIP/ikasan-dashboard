package org.ikasan.mongo.persistence.scheduled.joblock.repository;

import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheAuditRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@DependsOn("mongoTemplate")
public interface MongoJobLockCacheAuditRepository extends MongoRepository<MongoJobLockCacheAuditRecordImpl, String> {
}
