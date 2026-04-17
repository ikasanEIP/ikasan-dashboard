package org.ikasan.replay.dao;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.replay.model.SolrReplayEvent;
import org.ikasan.spec.solr.SolrDaoBase;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by Ikasan Development on 29/08/2017.
 */
public class SolrReplayDaoTest
{
    /**
     * Mockery for mocking concrete classes
     */
    private Mockery mockery = new Mockery()
    {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        }
    };

    private SolrClient server = mockery.mock(SolrClient.class);

    @Test
    public void test_convert_entity_to_solr_input_document() {
        SolrReplayDao dao = new SolrReplayDao();

        byte[] eventBytes = "event".getBytes();

        SolrReplayEvent event = new SolrReplayEvent("eventId", eventBytes, "eventAsString"
            , "moduleName", "flowName", 30);
        event.setId(12l);

        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event);

        Assert.assertEquals("moduleName-replay-12", solrInputDocument.getFieldValue(SolrDaoBase.ID));
        Assert.assertEquals("moduleName", solrInputDocument.getFieldValue(SolrDaoBase.MODULE_NAME));
        Assert.assertEquals("replay", solrInputDocument.getFieldValue(SolrDaoBase.TYPE));
        Assert.assertEquals("flowName", solrInputDocument.getFieldValue(SolrDaoBase.FLOW_NAME));
        Assert.assertEquals("eventId", solrInputDocument.getFieldValue(SolrDaoBase.EVENT));
        Assert.assertEquals(eventBytes, solrInputDocument.getFieldValue(SolrDaoBase.PAYLOAD_CONTENT_RAW));
        Assert.assertEquals("eventAsString", solrInputDocument.getFieldValue(SolrDaoBase.PAYLOAD_CONTENT));
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(SolrDaoBase.EXPIRY));
    }
}
