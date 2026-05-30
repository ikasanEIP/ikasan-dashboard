package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextParameterEncryptDialog extends AbstractCloseableResizableDialog {

    private static final Logger LOG = LoggerFactory.getLogger(ContextParameterEncryptDialog.class);

    private SpringCloudConfigRefreshService springCloudConfigRefreshService;

    public ContextParameterEncryptDialog(SpringCloudConfigRefreshService springCloudConfigRefreshService) {
        this.springCloudConfigRefreshService = springCloudConfigRefreshService;
        super.setVisible(true);

        super.title.setText(getTranslation("label.encrypt-context-parameters-values", UI.getCurrent().getLocale()));
        this.init();
    }

    private void init() {

        FormLayout formLayout = new FormLayout();
        formLayout.setVisible(true);
        formLayout.setWidthFull();
        formLayout.setHeight("180px");
        formLayout.setResponsiveSteps(
            // Use two columns by default
            new FormLayout.ResponsiveStep("0", 2)
        );
        H3 jobLockManagementLabel = new H3(getTranslation("label.encrypt-context-parameters-values", UI.getCurrent().getLocale()));
        jobLockManagementLabel.getElement().getStyle().set("margin-top", "10px");
        formLayout.add(jobLockManagementLabel, 2);

        H5 valueToEncryptLabel = new H5(getTranslation("label.encrypt-context-parameters-values", UI.getCurrent().getLocale()));
        valueToEncryptLabel.getElement().getStyle().set("margin-top", "30px");
        formLayout.add(valueToEncryptLabel);

        TextField valueToEncrypt = new TextField(getTranslation("label.value-to-encrypt", UI.getCurrent().getLocale()));
        valueToEncrypt.setId("valueToEncrypt");
        valueToEncrypt.getElement().getStyle().set("margin-top", "30px");
        formLayout.add(valueToEncrypt);

        NativeLabel hiddenLabel = new NativeLabel();
        formLayout.add(hiddenLabel);


        // Defining this above the button so that the button can reference this TextField to add the encrypted value.
        // Will be added to the UI after
        TextArea encryptedValue = new TextArea(getTranslation("label.encrypted-value", UI.getCurrent().getLocale()));
        encryptedValue.setId("encryptedValue");
        encryptedValue.getElement().getStyle().set("margin-top", "50px").set("minHeight", "100px");

        Button encryptButton = new Button(getTranslation("button.encrypt", UI.getCurrent().getLocale()));
        encryptButton.getElement().getStyle().set("text-align", "right");
        encryptButton.setIconAfterText(true);
        encryptButton.addClickListener(buttonClickEvent -> {
            try {
                String encryptedStringResponse = springCloudConfigRefreshService.encrypt(valueToEncrypt.getValue());
                encryptedValue.setValue(encryptedStringResponse);
            } catch (Exception e) {
                LOG.warn("Failed to encrypt value: {}", e.getMessage(), e);
                NotificationHelper.showUserNotification(getTranslation("message.unable-to-encrypt-value", UI.getCurrent().getLocale()));
            }
        });

        formLayout.add(encryptButton);

        formLayout.add(encryptedValue, 2);

        Button closeButton = new Button(getTranslation("button.close", UI.getCurrent().getLocale()));
        closeButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());
        closeButton.getElement().getStyle().set("margin-top", "50px");

        formLayout.add(closeButton, 2);

        super.content.add(formLayout);
        this.setWidth("60vw");
        this.setHeight("625px");

    }

}
