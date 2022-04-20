package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.LogStreamer;
import org.ikasan.spec.module.client.LogStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;

public class SchedulerJobLogFileViewerDialog extends AbstractCloseableResizableDialog {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerJobLogFileViewerDialog.class);
    private static final int SLEEP_MILLIS = 200;

    private final AtomicInteger streamCounter = new AtomicInteger(0);
    private final VerticalLayout layout;
    private final LogStreamingService logStreamingService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private AceEditor aceEditor;
    private LogStreamer logStreamer;
    private UI ui;
    private String host;
    private String endPoint;
    private String logFile;

    public SchedulerJobLogFileViewerDialog(LogStreamingService logStreamingService,
                                           String host,
                                           String endPoint,
                                           String logFile) {
        this.setHeight("80%");
        this.setWidth("80%");

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
        this.logStreamingService = logStreamingService;
        this.host = host;
        this.endPoint = endPoint;
        this.logFile = logFile;
        init();
    }

    private void init() {
        ui = UI.getCurrent();
        this.initialiseEditor();
        this.layout.add(aceEditor);
        this.logStreamer = new LogStreamer(logStreamingService, new LogConsumer(), new ErrorConsumer(), new CompletedRunner(), host, endPoint, logFile);
        streamLog();
    }

    private void initialiseEditor() {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.text);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setWidth("100%");
        aceEditor.setHeight("80vh");
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
        streamCounter.set(0);
    }

    private void streamLog() {
        Runnable runnable = () -> this.logStreamer.stream();
        executorService.submit(runnable);
    }

    private class LogConsumer implements Consumer<ServerSentEvent<String>> {
        private final StringBuffer buffer = new StringBuffer();
        private static final int BUFFER_SIZE = 500;
        private int count = 0;
        private ScheduledExecutorService executor;

        @Override
        public void accept(ServerSentEvent<String> event) {
            if (count == BUFFER_SIZE) {
                stopThread();
                addDataStringToBuffer(event);
                setCursorAndDisplay();
                // + 1 for the event just appended
                int pos = streamCounter.get() + BUFFER_SIZE + 1;
                setCursorAndReset(pos);
                count = 0;
            } else {
                stopThread();
                addDataStringToBuffer(event);
                count++;
                startThread();
            }
        }

        private void stopThread() {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }

        private void startThread() {
            executor = Executors.newSingleThreadScheduledExecutor();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    flush();
                }

                private void flush() {
                    setCursorAndDisplay();
                    int pos = streamCounter.get() + count;
                    setCursorAndReset(pos);
                    count = 0;
                    this.cancel();
                }
            };

            executor.schedule(task, 500, TimeUnit.MILLISECONDS);
        }

        private void setCursorAndReset(int pos) {
            streamCounter.set(pos);
            aceEditor.setCursorPosition(pos, 0);
            buffer.delete(0, buffer.length());
        }

        private void setCursorAndDisplay() {
            aceEditor.focus();
            aceEditor.setCursorPosition(streamCounter.get(), 0);
            ui.access(() -> aceEditor.addTextAtCurrentPosition(buffer.toString()));
            sleepToGivEditorChanceToRender();
        }

        private void addDataStringToBuffer(ServerSentEvent<String> event) {
            buffer.append(event.data() == null ? "" : event.data()).append(System.getProperty("line.separator"));
        }
    }

    private void sleepToGivEditorChanceToRender() {
        try {
            Thread.sleep(SLEEP_MILLIS);
        } catch (InterruptedException e) {
            try {
                Thread.sleep(SLEEP_MILLIS);
            } catch (InterruptedException ex) {
                LOG.error("Could not pause, error: " + e.getMessage());
            }
        }
    }

    private class ErrorConsumer implements Consumer<Throwable> {
        @Override
        public void accept(Throwable t) {
            aceEditor.setCursorPosition(streamCounter.get(), 0);
            ui.access(() -> aceEditor.addTextAtCurrentPosition("Streaming error..." + System.getProperty("line.separator")));
            aceEditor.setCursorPosition(streamCounter.incrementAndGet(), 0);
            aceEditor.focus();
            ui.access(() -> aceEditor.addTextAtCurrentPosition(t.getLocalizedMessage()));
            aceEditor.setCursorPosition(streamCounter.incrementAndGet(), 0);
            aceEditor.focus();
            LOG.error("Got error streaming: " + t.getMessage());
            sleepToGivEditorChanceToRender();

            //todo log err - rethrow and catch
            // think about how display error NotificationHelper
        }
    }

    private class CompletedRunner implements Runnable {
        @Override
        public void run() {
            aceEditor.setCursorPosition(streamCounter.get(), 0);
            try {
                ui.access(() -> aceEditor.addTextAtCurrentPosition("Log stream terminated." + System.getProperty("line.separator")));
                aceEditor.setCursorPosition(streamCounter.incrementAndGet(), 0);
                aceEditor.focus();
            } catch (UIDetachedException e) {
                // do nothing for UI detached exceptions
            }
            sleepToGivEditorChanceToRender();
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        executorService.shutdownNow();
    }
}
