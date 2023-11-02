package org.ikasan.job.orchestration.integration.component.endpoint;

import org.ikasan.component.endpoint.bigqueue.message.BigQueueMessageImpl;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.ScheduleProcessInboundProducer;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.configuration.ScheduleProcessInboundProducerConfiguration;
import org.ikasan.job.orchestration.integration.inbound.exception.InvalidContextInstanceIdException;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.component.endpoint.EndpointException;
import org.ikasan.spec.error.reporting.ErrorReportingService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScheduleProcessInboundProducerTest {

    @Mock
    private ContextMachine contextMachine;

    @Mock
    private ContextInstance contextInstance;

    @Mock
    TransactionManager transactionManager;

    @Mock
    ErrorReportingService errorReportingService;

    @Mock
    ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    ContextInstancePublicationService contextInstancePublicationService;

    @Mock
    ModuleMetaDataService moduleMetadataService;

    @Mock
    Xid xid;

    @Test
    public void test_invoke_success() throws IOException, XAException {
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("contextInstanceName");
        when(contextInstance.getId()).thenReturn("contextInstanceId");
        when(contextInstance.getStatus()).thenReturn(InstanceStatus.RUNNING);


        ContextMachineCache.instance().put(contextMachine);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId("contextInstanceId");

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);


        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
        scheduleProcessInboundProducer.commit(xid, true);

        verify(contextInstance).getName();
        verify(contextInstance, times(3)).getId();
        verify(contextMachine, times(9)).getContext();
        verify(contextMachine).registerToNotificationMonitors();
        verify(contextMachine).eventReceived(anyString());
        verify(contextInstance, times(3)).getStatus();

        verifyNoMoreInteractions(contextInstance
            , contextMachine);
    }

    @Test
    public void test_invoke_success_prepared_instance() throws IOException, XAException {
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("contextInstanceName");
        when(contextInstance.getId()).thenReturn("contextInstanceId");
        when(contextInstance.getStatus()).thenReturn(InstanceStatus.PREPARED);


        ContextMachineCache.instance().put(contextMachine);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId("contextInstanceId");
        contextualisedScheduledProcessEvent.setContextName("contextName");

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);
        scheduleProcessInboundProducer.setErrorReportingService(this.errorReportingService);


        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
        scheduleProcessInboundProducer.commit(xid, true);

        verify(contextInstance, times(1)).getName();
        verify(contextInstance, times(1)).getId();
        verify(contextMachine, times(7)).getContext();
        verify(contextInstance, times(4)).getStatus();
        verify(this.errorReportingService).notify(any(String.class), any(String.class), any());

        verifyNoMoreInteractions(contextInstance
            , contextMachine
            , this.errorReportingService);
    }

    @Test
    public void test_invoke_success_with_logging() throws IOException, XAException {
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("contextInstanceName");
        when(contextInstance.getId()).thenReturn("contextInstanceId");
        when(contextInstance.getStatus()).thenReturn(InstanceStatus.RUNNING);


        ContextMachineCache.instance().put(contextMachine);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId("contextInstanceId");

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        configuration.setLogDetails(true);
        scheduleProcessInboundProducer.setConfiguration(configuration);
        scheduleProcessInboundProducer.setErrorReportingService(errorReportingService);


        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
        scheduleProcessInboundProducer.commit(xid, true);

        verify(contextInstance, times(2)).getName();
        verify(contextInstance, times(5)).getId();
        verify(contextMachine, times(12)).getContext();
        verify(contextInstance, times(3)).getStatus();
        verify(contextMachine).registerToNotificationMonitors();
        verify(contextMachine).eventReceived(anyString());

        verifyNoMoreInteractions(contextInstance
            , contextMachine);
    }

    @Test(expected = InvalidContextInstanceIdException.class)
    public void test_invoke_exception_context_instance_not_found_in_cache() throws IOException {
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("contextInstanceName");
        when(contextInstance.getId()).thenReturn("contextInstanceId");


        ContextMachineCache.instance().put(contextMachine);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId("badContextInstanceId");

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);
        scheduleProcessInboundProducer.setErrorReportingService(errorReportingService);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
    }

    @Test
    public void test_invoke_exception_context_instance_not_found_in_cache_and_ended_in_solr() throws IOException {

        // Setup scheduledContextInstanceService mock
        ContextInstance mockInstanceInSolr = new ContextInstanceImpl();
        mockInstanceInSolr.setStatus(InstanceStatus.ENDED);
        mockInstanceInSolr.setId("expiredContextInstanceId");
        mockInstanceInSolr.setName("contextInstanceName");
        SchedulerJobInstance mockJobInstance = new SchedulerJobInstanceImpl();
        mockJobInstance.setAgentName("agentName");
        mockInstanceInSolr.setScheduledJobs(Collections.singletonList(mockJobInstance));

        SearchResults<ScheduledContextInstanceRecord> contextInstanceRecords = new SearchResults<ScheduledContextInstanceRecord>() {
            @Override
            public List<ScheduledContextInstanceRecord> getResultList() {
                ScheduledContextInstanceRecord record = new ScheduledContextInstanceRecordImpl();
                record.setContextName("contextInstanceName");
                record.setContextInstanceId("expiredContextInstanceId");
                record.setContextInstance(mockInstanceInSolr);
                return Collections.singletonList(record);
            }
            @Override
            public long getTotalNumberOfResults() { return 1; }
            @Override
            public long getQueryResponseTime() { return 0; }
        };
        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull())).thenReturn(contextInstanceRecords);

        // Setup moduleMetadataService mock
        ModuleMetaData mockModuleMetaData = new ModuleMetaDataImpl();
        mockModuleMetaData.setName("agentName");
        mockModuleMetaData.setUrl("http://localhost/agentName/");
        ModuleMetadataSearchResults searchResults = new ModuleMetadataSearchResults(Collections.singletonList(mockModuleMetaData), 1, 1);
        when(moduleMetadataService.find(any(), eq(ModuleType.SCHEDULER_AGENT), eq(-1), eq(-1))).thenReturn(searchResults);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextName("contextInstanceName");
        contextualisedScheduledProcessEvent.setContextInstanceId("expiredContextInstanceId");

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);
        scheduleProcessInboundProducer.setErrorReportingService(errorReportingService);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));

        verify(errorReportingService, times(1)).notify(eq("Scheduled Process Event Inbound Flow"),
            eq(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage)),
            any());

        verify(scheduledContextInstanceService, times(1)).getScheduledContextInstancesByFilter(
            any(), eq(-1), eq(-1), isNull(), isNull());

        verify(moduleMetadataService, times(1)).find(
            any(), eq(ModuleType.SCHEDULER_AGENT), eq(-1), eq(-1));

        verify(contextInstancePublicationService, times(1)).remove(
            eq("http://localhost/agentName/"), any());

        verifyNoMoreInteractions(contextInstance, contextMachine, errorReportingService,
            scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
    }

    @Test
    public void test_invoke_exception_null_context_instance_id_in_scheduler_event() throws IOException {
        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId(null);

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);
        scheduleProcessInboundProducer.setErrorReportingService(errorReportingService);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));

        verify(errorReportingService, times(1)).notify(eq("Scheduled Process Event Inbound Flow"),
            eq(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage)),
            any());
        verifyNoMoreInteractions(contextInstance, contextMachine, errorReportingService);
    }

    @Test(expected = EndpointException.class)
    public void test_invoke_exception_bad_inbound_message() throws IOException {
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("contextInstanceName");
        when(contextInstance.getId()).thenReturn("contextInstanceId");


        ContextMachineCache.instance().put(contextMachine);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId("contextInstanceId");

        bigQueueMessage.setMessage("bad message");

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
    }

    @Test
    public void test_invoke_exception_bad_inbound_message_ignore_error() throws IOException {
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("contextInstanceName");
        when(contextInstance.getId()).thenReturn("contextInstanceId");


        ContextMachineCache.instance().put(contextMachine);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId("contextInstanceId");

        bigQueueMessage.setMessage("bad message");

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager, scheduledContextInstanceService, contextInstancePublicationService, moduleMetadataService);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(true);
        scheduleProcessInboundProducer.setConfiguration(configuration);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
    }
}
