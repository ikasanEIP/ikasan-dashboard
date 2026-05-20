package org.ikasan.dashboard.ui.visualisation.component;

import org.ikasan.configurationService.metadata.JsonConfigurationMetaDataProvider;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.spec.metadata.model.ConfigurationMetaDataProvider;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;

@Ignore
public class ComponentConfigurationDialogTest extends UITest {

    public static final String LARGE_COMPONENT_CONFIGURATION = "/data/graph/largeComponentConfiguration.json";
    public static final String MODULE_JSON = "/data/graph/module.json";

    private Mockery mockery = new Mockery()
    {{
        setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        setThreadingPolicy(new Synchroniser());
    }};

    ConfigurationMetaDataProvider<String> configurationMetaDataProvider = new JsonConfigurationMetaDataProvider(null);

//    JsonObject jsonObject = mockery.mock(JsonObject.class);
//    JsonObject pointer = mockery.mock(JsonObject.class, "pointer");
//    JsonObject canvas = mockery.mock(JsonObject.class, "canvas");
//    JsonArray jsonNodes = mockery.mock(JsonArray.class, "nodes");
//    JsonValue jsonValue = mockery.mock(JsonValue.class, "node");
//
//
//    @MockitoBean
//    private ConfigurationService configurationRestService;
//
//    @MockitoBean
//    private ConfigurationMetaDataService configurationMetadataService;
//
    @Override
    public void setup_expectations() {

    }
//
//    @Test
//    public void test_open_large_mixed_component_configuration() throws IOException {
//
//        ConfigurationMetaData data = configurationMetaDataProvider.deserialiseMetadataConfiguration(loadDataFile(LARGE_COMPONENT_CONFIGURATION));
//
//        Mockito.when(this.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
//            .thenReturn(true);
//
//        SecurityContextHolder.getContext().setAuthentication(this.ikasanAuthentication);
//
//        UI.getCurrent().navigate("visualisation");
//
//        GraphView graphView = _get(GraphView.class);
//
//        JsonModuleMetaDataProvider provider = new JsonModuleMetaDataProvider(new JsonFlowMetaDataProvider());
//
//        GraphVisualisation graphVisualisation = (GraphVisualisation)ReflectionTestUtils.getField(graphView, "graphVisualisation");
//        ReflectionTestUtils.invokeMethod(graphVisualisation, "createModuleVisualisation", provider.deserialiseModule(loadDataFile(MODULE_JSON)));
//
//        NetworkDiagram networkDiagram = _get(NetworkDiagram.class);
//
//        Set<Node> nodes = networkDiagram.getNodesDataProvider().fetch(new Query<>()).collect(Collectors.toSet());
//
//        Optional<Node> consumer = nodes.stream().filter(node -> node.getId().equals("Test Consumer0")).findFirst();
//
//        consumer.get().clickedOn(consumer.get().getX(), consumer.get().getY());
//
//        mockery.checking(new Expectations()
//        {
//            {
//                // set event factory
//                oneOf(jsonObject).getObject(with(any(String.class)));
//                will(returnValue(pointer));
//                oneOf(pointer).getObject(with(any(String.class)));
//                will(returnValue(canvas));
//                oneOf(canvas).getNumber(with(any(String.class)));
//                will(returnValue(new Double(consumer.get().getX())));
//                oneOf(canvas).getNumber(with(any(String.class)));
//                will(returnValue(new Double(consumer.get().getY())));
//                oneOf(jsonObject).getArray(with(any(String.class)));
//                will(returnValue(jsonNodes));
//                oneOf(jsonNodes).length();
//                will(returnValue(5));
//                oneOf(jsonNodes).get(0);
//                will(returnValue(jsonValue));
//                oneOf(jsonValue).asString();
//                will(returnValue("Test Consumer0"));
//            }
//        });
//
//        ReflectionTestUtils.invokeMethod(networkDiagram, "fireEvent", new DoubleClickEvent(networkDiagram, true, jsonObject));
//
//        ComponentOptionsDialog componentOptionsDialog = _get(ComponentOptionsDialog.class);
//
//        componentOptionsDialog.getElement();
//
//        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(anyString(), anyString(), anyString(), anyString()))
//            .thenReturn(data);
//
//        _click(_get(Button.class, spec -> spec.withCaption("Component Configuration")));
//    }
//
//    protected String loadDataFile(String fileName) throws IOException
//    {
//        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");
//
//        return contentToSend;
//    }
//
//    protected InputStream loadDataFileStream(String fileName) throws IOException
//    {
//        return getClass().getResourceAsStream(fileName);
//    }
}
