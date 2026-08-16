package org.ikasan.mongo.persistence.wiretap.repository;

import org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for WiretapEvent.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoWiretapEventRepository extends MongoRepository<MongoWiretapEventImpl, String> {

    /**
     * Find wiretap events by module name.
     *
     * @param moduleName the module name
     * @return list of wiretap events
     */
    List<MongoWiretapEventImpl> findByModuleName(String moduleName);

    /**
     * Find wiretap events by flow name.
     *
     * @param flowName the flow name
     * @return list of wiretap events
     */
    List<MongoWiretapEventImpl> findByFlowName(String flowName);

    /**
     * Find wiretap events by component name.
     *
     * @param componentName the component name
     * @return list of wiretap events
     */
    List<MongoWiretapEventImpl> findByComponentName(String componentName);

    /**
     * Find wiretap events by event ID.
     *
     * @param eventId the event ID
     * @return list of wiretap events
     */
    List<MongoWiretapEventImpl> findByEventId(String eventId);

    /**
     * Find wiretap events by module name and flow name.
     *
     * @param moduleName the module name
     * @param flowName the flow name
     * @return list of wiretap events
     */
    List<MongoWiretapEventImpl> findByModuleNameAndFlowName(String moduleName, String flowName);

    /**
     * Find wiretap events by module name, flow name and component name.
     *
     * @param moduleName the module name
     * @param flowName the flow name
     * @param componentName the component name
     * @return list of wiretap events
     */
    List<MongoWiretapEventImpl> findByModuleNameAndFlowNameAndComponentName(
            String moduleName, String flowName, String componentName);

    /**
     * Find wiretap events by timestamp range.
     *
     * @param start start timestamp
     * @param end end timestamp
     * @return list of wiretap events
     */
    List<MongoWiretapEventImpl> findByTimestampBetween(long start, long end);

    /**
     * Delete wiretap events that have expired.
     *
     * @param currentTime the current time in milliseconds
     */
    void deleteByExpiryLessThan(long currentTime);
}
