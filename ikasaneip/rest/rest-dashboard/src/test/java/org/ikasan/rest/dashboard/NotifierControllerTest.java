package org.ikasan.rest.dashboard;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.rest.dashboard.model.flow.FlowStateImpl;
import org.ikasan.rest.dashboard.util.TestCacheAdapter;
import org.ikasan.spec.flow.FlowState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.Resource;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = NotifierController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml"
    }
)
public class NotifierControllerTest extends  AbstractRestMvcTest
{
    protected MockMvc mvc;
    @Autowired
    WebApplicationContext webApplicationContext;

    @Resource
    TestCacheAdapter cacheAdapter;

    private ObjectMapper mapper;

    @BeforeEach
    @Before
    public void setUp()
    {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    @Test
    public void update_cache_success() throws Exception
    {
        String uri = "/rest/flowStates/cache";

        FlowStateImpl flowState = new FlowStateImpl();
        flowState.setModuleName("moduleName");
        flowState.setFlowName("flowName");
        flowState.setState("state");

        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(this.mapper.writeValueAsString(flowState))).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();

        Assert.assertEquals("State equals", "state", cacheAdapter.get(flowState.getModuleName()+flowState.getFlowName()));
    }

    @Test
    public void test_exception_bad_post_json() throws Exception
    {
        String uri = "/rest/flowStates/cache";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("bad json")).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,containsString( "An error has occurred attempting to update dashboard flow state cache!"));

    }

    @Test
    public void update_all_cache_success() throws Exception
    {
        String uri = "/rest/flowStatesAll/cache";

        ArrayList<FlowState> flowStates = new ArrayList<>();

        FlowStateImpl flowState = new FlowStateImpl();
        flowState.setModuleName("moduleName");
        flowState.setFlowName("flowName1");
        flowState.setState("running");

        flowStates.add(flowState);

        flowState = new FlowStateImpl();
        flowState.setModuleName("moduleName");
        flowState.setFlowName("flowName2");
        flowState.setState("stopped");

        flowStates.add(flowState);

        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(this.mapper.writeValueAsString(flowStates))).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        Assert.assertEquals("State equals", "running", cacheAdapter.get(flowState.getModuleName()+"flowName1"));
        Assert.assertEquals("State equals", "stopped", cacheAdapter.get(flowState.getModuleName()+"flowName2"));
    }

    @Test
    public void test_exception_all_bad_post_json() throws Exception
    {
        String uri = "/rest/flowStatesAll/cache";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("bad json")).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,containsString( "An error has occurred attempting to update dashboard flow state cache!"));

    }
}
