package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobRecordImpl;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrFileEventDrivenJobRecordDaoImpl extends SolrDaoBase<FileEventDrivenJobRecord>
    implements FileEventDrivenJobRecordDao<FileEventDrivenJobRecord> {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrFileEventDrivenJobRecordDaoImpl.class);


    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, FileEventDrivenJobRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, JobConstants.FILE_EVENT_DRIVEN_JOB);
        try {
            document.addField(PAYLOAD_CONTENT, this.getFileEventDrivenJob(event.getFileEventDrivenJob()));
            document.addField(COMPONENT_NAME, event.getFileEventDrivenJob().getContextId());
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert FileEventDrivenJob to string! [%s]", event));
        }

        document.addField(ID, event.getId());
        document.addField(MODULE_NAME, event.getAgentName());
        document.addField(FLOW_NAME, event.getJobName());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled process event to SolrDocument[%s]", document));
        return document;
    }

    private String getFileEventDrivenJob(FileEventDrivenJob scheduledProcessEvent) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(scheduledProcessEvent);
    }

    @Override
    public SearchResults<? extends FileEventDrivenJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrFileEventDrivenJobRecordImpl.class);
    }


    @Override
    public SearchResults<? extends FileEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrFileEventDrivenJobRecordImpl.class);
    }


    @Override
    public FileEventDrivenJobRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, JobConstants.FILE_EVENT_DRIVEN_JOB);

        logger.debug("query: " + query);

        List<? extends FileEventDrivenJobRecord> beans = this.findByQuery(query, SolrFileEventDrivenJobRecordImpl.class).getResultList();

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }
}
