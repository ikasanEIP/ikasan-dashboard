package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;

public class JobLockCacheViewerDialog extends AbstractCloseableResizableDialog {
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    public JobLockCacheViewerDialog() {
        this.setHeight("95vh");
        this.setWidth("90vw");

        AceEditor aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.json);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setWidth("100%");
        aceEditor.setHeight("1300px");
        aceEditor.setReadOnly(true);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(aceEditor);
        layout.getStyle().set("padding-bottom", "20px");

        try {
            aceEditor.setValue(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(JobLockCacheImpl.instance().getJobLockCacheData()));
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification("Could not display Job Lock Cache contents!");
        }

        super.title.setText("Job Lock Cache Contents");

        super.content.add(layout);
    }
}
