package org.ikasan.scheduled.joblock.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.joblock.model.SolrJobLockCacheAuditRecordImpl;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.ikasan.spec.entity.EntityFields.*;

public class SolrJobLockCacheAuditDaoImpl extends SolrDaoBase<JobLockCacheAuditRecord> implements JobLockCacheAuditDao {
    private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final Logger LOG = LoggerFactory.getLogger(SolrJobLockCacheDaoImpl.class);

    @Override
    public SearchResults<JobLockCacheAuditRecord> findAll(int limit, int offset) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(TYPE + COLON + JOB_LOCK_AUDIT_CACHE_TYPE);
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        LOG.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrJobLockCacheAuditRecordImpl.class);
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, JobLockCacheAuditRecord record) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, JOB_LOCK_AUDIT_CACHE_TYPE_ID + "_" + UUID.randomUUID());
        document.addField(TYPE, JOB_LOCK_AUDIT_CACHE_TYPE);
        try {
            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(record.getJobLockCache()));
        } catch (JacksonException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert JobLockCacheAuditRecord lockHolders to string! [%s]", record));
        }
        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, expiry);

        LOG.debug(String.format("Converted JobLockCacheAuditRecord to SolrDocument[%s]", document));
        return document;
    }

}
