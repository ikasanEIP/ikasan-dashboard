package org.ikasan.mongo.persistence.hospital.dao;

import org.ikasan.mongo.persistence.hospital.model.MongoExclusionEventAction;
import org.ikasan.mongo.persistence.hospital.repository.MongoExclusionEventActionRepository;
import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
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
 * MongoDB implementation of Hospital DAO for ExclusionEventAction.
 *
 * @author Ikasan Development Team
 */
public class MongoHospitalDao implements EsbEntityDao<ExclusionEventAction> {

    private static final Logger logger = LoggerFactory.getLogger(MongoHospitalDao.class);

    private final MongoExclusionEventActionRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoHospitalDao(MongoExclusionEventActionRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public void save(ExclusionEventAction exclusionEventAction) {
        MongoExclusionEventAction entity = convertToEntity(exclusionEventAction);
        repository.save(entity);
        logger.debug("Saved exclusion event action with error URI: {}", exclusionEventAction.getErrorUri());
    }

    public void save(List<ExclusionEventAction> exclusionEventActions) {
        List<MongoExclusionEventAction> entities = exclusionEventActions.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} exclusion event actions", exclusionEventActions.size());
    }

    public ExclusionEventAction findByErrorUri(String errorUri) {
        Optional<MongoExclusionEventAction> entity = repository.findByErrorUri(errorUri);
        return entity.orElse(null);
    }

    public List<ExclusionEventAction> findByModuleName(String moduleName) {
        List<MongoExclusionEventAction> entities = repository.findByModuleName(moduleName);
        return entities.stream()
                .map(entity -> (ExclusionEventAction) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEventAction> findByModuleNameAndFlowName(String moduleName, String flowName) {
        List<MongoExclusionEventAction> entities = repository.findByModuleNameAndFlowName(moduleName, flowName);
        return entities.stream()
                .map(entity -> (ExclusionEventAction) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEventAction> find(List<String> moduleNames, List<String> flowNames, String searchString,
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
                    Criteria.where("comment").regex(searchPattern, "i"),
                    Criteria.where("actioned_by").regex(searchPattern, "i")
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

        List<MongoExclusionEventAction> entities = mongoTemplate.find(query, MongoExclusionEventAction.class);
        return entities.stream()
                .map(entity -> (ExclusionEventAction) entity)
                .collect(Collectors.toList());
    }

    public void deleteByErrorUri(String errorUri) {
        Optional<MongoExclusionEventAction> entity = repository.findByErrorUri(errorUri);
        entity.ifPresent(repository::delete);
        logger.debug("Deleted exclusion event action with error URI: {}", errorUri);
    }

    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Removed expired exclusion event actions before timestamp: {}", currentTime);
    }

    /**
     * Converts an {@link ExclusionEventAction} object to a {@link MongoExclusionEventAction} object.
     *
     * @param exclusionEventAction the {@link ExclusionEventAction} instance to be converted
     * @return the {@link MongoExclusionEventAction} representation of the input {@link ExclusionEventAction} instance
     */
    private MongoExclusionEventAction convertToEntity(ExclusionEventAction exclusionEventAction) {
        // If already a MongoExclusionEventAction, set the ID and return it
        if (exclusionEventAction instanceof MongoExclusionEventAction) {
            MongoExclusionEventAction mongoAction = (MongoExclusionEventAction) exclusionEventAction;
            mongoAction.setId(exclusionEventAction.getErrorUri());
            mongoAction.setCreatedTimestamp(System.currentTimeMillis());
            return mongoAction;
        }

        // Otherwise, create a new MongoExclusionEventAction and copy fields
        MongoExclusionEventAction entity = new MongoExclusionEventAction();

        entity.setId(exclusionEventAction.getErrorUri());
        entity.setModuleName(exclusionEventAction.getModuleName());
        entity.setFlowName(exclusionEventAction.getFlowName());
        entity.setErrorUri(exclusionEventAction.getErrorUri());
        entity.setActionedBy(exclusionEventAction.getActionedBy());
        entity.setAction(exclusionEventAction.getAction());
        entity.setEvent((String) exclusionEventAction.getEvent());
        entity.setTimestamp(exclusionEventAction.getTimestamp());
        entity.setComment(exclusionEventAction.getComment());
        entity.setHarvested(((MongoExclusionEventAction)exclusionEventAction).isHarvested());
        entity.setCreatedTimestamp(System.currentTimeMillis());

        return entity;
    }
}
