package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.DownloadModulesLogDialog;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.DownloadLogFileService;
import org.springframework.security.core.context.SecurityContextHolder;

public class SchedulerAgentManagementDialog extends AbstractCloseableResizableDialog {

    /**
     * Constructor
     *
     * @param agent
     */
    public SchedulerAgentManagementDialog(ModuleMetaData agent, DownloadLogFileService downloadLogFileService) {
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

        Button downloadLogFileButton = new Button(getTranslation("button.download-log-file", UI.getCurrent().getLocale()), VaadinIcon.DOWNLOAD.create());
        downloadLogFileButton.setIconAfterText(false);
        downloadLogFileButton.addClickListener(buttonClickEvent -> {
            DownloadModulesLogDialog downloadModulesLogDialog = new DownloadModulesLogDialog(agent, downloadLogFileService);
            downloadModulesLogDialog.open();
        });
        ComponentSecurityVisibility.applySecurity((IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication(), downloadLogFileButton,
            SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE,
            SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN,
            SecurityConstants.SCHEDULER_ALL_WRITE);
        formLayout.add(downloadLogFileButton, 2);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(agentDetails, formLayout);
        super.content.add(layout);
    }
}
