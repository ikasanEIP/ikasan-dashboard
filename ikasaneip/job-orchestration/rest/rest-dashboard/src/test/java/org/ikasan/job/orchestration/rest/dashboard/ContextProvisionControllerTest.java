package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.junit.Before;
import org.junit.Test;
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
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static tools.jackson.databind.DefaultTyping.NON_FINAL;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ContextProvisionController.class)
@WebAppConfiguration
@EnableWebMvc
public class ContextProvisionControllerTest {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockitoBean
    private ContextProvisionService contextProvisionService;
    
    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void test_provision_context_success() throws Exception {
        ContextBundle contextBundle = loadContextBundle();

        JsonMapper mapper = ConcurrentObjectMapperFactory.newInstance();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("org.ikasan.job.orchestration.model.profile")
            .allowIfSubType("org.ikasan.job.orchestration.model.notification")
            .allowIfSubType("org.ikasan.spec.scheduled.notification.model")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArrayList")
            .allowIfSubType("java.util.concurrent.ConcurrentHashMap")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .allowIfSubType("java.util.HashSet")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArraySet")
            .build();
        mapper = mapper.rebuild().activateDefaultTyping(ptv, NON_FINAL)
            .disable(MapperFeature.USE_ANNOTATIONS).build();

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/provision/context")
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(mapper.writeValueAsBytes(contextBundle))).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    /**
     * Test for all files in the directories
     * root
     *  - context
     *  - job
     *    - file
     *    - internal
     *    - quartz
     *  - notification
     *  - notification_details
     */
    @Test
    public void test_provision_context_success_all() throws Exception {
        ContextBundle contextBundle = loadContextBundleAll();

        JsonMapper mapper = ConcurrentObjectMapperFactory.newInstance();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("org.ikasan.job.orchestration.model.profile")
            .allowIfSubType("org.ikasan.job.orchestration.model.notification")
            .allowIfSubType("org.ikasan.spec.scheduled.notification.model")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArrayList")
            .allowIfSubType("java.util.concurrent.ConcurrentHashMap")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .allowIfSubType("java.util.HashSet")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArraySet")
            .build();
        mapper = mapper.rebuild().activateDefaultTyping(ptv, NON_FINAL)
            .disable(MapperFeature.USE_ANNOTATIONS).build();

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/provision/context")
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(mapper.writeValueAsBytes(contextBundle))).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void test_provision_context_success_dynamic_file_watcher() throws Exception {
        ContextBundle contextBundle = loadContextBundleDynamic();

        JsonMapper mapper = ConcurrentObjectMapperFactory.newInstance();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.spec.scheduled.job.context")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("org.ikasan.job.orchestration.model.profile")
            .allowIfSubType("org.ikasan.job.orchestration.model.notification")
            .allowIfSubType("org.ikasan.spec.scheduled.notification.model")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArrayList")
            .allowIfSubType("java.util.concurrent.ConcurrentHashMap")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .allowIfSubType("java.util.HashSet")
            .allowIfSubType("java.util")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArraySet")
            .allowIfSubType("java.util.concurrent")
            .build();
        mapper = mapper.rebuild().activateDefaultTyping(ptv, NON_FINAL)
            .disable(MapperFeature.USE_ANNOTATIONS).build();

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/provision/context")
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(mapper.writeValueAsBytes(contextBundle))).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void test_exception_provision_context_bad_content() throws Exception {

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/provision/context")
            .contentType(MediaType.APPLICATION_JSON_VALUE).content("bad-content")).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
    }


    private ContextBundle loadContextBundle() throws IOException {
        InputStream inputStream = new ClassPathResource("data/CONTEXT-1793100514.zip").getInputStream();
        return ContextImportZipUtils.extractZipFile(inputStream);
    }

    // Test all context bundles
    private ContextBundle loadContextBundleAll() throws IOException {
        InputStream inputStream = new ClassPathResource("data/CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS-2.zip").getInputStream();
        return ContextImportZipUtils.extractZipFile(inputStream);
    }

    private ContextBundle loadContextBundleDynamic() throws IOException {
        InputStream inputStream = new ClassPathResource("data/JOB_PLAN_WITH_DYNAMIC_FILE_WATCHERS.zip").getInputStream();
        return ContextImportZipUtils.extractZipFile(inputStream);
    }
}