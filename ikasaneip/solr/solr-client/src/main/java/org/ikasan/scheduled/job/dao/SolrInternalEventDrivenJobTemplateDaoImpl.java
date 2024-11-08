package org.ikasan.scheduled.job.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobRecordImpl;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrInternalEventDrivenJobTemplateDaoImpl extends SolrInternalEventDrivenJobDaoImpl {

    private static Logger logger = LoggerFactory.getLogger(SolrInternalEventDrivenJobTemplateDaoImpl.class);

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, InternalEventDrivenJobRecord event) {
        SolrInputDocument document = super.convertEntityToSolrInputDocument(expiry, event);
        document.setField(TYPE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE);
        return document;
    }

    @Override
    public SearchResults<? extends InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return super.findByQuery(solrQuery, SolrInternalEventDrivenJobRecordImpl.class);
    }

    @Override
    public SearchResults<? extends InternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrInternalEventDrivenJobRecordImpl.class, offset, limit);
    }

    @Override
    public InternalEventDrivenJobRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE);

        logger.debug("query: " + query);

        List<? extends InternalEventDrivenJobRecord> beans = this.findByQuery(query, SolrInternalEventDrivenJobRecordImpl.class).getResultList();

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
    public void skip(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job tamplates");
    }

    @Override
    public void hold(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job tamplates");
    }

    @Override
    public void enable(InternalEventDrivenJobRecord jobRecord, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job tamplates");
    }

    @Override
    public void release(InternalEventDrivenJobRecord jobRecord, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job tamplates");
    }

    @Override
    public void releaseAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job tamplates");
    }

    @Override
    public void holdAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job tamplates");
    }

    @Override
    public void enableAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        throw new UnsupportedOperationException("This operation is not support for internal event driven job tamplates");
    }
}
