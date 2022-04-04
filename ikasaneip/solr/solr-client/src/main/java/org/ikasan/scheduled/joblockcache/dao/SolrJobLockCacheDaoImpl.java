package org.ikasan.scheduled.joblockcache.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.joblockcache.model.SolrJobLockCacheRecordImpl;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SolrJobLockCacheDaoImpl extends SolrDaoBase<JobLockCacheRecord> implements JobLockCacheDao {

    private static final String JOB_LOCK_CACHE_TYPE = "jockLockCacheRecordInstance";
    private static final String JOB_LOCK_CACHE_ID = "jockLockCacheRecordInstanceID";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrJobLockCacheDaoImpl.class);

    @Override
    public JobLockCacheRecord get() {
        SolrQuery query = super.buildIdQuery(JOB_LOCK_CACHE_ID, JOB_LOCK_CACHE_TYPE);
        LOGGER.debug("query: " + query);
        SearchResults<JobLockCacheRecord> searchResults = this.findByQuery(query, SolrJobLockCacheRecordImpl.class, 0, 1);
        return searchResults.getResultList().size() > 0 ? searchResults.getResultList().get(0) : null;
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, JobLockCacheRecord record) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, JOB_LOCK_CACHE_ID);
        document.addField(TYPE, JOB_LOCK_CACHE_TYPE);
        try {
            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(record.getJobLockCache()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert JobLockCacheRecord lockHolders to string! [%s]", record));
        }
        document.addField(CREATED_DATE_TIME, record.getTimestamp());

        document.setField(EXPIRY, expiry);

        LOGGER.debug(String.format("Converted JobLockCacheRecord to SolrDocument[%s]", document));
        return document;
    }

}
