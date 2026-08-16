package org.ikasan.mongo.persistence.scheduled.job.repository;

import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextTerminalJobRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for ContextTerminalJob records.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoContextTerminalJobRepository extends MongoRepository<MongoContextTerminalJobRecordImpl, String> {

    /**
     * Find all ContextTerminalJob records by context name.
     *
     * @param contextName the context name to search for
     * @return list of matching records
     */
    List<MongoContextTerminalJobRecordImpl> findByContextName(String contextName);
}
