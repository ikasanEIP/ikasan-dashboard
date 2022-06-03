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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuccessfulReturnCodesDialog extends AbstractCloseableResizableDialog {

    private boolean isSaveClose = false;
    private VerticalLayout buttonLayout;
    private List<SuccessfulReturnCodeHolder> successfulReturnCodeHolders;
    private boolean editable;

    public SuccessfulReturnCodesDialog(boolean editable) {
        this.editable = editable;
        super.showResize(false);
        super.title.setText(getTranslation("label.successful-return-codes", UI.getCurrent().getLocale()));
        this.successfulReturnCodeHolders = new ArrayList<>();
        init();
    }

    private void init() {

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.setVisible(this.editable);
        okButton.addClickListener(event -> {
            AtomicBoolean isValid = new AtomicBoolean(true);
            this.successfulReturnCodeHolders.forEach(successfulReturnCodeHolder -> {
                if(!successfulReturnCodeHolder.validate()) {
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

        this.setWidth("600px");
        this.setHeight("500px");
    }

    public void initReturnCodes(List<String> successfulReturnCodes) {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 3)
        );

        successfulReturnCodes.forEach(contextParameter -> {
            SuccessfulReturnCodeHolder successfulReturnCodeHolder = new SuccessfulReturnCodeHolder(contextParameter);

            formLayout.add(successfulReturnCodeHolder.getReturnCode()
                , successfulReturnCodeHolder.getRemoveIcon());
            formLayout.setColspan(successfulReturnCodeHolder.getReturnCode(), 2);

            successfulReturnCodeHolder.getRemoveIcon().addClickListener(event -> {
               formLayout.remove(successfulReturnCodeHolder.getReturnCode()
                   , successfulReturnCodeHolder.getRemoveIcon());

               successfulReturnCodeHolders.remove(successfulReturnCodeHolder);
            });

            successfulReturnCodeHolders.add(successfulReturnCodeHolder);
        });

        Icon addIcon = IconDecorator.decorate(VaadinIcon.PLUS.create(), getTranslation("label.add-return-code", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        addIcon.getElement().getStyle().set("margin-left", "auto");
        addIcon.setVisible(this.editable);

        addIcon.addClickListener(event -> {
            SuccessfulReturnCodeHolder successfulReturnCodeHolder = new SuccessfulReturnCodeHolder(null);

            formLayout.add(successfulReturnCodeHolder.getReturnCode()
                , successfulReturnCodeHolder.getRemoveIcon());
            formLayout.setColspan(successfulReturnCodeHolder.getReturnCode(), 2);

            successfulReturnCodeHolder.getRemoveIcon().addClickListener(clickEvent -> {
                formLayout.remove(successfulReturnCodeHolder.getReturnCode()
                    , successfulReturnCodeHolder.getRemoveIcon());

                successfulReturnCodeHolders.remove(successfulReturnCodeHolder);
            });

            successfulReturnCodeHolders.add(successfulReturnCodeHolder);
        });

        super.content.add(addIcon, formLayout, buttonLayout);
    }

    public boolean isSaveClose() {
        return isSaveClose;
    }

    public List<String> getSuccessfulReturnCodes() {
        List<String> successfulReturnCodes = new ArrayList<>();

        successfulReturnCodeHolders.forEach(successfulReturnCodeHolder -> successfulReturnCodes.add(successfulReturnCodeHolder.getCode()));

        return successfulReturnCodes;
    }

    private class SuccessfulReturnCodeHolder {
        private TextField returnCode = new TextField(getTranslation("label.successful-return-code", UI.getCurrent().getLocale()));

        private Icon removeIcon = IconDecorator.decorate(VaadinIcon.MINUS.create()
            , getTranslation("label.remove-return-code", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        private String code;

        public SuccessfulReturnCodeHolder(String code) {
            this.code = code;
            this.returnCode.setRequired(true);
            if(code != null)this.returnCode.setValue(code);
            this.returnCode.setErrorMessage(getTranslation("error.missing-return-code", UI.getCurrent().getLocale()));
            returnCode.setEnabled(editable);
            removeIcon.setVisible(editable);
        }

        public TextField getReturnCode() {
            return this.returnCode;
        }


        public Icon getRemoveIcon() {
            return this.removeIcon;
        }

        public String getCode() {
            return this.returnCode.getValue();
        }

        public boolean validate() {
            boolean isValid = true;

            if(returnCode.getValue() == null || returnCode.getValue().isEmpty()) {
                returnCode.setInvalid(true);
                isValid = false;
            }

            try {
                Integer.parseInt(this.returnCode.getValue());
            }
            catch (Exception e) {
                returnCode.setInvalid(true);
                isValid = false;
            }

            return isValid;
        }
    }

}