package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextParameterDialog extends AbstractCloseableResizableDialog {

    private boolean isSaveClose = false;
    private VerticalLayout buttonLayout;
    private List<ContextParameterHolder> contextParameterHolders;

    public ContextParameterDialog() {
        super.showResize(false);
        super.title.setText(getTranslation("label.context-parameters", UI.getCurrent().getLocale()));
        this.contextParameterHolders = new ArrayList<>();
        init();
    }

    private void init() {

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.addClickListener(event -> {
            AtomicBoolean isValid = new AtomicBoolean(true);
            this.contextParameterHolders.forEach(contextParameterHolder -> {
                if(!contextParameterHolder.validate()) {
                    isValid.set(false);
                }
            });

            if(isValid.get()) {
                this.isSaveClose = true;
                this.close();
            }
            else {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
            }
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(event -> this.close());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.add(okButton, cancelButton);

        buttonLayout = new VerticalLayout();
        buttonLayout.setWidth("100%");
        buttonLayout.add(buttons);
        buttonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttons);

        this.setWidth("1400px");
        this.setHeight("500px");
    }

    public void initParams(List<ContextParameter> contextParameters) {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 7)
        );

        contextParameters.forEach(contextParameter -> {
            ContextParameterHolder contextParameterHolder = new ContextParameterHolder(contextParameter);

            formLayout.add(contextParameterHolder.getParamName()
                , contextParameterHolder.getParamType(), contextParameterHolder.getDefaultValue(), contextParameterHolder.getRemoveIcon());
            formLayout.setColspan(contextParameterHolder.getParamName(), 2);
            formLayout.setColspan(contextParameterHolder.getParamType(), 2);
            formLayout.setColspan(contextParameterHolder.getDefaultValue(), 2);

            contextParameterHolder.getRemoveIcon().addClickListener(event -> {
               formLayout.remove(contextParameterHolder.getParamName()
                   , contextParameterHolder.getParamType()
                   , contextParameterHolder.getDefaultValue()
                   , contextParameterHolder.getRemoveIcon());

               contextParameterHolders.remove(contextParameterHolder);
            });

            contextParameterHolders.add(contextParameterHolder);
        });

        Icon addIcon = IconDecorator.decorate(VaadinIcon.PLUS.create(), getTranslation("label.add-context-parameter", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        addIcon.getElement().getStyle().set("margin-left", "auto");

        addIcon.addClickListener(event -> {
            ContextParameterHolder contextParameterHolder = new ContextParameterHolder(new ContextParameterImpl());

            formLayout.add(contextParameterHolder.getParamName()
                , contextParameterHolder.getParamType(), contextParameterHolder.getDefaultValue(), contextParameterHolder.getRemoveIcon());
            formLayout.setColspan(contextParameterHolder.getParamName(), 2);
            formLayout.setColspan(contextParameterHolder.getParamType(), 2);
            formLayout.setColspan(contextParameterHolder.getDefaultValue(), 2);

            contextParameterHolder.getRemoveIcon().addClickListener(clickEvent -> {
                formLayout.remove(contextParameterHolder.getParamName()
                    , contextParameterHolder.getParamType()
                    , contextParameterHolder.getDefaultValue()
                    , contextParameterHolder.getRemoveIcon());

                contextParameterHolders.remove(contextParameterHolder);
            });

            contextParameterHolders.add(contextParameterHolder);
        });

        super.content.add(addIcon, formLayout, buttonLayout);
    }

    public boolean isSaveClose() {
        return isSaveClose;
    }

    public List<ContextParameter> getContextParameters() {
        List<ContextParameter> contextParameters = new ArrayList<>();

        contextParameterHolders.forEach(contextParameterHolder -> contextParameters.add(contextParameterHolder.getContextParameter()));

        return contextParameters;
    }

    private class ContextParameterHolder {
        private TextField paramName = new TextField(getTranslation("label.parameter-name", UI.getCurrent().getLocale()));
        private TextField paramType = new TextField(getTranslation("label.parameter-type", UI.getCurrent().getLocale()));
        private TextField defaultValue = new TextField(getTranslation("label.parameter-default-value", UI.getCurrent().getLocale()));

        private Icon removeIcon = IconDecorator.decorate(VaadinIcon.MINUS.create()
            , getTranslation("label.remove-context-parameter", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        private ContextParameter contextParameter;

        public ContextParameterHolder(ContextParameter contextParameter) {
            this.contextParameter = contextParameter;
            this.paramName.setRequired(true);
            if(contextParameter.getName()!= null)this.paramName.setValue(contextParameter.getName());
            this.paramName.setErrorMessage(getTranslation("error.parameter-name-is-required", UI.getCurrent().getLocale()));
            this.paramType.setRequired(true);
            if(contextParameter.getType()!= null)this.paramType.setValue(contextParameter.getType());
            this.paramType.setErrorMessage(getTranslation("error.parameter-type-is-required", UI.getCurrent().getLocale()));
        }

        public TextField getParamName() {
            return this.paramName;
        }

        public TextField getParamType() {
            return this.paramType;
        }

        public TextField getDefaultValue() {
            return this.defaultValue;
        }

        public Icon getRemoveIcon() {
            return this.removeIcon;
        }

        public ContextParameter getContextParameter() {
            this.contextParameter.setName(this.paramName.getValue());
            this.contextParameter.setType(this.paramType.getValue());
            return this.contextParameter;
        }

        public boolean validate() {
            boolean isValid = true;

            if(paramName.getValue() == null || paramName.getValue().isEmpty()) {
                paramName.setInvalid(true);
                isValid = false;
            }

            if(paramType.getValue() == null || paramType.getValue().isEmpty()) {
                paramType.setInvalid(true);
                isValid = false;
            }

            return isValid;
        }
    }

}