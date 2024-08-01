package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SessionDetailsDialog extends AbstractCloseableResizableDialog
{
    public SessionDetailsDialog()
    {
        init();
    }

    private void init()
    {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        TextField username = new TextField();
        username.setWidthFull();
        username.setValue(authentication.getName());
        username.setReadOnly(true);
        formLayout.addFormItem(username, "Username");
        TextField locale = new TextField();
        locale.setWidthFull();
        locale.setValue(VaadinSession.getCurrent().getLocale().getDisplayName());
        locale.setReadOnly(true);
        formLayout.addFormItem(locale, "Locale");
        TextArea browser = new TextArea();
        browser.setWidthFull();
        browser.setValue(VaadinSession.getCurrent().getBrowser().getBrowserApplication());
        browser.setReadOnly(true);
        formLayout.addFormItem(browser, "Browser");
        TextField sessionId = new TextField();
        sessionId.setWidthFull();
        sessionId.setValue(VaadinSession.getCurrent().getSession().getId());
        sessionId.setReadOnly(true);
        formLayout.addFormItem(sessionId, "Session Id");

        TextField buildDateTime = new TextField();
        buildDateTime.setWidthFull();
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");
        buildDateTime.setValue(formatter.format(VaadinSession.getCurrent().getSession().getCreationTime()));
        buildDateTime.setReadOnly(true);
        formLayout.addFormItem(buildDateTime, "Session start time");

        super.content.add(formLayout);
        showResize(false);
        setResizable(false);
        this.setWidth("650px");
        this.setHeight("500px");
        super.title.setText("Session details");
    }
}
