package org.ikasan.mongo.persistence.systemevent.repository;

import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for SystemEvent.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoSystemEventRepository extends MongoRepository<MongoSystemEventImpl, String> {

    /**
     * Find system event by module name and system event id
     *
     * @param moduleName the module name
     * @param id the id
     * @return optional system event
     */
    Optional<MongoSystemEventImpl> findByModuleNameAndId(String moduleName, String id);

    /**
     * Find system events by actor (case-insensitive contains)
     *
     * @param actor the actor
     * @return list of system events
     */
    List<MongoSystemEventImpl> findByActorContainingIgnoreCase(String actor);

    /**
     * Find system events by subject (case-insensitive contains)
     *
     * @param subject the subject
     * @return list of system events
     */
    List<MongoSystemEventImpl> findBySubjectContainingIgnoreCase(String subject);

    /**
     * Find system events by action (case-insensitive contains)
     *
     * @param action the action
     * @return list of system events
     */
    List<MongoSystemEventImpl> findByActionContainingIgnoreCase(String action);

    /**
     * Find system events by timestamp between two dates
     *
     * @param start the start date
     * @param end the end date
     * @return list of system events
     */
    List<MongoSystemEventImpl> findByTimestampBetween(Date start, Date end);

    /**
     * Delete system events with expiry less than the current time
     *
     * @param currentTime the current time
     */
    void deleteByExpiryLessThan(Date currentTime);
}
