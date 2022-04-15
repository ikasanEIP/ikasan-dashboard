package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonViewerDialog extends AbstractCloseableResizableDialog  {

    private Logger logger = LoggerFactory.getLogger(JsonViewerDialog.class);

    private VerticalLayout layout;

    private AceEditor aceEditor;

    private boolean initialised = false;

    private ObjectMapper objectMapper = new ObjectMapper();

    public JsonViewerDialog(Object contents) {
        this.setHeight("90%");
        this.setWidth("90%");

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
        init(contents);
    }

    private void init(Object contents) {
        if(!initialised) {
            this.initialiseEditor();
            this.layout.add(aceEditor);

            try {
                this.aceEditor.setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contents));
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }
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
