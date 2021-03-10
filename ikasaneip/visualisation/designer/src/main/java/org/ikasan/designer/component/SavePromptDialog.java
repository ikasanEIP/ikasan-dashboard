package org.ikasan.designer.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.ikasan.designer.action.DesignerAction;

public class SavePromptDialog extends DesignerActionDialog {
    private DesignerAction designerAction;

    public SavePromptDialog(DesignerAction designerAction) {
        this.designerAction = designerAction;
        this.showResize(false);

        H2 label = new H2("Your diagram has unsaved changes. Are you sure you want to continue?");

        Button ok = new Button("OK");
        Button cancel = new Button("Cancel");

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(ok, cancel);

        ok.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.close();
            if(designerAction != null) {
               designerAction.execute();
            }
        });

        cancel.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.close();
        });

        this.content.add(label, buttonLayout);
        this.content.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, label, buttonLayout);

        this.setWidth("600px");
        this.setHeight("250px");
    }
}
