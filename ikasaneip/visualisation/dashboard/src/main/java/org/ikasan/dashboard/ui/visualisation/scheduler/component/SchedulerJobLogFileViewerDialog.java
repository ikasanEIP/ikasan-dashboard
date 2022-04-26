package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.LogStreamer;
import org.ikasan.spec.module.client.LogStreamingService;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;

public class SchedulerJobLogFileViewerDialog extends AbstractCloseableResizableDialog {

    private final VerticalLayout layout;
    private final LogStreamingService logStreamingService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private AceEditor aceEditor;
    private LogStreamer logStreamer;
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
        this.initialiseEditor();
        this.layout.add(aceEditor);
        AceEditorLogConsumer aceEditorLogConsumer = new AceEditorLogConsumer(aceEditor, UI.getCurrent());
        this.logStreamer = new LogStreamer(
            logStreamingService,
            aceEditorLogConsumer::logConsumer,
            aceEditorLogConsumer::errorConsumer,
            aceEditorLogConsumer::completedConsumer,
            host, endPoint, logFile);

        executorService.submit(() -> this.logStreamer.stream());
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
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        executorService.shutdownNow();
    }

    public String getHost() {
        return host;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getLogFile() {
        return logFile;
    }

    public LogStreamingService getLogStreamingService() {
        return logStreamingService;
    }
}
