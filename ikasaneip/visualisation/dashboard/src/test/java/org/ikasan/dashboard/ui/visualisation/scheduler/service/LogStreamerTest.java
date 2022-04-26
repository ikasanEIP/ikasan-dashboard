package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.ikasan.spec.module.client.LogStreamingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LogStreamerTest {

    @Mock
    private LogStreamingService logStreamingService;

    @Mock
    private Consumer logConsumer;

    @Mock
    private Consumer errorConsumer;

    @Mock
    private Runnable completedRunner;

    private final String host = "host";
    private final String endPoint = "/rest/logs";
    private final String logFile = "/path/to/log/file";

    private LogStreamer logStreamer;

    @Before
    public void setUp() {
        logStreamer = new LogStreamer(logStreamingService, logConsumer, errorConsumer, completedRunner, host, endPoint, logFile);
    }

    @Test
    public void shouldCallLogStreamer() throws InterruptedException {
        logStreamer.stream();

        verify(logStreamingService).streamLogFile(eq(host), eq(endPoint), eq(logFile), eq(logConsumer), eq(errorConsumer), eq(completedRunner));
    }

    @Test
    public void shouldThrowRunTimeExceptionIfStreamLogFileErrors() throws InterruptedException {
        doThrow(new InterruptedException("some interrupt"))
            .when(logStreamingService).streamLogFile(eq(host), eq(endPoint), eq(logFile), eq(logConsumer), eq(errorConsumer), eq(completedRunner));

        try {
            logStreamer.stream();
            fail("Should not get here");
        } catch (Exception e) {
            assertEquals("some interrupt", e.getMessage());
        }

        verify(logStreamingService).streamLogFile(eq(host), eq(endPoint), eq(logFile), eq(logConsumer), eq(errorConsumer), eq(completedRunner));
    }
}