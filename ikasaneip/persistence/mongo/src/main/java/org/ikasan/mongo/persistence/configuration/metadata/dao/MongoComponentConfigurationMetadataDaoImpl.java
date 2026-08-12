package org.ikasan.mongo.persistence.configuration.metadata.dao;

import org.ikasan.mongo.persistence.configuration.metadata.model.MongoComponentConfigurationMetadata;
import org.ikasan.mongo.persistence.configuration.metadata.model.MongoConfigurationMetaData;
import org.ikasan.mongo.persistence.configuration.metadata.repository.MongoComponentConfigurationMetadataRepository;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ComponentConfigurationMetadataDao.
 *
 * This DAO leverages:
 * - Spring Data MongoDB for repository operations
 * - MongoTemplate for complex queries
 * - MongoDB indexing for performance
 * - Jackson for JSON serialization/deserialization
 */
public class MongoComponentConfigurationMetadataDaoImpl implements ComponentConfigurationMetadataDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoComponentConfigurationMetadataDaoImpl.class);

    private final MongoComponentConfigurationMetadataRepository repository;
    private final MongoTemplate mongoTemplate;
    private final JsonMapper objectMapper;

    public MongoComponentConfigurationMetadataDaoImpl(MongoComponentConfigurationMetadataRepository repository,
                                                      MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = JsonMapper.builder().build();
    }

    @Override
    public void save(List<ConfigurationMetaData> configurationMetaDataList) {
        logger.debug("Saving {} ConfigurationMetaData records", configurationMetaDataList.size());

        try {
            // Extract configuration IDs for deletion
            List<String> configurationIds = configurationMetaDataList.stream()
                .map(ConfigurationMetaData::getConfigurationId)
                .collect(Collectors.toList());

            // Remove existing configurations by IDs
            configurationIds.forEach(repository::deleteById);

            // Save new configurations
            for (ConfigurationMetaData configurationMetaData : configurationMetaDataList) {
                MongoComponentConfigurationMetadata entity = new MongoComponentConfigurationMetadata(
                    configurationMetaData.getConfigurationId());
                entity.setConfigurationMetadataJson(objectMapper.writeValueAsString(configurationMetaData));
                entity.setCreatedTimestamp(System.currentTimeMillis());

                repository.save(entity);

                logger.debug("Saved ConfigurationMetaData: {}", configurationMetaData.getConfigurationId());
            }
        } catch (JacksonException e) {
            throw new RuntimeException("An exception has occurred attempting to write component configuration to MongoDB", e);
        }
    }

    @Override
    public ConfigurationMetaData findById(String id) {
        logger.debug("Finding ConfigurationMetaData by id: {}", id);

        MongoComponentConfigurationMetadata entity = repository.findById(id).orElse(null);

        if (entity != null && entity.getConfigurationMetadataJson() != null) {
            return convert(entity.getConfigurationMetadataJson());
        }

        logger.debug("ConfigurationMetaData not found for id: {}", id);
        return null;
    }

    @Override
    public List<ConfigurationMetaData> findAll() {
        logger.debug("Finding all ConfigurationMetaData");

        List<MongoComponentConfigurationMetadata> results = repository.findAll(
            Sort.by(Sort.Direction.ASC, "configuration_id"));

        logger.debug("Found {} ConfigurationMetaData records", results.size());

        return results.stream()
            .map(entity -> convert(entity.getConfigurationMetadataJson()))
            .collect(Collectors.toList());
    }

    @Override
    public List<ConfigurationMetaData> findInIdList(List<String> configurationIds) {
        logger.debug("Finding ConfigurationMetaData in ID list with {} IDs",
            configurationIds != null ? configurationIds.size() : 0);

        if (configurationIds == null || configurationIds.isEmpty()) {
            return new ArrayList<>();
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("configuration_id").in(configurationIds));

        List<MongoComponentConfigurationMetadata> results = mongoTemplate.find(
            query, MongoComponentConfigurationMetadata.class);

        logger.debug("Found {} ConfigurationMetaData records from ID list", results.size());

        return results.stream()
            .map(entity -> convert(entity.getConfigurationMetadataJson()))
            .collect(Collectors.toList());
    }

    /**
     * Helper method to convert raw configuration metadata JSON to ConfigurationMetaData object.
     *
     * @param rawConfigurationMetadata JSON string
     * @return ConfigurationMetaData object
     */
    private MongoConfigurationMetaData convert(String rawConfigurationMetadata) {
        try {
            return objectMapper.readValue(rawConfigurationMetadata, MongoConfigurationMetaData.class);
        } catch (Exception e) {
            throw new RuntimeException(
                String.format("Unable to deserialise ConfigurationMetaData [%s]", rawConfigurationMetadata), e);
        }
    }
}
