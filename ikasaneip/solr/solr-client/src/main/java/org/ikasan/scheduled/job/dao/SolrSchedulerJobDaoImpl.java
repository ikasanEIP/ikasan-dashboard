package org.ikasan.scheduled.job.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrSchedulerJobRecordImpl;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrSchedulerJobDaoImpl extends SolrDaoBase<SchedulerJobRecord>
    implements SchedulerJobDao<SchedulerJobRecord> {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrSchedulerJobDaoImpl.class);

    @Override
    public SearchResults<? extends SchedulerJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(OPEN_BRACKET);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        typeBuffer.append(CLOSE_BRACKET);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class);
    }

    @Override
    public SearchResults<? extends SchedulerJobRecord> findByContext(String contextId, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class);
    }

    @Override
    public SearchResults<? extends SchedulerJobRecord> findByAgent(String agentName, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);
        queryBuffer.append(AND).append(" ").append(MODULE_NAME).append(COLON);
        queryBuffer.append("\"").append(agentName).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        if(limit == -1 && offset == -1) {
            solrQuery.setRows(0);
            solrQuery.setStart(0);
            solrQuery.setRows((int)this.findByQuery(solrQuery
                , SolrSchedulerJobRecordImpl.class).getTotalNumberOfResults());
        }

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class);
    }

    @Override
    public SchedulerJobRecord findById(String id) {
        SolrQuery query = this.buildIdQuery(id);

        logger.debug("query: " + query);

        List<? extends SchedulerJobRecord> beans = this.findByQuery(query, SolrSchedulerJobRecordImpl.class).getResultList();

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }

    @Override
    public void delete(SchedulerJobRecord record) {
        super.removeById(record.getType(), record.getId());
    }

    @Override
    public void deleteByAgentName(String agentName) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);
        queryBuffer.append(AND).append(" ").append(MODULE_NAME).append(COLON);
        queryBuffer.append("\"").append(agentName).append("\" ");

        super.deleteByQuery(queryBuffer.toString());
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SchedulerJobRecord event) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    @Override
    public void save(SchedulerJobRecord event) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    @Override
    public void save(List<SchedulerJobRecord> events) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    protected SolrQuery buildIdQuery(String id)
    {
        StringBuffer idBuffer = new StringBuffer();
        StringBuffer typeBuffer = new StringBuffer();


        idBuffer.append(ID).append(COLON).append("\"").append(id).append("\"");

        typeBuffer.append(OPEN_BRACKET);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        typeBuffer.append(CLOSE_BRACKET);


        StringBuffer bufferFinalQuery = new StringBuffer(idBuffer);

        if(typeBuffer.length() > 0)
        {
            bufferFinalQuery.append(AND).append(typeBuffer);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(bufferFinalQuery.toString());

        return solrQuery;
    }
}
