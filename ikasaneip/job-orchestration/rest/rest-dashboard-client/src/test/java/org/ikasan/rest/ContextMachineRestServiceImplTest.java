package org.ikasan.rest;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.status.ContextInstanceStatus;
import org.ikasan.job.orchestration.rest.client.ContextMachineRestServiceImpl;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
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

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContextMachineRestServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private ContextMachineRestServiceImpl uut;

    @Mock
    private Environment environment;

    private static final String CONTEXT_INSTANCE_ID = "test-context-instance-id";
    private static final String JOB_IDENTIFIER      = "my-job";
    private static final String CHILD_CONTEXT_NAME  = "my-child-context";
    private static final String CONTEXT_NAME        = "my-context";
    private static final String AGENT_NAME          = "my-agent";
    private static final String JOB_NAME            = "my-job-name";
    private static final String MESSAGE_ID          = "msg-001";

    @Before
    public void setup() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        uut = new ContextMachineRestServiceImpl(baseUrl, environment, new HttpComponentsClientHttpRequestFactory());
    }

    // ── Path helpers ─────────────────────────────────────────────────────────────────────────────

    private String base() {
        return ContextMachineRestServiceImpl.CONTEXT_MACHINE_BASE_URL + "/" + CONTEXT_INSTANCE_ID;
    }

    private String holdPath()              { return base() + ContextMachineRestServiceImpl.HOLD_PATH; }
    private String releasePath()           { return base() + ContextMachineRestServiceImpl.RELEASE_PATH; }
    private String resetPath()             { return base() + ContextMachineRestServiceImpl.RESET_PATH; }
    private String skipPath()              { return base() + ContextMachineRestServiceImpl.SKIP_PATH; }
    private String holdAllJobsPath()       { return base() + ContextMachineRestServiceImpl.HOLD_ALL_JOBS_PATH; }
    private String releaseAllJobsPath()    { return base() + ContextMachineRestServiceImpl.RELEASE_ALL_JOBS_PATH; }
    private String skipAllJobsPath()       { return base() + ContextMachineRestServiceImpl.SKIP_ALL_JOBS_PATH; }
    private String acknowledgeErrorPath()  { return base() + ContextMachineRestServiceImpl.ACKNOWLEDGE_ERROR_PATH; }
    private String broadcastLocalPath()    { return base() + ContextMachineRestServiceImpl.BROADCAST_LOCAL_EVENT_PATH; }
    private String disableQuartzPath()     { return base() + ContextMachineRestServiceImpl.DISABLE_QUARTZ_JOBS_PATH; }
    private String enableQuartzPath()      { return base() + ContextMachineRestServiceImpl.ENABLE_QUARTZ_JOBS_PATH; }
    private String runUntilEndedPath()     { return base() + ContextMachineRestServiceImpl.RUN_UNTIL_MANUALLY_ENDED_PATH; }
    private String releaseQueuedPath()     { return base() + ContextMachineRestServiceImpl.RELEASE_QUEUED_JOBS_PATH; }
    private String killRunningPath()       { return base() + ContextMachineRestServiceImpl.KILL_RUNNING_JOBS_PATH; }
    private String saveContextPath()       { return base() + ContextMachineRestServiceImpl.SAVE_CONTEXT_PATH; }
    private String updateParamsPath()      { return base() + ContextMachineRestServiceImpl.UPDATE_CONTEXT_PARAMETERS_PATH; }
    private String dryRunParamsPath()      { return base() + ContextMachineRestServiceImpl.DRY_RUN_PARAMETERS_PATH; }
    private String broadcastGlobalPath()   { return base() + ContextMachineRestServiceImpl.BROADCAST_GLOBAL_EVENTS_PATH; }
    private String publishInitiationPath() { return base() + ContextMachineRestServiceImpl.PUBLISH_JOB_INITIATION_PATH; }
    private String addQueuedPath()         { return base() + ContextMachineRestServiceImpl.ADD_QUEUED_INITIATION_PATH; }
    private String resubmitDlqPath()       { return base() + ContextMachineRestServiceImpl.RESUBMIT_DLQ_PATH; }
    private String getContextPath()        { return base() + ContextMachineRestServiceImpl.GET_CONTEXT_PATH; }
    private String getChildContextPath()   { return base() + ContextMachineRestServiceImpl.GET_CONTEXT_PATH + "/" + CONTEXT_NAME; }
    private String contextStatusPath()     { return base() + ContextMachineRestServiceImpl.GET_CONTEXT_STATUS_PATH + "/" + CONTEXT_NAME; }
    private String jobStatusPath()         { return base() + ContextMachineRestServiceImpl.GET_JOB_STATUS_PATH + "/" + CONTEXT_NAME + "/" + JOB_IDENTIFIER; }
    private String instanceStatusPath()    { return base() + ContextMachineRestServiceImpl.GET_INSTANCE_STATUS_PATH; }
    private String isDryRunPath()          { return base() + ContextMachineRestServiceImpl.IS_DRY_RUN_PATH; }
    private String dlqMessagesPath()       { return base() + ContextMachineRestServiceImpl.GET_DLQ_MESSAGES_PATH; }
    private String dlqMessagePath()        { return base() + ContextMachineRestServiceImpl.GET_DLQ_MESSAGES_PATH + "/" + MESSAGE_ID; }

    // ── holdJob ───────────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_holdJob_returns_true_on_200() {
        stubFor(put(urlEqualTo(holdPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.holdJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME));
        verify(putRequestedFor(urlEqualTo(holdPath())));
    }

    @Test
    public void test_holdJob_returns_false_on_404() {
        stubFor(put(urlEqualTo(holdPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.holdJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME));
    }

    // ── releaseJob ────────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_releaseJob_returns_true_on_200() {
        stubFor(put(urlEqualTo(releasePath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.releaseJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME));
        verify(putRequestedFor(urlEqualTo(releasePath())));
    }

    @Test
    public void test_releaseJob_returns_false_on_404() {
        stubFor(put(urlEqualTo(releasePath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.releaseJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME));
    }

    // ── resetJob ──────────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_resetJob_returns_true_on_200() {
        stubFor(put(urlEqualTo(resetPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.resetJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME));
        verify(putRequestedFor(urlEqualTo(resetPath())));
    }

    @Test
    public void test_resetJob_returns_false_on_404() {
        stubFor(put(urlEqualTo(resetPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.resetJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME));
    }

    // ── skipJob ───────────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_skipJob_returns_true_on_200() {
        stubFor(put(urlEqualTo(skipPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.skipJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME, true));
        verify(putRequestedFor(urlEqualTo(skipPath())));
    }

    @Test
    public void test_skipJob_returns_false_on_404() {
        stubFor(put(urlEqualTo(skipPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.skipJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME, false));
    }

    // ── holdAllJobs ───────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_holdAllJobs_returns_true_on_200() {
        stubFor(put(urlEqualTo(holdAllJobsPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT_NAME));
        verify(putRequestedFor(urlEqualTo(holdAllJobsPath())));
    }

    @Test
    public void test_holdAllJobs_returns_false_on_404() {
        stubFor(put(urlEqualTo(holdAllJobsPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT_NAME));
    }

    // ── releaseAllJobs ────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_releaseAllJobs_returns_true_on_200() {
        stubFor(put(urlEqualTo(releaseAllJobsPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.releaseAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT_NAME));
        verify(putRequestedFor(urlEqualTo(releaseAllJobsPath())));
    }

    @Test
    public void test_releaseAllJobs_returns_false_on_404() {
        stubFor(put(urlEqualTo(releaseAllJobsPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.releaseAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT_NAME));
    }

    // ── skipAllJobs ───────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_skipAllJobs_returns_true_on_200() {
        stubFor(put(urlEqualTo(skipAllJobsPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.skipAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT_NAME, true));
        verify(putRequestedFor(urlEqualTo(skipAllJobsPath())));
    }

    @Test
    public void test_skipAllJobs_returns_false_on_404() {
        stubFor(put(urlEqualTo(skipAllJobsPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.skipAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT_NAME, false));
    }

    // ── acknowledgeError ─────────────────────────────────────────────────────────────────────────

    @Test
    public void test_acknowledgeError_returns_true_on_200() {
        stubFor(put(urlEqualTo(acknowledgeErrorPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.acknowledgeError(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, false, CHILD_CONTEXT_NAME, List.of()));
        verify(putRequestedFor(urlEqualTo(acknowledgeErrorPath())));
    }

    // ── broadcastLocalEvent ───────────────────────────────────────────────────────────────────────

    @Test
    public void test_broadcastLocalEvent_returns_true_on_200() {
        stubFor(put(urlEqualTo(broadcastLocalPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.broadcastLocalEvent(CONTEXT_INSTANCE_ID, AGENT_NAME, JOB_NAME, CONTEXT_NAME, CONTEXT_INSTANCE_ID, List.of()));
        verify(putRequestedFor(urlEqualTo(broadcastLocalPath())));
    }

    // ── disableQuartzJobs ─────────────────────────────────────────────────────────────────────────

    @Test
    public void test_disableQuartzJobs_returns_true_on_200() {
        stubFor(put(urlEqualTo(disableQuartzPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.disableQuartzJobs(CONTEXT_INSTANCE_ID));
        verify(putRequestedFor(urlEqualTo(disableQuartzPath())));
    }

    // ── enableQuartzJobs ──────────────────────────────────────────────────────────────────────────

    @Test
    public void test_enableQuartzJobs_returns_true_on_200() {
        stubFor(put(urlEqualTo(enableQuartzPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.enableQuartzJobs(CONTEXT_INSTANCE_ID));
        verify(putRequestedFor(urlEqualTo(enableQuartzPath())));
    }

    // ── runUntilManuallyEnded ─────────────────────────────────────────────────────────────────────

    @Test
    public void test_runUntilManuallyEnded_returns_true_on_200() {
        stubFor(put(urlEqualTo(runUntilEndedPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.runUntilManuallyEnded(CONTEXT_INSTANCE_ID));
        verify(putRequestedFor(urlEqualTo(runUntilEndedPath())));
    }

    // ── releaseQueuedJobs ─────────────────────────────────────────────────────────────────────────

    @Test
    public void test_releaseQueuedJobs_returns_true_on_200() {
        stubFor(put(urlEqualTo(releaseQueuedPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.releaseQueuedJobs(CONTEXT_INSTANCE_ID));
        verify(putRequestedFor(urlEqualTo(releaseQueuedPath())));
    }

    // ── killRunningJobs ───────────────────────────────────────────────────────────────────────────

    @Test
    public void test_killRunningJobs_returns_true_on_200() {
        stubFor(put(urlEqualTo(killRunningPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.killRunningJobs(CONTEXT_INSTANCE_ID));
        verify(putRequestedFor(urlEqualTo(killRunningPath())));
    }

    // ── saveContext ───────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_saveContext_returns_true_on_200() {
        stubFor(put(urlEqualTo(saveContextPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.saveContext(CONTEXT_INSTANCE_ID));
        verify(putRequestedFor(urlEqualTo(saveContextPath())));
    }

    // ── updateContextParameters ───────────────────────────────────────────────────────────────────

    @Test
    public void test_updateContextParameters_returns_true_on_200() {
        stubFor(put(urlEqualTo(updateParamsPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.updateContextParameters(CONTEXT_INSTANCE_ID, List.of()));
        verify(putRequestedFor(urlEqualTo(updateParamsPath())));
    }

    // ── setDryRunParameters ───────────────────────────────────────────────────────────────────────

    @Test
    public void test_setDryRunParameters_returns_true_on_200() {
        stubFor(put(urlEqualTo(dryRunParamsPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.setDryRunParameters(CONTEXT_INSTANCE_ID, 100L, 1000L, 500L, 0.0, false));
        verify(putRequestedFor(urlEqualTo(dryRunParamsPath())));
    }

    // ── broadcastGlobalEvents ─────────────────────────────────────────────────────────────────────

    @Test
    public void test_broadcastGlobalEvents_returns_true_on_200() {
        stubFor(put(urlEqualTo(broadcastGlobalPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.broadcastGlobalEvents(CONTEXT_INSTANCE_ID, AGENT_NAME, JOB_NAME, CONTEXT_NAME,
            CONTEXT_INSTANCE_ID, List.of(), false, false));
        verify(putRequestedFor(urlEqualTo(broadcastGlobalPath())));
    }

    // ── publishJobInitiationEvent ─────────────────────────────────────────────────────────────────

    @Test
    public void test_publishJobInitiationEvent_returns_true_on_200() {
        stubFor(put(urlEqualTo(publishInitiationPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.publishJobInitiationEvent(CONTEXT_INSTANCE_ID, AGENT_NAME, JOB_NAME, CONTEXT_NAME,
            CONTEXT_INSTANCE_ID, List.of()));
        verify(putRequestedFor(urlEqualTo(publishInitiationPath())));
    }

    // ── addQueuedInitiationEvent ──────────────────────────────────────────────────────────────────

    @Test
    public void test_addQueuedInitiationEvent_returns_true_on_200() {
        stubFor(put(urlEqualTo(addQueuedPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.addQueuedInitiationEvent(CONTEXT_INSTANCE_ID, AGENT_NAME, JOB_NAME, CONTEXT_NAME,
            CONTEXT_INSTANCE_ID, List.of()));
        verify(putRequestedFor(urlEqualTo(addQueuedPath())));
    }

    // ── resubmitDlq ───────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_resubmitDlq_returns_true_on_200() {
        stubFor(put(urlEqualTo(resubmitDlqPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.resubmitDlq(CONTEXT_INSTANCE_ID, MESSAGE_ID));
        verify(putRequestedFor(urlEqualTo(resubmitDlqPath())));
    }

    @Test
    public void test_resubmitDlq_returns_false_on_404() {
        stubFor(put(urlEqualTo(resubmitDlqPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.resubmitDlq(CONTEXT_INSTANCE_ID, MESSAGE_ID));
    }

    // ── getContextFromPeer ────────────────────────────────────────────────────────────────────────

    @Test
    public void test_getContextFromPeer_calls_correct_path() {
        stubFor(get(urlEqualTo(getContextPath())).willReturn(aResponse().withStatus(404)));

        assertNull(uut.getContextFromPeer(CONTEXT_INSTANCE_ID));
        verify(getRequestedFor(urlEqualTo(getContextPath())));
    }

    // ── getChildContextFromPeer ───────────────────────────────────────────────────────────────────

    @Test
    public void test_getChildContextFromPeer_includes_context_name_in_path() {
        stubFor(get(urlEqualTo(getChildContextPath())).willReturn(aResponse().withStatus(404)));

        assertNull(uut.getChildContextFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME));
        verify(getRequestedFor(urlEqualTo(getChildContextPath())));
    }

    // ── getContextStatusFromPeer ──────────────────────────────────────────────────────────────────

    @Test
    public void test_getContextStatusFromPeer_calls_correct_path() {
        stubFor(get(urlEqualTo(contextStatusPath())).willReturn(aResponse().withStatus(404)));

        assertNull(uut.getContextStatusFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME));
        verify(getRequestedFor(urlEqualTo(contextStatusPath())));
    }

    // ── getJobStatusFromPeer ──────────────────────────────────────────────────────────────────────

    @Test
    public void test_getJobStatusFromPeer_includes_context_and_job_in_path() {
        stubFor(get(urlEqualTo(jobStatusPath())).willReturn(aResponse().withStatus(404)));

        assertNull(uut.getJobStatusFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME, JOB_IDENTIFIER));
        verify(getRequestedFor(urlEqualTo(jobStatusPath())));
    }

    // ── getInstanceStatusFromPeer ─────────────────────────────────────────────────────────────────

    @Test
    public void test_getInstanceStatusFromPeer_calls_correct_path() {
        stubFor(get(urlEqualTo(instanceStatusPath())).willReturn(aResponse().withStatus(404)));

        assertNull(uut.getInstanceStatusFromPeer(CONTEXT_INSTANCE_ID));
        verify(getRequestedFor(urlEqualTo(instanceStatusPath())));
    }

    // ── isDryRunOnPeer ────────────────────────────────────────────────────────────────────────────

    @Test
    public void test_isDryRunOnPeer_returns_true_when_server_returns_true() {
        stubFor(get(urlEqualTo(isDryRunPath()))
            .willReturn(aResponse().withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("true")));

        assertTrue(uut.isDryRunOnPeer(CONTEXT_INSTANCE_ID));
        verify(getRequestedFor(urlEqualTo(isDryRunPath())));
    }

    @Test
    public void test_isDryRunOnPeer_returns_null_on_404() {
        stubFor(get(urlEqualTo(isDryRunPath())).willReturn(aResponse().withStatus(404)));

        assertNull(uut.isDryRunOnPeer(CONTEXT_INSTANCE_ID));
    }

    // ── getDlqMessagesFromPeer ────────────────────────────────────────────────────────────────────

    @Test
    public void test_getDlqMessagesFromPeer_returns_empty_list_when_server_returns_empty_array() {
        stubFor(get(urlEqualTo(dlqMessagesPath()))
            .willReturn(aResponse().withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("[]")));

        List<?> result = uut.getDlqMessagesFromPeer(CONTEXT_INSTANCE_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(getRequestedFor(urlEqualTo(dlqMessagesPath())));
    }

    @Test
    public void test_getDlqMessagesFromPeer_returns_null_on_404() {
        stubFor(get(urlEqualTo(dlqMessagesPath())).willReturn(aResponse().withStatus(404)));

        assertNull(uut.getDlqMessagesFromPeer(CONTEXT_INSTANCE_ID));
    }

    // ── deleteDlqMessage ──────────────────────────────────────────────────────────────────────────

    @Test
    public void test_deleteDlqMessage_returns_true_on_200() {
        stubFor(delete(urlEqualTo(dlqMessagePath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.deleteDlqMessage(CONTEXT_INSTANCE_ID, MESSAGE_ID));
        verify(deleteRequestedFor(urlEqualTo(dlqMessagePath())));
    }

    @Test
    public void test_deleteDlqMessage_returns_false_on_404() {
        stubFor(delete(urlEqualTo(dlqMessagePath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.deleteDlqMessage(CONTEXT_INSTANCE_ID, MESSAGE_ID));
    }

    // ── deleteAllDlqMessages ──────────────────────────────────────────────────────────────────────

    @Test
    public void test_deleteAllDlqMessages_returns_true_on_200() {
        stubFor(delete(urlEqualTo(dlqMessagesPath())).willReturn(aResponse().withStatus(200)));

        assertTrue(uut.deleteAllDlqMessages(CONTEXT_INSTANCE_ID));
        verify(deleteRequestedFor(urlEqualTo(dlqMessagesPath())));
    }

    @Test
    public void test_deleteAllDlqMessages_returns_false_on_404() {
        stubFor(delete(urlEqualTo(dlqMessagesPath())).willReturn(aResponse().withStatus(404)));

        assertFalse(uut.deleteAllDlqMessages(CONTEXT_INSTANCE_ID));
    }

    // ── Request body mapping ─────────────────────────────────────────────────────────────────────

    @Test
    public void test_holdJob_sends_job_action_body() {
        stubFor(put(urlEqualTo(holdPath()))
            .withRequestBody(matchingJsonPath("$.jobIdentifier", equalTo(JOB_IDENTIFIER)))
            .withRequestBody(matchingJsonPath("$.childContextName", equalTo(CHILD_CONTEXT_NAME)))
            .withRequestBody(matchingJsonPath("$.skipFlag", equalTo("false")))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.holdJob(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, CHILD_CONTEXT_NAME));

        verify(putRequestedFor(urlEqualTo(holdPath()))
            .withRequestBody(matchingJsonPath("$.jobIdentifier", equalTo(JOB_IDENTIFIER)))
            .withRequestBody(matchingJsonPath("$.childContextName", equalTo(CHILD_CONTEXT_NAME))));
    }

    @Test
    public void test_skipAllJobs_sends_skip_flag_body() {
        stubFor(put(urlEqualTo(skipAllJobsPath()))
            .withRequestBody(matchingJsonPath("$.childContextName", equalTo(CHILD_CONTEXT_NAME)))
            .withRequestBody(matchingJsonPath("$.skipFlag", equalTo("true")))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.skipAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT_NAME, true));

        verify(putRequestedFor(urlEqualTo(skipAllJobsPath()))
            .withRequestBody(matchingJsonPath("$.childContextName", equalTo(CHILD_CONTEXT_NAME)))
            .withRequestBody(matchingJsonPath("$.skipFlag", equalTo("true"))));
    }

    @Test
    public void test_acknowledgeError_sends_all_body_fields() {
        stubFor(put(urlEqualTo(acknowledgeErrorPath()))
            .withRequestBody(matchingJsonPath("$.identifier", equalTo(JOB_IDENTIFIER)))
            .withRequestBody(matchingJsonPath("$.targetResidingContextOnly", equalTo("true")))
            .withRequestBody(matchingJsonPath("$.childContextName", equalTo(CHILD_CONTEXT_NAME)))
            .withRequestBody(matchingJsonPath("$.childContextNames[0]", equalTo(CHILD_CONTEXT_NAME)))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.acknowledgeError(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, true, CHILD_CONTEXT_NAME,
            List.of(CHILD_CONTEXT_NAME)));
    }

    @Test
    public void test_updateContextParameters_sends_parameter_array_body() {
        ContextParameterInstanceImpl parameter = new ContextParameterInstanceImpl();
        parameter.setName("threshold");
        parameter.setDefaultValue("10");
        parameter.setValue("25");
        stubFor(put(urlEqualTo(updateParamsPath()))
            .withRequestBody(matchingJsonPath("$[0].name", equalTo("threshold")))
            .withRequestBody(matchingJsonPath("$[0].defaultValue", equalTo("10")))
            .withRequestBody(matchingJsonPath("$[0].value", equalTo("25")))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.updateContextParameters(CONTEXT_INSTANCE_ID, List.of(parameter)));
    }

    @Test
    public void test_setDryRunParameters_sends_all_body_fields() {
        stubFor(put(urlEqualTo(dryRunParamsPath()))
            .withRequestBody(matchingJsonPath("$.minExecutionTimeMillis", equalTo("100")))
            .withRequestBody(matchingJsonPath("$.maxExecutionTimeMillis", equalTo("1000")))
            .withRequestBody(matchingJsonPath("$.fixedExecutionTimeMillis", equalTo("500")))
            .withRequestBody(matchingJsonPath("$.jobErrorPercentage", equalTo("12.5")))
            .withRequestBody(matchingJsonPath("$.error", equalTo("true")))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.setDryRunParameters(CONTEXT_INSTANCE_ID, 100L, 1000L, 500L, 12.5, true));
    }

    @Test
    public void test_broadcastGlobalEvents_sends_all_body_fields() {
        stubFor(put(urlEqualTo(broadcastGlobalPath()))
            .withRequestBody(matchingJsonPath("$.agentName", equalTo(AGENT_NAME)))
            .withRequestBody(matchingJsonPath("$.jobName", equalTo(JOB_NAME)))
            .withRequestBody(matchingJsonPath("$.contextName", equalTo(CONTEXT_NAME)))
            .withRequestBody(matchingJsonPath("$.contextInstanceId", equalTo(CONTEXT_INSTANCE_ID)))
            .withRequestBody(matchingJsonPath("$.childContextNames[0]", equalTo(CHILD_CONTEXT_NAME)))
            .withRequestBody(matchingJsonPath("$.ignoreEnvironmentGroup", equalTo("true")))
            .withRequestBody(matchingJsonPath("$.forceSending", equalTo("true")))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.broadcastGlobalEvents(CONTEXT_INSTANCE_ID, AGENT_NAME, JOB_NAME, CONTEXT_NAME,
            CONTEXT_INSTANCE_ID, List.of(CHILD_CONTEXT_NAME), true, true));
    }

    @Test
    public void test_resubmitDlq_sends_message_id_body() {
        stubFor(put(urlEqualTo(resubmitDlqPath()))
            .withRequestBody(matchingJsonPath("$.messageId", equalTo(MESSAGE_ID)))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.resubmitDlq(CONTEXT_INSTANCE_ID, MESSAGE_ID));
    }

    // ── Read response mapping ────────────────────────────────────────────────────────────────────

    @Test
    public void test_getContextStatusFromPeer_returns_status_enum() {
        stubFor(get(urlEqualTo(contextStatusPath()))
            .willReturn(aResponse().withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("\"RUNNING\"")));

        assertEquals(InstanceStatus.RUNNING, uut.getContextStatusFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME));
    }

    @Test
    public void test_getJobStatusFromPeer_returns_status_enum() {
        stubFor(get(urlEqualTo(jobStatusPath()))
            .willReturn(aResponse().withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("\"ERROR\"")));

        assertEquals(InstanceStatus.ERROR, uut.getJobStatusFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME, JOB_IDENTIFIER));
    }

    @Test
    public void test_getInstanceStatusFromPeer_deserialises_status_body() {
        stubFor(get(urlEqualTo(instanceStatusPath()))
            .willReturn(aResponse().withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"contextName\":\"" + CONTEXT_NAME + "\",\"instanceStatus\":\"COMPLETE\"}")));

        ContextInstanceStatus result = uut.getInstanceStatusFromPeer(CONTEXT_INSTANCE_ID);

        assertNotNull(result);
        assertEquals(CONTEXT_NAME, result.getContextName());
        assertEquals(InstanceStatus.COMPLETE, result.getInstanceStatus());
    }
}
