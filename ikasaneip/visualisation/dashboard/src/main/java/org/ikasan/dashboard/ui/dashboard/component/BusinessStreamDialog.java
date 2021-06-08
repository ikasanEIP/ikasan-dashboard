package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class BusinessStreamDialog extends AbstractCloseableResizableDialog {
    public BusinessStreamDialog() {
        super.title.setText("Business Stream ");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(new H1("I would show a business stream"));

        this.setHeight("100%");
        this.setWidth("100%");
        this.add(layout);
    }
}
