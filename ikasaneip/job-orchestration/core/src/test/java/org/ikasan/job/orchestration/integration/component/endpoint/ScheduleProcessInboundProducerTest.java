package org.ikasan.job.orchestration.integration.component.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.component.endpoint.bigqueue.message.BigQueueMessageImpl;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.ScheduleProcessInboundProducer;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.configuration.ScheduleProcessInboundProducerConfiguration;
import org.ikasan.job.orchestration.integration.inbound.exception.InvalidContextInstanceIdException;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.component.endpoint.EndpointException;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import java.io.IOException;

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
    Xid xid;

    @Test
    public void test_invoke_success() throws IOException, XAException {
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getName()).thenReturn("contextInstanceName");
        when(contextInstance.getId()).thenReturn("contextInstanceId");


        ContextMachineCache.instance().put(contextMachine);

        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId("contextInstanceId");

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);


        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
        scheduleProcessInboundProducer.commit(xid, true);

        verify(contextInstance).getName();
        verify(contextInstance).getId();
        verify(contextMachine, times(2)).getContext();
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

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
    }

    @Test(expected = InvalidContextInstanceIdException.class)
    public void test_invoke_exception_null_context_instance_id_in_scheduler_event() throws IOException {
        BigQueueMessageImpl<String> bigQueueMessage = new BigQueueMessageImpl();
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setContextInstanceId(null);

        bigQueueMessage.setMessage(ObjectMapperFactory.newInstance().writeValueAsString(contextualisedScheduledProcessEvent));

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(false);
        scheduleProcessInboundProducer.setConfiguration(configuration);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
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

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager);
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

        ScheduleProcessInboundProducer scheduleProcessInboundProducer = new ScheduleProcessInboundProducer(transactionManager);
        ScheduleProcessInboundProducerConfiguration configuration = new ScheduleProcessInboundProducerConfiguration();
        configuration.setIgnoreErrors(true);
        scheduleProcessInboundProducer.setConfiguration(configuration);

        scheduleProcessInboundProducer.invoke(ObjectMapperFactory.newInstance().writeValueAsString(bigQueueMessage));
    }
}
