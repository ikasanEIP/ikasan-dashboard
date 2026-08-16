package org.ikasan.mongo.persistence.scheduled.job.repository;

import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@DependsOn("mongoTemplate")
public interface MongoInternalEventDrivenJobRepository extends MongoRepository<MongoInternalEventDrivenJobRecordImpl, String> {
    List<MongoInternalEventDrivenJobRecordImpl> findByContextName(String contextName);
}
