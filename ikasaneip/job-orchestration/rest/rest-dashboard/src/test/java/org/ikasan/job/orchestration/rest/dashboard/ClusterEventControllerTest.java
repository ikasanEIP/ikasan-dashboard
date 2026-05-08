package org.ikasan.job.orchestration.rest.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.event.JobLockCacheEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ClusterEventController.class)
@WebAppConfiguration
@EnableWebMvc
public class ClusterEventControllerTest {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = ObjectMapperFactory.newInstance();
    }

    @Test
    public void test_handle_context_instance_state_change_success() throws Exception {
        String json = objectMapper.writeValueAsString(new ContextInstanceStateChangeEventImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/context-instance-state-change")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_scheduler_job_state_change_success() throws Exception {
        String json = objectMapper.writeValueAsString(new SchedulerJobInstanceStateChangeEventImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/scheduler-job-state-change")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_context_instance_saved_success() throws Exception {
        String json = objectMapper.writeValueAsString(new ContextInstanceImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/context-instance-saved")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_context_instance_dlq_success() throws Exception {
        String json = objectMapper.writeValueAsString(new ContextInstanceImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/context-instance-dlq")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_context_template_saved_success() throws Exception {
        String json = objectMapper.writeValueAsString(new ContextTemplateImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/context-template-saved")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_context_template_enable_disable_success() throws Exception {
        String json = objectMapper.writeValueAsString(new ContextTemplateImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/context-template-enable-disable")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_context_view_update_success() throws Exception {
        String json = objectMapper.writeValueAsString("test-view-update-message");

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/context-view-update")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_new_scheduler_job_success() throws Exception {
        String json = objectMapper.writeValueAsString(new SchedulerJobImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/new-scheduler-job")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_handle_job_lock_cache_success() throws Exception {
        String json = objectMapper.writeValueAsString(new JobLockCacheEventImpl());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/job-lock-cache")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)).andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_bad_content_returns_bad_request() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders
            .post("/rest/clusterEvents/context-instance-state-change")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("bad-content")).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
    }
}
