package org.ikasan.mongo.persistence.scheduled.job.repository;

import org.ikasan.mongo.persistence.scheduled.job.model.MongoQuartzScheduleDrivenJobRecordImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@DependsOn("mongoTemplate")
public interface MongoQuartzScheduleDrivenJobRepository extends MongoRepository<MongoQuartzScheduleDrivenJobRecordImpl, String> {

    /**
     * Find all jobs by context name
     * @param contextName the context name
     * @return list of job records
     */
    List<MongoQuartzScheduleDrivenJobRecordImpl> findByContextName(String contextName);
}
