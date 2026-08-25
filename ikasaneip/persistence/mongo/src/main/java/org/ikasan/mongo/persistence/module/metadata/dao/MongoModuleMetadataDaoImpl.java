package org.ikasan.mongo.persistence.module.metadata.dao;

import org.ikasan.mongo.persistence.module.metadata.model.*;
import org.ikasan.mongo.persistence.module.metadata.repository.MongoModuleMetadataRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.model.*;
import org.ikasan.spec.module.ModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ModuleMetadataDao.
 *
 * This DAO leverages:
 * - Spring Data MongoDB for repository operations
 * - MongoTemplate for complex queries
 * - MongoDB indexing for performance
 * - Jackson for JSON serialization/deserialization
 */
public class MongoModuleMetadataDaoImpl implements ModuleMetadataDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoModuleMetadataDaoImpl.class);

    private final MongoModuleMetadataRepository repository;
    private final MongoTemplate mongoTemplate;
    private final JsonMapper objectMapper;

    public MongoModuleMetadataDaoImpl(MongoModuleMetadataRepository repository,
                                      MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;

        // Configure Jackson mapper with MongoDB-specific type mappings
        SimpleModule m = new SimpleModule();
        m.addAbstractTypeMapping(ModuleMetaData.class, MongoModuleMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowMetaData.class, MongoFlowMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowElementMetaData.class, MongoFlowElementMetaDataImpl.class);
        m.addAbstractTypeMapping(Transition.class, MongoTransitionImpl.class);
        m.addAbstractTypeMapping(DecoratorMetaData.class, MongoDecoratorMetaDataImpl.class);

        this.objectMapper = JsonMapper.builder()
            .addModule(m)
            .build();
    }

    @Override
    public void save(List<ModuleMetaData> moduleMetaDataList) {
        logger.debug("Saving {} ModuleMetaData records", moduleMetaDataList.size());

        try {
            for (ModuleMetaData moduleMetaData : moduleMetaDataList) {
                MongoModuleMetadata entity = new MongoModuleMetadata(moduleMetaData.getName());
                entity.setModuleMetadataJson(objectMapper.writeValueAsString(moduleMetaData));
                entity.setType(MODULE_METADATA);
                entity.setCreatedTimestamp(System.currentTimeMillis());

                repository.save(entity);

                logger.debug("Saved ModuleMetaData: {}", moduleMetaData.getName());
            }
        } catch (JacksonException e) {
            throw new RuntimeException("An exception has occurred attempting to write module metadata to MongoDB", e);
        }
    }

    @Override
    public ModuleMetaData findById(String id) {
        logger.debug("Finding ModuleMetaData by id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(MODULE_METADATA));

        MongoModuleMetadata entity = mongoTemplate.findOne(query, MongoModuleMetadata.class);

        if (entity != null && entity.getModuleMetadataJson() != null) {
            return convert(entity.getModuleMetadataJson());
        }

        logger.debug("ModuleMetaData not found for id: {}", id);
        return null;
    }

    @Override
    public void deleteById(String id) {
        logger.debug("Deleting ModuleMetaData with id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(MODULE_METADATA));

        mongoTemplate.remove(query, MongoModuleMetadata.class);

        logger.debug("Successfully deleted ModuleMetaData: {}", id);
    }

    @Override
    public List<ModuleMetaData> findAll(Integer startOffset, Integer resultSize) {
        logger.debug("Finding all ModuleMetaData with startOffset={}, resultSize={}", startOffset, resultSize);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(MODULE_METADATA));

        List<MongoModuleMetadata> results;

        if (resultSize != null && resultSize > 0) {
            int offset = startOffset != null ? startOffset : 0;
            int page = offset > 0 ? offset / resultSize : 0;
            query.with(PageRequest.of(page, resultSize, Sort.by(Sort.Direction.ASC, "module_name")));
        } else {
            query.with(Sort.by(Sort.Direction.ASC, EntityFields.ID));
        }

        results = mongoTemplate.find(query, MongoModuleMetadata.class);

        logger.debug("Found {} ModuleMetaData records", results.size());

        return results.stream()
            .map(entity -> convert(entity.getModuleMetadataJson()))
            .collect(Collectors.toList());
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> moduleNames, Integer startOffset, Integer resultSize) {
        logger.debug("Finding ModuleMetaData with module names filter");

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(MODULE_METADATA));

        // Add module name filter if provided
        if (moduleNames != null && !moduleNames.isEmpty()) {
            Criteria criteria = Criteria.where(EntityFields.ID).in(moduleNames);
            query.addCriteria(criteria);
        }

        // Count total matching records
        long totalCount = mongoTemplate.count(query, MongoModuleMetadata.class);

        // Apply pagination
        if (resultSize != null && resultSize > 0) {
            int offset = startOffset != null ? startOffset : 0;
            int page = offset > 0 ? offset / resultSize : 0;
            query.with(PageRequest.of(page, resultSize));
        }

        List<MongoModuleMetadata> results = mongoTemplate.find(query, MongoModuleMetadata.class);

        List<ModuleMetaData> moduleMetaDataList = results.stream()
            .map(entity -> convert(entity.getModuleMetadataJson()))
            .collect(Collectors.toList());

        logger.debug("Found {} ModuleMetaData out of {} total", results.size(), totalCount);

        return new ModuleMetadataSearchResults(moduleMetaDataList, totalCount, 0);
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> moduleNames, ModuleType moduleType, Integer startOffset, Integer resultSize) {
        logger.debug("Finding ModuleMetaData with module names filter and moduleType: {}", moduleType);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(MODULE_METADATA));

        // Add module name filter if provided
        if (moduleNames != null && !moduleNames.isEmpty()) {
            Criteria criteria = Criteria.where(EntityFields.ID).in(moduleNames);
            query.addCriteria(criteria);
        }

        // Add module type filter by searching in JSON content
        if (moduleType != null) {
            String typePattern = "\"type\":\"" + moduleType + "\"";
            Criteria typeCriteria = Criteria.where(EntityFields.PAYLOAD_CONTENT).regex(typePattern);
            query.addCriteria(typeCriteria);
        }

        // Count total matching records
        long totalCount = mongoTemplate.count(query, MongoModuleMetadata.class);

        // Apply pagination
        if (startOffset != null && startOffset > -1) {
            if (resultSize != null && resultSize > -1) {
                int page = startOffset > 0 ? startOffset / resultSize : 0;
                query.with(PageRequest.of(page, resultSize));
            }
        }

        List<MongoModuleMetadata> results = mongoTemplate.find(query, MongoModuleMetadata.class);

        List<ModuleMetaData> moduleMetaDataList = results.stream()
            .map(entity -> convert(entity.getModuleMetadataJson()))
            .collect(Collectors.toList());

        logger.debug("Found {} ModuleMetaData out of {} total", results.size(), totalCount);

        return new ModuleMetadataSearchResults(moduleMetaDataList, totalCount, 0);
    }

    /**
     * Helper method to convert raw module metadata JSON to ModuleMetaData object.
     *
     * @param rawModuleMetaData JSON string
     * @return ModuleMetaData object
     */
    private ModuleMetaData convert(String rawModuleMetaData) {
        try {
            return objectMapper.readValue(rawModuleMetaData, MongoModuleMetaDataImpl.class);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to deserialise ModuleMetaData [%s]", rawModuleMetaData), e);
        }
    }
}
