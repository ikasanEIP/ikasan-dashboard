package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextViewerDialog extends AbstractCloseableResizableDialog  {

    private Logger logger = LoggerFactory.getLogger(TextViewerDialog.class);

    private VerticalLayout layout;

    private AceEditor aceEditor;

    private boolean initialised = false;

    private ObjectMapper objectMapper = new ObjectMapper();

    public TextViewerDialog(String contents, String header) {
        this(contents);

        super.title.setText(header);
    }

    public TextViewerDialog(String contents) {
        this.setHeight("90%");
        this.setWidth("90%");

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
        init(contents);
    }

    private void init(String contents) {
        if(!initialised) {
            this.initialiseEditor();
            this.layout.add(aceEditor);
            this.layout.expand(aceEditor);
            this.aceEditor.setValue(contents);
        }
    }

    protected void initialiseEditor()
    {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.text);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setSizeFull();
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
    }
}
