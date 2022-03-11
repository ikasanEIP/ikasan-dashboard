package org.ikasan.job.orchestration.rest.dashboard.status;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.ikasan.job.orchestration.rest.dashboard.status.model.ContextStatusJobDto;
import org.ikasan.job.orchestration.rest.dashboard.status.model.ContextStatusNameDto;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_return_response_entity_correctly_context_status() throws Exception {
        ContextStatusNameDto dto = new ContextStatusNameDto();
        dto.setInstanceName("Instance_Name");
        dto.setContextName("Context_Name");

        when(contextStatusService.getContextStatus("Instance_Name", "Context_Name")).thenReturn("RUNNING");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(dto))).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("RUNNING", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_context_status() throws Exception {
        ContextStatusNameDto dto = new ContextStatusNameDto();
        dto.setInstanceName("Instance_Name");
        dto.setContextName("Context_Name");

        when(contextStatusService.getContextStatus("Instance_Name", "Context_Name")).thenThrow(new IllegalArgumentException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(dto))).andReturn();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance Instance_Name and context Context_Name!"));
    }

    @Test
    public void should_return_response_entity_correctly_context_status_job() throws Exception {
        ContextStatusJobDto dto = new ContextStatusJobDto();
        dto.setInstanceName("instance-name");
        dto.setContextName("context-name");
        dto.setAgentName("agent-name");
        dto.setJobIdentifier("job-identifier");

        when(contextStatusService.getContextStatusForJob("instance-name", "context-name", "agent-name", "job-identifier"))
            .thenReturn("COMPLETE");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status/job")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(dto))).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("COMPLETE", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_context_status_job() throws Exception {
        ContextStatusJobDto dto = new ContextStatusJobDto();
        dto.setInstanceName("instance-name");
        dto.setContextName("context-name");
        dto.setAgentName("agent-name");
        dto.setJobIdentifier("job-identifier");

        when(contextStatusService.getContextStatusForJob("instance-name", "context-name", "agent-name", "job-identifier"))
            .thenThrow(new IllegalArgumentException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/context/status/job")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(dto))).andReturn();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance instance-name, context context-name, agent agent-name, job-id job-identifier!"));
    }
}