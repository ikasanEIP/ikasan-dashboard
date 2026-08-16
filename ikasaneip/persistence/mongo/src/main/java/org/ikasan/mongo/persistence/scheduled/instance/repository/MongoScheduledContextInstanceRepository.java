package org.ikasan.mongo.persistence.scheduled.instance.repository;

import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for ScheduledContextInstance operations.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoScheduledContextInstanceRepository extends MongoRepository<MongoScheduledContextInstanceRecordImpl, String> {

    /**
     * Find all by status
     *
     * @param status the status to search for
     * @return list of matching records
     */
    List<MongoScheduledContextInstanceRecordImpl> findByStatus(String status);

    /**
     * Find all by context name
     *
     * @param contextName the context name to search for
     * @return list of matching records
     */
    List<MongoScheduledContextInstanceRecordImpl> findByContextName(String contextName);

    /**
     * Find by context name and timestamp range
     *
     * @param contextName the context name
     * @param startTimestamp the start timestamp
     * @param endTimestamp the end timestamp
     * @return list of matching records
     */
    List<MongoScheduledContextInstanceRecordImpl> findByContextNameAndTimestampBetween(
            String contextName, long startTimestamp, long endTimestamp);
}
