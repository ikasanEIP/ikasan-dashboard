package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.ikasan.dashboard.ui.UITest;
import org.ikasan.spec.module.client.LogStreamingService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;

public class SchedulerJobLogFileViewerDialogTest extends UITest {

    @MockBean
    private LogStreamingService logStreamingService;

    private final String host = "host";
    private final String endPoint = "/rest/logs";
    private final String logFile = "/path/to/log/file";

    private SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog;

    @Before
    public void setUp() {
        schedulerJobLogFileViewerDialog = new SchedulerJobLogFileViewerDialog(logStreamingService, host, endPoint, logFile);
    }

    @Test
    public void shouldCreateDialogue() {
        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "logStreamingService"));
        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "layout"));
        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "executorService"));
        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "logStreamer"));

        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "aceEditor"));

        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "host"));
        assertEquals(host, ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "host"));

        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "endPoint"));
        assertEquals(endPoint, ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "endPoint"));

        assertNotNull(ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "logFile"));
        assertEquals(logFile, ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "logFile"));

        ExecutorService executorService = (ExecutorService) ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "executorService");
        assertFalse(executorService.isShutdown());

        VerticalLayout layout = (VerticalLayout) ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "layout");
        assertEquals("100%", layout.getWidth());
        assertEquals("100%", layout.getHeight());

        AceEditor aceEditor = (AceEditor) ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "aceEditor");
        assertEquals(AceTheme.dracula, aceEditor.getTheme());
        assertEquals(AceMode.text, aceEditor.getMode());
        assertEquals(11, aceEditor.getFontSize());
        assertEquals(4, aceEditor.getTabSize());
        assertEquals("100%", aceEditor.getWidth());
        assertEquals("80vh", aceEditor.getHeight());
        assertTrue(aceEditor.isReadOnly());
        assertFalse(aceEditor.isWrap());
    }

    @Test
    public void shouldShutdownExecutorServiceOnDetach() {
        ExecutorService executorService = (ExecutorService) ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "executorService");
        assertFalse(executorService.isShutdown());

        schedulerJobLogFileViewerDialog.onDetach(new DetachEvent(schedulerJobLogFileViewerDialog));

        executorService = (ExecutorService) ReflectionTestUtils.getField(schedulerJobLogFileViewerDialog, "executorService");
        assertTrue(executorService.isShutdown());
    }

    @Override
    public void setup_expectations() throws IOException {

    }
}