package org.ikasan.mongo.persistence.error.reporting.dao;

import org.ikasan.mongo.persistence.error.reporting.model.MongoErrorOccurrence;
import org.ikasan.mongo.persistence.error.reporting.repository.MongoErrorOccurrenceRepository;
import org.ikasan.spec.entity.EntityDao;
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
import java.util.stream.Collectors;

public class MongoErrorReportingServiceDaoImpl implements EntityDao<ErrorOccurrence> {
    private static final Logger logger = LoggerFactory.getLogger(MongoErrorReportingServiceDaoImpl.class);

    private final MongoErrorOccurrenceRepository repository;
    private final MongoTemplate mongoTemplate;
    private final JsonMapper objectMapper;

    public MongoErrorReportingServiceDaoImpl(MongoErrorOccurrenceRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = JsonMapper.builder().build();
    }

    public void save(ErrorOccurrence errorOccurrence) {
        MongoErrorOccurrence entity = convertToEntity(errorOccurrence);
        repository.save(entity);
        logger.debug("Saved error occurrence with URI: {}", errorOccurrence.getUri());
    }

    public void save(List<ErrorOccurrence> errorOccurrences) {
        List<MongoErrorOccurrence> entities = errorOccurrences.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} error occurrences", errorOccurrences.size());
    }

    public ErrorOccurrence findByUri(String uri) {
        Optional<MongoErrorOccurrence> entity = repository.findByErrorUri(uri);
        if (entity.isPresent()) {
            return (ErrorOccurrence) entity.get();
        }
        return null;
    }

    public List<ErrorOccurrence> findByModuleName(String moduleName) {
        List<MongoErrorOccurrence> entities = repository.findByModuleName(moduleName);
        return entities.stream()
            .map(entity -> (ErrorOccurrence)entity)
            .collect(Collectors.toList());
    }

    public List<ErrorOccurrence> findByModuleNameAndFlowName(String moduleName, String flowName) {
        List<MongoErrorOccurrence> entities = repository.findByModuleNameAndFlowName(moduleName, flowName);
        return entities.stream()
            .map(entity -> (ErrorOccurrence)entity)
            .collect(Collectors.toList());
    }

    public List<ErrorOccurrence> find(List<String> moduleNames, List<String> flowNames, List<String> componentNames,
                                      String searchString, long fromTimestamp, long untilTimestamp, int offset, int limit) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (moduleNames != null && !moduleNames.isEmpty()) {
            criteriaList.add(Criteria.where("module_name").in(moduleNames));
        }

        if (flowNames != null && !flowNames.isEmpty()) {
            criteriaList.add(Criteria.where("flow_name").in(flowNames));
        }

        if (componentNames != null && !componentNames.isEmpty()) {
            criteriaList.add(Criteria.where("component_name").in(componentNames));
        }

        if (searchString != null && !searchString.trim().isEmpty()) {
            String searchPattern = ".*" + searchString + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                Criteria.where("error_detail").regex(searchPattern, "i"),
                Criteria.where("error_message").regex(searchPattern, "i"),
                Criteria.where("error_uri").regex(searchPattern, "i"),
                Criteria.where("event_life_identifier").regex(searchPattern, "i")
            );
            criteriaList.add(searchCriteria);
        }

        if (fromTimestamp > 0 && untilTimestamp > 0) {
            criteriaList.add(Criteria.where("timestamp").gte(fromTimestamp).lte(untilTimestamp));
        } else if (fromTimestamp > 0) {
            criteriaList.add(Criteria.where("timestamp").gte(fromTimestamp));
        } else if (untilTimestamp > 0) {
            criteriaList.add(Criteria.where("timestamp").lte(untilTimestamp));
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
        Optional<MongoErrorOccurrence> entity = repository.findByErrorUri(uri);
        entity.ifPresent(repository::delete);
        logger.debug("Deleted error occurrence with URI: {}", uri);
    }

    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Removed expired error occurrences before timestamp: {}", currentTime);
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
            mongoError.setCreatedTimestamp(System.currentTimeMillis());
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
        entity.setUserAction(errorOccurrence.getUserAction());
        entity.setActionedBy(errorOccurrence.getActionedBy());
        entity.setUserActionTimestamp(errorOccurrence.getUserActionTimestamp());
        entity.setExpiry(errorOccurrence.getExpiry());
        entity.setHarvested(((MongoErrorOccurrence)errorOccurrence).isHarvested());
        entity.setCreatedTimestamp(System.currentTimeMillis());

        return entity;
    }
}
