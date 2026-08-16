package org.ikasan.mongo.persistence.scheduled.notification.repository;

import org.ikasan.mongo.persistence.scheduled.notification.model.MongoEmailNotificationContextRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@DependsOn("mongoTemplate")
public interface MongoEmailNotificationContextRepository extends MongoRepository<MongoEmailNotificationContextRecordImpl, String> {
}
