package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.ikasan.dashboard.ui.util.ApplicationContextProvider;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AboutIkasanDialog extends AbstractCloseableResizableDialog
{

    public AboutIkasanDialog()
    {
        init();
    }

    private void init()
    {
        BuildProperties buildProperties = (BuildProperties)ApplicationContextProvider.getContext().getBean("buildProperties");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        TextField applicationName = new TextField();
        applicationName.setWidthFull();
        applicationName.setValue(buildProperties.getName());
        applicationName.setReadOnly(true);
        formLayout.addFormItem(applicationName, "Application Name");
        TextField buildVersion = new TextField();
        buildVersion.setWidthFull();
        buildVersion.setValue(buildProperties.getVersion());
        buildVersion.setReadOnly(true);
        formLayout.addFormItem(buildVersion, "Build Version");
        TextField buildDateTime = new TextField();
        buildDateTime.setWidthFull();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());
        buildDateTime.setValue(formatter.format(buildProperties.getTime()));
        buildDateTime.setReadOnly(true);
        formLayout.addFormItem(buildDateTime, "Build date/time");

        super.content.add(formLayout);
        showResize(false);
        setResizable(false);
        this.setWidth("650px");
        this.setHeight("300px");
        super.title.setText("About");
    }
}
