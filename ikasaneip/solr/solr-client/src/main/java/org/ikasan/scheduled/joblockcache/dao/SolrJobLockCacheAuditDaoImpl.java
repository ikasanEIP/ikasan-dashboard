package org.ikasan.scheduled.joblockcache.dao;

import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.joblockcache.model.SolrJobLockCacheAuditRecordImpl;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SolrJobLockCacheAuditDaoImpl extends SolrDaoBase<JobLockCacheAuditRecord> implements JobLockCacheAuditDao {
    private static final String JOB_LOCK_AUDIT_CACHE_TYPE = "jockLockCacheRecordAudit";
    private static final String JOB_LOCK_AUDIT_CACHE_TYPE_ID = "jockLockCacheRecordAuditID";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert JobLockCacheAuditRecord lockHolders to string! [%s]", record));
        }
        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, expiry);

        LOG.debug(String.format("Converted JobLockCacheAuditRecord to SolrDocument[%s]", document));
        return document;
    }

}
