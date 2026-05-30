package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.DryRunParametersImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.rest.dashboard.model.dto.*;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ContextMachineController.class)
@WebAppConfiguration
@EnableWebMvc
public class ContextMachineControllerTest extends AbstractRestMvcTest {

    private static final String CONTEXT_INSTANCE_ID = "test-context-instance-id";
    private static final String CONTEXT_NAME        = "test-context-name";
    private static final String JOB_IDENTIFIER      = "test-job-id";
    private static final String CHILD_CONTEXT_NAME  = "test-child-context";
    private static final String MESSAGE_ID          = "test-message-id";

    private static final String BASE = "/rest/contextMachine/" + CONTEXT_INSTANCE_ID;

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    private ContextMachine contextMachine;

    @BeforeEach
    public void setUp() {
        ContextMachineCache.instance().resetAllCache();
        ContextMachineCache.instance().registerLeaderProvider(null);
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        contextMachine = mock(ContextMachine.class);
        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setId(CONTEXT_INSTANCE_ID);
        contextInstance.setName(CONTEXT_NAME);
        contextInstance.setStatus(InstanceStatus.PREPARED);
        when(contextMachine.getContext()).thenReturn(contextInstance);
    }

    @AfterEach
    public void tearDown() {
        ContextMachineCache.instance().resetAllCache();
        ContextMachineCache.instance().registerLeaderProvider(null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void putInCache() {
        ContextMachineCache.instance().put(contextMachine);
        clearInvocations(contextMachine);
    }

    private void setLeader(boolean leader) {
        ContextMachineCache.instance().registerLeaderProvider(() -> leader);
    }

    private void assertSchedulerJobInitiationEvent(SchedulerJobInitiationEvent event) {
        assertEquals("agent", event.getAgentName());
        assertEquals("job", event.getJobName());
        assertEquals("ctx", event.getContextName());
        assertEquals(CONTEXT_INSTANCE_ID, event.getContextInstanceId());
        assertEquals(List.of(CHILD_CONTEXT_NAME), event.getChildContextNames());
    }

    // Because the controller is pure routing — every endpoint has the same shape — rather than 20×3 = 60 tests, we have
    //  - One test of each pattern's 404 path
    //  - One test of each pattern's 500 path
    //  - Per-endpoint success tests for each of the 20 endpoints (confirms the right method is called and 200 is returned)

    // -------------------------------------------------------------------------
    // Pattern coverage — cache-miss 404, leader-miss 404, 500
    // -------------------------------------------------------------------------

    @Test
    public void test_returns_404_when_context_machine_not_in_cache() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/holdAllJobs")
            .content(mapToJson(new ContextMachineJobActionDto()))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_leader_gated_returns_404_when_not_cluster_leader() throws Exception {
        putInCache();
        setLeader(false);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/context")).andReturn();
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_leader_gated_returns_404_when_leader_but_context_machine_not_local() throws Exception {
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/context")).andReturn();
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_returns_500_when_context_machine_throws() throws Exception {
        putInCache();
        doThrow(new RuntimeException("simulated failure")).when(contextMachine).holdJobs(any());
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/holdAllJobs")
            .content(mapToJson(new ContextMachineJobActionDto(null, CHILD_CONTEXT_NAME)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_leader_gated_returns_500_when_context_machine_throws() throws Exception {
        putInCache();
        setLeader(true);
        doThrow(new RuntimeException("simulated failure")).when(contextMachine).deleteAllDlqMessages();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.delete(BASE + "/dlqMessages")).andReturn();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
    }

    // -------------------------------------------------------------------------
    // holdAllJobs
    // -------------------------------------------------------------------------

    @Test
    public void test_holdAllJobs_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/holdAllJobs")
            .content(mapToJson(new ContextMachineJobActionDto(null, CHILD_CONTEXT_NAME)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).holdJobs(CHILD_CONTEXT_NAME);
    }

    // -------------------------------------------------------------------------
    // releaseAllJobs
    // -------------------------------------------------------------------------

    @Test
    public void test_releaseAllJobs_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/releaseAllJobs")
            .content(mapToJson(new ContextMachineJobActionDto(null, CHILD_CONTEXT_NAME)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).releaseJobs(CHILD_CONTEXT_NAME);
    }

    // -------------------------------------------------------------------------
    // skipAllJobs
    // -------------------------------------------------------------------------

    @Test
    public void test_skipAllJobs_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/skipAllJobs")
            .content(mapToJson(new ContextMachineJobActionDto(null, CHILD_CONTEXT_NAME, true)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).skipJobs(CHILD_CONTEXT_NAME, true);
    }

    // -------------------------------------------------------------------------
    // getContext (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_getContext_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/context")).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).getContext();
    }

    // -------------------------------------------------------------------------
    // getChildContext (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_getChildContext_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/context/" + CONTEXT_NAME)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).getContext(CONTEXT_NAME);
    }

    // -------------------------------------------------------------------------
    // getContextStatus (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_getContextStatus_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/contextStatus/" + CONTEXT_NAME)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).getContextStatus(CONTEXT_NAME);
    }

    // -------------------------------------------------------------------------
    // getJobStatus (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_getJobStatus_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(
            BASE + "/jobStatus/" + CONTEXT_NAME + "/" + JOB_IDENTIFIER)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).getJobStatus(CONTEXT_NAME, JOB_IDENTIFIER);
    }

