package org.ikasan.mongo.persistence.metrics.repository;

import org.ikasan.mongo.persistence.metrics.model.MongoFlowInvocationMetric;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@DependsOn("mongoTemplate")
public interface MongoFlowInvocationMetricRepository extends MongoRepository<MongoFlowInvocationMetric, String> {

    List<MongoFlowInvocationMetric> findByModuleName(String moduleName);

    List<MongoFlowInvocationMetric> findByModuleNameAndFlowName(String moduleName, String flowName);

    List<MongoFlowInvocationMetric> findByExpiryLessThan(long currentTime);

    void deleteByExpiryLessThan(long currentTime);

    @Query(value = "{ 'invocation_start_time': { $gte: ?0, $lte: ?1 } }", count = true)
    long countByInvocationStartTimeBetween(long startTime, long endTime);

    @Query(value = "{ 'module_name': ?0, 'invocation_start_time': { $gte: ?1, $lte: ?2 } }", count = true)
    long countByModuleNameAndInvocationStartTimeBetween(String moduleName, long startTime, long endTime);

    @Query(value = "{ 'module_name': ?0, 'flow_name': ?1, 'invocation_start_time': { $gte: ?2, $lte: ?3 } }", count = true)
    long countByModuleNameAndFlowNameAndInvocationStartTimeBetween(String moduleName, String flowName, long startTime, long endTime);
}
