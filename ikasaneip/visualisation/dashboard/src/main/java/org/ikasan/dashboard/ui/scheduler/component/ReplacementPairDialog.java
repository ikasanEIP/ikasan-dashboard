package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.job.orchestration.model.job.ReplacementPairImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;

public class ReplacementPairDialog extends AbstractCloseableResizableDialog {

    private TextField replacementTokenTf;
    private Select<ContextParameter> contextParameterSelect;

    private ContextTemplate contextTemplate;

    private boolean saved = false;

    public ReplacementPairDialog(ContextTemplate contextTemplate) {
        this.contextTemplate = contextTemplate;
        this.init();
    }

    private void init() {
        super.title.setText("Replacement Tokens");
        super.setResizable(false);
        this.replacementTokenTf = new TextField("Replacement Token");
        this.replacementTokenTf.setErrorMessage("A replacement token must be provided!");
        this.replacementTokenTf.setWidth("350px");
        this.replacementTokenTf.setId("replacementTokenTf");
        this.contextParameterSelect = new Select<>();
        this.contextParameterSelect.setWidth("350px");
        this.contextParameterSelect.setLabel("Job Plan Parameter Name");
        this.contextParameterSelect.setItems(contextTemplate.getContextParameters());
        this.contextParameterSelect.setItemLabelGenerator(ContextParameter::getName);
        this.contextParameterSelect.setErrorMessage("A job plan parameter name must be selected!");
        this.contextParameterSelect.setId("contextParameterSelect");

        HorizontalLayout formLayout = new HorizontalLayout();
        formLayout.add(this.replacementTokenTf, this.contextParameterSelect);

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("replacementPairSaveButton");
        saveButton.addClickListener(event -> {
            boolean valid = true;

            if (this.replacementTokenTf.getValue() == null || this.replacementTokenTf.getValue().isEmpty()) {
                valid = false;
                this.replacementTokenTf.setInvalid(true);
            }
            if (this.contextParameterSelect.getValue() == null) {
                valid = false;
                this.contextParameterSelect.setInvalid(true);
            }
            this.saved = valid;
            if (valid) this.close();
        });

        Button cancelButton = new Button(getTranslation("button.close", UI.getCurrent().getLocale()));
        cancelButton.setId("replacementPairCancelButton");
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(true);
        buttonLayout.setSpacing(true);
        buttonLayout.add(saveButton, cancelButton);
        buttonLayout.getStyle().set("padding-bottom", "40px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(formLayout, buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, formLayout, buttonLayout);

        formLayout.setSizeFull();
        this.setWidth("800px");
        this.setHeight("250px");
        super.content.add(layout);
    }

    public ReplacementPairImpl getReplacementPair() {
        ReplacementPairImpl replacementPair = new ReplacementPairImpl();
        replacementPair.setReplacementToken(this.replacementTokenTf.getValue());
        replacementPair.setJobPlanParameterName(this.contextParameterSelect.getValue().getName());

        return replacementPair;
    }

    public boolean isSaved() {
        return this.saved;
    }
}
