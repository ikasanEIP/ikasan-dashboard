package org.ikasan.scheduled.joblock.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.joblock.model.SolrJobLockCacheRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class SolrJobLockCacheDaoImpl extends SolrDaoBase<JobLockCacheRecord> implements JobLockCacheDao {

    public static final String JOB_LOCK_CACHE_TYPE = "jobLockCache";
    public static final String JOB_LOCK_CACHE_ID = "jobLockCacheIdentifier";
    private static final JsonMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();
    private static final Logger LOG = LoggerFactory.getLogger(SolrJobLockCacheDaoImpl.class);

    @Override
    public JobLockCacheRecord get(String environment) {
        if(environment == null) environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        SolrQuery query = super.buildIdQuery(JOB_LOCK_CACHE_ID + "__" + environment, JOB_LOCK_CACHE_TYPE);
        LOG.debug("query: " + query);
        SearchResults<JobLockCacheRecord> searchResults = this.findByQuery(query, SolrJobLockCacheRecordImpl.class, 0, 1);
        return searchResults.getResultList().size() > 0 ? searchResults.getResultList().get(0) : null;
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, JobLockCacheRecord record) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, JOB_LOCK_CACHE_ID + "__" + record.getEnvironment());
        document.addField(TYPE, JOB_LOCK_CACHE_TYPE);
        try {
            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(record.getJobLockCache()));
        } catch (JacksonException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert JobLockCacheRecord lockHolders to string! [%s]", record));
        }
        if(record.getTimestamp() == 0) {
            document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
        }
        else {
            document.addField(CREATED_DATE_TIME, record.getTimestamp());
        }

        document.setField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        LOG.debug(String.format("Converted JobLockCacheRecord to SolrDocument[%s]", document));
        return document;
    }

}
