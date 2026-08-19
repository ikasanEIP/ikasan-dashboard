package org.ikasan.error.reporting.dao;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDaoImpl;
import org.ikasan.error.reporting.model.SolrErrorOccurrence;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.solr.SolrDaoBase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public class SolrErrorReportingServiceDaoTest
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

        SolrErrorReportingServiceDaoImpl dao = new SolrErrorReportingServiceDaoImpl();
        dao.setSolrClient(server);
        dao.setDaysToKeep(0);

        SolrErrorOccurrence event = new SolrErrorOccurrence("uri", "moduleName", "flowName"
            , "componentName", "action", "detail", "message", "exceptionClass"
            , "eventId", "relatedEventId", "eventAsString", 12345L);

        dao.save(event);
    }

    @Test
    public void test_convert_entity_to_solr_input_document() {
        SolrErrorReportingServiceDaoImpl dao = new SolrErrorReportingServiceDaoImpl();

        SolrErrorOccurrence event = new SolrErrorOccurrence("uri", "moduleName", "flowName"
            , "componentName", "action", "detail", "message", "exceptionClass"
            , "eventId", "relatedEventId", "eventAsString", 12345L);

        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event);

        Assert.assertEquals("moduleName-error-uri", solrInputDocument.getFieldValue(EntityFields.ID));
        Assert.assertEquals("uri", solrInputDocument.getFieldValue(EntityFields.ERROR_URI));
        Assert.assertEquals("moduleName", solrInputDocument.getFieldValue(EntityFields.MODULE_NAME));
        Assert.assertEquals("flowName", solrInputDocument.getFieldValue(EntityFields.FLOW_NAME));
        Assert.assertEquals("componentName", solrInputDocument.getFieldValue(EntityFields.COMPONENT_NAME));
        Assert.assertEquals("action", solrInputDocument.getFieldValue(EntityFields.ERROR_ACTION));
        Assert.assertEquals("detail", solrInputDocument.getFieldValue(EntityFields.ERROR_DETAIL));
        Assert.assertEquals("message", solrInputDocument.getFieldValue(EntityFields.ERROR_MESSAGE));
        Assert.assertEquals("exceptionClass", solrInputDocument.getFieldValue(EntityFields.EXCEPTION_CLASS));
        Assert.assertEquals("eventId", solrInputDocument.getFieldValue(EntityFields.EVENT));
        Assert.assertEquals("relatedEventId", solrInputDocument.getFieldValue(EntityFields.RELATED_EVENT));
        Assert.assertEquals("eventAsString", solrInputDocument.getFieldValue(EntityFields.PAYLOAD_CONTENT));
        Assert.assertEquals("error", solrInputDocument.getFieldValue(EntityFields.TYPE));
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(EntityFields.EXPIRY));
        Assert.assertEquals(12345L, solrInputDocument.getFieldValue(EntityFields.CREATED_DATE_TIME));
    }
}
