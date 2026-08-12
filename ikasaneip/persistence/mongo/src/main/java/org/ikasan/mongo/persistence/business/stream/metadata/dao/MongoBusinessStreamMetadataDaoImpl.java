package org.ikasan.mongo.persistence.business.stream.metadata.dao;

import org.ikasan.mongo.persistence.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.mongo.persistence.business.stream.metadata.model.MongoBusinessStream;
import org.ikasan.mongo.persistence.business.stream.metadata.repository.MongoBusinessStreamRepository;
import org.ikasan.spec.metadata.BusinessStreamMetadataSearchResults;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of BusinessStreamMetadataDao.
 *
 * This DAO leverages:
 * - Spring Data MongoDB for repository operations
 * - MongoTemplate for complex queries
 * - MongoDB text search and regex for filtering
 */
public class MongoBusinessStreamMetadataDaoImpl implements BusinessStreamMetadataDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoBusinessStreamMetadataDaoImpl.class);

    private final MongoBusinessStreamRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoBusinessStreamMetadataDaoImpl(MongoBusinessStreamRepository repository,
                                              MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(BusinessStreamMetaData metaData) {
        logger.debug("Saving BusinessStreamMetaData: {}", metaData.getId());

        MongoBusinessStream mongoBusinessStream = new MongoBusinessStream();
        mongoBusinessStream.setId(metaData.getId());
        mongoBusinessStream.setName(metaData.getName());
        mongoBusinessStream.setDescription(metaData.getDescription());
        mongoBusinessStream.setBusinessStreamMetadata(metaData.getJson());
        mongoBusinessStream.setCreatedTimestamp(System.currentTimeMillis());

        repository.save(mongoBusinessStream);

        logger.debug("Successfully saved BusinessStreamMetaData: {}", metaData.getId());
    }

    @Override
    public BusinessStreamMetaData findById(String id) {
        logger.debug("Finding BusinessStreamMetaData by id: {}", id);

        MongoBusinessStream result = repository.findById(id).orElse(null);

        if (result != null) {
            return convert(result);
        }

        logger.debug("BusinessStreamMetaData not found for id: {}", id);
        return null;
    }

    @Override
    public BusinessStreamMetadataSearchResults find(List<String> businessStreamNames, Integer startOffset, Integer resultSize) {
        logger.debug("Finding BusinessStreamMetaData with names filter");

        Query query = new Query();

        // Add name filter if provided
        if (businessStreamNames != null && !businessStreamNames.isEmpty()) {
            Criteria criteria = Criteria.where("name").in(businessStreamNames);
            query.addCriteria(criteria);
        }

        // Count total matching records
        long totalCount = mongoTemplate.count(query, MongoBusinessStream.class);

        // Apply pagination
        if (resultSize != null && resultSize > 0) {
            int offset = startOffset != null ? startOffset : 0;
            int page = offset > 0 ? offset / resultSize : 0;
            query.with(PageRequest.of(page, resultSize));
        }

        List<MongoBusinessStream> results = mongoTemplate.find(query, MongoBusinessStream.class);

        List<BusinessStreamMetaData> businessStreamMetaData = results.stream()
            .map(this::convert)
            .collect(Collectors.toList());

        logger.debug("Found {} BusinessStreamMetaData out of {} total", results.size(), totalCount);

        return new BusinessStreamMetadataSearchResults(businessStreamMetaData, totalCount, 0);
    }

    @Override
    public List<BusinessStreamMetaData> findBusinessStreamsContainingFlow(String moduleName, String flowName, int offset, int limit) {
        logger.debug("Finding BusinessStreams containing flow: {}.{}", moduleName, flowName);

        Query query = new Query();

        // Search for module.flow pattern in the business stream metadata JSON
        String searchPattern = Pattern.quote(moduleName + "." + flowName);
        Criteria criteria = Criteria.where("business_stream_metadata").regex(searchPattern, "i");
        query.addCriteria(criteria);

        // Apply pagination
        if (limit > 0) {
            int page = offset > 0 ? offset / limit : 0;
            query.with(PageRequest.of(page, limit));
        }

        List<MongoBusinessStream> results = mongoTemplate.find(query, MongoBusinessStream.class);

        logger.debug("Found {} BusinessStreams containing flow {}.{}", results.size(), moduleName, flowName);

        return results.stream()
            .map(this::convert)
            .collect(Collectors.toList());
    }

    @Override
    public BusinessStreamMetadataSearchResults findBusinessStreamsForModules(String filter, List<ModuleMetaData> modules, int offset, int limit) {
        logger.debug("Finding BusinessStreams for modules with filter: {}", filter);

        if (modules == null || modules.isEmpty()) {
            return new BusinessStreamMetadataSearchResults(List.of(), 0, 0);
        }

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Build criteria for each module and its flows
        for (ModuleMetaData module : modules) {
            if (module.getFlows() != null && !module.getFlows().isEmpty()) {
                module.getFlows().forEach(flowMetaData -> {
                    String searchPattern = Pattern.quote(module.getName() + "." + flowMetaData.getName());
                    criteriaList.add(Criteria.where("business_stream_metadata").regex(searchPattern, "i"));
                });
            }
        }

        // Combine all flow criteria with OR
        if (!criteriaList.isEmpty()) {
            Criteria flowCriteria = new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
            query.addCriteria(flowCriteria);
        }

        // Add filter on business stream name if provided
        if (filter != null && !filter.isEmpty()) {
            Criteria filterCriteria = Criteria.where("name").regex(Pattern.quote(filter), "i");
            query.addCriteria(filterCriteria);
        }

        // Count total matching records
        long totalCount = mongoTemplate.count(query, MongoBusinessStream.class);

        // Apply pagination
        if (limit > 0) {
            int page = offset > 0 ? offset / limit : 0;
            query.with(PageRequest.of(page, limit));
        }

        List<MongoBusinessStream> results = mongoTemplate.find(query, MongoBusinessStream.class);

        List<BusinessStreamMetaData> businessStreamMetaData = results.stream()
            .map(this::convert)
            .collect(Collectors.toList());

        logger.debug("Found {} BusinessStreams for modules out of {} total", results.size(), totalCount);

        return new BusinessStreamMetadataSearchResults(businessStreamMetaData, totalCount, 0);
    }

    @Override
    public List<BusinessStreamMetaData> findAll(Integer startOffset, Integer resultSize) {
        logger.debug("Finding all BusinessStreamMetaData with startOffset={}, resultSize={}", startOffset, resultSize);

        List<MongoBusinessStream> results;

        if (resultSize != null && resultSize > 0) {
            int offset = startOffset != null ? startOffset : 0;
            int page = offset > 0 ? offset / resultSize : 0;
            Pageable pageable = PageRequest.of(page, resultSize, Sort.by(Sort.Direction.ASC, "name"));
            results = repository.findAll(pageable).getContent();
        } else {
            results = repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }

        logger.debug("Found {} BusinessStreamMetaData records", results.size());

        return results.stream()
            .map(this::convert)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        logger.debug("Deleting BusinessStreamMetaData with id: {}", id);

        repository.deleteById(id);

        logger.debug("Successfully deleted BusinessStreamMetaData: {}", id);
    }

    /**
     * Helper method to convert MongoBusinessStream to BusinessStreamMetaData.
     *
     * @param mongoBusinessStream the MongoDB entity
     * @return the business stream metadata
     */
    private BusinessStreamMetaData convert(MongoBusinessStream mongoBusinessStream) {
        BusinessStreamMetaData businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setId(mongoBusinessStream.getId());
        businessStreamMetaData.setName(mongoBusinessStream.getName());
        businessStreamMetaData.setDescription(mongoBusinessStream.getDescription());
        businessStreamMetaData.setJson(mongoBusinessStream.getBusinessStreamMetaData());

        return businessStreamMetaData;
    }
}
