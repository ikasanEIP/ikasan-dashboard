package org.ikasan.solr.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.persistence.dao.EntityDeleteDao;
import org.ikasan.spec.search.dao.ESBSearchDao;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public class SolrGeneralDaoImpl extends SolrDaoBase<IkasanSolrDocument> implements
    SolrGeneralDao<IkasanSolrDocumentSearchResults, IkasanSolrDocument>,
    ESBSearchDao<IkasanSolrDocumentSearchResults, IkasanSolrDocument>,
    EntityDeleteDao
{
    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(SolrGeneralDaoImpl.class);

    public static final String ASCENDING = "ASCENDING";
    public static final String DESCENDING = "DESCENDING";


    @Override
    public IkasanSolrDocumentSearchResults search(String searchString, long startTime, long endTime, int resultSize
        , List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(null, null, null, null, searchString
            , startTime, endTime, 0, resultSize, null, negateQuery, sortField, sortOrder, true);
    }

    @Override
    public IkasanSolrDocumentSearchResults search(String searchString, long startTime, long endTime, int offset, int resultSize
        , List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(null, null, null, null, searchString, startTime
            , endTime, offset, resultSize, null, negateQuery, sortField, sortOrder, true);
    }

    @Override
    public IkasanSolrDocumentSearchResults search(Set<String> moduleName, Set<String> flowNames, String searchString, long startTime
        , long endTime, int resultSize, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleName, flowNames, null, null, searchString, startTime, endTime
            , 0, resultSize, null, negateQuery, sortField, sortOrder, true);
    }

    @Override
    public IkasanSolrDocumentSearchResults search(Set<String> moduleName, Set<String> flowNames, String searchString
        , long startTime, long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleName, flowNames, null, null, searchString, startTime, endTime
            , 0, resultSize, entityTypes, negateQuery, sortField, sortOrder, false);
    }

    @Override
    public IkasanSolrDocumentSearchResults search(Set<String> moduleName, Set<String> flowNames, Set<String> componentNames
        , String eventId, String searchString, long startTime
        , long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleName, flowNames, componentNames, eventId, searchString, startTime, endTime, offset
            , resultSize, entityTypes, negateQuery, sortField, sortOrder, true);
    }

    /**
     * Utility search method.
     *
     * @param moduleNames
     * @param flowNames
     * @param componentNames
     * @param eventId
     * @param searchString
     * @param startTime
     * @param endTime
     * @param offset
     * @param resultSize
     * @param entityTypes
     * @param negateQuery
     * @param sortField
     * @param sortOrder
     * @return
     */
    protected IkasanSolrDocumentSearchResults searchBase(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames
        , String eventId, String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery
        , String sortField, String sortOrder, boolean addWildCards) {
        SolrQuery query = new SolrQuery();
        query.setStart(offset);
        query.setRows(resultSize);

        if(sortField != null && !sortField.isEmpty() && sortOrder != null && !sortOrder.isEmpty())
        {
            if(sortOrder.equals(DESCENDING))
            {
                query.setSort(sortField, SolrQuery.ORDER.desc);
            }
            else
            {
                query.setSort(sortField, SolrQuery.ORDER.asc);
            }
        }
        else
        {
            // Default
            query.setSort(CREATED_DATE_TIME, SolrQuery.ORDER.desc);
        }

        String queryFilter;

        try {
            if (moduleNames != null && !moduleNames.isEmpty()) {
                if (moduleNames.size() == 1 && !moduleNames.stream().findFirst().get().isEmpty() && addWildCards) {
                    moduleNames = moduleNames.stream()
                        .map(moduleName -> "*"+moduleName+"*")
                        .collect(Collectors.toSet());
                }
            }

            if (flowNames != null && !flowNames.isEmpty() && !flowNames.stream().findFirst().get().isEmpty()) {
                if(flowNames.size() == 1 && !flowNames.stream().findFirst().get().isEmpty() && addWildCards) {
                    flowNames = flowNames.stream()
                        .map(flowName -> "*"+flowName+"*")
                        .collect(Collectors.toSet());
                }
            }

            // Component names filter
            if (componentNames != null && !componentNames.isEmpty()) {
                if(componentNames.size() == 1 && !componentNames.stream().findFirst().get().isEmpty() && addWildCards) {
                    componentNames = componentNames.stream()
                        .map(componentName -> "*"+componentName+"*")
                        .collect(Collectors.toSet());
                }
            }

            // Event ID filter
            if (eventId != null && !eventId.isEmpty() && addWildCards) {
                eventId = "*"+eventId+"*";
            }

            queryFilter = super.buildQuery(moduleNames, flowNames, componentNames, new Date(startTime)
                , new Date(endTime), searchString, eventId, entityTypes, negateQuery);
        }
        catch (IOException e) {
            throw new RuntimeException(String.format("An error has occurred building Solr query.", e.getMessage()));
        }

        query.setQuery(queryFilter);

        try
        {
            logger.debug("query: " + query);

            QueryRequest req = new QueryRequest(query, SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            List<IkasanSolrDocument> beans = rsp.getBeans(IkasanSolrDocument.class);

            return new IkasanSolrDocumentSearchResults(beans.stream()
                .map(doc -> (IkasanESBDocument)doc)
                .toList()
                , rsp.getResults().getNumFound(), rsp.getQTime());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Caught exception perform general ikasan search!", e);
        }
    }

    @Override
    public IkasanSolrDocument findById(String type, String id) {

        SolrQuery solrQuery = super.buildIdQuery(id, type);
        return this.getUniqueResult(solrQuery);
    }

    @Override
    public IkasanSolrDocument findByErrorUri(String type, String uri) {

        SolrQuery solrQuery = super.buildErrorUriQuery(uri, type);
        return this.getUniqueResult(solrQuery);
    }

    protected IkasanSolrDocument getUniqueResult(SolrQuery solrQuery) {

        try {
            logger.debug("query: " + solrQuery.getQuery());

            QueryRequest req = new QueryRequest(solrQuery);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            List<IkasanSolrDocument> beans = rsp.getBeans(IkasanSolrDocument.class);

            return beans.stream().findFirst().orElse(null);
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Caught exception perform ikasan solr search using query %s!", solrQuery), e);
        }
    }

    @Override
    public void saveOrUpdate(IkasanSolrDocument ikasanSolrDocument)
    {
        super.save(ikasanSolrDocument);
    }

    @Override
    public void saveOrUpdate(List<IkasanSolrDocument> ikasanSolrDocuments)
    {
        super.save(ikasanSolrDocuments);
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, IkasanSolrDocument ikasanSolrDocument) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, ikasanSolrDocument.getId());
        document.addField(TYPE, ikasanSolrDocument.getType());
        document.addField(ERROR_URI, ikasanSolrDocument.getErrorUri());
        document.addField(ERROR_ACTION, ikasanSolrDocument.getErrorAction());
        document.addField(ERROR_DETAIL, ikasanSolrDocument.getErrorDetail());
        document.addField(ERROR_MESSAGE, ikasanSolrDocument.getErrorMessage());
        document.addField(MODULE_NAME, ikasanSolrDocument.getModuleName());
        document.addField(FLOW_NAME, ikasanSolrDocument.getFlowName());
        document.addField(EVENT, ikasanSolrDocument.getEventId());
        document.addField(PAYLOAD_CONTENT, ikasanSolrDocument.getEvent());
        document.addField(PAYLOAD_CONTENT_RAW, ikasanSolrDocument.getPayloadRaw());
        document.addField(CREATED_DATE_TIME, ikasanSolrDocument.getTimeStamp());
        document.setField(EXPIRY, expiry);

        return document;
    }

    @Override
    public void backupIndex(String backupLocationPath, int numberOfBackupsToKeep) {
        try {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set("command", "backup");
            params.set("numberToKeep", numberOfBackupsToKeep);
            params.set("location", backupLocationPath);

            // The 'BACKUP' action is part of the /replication handler API
            GenericSolrRequest request = new GenericSolrRequest(
                GenericSolrRequest.METHOD.GET,
                "/" + SolrConstants.CORE + "/replication",
                params
            );
            request.setBasicAuthCredentials(this.solrUsername, this.solrPassword);


            logger.info("Performing Ikasan SOLR backup to location[{}]. Number of backups to keep[{}].",
                backupLocationPath, numberOfBackupsToKeep);
            NamedList<Object> response = solrClient.request(request);

            logger.info("Successfully performed Ikasan SOLR backup to location[{}]" +
                    ". Number of backups to keep[{}]. Response[{}]",
                backupLocationPath, numberOfBackupsToKeep, response);
        }
        catch (Exception e) {
            throw new RuntimeException("Caught exception performing Ikasan SOLR backup!", e);
        }
    }
}
