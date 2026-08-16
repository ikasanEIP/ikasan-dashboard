package org.ikasan.mongo.persistence.scheduled.instance.repository;

import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceAuditAggregateRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for ScheduledContextInstanceAuditAggregate operations.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoScheduledContextInstanceAuditAggregateRepository extends MongoRepository<MongoScheduledContextInstanceAuditAggregateRecordImpl, String> {

    /**
     * Find all by context instance ID
     *
     * @param contextInstanceId the context instance ID
     * @return list of matching records
     */
    List<MongoScheduledContextInstanceAuditAggregateRecordImpl> findByContextInstanceId(String contextInstanceId);

    /**
     * Find all by context instance ID and is repeating job
     *
     * @param contextInstanceId the context instance ID
     * @param isRepeatingJob whether it's a repeating job
     * @return list of matching records
     */
    List<MongoScheduledContextInstanceAuditAggregateRecordImpl> findByContextInstanceIdAndIsRepeatingJob(
            String contextInstanceId, boolean isRepeatingJob);
}
