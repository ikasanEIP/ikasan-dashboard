package org.ikasan.mongo.persistence.error.reporting.dao;

import org.ikasan.mongo.persistence.error.reporting.model.MongoErrorOccurrence;
import org.ikasan.mongo.persistence.error.reporting.repository.MongoErrorOccurrenceRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MongoErrorReportingServiceDaoImpl implements EntityDao<ErrorOccurrence> {
    private static final Logger logger = LoggerFactory.getLogger(MongoErrorReportingServiceDaoImpl.class);

    private final MongoErrorOccurrenceRepository repository;
    private final MongoTemplate mongoTemplate;
    private final JsonMapper objectMapper;
    private final int daysToKeep;

    public MongoErrorReportingServiceDaoImpl(MongoErrorOccurrenceRepository repository
        , MongoTemplate mongoTemplate, int daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
        this.objectMapper = JsonMapper.builder().build();
    }

    public void save(ErrorOccurrence errorOccurrence) {
        MongoErrorOccurrence entity = convertToEntity(errorOccurrence);
        entity.setType(ERROR);
        repository.save(entity);
        logger.debug("Saved error occurrence with URI: {}", errorOccurrence.getUri());
    }

    public void save(List<ErrorOccurrence> errorOccurrences) {
        List<MongoErrorOccurrence> entities = errorOccurrences.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());
        entities.forEach(entity -> entity.setType(ERROR));
        repository.saveAll(entities);
        logger.debug("Saved {} error occurrences", errorOccurrences.size());
    }

    public ErrorOccurrence findByUri(String uri) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ERROR_URI).is(uri));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(ERROR));

        MongoErrorOccurrence entity = mongoTemplate.findOne(query, MongoErrorOccurrence.class);
        return entity;
    }

    public List<ErrorOccurrence> findByModuleName(String moduleName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(ERROR));

        List<MongoErrorOccurrence> entities = mongoTemplate.find(query, MongoErrorOccurrence.class);
        return entities.stream()
            .map(entity -> (ErrorOccurrence)entity)
            .collect(Collectors.toList());
    }

    public List<ErrorOccurrence> findByModuleNameAndFlowName(String moduleName, String flowName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName));
        query.addCriteria(Criteria.where(EntityFields.FLOW_NAME).is(flowName));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(ERROR));

        List<MongoErrorOccurrence> entities = mongoTemplate.find(query, MongoErrorOccurrence.class);
        return entities.stream()
            .map(entity -> (ErrorOccurrence)entity)
            .collect(Collectors.toList());
    }

    public List<ErrorOccurrence> find(List<String> moduleNames, List<String> flowNames, List<String> componentNames,
                                      String searchString, long fromTimestamp, long untilTimestamp, int offset, int limit) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where(EntityFields.TYPE).is(ERROR));

        if (moduleNames != null && !moduleNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).in(moduleNames));
        }

        if (flowNames != null && !flowNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).in(flowNames));
        }

        if (componentNames != null && !componentNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.COMPONENT_NAME).in(componentNames));
        }

        if (searchString != null && !searchString.trim().isEmpty()) {
            String searchPattern = ".*" + searchString + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                Criteria.where(EntityFields.ERROR_DETAIL).regex(searchPattern, "i"),
                Criteria.where(EntityFields.ERROR_MESSAGE).regex(searchPattern, "i"),
                Criteria.where(EntityFields.ERROR_URI).regex(searchPattern, "i"),
                Criteria.where(EntityFields.EVENT).regex(searchPattern, "i")
            );
            criteriaList.add(searchCriteria);
        }

        if (fromTimestamp > 0 && untilTimestamp > 0) {
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(fromTimestamp).lte(untilTimestamp));
        } else if (fromTimestamp > 0) {
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(fromTimestamp));
        } else if (untilTimestamp > 0) {
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).lte(untilTimestamp));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        query.skip(offset).limit(limit);

        List<MongoErrorOccurrence> entities = mongoTemplate.find(query, MongoErrorOccurrence.class);
        return entities.stream()
            .map(entity -> (ErrorOccurrence)entity)
            .collect(Collectors.toList());
    }

    public void deleteByUri(String uri) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ERROR_URI).is(uri));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(ERROR));

        mongoTemplate.remove(query, MongoErrorOccurrence.class);
        logger.debug("Deleted error occurrence with URI: {}", uri);
    }

    /**
     * Converts an {@link ErrorOccurrence} object to a {@link MongoErrorOccurrence} object.
     *
     * @param errorOccurrence the {@link ErrorOccurrence} instance to be converted
     * @return the {@link MongoErrorOccurrence} representation of the input {@link ErrorOccurrence} instance
     * @throws RuntimeException if serialization of the {@link ErrorOccurrence} object to JSON fails
     */
    private MongoErrorOccurrence convertToEntity(ErrorOccurrence errorOccurrence) {
        // If already a MongoErrorOccurrence, set the ID and return it
        if (errorOccurrence instanceof MongoErrorOccurrence) {
            MongoErrorOccurrence mongoError = (MongoErrorOccurrence) errorOccurrence;
            String id = errorOccurrence.getModuleName() + "-error-" + errorOccurrence.getUri();
            mongoError.setId(id);
            return mongoError;
        }

        // Otherwise, create a new MongoErrorOccurrence and copy fields
        MongoErrorOccurrence entity = new MongoErrorOccurrence();

        String id = errorOccurrence.getModuleName() + "-error-" + errorOccurrence.getUri();
        entity.setId(id);
        entity.setUri(errorOccurrence.getUri());
        entity.setModuleName(errorOccurrence.getModuleName());
        entity.setFlowName(errorOccurrence.getFlowName());
        entity.setFlowElementName(errorOccurrence.getFlowElementName());
        entity.setAction(errorOccurrence.getAction());
        entity.setErrorDetail(errorOccurrence.getErrorDetail());
        entity.setErrorMessage(errorOccurrence.getErrorMessage());
        entity.setExceptionClass(errorOccurrence.getExceptionClass());
        entity.setEventLifeIdentifier(errorOccurrence.getEventLifeIdentifier());
        entity.setEventRelatedIdentifier(errorOccurrence.getEventRelatedIdentifier());

        if (errorOccurrence.getEvent() != null) {
            entity.setEventAsString(new String((byte[]) errorOccurrence.getEvent()));
        }

        entity.setTimestamp(errorOccurrence.getTimestamp());
        entity.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());

        return entity;
    }
}
