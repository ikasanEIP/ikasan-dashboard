package org.ikasan.mongo.persistence.scheduled.job.repository;

import org.ikasan.mongo.persistence.scheduled.job.model.MongoFileEventDrivenJobRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for FileEventDrivenJob records.
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoFileEventDrivenJobRepository extends MongoRepository<MongoFileEventDrivenJobRecordImpl, String> {

    /**
     * Find all FileEventDrivenJob records by context name.
     *
     * @param contextName the context name to search for
     * @return list of matching records
     */
    List<MongoFileEventDrivenJobRecordImpl> findByContextName(String contextName);
}
