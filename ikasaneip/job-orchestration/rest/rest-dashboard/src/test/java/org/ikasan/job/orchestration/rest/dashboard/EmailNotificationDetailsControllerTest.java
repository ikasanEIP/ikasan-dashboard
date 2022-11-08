package org.ikasan.job.orchestration.rest.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsWrapperImpl;
import org.ikasan.job.orchestration.rest.dashboard.model.scheduled.EmailNotificationDetailsRecordRestImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsWrapper;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationTemplateParameters;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.*;

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
    EmailNotificationDetailsService emailNotificationDetailsService;

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

        EmailNotificationDetailsRecord record = emailNotificationDetailsService.findByJobNameAndMonitorType("job-from-template-1","context-from-template-1","ERROR");

        assertNotNull(record);
        assertNotNull(record.getEmailNotificationDetails());
        assertEquals("subject-from-template-1", record.getEmailNotificationDetails().getEmailSubject());
    }

    @Test
    public void save_all_success() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.notification.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.notification")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .build();
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        String uri = "/rest/emailNotificationDetails/saveAll";

        List<EmailNotificationDetails> emailNotificationDetails = new ArrayList<>();

        EmailNotificationDetails emailDetails = new EmailNotificationDetailsImpl();
        emailDetails.setContextName("parent-context-1");
        emailDetails.setChildContextName("context-1");
        emailDetails.setJobName("job-1");
        emailDetails.setEmailBody("body-1");
        emailDetails.setEmailSubject("subject-1");
        emailDetails.setMonitorType("ERROR");
        List<String> distributionList = new ArrayList<>();
        distributionList.add("email-1");
        distributionList.add("email-2");
        emailDetails.setEmailSendTo(distributionList);
        Map<String,String> params = new HashMap<>();
        params.put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK_1.name(), "link-1");
        params.put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK_2.name(), "link-2");
        emailDetails.setEmailNotificationTemplateParameters(params);

        emailNotificationDetails.add(emailDetails);

        EmailNotificationDetailsWrapper wrapper = new EmailNotificationDetailsWrapperImpl();
        wrapper.setEmailNotificationDetails(emailNotificationDetails);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(objectMapper.writeValueAsString(wrapper))).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        EmailNotificationDetailsRecord record = emailNotificationDetailsService.findByJobNameAndMonitorType("job-1","context-1","ERROR");

        assertNotNull(record);
        assertNotNull(record.getEmailNotificationDetails());
        assertEquals("subject-1", record.getEmailNotificationDetails().getEmailSubject());
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

    @Test
    public void getAllByContext() throws Exception {

        List<EmailNotificationDetailsRecord> emailNotificationDetailsRecord = new ArrayList<>();
        EmailNotificationDetailsRecord rr = new EmailNotificationDetailsRecordRestImpl();
        EmailNotificationDetails r = new EmailNotificationDetailsImpl();
        r.setContextName("ABC1");
        r.setChildContextName("ABC1");
        r.setJobName("ABC1");
        r.setMonitorType("ERROR");
        rr.setEmailNotificationDetails(r);
        rr.setContextName("ABC1");
        emailNotificationDetailsRecord.add(rr);

        EmailNotificationDetailsRecord rr2 = new EmailNotificationDetailsRecordRestImpl();
        EmailNotificationDetails r2 = new EmailNotificationDetailsImpl();
        r2.setContextName("ABC1");
        r2.setChildContextName("ABC11");
        r2.setJobName("ABC11");
        r2.setMonitorType("ERROR");
        rr2.setEmailNotificationDetails(r2);
        rr2.setContextName("ABC1");
        emailNotificationDetailsRecord.add(rr2);

        emailNotificationDetailsService.save(emailNotificationDetailsRecord);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/emailNotificationDetails/get/ABC1/0/0")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        /* returns the below
            {
              "totalNumberOfResults" : 2,
              "queryResponseTime" : 1,
              "resultList" : [ {
                "contextName" : "ABC1",
                "emailNotificationDetails" : {
                  "jobName" : "ABC1",
                  "contextName" : "ABC1",
                  "childContextName" : "ABC1",
                  "monitorType" : "ERROR",
                  "emailNotificationTemplateParameters" : { },
                  "html" : false
                },
                "timestamp" : 0,
                "modifiedTimestamp" : 0
              }, {
                "contextName" : "ABC1",
                "emailNotificationDetails" : {
                  "jobName" : "ABC11",
                  "contextName" : "ABC1",
                  "childContextName" : "ABC11",
                  "monitorType" : "ERROR",
                  "emailNotificationTemplateParameters" : { },
                  "html" : false
                },
                "timestamp" : 0,
                "modifiedTimestamp" : 0
              } ]
            }
         */

        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    public void getByJobNameAndMonitorType() throws Exception {

        List<EmailNotificationDetailsRecord> emailNotificationDetailsRecord = new ArrayList<>();
        EmailNotificationDetailsRecord rr = new EmailNotificationDetailsRecordRestImpl();
        EmailNotificationDetails r = new EmailNotificationDetailsImpl();
        r.setContextName("Test111");
        r.setChildContextName("TestChild111");
        r.setJobName("Job111");
        r.setMonitorType("ERROR");
        rr.setEmailNotificationDetails(r);
        rr.setContextName("Test111");
        emailNotificationDetailsRecord.add(rr);

        emailNotificationDetailsService.save(emailNotificationDetailsRecord);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/rest/emailNotificationDetails/getById/Job111/TestChild111/ERROR")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        /* returns the below
            {
              "contextName" : "Test111",
              "emailNotificationDetails" : {
                "jobName" : "Job111",
                "contextName" : "Test111",
                "childContextName" : "TestChild111",
                "monitorType" : "ERROR",
                "emailNotificationTemplateParameters" : { },
                "html" : false
              },
              "timestamp" : 0,
              "modifiedTimestamp" : 0
            }
         */

        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    public void deleteByContext() throws Exception {

        List<EmailNotificationDetailsRecord> emailNotificationDetailsRecord = new ArrayList<>();
        EmailNotificationDetailsRecord rr = new EmailNotificationDetailsRecordRestImpl();
        EmailNotificationDetails r = new EmailNotificationDetailsImpl();
        r.setContextName("ABC");
        r.setChildContextName("ABC1");
        r.setJobName("ABC1");
        r.setMonitorType("ERROR");
        rr.setEmailNotificationDetails(r);
        rr.setContextName("ABC");
        emailNotificationDetailsRecord.add(rr);

        EmailNotificationDetailsRecord rr2 = new EmailNotificationDetailsRecordRestImpl();
        EmailNotificationDetails r2 = new EmailNotificationDetailsImpl();
        r2.setContextName("DEF");
        r2.setChildContextName("DEF1");
        r2.setJobName("DEF1");
        r2.setMonitorType("ERROR");
        rr2.setEmailNotificationDetails(r2);
        rr2.setContextName("DEF");
        emailNotificationDetailsRecord.add(rr2);

        emailNotificationDetailsService.save(emailNotificationDetailsRecord);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/emailNotificationDetails/deleteByContextName/ABC")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        SearchResults<EmailNotificationDetailsRecord> abc =  emailNotificationDetailsService.findByContextName("ABC", 0, 0);
        assertEquals(abc.getTotalNumberOfResults(), 0);

        SearchResults<EmailNotificationDetailsRecord> def =  emailNotificationDetailsService.findByContextName("DEF", 0, 0);
        assertEquals(def.getTotalNumberOfResults(), 1);
    }

    @Test
    public void deleteByJobNameAndMonitorType() throws Exception {

        List<EmailNotificationDetailsRecord> emailNotificationDetailsRecord = new ArrayList<>();
        EmailNotificationDetailsRecord rr = new EmailNotificationDetailsRecordRestImpl();
        EmailNotificationDetails r = new EmailNotificationDetailsImpl();
        r.setContextName("ABC");
        r.setChildContextName("childC-1");
        r.setJobName("job1");
        r.setMonitorType("ERROR");
        rr.setEmailNotificationDetails(r);
        rr.setContextName("ABC");
        emailNotificationDetailsRecord.add(rr);

        emailNotificationDetailsService.save(emailNotificationDetailsRecord);

        EmailNotificationDetailsRecord job1 =  emailNotificationDetailsService.findByJobNameAndMonitorType("job1", "childC-1", "ERROR");
        assertNotNull(job1);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/rest/emailNotificationDetails/deleteById/job1/childC-1/ERROR")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(requestBuilder).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        job1 =  emailNotificationDetailsService.findByJobNameAndMonitorType("job1", "childC-1", "ERROR");
        assertNull(job1);
    }

}
