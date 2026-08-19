package org.ikasan.replay.dao;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.replay.model.SolrReplayEvent;
import org.ikasan.spec.entity.EntityFields;
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

        Assert.assertEquals("moduleName-replay-12", solrInputDocument.getFieldValue(EntityFields.ID));
        Assert.assertEquals("moduleName", solrInputDocument.getFieldValue(EntityFields.MODULE_NAME));
        Assert.assertEquals("replay", solrInputDocument.getFieldValue(EntityFields.TYPE));
        Assert.assertEquals("flowName", solrInputDocument.getFieldValue(EntityFields.FLOW_NAME));
        Assert.assertEquals("eventId", solrInputDocument.getFieldValue(EntityFields.EVENT));
        Assert.assertEquals(eventBytes, solrInputDocument.getFieldValue(EntityFields.PAYLOAD_CONTENT_RAW));
        Assert.assertEquals("eventAsString", solrInputDocument.getFieldValue(EntityFields.PAYLOAD_CONTENT));
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(EntityFields.EXPIRY));
    }
}
