package org.ikasan.job.orchestration.rest.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SchedulerJobProvisionController.class)
@WebAppConfiguration
@EnableWebMvc
public class SchedulerJobProvisionControllerTest {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockitoBean
    private JobProvisionService jobProvisionService;

    @MockitoBean
    private SchedulerJobService schedulerJobService;

    @MockitoBean
    private SchedulerJobRecord<FileEventDrivenJob> schedulerJobRecord;

    @Autowired
    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void test_provision_context_success() throws Exception {
        ContextBundle contextBundle = loadContextBundle();

        ObjectMapper mapper = ConcurrentObjectMapperFactory.newInstance();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("org.ikasan.job.orchestration.model.profile")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArrayList")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        SchedulerJobWrapperImpl schedulerJobWrapper = new SchedulerJobWrapperImpl();
        schedulerJobWrapper.setJobs(contextBundle.getSchedulerJobs());

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/provision/jobs")
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(mapper.writeValueAsBytes(schedulerJobWrapper))).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void test_exception_provision_context_bad_content() throws Exception {

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/provision/jobs")
            .contentType(MediaType.APPLICATION_JSON_VALUE).content("bad-content")).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void test_get_job_success() throws Exception {
        Mockito.when(this.schedulerJobService.findByContextNameAndJobName(anyString(), anyString()))
            .thenReturn(this.schedulerJobRecord);
        Mockito.when(this.schedulerJobRecord.getJob()).thenReturn(new FileEventDrivenJobImpl());
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
            .get("/rest/job/contextName/jobName").contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());

        Mockito.verify(this.schedulerJobService).findByContextNameAndJobName(anyString(), anyString());
        Mockito.verify(this.schedulerJobRecord, Mockito.times(2)).getJob();

        Mockito.verifyNoMoreInteractions(this.schedulerJobService
            , this.schedulerJobRecord);
    }

    @Test
    public void test_get_job_not_found_null_job_exception() throws Exception {
        Mockito.when(this.schedulerJobService.findByContextNameAndJobName(anyString(), anyString()))
            .thenReturn(this.schedulerJobRecord);
        Mockito.when(this.schedulerJobRecord.getJob()).thenReturn(null);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .get("/rest/job/contextName/jobName").contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), mvcResult.getResponse().getStatus());

        Mockito.verify(this.schedulerJobService).findByContextNameAndJobName(anyString(), anyString());
        Mockito.verify(this.schedulerJobRecord).getJob();

        Mockito.verifyNoMoreInteractions(this.schedulerJobService
            , this.schedulerJobRecord);
    }

    @Test
    public void test_get_job_not_found_null_job_record_exception() throws Exception {
        Mockito.when(this.schedulerJobService.findByContextNameAndJobName(anyString(), anyString()))
            .thenReturn(null);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders
                .get("/rest/job/contextName/jobName").contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), mvcResult.getResponse().getStatus());

        Mockito.verify(this.schedulerJobService).findByContextNameAndJobName(anyString(), anyString());

        Mockito.verifyNoMoreInteractions(this.schedulerJobService
            , this.schedulerJobRecord);
    }


    private ContextBundle loadContextBundle() throws IOException {
        InputStream inputStream = new ClassPathResource("data/CONTEXT-1793100514.zip").getInputStream();
        return ContextImportZipUtils.extractZipFile(inputStream);
    }
}