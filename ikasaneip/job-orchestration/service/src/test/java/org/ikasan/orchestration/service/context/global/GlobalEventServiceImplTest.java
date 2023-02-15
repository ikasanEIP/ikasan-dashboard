package org.ikasan.orchestration.service.context.global;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.GlobalEventJobInstanceImpl;
import org.ikasan.orchestration.service.utils.TestUtils;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GlobalEventServiceImplTest {

    @Mock
    ContextMachine contextMachine1;
    @Mock
    ContextInstance context1;

    @Mock
    ContextMachine contextMachine2;
    @Mock
    ContextInstance context2;

    @Mock
    ContextMachine contextMachine3;
    @Mock
    ContextInstance context3;

    @Mock
    ContextMachine contextMachine4;
    @Mock
    ContextInstance context4;

    @Mock
    ContextMachine contextMachine5;
    @Mock
    ContextInstance context5;

    GlobalEventServiceImpl globalEventService;

    @Before
    public void setUp() {
        TestUtils.resetContextMachineCache();
        JobLockCacheImpl.instance().reset();

        when(contextMachine1.getContext()).thenReturn(context1);
        when(context1.getName()).thenReturn("context1");
        when(context1.getId()).thenReturn("context1");

        when(contextMachine2.getContext()).thenReturn(context2);
        when(context2.getName()).thenReturn("context2");
        when(context2.getId()).thenReturn("context2");

        when(contextMachine3.getContext()).thenReturn(context3);
        when(context3.getName()).thenReturn("context3");
        when(context3.getId()).thenReturn("context3");

        when(contextMachine4.getContext()).thenReturn(context4);
        when(context4.getName()).thenReturn("context4");
        when(context4.getId()).thenReturn("context4");

        when(contextMachine5.getContext()).thenReturn(context5);
        when(context5.getName()).thenReturn("context5");
        when(context5.getId()).thenReturn("context5");

        ContextMachineCache.instance().put(contextMachine1);
        ContextMachineCache.instance().put(contextMachine2);
        ContextMachineCache.instance().put(contextMachine3);
        ContextMachineCache.instance().put(contextMachine4);
        ContextMachineCache.instance().put(contextMachine5);

        globalEventService = new GlobalEventServiceImpl();
    }

    @Test(expected = GlobalEventServiceException.class)
    public void test_exception_bad_context_id() {
        globalEventService.raiseGlobalEventJob(new GlobalEventJobInstanceImpl(), "bad-context-id");
    }

    @Test(expected = GlobalEventServiceException.class)
    public void test_exception_io_exception_broadcast() throws IOException {
        doThrow(new IOException("test")).when(contextMachine1).broadcastGlobalEvents(any(), eq(false), eq(false));
        globalEventService.raiseGlobalEventJob(new GlobalEventJobInstanceImpl(), "context1");
    }

    @Test
    public void test_broadcast_success() throws IOException {
        globalEventService.raiseGlobalEventJob(new GlobalEventJobInstanceImpl(), "context1");

        verify(contextMachine1).broadcastGlobalEvents(any(), eq(false), eq(false));
    }

    @Test
    public void test_broadcast_job_name_success() throws IOException {

        // Remove some context machine so when mocking we know the exact context machine to use in this mock
        ContextMachineCache.instance().remove(contextMachine1);
        ContextMachineCache.instance().remove(contextMachine2);
        ContextMachineCache.instance().remove(contextMachine3);
        ContextMachineCache.instance().remove(contextMachine4);

        globalEventService.raiseGlobalEventJob("some_event");

        verify(contextMachine5).broadcastGlobalEvents(any(), eq(true), eq(true));

    }

    @Test(expected = GlobalEventServiceException.class)
    public void test_broadcast_no_context_exist_success() throws IOException {

        // Remove all context machine from cache
        ContextMachineCache.instance().remove(contextMachine1);
        ContextMachineCache.instance().remove(contextMachine2);
        ContextMachineCache.instance().remove(contextMachine3);
        ContextMachineCache.instance().remove(contextMachine4);
        ContextMachineCache.instance().remove(contextMachine5);

        globalEventService.raiseGlobalEventJob("some_event");
    }

    @Test(expected = GlobalEventServiceException.class)
    public void test_broadcast_job_name_io_exception() throws IOException {

        doThrow(new IOException("test")).when(contextMachine3).broadcastGlobalEvents(any(), eq(true), eq(true));

        // Remove all context machine from cache
        ContextMachineCache.instance().remove(contextMachine1);
        ContextMachineCache.instance().remove(contextMachine2);
        ContextMachineCache.instance().remove(contextMachine4);
        ContextMachineCache.instance().remove(contextMachine5);

        globalEventService.raiseGlobalEventJob("some_event");
    }

}
