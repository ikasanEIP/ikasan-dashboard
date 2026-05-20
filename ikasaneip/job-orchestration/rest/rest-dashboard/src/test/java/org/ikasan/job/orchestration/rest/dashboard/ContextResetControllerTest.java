package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.spec.scheduled.reset.ContextResetService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ContextResetController.class)
@WebAppConfiguration
@EnableWebMvc
public class ContextResetControllerTest {
    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockitoBean
    private ContextResetService contextResetService;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_reset_context_no_error() throws Exception {

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/context/reset/ContextName")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        verify(contextResetService).resetContext("ContextName", false);
    }

    @Test
    public void should_return_response_entity_error_context_status() throws Exception {
        doThrow(new RuntimeException("expected exception")).when(contextResetService).resetContext("ContextName", false);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put("/rest/context/reset/ContextName")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("An error has occurred attempting to reset context for context name ContextName! Error message [expected exception]"));

        verify(contextResetService).resetContext("ContextName", false);
    }
}