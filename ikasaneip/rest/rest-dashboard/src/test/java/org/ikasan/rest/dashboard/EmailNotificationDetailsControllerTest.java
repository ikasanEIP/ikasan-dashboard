package org.ikasan.rest.dashboard;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EmailNotificationDetailsController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml"
    }
)
public class EmailNotificationDetailsControllerTest extends AbstractRestMvcTest
{
    public static final String JSON = "/data/email-notification-details.json";

    protected MockMvc mvc;
    @Autowired
    WebApplicationContext webApplicationContext;
    @Autowired
    EmailNotificationDetailsService<EmailNotificationDetails> emailNotificationDetailsService;

    @BeforeEach
    @Before
    public void setUp()
    {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    @Test
    public void save_success() throws Exception
    {
        String uri = "/rest/emailNotificationDetails/save";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(super.loadDataFile(JSON))).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        EmailNotificationDetails details = emailNotificationDetailsService.findByJobNameAndMonitorType("job-from-template-1","context-from-template-1","ERROR");

        assertNotNull(details);
        assertEquals("subject-from-template-1", details.getEmailSubject());
    }

    @Test
    public void test_exception_bad_post_json() throws Exception
    {
        String uri = "/rest/emailNotificationDetails/save";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content("bad json")).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content,containsString( "An error has occurred attempting to perform a save of EmailNotificationDetails!"));
    }

}
