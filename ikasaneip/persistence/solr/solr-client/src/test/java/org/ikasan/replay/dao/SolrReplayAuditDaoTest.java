package org.ikasan.replay.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.replay.model.SolrReplayAudit;
import org.ikasan.replay.model.SolrReplayAuditEvent;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.replay.ReplayAudit;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;


/**
 * Created by Ikasan Development on 29/08/2017.
 */
public class SolrReplayAuditDaoTest
{

    @Test
    public void test_convert_entity_to_solr_input_document() throws JsonProcessingException, JSONException {
        SolrReplayAuditDao dao = new SolrReplayAuditDao();

        byte[] eventBytes = "event".getBytes();

        ReplayAudit replayAudit = new SolrReplayAudit(1L, "user", "replayReason"
            , "targetServer", 12345l);

        SolrReplayAuditEvent event = new SolrReplayAuditEvent("id", replayAudit, true, "resultMessage", 12345L);


        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event);

        Assert.assertEquals("replay_audit", solrInputDocument.getFieldValue(EntityFields.TYPE));
        JSONAssert.assertEquals(new ObjectMapper().writeValueAsString(event)
            , solrInputDocument.getFieldValue(EntityFields.PAYLOAD_CONTENT).toString(), JSONCompareMode.LENIENT);
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(EntityFields.EXPIRY));
    }
}
