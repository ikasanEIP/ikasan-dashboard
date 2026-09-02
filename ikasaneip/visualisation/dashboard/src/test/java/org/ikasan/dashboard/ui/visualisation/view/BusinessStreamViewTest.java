package org.ikasan.dashboard.ui.visualisation.view;

import com.vaadin.flow.component.UI;
import org.apache.commons.io.IOUtils;
import org.ikasan.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.ikasan.spec.metadata.model.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.service.ConfigurationMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.*;

public class BusinessStreamViewTest extends UITest
{
    public static final String BUSINESS_STREAM_JSON = "/data/businessStream/sample-business-stream.json";
    public static final String EMPTY_BUSINESS_STREAM_JSON = "/data/businessStream/empty-business-stream.json";
    public static final String COMPLEX_BUSINESS_STREAM_JSON = "/data/businessStream/complex-business-stream.json";
    public static final String MALFORMED_BUSINESS_STREAM_JSON = "/data/businessStream/malformed-business-stream.json";

    private Mockery mockery = new Mockery()
    {{
        setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        setThreadingPolicy(new Synchroniser());
    }};

    /**
     * mocked container, listener and dao
     */
    ConfigurationMetaData configurationMetaData = mockery.mock(ConfigurationMetaData.class);

    @MockitoBean
    private ConfigurationMetaDataService configurationMetadataService;

    @MockitoBean
    private ConfigurationService configurationService;

    @Override
    public void setup_expectations() {

    }

    @Test
    public void test_create_business_stream_graph() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("BusinessStreamImpl");
        businessStreamMetaData.setId("id");
        businessStreamMetaData.setDescription("description");

        graphView.createBusinessStreamGraph("BusinessStreamImpl"
            , businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        Assertions.assertNotNull(designerCanvas, "designerCanvas should not be null!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }


    @Test
    public void test_navigate_to_visualisation_view() throws IOException
    {
        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        assertNotNull(graphView, "GraphVisualisation view should be accessible!");
    }

    @Test
    public void test_create_business_stream_with_empty_json() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(EMPTY_BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("EmptyBusinessStream");
        businessStreamMetaData.setId("empty-id");
        businessStreamMetaData.setDescription("Empty business stream for testing");

        graphView.createBusinessStreamGraph("EmptyBusinessStream", businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should not be null even with empty JSON!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_create_complex_business_stream_with_multiple_flows_and_connections() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(COMPLEX_BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("ComplexBusinessStream");
        businessStreamMetaData.setId("complex-id");
        businessStreamMetaData.setDescription("Complex business stream with multiple flows and connections");

        graphView.createBusinessStreamGraph("ComplexBusinessStream", businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should not be null!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_create_business_stream_with_null_description() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("BusinessStreamNoDescription");
        businessStreamMetaData.setId("no-desc-id");
        businessStreamMetaData.setDescription(null);

        graphView.createBusinessStreamGraph("BusinessStreamNoDescription", businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should not be null even with null description!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_create_business_stream_with_empty_name() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("");
        businessStreamMetaData.setId("empty-name-id");
        businessStreamMetaData.setDescription("Business stream with empty name");

        graphView.createBusinessStreamGraph("", businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should not be null even with empty name!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_create_business_stream_with_special_characters_in_name() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("Business-Stream_Test.2025@Version#1");
        businessStreamMetaData.setId("special-chars-id");
        businessStreamMetaData.setDescription("Business stream with special characters in name");

        graphView.createBusinessStreamGraph("Business-Stream_Test.2025@Version#1", businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should handle special characters in name!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_create_business_stream_with_long_description() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        String longDescription = "This is a very long description that tests how the system handles extensive text. ".repeat(20);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("LongDescriptionStream");
        businessStreamMetaData.setId("long-desc-id");
        businessStreamMetaData.setDescription(longDescription);

        graphView.createBusinessStreamGraph("LongDescriptionStream", businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should handle long descriptions!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_create_business_stream_verifies_metadata_properties() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        String expectedName = "TestBusinessStream";
        String expectedId = "test-bs-id-123";
        String expectedDescription = "This is a test business stream description";

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName(expectedName);
        businessStreamMetaData.setId(expectedId);
        businessStreamMetaData.setDescription(expectedDescription);

        // Verify metadata is correctly set
        assertEquals(expectedName, businessStreamMetaData.getName(), "Name should match");
        assertEquals(expectedId, businessStreamMetaData.getId(), "ID should match");
        assertEquals(expectedDescription, businessStreamMetaData.getDescription(), "Description should match");

        graphView.createBusinessStreamGraph(expectedName, businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should not be null!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_multiple_business_stream_creations_sequentially() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        // Create first business stream
        BusinessStreamMetaDataImpl businessStreamMetaData1 = new BusinessStreamMetaDataImpl();
        businessStreamMetaData1.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData1.setName("FirstStream");
        businessStreamMetaData1.setId("first-id");
        businessStreamMetaData1.setDescription("First business stream");

        graphView.createBusinessStreamGraph("FirstStream", businessStreamMetaData1);

        DesignerCanvas designerCanvas1 = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas1, "First DesignerCanvas should not be null!");

        // Create second business stream
        BusinessStreamMetaDataImpl businessStreamMetaData2 = new BusinessStreamMetaDataImpl();
        businessStreamMetaData2.setJson(loadDataFile(COMPLEX_BUSINESS_STREAM_JSON));
        businessStreamMetaData2.setName("SecondStream");
        businessStreamMetaData2.setId("second-id");
        businessStreamMetaData2.setDescription("Second business stream");

        graphView.createBusinessStreamGraph("SecondStream", businessStreamMetaData2);

        DesignerCanvas designerCanvas2 = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas2, "Second DesignerCanvas should not be null!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_business_stream_with_unicode_characters() throws IOException
    {
        Mockito.when(this.configurationService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphView = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphView);

        BusinessStreamMetaDataImpl businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setJson(loadDataFile(BUSINESS_STREAM_JSON));
        businessStreamMetaData.setName("商業流程 Business Stream 데이터");
        businessStreamMetaData.setId("unicode-id-日本語");
        businessStreamMetaData.setDescription("Description with émojis: 🚀💡✨ and spëcial çhars");

        graphView.createBusinessStreamGraph("商業流程 Business Stream 데이터", businessStreamMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        assertNotNull(designerCanvas, "DesignerCanvas should handle unicode characters!");

        Mockito.verifyNoMoreInteractions(this.configurationService);
        mockery.assertIsSatisfied();
    }

    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName));

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}
