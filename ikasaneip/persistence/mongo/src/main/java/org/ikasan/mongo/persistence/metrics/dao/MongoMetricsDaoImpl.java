package org.ikasan.mongo.persistence.metrics.dao;

import org.ikasan.mongo.persistence.metrics.model.MongoFlowInvocationMetric;
import org.ikasan.mongo.persistence.metrics.repository.MongoFlowInvocationMetricRepository;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of MetricsDao.
 *
 * @author Ikasan Development Team
 */
public class MongoMetricsDaoImpl implements MetricsDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoMetricsDaoImpl.class);

    private final MongoFlowInvocationMetricRepository repository;
    private final MongoTemplate mongoTemplate;
    private final int metricsQueryLimit;

    public MongoMetricsDaoImpl(MongoFlowInvocationMetricRepository repository, MongoTemplate mongoTemplate, int metricsQueryLimit) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.metricsQueryLimit = metricsQueryLimit;
    }

    @Override
    public void save(FlowInvocationMetric flowInvocationMetric) {
        MongoFlowInvocationMetric entity = convertToEntity(flowInvocationMetric);
        repository.save(entity);
        logger.debug("Saved flow invocation metric for module: {}, flow: {}",
                flowInvocationMetric.getModuleName(), flowInvocationMetric.getFlowName());
    }

    @Override
    public void save(List<FlowInvocationMetric> flowInvocationMetrics) {
        List<MongoFlowInvocationMetric> entities = flowInvocationMetrics.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} flow invocation metrics", flowInvocationMetrics.size());
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime) {
        return getMetrics(startTime, endTime, 0, metricsQueryLimit);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime, int offset, int limit) {
        if (limit > metricsQueryLimit) {
            throw new RuntimeException(String.format(
                    "Error resolving MongoDB flow invocation metrics! The limit on the query[%s] exceeds the maximum limit configured [%s]. " +
                    "The limit can be increased by setting configuration property 'mongo.metrics.query.limit'.",
                    limit, metricsQueryLimit));
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("invocation_start_time").gte(startTime).lte(endTime));
        query.skip(offset).limit(limit);

        List<MongoFlowInvocationMetric> entities = mongoTemplate.find(query, MongoFlowInvocationMetric.class);
        return entities.stream()
                .map(this::convertFromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count(long startTime, long endTime) {
        return repository.countByInvocationStartTimeBetween(startTime, endTime);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime) {
        return getMetrics(moduleName, startTime, endTime, 0, metricsQueryLimit);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime, int offset, int limit) {
        if (limit > metricsQueryLimit) {
            throw new RuntimeException(String.format(
                    "Error resolving MongoDB flow invocation metrics! The limit on the query[%s] exceeds the maximum limit configured [%s]. " +
                    "The limit can be increased by setting configuration property 'mongo.metrics.query.limit'.",
                    limit, metricsQueryLimit));
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("module_name").is(moduleName)
                .and("invocation_start_time").gte(startTime).lte(endTime));
        query.skip(offset).limit(limit);

        List<MongoFlowInvocationMetric> entities = mongoTemplate.find(query, MongoFlowInvocationMetric.class);
        return entities.stream()
                .map(this::convertFromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String moduleName, long startTime, long endTime) {
        return repository.countByModuleNameAndInvocationStartTimeBetween(moduleName, startTime, endTime);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime) {
        return getMetrics(moduleName, flowName, startTime, endTime, 0, metricsQueryLimit);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime, int offset, int limit) {
        if (limit > metricsQueryLimit) {
            throw new RuntimeException(String.format(
                    "Error resolving MongoDB flow invocation metrics! The limit on the query[%s] exceeds the maximum limit configured [%s]. " +
                    "The limit can be increased by setting configuration property 'mongo.metrics.query.limit'.",
                    limit, metricsQueryLimit));
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("module_name").is(moduleName)
                .and("flow_name").is(flowName)
                .and("invocation_start_time").gte(startTime).lte(endTime));
        query.skip(offset).limit(limit);

        List<MongoFlowInvocationMetric> entities = mongoTemplate.find(query, MongoFlowInvocationMetric.class);
        return entities.stream()
                .map(this::convertFromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String moduleName, String flowName, long startTime, long endTime) {
        return repository.countByModuleNameAndFlowNameAndInvocationStartTimeBetween(moduleName, flowName, startTime, endTime);
    }

    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Removed expired flow invocation metrics before timestamp: {}", currentTime);
    }

    /**
     * Converts a {@link FlowInvocationMetric} object to a {@link MongoFlowInvocationMetric} object.
     *
     * @param flowInvocationMetric the {@link FlowInvocationMetric} instance to be converted
     * @return the {@link MongoFlowInvocationMetric} representation
     */
    private MongoFlowInvocationMetric convertToEntity(FlowInvocationMetric flowInvocationMetric) {
        if (flowInvocationMetric instanceof MongoFlowInvocationMetric) {
            MongoFlowInvocationMetric mongoMetric = (MongoFlowInvocationMetric) flowInvocationMetric;
            if (mongoMetric.getId() == null) {
                String id = flowInvocationMetric.getModuleName() + "-metric-" + UUID.randomUUID();
                mongoMetric.setId(id);
            }
            if (mongoMetric.getCreatedTimestamp() == 0) {
                mongoMetric.setCreatedTimestamp(System.currentTimeMillis());
            }
            if (mongoMetric.getHarvestedDateTime() == 0) {
                mongoMetric.setHarvestedDateTime(System.currentTimeMillis());
            }
            return mongoMetric;
        }

        MongoFlowInvocationMetric entity = new MongoFlowInvocationMetric();

        String id = flowInvocationMetric.getModuleName() + "-metric-" + UUID.randomUUID();
        entity.setId(id);
        entity.setModuleName(flowInvocationMetric.getModuleName());
        entity.setFlowName(flowInvocationMetric.getFlowName());
        entity.setInvocationStartTime(flowInvocationMetric.getInvocationStartTime());
        entity.setInvocationEndTime(flowInvocationMetric.getInvocationEndTime());
        entity.setFinalAction(flowInvocationMetric.getFinalAction());
        entity.setErrorUri(flowInvocationMetric.getErrorUri());
        entity.setHarvested(flowInvocationMetric.getHarvested());
        entity.setExpiry(flowInvocationMetric.getExpiry());

        long currentTime = System.currentTimeMillis();
        entity.setHarvestedDateTime(flowInvocationMetric.getHarvestedDateTime() > 0
                ? flowInvocationMetric.getHarvestedDateTime()
                : currentTime);
        entity.setCreatedTimestamp(currentTime);

        return entity;
    }

    /**
     * Converts a {@link MongoFlowInvocationMetric} object to a {@link FlowInvocationMetric} object.
     *
     * @param entity the {@link MongoFlowInvocationMetric} instance to be converted
     * @return the {@link FlowInvocationMetric} representation
     */
    private FlowInvocationMetric convertFromEntity(MongoFlowInvocationMetric entity) {
        // MongoFlowInvocationMetric already implements FlowInvocationMetric,
        // so we can return the entity directly
        return entity;
    }
}
