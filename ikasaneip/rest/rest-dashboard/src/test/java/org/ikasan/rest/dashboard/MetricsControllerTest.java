package org.ikasan.rest.dashboard;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.rest.dashboard.model.metrics.FlowInvocationMetricImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
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

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MetricsController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml"
    }
)
public class MetricsControllerTest extends  AbstractRestMvcTest
{
    public static final String METRICS_JSON = "/data/metrics.json";

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
    }


    @Test
    public void harvest_metrics_success() throws Exception
    {
        String uri = "/rest/harvest/metrics";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(super.loadDataFile(METRICS_JSON))).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

    }

    @Test
    public void test_exception_bad_post_json() throws Exception
    {
        String uri = "/rest/harvest/metrics";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content("bad json")).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,containsString( "Cannot parse metrics JSON!"));
    }

    @Test
    public void get_metrics_within_timeframe_success() throws Exception
    {
        String uri = "/rest/metrics/0/100000000";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(objectMapper.readValue(loadDataFile(METRICS_JSON)
            , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)))
            , mvcResult.getResponse().getContentAsString(), false);
    }

    @Test
    public void get_metrics_for_module_within_timeframe_success() throws Exception
    {
        String uri = "/rest/metrics/my-module/0/100000000";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(objectMapper.readValue(loadDataFile(METRICS_JSON)
            , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)))
            , mvcResult.getResponse().getContentAsString(), false);
    }

    @Test
    public void get_metrics_for_module_and_flow_within_timeframe_success() throws Exception
    {
        String uri = "/rest/metrics/my-module/my-flow/0/100000000";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(objectMapper.readValue(loadDataFile(METRICS_JSON)
            , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)))
            , mvcResult.getResponse().getContentAsString(), false);
    }
}
