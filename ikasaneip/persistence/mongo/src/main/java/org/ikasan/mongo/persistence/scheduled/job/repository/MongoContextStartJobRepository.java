package org.ikasan.mongo.persistence.scheduled.job.repository;

import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextStartJobRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for ContextStartJob records.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoContextStartJobRepository extends MongoRepository<MongoContextStartJobRecordImpl, String> {

    /**
     * Find all ContextStartJob records by context name.
     *
     * @param contextName the context name to search for
     * @return list of matching records
     */
    List<MongoContextStartJobRecordImpl> findByContextName(String contextName);
}
