package org.ikasan.mongo.persistence.exclusion.dao;

import org.ikasan.mongo.persistence.exclusion.model.MongoExclusionEvent;
import org.ikasan.mongo.persistence.exclusion.repository.MongoExclusionEventRepository;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchFilter;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ExclusionEvent DAO.
 *
 * @author Ikasan Development Team
 */
public class MongoExclusionEventDaoImpl implements EntityDao<ExclusionEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MongoExclusionEventDaoImpl.class);

    /**
     * Fields to search in for full-text search.
     */
    private static final List<String> SEARCH_FIELDS = List.of(
        EntityFields.PAYLOAD_CONTENT,
        EntityFields.ERROR_URI,
        EntityFields.EVENT
    );

    private final MongoExclusionEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final AtlasSearchQueryHelper atlasSearchQueryHelper;
    private final int daysToKeep;

    public MongoExclusionEventDaoImpl(MongoExclusionEventRepository repository, MongoTemplate mongoTemplate,
                                      int daysToKeep) {
        this(repository, mongoTemplate, daysToKeep, new AtlasSearchQueryHelper(mongoTemplate));
    }

    public MongoExclusionEventDaoImpl(MongoExclusionEventRepository repository, MongoTemplate mongoTemplate,
                                      int daysToKeep, AtlasSearchQueryHelper atlasSearchQueryHelper) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.atlasSearchQueryHelper = atlasSearchQueryHelper;
        this.daysToKeep = daysToKeep;
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
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ERROR_URI).is(errorUri));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION));

        MongoExclusionEvent entity = mongoTemplate.findOne(query, MongoExclusionEvent.class);
        return entity;
    }

    public List<ExclusionEvent> findByModuleName(String moduleName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION));

        List<MongoExclusionEvent> entities = mongoTemplate.find(query, MongoExclusionEvent.class);
        return entities.stream()
                .map(entity -> (ExclusionEvent) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEvent> findByModuleNameAndFlowName(String moduleName, String flowName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(moduleName));
        query.addCriteria(Criteria.where(EntityFields.FLOW_NAME).is(flowName));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION));

        List<MongoExclusionEvent> entities = mongoTemplate.find(query, MongoExclusionEvent.class);
        return entities.stream()
                .map(entity -> (ExclusionEvent) entity)
                .collect(Collectors.toList());
    }

    public List<ExclusionEvent> find(List<String> moduleNames, List<String> flowNames, String searchString,
                                     long fromTimestamp, long untilTimestamp, int offset, int limit) {

        AtlasSearchFilter searchFilter = AtlasSearchFilter.builder()
            .equals(EntityFields.TYPE, EXCLUSION)
            .in(EntityFields.MODULE_NAME, moduleNames)
            .in(EntityFields.FLOW_NAME, flowNames)
            .rangeMillis(EntityFields.CREATED_DATE_TIME, fromTimestamp, untilTimestamp)
            .containsAny(SEARCH_FIELDS, searchString)
            .build();

        return atlasSearchQueryHelper.search(searchFilter, null, offset, limit, false, MongoExclusionEvent.class)
            .getResults().stream()
            .map(entity -> (ExclusionEvent) entity)
            .collect(Collectors.toList());
    }

    public void deleteByErrorUri(String errorUri) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ERROR_URI).is(errorUri));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION));

        mongoTemplate.remove(query, MongoExclusionEvent.class);
        logger.debug("Deleted exclusion event with error URI: {}", errorUri);
    }

    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.EXPIRY).lt(currentTime));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(EXCLUSION));

        mongoTemplate.remove(query, MongoExclusionEvent.class);
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
            mongoExclusion.setType(EXCLUSION);
            mongoExclusion.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());
            return mongoExclusion;
        }

        // Otherwise, create a new MongoExclusionEvent and copy fields
        MongoExclusionEvent entity = new MongoExclusionEvent();

        String id = exclusionEvent.getModuleName() + ":exclusion:" + exclusionEvent.getErrorUri();
        entity.setId(id);
        entity.setType(EXCLUSION);
        entity.setModuleName(exclusionEvent.getModuleName());
        entity.setFlowName(exclusionEvent.getFlowName());
        entity.setIdentifier(exclusionEvent.getIdentifier());
        entity.setEvent(exclusionEvent.getEvent());
        entity.setTimestamp(exclusionEvent.getTimestamp());
        entity.setErrorUri(exclusionEvent.getErrorUri());
        entity.setHarvested(exclusionEvent.isHarvested());
        entity.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());
        entity.setHarvestReceivedTimestamp(System.currentTimeMillis());

        return entity;
    }
}
