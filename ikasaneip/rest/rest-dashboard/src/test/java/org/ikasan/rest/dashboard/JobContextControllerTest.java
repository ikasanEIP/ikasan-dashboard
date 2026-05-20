package org.ikasan.rest.dashboard;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachineImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.rest.dashboard.util.TestContextParametersInstanceService;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = JobContextController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml"
    }
)
public class JobContextControllerTest extends AbstractRestMvcTest {

    protected MockMvc mvc;
    @Autowired
    WebApplicationContext webApplicationContext;
    @Autowired
    TestContextParametersInstanceService contextParametersInstanceService;
    @MockitoBean
    private ScheduledContextService scheduledContextService;
    @MockitoBean
    private ModuleMetaDataService moduleMetadataService;
    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;
    @Mock
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;
    @Mock
    private ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;
    private ObjectMapper objectMapper;

    private static final String AGENT = "scheduler-agent";
    private static final Map<String, ModuleMetaData> AGENTS_MAP = Map.of(AGENT, new ModuleMetaDataImpl());

    @Before
    public void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ContextParameterInstance contextParameterInstance1 = new ContextParameterInstanceImpl();
        contextParameterInstance1.setName("BusinessDate");
        contextParameterInstance1.setDefaultValue("value");
        contextParameterInstance1.setValue("20220530");

        ContextParameterInstance contextParameterInstance2 = new ContextParameterInstanceImpl();
        contextParameterInstance2.setName("localFilePath");
        contextParameterInstance2.setDefaultValue("value");
        contextParameterInstance2.setValue("/opt/data/files");

        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();
        contextParameterInstances.add(contextParameterInstance1);
        contextParameterInstances.add(contextParameterInstance2);

        ContextInstance contextInstance1 = new ContextInstanceImpl();
        contextInstance1.setContextParameters(contextParameterInstances);
        contextInstance1.setName("context-instance-1");
        contextInstance1.setId("UUID1");
        contextInstance1.setCreatedDateTime(11);
        contextInstance1.setUpdatedDateTime(111);

        contextParametersInstanceService.addParamsToContext("context-instance-1", contextParameterInstances);

        contextParameterInstances = new ArrayList<>();
        contextParameterInstances.add(contextParameterInstance1);

        ContextInstance contextInstance2 = new ContextInstanceImpl();
        contextInstance2.setContextParameters(contextParameterInstances);
        contextInstance2.setName("context-instance-2");
        contextInstance2.setId("UUID2");
        contextInstance2.setCreatedDateTime(22);
        contextInstance2.setUpdatedDateTime(222);
        contextParametersInstanceService.addParamsToContext("context-instance-2", contextParameterInstances);

        ContextTemplate contextTemplate1 = new ContextTemplateImpl();
        contextTemplate1.setName("context-template-1");

        ContextTemplate contextTemplate2 = new ContextTemplateImpl();
        contextTemplate2.setName("context-template-2");


        ContextMachineImpl contextMachine1 = new ContextMachineImpl(contextTemplate1, contextInstance1, null, null, null, null, null, null
            ,null, null, null, AGENTS_MAP, moduleMetadataService, null, null, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, this.contextInstancePublicationService, null);
        ContextMachineImpl contextMachine2 = new ContextMachineImpl(contextTemplate2, contextInstance2, null, null, null, null, null, null
            ,null, null, null, AGENTS_MAP, moduleMetadataService, null, null, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, this.contextInstancePublicationService, null);

        ContextMachineCache.instance().put(contextMachine1);
        ContextMachineCache.instance().put(contextMachine2);
    }

    @Test
    public void test_get_by_agentName() throws Exception {

        String uri = "/rest/jobContext/getByAgentName?agentName=scheduler-agent";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        String expected = objectMapper.writeValueAsString(objectMapper.readValue(loadDataFile("/data/job-context-instances-by-agentName.json"), Map.class));
        String actual = mvcResult.getResponse().getContentAsString();

        JSONAssert.assertEquals(expected, actual, false);
    }
}