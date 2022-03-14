package org.ikasan.job.orchestration.rest.dashboard.status;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.ikasan.spec.scheduled.context.service.ContextStatusService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ContextStatusServiceController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml"
    }
)
public class ContextStatusServiceControllerTest {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockBean
    private ContextStatusService contextStatusService;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_return_response_entity_correctly_context_status() throws Exception {
        when(contextStatusService.getContextStatus("Instance_Name", "Context_Name")).thenReturn("RUNNING");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status/Instance_Name/Context_Name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("RUNNING", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_context_status() throws Exception {
        when(contextStatusService.getContextStatus("Instance_Name", "Context_Name")).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status/Instance_Name/Context_Name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance Instance_Name, context Context_Name, jobIdentifier null!"));
    }

    @Test
    public void should_return_response_entity_correctly_context_status_job() throws Exception {
        when(contextStatusService.getContextStatusForJob("instance-name", "context-name", "job-identifier")).thenReturn("COMPLETE");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status/instance-name/context-name/job-identifier")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("COMPLETE", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_context_status_job() throws Exception {

        when(contextStatusService.getContextStatusForJob("instance-name", "context-name", "job-identifier")).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status/instance-name/context-name/job-identifier")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance instance-name, context context-name, jobIdentifier job-identifier!"));
    }
}