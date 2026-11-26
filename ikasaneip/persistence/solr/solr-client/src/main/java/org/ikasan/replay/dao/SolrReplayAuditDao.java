package org.ikasan.replay.dao;

import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayAuditEvent;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * Writes replay audits to Solr. It takes any {@link ReplayAuditEvent}: callers (the dashboard's replay dialog and bulk
 * replay) pass their own implementation, and typing the dao to one implementation made every write fail with a
 * ClassCastException. Each audit gets a unique id, so audits written in the same millisecond (bulk replay) are all kept.
 */
public class SolrReplayAuditDao extends SolrDaoBase<ReplayAuditEvent>
    implements BatchInsert<ReplayAuditEvent>, EntityDao<ReplayAuditEvent>
{
    private static Logger logger = LoggerFactory.getLogger(SolrReplayAuditDao.class);

    JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void insert(List<ReplayAuditEvent> entities)
    {
        long millisecondsInDay = (this.daysToKeep * TimeUnit.DAYS.toMillis(1));
        long expiry = millisecondsInDay + System.currentTimeMillis();

        try
        {
            UpdateRequest req = new UpdateRequest();
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            String json = mapper.writeValueAsString(entities);

            logger.debug("Result json: " + json);

            SolrInputDocument document = new SolrInputDocument();
            document.addField(ID, newId());
            document.addField(TYPE, REPLAY_AUDIT);
            document.addField(PAYLOAD_CONTENT, json);
            document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
            document.setField(EXPIRY, expiry);

            req.add(document);

            logger.debug("Adding document: " + document);


            commitSolrRequest(req);
        }
        catch (Exception e)
        {
            logger.error("An exception has occurred attempting to write replay audit event to Solr", e);
            throw new RuntimeException("An exception has occurred attempting to write replay audit event to Solr", e);
        }
    }

    private static String newId()
    {
        return "replay-audit-" + UUID.randomUUID();
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ReplayAuditEvent event)
    {
        SolrInputDocument document = new SolrInputDocument();

        try
        {
            String json = mapper.writeValueAsString(event);

            logger.debug("Result json: " + json);

            document.addField(ID, newId());
            document.addField(TYPE, REPLAY_AUDIT);
            document.addField(PAYLOAD_CONTENT, json);
            document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
            document.setField(EXPIRY, expiry);
        }
        catch (Exception e)
        {
            logger.error("An exception has occurred convert a replay audit event to a solr document", e);
            throw new RuntimeException("An exception has occurred convert a replay audit event to a solr document", e);
        }

        return document;
    }
}
