package org.ikasan.mongo.persistence.scheduled.context.repository;

import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for MongoScheduledContextRecordImpl.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoScheduledContextRecordRepository extends MongoRepository<MongoScheduledContextRecordImpl, String> {

    /**
     * Find a ScheduledContextRecord by context name.
     *
     * @param contextName the context name
     * @return Optional containing the record if found
     */
    Optional<MongoScheduledContextRecordImpl> findByContextName(String contextName);

    /**
     * Delete a ScheduledContextRecord by context name.
     *
     * @param contextName the context name
     */
    void deleteByContextName(String contextName);
}
