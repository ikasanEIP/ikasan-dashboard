package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.job.orchestration.model.event.DryRunParametersImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.rest.client.ContextMachineRestServiceImpl;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the routing/retry logic in {@link ContextMachineRestImpl}.
 * -
 * The 24 delegation methods are all structurally identical pass-throughs, so
 * holdJobs / getContext serve as representatives for doWrite / doRead patterns.
 * The key distinction under test: in doRead, a null response from the service is
 * a definitive server answer and is returned immediately without retrying, whereas
 * in doWrite, a false response triggers a retry.
 */
class ContextMachineRestImplTest {

    private static final String CONTEXT_INSTANCE_ID = "ctx-instance-1";
    private static final String LEADER_URL          = "http://leader:8080";
    private static final String CHILD_CONTEXT       = "child-ctx";
    private static final String CONTEXT_NAME        = "ctx";
    private static final String JOB_IDENTIFIER      = "job-1";
    private static final String AGENT_NAME          = "agent-1";
    private static final String JOB_NAME            = "job-name";
    private static final String MESSAGE_ID          = "message-1";

    private ContextMachineRestServiceImpl mockSvc;
    private Map<String, ContextMachineRestServiceImpl> peersByUrl;

    // Mutable field read by the lambda supplier — lets tests control leader availability
    private String leaderUrl;

    // 0 retries — for happy-path and null-wrapping tests
    private ContextMachineRestImpl subject;
    // 1 retry — for retry-path tests
    private ContextMachineRestImpl subjectWithRetries;

    @BeforeEach
    void setUp() {
        mockSvc = mock(ContextMachineRestServiceImpl.class);
        peersByUrl = new HashMap<>();
        peersByUrl.put(LEADER_URL.toLowerCase(), mockSvc);

        leaderUrl = LEADER_URL; // default: leader available

        subject            = new ContextMachineRestImpl(CONTEXT_INSTANCE_ID, () -> leaderUrl, peersByUrl, 0, 0L);
        subjectWithRetries = new ContextMachineRestImpl(CONTEXT_INSTANCE_ID, () -> leaderUrl, peersByUrl, 1, 0L);
    }

    // ── isLocal ─────────────────────────────────────────────────────────────────────────────────

    @Test
    void isLocal_returns_false() {
        assertFalse(subject.isLocal());
    }

    // ── doWrite happy path ───────────────────────────────────────────────────────────────────────

    @Test
    void holdJobs_delegates_to_service_when_leader_available() {
        when(mockSvc.holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT)).thenReturn(true);

        subject.holdJobs(CHILD_CONTEXT);

