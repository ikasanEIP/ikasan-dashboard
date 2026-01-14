package org.ikasan.rest.dashboard.util.service.bigqueue;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.rest.dashboard.service.bigqueue.BigQueueDashboardServiceImpl;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BigQueueDashboardServiceImplTest {

    @Mock
    private IBigQueue inboundQueue;

    @Test(expected = BigQueueNotFoundException.class)
    public void test_queue_not_found_null_queue_name_exception() throws BigQueueNotFoundException {
        BigQueueDashboardServiceImpl bigQueueDashboardService = new BigQueueDashboardServiceImpl(this.inboundQueue);

        bigQueueDashboardService.getBigQueue(null);
    }

    @Test(expected = BigQueueNotFoundException.class)
    public void test_queue_not_found_exception() throws BigQueueNotFoundException {
        BigQueueDashboardServiceImpl bigQueueDashboardService = new BigQueueDashboardServiceImpl(this.inboundQueue);

        bigQueueDashboardService.getBigQueue("queueName");
    }

    @Test
    public void test_get_inbound_big_queue_success() throws BigQueueNotFoundException {
        ContextMachine contextMachine = mock(ContextMachine.class);
        ContextInstance contextInstance = mock(ContextInstance.class);
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getId()).thenReturn(UUID.randomUUID().toString());
        when(contextInstance.getName()).thenReturn("contextName");
        when(contextInstance.getStatus()).thenReturn(InstanceStatus.WAITING);
        when(contextMachine.getInboundQueueName()).thenReturn("queueName");
        when(contextMachine.getInboundQueue()).thenReturn(this.inboundQueue);

        ContextMachineCache.instance().put(contextMachine);
        BigQueueDashboardServiceImpl bigQueueDashboardService = new BigQueueDashboardServiceImpl(this.inboundQueue);

        IBigQueue result = bigQueueDashboardService.getBigQueue("queueName");

        Assert.assertEquals(this.inboundQueue, result);

        verify(contextMachine, times(3)).getContext();
        verify(contextInstance).getId();
        verify(contextInstance).getName();
        verify(contextInstance).getStatus();
        verify(contextMachine).registerToNotificationMonitors();
        verify(contextMachine).getInboundQueueName();
        verify(contextMachine).getInboundQueue();

        verifyNoMoreInteractions(contextInstance, contextMachine);

        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void test_get_outbound_big_queue_success() throws BigQueueNotFoundException {
        ContextMachine contextMachine = mock(ContextMachine.class);
        ContextInstance contextInstance = mock(ContextInstance.class);
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getId()).thenReturn(UUID.randomUUID().toString());
        when(contextInstance.getName()).thenReturn("contextName");
        when(contextInstance.getStatus()).thenReturn(InstanceStatus.WAITING);
        when(contextMachine.getInboundQueueName()).thenReturn("some other name");
        when(contextMachine.getOutboundQueueName()).thenReturn("queueName");
        when(contextMachine.getOutboundQueue()).thenReturn(this.inboundQueue);

        ContextMachineCache.instance().put(contextMachine);
        BigQueueDashboardServiceImpl bigQueueDashboardService = new BigQueueDashboardServiceImpl(this.inboundQueue);

        IBigQueue result = bigQueueDashboardService.getBigQueue("queueName");

        Assert.assertEquals(this.inboundQueue, result);

        verify(contextMachine, times(3)).getContext();
        verify(contextInstance).getId();
        verify(contextInstance).getName();
        verify(contextInstance).getStatus();
        verify(contextMachine).registerToNotificationMonitors();
        verify(contextMachine).getInboundQueueName();
        verify(contextMachine).getOutboundQueueName();
        verify(contextMachine).getOutboundQueue();

        verifyNoMoreInteractions(contextInstance, contextMachine);

        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void test_get_dashboard_inbound_big_queue_success() throws BigQueueNotFoundException {
        ContextMachine contextMachine = mock(ContextMachine.class);
        ContextInstance contextInstance = mock(ContextInstance.class);
        when(contextMachine.getContext()).thenReturn(contextInstance);
        when(contextInstance.getId()).thenReturn(UUID.randomUUID().toString());
        when(contextInstance.getName()).thenReturn("contextName");
        when(contextInstance.getStatus()).thenReturn(InstanceStatus.WAITING);

        ContextMachineCache.instance().put(contextMachine);
        BigQueueDashboardServiceImpl bigQueueDashboardService = new BigQueueDashboardServiceImpl(this.inboundQueue);

        IBigQueue result = bigQueueDashboardService.getBigQueue(BigQueueDashboardServiceImpl.INBOUND_QUEUE);

        Assert.assertEquals(this.inboundQueue, result);

        verify(contextMachine, times(3)).getContext();
        verify(contextInstance).getId();
        verify(contextInstance).getName();
        verify(contextInstance).getStatus();
        verify(contextMachine).registerToNotificationMonitors();

        verifyNoMoreInteractions(contextInstance, contextMachine);

        ContextMachineCache.instance().resetAllCache();
    }
}
