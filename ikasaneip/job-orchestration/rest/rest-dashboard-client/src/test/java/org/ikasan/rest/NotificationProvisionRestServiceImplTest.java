package org.ikasan.rest;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsWrapperImpl;
import org.ikasan.job.orchestration.rest.client.NotificationProvisionRestServiceImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsWrapper;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationTemplateParameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NotificationProvisionRestServiceImplTest extends AbstractTest {

    @Mock
    Environment environment;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    String dashboardBaseUrl = "http://localhost:";

    @Before
    public void setup() {
        dashboardBaseUrl = dashboardBaseUrl + wireMockRule.port();
    }

    @Test
    public void test_notification_provision() throws IOException {

        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("testModule");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(dashboardBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        NotificationProvisionRestServiceImpl notificationProvisionService = new NotificationProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/emailNotificationDetails/save");

        stubFor(put(urlEqualTo("/rest/emailNotificationDetails/save"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("testModule"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(201)
            ));


        List<EmailNotificationDetails> emailNotificationDetails = new ArrayList<>();

        EmailNotificationDetails details = new EmailNotificationDetailsImpl();
        details.setContextName("context-1");
        details.setJobName("job-1");
        details.setEmailBody("body-1");
        details.setEmailSubject("subject-1");
        details.setEmailSendTo(Arrays.asList("email-1","email-2"));
        Map<String,String> params = new HashMap<>();
        params.put(EmailNotificationTemplateParameters.EMAIL_BODY_LINK_1.name(), "link-1");
        details.setEmailNotificationTemplateParameters(params);

        emailNotificationDetails.add(details);

        EmailNotificationDetailsWrapper wrapper = new EmailNotificationDetailsWrapperImpl();
        wrapper.setEmailNotificationDetails(emailNotificationDetails);

        notificationProvisionService.provisionNotification(wrapper);
    }


}
