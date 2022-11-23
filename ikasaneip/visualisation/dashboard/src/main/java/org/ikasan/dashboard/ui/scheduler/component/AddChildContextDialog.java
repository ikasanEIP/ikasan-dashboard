package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.listener.NewContextListener;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.job.orchestration.builder.context.ContextTemplateBuilder;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.ArrayList;
import java.util.List;

public class AddChildContextDialog extends AbstractCloseableResizableDialog {

    private List<NewContextListener> newContextListeners = new ArrayList<>();

    public AddChildContextDialog() {
        this.setHeight("420px");
        this.setWidth("500px");

        super.setResizable(false);
        super.showResize(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add();
        layout.getStyle().set("padding-bottom", "20px");
        layout.getStyle().set("padding-top", "20px");

        FormLayout formLayout = new FormLayout();

        Binder<ContextDetails> formBinder = new Binder<>(ContextDetails.class);

        TextField contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        contextNameTf.setRequired(true);
        contextNameTf.addThemeName("always-float-label");
        formLayout.add(contextNameTf, 2);
        formBinder.forField(contextNameTf)
            .withValidator(contextName -> !contextName.isEmpty(), getTranslation("error.missing-context-name", UI.getCurrent().getLocale()))
            .bind(ContextDetails::getContextName, ContextDetails::setContextName);

        TextArea contextDescriptionTa = new TextArea(getTranslation("label.context-description", UI.getCurrent().getLocale()));
        contextDescriptionTa.addThemeName("always-float-label");
        contextDescriptionTa.setRequired(true);
        contextDescriptionTa.setMinHeight("150px");
        contextDescriptionTa.setMaxHeight("150px");
        formLayout.add(contextDescriptionTa, 2);
        formBinder.forField(contextDescriptionTa)
            .withValidator(contextDescription -> !contextDescription.isEmpty(), getTranslation("error.missing-context-description", UI.getCurrent().getLocale()))
            .bind(ContextDetails::getContextDescription, ContextDetails::setContextDescription);

        formBinder.setBean(new ContextDetails());

        Button addButton = new Button(getTranslation("button.add", UI.getCurrent().getLocale()));
        addButton.addClickListener(buttonClickEvent ->  {
            ContextDetails contextDetails = new ContextDetails();

            try {
                formBinder.writeBean(contextDetails);
            }
            catch (ValidationException e) {
                return;
            }

            ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();
            ContextTemplate contextTemplate = contextTemplateBuilder.withName(contextDetails.getContextName())
                .withDescription(contextDetails.getContextDescription())
                .build();

            newContextListeners.forEach(newContextListener -> newContextListener.newContext(contextTemplate));

            this.close();
        });

        ComponentSecurityVisibility.applySecurity(addButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(addButton, cancelButton);
        buttonLayout.getElement().getStyle().set("margin-top", "40px");

        layout.add(formLayout, buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

        super.title.setText(getTranslation("label.add-child-context", UI.getCurrent().getLocale()));

        super.content.getStyle().set("padding-top", "0px");
        super.content.add(layout);
    }

    public void addNewContextListener(NewContextListener newContextListener) {
        this.newContextListeners.add(newContextListener);
    }

    private class ContextDetails {
        private String contextName;
        private String contextDescription;

        public String getContextName() {
            return contextName;
        }

        public void setContextName(String contextName) {
            this.contextName = contextName;
        }

        public String getContextDescription() {
            return contextDescription;
        }

        public void setContextDescription(String contextDescription) {
            this.contextDescription = contextDescription;
        }
    }
}
