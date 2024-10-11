package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.formlayout.FormLayout;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.text.SimpleDateFormat;

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
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP)
            , new FormLayout.ResponsiveStep("600px", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE));
        formLayout.setWidthFull();
        TextField username = new TextField();
        username.setWidthFull();
        username.setValue(authentication.getName());
        username.setReadOnly(true);
        formLayout.addFormItem(username, getTranslation("label.username"));
        TextField locale = new TextField();
        locale.setWidthFull();
        locale.setValue(VaadinSession.getCurrent().getLocale()
            .getDisplayName(VaadinSession.getCurrent().getLocale()));
        locale.setReadOnly(true);
        formLayout.addFormItem(locale, getTranslation("label.locale"));
        TextArea browser = new TextArea();
        browser.setWidthFull();
        browser.setValue(VaadinSession.getCurrent().getBrowser().getBrowserApplication());
        browser.setReadOnly(true);
        formLayout.addFormItem(browser, getTranslation("label.browser"));
        TextField sessionId = new TextField();
        sessionId.setWidthFull();
        sessionId.setValue(VaadinSession.getCurrent().getSession().getId());
        sessionId.setReadOnly(true);
        formLayout.addFormItem(sessionId, getTranslation("label.session-id"));

        TextField buildDateTime = new TextField();
        buildDateTime.setWidthFull();
        buildDateTime.setValue(DateFormatter.instance().getLongFormattedDate(VaadinSession.getCurrent().getSession().getCreationTime()));
        buildDateTime.setReadOnly(true);
        formLayout.addFormItem(buildDateTime, getTranslation("label.session-start-time"));
        formLayout.getStyle().setMarginBottom("50px");

        super.content.add(new VerticalLayout(formLayout));
        showResize(false);
        setResizable(false);
        this.setSizeUndefined();
        this.setWidth("590px");
        this.setHeight("750px");
        super.title.setText(getTranslation("label.session-details"));
    }
}
