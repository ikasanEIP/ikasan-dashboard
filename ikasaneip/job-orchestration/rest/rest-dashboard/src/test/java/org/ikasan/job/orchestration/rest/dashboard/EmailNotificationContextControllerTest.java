package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextRecordImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EmailNotificationContextController.class)
@WebAppConfiguration
@EnableWebMvc
public class EmailNotificationContextControllerTest extends AbstractRestMvcTest {

    public static final String JSON = "/data/email-notification-context.json";

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockBean
    EmailNotificationContextService emailNotificationContextService;

    @Autowired
    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void save_success() throws Exception {
        String uri = "/rest/emailNotificationContext/save";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(super.loadDataFile(JSON))).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        verify(emailNotificationContextService).save(isA(EmailNotificationContextRecord.class));
        verifyNoMoreInteractions(emailNotificationContextService);
    }

    @Test
    public void test_exception_bad_post_json() throws Exception {
        String uri = "/rest/emailNotificationContext/save";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content("bad json")).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,containsString( "An error has occurred attempting to perform a save of EmailNotificationContext!"));
        verifyNoMoreInteractions(emailNotificationContextService);
    }

    @Test
    public void test_get_context() throws Exception {
        String uri = "/rest/emailNotificationContext/get/job-plan-1/10/0";

        // Quick return for the results from REST
        SearchResults<EmailNotificationContextRecord> notification = new SearchResults<EmailNotificationContextRecord>() {
            @Override
            public List<EmailNotificationContextRecord> getResultList() {
                EmailNotificationContext notif = new EmailNotificationContextImpl();
                notif.setContextName("job-plan-1");

                EmailNotificationContextRecord rec = new EmailNotificationContextRecordImpl();
                rec.setContextName("job-plan-1");
                rec.setEmailNotificationContext(notif);
                return List.of(rec);
            }

            @Override
            public long getTotalNumberOfResults() {
                return 1;
            }

            @Override
            public long getQueryResponseTime() {
                return 1;
            }
        };

        when(emailNotificationContextService.findByContextName("job-plan-1", 10, 0)).thenReturn(notification);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        System.out.println(content);

        verify(emailNotificationContextService).findByContextName("job-plan-1", 10, 0);
        verifyNoMoreInteractions(emailNotificationContextService);
    }

    @Test
    public void test_get_no_context() throws Exception {
        String uri = "/rest/emailNotificationContext/get/job-plan-1/10/0";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content("")).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.NO_CONTENT.value(), status);

        verify(emailNotificationContextService).findByContextName("job-plan-1", 10, 0);
        verifyNoMoreInteractions(emailNotificationContextService);
    }

    @Test
    public void test_get_exception_context() throws Exception {
        String uri = "/rest/emailNotificationContext/get/job-plan-1/10/0";

        when(emailNotificationContextService.findByContextName("job-plan-1", 10, 0)).thenThrow(new RuntimeException("Expected"));

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,containsString( "Error converting to JSON"));

        verify(emailNotificationContextService).findByContextName("job-plan-1", 10, 0);
        verifyNoMoreInteractions(emailNotificationContextService);
    }

    @Test
    public void test_delete_context() throws Exception {
        String uri = "/rest/emailNotificationContext/delete/job-plan-1";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.delete(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        verify(emailNotificationContextService).deleteByContextName("job-plan-1");
        verifyNoMoreInteractions(emailNotificationContextService);
    }

    @Test
    public void test_delete_context_Exception() throws Exception {
        String uri = "/rest/emailNotificationContext/delete/job-plan-1";

        doThrow(new RuntimeException()).when(emailNotificationContextService).deleteByContextName("job-plan-1");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.delete(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,containsString( "Got exception trying to delete notification for the context [job-plan-1]"));

        verify(emailNotificationContextService).deleteByContextName("job-plan-1");
        verifyNoMoreInteractions(emailNotificationContextService);
    }

}
