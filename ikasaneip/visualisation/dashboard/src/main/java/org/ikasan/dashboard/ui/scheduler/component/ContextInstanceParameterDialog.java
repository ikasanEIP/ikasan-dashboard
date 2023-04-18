package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ContextInstanceParameterDialog extends AbstractCloseableResizableDialog {

    private boolean isSaveClose = false;
    private VerticalLayout buttonLayout;
    private List<ContextParameterInstanceHolder> contextParameterHolders;
    private boolean editable;
    private Grid<ContextParameterInstanceHolder> contextParameterHolderGrid;

    private DataProvider<ContextParameterInstanceHolder, ContextParameterFilter> dataProvider;
    private ConfigurableFilterDataProvider<ContextParameterInstanceHolder, Void, ContextParameterFilter> filteredDataProvider;

    private ContextParameterFilter searchFilter = new ContextParameterFilter();

    /**
     * Constructor
     *
     * @param editable
     */
    public ContextInstanceParameterDialog(boolean editable) {
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
            .setKey("paramName")
            .setResizable(true)
            .setFlexGrow(40);
        contextParameterHolderGrid.addColumn(new ComponentRenderer<>(contextParameterHolder -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(contextParameterHolder.getValue());
                return horizontalLayout;
            }))
            .setHeader(getTranslation("table-header.parameter-default-value", UI.getCurrent().getLocale()))
            .setKey("paramDefault")
            .setResizable(true)
            .setFlexGrow(40);

        contextParameterHolderGrid.addClassName("small-header");
        this.initDataProvider();

        HeaderRow hr = contextParameterHolderGrid.appendHeaderRow();
        this.addGridFiltering(hr, this.searchFilter::setParameterName, "paramName");

        Button okButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
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
    public void initParams(List<ContextParameterInstance> contextParameters) {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 15)
        );

        contextParameters.forEach(contextParameter -> {
            ContextParameterInstanceHolder contextParameterHolder = new ContextParameterInstanceHolder(contextParameter);
            contextParameterHolders.add(contextParameterHolder);
        });

        this.contextParameterHolderGrid.getDataProvider().refreshAll();

        formLayout.add(this.contextParameterHolderGrid, 15);
        super.content.add(formLayout, buttonLayout);
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    private void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey) {
        TextField textField = new TextField();
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(this.contextParameterHolderGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Initialise the grid.
     */
    public void initDataProvider() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<ContextParameterFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<ContextParameterInstanceHolder> results = this.getResults(filter.get(), offset, limit);

            return results.stream();
        }, query -> {
            Optional<ContextParameterFilter> filter = query.getFilter();

            List<ContextParameterInstanceHolder> results = this.getResults(filter.get(), -1, -1);

            return results.size();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.contextParameterHolderGrid.setDataProvider(filteredDataProvider);
    }

    private List<ContextParameterInstanceHolder> getResults(ContextParameterFilter filter, int offset, int limit) {
        List<ContextParameterInstanceHolder> results = this.contextParameterHolders.stream()
            .filter(contextParameterHolder -> {
                if(filter.getParameterName() == null || filter.getParameterName().isEmpty()) {
                    return true;
                }
                else if(contextParameterHolder.getParamName().getValue() == null
                    || contextParameterHolder.getParamName().getValue().isEmpty()) {
                    return true;
                }
                else if(contextParameterHolder.getParamName().getValue() != null && contextParameterHolder.contextParameter.getName() == null) {
                    return contextParameterHolder.getParamName().getValue().toLowerCase().contains(filter.getParameterName().toLowerCase());
                }
                else {
                    return contextParameterHolder.contextParameter.getName()
                        .toLowerCase().contains(filter.getParameterName().toLowerCase());
                }
            })
            .collect(Collectors.toList());

        if(offset == -1 && limit == -1) {
            return results;
        }

        if(limit > results.size()) {
            return results;
        }
        else {
            return results.subList(offset, offset + limit);
        }
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
    public List<ContextParameterInstance> getContextParameters() {
        List<ContextParameterInstance> contextParameters = new ArrayList<>();

        contextParameterHolders.forEach(contextParameterHolder
            -> contextParameters.add(contextParameterHolder.getContextParameter()));

        return contextParameters;
    }

    /**
     * Internal private class to assist managing the parameters
     * and associated UI elements.
     */
    private class ContextParameterInstanceHolder {
        private TextField paramName = new TextField();
        private TextField value = new TextField();

        private ContextParameterInstance contextParameter;

        public ContextParameterInstanceHolder(ContextParameterInstance contextParameter) {
            this.contextParameter = contextParameter;
            this.paramName.setRequired(true);
            if(contextParameter.getName()!= null)this.paramName.setValue(contextParameter.getName());
            this.paramName.setErrorMessage(getTranslation("error.parameter-name-is-required", UI.getCurrent().getLocale()));
            this.paramName.getElement().getThemeList().add("always-float-label");
            this.paramName.setWidthFull();

            this.value.getElement().getThemeList().add("always-float-label");
            if(contextParameter.getValue() != null && !contextParameter.getValue().isEmpty()) {
                this.value.setValue(contextParameter.getValue());
            }
            else if(contextParameter.getDefaultValue()!= null) {
                this.value.setValue(contextParameter.getDefaultValue());
            }

            this.value.setWidthFull();

            this.paramName.setEnabled(editable &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.value.setEnabled(editable &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        }

        public TextField getParamName() {
            return this.paramName;
        }

        public TextField getValue() {
            return this.value;
        }

        public ContextParameterInstance getContextParameter() {
            this.contextParameter.setName(this.paramName.getValue());
            this.contextParameter.setValue(this.value.getValue() != null
                ? this.value.getValue() : "");
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

    private class ContextParameterFilter {
        private String parameterName;

        public String getParameterName() {
            return parameterName;
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }
    }

}