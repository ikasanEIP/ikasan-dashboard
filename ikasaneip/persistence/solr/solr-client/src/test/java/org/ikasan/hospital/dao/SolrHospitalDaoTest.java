package org.ikasan.hospital.dao;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.hospital.model.SolrExclusionEventActionImpl;
import org.ikasan.spec.entity.EntityFields;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public class SolrHospitalDaoTest
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

        SolrHospitalDao dao = new SolrHospitalDao();
        dao.setSolrClient(server);
        dao.setDaysToKeep(0);

        SolrExclusionEventActionImpl event = new SolrExclusionEventActionImpl("moduleName", "flowName"
            , "uri", "actionedBy",  "action", "event", 12345L, "comment");


        dao.save(event);
    }


    @Test
    public void test_convert_entity_to_solr_input_document() {
        SolrHospitalDao dao = new SolrHospitalDao();

        SolrExclusionEventActionImpl event = new SolrExclusionEventActionImpl("moduleName", "flowName"
            , "uri", "actionedBy",  "action", "event", 12345L, "comment");

        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event);

        Assert.assertEquals("uri", solrInputDocument.getFieldValue(EntityFields.ID));
        Assert.assertEquals("moduleName", solrInputDocument.getFieldValue(EntityFields.MODULE_NAME));
        Assert.assertEquals("flowName", solrInputDocument.getFieldValue(EntityFields.FLOW_NAME));
        Assert.assertEquals("action", solrInputDocument.getFieldValue(EntityFields.HOSPITAL_EVENT_ACTION));
        Assert.assertEquals("event", solrInputDocument.getFieldValue(EntityFields.PAYLOAD_CONTENT));
        Assert.assertEquals("exclusionEventAction", solrInputDocument.getFieldValue(EntityFields.TYPE));
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(EntityFields.EXPIRY));
        Assert.assertEquals(12345L, solrInputDocument.getFieldValue(EntityFields.CREATED_DATE_TIME));
    }
}
