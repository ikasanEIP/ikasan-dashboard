package org.ikasan.mongo.persistence.hospital.dao;

import org.ikasan.mongo.persistence.hospital.model.MongoExclusionEventAction;
import org.ikasan.mongo.persistence.hospital.repository.MongoExclusionEventActionRepository;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of Hospital DAO for ExclusionEventAction.
 *
 * @author Ikasan Development Team
 */
public class MongoHospitalDaoImpl implements EntityDao<ExclusionEventAction> {

    private static final Logger logger = LoggerFactory.getLogger(MongoHospitalDaoImpl.class);

    private final MongoExclusionEventActionRepository repository;
    private final MongoTemplate mongoTemplate;
    private final int daysToKeep;

    /**
     * Constructs a new instance of {@code MongoHospitalDaoImpl}.
     *
     * @param repository the {@link MongoExclusionEventActionRepository} used to interact with MongoDB for exclusion event actions
     * @param mongoTemplate the {@link MongoTemplate} used for custom queries and database operations
     * @param daysToKeep the number of days to retain records in the database
     */
    public MongoHospitalDaoImpl(MongoExclusionEventActionRepository repository
        , MongoTemplate mongoTemplate, int daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
    }

    @Override
    public void save(ExclusionEventAction exclusionEventAction) {
        MongoExclusionEventAction entity = convertToEntity(exclusionEventAction);
        repository.save(entity);
        logger.debug("Saved exclusion event action with error URI: {}", exclusionEventAction.getErrorUri());
    }

    @Override
    public void save(List<ExclusionEventAction> exclusionEventActions) {
        List<MongoExclusionEventAction> entities = exclusionEventActions.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} exclusion event actions", exclusionEventActions.size());
    }


    public ExclusionEventAction findByErrorUri(String errorUri) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ERROR_URI).is(errorUri));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION_EVENT_ACTION));

        MongoExclusionEventAction entity = mongoTemplate.findOne(query, MongoExclusionEventAction.class);
        return entity;
    }

    public List<ExclusionEventAction> findByModuleName(String moduleName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION_EVENT_ACTION));

        List<MongoExclusionEventAction> entities = mongoTemplate.find(query, MongoExclusionEventAction.class);
        return entities.stream()
                .map(entity -> (ExclusionEventAction) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEventAction> findByModuleNameAndFlowName(String moduleName, String flowName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName));
        query.addCriteria(Criteria.where(EntityFields.FLOW_NAME).is(flowName));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION_EVENT_ACTION));

        List<MongoExclusionEventAction> entities = mongoTemplate.find(query, MongoExclusionEventAction.class);
        return entities.stream()
                .map(entity -> (ExclusionEventAction) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEventAction> find(List<String> moduleNames, List<String> flowNames, String searchString,
                                           long fromTimestamp, long untilTimestamp, int offset, int limit) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where(EntityFields.TYPE).is(EXCLUSION_EVENT_ACTION));

        if (moduleNames != null && !moduleNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).in(moduleNames));
        }

        if (flowNames != null && !flowNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).in(flowNames));
        }

        if (searchString != null && !searchString.trim().isEmpty()) {
            String searchPattern = ".*" + searchString + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where(EntityFields.PAYLOAD_CONTENT).regex(searchPattern, "i"),
                    Criteria.where(EntityFields.ERROR_URI).regex(searchPattern, "i"),
                    Criteria.where(EntityFields.ACTOR).regex(searchPattern, "i")
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

        List<MongoExclusionEventAction> entities = mongoTemplate.find(query, MongoExclusionEventAction.class);
        return entities.stream()
                .map(entity -> (ExclusionEventAction) entity)
                .collect(Collectors.toList());
    }

    public void deleteByErrorUri(String errorUri) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ERROR_URI).is(errorUri));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION_EVENT_ACTION));

        mongoTemplate.remove(query, MongoExclusionEventAction.class);
        logger.debug("Deleted exclusion event action with error URI: {}", errorUri);
    }

    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.EXPIRY).lt(currentTime));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION_EVENT_ACTION));

        mongoTemplate.remove(query, MongoExclusionEventAction.class);
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
            mongoAction.setType(EXCLUSION_EVENT_ACTION);
            mongoAction.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());
            return mongoAction;
        }

        // Otherwise, create a new MongoExclusionEventAction and copy fields
        MongoExclusionEventAction entity = new MongoExclusionEventAction();

        entity.setId(exclusionEventAction.getErrorUri());
        entity.setType(EXCLUSION_EVENT_ACTION);
        entity.setModuleName(exclusionEventAction.getModuleName());
        entity.setFlowName(exclusionEventAction.getFlowName());
        entity.setErrorUri(exclusionEventAction.getErrorUri());
        entity.setActionedBy(exclusionEventAction.getActionedBy());
        entity.setAction(exclusionEventAction.getAction());
        entity.setEvent((String) exclusionEventAction.getEvent());
        entity.setTimestamp(exclusionEventAction.getTimestamp());
        entity.setComment(exclusionEventAction.getComment());
        entity.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());

        return entity;
    }
}
