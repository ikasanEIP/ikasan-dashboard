package org.ikasan.mongo.persistence.scheduled.job.repository;

import org.ikasan.mongo.persistence.scheduled.job.model.MongoSchedulerJobRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@DependsOn("mongoTemplate")
public interface MongoSchedulerJobRepository extends MongoRepository<MongoSchedulerJobRecordImpl, String> {

    /**
     * Find all jobs by context name
     * @param contextName the context name
     * @return list of job records
     */
    List<MongoSchedulerJobRecordImpl> findByContextName(String contextName);

    /**
     * Find all jobs by agent name
     * @param agentName the agent name
     * @return list of job records
     */
    List<MongoSchedulerJobRecordImpl> findByAgentName(String agentName);

    /**
     * Find job by context name and job name
     * @param contextName the context name
     * @param jobName the job name
     * @return the job record
     */
    MongoSchedulerJobRecordImpl findByContextNameAndJobName(String contextName, String jobName);

    /**
     * Delete all jobs by agent name
     * @param agentName the agent name
     */
    void deleteByAgentName(String agentName);

    /**
     * Delete all jobs by context name
     * @param contextName the context name
     */
    void deleteByContextName(String contextName);

    /**
     * Get all distinct agent names
     * @return list of agent names
     */
    List<String> findDistinctAgentNameBy();
}
