package org.ikasan.mongo.persistence.exclusion.dao;

import org.ikasan.mongo.persistence.exclusion.model.MongoExclusionEvent;
import org.ikasan.mongo.persistence.exclusion.repository.MongoExclusionEventRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ExclusionEvent DAO.
 *
 * @author Ikasan Development Team
 */
public class MongoExclusionEventDao implements EntityDao<ExclusionEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoExclusionEventDao.class);

    private final MongoExclusionEventRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoExclusionEventDao(MongoExclusionEventRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public void save(ExclusionEvent exclusionEvent) {
        MongoExclusionEvent entity = convertToEntity(exclusionEvent);
        repository.save(entity);
        logger.debug("Saved exclusion event with error URI: {}", exclusionEvent.getErrorUri());
    }

    public void save(List<ExclusionEvent> exclusionEvents) {
        List<MongoExclusionEvent> entities = exclusionEvents.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} exclusion events", exclusionEvents.size());
    }

    public ExclusionEvent findByErrorUri(String errorUri) {
        Optional<MongoExclusionEvent> entity = repository.findByErrorUri(errorUri);
        return entity.orElse(null);
    }

    public List<ExclusionEvent> findByModuleName(String moduleName) {
        List<MongoExclusionEvent> entities = repository.findByModuleName(moduleName);
        return entities.stream()
                .map(entity -> (ExclusionEvent) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEvent> findByModuleNameAndFlowName(String moduleName, String flowName) {
        List<MongoExclusionEvent> entities = repository.findByModuleNameAndFlowName(moduleName, flowName);
        return entities.stream()
                .map(entity -> (ExclusionEvent) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEvent> find(List<String> moduleNames, List<String> flowNames, String searchString,
                                     long fromTimestamp, long untilTimestamp, int offset, int limit) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (moduleNames != null && !moduleNames.isEmpty()) {
            criteriaList.add(Criteria.where("module_name").in(moduleNames));
        }

        if (flowNames != null && !flowNames.isEmpty()) {
            criteriaList.add(Criteria.where("flow_name").in(flowNames));
        }

        if (searchString != null && !searchString.trim().isEmpty()) {
            String searchPattern = ".*" + searchString + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("event").regex(searchPattern, "i"),
                    Criteria.where("error_uri").regex(searchPattern, "i"),
                    Criteria.where("identifier").regex(searchPattern, "i")
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

        List<MongoExclusionEvent> entities = mongoTemplate.find(query, MongoExclusionEvent.class);
        return entities.stream()
                .map(entity -> (ExclusionEvent) entity)
                .collect(Collectors.toList());
    }

    public void deleteByErrorUri(String errorUri) {
        Optional<MongoExclusionEvent> entity = repository.findByErrorUri(errorUri);
        entity.ifPresent(repository::delete);
        logger.debug("Deleted exclusion event with error URI: {}", errorUri);
    }

    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Removed expired exclusion events before timestamp: {}", currentTime);
    }

    /**
     * Converts an {@link ExclusionEvent} object to a {@link MongoExclusionEvent} object.
     *
     * @param exclusionEvent the {@link ExclusionEvent} instance to be converted
     * @return the {@link MongoExclusionEvent} representation of the input {@link ExclusionEvent} instance
     */
    private MongoExclusionEvent convertToEntity(ExclusionEvent exclusionEvent) {
        // If already a MongoExclusionEvent, set the ID and return it
        if (exclusionEvent instanceof MongoExclusionEvent) {
            MongoExclusionEvent mongoExclusion = (MongoExclusionEvent) exclusionEvent;
            String id = exclusionEvent.getModuleName() + ":exclusion:" + exclusionEvent.getErrorUri();
            mongoExclusion.setId(id);
            mongoExclusion.setCreatedTimestamp(System.currentTimeMillis());
            return mongoExclusion;
        }

        // Otherwise, create a new MongoExclusionEvent and copy fields
        MongoExclusionEvent entity = new MongoExclusionEvent();

        String id = exclusionEvent.getModuleName() + ":exclusion:" + exclusionEvent.getErrorUri();
        entity.setId(id);
        entity.setModuleName(exclusionEvent.getModuleName());
        entity.setFlowName(exclusionEvent.getFlowName());
        entity.setIdentifier(exclusionEvent.getIdentifier());
        entity.setEvent(exclusionEvent.getEvent());
        entity.setTimestamp(exclusionEvent.getTimestamp());
        entity.setErrorUri(exclusionEvent.getErrorUri());
        entity.setHarvested(exclusionEvent.isHarvested());
        entity.setCreatedTimestamp(System.currentTimeMillis());

        return entity;
    }
}
