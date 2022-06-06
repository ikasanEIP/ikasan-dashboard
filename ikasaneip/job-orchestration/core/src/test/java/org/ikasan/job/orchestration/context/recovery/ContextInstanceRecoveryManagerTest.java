package org.ikasan.job.orchestration.context.recovery;

import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRecoveryManagerTest {
    @Mock
    private ContextInstanceRecoveryService contextInstanceRecoveryService;

    private ContextInstanceRecoveryManager contextInstanceRecoveryManager;

    @Before
    public void setUp() {
        contextInstanceRecoveryManager = new ContextInstanceRecoveryManager(contextInstanceRecoveryService, true);
    }

    @Test
    public void should_do_nothing_if_featured_flagged_off() {
        ReflectionTestUtils.setField(contextInstanceRecoveryManager, "isContextLifeCycleActive", false);

        contextInstanceRecoveryManager.recoverContextInstances();

        verifyNoMoreInteractions(contextInstanceRecoveryService);
    }

    @Test
    public void recovers_instances() {
        ReflectionTestUtils.setField(contextInstanceRecoveryManager, "isContextLifeCycleActive", true);

        contextInstanceRecoveryManager.recoverContextInstances();

        verify(contextInstanceRecoveryService).recoverInstances();

        verifyNoMoreInteractions(contextInstanceRecoveryService);
    }

    @Test
    public void recovers_instances_should_catch_exceptions() {
        ReflectionTestUtils.setField(contextInstanceRecoveryManager, "isContextLifeCycleActive", true);

        doThrow(new RuntimeException("test exception")).when(contextInstanceRecoveryService).recoverInstances();

        contextInstanceRecoveryManager.recoverContextInstances();

        verify(contextInstanceRecoveryService).recoverInstances();

        verifyNoMoreInteractions(contextInstanceRecoveryService);
    }
}
