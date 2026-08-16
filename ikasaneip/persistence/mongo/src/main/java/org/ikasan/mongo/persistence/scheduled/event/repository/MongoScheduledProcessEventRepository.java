package org.ikasan.mongo.persistence.scheduled.event.repository;

import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for ScheduledProcessEvent.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoScheduledProcessEventRepository extends MongoRepository<ContextualisedScheduledProcessEventImpl, Long> {

    /**
     * Find scheduled process events by agent name
     *
     * @param agentName the agent name
     * @return list of scheduled process events
     */
    List<ContextualisedScheduledProcessEventImpl> findByAgentName(String agentName);

    /**
     * Find scheduled process events by job group
     *
     * @param jobGroup the job group
     * @return list of scheduled process events
     */
    List<ContextualisedScheduledProcessEventImpl> findByJobGroup(String jobGroup);

    /**
     * Find scheduled process events by job name
     *
     * @param jobName the job name
     * @return list of scheduled process events
     */
    List<ContextualisedScheduledProcessEventImpl> findByJobName(String jobName);

    /**
     * Find scheduled process events by agent name and fire time between start and end
     *
     * @param agentName the agent name
     * @param startTime the start time
     * @param endTime the end time
     * @return list of scheduled process events
     */
    List<ContextualisedScheduledProcessEventImpl> findByAgentNameAndFireTimeBetween(String agentName, long startTime, long endTime);

    /**
     * Find scheduled process events by agent names and fire time between start and end
     *
     * @param agentNames the list of agent names
     * @param startTime the start time
     * @param endTime the end time
     * @return list of scheduled process events
     */
    List<ContextualisedScheduledProcessEventImpl> findByAgentNameInAndFireTimeBetween(List<String> agentNames, long startTime, long endTime);

    /**
     * Find scheduled process events by successful status
     *
     * @param successful the successful flag
     * @return list of scheduled process events
     */
    List<ContextualisedScheduledProcessEventImpl> findBySuccessful(boolean successful);

    /**
     * Find scheduled process events by fire time between start and end
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return list of scheduled process events
     */
    List<ContextualisedScheduledProcessEventImpl> findByFireTimeBetween(long startTime, long endTime);

    /**
     * Find all distinct agent names
     *
     * @return list of distinct agent names
     */
    @Query(value = "{}", fields = "{ 'agentName' : 1 }")
    List<ContextualisedScheduledProcessEventImpl> findAllProjectedBy();

    /**
     * Delete scheduled process events older than a certain timestamp
     * Since the model doesn't have an expiry field, we delete by fireTime
     *
     * @param timestamp the timestamp threshold
     */
    void deleteByFireTimeLessThan(long timestamp);
}
