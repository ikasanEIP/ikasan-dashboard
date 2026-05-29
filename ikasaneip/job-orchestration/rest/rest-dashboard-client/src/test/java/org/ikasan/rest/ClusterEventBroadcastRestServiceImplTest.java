package org.ikasan.rest;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.event.JobLockCacheEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.rest.client.ClusterEventBroadcastRestServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterEventBroadcastRestServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private ClusterEventBroadcastRestServiceImpl uut;

    @Mock
    private Environment environment;

    @Before
    public void setup() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        uut = new ClusterEventBroadcastRestServiceImpl(baseUrl, environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_broadcast_context_instance_state_change() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_INSTANCE_STATE_CHANGE_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastContextInstanceStateChange(new ContextInstanceStateChangeEventImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_INSTANCE_STATE_CHANGE_PATH)));
    }

    @Test
    public void test_broadcast_scheduler_job_state_change() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.SCHEDULER_JOB_STATE_CHANGE_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastSchedulerJobStateChange(new SchedulerJobInstanceStateChangeEventImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.SCHEDULER_JOB_STATE_CHANGE_PATH)));
    }

    @Test
    public void test_broadcast_context_instance_saved() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_INSTANCE_SAVED_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastContextInstanceSaved(new ContextInstanceImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_INSTANCE_SAVED_PATH)));
    }

    @Test
    public void test_broadcast_context_instance_dlq() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_INSTANCE_DLQ_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastContextInstanceDlq(new ContextInstanceImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_INSTANCE_DLQ_PATH)));
    }

    @Test
    public void test_broadcast_context_template_saved() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_TEMPLATE_SAVED_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastContextTemplateSaved(new ContextTemplateImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_TEMPLATE_SAVED_PATH)));
    }

    @Test
    public void test_broadcast_context_template_enable_disable() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_TEMPLATE_ENABLE_DISABLE_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastContextTemplateEnableDisable(new ContextTemplateImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_TEMPLATE_ENABLE_DISABLE_PATH)));
    }

    @Test
    public void test_broadcast_context_view_update() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_VIEW_UPDATE_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastContextViewUpdate("test-view-update-message");

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.CONTEXT_VIEW_UPDATE_PATH)));
    }

    @Test
    public void test_broadcast_new_scheduler_job() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.NEW_SCHEDULER_JOB_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastNewSchedulerJob(new SchedulerJobImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.NEW_SCHEDULER_JOB_PATH)));
    }

    @Test
    public void test_broadcast_job_lock_cache() {
        stubFor(post(urlEqualTo(ClusterEventBroadcastRestServiceImpl.JOB_LOCK_CACHE_PATH))
            .willReturn(aResponse().withStatus(200)));

        uut.broadcastJobLockCache(new JobLockCacheEventImpl());

        verify(postRequestedFor(urlEqualTo(ClusterEventBroadcastRestServiceImpl.JOB_LOCK_CACHE_PATH)));
    }
}
