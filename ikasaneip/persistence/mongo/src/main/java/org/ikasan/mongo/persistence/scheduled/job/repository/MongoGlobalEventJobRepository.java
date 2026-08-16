package org.ikasan.mongo.persistence.scheduled.job.repository;

import org.ikasan.mongo.persistence.scheduled.job.model.MongoGlobalEventJobRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for GlobalEventJob records.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoGlobalEventJobRepository extends MongoRepository<MongoGlobalEventJobRecordImpl, String> {

    /**
     * Find all GlobalEventJob records by context name.
     *
     * @param contextName the context name to search for
     * @return list of matching records
     */
    List<MongoGlobalEventJobRecordImpl> findByContextName(String contextName);
}