    // -------------------------------------------------------------------------
    // getContextInstanceStatus (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_getContextInstanceStatus_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/instanceStatus")).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).getContextInstanceStatus();
    }

    // -------------------------------------------------------------------------
    // isDryRun (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_isDryRun_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/isDryRun")).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).isDryRun();
    }

    // -------------------------------------------------------------------------
    // disableQuartzJobs
    // -------------------------------------------------------------------------

    @Test
    public void test_disableQuartzJobs_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/disableQuartzJobs")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).disableQuartzBasedJobs();
    }

    // -------------------------------------------------------------------------
    // enableQuartzJobs
    // -------------------------------------------------------------------------

    @Test
    public void test_enableQuartzJobs_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/enableQuartzJobs")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).enableQuartzBasedJobs();
    }

    // -------------------------------------------------------------------------
    // runUntilManuallyEnded
    // -------------------------------------------------------------------------

    @Test
    public void test_runUntilManuallyEnded_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/runUntilManuallyEnded")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).runContextUntilManuallyEnded();
    }

    // -------------------------------------------------------------------------
    // releaseQueuedJobs
    // -------------------------------------------------------------------------

    @Test
    public void test_releaseQueuedJobs_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/releaseQueuedJobs")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).releaseQueuedJobs();
    }

    // -------------------------------------------------------------------------
    // killRunningJobs
    // -------------------------------------------------------------------------

    @Test
    public void test_killRunningJobs_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/killRunningJobs")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).killRunningJobs();
    }

    // -------------------------------------------------------------------------
    // saveContext
    // -------------------------------------------------------------------------

    @Test
    public void test_saveContext_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/saveContext")
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).saveContext();
    }

    // -------------------------------------------------------------------------
    // updateContextParameters (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_updateContextParameters_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        ContextParameterInstanceImpl param = new ContextParameterInstanceImpl();
        param.setName("param1");
        param.setValue("value1");
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/updateContextParameters")
            .content(mapToJson(List.of(param)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        ArgumentCaptor<List<ContextParameterInstance>> captor = ArgumentCaptor.forClass(List.class);
        verify(contextMachine).updateContextParameters(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("param1", captor.getValue().get(0).getName());
        assertEquals("value1", captor.getValue().get(0).getValue());
    }

    // -------------------------------------------------------------------------
    // setDryRunParameters
    // -------------------------------------------------------------------------

    @Test
    public void test_setDryRunParameters_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/dryRunParameters")
            .content(mapToJson(new DryRunParametersImpl()))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).setDryRunParameters(any());
    }

    // -------------------------------------------------------------------------
    // broadcastGlobalEvents
    // -------------------------------------------------------------------------

    @Test
    public void test_broadcastGlobalEvents_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        BroadcastGlobalEventsDto dto = new BroadcastGlobalEventsDto();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/broadcastGlobalEvents")
            .content(mapToJson(dto))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).broadcastGlobalEvents(any(), eq(false), eq(false));
    }

    @Test
    public void test_broadcastGlobalEvents_passes_ignore_and_force_flags_to_context_machine() throws Exception {
        putInCache();
        BroadcastGlobalEventsDto dto = new BroadcastGlobalEventsDto(
            "agent", "job", "ctx", CONTEXT_INSTANCE_ID, List.of(CHILD_CONTEXT_NAME), true, true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/broadcastGlobalEvents")
            .content(mapToJson(dto))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        ArgumentCaptor<SchedulerJobInitiationEvent> captor = ArgumentCaptor.forClass(SchedulerJobInitiationEvent.class);
        verify(contextMachine).broadcastGlobalEvents(captor.capture(), eq(true), eq(true));
        assertSchedulerJobInitiationEvent(captor.getValue());
    }

    // -------------------------------------------------------------------------
    // publishJobInitiationEvent
    // -------------------------------------------------------------------------

    @Test
    public void test_publishJobInitiationEvent_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/publishJobInitiationEvent")
            .content(mapToJson(new BroadcastLocalEventDto(
                "agent", "job", "ctx", CONTEXT_INSTANCE_ID, List.of(CHILD_CONTEXT_NAME))))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        ArgumentCaptor<SchedulerJobInitiationEvent> captor = ArgumentCaptor.forClass(SchedulerJobInitiationEvent.class);
        verify(contextMachine).publishJobInitiationEvent(captor.capture());
        assertSchedulerJobInitiationEvent(captor.getValue());
    }

    // -------------------------------------------------------------------------
    // addQueuedInitiationEvent
    // -------------------------------------------------------------------------

    @Test
    public void test_addQueuedInitiationEvent_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/addQueuedInitiationEvent")
            .content(mapToJson(new BroadcastLocalEventDto(
                "agent", "job", "ctx", CONTEXT_INSTANCE_ID, List.of(CHILD_CONTEXT_NAME))))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        ArgumentCaptor<SchedulerJobInitiationEvent> captor = ArgumentCaptor.forClass(SchedulerJobInitiationEvent.class);
        verify(contextMachine).addQueuedSchedulerJobInitiationEvent(captor.capture());
        assertSchedulerJobInitiationEvent(captor.getValue());
    }

    // -------------------------------------------------------------------------
    // resubmitDlq
    // -------------------------------------------------------------------------

    @Test
    public void test_resubmitDlq_returns_200_when_message_found() throws Exception {
        putInCache();
        when(contextMachine.resubmitMessageFromDeadLetterQueue(MESSAGE_ID)).thenReturn(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/resubmitDlq")
            .content(mapToJson(new MessageIdDto(MESSAGE_ID)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_resubmitDlq_returns_404_when_message_not_found() throws Exception {
        putInCache();
        when(contextMachine.resubmitMessageFromDeadLetterQueue(MESSAGE_ID)).thenReturn(false);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/resubmitDlq")
            .content(mapToJson(new MessageIdDto(MESSAGE_ID)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_resubmitDlq_returns_500_when_context_machine_throws() throws Exception {
        putInCache();
        when(contextMachine.resubmitMessageFromDeadLetterQueue(MESSAGE_ID))
            .thenThrow(new RuntimeException("simulated failure"));
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/resubmitDlq")
            .content(mapToJson(new MessageIdDto(MESSAGE_ID)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
    }

    // -------------------------------------------------------------------------
    // acknowledgeError
    // -------------------------------------------------------------------------

    @Test
    public void test_acknowledgeError_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/acknowledgeError")
            .content(mapToJson(new AcknowledgeJobDto(JOB_IDENTIFIER, true, CHILD_CONTEXT_NAME, List.of(CHILD_CONTEXT_NAME))))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        ArgumentCaptor<InternalEventDrivenJobInstance> captor = ArgumentCaptor.forClass(InternalEventDrivenJobInstance.class);
        verify(contextMachine).acknowledgeSchedulerJobError(captor.capture());
        assertEquals(JOB_IDENTIFIER, captor.getValue().getIdentifier());
        assertTrue(captor.getValue().isTargetResidingContextOnly());
        assertEquals(CHILD_CONTEXT_NAME, captor.getValue().getChildContextName());
        assertEquals(List.of(CHILD_CONTEXT_NAME), captor.getValue().getChildContextNames());
    }

    // -------------------------------------------------------------------------
    // broadcastLocalEvent
    // -------------------------------------------------------------------------

    @Test
    public void test_broadcastLocalEvent_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/broadcastLocalEvent")
            .content(mapToJson(new BroadcastLocalEventDto(
                "agent", "job", "ctx", CONTEXT_INSTANCE_ID, List.of(CHILD_CONTEXT_NAME))))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        ArgumentCaptor<SchedulerJobInitiationEvent> captor = ArgumentCaptor.forClass(SchedulerJobInitiationEvent.class);
        verify(contextMachine).broadcastLocalEvent(captor.capture());
        assertSchedulerJobInitiationEvent(captor.getValue());
    }

    // -------------------------------------------------------------------------
    // holdJob
    // -------------------------------------------------------------------------

    @Test
    public void test_holdJob_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/hold")
            .content(mapToJson(new ContextMachineJobActionDto(JOB_IDENTIFIER, CHILD_CONTEXT_NAME)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).holdJob(JOB_IDENTIFIER, CHILD_CONTEXT_NAME);
    }

    // -------------------------------------------------------------------------
    // releaseJob
    // -------------------------------------------------------------------------

    @Test
    public void test_releaseJob_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/release")
            .content(mapToJson(new ContextMachineJobActionDto(JOB_IDENTIFIER, CHILD_CONTEXT_NAME)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).releaseJob(JOB_IDENTIFIER, CHILD_CONTEXT_NAME);
    }

    // -------------------------------------------------------------------------
    // resetJob
    // -------------------------------------------------------------------------

    @Test
    public void test_resetJob_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/reset")
            .content(mapToJson(new ContextMachineJobActionDto(JOB_IDENTIFIER, CHILD_CONTEXT_NAME)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).resetJob(JOB_IDENTIFIER, CHILD_CONTEXT_NAME);
    }

    // -------------------------------------------------------------------------
    // skipJob
    // -------------------------------------------------------------------------

    @Test
    public void test_skipJob_returns_200_and_delegates_to_context_machine() throws Exception {
        putInCache();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(BASE + "/skip")
            .content(mapToJson(new ContextMachineJobActionDto(JOB_IDENTIFIER, CHILD_CONTEXT_NAME, true)))
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).skipJob(JOB_IDENTIFIER, CHILD_CONTEXT_NAME, true);
    }

    // -------------------------------------------------------------------------
    // getDlqMessages (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_getDlqMessages_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        when(contextMachine.getDlqMessages()).thenReturn(List.of());
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE + "/dlqMessages")).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).getDlqMessages();
    }

    // -------------------------------------------------------------------------
    // deleteDlqMessage (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_deleteDlqMessage_returns_200_when_message_deleted() throws Exception {
        putInCache();
        setLeader(true);
        when(contextMachine.deleteDlqMessage(MESSAGE_ID)).thenReturn(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.delete(
            BASE + "/dlqMessages/" + MESSAGE_ID)).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void test_deleteDlqMessage_returns_404_when_message_not_found() throws Exception {
        putInCache();
        setLeader(true);
        when(contextMachine.deleteDlqMessage(MESSAGE_ID)).thenReturn(false);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.delete(
            BASE + "/dlqMessages/" + MESSAGE_ID)).andReturn();
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    // -------------------------------------------------------------------------
    // deleteAllDlqMessages (leader-gated)
    // -------------------------------------------------------------------------

    @Test
    public void test_deleteAllDlqMessages_returns_200_when_leader() throws Exception {
        putInCache();
        setLeader(true);
        MvcResult result = mvc.perform(MockMvcRequestBuilders.delete(BASE + "/dlqMessages")).andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        verify(contextMachine).deleteAllDlqMessages();
    }
}
