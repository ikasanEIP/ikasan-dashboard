package org.ikasan.solr.service;

import org.ikasan.solr.dao.SolrGeneralDao;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.housekeeping.HousekeepService;
import org.ikasan.spec.persistence.service.EntityDeleteService;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.ikasan.spec.solr.SolrServiceBase;

import java.util.List;
import java.util.Set;

/**
 * Created by Ikasan Development Team on 26/08/2017.
 */
public class SolrGeneralServiceImpl extends SolrServiceBase implements SolrGeneralService<IkasanESBDocument, IkasanDocumentSearchResults>
    , ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults>
    , HousekeepService, EntityDeleteService
{
    private SolrGeneralDaoImpl solrGeneralDao;

    /**
     * Constructor for the SolrGeneralServiceImpl class.
     * Initializes the service with the given SolrGeneralDaoImpl instance.
     *
     * @param solrGeneralDao the SolrGeneralDaoImpl instance used for Solr operations
     * @throws IllegalArgumentException if solrGeneralDao is null
     */
    public SolrGeneralServiceImpl(SolrGeneralDaoImpl solrGeneralDao)
    {
        this.solrGeneralDao = solrGeneralDao;
        if(this.solrGeneralDao == null)
        {
            throw new IllegalArgumentException("solrGeneralSearchDao cannot be null!");
        }
    }


    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleName, Set<String> flowNames,
                                                  String searchString, long startTime, long endTime, int resultSize, boolean negateQuery, String sortField, String sortOrder) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        return this.solrGeneralDao.search(moduleName, flowNames, searchString, startTime, endTime, resultSize, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, String searchString, long startTime
        , long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        return this.solrGeneralDao.search(moduleNames, flowNames, searchString, startTime, endTime, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        return this.solrGeneralDao.search(searchString, startTime, endTime, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        return this.solrGeneralDao.search(searchString, startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder)  {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        return this.solrGeneralDao.search(moduleNames, null, null, null, searchString, startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames, String eventId, String searchString, long startTime
        , long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        return this.solrGeneralDao.search(moduleNames, flowNames, componentNames, eventId, searchString, startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanESBDocument findById(String type, String id) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);

        return this.solrGeneralDao.findById(type, id);
    }

    @Override
    public IkasanESBDocument findByErrorUri(String type, String uri) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);

        return this.solrGeneralDao.findByErrorUri(type, uri);
    }

    @Override
    public void saveOrUpdate(IkasanESBDocument document)
    {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        this.solrGeneralDao.saveOrUpdate((IkasanSolrDocument) document);
    }

    @Override
    public void saveOrUpdate(List<IkasanESBDocument> documents)
    {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        this.solrGeneralDao.saveOrUpdate(documents.stream()
            .map(doc -> (IkasanSolrDocument)doc)
            .toList());
    }

    @Override
    public void housekeep()
    {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        this.solrGeneralDao.removeExpired();
    }

    @Override
    public boolean housekeepablesExist()
    {
        return true;
    }

    @Override
    public void setHousekeepingBatchSize(Integer housekeepingBatchSize)
    {
        // not relevant for solr housekeeping
    }

    @Override
    public void setTransactionBatchSize(Integer transactionBatchSize)
    {
        // not relevant for solr housekeeping
    }

    @Override
    public void removeById(String type, String id)
    {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        this.solrGeneralDao.removeById(type, id);
    }

    @Override
    public void backupIndex(String backupLocationPath, int numberOfBackupsToKeep) {
        this.solrGeneralDao.setSolrUsername(this.solrUsername);
        this.solrGeneralDao.setSolrPassword(this.solrPassword);
        this.solrGeneralDao.backupIndex(backupLocationPath, numberOfBackupsToKeep);
    }
}
