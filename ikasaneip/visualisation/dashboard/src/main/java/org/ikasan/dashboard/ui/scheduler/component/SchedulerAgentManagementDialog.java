package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.spec.metadata.ModuleMetaData;

public class SchedulerAgentManagementDialog extends AbstractCloseableResizableDialog {

    /**
     * Constructor
     *
     * @param agent
     */
    public SchedulerAgentManagementDialog(ModuleMetaData agent) {
        super.showResize(false);
        super.setResizable(false);
        super.title.setText(getTranslation("header.scheduler-agent-management", UI.getCurrent().getLocale()));

        this.setHeight("350px");
        this.setWidth("70%");

        H4 agentDetails = new H4(getTranslation("header.agent-details", UI.getCurrent().getLocale()));

        FormLayout formLayout = new FormLayout();

        TextField agentName = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        agentName.setValue(agent.getName());
        agentName.setEnabled(false);
        formLayout.add(agentName);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        TextField agentUrlLf = new TextField(getTranslation("label.agent-url", UI.getCurrent().getLocale()));
        agentUrlLf.setPrefixComponent(link);
        agentUrlLf.setValue(" ");
        formLayout.add(agentUrlLf);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(agentDetails, formLayout);
        super.content.add(layout);
    }
}