        verify(mockSvc).holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT);
    }

    @Test
    void doWrite_does_nothing_when_leader_supplier_returns_null() {
        leaderUrl = null;

        subject.holdJobs(CHILD_CONTEXT);

        verifyNoInteractions(mockSvc);
    }

    // ── doWrite retry paths ──────────────────────────────────────────────────────────────────────

    @Test
    void doWrite_retries_on_resource_access_exception_then_succeeds() {
        when(mockSvc.holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT))
            .thenThrow(new ResourceAccessException("network down"))
            .thenReturn(true);

        subjectWithRetries.holdJobs(CHILD_CONTEXT);

        verify(mockSvc, times(2)).holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT);
    }

    @Test
    void doWrite_retries_when_op_returns_false_then_succeeds() {
        // false means the leader didn't own the context instance; retry on next leader
        when(mockSvc.holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT))
            .thenReturn(false)
            .thenReturn(true);

        subjectWithRetries.holdJobs(CHILD_CONTEXT);

        verify(mockSvc, times(2)).holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT);
    }

    @Test
    void doWrite_exhausts_all_retries_on_persistent_network_failure() {
        when(mockSvc.holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT))
            .thenThrow(new ResourceAccessException("network down"));

        subjectWithRetries.holdJobs(CHILD_CONTEXT); // must not throw

        // 1 initial attempt + 1 configured retry = 2 total
        verify(mockSvc, times(2)).holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT);
    }

    // ── doRead happy path ────────────────────────────────────────────────────────────────────────

    @Test
    void getContext_returns_result_from_service() {
        ContextInstance mockCtx = mock(ContextInstance.class);
        when(mockSvc.getContextFromPeer(CONTEXT_INSTANCE_ID)).thenReturn(mockCtx);

        assertSame(mockCtx, subject.getContext());
    }

    @Test
    void getContext_with_context_name_delegates_context_name_to_service() {
        ContextInstance mockCtx = mock(ContextInstance.class);
        when(mockSvc.getChildContextFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME)).thenReturn(mockCtx);

        assertSame(mockCtx, subject.getContext(CONTEXT_NAME));
        verify(mockSvc).getChildContextFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME);
    }

    @Test
    void getJobStatus_delegates_context_name_and_job_identifier_to_service() {
        when(mockSvc.getJobStatusFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME, JOB_IDENTIFIER))
            .thenReturn(InstanceStatus.RUNNING);

        assertEquals(InstanceStatus.RUNNING, subject.getJobStatus(CONTEXT_NAME, JOB_IDENTIFIER));
        verify(mockSvc).getJobStatusFromPeer(CONTEXT_INSTANCE_ID, CONTEXT_NAME, JOB_IDENTIFIER);
    }

    @Test
    void doRead_returns_fallback_when_no_leader() {
        leaderUrl = null;

        ContextInstance result = subject.getContext();

        assertNull(result);
        verifyNoInteractions(mockSvc);
    }

    // ── doRead vs doWrite null-semantics distinction ─────────────────────────────────────────────

    @Test
    void doRead_returns_null_immediately_without_retry_when_service_returns_null() {
        // null from the service is a definitive "not found" answer — not a signal to retry.
        // Contrast with doWrite, where false means "not handled here, try again".
        when(mockSvc.getContextFromPeer(CONTEXT_INSTANCE_ID)).thenReturn(null);

        ContextInstance result = subjectWithRetries.getContext(); // leaderLookupRetries=1

        assertNull(result);
        verify(mockSvc, times(1)).getContextFromPeer(CONTEXT_INSTANCE_ID); // called exactly once
    }

    // ── doRead retry paths ───────────────────────────────────────────────────────────────────────

    @Test
    void doRead_retries_on_resource_access_exception_then_returns_result() {
        ContextInstance mockCtx = mock(ContextInstance.class);
        when(mockSvc.getContextFromPeer(CONTEXT_INSTANCE_ID))
            .thenThrow(new ResourceAccessException("network"))
            .thenReturn(mockCtx);

        assertSame(mockCtx, subjectWithRetries.getContext());
        verify(mockSvc, times(2)).getContextFromPeer(CONTEXT_INSTANCE_ID);
    }

    @Test
    void doRead_returns_fallback_after_exhausting_retries() {
        when(mockSvc.getContextFromPeer(CONTEXT_INSTANCE_ID))
            .thenThrow(new ResourceAccessException("network"));

        ContextInstance result = subjectWithRetries.getContext();

        assertNull(result); // fallback
        verify(mockSvc, times(2)).getContextFromPeer(CONTEXT_INSTANCE_ID); // both attempts made
    }

    // ── URL case-insensitivity ───────────────────────────────────────────────────────────────────

    @Test
    void resolves_leader_case_insensitively() {
        leaderUrl = LEADER_URL.toUpperCase();
        when(mockSvc.holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT)).thenReturn(true);

        subject.holdJobs(CHILD_CONTEXT);

        verify(mockSvc).holdAllJobs(CONTEXT_INSTANCE_ID, CHILD_CONTEXT);
    }

    // ── Null-result wrapping ─────────────────────────────────────────────────────────────────────

    @Test
    void isDryRun_returns_false_when_service_returns_null() {
        // isDryRunOnPeer returns Boolean (object), so null is a valid mock return
        when(mockSvc.isDryRunOnPeer(CONTEXT_INSTANCE_ID)).thenReturn(null);

        assertFalse(subject.isDryRun());
    }

    @Test
    void isDryRun_returns_true_when_service_returns_true() {
        when(mockSvc.isDryRunOnPeer(CONTEXT_INSTANCE_ID)).thenReturn(true);

        assertTrue(subject.isDryRun());
    }

    @Test
    void getDlqMessages_returns_empty_list_when_service_returns_null() {
        when(mockSvc.getDlqMessagesFromPeer(CONTEXT_INSTANCE_ID)).thenReturn(null);

        List<?> result = subject.getDlqMessages();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void setDryRunParameters_delegates_all_parameter_values_to_service() {
        DryRunParametersImpl dryRunParameters = new DryRunParametersImpl();
        dryRunParameters.setMinExecutionTimeMillis(11L);
        dryRunParameters.setMaxExecutionTimeMillis(22L);
        dryRunParameters.setFixedExecutionTimeMillis(33L);
        dryRunParameters.setJobErrorPercentage(44.5);
        dryRunParameters.setError(true);
        when(mockSvc.setDryRunParameters(CONTEXT_INSTANCE_ID, 11L, 22L, 33L, 44.5, true)).thenReturn(true);

        subject.setDryRunParameters(dryRunParameters);

        verify(mockSvc).setDryRunParameters(CONTEXT_INSTANCE_ID, 11L, 22L, 33L, 44.5, true);
    }

    @Test
    void acknowledgeSchedulerJobError_delegates_job_identity_and_child_contexts_to_service() {
        InternalEventDrivenJobInstanceImpl jobInstance = new InternalEventDrivenJobInstanceImpl();
        jobInstance.setIdentifier(JOB_IDENTIFIER);
        jobInstance.setTargetResidingContextOnly(true);
        jobInstance.setChildContextName(CHILD_CONTEXT);
        jobInstance.setChildContextNames(List.of(CHILD_CONTEXT));
        when(mockSvc.acknowledgeError(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, true, CHILD_CONTEXT, List.of(CHILD_CONTEXT)))
            .thenReturn(true);

        subject.acknowledgeSchedulerJobError(jobInstance);

        verify(mockSvc).acknowledgeError(CONTEXT_INSTANCE_ID, JOB_IDENTIFIER, true, CHILD_CONTEXT, List.of(CHILD_CONTEXT));
    }

    @Test
    void broadcastGlobalEvents_delegates_event_fields_and_flags_to_service() throws Exception {
        SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
        event.setAgentName(AGENT_NAME);
        event.setJobName(JOB_NAME);
        event.setContextName(CONTEXT_NAME);
        event.setContextInstanceId(CONTEXT_INSTANCE_ID);
        event.setChildContextNames(List.of(CHILD_CONTEXT));
        when(mockSvc.broadcastGlobalEvents(CONTEXT_INSTANCE_ID, AGENT_NAME, JOB_NAME, CONTEXT_NAME,
            CONTEXT_INSTANCE_ID, List.of(CHILD_CONTEXT), true, true)).thenReturn(true);

        subject.broadcastGlobalEvents(event, true, true);

        verify(mockSvc).broadcastGlobalEvents(CONTEXT_INSTANCE_ID, AGENT_NAME, JOB_NAME, CONTEXT_NAME,
            CONTEXT_INSTANCE_ID, List.of(CHILD_CONTEXT), true, true);
    }

    @Test
    void resubmitMessageFromDeadLetterQueue_returns_true_when_service_returns_true() throws Exception {
        when(mockSvc.resubmitDlq(CONTEXT_INSTANCE_ID, MESSAGE_ID)).thenReturn(true);

        assertTrue(subject.resubmitMessageFromDeadLetterQueue(MESSAGE_ID));
        verify(mockSvc).resubmitDlq(CONTEXT_INSTANCE_ID, MESSAGE_ID);
    }

    @Test
    void resubmitMessageFromDeadLetterQueue_retries_when_service_returns_false_then_true() throws Exception {
        when(mockSvc.resubmitDlq(CONTEXT_INSTANCE_ID, MESSAGE_ID))
            .thenReturn(false)
            .thenReturn(true);

        assertTrue(subjectWithRetries.resubmitMessageFromDeadLetterQueue(MESSAGE_ID));
        verify(mockSvc, times(2)).resubmitDlq(CONTEXT_INSTANCE_ID, MESSAGE_ID);
    }

    @Test
    void resubmitMessageFromDeadLetterQueue_returns_false_when_doRead_returns_null_fallback()
            throws Exception {
        // The service returns primitive boolean (cannot be null), so the null fallback only
        // occurs when no leader is available and doRead exhausts attempts.
        leaderUrl = null;

        assertFalse(subject.resubmitMessageFromDeadLetterQueue("msg-1"));
    }

    @Test
    void deleteDlqMessage_returns_false_when_doRead_returns_null_fallback() throws Exception {
        leaderUrl = null;

        assertFalse(subject.deleteDlqMessage("msg-1"));
    }

    @Test
    void deleteDlqMessage_returns_true_when_service_returns_true() throws Exception {
        when(mockSvc.deleteDlqMessage(CONTEXT_INSTANCE_ID, MESSAGE_ID)).thenReturn(true);

        assertTrue(subject.deleteDlqMessage(MESSAGE_ID));
        verify(mockSvc).deleteDlqMessage(CONTEXT_INSTANCE_ID, MESSAGE_ID);
    }

    @Test
    void deleteDlqMessage_retries_when_service_returns_false_then_true() throws Exception {
        when(mockSvc.deleteDlqMessage(CONTEXT_INSTANCE_ID, MESSAGE_ID))
            .thenReturn(false)
            .thenReturn(true);

        assertTrue(subjectWithRetries.deleteDlqMessage(MESSAGE_ID));
        verify(mockSvc, times(2)).deleteDlqMessage(CONTEXT_INSTANCE_ID, MESSAGE_ID);
    }

    // ── Unsupported lifecycle operations ─────────────────────────────────────────────────────────

    @Test
    void unsupported_lifecycle_methods_throw_UnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, subject::init);
    }
}
