package org.ikasan.rest.dashboard;

import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.BigQueueModuleService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {BigQueueModuleController.class})
@EnableWebMvc
public class BigQueueModuleControllerTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    protected MockMvc mockMvc;

    @MockBean
    private BigQueueModuleService bigQueueModuleService;

    @MockBean
    private ModuleMetaDataService moduleMetaDataService;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @BeforeEach
    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void get_queues_admin() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.listQueues("localhost")).thenReturn(List.of("queueName1", "queueName2"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("[\"queueName1\",\"queueName2\"]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).listQueues("localhost");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_null_response() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.listQueues("localhost")).thenReturn(null);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).listQueues("localhost");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_no_modules_found() throws Exception {
        // Empty Module list
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();

        when(bigQueueModuleService.listQueues("localhost")).thenReturn(List.of("queueName1", "queueName2"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to list queues for the module [someModule]. Error [The module was not found in the Ikasan Dashboard]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_runtime_exception() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.listQueues("localhost")).thenThrow(new RuntimeException("Expected"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to list queues for the module [someModule]. Error [Expected]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).listQueues("localhost");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_size_by_module() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.size("localhost", "queueName1")).thenReturn(1L);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("1",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_size_by_module_does_not_exist() throws Exception {
        // Empty
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();

        when(bigQueueModuleService.size("localhost", "queueName1")).thenReturn(1L);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get size of queue [queueName1] for the module [someModule]. " +
                "Error [The module was not found in the Ikasan Dashboard]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_size_by_module_runtime_exception() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.size("localhost", "queueName1")).thenThrow(new RuntimeException("Expected"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get size of queue [queueName1] for the module [someModule]. " +
                "Error [Expected]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_module() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        Map<String, Long> allQueueSizes = new HashMap<>();
        allQueueSizes.put("queue1", 0L);
        allQueueSizes.put("queue2", 3L);
        allQueueSizes.put("queue3", 0L);
        allQueueSizes.put("queue4", 4L);

        when(bigQueueModuleService.size("localhost", true)).thenReturn(allQueueSizes);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/module/true/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("{\"queue1\":0,\"queue2\":3,\"queue3\":0,\"queue4\":4}",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost", true);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_module_include_zero_false() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        Map<String, Long> allQueueSizes = new HashMap<>();
        allQueueSizes.put("queue2", 3L);
        allQueueSizes.put("queue4", 4L);

        when(bigQueueModuleService.size("localhost", false)).thenReturn(allQueueSizes);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/module/false/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("{\"queue2\":3,\"queue4\":4}",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost", false);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_module_null_response() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        Map<String, Long> allQueueSizes = new HashMap<>();

        when(bigQueueModuleService.size("localhost", true)).thenReturn(allQueueSizes);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/module/true/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("{}",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost", true);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_module_does_not_exist() throws Exception {
        // Empty
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();

        Map<String, Long> allQueueSizes = new HashMap<>();

        when(bigQueueModuleService.size("localhost", true)).thenReturn(allQueueSizes);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/module/true/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get all sizes of the queues for the module [someModule] with setting includeZeros = [true]. " +
                "Error [The module was not found in the Ikasan Dashboard]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_module_run_time_exeception() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.size("localhost", true)).thenThrow(new RuntimeException("Expected"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/module/true/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get all sizes of the queues for the module [someModule] with setting includeZeros = [true]. " +
                "Error [Expected]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost", true);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_schedule_agents() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule1");
        moduleMetaData.setUrl("localhost1");
        moduleMetaData.setType(ModuleType.SCHEDULER_AGENT);
        mockMetadataModuleDtos.add(moduleMetaData);

        ModuleMetaData moduleMetaData2 = new ModuleMetaDataImpl();
        moduleMetaData2.setName("someModule2");
        moduleMetaData2.setUrl("localhost2");
        moduleMetaData2.setType(ModuleType.SCHEDULER_AGENT);
        mockMetadataModuleDtos.add(moduleMetaData2);

        Map<String, Long> allQueueSizes = new HashMap<>();
        allQueueSizes.put("queue1", 0L);
        allQueueSizes.put("queue2", 3L);
        allQueueSizes.put("queue3", 0L);
        allQueueSizes.put("queue4", 4L);

        when(bigQueueModuleService.size("localhost1", true)).thenReturn(allQueueSizes);
        when(bigQueueModuleService.size("localhost2", true)).thenReturn(allQueueSizes);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/all/true/SCHEDULER_AGENT")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("" +
                "[" +
                    "{\"moduleName\":\"someModule1\",\"moduleUrl\":\"localhost1\"," +
                    "\"queueSizeMap\":{\"queue1\":0,\"queue2\":3,\"queue3\":0,\"queue4\":4}," +
                    "\"successful\":true}," +
                    "{\"moduleName\":\"someModule2\",\"moduleUrl\":\"localhost2\"," +
                    "\"queueSizeMap\":{\"queue1\":0,\"queue2\":3,\"queue3\":0,\"queue4\":4}," +
                    "\"successful\":true}" +
                "]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost1", true);
        verify(bigQueueModuleService).size("localhost2", true);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_all_module() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule1");
        moduleMetaData.setUrl("localhost1");
        moduleMetaData.setType(ModuleType.SCHEDULER_AGENT);
        mockMetadataModuleDtos.add(moduleMetaData);

        ModuleMetaData moduleMetaData2 = new ModuleMetaDataImpl();
        moduleMetaData2.setName("someModule2");
        moduleMetaData2.setUrl("localhost2");
        moduleMetaData2.setType(ModuleType.INTEGRATION_MODULE);
        mockMetadataModuleDtos.add(moduleMetaData2);

        Map<String, Long> allQueueSizes = new HashMap<>();
        allQueueSizes.put("queue1", 0L);
        allQueueSizes.put("queue2", 3L);
        allQueueSizes.put("queue3", 0L);
        allQueueSizes.put("queue4", 4L);

        when(bigQueueModuleService.size("localhost1", true)).thenReturn(allQueueSizes);
        when(bigQueueModuleService.size("localhost2", true)).thenReturn(allQueueSizes);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/all/true/ALL")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("" +
                "[" +
                "{\"moduleName\":\"someModule1\",\"moduleUrl\":\"localhost1\"," +
                "\"queueSizeMap\":{\"queue1\":0,\"queue2\":3,\"queue3\":0,\"queue4\":4}," +
                "\"successful\":true}," +
                "{\"moduleName\":\"someModule2\",\"moduleUrl\":\"localhost2\"," +
                "\"queueSizeMap\":{\"queue1\":0,\"queue2\":3,\"queue3\":0,\"queue4\":4}" +
                ",\"successful\":true}" +
                "]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost1", true);
        verify(bigQueueModuleService).size("localhost2", true);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_all_module_integration_no_bigqueue() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule1");
        moduleMetaData.setUrl("localhost1");
        moduleMetaData.setType(ModuleType.SCHEDULER_AGENT);
        mockMetadataModuleDtos.add(moduleMetaData);

        ModuleMetaData moduleMetaData2 = new ModuleMetaDataImpl();
        moduleMetaData2.setName("someModule2");
        moduleMetaData2.setUrl("localhost2");
        moduleMetaData2.setType(ModuleType.INTEGRATION_MODULE);
        mockMetadataModuleDtos.add(moduleMetaData2);

        Map<String, Long> allQueueSizes = new HashMap<>();
        allQueueSizes.put("queue1", 0L);
        allQueueSizes.put("queue2", 3L);
        allQueueSizes.put("queue3", 0L);
        allQueueSizes.put("queue4", 4L);

        when(bigQueueModuleService.size("localhost1", true)).thenReturn(allQueueSizes);
        when(bigQueueModuleService.size("localhost2", true)).thenReturn(null);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/all/true/ALL")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("" +
                "[" +
                    "{\"moduleName\":\"someModule1\",\"moduleUrl\":\"localhost1\",\"queueSizeMap\":{\"queue1\":0,\"queue2\":3,\"queue3\":0,\"queue4\":4},\"successful\":true}," +
                    "{\"moduleName\":\"someModule2\",\"moduleUrl\":\"localhost2\",\"queueSizeMap\":{},\"successful\":false}" +
                "]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost1", true);
        verify(bigQueueModuleService).size("localhost2", true);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_integration_module() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule1");
        moduleMetaData.setUrl("localhost1");
        moduleMetaData.setType(ModuleType.INTEGRATION_MODULE);
        mockMetadataModuleDtos.add(moduleMetaData);

        // Integration module may not have have BIGQUEUE
        when(bigQueueModuleService.size("localhost1", true)).thenReturn(null);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/all/true/INTEGRATION_MODULE")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("" +
                "[{\"moduleName\":\"someModule1\",\"moduleUrl\":\"localhost1\",\"queueSizeMap\":{},\"successful\":false}]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).size("localhost1", true);
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_all_queues_size_by_invalid_module_type() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/size/all/true/INVALID")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Module Type must one of, [INTEGRATION_MODULE], [SCHEDULER_AGENT] or [ALL]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void peek_queues_admin() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder()
            .withMessageId("uuidAsMessageId")
            .withCreatedTime(1657509967)
            .withMessage("some message").build();

        when(bigQueueModuleService.peek("localhost", "queueName1")).thenReturn(bigQueueMessage);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/peek/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("{\"messageId\":\"uuidAsMessageId\",\"createdTime\":1657509967,\"message\":\"some message\",\"messageProperties\":null}",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).peek("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void peek_queues_module_not_exist() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();

        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder()
            .withMessageId("uuidAsMessageId")
            .withCreatedTime(1657509967)
            .withMessage("some message").build();

        when(bigQueueModuleService.peek("localhost", "queueName1")).thenReturn(bigQueueMessage);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/peek/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get the first message from the queue [queueName1] for the module [someModule]. " +
                "Error [The module was not found in the Ikasan Dashboard]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void peek_queues_no_messages() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.peek("localhost", "queueName1")).thenReturn(null);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/peek/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).peek("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void peek_queues_module_runtime_exception() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.peek("localhost", "queueName1")).thenThrow(new RuntimeException("Expected"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/peek/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get the first message from the queue [queueName1] for the module [someModule]. " +
                "Error [Expected]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).peek("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_messages() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        BigQueueMessage bigQueueMessage1 = new BigQueueMessageBuilder()
            .withMessageId("uuidAsMessageId1")
            .withCreatedTime(1657509967)
            .withMessage("some message 1").build();

        BigQueueMessage bigQueueMessage2 = new BigQueueMessageBuilder()
            .withMessageId("uuidAsMessageId2")
            .withCreatedTime(1657509960)
            .withMessage("some message 2").build();

        when(bigQueueModuleService.getMessages("localhost", "queueName1")).thenReturn(List.of(bigQueueMessage1, bigQueueMessage2));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/messages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("[{\"messageId\":\"uuidAsMessageId1\",\"createdTime\":1657509967,\"message\":\"some message 1\",\"messageProperties\":null}," +
                "{\"messageId\":\"uuidAsMessageId2\",\"createdTime\":1657509960,\"message\":\"some message 2\",\"messageProperties\":null}]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).getMessages("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_messages_no_module_found() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();

        BigQueueMessage bigQueueMessage1 = new BigQueueMessageBuilder()
            .withMessageId("uuidAsMessageId1")
            .withCreatedTime(1657509967)
            .withMessage("some message 1").build();

        BigQueueMessage bigQueueMessage2 = new BigQueueMessageBuilder()
            .withMessageId("uuidAsMessageId2")
            .withCreatedTime(1657509960)
            .withMessage("some message 2").build();

        when(bigQueueModuleService.getMessages("localhost", "queueName1")).thenReturn(List.of(bigQueueMessage1, bigQueueMessage2));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/messages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get all messages from the queue [queueName1] for the module [someModule]. " +
                "Error [The module was not found in the Ikasan Dashboard]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_messages_no_messages() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.getMessages("localhost", "queueName1")).thenReturn(new ArrayList());
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/messages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("[]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).getMessages("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void get_queues_messages_runtime_exception() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.getMessages("localhost", "queueName1")).thenThrow(new RuntimeException("Expected"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/module/bigQueue/messages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to get all messages from the queue [queueName1] for the module [someModule]. " +
                "Error [Expected]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).getMessages("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void delete_queues() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.deleteMessage("localhost", "queueName1", "uuidAsMessageId1")).thenReturn(true);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/queueName1/uuidAsMessageId1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("true",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).deleteMessage("localhost", "queueName1", "uuidAsMessageId1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void delete_queues_returns_false() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.deleteMessage("localhost", "queueName1", "uuidAsMessageId1")).thenReturn(false);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/queueName1/uuidAsMessageId1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("false",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).deleteMessage("localhost", "queueName1", "uuidAsMessageId1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void delete_queues_module_do_not_exist() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();

        when(bigQueueModuleService.deleteMessage("localhost", "queueName1", "uuidAsMessageId1")).thenReturn(true);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/queueName1/uuidAsMessageId1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to delete the messagesId [uuidAsMessageId1] from the queue [queueName1] for the module [someModule]. " +
                "Error [The module was not found in the Ikasan Dashboard]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void delete_queues_runtime_exception() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.deleteMessage("localhost", "queueName1", "uuidAsMessageId1")).thenThrow(new RuntimeException("Expected"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/queueName1/uuidAsMessageId1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to delete the messagesId [uuidAsMessageId1] from the queue [queueName1] for the module [someModule]. " +
                "Error [Expected]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).deleteMessage("localhost", "queueName1", "uuidAsMessageId1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void deleteAllMessages_queues() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.deleteAllMessage("localhost", "queueName1")).thenReturn(true);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/allMessages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("true",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).deleteAllMessage("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void deleteAllMessages_queues_returns_false() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.deleteAllMessage("localhost", "queueName1")).thenReturn(false);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/allMessages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("false",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).deleteAllMessage("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void deleteAllMessages_queues_module_do_not_exist() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();

        when(bigQueueModuleService.deleteAllMessage("localhost", "queueName1")).thenReturn(true);
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/allMessages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to delete all messages from the queue [queueName1] for the module [someModule]. " +
                "Error [The module was not found in the Ikasan Dashboard]",
            result.getResponse().getContentAsString());

        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }

    @Test
    public void deleteAllMessages_queues_runtime_exception() throws Exception {
        List<ModuleMetaData> mockMetadataModuleDtos = new ArrayList<>();
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("someModule");
        moduleMetaData.setUrl("localhost");
        mockMetadataModuleDtos.add(moduleMetaData);

        when(bigQueueModuleService.deleteAllMessage("localhost", "queueName1")).thenThrow(new RuntimeException("Expected"));
        when(moduleMetaDataService.findAll()).thenReturn(mockMetadataModuleDtos);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/module/bigQueue/delete/allMessages/queueName1/someModule")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Got exception trying to delete all messages from the queue [queueName1] for the module [someModule]. " +
                "Error [Expected]",
            result.getResponse().getContentAsString());

        verify(bigQueueModuleService).deleteAllMessage("localhost", "queueName1");
        verifyNoMoreInteractions(bigQueueModuleService);
        verify(moduleMetaDataService).findAll();
        verifyNoMoreInteractions(moduleMetaDataService);
    }
}
