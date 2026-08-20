package org.ikasan.mongo.persistence.metrics.dao;

import org.ikasan.mongo.persistence.metrics.model.FlowInvocationMetricImpl;
import org.ikasan.mongo.persistence.metrics.model.MongoFlowInvocationMetric;
import org.ikasan.mongo.persistence.metrics.repository.MongoFlowInvocationMetricRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.PAYLOAD_CONTENT;

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

    private final JsonMapper mapper;

    public MongoMetricsDaoImpl(MongoFlowInvocationMetricRepository repository, MongoTemplate mongoTemplate, int metricsQueryLimit) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.metricsQueryLimit = metricsQueryLimit;

        this.mapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
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
        query.addCriteria(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(startTime).lte(endTime));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(METRIC_ENTITY_TYPE));
        query.skip(offset).limit(limit);

        List<MongoFlowInvocationMetric> entities = mongoTemplate.find(query, MongoFlowInvocationMetric.class);
        return entities.stream()
                .map(this::convertFromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count(long startTime, long endTime) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(startTime).lte(endTime));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(METRIC_ENTITY_TYPE));

        return mongoTemplate.count(query, MongoFlowInvocationMetric.class);
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
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName)
                .and(EntityFields.CREATED_DATE_TIME).gte(startTime).lte(endTime)
                .and(EntityFields.TYPE).is(METRIC_ENTITY_TYPE));
        query.skip(offset).limit(limit);

        List<MongoFlowInvocationMetric> entities = mongoTemplate.find(query, MongoFlowInvocationMetric.class);
        return entities.stream()
                .map(this::convertFromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String moduleName, long startTime, long endTime) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName)
                .and(EntityFields.CREATED_DATE_TIME).gte(startTime).lte(endTime)
                .and(EntityFields.TYPE).is(METRIC_ENTITY_TYPE));

        return mongoTemplate.count(query, MongoFlowInvocationMetric.class);
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
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName)
                .and(EntityFields.FLOW_NAME).is(flowName)
                .and(EntityFields.CREATED_DATE_TIME).gte(startTime).lte(endTime)
                .and(EntityFields.TYPE).is(METRIC_ENTITY_TYPE));
        query.skip(offset).limit(limit);

        List<MongoFlowInvocationMetric> entities = mongoTemplate.find(query, MongoFlowInvocationMetric.class);
        return entities.stream()
                .map(this::convertFromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String moduleName, String flowName, long startTime, long endTime) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName)
                .and(EntityFields.FLOW_NAME).is(flowName)
                .and(EntityFields.CREATED_DATE_TIME).gte(startTime).lte(endTime)
                .and(EntityFields.TYPE).is(METRIC_ENTITY_TYPE));

        return mongoTemplate.count(query, MongoFlowInvocationMetric.class);
    }

    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.EXPIRY).lt(currentTime));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(METRIC_ENTITY_TYPE));

        mongoTemplate.remove(query, MongoFlowInvocationMetric.class);
        logger.debug("Removed expired flow invocation metrics before timestamp: {}", currentTime);
    }

    /**
     * Converts a {@link FlowInvocationMetric} object to a {@link MongoFlowInvocationMetric} object.
     *
     * @param flowInvocationMetric the {@link FlowInvocationMetric} instance to be converted
     * @return the {@link MongoFlowInvocationMetric} representation
     */
    private MongoFlowInvocationMetric convertToEntity(FlowInvocationMetric flowInvocationMetric) {

        MongoFlowInvocationMetric entity = new MongoFlowInvocationMetric();

        String id = flowInvocationMetric.getModuleName() + "-metric-" + UUID.randomUUID();
        entity.setId(id);
        entity.setType(METRIC_ENTITY_TYPE);
        entity.setModuleName(flowInvocationMetric.getModuleName());
        entity.setFlowName(flowInvocationMetric.getFlowName());
        entity.setTimestamp(System.currentTimeMillis());
        flowInvocationMetric.setHarvestedDateTime(System.currentTimeMillis());
        try {
            entity.setRawFlowInvocationMetric(this.mapper.writeValueAsString(flowInvocationMetric));
        }
        catch (JacksonException e) {
            logger.warn(String.format("Could not set metric payload content[%s]", flowInvocationMetric), e);
        }
        entity.setExpiry(flowInvocationMetric.getExpiry());

        return entity;
    }

    /**
     * Converts a {@link MongoFlowInvocationMetric} object to a {@link FlowInvocationMetric} object.
     *
     * @param entity the {@link MongoFlowInvocationMetric} instance to be converted
     * @return the {@link FlowInvocationMetric} representation
     */
    private FlowInvocationMetric convertFromEntity(MongoFlowInvocationMetric entity) {
        try
        {
            FlowInvocationMetric solrModuleMetaData
                = mapper.readValue(entity.getRawFlowInvocationMetric(), FlowInvocationMetricImpl.class);

            return solrModuleMetaData;
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Unable to deserialise FlowInvocationMetric [%s]"
                , entity.getRawFlowInvocationMetric()), e);
        }
    }
}
