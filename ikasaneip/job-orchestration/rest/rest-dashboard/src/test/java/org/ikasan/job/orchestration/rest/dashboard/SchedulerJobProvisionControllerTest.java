package org.ikasan.job.orchestration.rest.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
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


    private ContextBundle loadContextBundle() throws IOException {
        InputStream inputStream = new ClassPathResource("data/CONTEXT-1793100514.zip").getInputStream();
        return ContextImportZipUtils.extractZipFile(inputStream);
    }
}