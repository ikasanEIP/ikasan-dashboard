package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class MessageChannelNameDialog extends AbstractCloseableResizableDialog {

    private TextField textField;
    boolean okPressed = false;

    public MessageChannelNameDialog() {
        super.title.setText("Provide message channel name");
        this.textField = new TextField("Message channel name");
        textField.setWidthFull();
        super.setResizable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.add(textField);
        layout.setWidthFull();
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, textField);

        Button ok = new Button("OK");
        Button cancel = new Button("Cancel");

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(ok, cancel);

        ok.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            boolean isValid = true;
            if(this.textField.getValue() == null || this.textField.getValue().isEmpty())
            {
                this.textField.setErrorMessage("The message channel name cannot be empty!");
                this.textField.setInvalid(true);
                isValid = false;
            }

            if(isValid)
            {
                this.okPressed = true;
                this.close();
            }
        });

        cancel.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.close();
        });

        layout.add(buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);


        super.showResize(false);

        super.content.add(layout);
        this.setHeight("250px");
        this.setWidth("400px");
    }

    public String getMessageChannelName() {
        return this.textField.getValue();
    }

    public boolean isOkPressed() {
        return okPressed;
    }
}
