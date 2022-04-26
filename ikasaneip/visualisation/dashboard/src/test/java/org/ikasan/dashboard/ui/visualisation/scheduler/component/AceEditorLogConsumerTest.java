package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;

import com.vaadin.flow.component.UI;

import de.f0rce.ace.AceEditor;

@RunWith(MockitoJUnitRunner.class)
public class AceEditorLogConsumerTest {

    @Mock
    private AceEditor aceEditor;

    @Mock
    private UI ui;

    private AceEditorLogConsumer aceEditorLogConsumer;

    @Before
    public void setUp() {
        aceEditorLogConsumer = new AceEditorLogConsumer(aceEditor, ui);
    }

    @Test
    public void shouldLogToScreenEvery5000() {
        for (int i = 0; i < 5001; i++) {
            String event = "log message " + i;
            ServerSentEvent<String> data = ServerSentEvent.builder(event).build();
            aceEditorLogConsumer.logConsumer(data);
            if (i != 0 && i != 5000 && i % 1000 == 0) {
                ScheduledExecutorService executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
                assertNotNull(executor);
                assertFalse(executor.isTerminated());
                StringBuffer buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
                assertNotNull(buffer);
                assertEquals(buildExpectedBuffer(i + 1).toString(), buffer.toString());
                AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
                assertNotNull(count);
                assertEquals(i + 1, count.get());
            }
        }

        verify(aceEditor).setCursorPosition(0, 0);
        verify(aceEditor).setCursorPosition(5001, 0);
        verify(ui).access(any());
        reset(aceEditor, ui);

        ScheduledExecutorService executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
        assertNotNull(executor);
        assertTrue(executor.isTerminated());

        StringBuffer buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
        assertNotNull(buffer);
        assertEquals("", buffer.toString());

        AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
        assertNotNull(count);
        assertEquals(0, count.get());

        AtomicInteger streamCounter = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "streamCounter");
        assertNotNull(streamCounter);
        assertEquals(5001, streamCounter.get());

        for (int i = 0; i < 5001; i++) {
            String event = "log message " + i;
            ServerSentEvent<String> data = ServerSentEvent.builder(event).build();
            aceEditorLogConsumer.logConsumer(data);
            if (i != 0 && i != 5000 && i % 1000 == 0) {
                executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
                assertNotNull(executor);
                assertFalse(executor.isTerminated());
                buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
                assertNotNull(buffer);
                assertEquals(buildExpectedBuffer(i + 1).toString(), buffer.toString());

                count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
                assertNotNull(count);
                assertEquals(i + 1, count.get());
            }
        }

        executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
        assertNotNull(executor);
        assertTrue(executor.isTerminated());

        count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
        assertNotNull(count);
        assertEquals(0, count.get());

        buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
        assertNotNull(buffer);
        assertEquals("", buffer.toString());

        streamCounter = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "streamCounter");
        assertNotNull(streamCounter);
        assertEquals(10002, streamCounter.get());

        verify(aceEditor).setCursorPosition(5001, 0);
        verify(aceEditor).setCursorPosition(10002, 0);
        verify(ui).access(any());
        reset(aceEditor, ui);

        verifyNoInteractions(aceEditor, ui);
    }

    @Test
    public void shouldLogToScreenIfLessThan5000() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            String event = "log message " + i;
            ServerSentEvent<String> data = ServerSentEvent.builder(event).build();
            aceEditorLogConsumer.logConsumer(data);
            ScheduledExecutorService executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
            assertNotNull(executor);
            assertFalse(executor.isTerminated());
            if (i == 9) {
                StringBuffer buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
                assertNotNull(buffer);
                assertEquals(buildExpectedBuffer(10).toString(), buffer.toString());
                AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
                assertNotNull(count);
                assertEquals(10, count.get());
            }
        }

        Thread.sleep(1000);

        verify(aceEditor).setCursorPosition(0, 0);
        verify(aceEditor).setCursorPosition(10, 0);
        verify(ui).access(any());

        StringBuffer buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
        assertNotNull(buffer);
        assertEquals("", buffer.toString());

        AtomicInteger streamCounter = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "streamCounter");
        assertNotNull(streamCounter);
        assertEquals(10, streamCounter.get());

        AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
        assertNotNull(count);
        assertEquals(0, count.get());

        Thread.sleep(1000);

        ScheduledExecutorService executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
        assertNotNull(executor);
        assertTrue(executor.isTerminated());

        verifyNoMoreInteractions(aceEditor, ui);
    }

    @Test
    public void shouldLogEmptyStringIfDataIsNull() throws InterruptedException {
        for (int i = 0; i < 1; i++) {
            String event = null;
            ServerSentEvent<String> data = ServerSentEvent.builder(event).build();
            aceEditorLogConsumer.logConsumer(data);
            ScheduledExecutorService executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
            assertNotNull(executor);
            assertFalse(executor.isTerminated());
            StringBuffer buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
            assertNotNull(buffer);
            assertEquals("" + System.getProperty("line.separator"), buffer.toString());
            AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
            assertNotNull(count);
            assertEquals(1, count.get());
        }

        Thread.sleep(1000);

        verify(aceEditor).setCursorPosition(0, 0);
        verify(aceEditor).setCursorPosition(1, 0);
        verify(ui).access(any());

        StringBuffer buffer = (StringBuffer) ReflectionTestUtils.getField(aceEditorLogConsumer, "buffer");
        assertNotNull(buffer);
        assertEquals("", buffer.toString());

        AtomicInteger streamCounter = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "streamCounter");
        assertNotNull(streamCounter);
        assertEquals(1, streamCounter.get());

        AtomicInteger count = (AtomicInteger) ReflectionTestUtils.getField(aceEditorLogConsumer, "count");
        assertNotNull(count);
        assertEquals(0, count.get());

        Thread.sleep(1000);

        ScheduledExecutorService executor = (ScheduledExecutorService) ReflectionTestUtils.getField(aceEditorLogConsumer, "executor");
        assertNotNull(executor);
        assertTrue(executor.isTerminated());

        verifyNoMoreInteractions(aceEditor, ui);
    }

    @Test
    public void shouldLogCompletedConsumer() throws InterruptedException {
        aceEditorLogConsumer.completedConsumer();
        Thread.sleep(500);

        verify(aceEditor).setCursorPosition(0, 0);
        verify(aceEditor).setCursorPosition(1, 0);
        verify(ui).access(any());
    }

    @Test
    public void shouldLogErrorConsumer() throws InterruptedException {
        aceEditorLogConsumer.errorConsumer(new RuntimeException("error"));
        Thread.sleep(500);

        verify(aceEditor).setCursorPosition(0, 0);
        verify(aceEditor).setCursorPosition(1, 0);
        verify(aceEditor).setCursorPosition(0, 0);
        verify(aceEditor).setCursorPosition(1, 0);
        verify(ui, times(2)).access(any());
    }

    private StringBuffer buildExpectedBuffer(int count) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < count; i++) {
            buffer.append("log message ").append(i);
            buffer.append(System.getProperty("line.separator"));
        }
        return buffer;
    }
}