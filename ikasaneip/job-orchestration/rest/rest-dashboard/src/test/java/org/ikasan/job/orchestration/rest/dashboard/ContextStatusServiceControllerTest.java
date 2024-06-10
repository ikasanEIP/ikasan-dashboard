package org.ikasan.job.orchestration.rest.dashboard;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.rest.dashboard.ContextStatusServiceController;
import org.ikasan.spec.scheduled.context.service.ContextStatusService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
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

import java.util.Collections;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ContextStatusServiceController.class)
@WebAppConfiguration
@EnableWebMvc
public class ContextStatusServiceControllerTest {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockBean
    private ContextStatusService contextStatusService;

    @Autowired
    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @After
    public void tidyUp() {
        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void should_return_response_entity_correctly_context_status() throws Exception {
        when(contextStatusService.getContextStatus("Instance_Name", "Context_Name")).thenReturn("RUNNING");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/Instance_Name/Context_Name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("RUNNING", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_context_status() throws Exception {
        when(contextStatusService.getContextStatus("Instance_Name", "Context_Name")).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/Instance_Name/Context_Name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance Instance_Name, context Context_Name, jobIdentifier null!"));
    }

    @Test
    public void should_return_response_entity_correctly_context_status_job() throws Exception {
        when(contextStatusService.getContextStatusForJob("instance-name", "context-name", "job-identifier")).thenReturn("COMPLETE");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/instance-name/context-name/job-identifier")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("COMPLETE", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_context_status_job() throws Exception {

        when(contextStatusService.getContextStatusForJob("instance-name", "context-name", "job-identifier")).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/instance-name/context-name/job-identifier")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance instance-name, context context-name, jobIdentifier job-identifier!"));
    }

    @Test
    public void should_return_response_entity_correctly_json_context_status() throws Exception {
        when(contextStatusService.getJsonContextStatus("Instance_Name", "Context_Name")).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/Instance_Name/Context_Name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_status() throws Exception {
        when(contextStatusService.getJsonContextStatus("Instance_Name", "Context_Name")).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/Instance_Name/Context_Name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance Instance_Name, context Context_Name, jobName null!"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_status() throws Exception {
        when(contextStatusService.getJsonContextStatus("Instance_Name", "Context_Name")).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/Instance_Name/Context_Name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void should_return_response_entity_correctly_json_context_status_job() throws Exception {
        when(contextStatusService.getJsonContextStatusForJob("instance-name", "context-name", "job-name")).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/instance-name/context-name/job-name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_status_job() throws Exception {

        when(contextStatusService.getJsonContextStatusForJob("instance-name", "context-name", "job-name")).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/instance-name/context-name/job-name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get status for instance instance-name, context context-name, jobName job-name!"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_status_job() throws Exception {

        when(contextStatusService.getJsonContextStatusForJob("instance-name", "context-name", "job-name")).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/instance-name/context-name/job-name")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void should_return_response_entity_correctly_json_context_machine_status() throws Exception {
        when(contextStatusService.getJsonContextMachineStatus(false)).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/allInstance")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_machine_status() throws Exception {
        when(contextStatusService.getJsonContextMachineStatus(false)).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/allInstance")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get all status found in the context machine, includePrepared = [false]"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_machine_status() throws Exception {
        when(contextStatusService.getJsonContextMachineStatus(false)).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/allInstance")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void should_return_response_entity_correctly_json_context_machine_status_prepared() throws Exception {
        when(contextStatusService.getJsonContextMachineStatus(true)).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/allInstance?includePrepared=true")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_machine_status_prepared() throws Exception {
        when(contextStatusService.getJsonContextMachineStatus(true)).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/allInstance?includePrepared=true")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get all status found in the context machine, includePrepared = [true]"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_machine_status_prepared() throws Exception {
        when(contextStatusService.getJsonContextMachineStatus(true)).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/allInstance?includePrepared=true")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void should_return_response_entity_correctly_json_context_job_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_job_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get job status from the context machine Error message [expected exception]"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_job_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void should_return_response_entity_correctly_json_context_job_status_search_by_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(InstanceStatus.ERROR, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus?instanceStatus=ERROR")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_job_status_search_by_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(InstanceStatus.ERROR, Collections.singletonMap("test-instance-id", contextMachine))).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus?instanceStatus=ERROR")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get job status from the context machine Error message [expected exception]"));
    }

    @Test
    public void should_return_response_entity_error_json_context_job_status_search_by_status_2() throws Exception {

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus?instanceStatus=DOESNOTEXIST")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("Instance Status [DOESNOTEXIST] is not valid, please try again"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_job_status_search_by_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus?instanceStatus=ERROR")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }


    //TODO
    @Test
    public void should_return_response_entity_correctly_json_context_name_job_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus/JOB_PLAN")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_name_job_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus/JOB_PLAN")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get job status from the context machine Error message [expected exception]"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_name_job_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus/JOB_PLAN")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void should_return_response_entity_correctly_json_context_name_job_status_search_by_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(InstanceStatus.ERROR, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("{\"message\":\"good\"}");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus/JOB_PLAN?instanceStatus=ERROR")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        assertEquals("{\"message\":\"good\"}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_return_response_entity_error_json_context_name_job_status_search_by_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(InstanceStatus.ERROR, Collections.singletonMap("test-instance-id", contextMachine))).thenThrow(new RuntimeException("expected exception"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus/JOB_PLAN?instanceStatus=ERROR")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("An error has occurred attempting to get job status from the context machine Error message [expected exception]"));
    }

    @Test
    public void should_return_response_entity_error_json_context_name_job_status_search_by_status_2() throws Exception {

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus/JOB_PLAN?instanceStatus=DOESNOTEXIST")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,
            containsString("Instance Status [DOESNOTEXIST] is not valid, please try again"));
    }

    @Test
    public void should_return_response_entity_empty_json_context_name_job_status_search_by_status() throws Exception {

        ContextInstance instance = new ContextInstanceImpl();
        instance.setId("test-instance-id");
        instance.setName("JOB_PLAN");
        ContextMachine contextMachine = new ContextMachine(null, instance, null, null, null
            , null, null, null, null, JobLockCacheImpl.instance(), null
            , null, null, null, null, null);
        ContextMachineCache.instance().put(contextMachine);

        when(contextStatusService.getJsonContextJobStatus(null, Collections.singletonMap("test-instance-id", contextMachine))).thenReturn("");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/contextStatus/json/jobStatus/JOB_PLAN?instanceStatus=ERROR")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus());
    }



}