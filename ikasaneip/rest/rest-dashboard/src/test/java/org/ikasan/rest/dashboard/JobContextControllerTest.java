package org.ikasan.rest.dashboard;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = JobContextController.class)
@WebAppConfiguration
@EnableWebMvc
public class JobContextControllerTest extends  AbstractRestMvcTest {

    protected MockMvc mvc;
    @Autowired
    WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @Autowired
    @Before
    public void setUp()
    {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ContextParameterInstance contextParameterInstance1 = new ContextParameterInstanceImpl();
        contextParameterInstance1.setName("businessDate");
        contextParameterInstance1.setType("date");
        contextParameterInstance1.setValue("19/04/2022");

        ContextParameterInstance contextParameterInstance2 = new ContextParameterInstanceImpl();
        contextParameterInstance2.setName("localFilePath");
        contextParameterInstance2.setType("path");
        contextParameterInstance2.setValue("/opt/data/files");

        ContextInstance contextInstance1 = new ContextInstanceImpl();
        contextInstance1.setContextParameters(Arrays.asList(contextParameterInstance1, contextParameterInstance2));
        contextInstance1.setName("context-instance-1");

        ContextInstance contextInstance2 = new ContextInstanceImpl();
        contextInstance2.setContextParameters(Arrays.asList(contextParameterInstance1));
        contextInstance2.setName("context-instance-2");

        ContextTemplate contextTemplate1 = new ContextTemplateImpl();
        contextTemplate1.setName("context-template-1");

        ContextTemplate contextTemplate2 = new ContextTemplateImpl();
        contextTemplate2.setName("context-template-2");

        ContextMachine contextMachine1 = new ContextMachine(contextTemplate1, contextInstance1, null, null,null,null,null);
        ContextMachine contextMachine2 = new ContextMachine(contextTemplate2, contextInstance2, null, null,null,null,null);

        ContextMachineCache.instance().put(contextMachine1);
        ContextMachineCache.instance().put(contextMachine2);
    }


    @Test
    public void test_get_all() throws Exception {

        String uri = "/rest/jobContext/getAll";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(objectMapper.readValue(loadDataFile("/data/job-context-parameters-all.json")
                , Map.class))
                , mvcResult.getResponse().getContentAsString(), false);

    }

    @Test
    public void test_get_by_context_name() throws Exception {

        String uri = "/rest/jobContext/getByContextName?contextName=context-instance-1";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(objectMapper.readValue(loadDataFile("/data/job-context-parameters-1.json")
                , Map.class))
            , mvcResult.getResponse().getContentAsString(), false);

    }

}
