package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SchedulerJobLogFileViewerDialog extends AbstractCloseableResizableDialog  {

    private Logger logger = LoggerFactory.getLogger(SchedulerJobLogFileViewerDialog.class);

    private VerticalLayout layout;

    private AceEditor aceEditor;

    private boolean initialised = false;

    public SchedulerJobLogFileViewerDialog() {
        this.setHeight("90%");
        this.setWidth("90%");

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
        init();
    }

    private void init() {
        if(!initialised) {
            this.initialiseEditor();
            this.layout.add(aceEditor);

            this.aceEditor.setValue("this the value");
        }
    }

    protected void initialiseEditor()
    {
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
}
