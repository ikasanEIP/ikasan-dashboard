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

    /**
     * The dashboard passes its own ReplayAuditEvent implementation; the dao must take any implementation (it used to
     * be typed to SolrReplayAuditEvent, so every audit the dashboard wrote failed with a ClassCastException).
     */
    @Test
    public void test_convert_any_replay_audit_event_implementation() throws JsonProcessingException, JSONException {
        SolrReplayAuditDao dao = new SolrReplayAuditDao();
        ReplayAudit replayAudit = new SolrReplayAudit(1L, "user", "replayReason", "targetServer", 12345L);
        OtherReplayAuditEvent event = new OtherReplayAuditEvent("other-id", replayAudit, true, "resultMessage", 12345L);

        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event);

        Assert.assertEquals("replay_audit", solrInputDocument.getFieldValue(EntityFields.TYPE));
        JSONAssert.assertEquals(new ObjectMapper().writeValueAsString(event)
            , solrInputDocument.getFieldValue(EntityFields.PAYLOAD_CONTENT).toString(), JSONCompareMode.LENIENT);
    }

    /** Audits converted in the same millisecond (a bulk replay) get different ids, so none overwrites another. */
    @Test
    public void test_each_audit_gets_its_own_id() {
        SolrReplayAuditDao dao = new SolrReplayAuditDao();
        ReplayAudit replayAudit = new SolrReplayAudit(1L, "user", "replayReason", "targetServer", 12345L);

        java.util.Set<Object> ids = new java.util.HashSet<>();
        for (int i = 0; i < 50; i++) {
            ids.add(dao.convertEntityToSolrInputDocument(1L,
                new SolrReplayAuditEvent("id" + i, replayAudit, true, "resultMessage", 12345L)).getFieldValue(EntityFields.ID));
        }

        Assert.assertEquals(50, ids.size());
    }

    /** A ReplayAuditEvent that is not a SolrReplayAuditEvent, like the dashboard's ReplayAuditEventImpl. */
    public static class OtherReplayAuditEvent implements org.ikasan.spec.replay.ReplayAuditEvent<String> {
        private String id;
        private ReplayAudit replayAudit;
        private boolean success;
        private String resultMessage;
        private long timestamp;

        OtherReplayAuditEvent(String id, ReplayAudit replayAudit, boolean success, String resultMessage, long timestamp) {
            this.id = id;
            this.replayAudit = replayAudit;
            this.success = success;
            this.resultMessage = resultMessage;
            this.timestamp = timestamp;
        }

        @Override public String getId() { return id; }
        @Override public void setId(String id) { this.id = id; }
        @Override public ReplayAudit getReplayAudit() { return replayAudit; }
        @Override public void setReplayAudit(ReplayAudit replayAudit) { this.replayAudit = replayAudit; }
        @Override public boolean isSuccess() { return success; }
        @Override public void setSuccess(boolean success) { this.success = success; }
        @Override public String getResultMessage() { return resultMessage; }
        @Override public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
        @Override public long getTimestamp() { return timestamp; }
        @Override public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
