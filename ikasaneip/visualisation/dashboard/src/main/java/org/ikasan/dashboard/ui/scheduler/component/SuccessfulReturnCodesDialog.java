package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SecurityConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuccessfulReturnCodesDialog extends AbstractCloseableResizableDialog {

    private boolean isSaveClose = false;
    private VerticalLayout buttonLayout;
    private List<SuccessfulReturnCodeHolder> successfulReturnCodeHolders;
    private Grid<SuccessfulReturnCodeHolder> successfulReturnCodeHolderGrid;
    private boolean editable;

    public SuccessfulReturnCodesDialog(boolean editable) {
        this.editable = editable;
        super.showResize(false);
        super.title.setText(getTranslation("label.successful-return-codes", UI.getCurrent().getLocale()));
        this.successfulReturnCodeHolders = new ArrayList<>();
        init();
    }

    private void init() {
        successfulReturnCodeHolderGrid = new Grid<>();
        successfulReturnCodeHolderGrid.setVisible(true);
        successfulReturnCodeHolderGrid.setWidthFull();
        successfulReturnCodeHolderGrid.setHeight("500px");

        successfulReturnCodeHolderGrid.addColumn(new ComponentRenderer<>(contextParameterHolder -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(contextParameterHolder.getReturnCode());
                return horizontalLayout;
            }))
            .setHeader(getTranslation("label.successful-return-code", UI.getCurrent().getLocale()))
            .setKey("startTimeDate")
            .setResizable(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE))
            .setFlexGrow(40);

        if(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)) {
            successfulReturnCodeHolderGrid.addColumn(new ComponentRenderer<>(contextParameterHolder -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();

                    Button remove = new Button();
                    remove.getElement().appendChild(VaadinIcon.MINUS.create().getElement());

                    remove.addClickListener(event -> {
                        successfulReturnCodeHolders.remove(contextParameterHolder);
                        successfulReturnCodeHolderGrid.getDataProvider().refreshAll();
                    });

                    ComponentSecurityVisibility.applySecurity(remove, SecurityConstants.ALL_AUTHORITY,
                        SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                        SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

                    horizontalLayout.add(remove);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, remove);
                    return horizontalLayout;
                }))
                .setKey("remove")
                .setWidth("50px");
        }

        successfulReturnCodeHolderGrid.setItems(this.successfulReturnCodeHolders);
        successfulReturnCodeHolderGrid.addClassName("small-header");

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.setVisible(this.editable &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
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
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-return-codes"
                    , UI.getCurrent().getLocale()));
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

        this.setWidth("700px");
        this.setHeight("625px");
    }

    public void initReturnCodes(List<String> successfulReturnCodes) {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 10)
        );

        if(successfulReturnCodes != null) {
            successfulReturnCodes.forEach(code -> {
                SuccessfulReturnCodeHolder successfulReturnCodeHolder = new SuccessfulReturnCodeHolder(code);
                successfulReturnCodeHolders.add(successfulReturnCodeHolder);
            });
            successfulReturnCodeHolderGrid.getDataProvider().refreshAll();
        }

        Icon addIcon = IconDecorator.decorate(VaadinIcon.PLUS.create()
            , getTranslation("label.add-return-code", UI.getCurrent().getLocale())
            , "14pt", "rgba(241, 90, 35, 1.0)");

        addIcon.getElement().getStyle().set("margin-left", "auto");
        addIcon.setVisible(this.editable &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        addIcon.addClickListener(event -> {
            SuccessfulReturnCodeHolder successfulReturnCodeHolder = new SuccessfulReturnCodeHolder(null);
            successfulReturnCodeHolders.add(successfulReturnCodeHolder);
            successfulReturnCodeHolderGrid.getDataProvider().refreshAll();
        });

        formLayout.add(this.successfulReturnCodeHolderGrid, 9);
        formLayout.add(addIcon, 1);

        super.content.add(formLayout, buttonLayout);
    }

    public boolean isSaveClose() {
        return isSaveClose;
    }

    public List<String> getSuccessfulReturnCodes() {
        List<String> successfulReturnCodes = new ArrayList<>();

        successfulReturnCodeHolders.forEach(successfulReturnCodeHolder
            -> successfulReturnCodes.add(successfulReturnCodeHolder.getCode()));

        return successfulReturnCodes;
    }

    private class SuccessfulReturnCodeHolder {
        private TextField returnCode = new TextField();

        public SuccessfulReturnCodeHolder(String code) {
            this.returnCode.setRequired(true);
            if(code != null)this.returnCode.setValue(code);
            this.returnCode.setErrorMessage(getTranslation("error.missing-return-code"
                , UI.getCurrent().getLocale()));
            returnCode.setEnabled(editable &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            returnCode.setWidthFull();
        }

        public TextField getReturnCode() {
            return this.returnCode;
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

            if(!isValid) {
                successfulReturnCodeHolderGrid.addClassName("error-header");
                successfulReturnCodeHolderGrid.getDataProvider().refreshAll();
            }
            else {
                successfulReturnCodeHolderGrid.removeClassName("error-header");
                successfulReturnCodeHolderGrid.getDataProvider().refreshAll();
            }

            return isValid;
        }
    }

}