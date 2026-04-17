package org.ikasan.wiretap.dao;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.wiretap.model.SolrWiretapEvent;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public class SolrWiretapDaoTest
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

    @Test(expected = RuntimeException.class)
    @DirtiesContext
    public void test_save_exception() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                // set event factory
                oneOf(server).request(with(any(UpdateRequest.class)));
                will(throwException(new RuntimeException("Error")));

            }
        });

        SolrWiretapDao dao = new SolrWiretapDao();
        dao.setSolrClient(server);
        dao.setDaysToKeep(0);

        SolrWiretapEvent event = new SolrWiretapEvent(1l, "moduleName", "flowName", "componentName",
                "eventId", "relatedEventId", System.currentTimeMillis(), "event");


        dao.save(event);
    }

    @Test
    public void test_convert_entity_to_solr_input_document() {
        SolrWiretapDao dao = new SolrWiretapDao();

        SolrWiretapEvent event = new SolrWiretapEvent(1l, "moduleName", "flowName", "componentName",
            "eventId", "relatedEventId", System.currentTimeMillis(), "event");

        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event);

        Assert.assertEquals("moduleName-wiretap-1", solrInputDocument.getFieldValue(SolrDaoBase.ID));
        Assert.assertEquals("moduleName", solrInputDocument.getFieldValue(SolrDaoBase.MODULE_NAME));
        Assert.assertEquals("wiretap", solrInputDocument.getFieldValue(SolrDaoBase.TYPE));
        Assert.assertEquals("flowName", solrInputDocument.getFieldValue(SolrDaoBase.FLOW_NAME));
        Assert.assertEquals("componentName", solrInputDocument.getFieldValue(SolrDaoBase.COMPONENT_NAME));
        Assert.assertEquals("eventId", solrInputDocument.getFieldValue(SolrDaoBase.EVENT));
        Assert.assertEquals("event", solrInputDocument.getFieldValue(SolrDaoBase.PAYLOAD_CONTENT));
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(SolrDaoBase.EXPIRY));
    }
}
