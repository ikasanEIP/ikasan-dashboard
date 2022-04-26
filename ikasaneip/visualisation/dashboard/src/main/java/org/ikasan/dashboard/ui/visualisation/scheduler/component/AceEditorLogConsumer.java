package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;

import de.f0rce.ace.AceEditor;

public class AceEditorLogConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AceEditorLogConsumer.class);

    private static final int BUFFER_SIZE_5000 = 5000;

    private static final int RENDER_SLEEP_750 = 750;
    private static final int RENDER_SLEEP_500 = 500;
    private static final int RENDER_SLEEP_100 = 100;

    private final StringBuffer buffer = new StringBuffer();
    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicInteger streamCounter = new AtomicInteger(0);
    private final AceEditor aceEditor;
    private final UI ui;

    private ScheduledExecutorService executor;

    public AceEditorLogConsumer(AceEditor aceEditor, UI ui) {
        this.aceEditor = aceEditor;
        this.ui = ui;
    }

    public void logConsumer(ServerSentEvent<String> event) {
        if (count.get() == BUFFER_SIZE_5000) {
            stopThread();
            addDataStringToBuffer(event);
            setCursorAndDisplay();
            // + 1 for the event just appended
            int pos = streamCounter.get() + BUFFER_SIZE_5000 + 1;
            setCursorAndReset(pos);
            count.set(0);
            sleepToGivEditorChanceToRender(RENDER_SLEEP_750);
        } else {
            stopThread();
            addDataStringToBuffer(event);
            count.incrementAndGet();
            startThread();
        }
    }

    private void stopThread() {
        if (executor != null) {
            executor.shutdownNow();
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
                int pos = streamCounter.get() + count.get();
                setCursorAndReset(pos);
                count.set(0);
                sleepToGivEditorChanceToRender(RENDER_SLEEP_500);
                stopThread();
                this.cancel();
            }
        };

        executor.schedule(task, RENDER_SLEEP_750, TimeUnit.MILLISECONDS);
    }

    public void errorConsumer(Throwable error) {
        aceEditor.setCursorPosition(streamCounter.get(), 0);
        ui.access(() -> aceEditor.addTextAtPosition(streamCounter.get(), 0, "Streaming error..." + System.getProperty("line.separator")));
        sleepToGivEditorChanceToRender(RENDER_SLEEP_100);
        aceEditor.setCursorPosition(streamCounter.incrementAndGet(), 0);
        ui.access(() -> aceEditor.addTextAtPosition(streamCounter.get(), 0, error.getLocalizedMessage()));
        sleepToGivEditorChanceToRender(RENDER_SLEEP_100);
        aceEditor.setCursorPosition(streamCounter.incrementAndGet(), 0);
        LOG.error("Got error streaming: " + error.getMessage());

        // todo think about how display error NotificationHelper
    }

    public void completedConsumer() {
        aceEditor.setCursorPosition(streamCounter.get(), 0);
        try {
            ui.access(() -> aceEditor.addTextAtPosition(streamCounter.get(), 0, "Log stream terminated." + System.getProperty("line.separator")));
            sleepToGivEditorChanceToRender(RENDER_SLEEP_100);
            aceEditor.setCursorPosition(streamCounter.incrementAndGet(), 0);
        } catch (UIDetachedException e) {
            // do nothing for UI detached exceptions
        }
    }

    private void setCursorAndReset(int pos) {
        streamCounter.set(pos);
        aceEditor.setCursorPosition(pos, 0);
        buffer.delete(0, buffer.length());
    }

    private void setCursorAndDisplay() {
        aceEditor.setCursorPosition(streamCounter.get(), 0);
        ui.access(() -> aceEditor.addTextAtPosition(streamCounter.get(), 0, buffer.toString()));
    }

    private void addDataStringToBuffer(ServerSentEvent<String> event) {
        buffer.append(event.data() == null ? "" : event.data()).append(System.getProperty("line.separator"));
    }

    private void sleepToGivEditorChanceToRender(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                LOG.error("Could not pause, error: " + e.getMessage());
            }
        }
    }
}
