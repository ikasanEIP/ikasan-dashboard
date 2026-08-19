package org.ikasan.scheduled.context.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.context.model.SolrScheduledContextViewRecordImpl;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ikasan.spec.entity.EntityFields.*;

public class SolrScheduledContextViewDaoImpl extends SolrDaoBase<ScheduledContextViewRecord> implements ScheduledContextViewDao {
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrScheduledContextViewDaoImpl.class);

    public static final String SCHEDULED_CONTEXT_VIEW = "scheduledContextView";

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextViewRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, SCHEDULED_CONTEXT_VIEW);
        document.addField(PAYLOAD_CONTENT, event.getContextView());
        document.addField(ID, event.getParentContextName() + "-" + event.getContextName() + "-" + SCHEDULED_CONTEXT_VIEW);
        document.addField(MODULE_NAME, event.getParentContextName());
        document.addField(FLOW_NAME, event.getContextName());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, event.getModifiedBy());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        logger.debug(String.format("Converted scheduled context view record to SolrDocument[%s]", document));
        return document;
    }

    @Override
    public ScheduledContextViewRecord getContextView(String parentContextName, String contextName) {
        SolrQuery query = new SolrQuery(super.buildFieldPredicate(parentContextName, MODULE_NAME)
            .append(AND)
            .append(super.buildFieldPredicate(contextName, FLOW_NAME))
            .append(AND)
            .append(super.buildFieldPredicate(SCHEDULED_CONTEXT_VIEW, TYPE)).toString());

        logger.debug("query: " + query);

        SearchResults<? extends ScheduledContextViewRecord> searchResults = this
            .findByQuery(query, SolrScheduledContextViewRecordImpl.class, 0, 1);

        if(searchResults.getResultList().size() > 0)
        {
            return searchResults.getResultList().get(0);
        }
        else
        {
            return null;
        }
    }

}
