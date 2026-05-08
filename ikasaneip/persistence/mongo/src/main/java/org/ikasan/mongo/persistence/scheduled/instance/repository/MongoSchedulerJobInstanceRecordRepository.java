package org.ikasan.mongo.persistence.scheduled.instance.repository;

import org.ikasan.mongo.persistence.scheduled.instance.model.MongoSchedulerJobInstanceRecordImpl;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for SchedulerJobInstanceRecord.
 */
@Repository
public interface MongoSchedulerJobInstanceRecordRepository extends MongoRepository<MongoSchedulerJobInstanceRecordImpl, String> {

    /**
     * Find all job instances by context instance id.
     *
     * @param contextInstanceId
     * @return
     */
    List<MongoSchedulerJobInstanceRecordImpl> findByContextInstanceId(String contextInstanceId);

    /**
     * Find all job instances by context name.
     *
     * @param contextName
     * @return
     */
    List<MongoSchedulerJobInstanceRecordImpl> findByContextName(String contextName);

    /**
     * Delete all job instances by context instance id.
     *
     * @param contextInstanceId
     */
    void deleteByContextInstanceId(String contextInstanceId);
}
