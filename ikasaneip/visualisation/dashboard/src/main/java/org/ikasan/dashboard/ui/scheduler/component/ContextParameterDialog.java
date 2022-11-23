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
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextParameterDialog extends AbstractCloseableResizableDialog {

    private boolean isSaveClose = false;
    private VerticalLayout buttonLayout;
    private List<ContextParameterHolder> contextParameterHolders;
    private boolean editable;
    private Grid<ContextParameterHolder> contextParameterHolderGrid;

    /**
     * Constructor
     *
     * @param editable
     */
    public ContextParameterDialog(boolean editable) {
        this.editable = editable;
        super.showResize(false);
        super.title.setText(getTranslation("label.context-parameters", UI.getCurrent().getLocale()));
        this.contextParameterHolders = new ArrayList<>();
        init();
    }

    /**
     * Helper method to initialise the dialog.
     */
    private void init() {
        contextParameterHolderGrid = new Grid<>();
        contextParameterHolderGrid.setVisible(true);
        contextParameterHolderGrid.setWidthFull();
        contextParameterHolderGrid.setHeight("500px");

        contextParameterHolderGrid.addColumn(new ComponentRenderer<>(contextParameterHolder -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(contextParameterHolder.getParamName());
                return horizontalLayout;
            }))
            .setHeader(getTranslation("table-header.parameter-name", UI.getCurrent().getLocale()))
            .setKey("startTimeDate")
            .setResizable(true)
            .setFlexGrow(40);
        contextParameterHolderGrid.addColumn(new ComponentRenderer<>(contextParameterHolder -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(contextParameterHolder.getDefaultValue());
                return horizontalLayout;
            }))
            .setHeader(getTranslation("table-header.parameter-default-value", UI.getCurrent().getLocale()))
            .setKey("endTimeDate")
            .setResizable(true)
            .setFlexGrow(40);

        if(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE)) {
            contextParameterHolderGrid.addColumn(new ComponentRenderer<>(contextParameterHolder -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();

                    Button remove = new Button();
                    remove.getElement().appendChild(VaadinIcon.MINUS.create().getElement());

                    remove.addClickListener(event -> {
                        contextParameterHolders.remove(contextParameterHolder);
                        contextParameterHolderGrid.getDataProvider().refreshAll();
                    });

                    ComponentSecurityVisibility.applySecurity(remove, SecurityConstants.ALL_AUTHORITY,
                        SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                        SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

                    horizontalLayout.add(remove);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, remove);
                    return horizontalLayout;
                }))
                .setKey("remove")
                .setFlexGrow(1);
        }

        contextParameterHolderGrid.setItems(this.contextParameterHolders);
        contextParameterHolderGrid.addClassName("small-header");

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.setVisible(this.editable && ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
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
                NotificationHelper.showErrorNotification(getTranslation("error.configuration-parameters-form"
                    , UI.getCurrent().getLocale()));
            }
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.setVisible(this.editable);
        cancelButton.addClickListener(event -> this.close());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.add(okButton, cancelButton);

        buttonLayout = new VerticalLayout();
        buttonLayout.setWidth("100%");
        buttonLayout.add(buttons);
        buttonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttons);

        this.setWidth("90vw");
        this.setHeight("625px");
    }

    /**
     * Helper method to initialise the context parameters set on the
     * dialog.
     *
     * @param contextParameters
     */
    public void initParams(List<ContextParameter> contextParameters) {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 15)
        );

        contextParameters.forEach(contextParameter -> {
            ContextParameterHolder contextParameterHolder = new ContextParameterHolder(contextParameter);
            contextParameterHolders.add(contextParameterHolder);

            this.contextParameterHolderGrid.getDataProvider().refreshAll();
        });

        Icon addIcon = IconDecorator.decorate(VaadinIcon.PLUS.create(), getTranslation("label.add-context-parameter"
            , UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        addIcon.setVisible(this.editable &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        addIcon.getElement().getStyle().set("margin-left", "auto");

        addIcon.addClickListener(event -> {
            ContextParameterHolder contextParameterHolder = new ContextParameterHolder(new ContextParameterImpl());

            this.contextParameterHolders.add(contextParameterHolder);
            this.contextParameterHolderGrid.getDataProvider().refreshAll();
        });

        formLayout.add(this.contextParameterHolderGrid, 14);
        formLayout.add(addIcon, 1);
        super.content.add(formLayout, buttonLayout);
    }

    /**
     * Indicate if the contents should be saved.
     *
     * @return
     */
    public boolean isSaveClose() {
        return isSaveClose;
    }

    /**
     * Get the context parameters from the dialog.
     *
     * @return
     */
    public List<ContextParameter> getContextParameters() {
        List<ContextParameter> contextParameters = new ArrayList<>();

        contextParameterHolders.forEach(contextParameterHolder
            -> contextParameters.add(contextParameterHolder.getContextParameter()));

        return contextParameters;
    }

    /**
     * Internal private class to assist managing the parameters
     * and associated UI elements.
     */
    private class ContextParameterHolder {
        private TextField paramName = new TextField();
        private TextField defaultValue = new TextField();

        private Icon removeIcon = IconDecorator.decorate(VaadinIcon.MINUS.create()
            , getTranslation("label.remove-context-parameter", UI.getCurrent().getLocale())
            , "14pt", "rgba(241, 90, 35, 1.0)");
        private ContextParameter contextParameter;

        public ContextParameterHolder(ContextParameter contextParameter) {
            this.contextParameter = contextParameter;
            this.paramName.setRequired(true);
            if(contextParameter.getName()!= null)this.paramName.setValue(contextParameter.getName());
            this.paramName.setErrorMessage(getTranslation("error.parameter-name-is-required", UI.getCurrent().getLocale()));
            this.paramName.getElement().getThemeList().add("always-float-label");
            this.paramName.setWidthFull();

            this.defaultValue.getElement().getThemeList().add("always-float-label");
            if(contextParameter.getDefaultValue()!= null)this.defaultValue.setValue(contextParameter.getDefaultValue());
            this.defaultValue.setWidthFull();

            this.paramName.setEnabled(editable &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.defaultValue.setEnabled(editable &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.removeIcon.setVisible(editable &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        }

        public TextField getParamName() {
            return this.paramName;
        }

        public TextField getDefaultValue() {
            return this.defaultValue;
        }

        public Icon getRemoveIcon() {
            return this.removeIcon;
        }

        public ContextParameter getContextParameter() {
            this.contextParameter.setName(this.paramName.getValue());
            this.contextParameter.setDefaultValue(this.defaultValue.getValue() != null
                ? this.defaultValue.getValue() : "");
            return this.contextParameter;
        }

        public boolean validate() {
            boolean isValid = true;

            if(paramName.getValue() == null || paramName.getValue().isEmpty()) {
                paramName.setInvalid(true);
                isValid = false;
            }

            if(!isValid) {
                contextParameterHolderGrid.addClassName("error-header");
                contextParameterHolderGrid.getDataProvider().refreshAll();
            }
            else {
                contextParameterHolderGrid.removeClassName("error-header");
                contextParameterHolderGrid.getDataProvider().refreshAll();
            }

            return isValid;
        }
    }

}