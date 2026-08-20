package org.ikasan.module.metadata.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.module.metadata.model.*;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.model.*;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * Solr implementation of ModuleMetadataDao.
 *
 * This class provides Solr-based persistence for module metadata, extending
 * the base Solr DAO functionality and implementing the ModuleMetadataDao interface.
 *
 * Created by Ikasan Development Team on 14/02/2017.
 */
public class SolrModuleMetadataDao extends SolrDaoBase<ModuleMetaData> implements ModuleMetadataDao
{
    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(SolrModuleMetadataDao.class);

    private final JsonMapper objectMapper;

    public SolrModuleMetadataDao()
    {

        SimpleModule m = new SimpleModule();
        m.addAbstractTypeMapping(ModuleMetaData.class, SolrModuleMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowMetaData.class, SolrFlowMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowElementMetaData.class, SolrFlowElementMetaDataImpl.class);
        m.addAbstractTypeMapping(Transition.class, SolrTransitionImpl.class);
        m.addAbstractTypeMapping(DecoratorMetaData.class, SolrDecoratorMetaDataImpl.class);

        this.objectMapper = JsonMapper.builder()
            .addModule(m)
            .build();
    }

    @Override
    public void save(List<ModuleMetaData> moduleMetaDataList)
    {
        try
        {
            UpdateRequest req = new UpdateRequest();
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            for(ModuleMetaData moduleMetaData: moduleMetaDataList)
            {
                SolrInputDocument document = convertEntityToSolrInputDocument(null,moduleMetaData);
                req.add(document);

                logger.debug("Adding document: " + document);
            }

            commitSolrRequest(req);
        }
        catch (Exception e)
        {
            throw new RuntimeException("An exception has occurred attempting to write a module metadata to Solr", e);
        }
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ModuleMetaData moduleMetaData)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, moduleMetaData.getName());
        document.addField(TYPE, MODULE_METADATA);
        try
        {
            document.addField(PAYLOAD_CONTENT, objectMapper.writeValueAsString(moduleMetaData));
        }
        catch (JacksonException e)
        {
            throw new RuntimeException("Unable to convert ["+moduleMetaData+"] to json format.");
        }

        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());

        return document;
    }

    @Override
    public ModuleMetaData findById(String id)
    {
        SolrQuery query = super.buildIdQuery(id, MODULE_METADATA);

        logger.debug("query: " + query);

        List<SolrModule> beans = this.findByQuery(query);

        if(beans.size() > 0 && beans.get(0).getModuleMetaData() != null)
        {
            return this.convert(beans.get(0).getModuleMetaData());
        }
        else
        {
            return null;
        }
    }

    @Override
    public void deleteById(String id)
    {
        String queryString = "id:\"" + id + "\" AND type:\"" + MODULE_METADATA + "\"";

        logger.debug("queryString: " + queryString);

        super.deleteByQuery(queryString);
    }

    @Override
    public List<ModuleMetaData> findAll(Integer startOffset, Integer resultSize)
    {
        String queryString = "type:\"" + MODULE_METADATA + "\"";

        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);
        query.setStart(startOffset);
        query.setRows(resultSize);

        List<SolrModule> beans = this.findByQuery(query);

        List<ModuleMetaData> results = beans.stream().map(bean -> convert(bean.getModuleMetaData())).collect(Collectors.toList());

        return results;
    }

    /**
     * Helper method to find by query.
     *
     * @param query
     */
    private List<SolrModule> findByQuery(SolrQuery query)
    {
        logger.debug("queryString: " + query);

        try
        {
            QueryRequest req = new QueryRequest(query, SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            return rsp.getBeans(SolrModule.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr module meta data by query [" + query + "] from the ikasan solr index!", e);
        }
    }

    /**
     * Get using offset with filtering capabilities.
     *
     * @param modulesNames
     * @param startOffset
     * @param resultSize
     * @return
     */
    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames, Integer startOffset, Integer resultSize)
    {
        String queryString = "type:\"" + MODULE_METADATA + "\"";

        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);
        query.setStart(startOffset);
        query.setRows(resultSize);

        StringBuffer moduleNamesBuffer = new StringBuffer();

        if(modulesNames != null && modulesNames.size() > 0)
        {
            moduleNamesBuffer.append(this.buildPredicate(ID, modulesNames));
        }

        query.setFilterQueries(moduleNamesBuffer.toString());

        ModuleMetadataSearchResults results;

        try
        {
            QueryRequest req = new QueryRequest(query, SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            List<SolrModule> beans = rsp.getBeans(SolrModule.class);

            results = new ModuleMetadataSearchResults(beans.stream()
                .map(solrModule -> convert(solrModule.getModuleMetaData()))
                .collect(Collectors.toList()), rsp.getResults().getNumFound(), rsp.getQTime());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr module meta data by query [" + query + "] from the ikasan solr index!", e);
        }

        return results;
    }

    /**
     * Get using offset with filtering capabilities.
     *
     * @param modulesNames
     * @param startOffset
     * @param resultSize
     * @return
     */
    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames, ModuleType moduleType, Integer startOffset, Integer resultSize)
    {
        String queryString = "type:\"" + MODULE_METADATA + "\"";

        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);
        if (startOffset > -1) {
            query.setStart(startOffset);
        }

        if(resultSize > -1) {
            query.setRows(resultSize);
        }

        StringBuffer filterBuffer = new StringBuffer();

        if(modulesNames != null && modulesNames.size() > 0)
        {
            filterBuffer.append(this.buildPredicate(ID, modulesNames));
        }

        if(filterBuffer.length() > 0) {
            filterBuffer.append(" AND ");
        }

        filterBuffer.append("payload:\"*\\\"type\\\":\\\""+moduleType+"\\\"*\"");

        query.setFilterQueries(filterBuffer.toString());

        ModuleMetadataSearchResults results;

        try
        {
            QueryRequest req = new QueryRequest(query, SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            List<SolrModule> beans = rsp.getBeans(SolrModule.class);

            results = new ModuleMetadataSearchResults(beans.stream()
                .map(solrModule -> convert(solrModule.getModuleMetaData()))
                .collect(Collectors.toList()), rsp.getResults().getNumFound(), rsp.getQTime());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr module meta data by query [" + query + "] from the ikasan solr index!", e);
        }

        return results;
    }

    /**
     * Helper method to convert raw module metadata.
     *
     * @param rawModuleMetaData
     * @return
     */
    private ModuleMetaData convert(String rawModuleMetaData)
    {
        try
        {
            SolrModuleMetaDataImpl solrModuleMetaData
                = objectMapper.readValue(rawModuleMetaData, SolrModuleMetaDataImpl.class);

            return solrModuleMetaData;
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Unable to deserialise ModuleMetaData [%s]"
                , rawModuleMetaData), e);
        }
    }

}
