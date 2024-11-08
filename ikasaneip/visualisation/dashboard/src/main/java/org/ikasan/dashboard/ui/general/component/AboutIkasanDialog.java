package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.util.DashboardApplicationContextProvider;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.springframework.boot.info.BuildProperties;

public class AboutIkasanDialog extends AbstractCloseableResizableDialog
{

    public AboutIkasanDialog()
    {
        init();
    }

    private void init()
    {
        BuildProperties buildProperties = (BuildProperties) DashboardApplicationContextProvider.getContext().getBean("buildProperties");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP)
            , new FormLayout.ResponsiveStep("600px", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE));
        formLayout.setWidthFull();
        TextField applicationName = new TextField();
        applicationName.setWidthFull();
        applicationName.setValue(buildProperties.getName());
        applicationName.setReadOnly(true);
        formLayout.addFormItem(applicationName, getTranslation("label.application", getLocale()));
        TextField buildVersion = new TextField();
        buildVersion.setWidthFull();
        buildVersion.setValue(buildProperties.getVersion());
        buildVersion.setReadOnly(true);
        formLayout.addFormItem(buildVersion, getTranslation("label.build-version", getLocale()));
        TextField buildDateTime = new TextField();
        buildDateTime.setWidthFull();
        buildDateTime.setValue(DateFormatter.instance().getLongFormattedDate(buildProperties.getTime().toEpochMilli()));
        buildDateTime.setReadOnly(true);
        formLayout.addFormItem(buildDateTime, getTranslation("label.build-date-time", getLocale()));

        super.content.add(new VerticalLayout(formLayout));
        super.content.setHeight("80%");
        showResize(false);
        setResizable(false);
        this.setSizeUndefined();
        this.setWidth("550px");
        this.setHeight("500px");
        super.title.setText(getTranslation("label.about", getLocale()));
    }
}
