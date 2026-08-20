package org.ikasan.mongo.persistence.metrics.repository;

import org.ikasan.mongo.persistence.metrics.model.MongoFlowInvocationMetric;
import org.ikasan.spec.entity.EntityFields;
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

    @Query(value = "{ '" + EntityFields.CREATED_DATE_TIME + "': { $gte: ?0, $lte: ?1 } }", count = true)
    long countByTimestampBetween(long startTime, long endTime);

    @Query(value = "{ '" + EntityFields.MODULE_NAME + "': ?0, '" + EntityFields.CREATED_DATE_TIME + "': { $gte: ?1, $lte: ?2 } }", count = true)
    long countByModuleNameAndTimestampBetween(String moduleName, long startTime, long endTime);

    @Query(value = "{ '"+ EntityFields.MODULE_NAME +"': ?0, '"+ EntityFields.FLOW_NAME +"': ?1, '"+ EntityFields.CREATED_DATE_TIME +"': { $gte: ?2, $lte: ?3 } }", count = true)
    long countByModuleNameAndFlowNameAndTimestampBetween(String moduleName, String flowName, long startTime, long endTime);
}
