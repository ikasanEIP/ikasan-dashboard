package org.ikasan.mongo.persistence.general.service;

import org.ikasan.mongo.persistence.general.dao.MongoGeneralDaoImpl;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocument;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocumentSearchResults;
import org.ikasan.spec.housekeeping.HousekeepService;
import org.ikasan.spec.persistence.service.EntityDeleteService;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * MongoDB implementation of general service for Ikasan documents.
 * Implements ESBSearchService, HousekeepService, and EntityDeleteService interfaces.
 */
public class MongoGeneralServiceImpl implements
    ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults>,
    HousekeepService,
    EntityDeleteService {

    private static final Logger logger = LoggerFactory.getLogger(MongoGeneralServiceImpl.class);

    private final MongoGeneralDaoImpl mongoGeneralDao;

    /**
     * Constructor for the MongoGeneralServiceImpl class.
     * Initializes the service with the given MongoGeneralDaoImpl instance.
     *
     * @param mongoGeneralDao the MongoGeneralDaoImpl instance used for MongoDB operations
     * @throws IllegalArgumentException if mongoGeneralDao is null
     */
    public MongoGeneralServiceImpl(MongoGeneralDaoImpl mongoGeneralDao) {
        this.mongoGeneralDao = mongoGeneralDao;
        if (this.mongoGeneralDao == null) {
            throw new IllegalArgumentException("mongoGeneralDao cannot be null!");
        }
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames,
                                                     String searchString, long startTime, long endTime, int resultSize, boolean negateQuery, String sortField, String sortOrder) {
        return this.mongoGeneralDao.search(moduleNames, flowNames, searchString, startTime, endTime, resultSize, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, String searchString, long startTime,
                                                     long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.mongoGeneralDao.search(moduleNames, flowNames, searchString, startTime, endTime, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.mongoGeneralDao.search(searchString, startTime, endTime, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.mongoGeneralDao.search(searchString, startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.mongoGeneralDao.search(moduleNames, null, null, null, searchString, startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames, String eventId, String searchString, long startTime,
                                                     long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.mongoGeneralDao.search(moduleNames, flowNames, componentNames, eventId, searchString, startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanESBDocument findById(String type, String id) {
        return this.mongoGeneralDao.findById(type, id);
    }

    @Override
    public IkasanESBDocument findByErrorUri(String type, String uri) {
        return this.mongoGeneralDao.findByErrorUri(type, uri);
    }

    /**
     * Save or update a single document.
     *
     * @param document the document to save
     */
    public void saveOrUpdate(IkasanESBDocument document) {
        this.mongoGeneralDao.saveOrUpdate((MongoIkasanDocument) document);
    }

    /**
     * Save or update multiple documents.
     *
     * @param documents the documents to save
     */
    public void saveOrUpdate(List<IkasanESBDocument> documents) {
        this.mongoGeneralDao.saveOrUpdate(documents.stream()
            .map(doc -> (MongoIkasanDocument) doc)
            .toList());
    }

    @Override
    public void housekeep() {
        logger.info("Starting MongoDB housekeeping for expired documents");
        this.mongoGeneralDao.removeExpired();
        logger.info("Completed MongoDB housekeeping for expired documents");
    }

    @Override
    public boolean housekeepablesExist() {
        return true;
    }

    @Override
    public void setHousekeepingBatchSize(Integer housekeepingBatchSize) {
        // Not relevant for MongoDB housekeeping
        logger.debug("setHousekeepingBatchSize called with value: {}, but not used in MongoDB implementation", housekeepingBatchSize);
    }

    @Override
    public void setTransactionBatchSize(Integer transactionBatchSize) {
        // Not relevant for MongoDB housekeeping
        logger.debug("setTransactionBatchSize called with value: {}, but not used in MongoDB implementation", transactionBatchSize);
    }

    @Override
    public void removeById(String type, String id) {
        this.mongoGeneralDao.removeById(type, id);
    }
}
