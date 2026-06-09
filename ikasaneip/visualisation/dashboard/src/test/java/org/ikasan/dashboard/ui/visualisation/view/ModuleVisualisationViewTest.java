package org.ikasan.dashboard.ui.visualisation.view;

import com.vaadin.flow.component.UI;
import org.apache.commons.io.IOUtils;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.ikasan.spec.metadata.model.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ConfigurationMetaDataService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.topology.metadata.JsonFlowMetaDataProvider;
import org.ikasan.topology.metadata.JsonModuleMetaDataProvider;
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

public class ModuleVisualisationViewTest extends UITest
{
    public static final String MODULE_JSON = "/data/graph/module.json";
    public static final String MODULE_FOUR_JSON = "/data/graph/module-four.json";
    public static final String SIMPLE_BOND_FLOW_JSON = "/data/graph/bondFlowsGraph.json";
    public static final String ELABORATE_BOND_FLOW_JSON = "/data/graph/bondFlowsGraphElaborate.json";
    public static final String BAD_JSON = "/data/graph/bad.json";
    public static final String BAD_XML = "/data/graph/bad.xml";

    private Mockery mockery = new Mockery()
    {{
        setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        setThreadingPolicy(new Synchroniser());
    }};

    ConfigurationMetaData configurationMetaData = mockery.mock(ConfigurationMetaData.class);
    ConfigurationParameterMetaData configurationParameterMetaData = mockery.mock(ConfigurationParameterMetaData.class);

    @MockitoBean
    private ConfigurationMetaDataService configurationMetadataService;

    @MockitoBean
    private ConfigurationService configurationRestService;

    @MockitoBean
    private ModuleMetaDataService moduleMetaDataService;

    @Override
    public void setup_expectations() {

    }



    @Test
    public void test_navigate_to_graph_view() throws IOException
    {
        UI.getCurrent().navigate("visualisation");

        GraphView graphView = _get(GraphView.class);

        Assertions.assertNotNull(graphView, "GraphView should not be null!");
    }

    @Test
    public void test_create_module_visualisation() throws IOException
    {
        JsonModuleMetaDataProvider provider = new JsonModuleMetaDataProvider(new JsonFlowMetaDataProvider());
        ModuleMetaData moduleMetaData = provider.deserialiseModule(loadDataFile(MODULE_JSON));

        Mockito.when(this.configurationRestService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        Mockito.when(this.moduleMetaDataService.findById(Mockito.anyString())).thenReturn(moduleMetaData);

        mockery.checking(new Expectations() {
            {
                exactly(2).of(configurationMetaData).getParameters();
                will(returnValue(List.of(configurationParameterMetaData)));

                exactly(2).of(configurationParameterMetaData).getName();
                will(returnValue("isRecording"));

                exactly(2).of(configurationParameterMetaData).getValue();
                will(returnValue(true));
            }
        });

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphVisualisation = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphVisualisation, "GraphVisualisation should not be null!");


        graphVisualisation.createModuleVisualisation(moduleMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        Assertions.assertNotNull(designerCanvas, "DesignerCanvas should not be null!");
    }

    @Test
    public void test_create_module_visualisation_with_multiple_flows() throws IOException
    {
        JsonModuleMetaDataProvider provider = new JsonModuleMetaDataProvider(new JsonFlowMetaDataProvider());
        ModuleMetaData moduleMetaData = provider.deserialiseModule(loadDataFile(MODULE_JSON));

        Mockito.when(this.configurationRestService.getFlowConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(configurationMetaData);

        Mockito.when(this.moduleMetaDataService.findById(Mockito.anyString())).thenReturn(moduleMetaData);

        mockery.checking(new Expectations() {
            {
                exactly(2).of(configurationMetaData).getParameters();
                will(returnValue(List.of(configurationParameterMetaData)));

                exactly(2).of(configurationParameterMetaData).getName();
                will(returnValue("isRecording"));

                exactly(2).of(configurationParameterMetaData).getValue();
                will(returnValue(true));
            }
        });

        UI.getCurrent().navigate("visualisation");

        GraphVisualisation graphVisualisation = _get(GraphVisualisation.class);

        Assertions.assertNotNull(graphVisualisation, "GraphVisualisation should not be null!");

        graphVisualisation.createModuleVisualisation(moduleMetaData);

        DesignerCanvas designerCanvas = _get(DesignerCanvas.class);
        Assertions.assertNotNull(designerCanvas, "DesignerCanvas should not be null!");
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
