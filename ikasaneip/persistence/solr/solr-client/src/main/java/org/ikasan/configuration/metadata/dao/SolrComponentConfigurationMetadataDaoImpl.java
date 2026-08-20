package org.ikasan.configuration.metadata.dao;

import org.apache.commons.collections4.ListUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.configuration.metadata.model.SolrComponentConfiguration;
import org.ikasan.configuration.metadata.model.SolrConfigurationMetaData;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * Created by Ikasan Development Team on 14/02/2017.
 */
public class SolrComponentConfigurationMetadataDaoImpl extends SolrDaoBase<ConfigurationMetaData> implements ComponentConfigurationMetadataDao
{
    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(SolrComponentConfigurationMetadataDaoImpl.class);

    private JsonMapper objectMapper;

    public SolrComponentConfigurationMetadataDaoImpl()
    {
        this.objectMapper = JsonMapper.builder().build();
    }

    @Override
    public void save(List<ConfigurationMetaData> configurationMetaDataList)
    {
        try
        {
            UpdateRequest req = new UpdateRequest();
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            List<String> configurationIds = configurationMetaDataList
                .stream()
                .map(configurationMetaData -> configurationMetaData.getConfigurationId())
                .collect(Collectors.toList());

            super.removeByIds(COMPONENT_CONFIGURATION, configurationIds);

            configurationMetaDataList.forEach(configurationMetaData -> {
                SolrInputDocument document = convertEntityToSolrInputDocument(null,configurationMetaData);
                req.add(document);

                logger.debug("Adding document: " + document);
            });

            commitSolrRequest(req);
        }
        catch (Exception e)
        {
            throw new RuntimeException("An exception has occurred attempting to write a component configuration to Solr", e);
        }
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ConfigurationMetaData configurationMetaData)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, configurationMetaData.getConfigurationId());
        document.addField(TYPE, COMPONENT_CONFIGURATION);
        try
        {
            document.addField(PAYLOAD_CONTENT, objectMapper.writeValueAsString(configurationMetaData));
        }
        catch (JacksonException e)
        {
            throw new RuntimeException("Unable to convert ["+configurationMetaData+"] to json format.");
        }
        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());

        return document;
    }

    public ConfigurationMetaData findById(String id)
    {
        String queryString = "id:\"" + id + "\" AND type: \"" + COMPONENT_CONFIGURATION + "\"";

        logger.debug("queryString: " + queryString);

        List<SolrComponentConfiguration> beans = this.findByQuery(queryString);

        if(beans.size() > 0 && beans.get(0).getRawConfigurationMetadata() != null)
        {
            return this.convert(beans.get(0).getRawConfigurationMetadata());
        }
        else
        {
            return null;
        }
    }

    public List<ConfigurationMetaData> findAll()
    {
        String queryString = "type: \"" + COMPONENT_CONFIGURATION + "\"";

        logger.debug("queryString: " + queryString);

        List<SolrComponentConfiguration> beans = this.findByQuery(queryString);

        return beans.stream().map(bean -> convert(bean.getRawConfigurationMetadata())).collect(Collectors.toList());
    }

    public List<ConfigurationMetaData> findInIdList(List<String> configurationIds)
    {
        if(configurationIds == null || configurationIds.isEmpty()) {
            return new ArrayList<>();
        }

        //Solr has an upper limit to the number of logical clauses of 1000, so partition the query.
        List<List<String>> partitions = ListUtils.partition(configurationIds, 500);
        List<ConfigurationMetaData> finalResults = new ArrayList<>();

        partitions.forEach(partition -> {
            StringBuffer queryString = new StringBuffer("type: \"").append(COMPONENT_CONFIGURATION).append("\"");
            queryString.append(" AND id:(");

            partition.forEach(id -> {
                queryString.append("\"").append(id).append("\",");
            });

            queryString.append(")");

            logger.debug("queryString: " + queryString);

            List<SolrComponentConfiguration> beans = this.findByQuery(queryString.toString());

            finalResults.addAll(beans.stream().map(bean -> convert(bean.getRawConfigurationMetadata())).collect(Collectors.toList()));
        });

        return finalResults;
    }

    private SolrConfigurationMetaData convert(String solrComponentConfiguration)
    {
        try
        {
            SolrConfigurationMetaData solrConfigurationMetaData
                = objectMapper.readValue(solrComponentConfiguration, SolrConfigurationMetaData.class);

            return solrConfigurationMetaData;
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Unable to deserialise ConfigurationMetaData [%s]"
                , solrComponentConfiguration), e);
        }
    }

    /**
     * Helper method to perform query.
     *
     * @param queryString
     * @return
     */
    private List<SolrComponentConfiguration> findByQuery(String queryString)
    {
        logger.debug("queryString: " + queryString);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);

        try
        {
            query.setStart(0);
            query.setRows(0);

            QueryRequest req = new QueryRequest(query,  SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            query.setRows((int)rsp.getResults().getNumFound());

            req = new QueryRequest(query,  SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            rsp = req.process(this.solrClient, SolrConstants.CORE);

            return rsp.getBeans(SolrComponentConfiguration.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr component configuration by query [" + queryString + "] from the ikasan solr index!", e);
        }
    }
}
