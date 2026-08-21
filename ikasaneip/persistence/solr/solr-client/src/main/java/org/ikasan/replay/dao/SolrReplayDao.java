package org.ikasan.replay.dao;

import org.apache.solr.common.SolrInputDocument;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.replay.ReplayEvent;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * Created by Ikasan Development Team on 25/08/2017.
 */
public class SolrReplayDao extends SolrDaoBase<ReplayEvent> implements EntityDao<ReplayEvent>
{
    private static Logger logger = LoggerFactory.getLogger(SolrReplayDao.class);

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ReplayEvent replayEvent)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, replayEvent.getModuleName() + "-replay-" + replayEvent.getId());
        document.addField(TYPE, REPLAY);
        document.addField(MODULE_NAME, replayEvent.getModuleName());
        document.addField(FLOW_NAME, replayEvent.getFlowName());
        document.addField(EVENT, replayEvent.getEventId());
        if(replayEvent.getEventAsString() != null && !replayEvent.getEventAsString().isEmpty())
        {
            document.addField(PAYLOAD_CONTENT, replayEvent.getEventAsString());
        }
        else
        {
            document.addField(PAYLOAD_CONTENT, new String(replayEvent.getEvent()));
        }
        document.addField(PAYLOAD_CONTENT_RAW, replayEvent.getEvent());
        document.addField(CREATED_DATE_TIME, replayEvent.getTimestamp());
        document.setField(EXPIRY, expiry);

        return document;
    }
}
