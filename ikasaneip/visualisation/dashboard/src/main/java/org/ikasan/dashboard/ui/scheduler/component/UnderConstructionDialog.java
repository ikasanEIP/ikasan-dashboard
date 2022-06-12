package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class UnderConstructionDialog extends AbstractCloseableResizableDialog {

    public UnderConstructionDialog() {
        super.title.setText("Under Construction");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.add(new H1("Under Construction"));
        layout.getStyle().set("padding-top", "0px");
        layout.getStyle().set("padding-bottom", "10px");
        super.content.getStyle().set("padding-top", "0px");
        super.content.add(layout);

        this.setHeight("200px");
        this.setWidth("500px");
    }
}
