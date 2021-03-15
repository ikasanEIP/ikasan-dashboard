package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class    ModuleStreamDialog extends AbstractCloseableResizableDialog {
    public ModuleStreamDialog() {
        super.title.setText("Module ");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(new H1("I would show a module"));

        this.setHeight("100%");
        this.setWidth("100%");
        this.add(layout);
    }
}
