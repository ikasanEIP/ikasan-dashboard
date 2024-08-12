package org.ikasan.dashboard.ui.search.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerStatusDiv;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class ReplayResultsDialog extends AbstractCloseableResizableDialog
{
    public ReplayResultsDialog(String replayResults, boolean containsErrors)
    {
        super.title.setText(getTranslation("header.replay-manifest", UI.getCurrent().getLocale()));

        init(replayResults, containsErrors);
    }

    private void init(String replayResults, boolean containsErrors)
    {
        SchedulerStatusDiv  statusDiv = new SchedulerStatusDiv();
        statusDiv.setHeight("45px");
        statusDiv.setWidth("100%");
        statusDiv.getStyle().set("margin-top", "5px");
        Div message = new Div();
        message.setHeight("30px");
        message.setWidth("100%");
        message.getStyle().set("margin-top", "5px");
        if(containsErrors) {
            statusDiv.setStatus(InstanceStatus.ERROR);
            message.setText(getTranslation("message.bulk-replay-manifest-error", UI.getCurrent().getLocale()));
        }
        else {
            statusDiv.setStatus(InstanceStatus.COMPLETE);
            message.setText(getTranslation("message.bulk-replay-manifest-success", UI.getCurrent().getLocale()));
        }
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);

        AceEditor aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.text);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
        aceEditor.setValue(replayResults);
        aceEditor.setHeight("100%");
        layout.add(aceEditor);

        super.content.add(statusDiv, message, layout);
        this.setWidth("85%");
        this.setHeight("85%");
    }

}
